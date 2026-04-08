package com.rohsec.huntrboard.service;

import burp.api.montoya.logging.Logging;
import com.rohsec.huntrboard.model.MusicTrack;
import com.rohsec.huntrboard.model.RadioStation;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MusicPlayerManager implements BasicPlayerListener {
    public static final String MODE_RADIO = "RADIO";
    public static final String MODE_LOCAL = "LOCAL";
    private static final int STREAM_BUFFER_SIZE = 512 * 1024;
    private static final int LINE_BUFFER_SIZE = 64 * 1024;

    private final Logging logging;
    private final Random random = new Random();
    private final ExecutorService executorService;
    private final List<MusicTrack> localTracks = new ArrayList<>();
    private final List<RadioStation> radioStations = new ArrayList<>();

    private Consumer<String> currentTrackListener = ignored -> {};
    private Consumer<String> statusListener = ignored -> {};
    private Consumer<Boolean> playbackStateListener = ignored -> {};
    private Runnable playlistListener = () -> {};

    private volatile BasicPlayer player;
    private volatile HttpURLConnection currentConnection;
    private volatile InputStream currentStream;
    private int currentIndex = -1;
    private int currentRadioIndex = -1;
    private boolean paused;
    private boolean playing;
    private double volume = 0.75d;
    private String audioSourceMode = MODE_RADIO;
    private String activePlaybackMode = MODE_RADIO;

    public MusicPlayerManager(Logging logging) {
        this.logging = logging;
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HuntrBoard-jukebox");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void setCurrentTrackListener(Consumer<String> listener) {
        this.currentTrackListener = listener == null ? ignored -> {} : listener;
        emitCurrentTrack();
    }

    public synchronized void setStatusListener(Consumer<String> listener) {
        this.statusListener = listener == null ? ignored -> {} : listener;
    }

    public synchronized void setPlaybackStateListener(Consumer<Boolean> listener) {
        this.playbackStateListener = listener == null ? ignored -> {} : listener;
        emitPlaybackState();
    }

    public synchronized void setPlaylistListener(Runnable listener) {
        this.playlistListener = listener == null ? () -> {} : listener;
    }

    public void setAudioSourceMode(String mode) {
        String normalized = MODE_LOCAL.equalsIgnoreCase(mode) ? MODE_LOCAL : MODE_RADIO;
        boolean changed;
        synchronized (this) {
            changed = !normalized.equals(this.audioSourceMode);
            this.audioSourceMode = normalized;
        }
        if (changed) {
            executorService.submit(() -> stopInternal("Source changed."));
            firePlaylistChanged();
            emitCurrentTrack();
        }
    }

    public synchronized String getAudioSourceMode() {
        return audioSourceMode;
    }

    public synchronized void setPlaylist(List<MusicTrack> tracks) {
        localTracks.clear();
        if (tracks != null) {
            localTracks.addAll(tracks);
        }
        if (currentIndex >= localTracks.size()) {
            currentIndex = localTracks.isEmpty() ? -1 : 0;
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized List<MusicTrack> getPlaylist() {
        return new ArrayList<>(localTracks);
    }

    public synchronized void setRadioStations(List<RadioStation> stations) {
        radioStations.clear();
        if (stations != null) {
            radioStations.addAll(stations);
        }
        if (currentRadioIndex >= radioStations.size()) {
            currentRadioIndex = radioStations.isEmpty() ? -1 : 0;
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized List<RadioStation> getRadioStations() {
        return new ArrayList<>(radioStations);
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized void setCurrentIndex(int index) {
        currentIndex = index < -1 || index >= localTracks.size() ? (localTracks.isEmpty() ? -1 : 0) : index;
        emitCurrentTrack();
    }

    public synchronized int getCurrentRadioIndex() {
        return currentRadioIndex;
    }

    public synchronized void setCurrentRadioIndex(int index) {
        currentRadioIndex = index < -1 || index >= radioStations.size() ? (radioStations.isEmpty() ? -1 : 0) : index;
        emitCurrentTrack();
    }

    public synchronized double getVolume() {
        return volume;
    }

    public synchronized boolean isPlaying() {
        return playing;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized void addTracks(List<Path> paths) {
        for (Path path : paths) {
            localTracks.add(MusicTrack.fromPath(path));
        }
        if (currentIndex < 0 && !localTracks.isEmpty()) {
            currentIndex = 0;
        }
        firePlaylistChanged();
    }

    public synchronized void removeTrack(int index) {
        if (MODE_RADIO.equals(audioSourceMode)) {
            removeRadioStation(index);
            return;
        }
        if (index < 0 || index >= localTracks.size()) {
            return;
        }
        boolean removingCurrent = MODE_LOCAL.equals(activePlaybackMode) && index == currentIndex;
        localTracks.remove(index);
        if (localTracks.isEmpty()) {
            stopInternal("Playback stopped.");
            currentIndex = -1;
        } else if (index < currentIndex) {
            currentIndex--;
        } else if (removingCurrent) {
            stopInternal("Playback stopped.");
            currentIndex = Math.min(index, localTracks.size() - 1);
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized void addOrUpdateRadioStation(String name, String url) {
        String safeName = name == null || name.isBlank() ? "Radio Stream" : name.trim();
        String safeUrl = url == null ? "" : url.trim();
        if (safeUrl.isBlank()) {
            notifyStatus("Stream URL is required.");
            return;
        }
        int existingIndex = -1;
        for (int index = 0; index < radioStations.size(); index++) {
            RadioStation station = radioStations.get(index);
            if (safeUrl.equalsIgnoreCase(station.url)) {
                existingIndex = index;
                break;
            }
        }
        if (existingIndex >= 0) {
            radioStations.set(existingIndex, new RadioStation(safeName, safeUrl));
            currentRadioIndex = existingIndex;
            notifyStatus("Updated radio stream.");
        } else {
            radioStations.add(new RadioStation(safeName, safeUrl));
            if (currentRadioIndex < 0) {
                currentRadioIndex = 0;
            }
            notifyStatus("Added radio stream.");
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized void removeRadioStation(int index) {
        if (index < 0 || index >= radioStations.size()) {
            return;
        }
        boolean removingCurrent = MODE_RADIO.equals(activePlaybackMode) && index == currentRadioIndex;
        radioStations.remove(index);
        if (radioStations.isEmpty()) {
            stopInternal("Stream stopped.");
            currentRadioIndex = -1;
        } else if (index < currentRadioIndex) {
            currentRadioIndex--;
        } else if (removingCurrent) {
            stopInternal("Stream stopped.");
            currentRadioIndex = Math.min(index, radioStations.size() - 1);
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized void shuffle() {
        if (MODE_RADIO.equals(audioSourceMode)) {
            if (radioStations.size() < 2) {
                notifyStatus("Add at least two radio streams to shuffle.");
                return;
            }
            RadioStation current = currentRadioIndex >= 0 && currentRadioIndex < radioStations.size() ? radioStations.get(currentRadioIndex) : null;
            Collections.shuffle(radioStations, random);
            if (current != null) {
                currentRadioIndex = radioStations.indexOf(current);
            }
        } else {
            if (localTracks.size() < 2) {
                notifyStatus("Add at least two local tracks to shuffle.");
                return;
            }
            MusicTrack current = currentIndex >= 0 && currentIndex < localTracks.size() ? localTracks.get(currentIndex) : null;
            Collections.shuffle(localTracks, random);
            if (current != null) {
                currentIndex = localTracks.indexOf(current);
            }
        }
        firePlaylistChanged();
        notifyStatus("Queue shuffled.");
        emitCurrentTrack();
    }

    public synchronized void togglePlayPause(int selectedIndex) {
        if (playing) {
            pause();
            return;
        }
        if (paused) {
            resume();
            return;
        }
        if (MODE_RADIO.equals(audioSourceMode)) {
            int index = selectedIndex >= 0 ? selectedIndex : (currentRadioIndex >= 0 ? currentRadioIndex : 0);
            playRadioAt(index);
        } else {
            int index = selectedIndex >= 0 ? selectedIndex : (currentIndex >= 0 ? currentIndex : 0);
            playAt(index);
        }
    }

    public synchronized void playAt(int index) {
        if (index < 0 || index >= localTracks.size()) {
            notifyStatus("No local track selected.");
            return;
        }
        currentIndex = index;
        MusicTrack track = localTracks.get(index);
        File file = Path.of(track.path).toFile();
        if (!file.isFile()) {
            notifyStatus("Track missing: " + track.displayName);
            return;
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(MusicPlayerManager.class.getClassLoader());
            stopInternalSilently();
            BasicPlayer newPlayer = new BasicPlayer();
            newPlayer.addBasicPlayerListener(this);
            newPlayer.open(file);
            newPlayer.play();
            player = newPlayer;
            paused = false;
            playing = true;
            activePlaybackMode = MODE_LOCAL;
            applyGain();
            notifyStatus("Playing " + track.displayName);
            emitCurrentTrack();
            emitPlaybackState();
        } catch (Exception exception) {
            logging.logToError("HuntrBoard local playback error for '" + track.displayName + "': " + exception);
            paused = false;
            playing = false;
            emitPlaybackState();
            notifyStatus("Unable to play " + track.displayName + ". Check Burp's extension output for details.");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public synchronized void playRadioAt(int index) {
        if (index < 0 || index >= radioStations.size()) {
            notifyStatus("No radio stream selected.");
            return;
        }
        currentRadioIndex = index;
        RadioStation station = radioStations.get(index);
        executorService.submit(() -> playRadioInternal(station, index));
    }

    private void playRadioInternal(RadioStation station, int index) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(MusicPlayerManager.class.getClassLoader());
            stopInternalSilently();
            HttpURLConnection connection = (HttpURLConnection) new URL(station.url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(0);
            connection.setRequestProperty("User-Agent", "HuntrBoard/1.0");
            connection.setRequestProperty("Icy-MetaData", "1");
            connection.setRequestProperty("Accept", "*/*");
            connection.connect();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream(), STREAM_BUFFER_SIZE);
            BasicPlayer.EXTERNAL_BUFFER_SIZE = STREAM_BUFFER_SIZE;
            BasicPlayer newPlayer = new BasicPlayer();
            newPlayer.addBasicPlayerListener(this);
            newPlayer.setLineBufferSize(LINE_BUFFER_SIZE);
            newPlayer.open(bufferedInputStream);
            newPlayer.play();

            synchronized (this) {
                currentConnection = connection;
                currentStream = bufferedInputStream;
                player = newPlayer;
                currentRadioIndex = index;
                paused = false;
                playing = true;
                activePlaybackMode = MODE_RADIO;
                applyGain();
            }
            notifyStatus("Playing stream: " + station.name);
            emitCurrentTrack();
            emitPlaybackState();
        } catch (Exception exception) {
            synchronized (this) {
                closeCurrentResources();
                paused = false;
                playing = false;
            }
            emitPlaybackState();
            logging.logToError("HuntrBoard radio stream error for '" + station.name + "' (" + station.url + "): " + exception);
            notifyStatus("Failed stream: " + station.name);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private synchronized void resume() {
        try {
            if (player != null) {
                player.resume();
                paused = false;
                playing = true;
                notifyStatus(MODE_RADIO.equals(activePlaybackMode) ? "Resumed radio stream." : "Resumed playback.");
                emitPlaybackState();
            }
        } catch (Exception exception) {
            logging.logToError("HuntrBoard resume error: " + exception);
            notifyStatus("Unable to resume playback.");
        }
    }

    public synchronized void pause() {
        try {
            if (player != null) {
                player.pause();
                paused = true;
                playing = false;
                notifyStatus(MODE_RADIO.equals(activePlaybackMode) ? "Radio stream paused." : "Playback paused.");
                emitPlaybackState();
            }
        } catch (Exception exception) {
            logging.logToError("HuntrBoard pause error: " + exception);
            notifyStatus("Unable to pause playback.");
        }
    }

    public synchronized void next() {
        if (MODE_RADIO.equals(audioSourceMode)) {
            if (radioStations.isEmpty()) {
                notifyStatus("No saved radio streams.");
                return;
            }
            int nextIndex = currentRadioIndex < 0 ? 0 : (currentRadioIndex + 1) % radioStations.size();
            playRadioAt(nextIndex);
        } else {
            if (localTracks.isEmpty()) {
                notifyStatus("Playlist is empty.");
                return;
            }
            int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % localTracks.size();
            playAt(nextIndex);
        }
    }

    public synchronized void previous() {
        if (MODE_RADIO.equals(audioSourceMode)) {
            if (radioStations.isEmpty()) {
                notifyStatus("No saved radio streams.");
                return;
            }
            int previousIndex = currentRadioIndex <= 0 ? radioStations.size() - 1 : currentRadioIndex - 1;
            playRadioAt(previousIndex);
        } else {
            if (localTracks.isEmpty()) {
                notifyStatus("Playlist is empty.");
                return;
            }
            int previousIndex = currentIndex <= 0 ? localTracks.size() - 1 : currentIndex - 1;
            playAt(previousIndex);
        }
    }

    public synchronized void setVolume(double volume) {
        this.volume = Math.max(0.0d, Math.min(1.0d, volume));
        applyGain();
    }

    private synchronized void applyGain() {
        try {
            if (player != null) {
                player.setGain(volume);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopInternal(String status) {
        stopInternalSilently();
        notifyStatus(status);
    }

    private void stopInternalSilently() {
        BasicPlayer playerToStop;
        InputStream streamToClose;
        HttpURLConnection connectionToClose;
        synchronized (this) {
            playerToStop = player;
            player = null;
            streamToClose = currentStream;
            currentStream = null;
            connectionToClose = currentConnection;
            currentConnection = null;
            paused = false;
            playing = false;
        }
        if (playerToStop != null) {
            try {
                playerToStop.stop();
            } catch (Exception exception) {
                logging.logToError("HuntrBoard stop error: " + exception);
            }
        }
        closeResources(streamToClose, connectionToClose);
        emitPlaybackState();
    }

    private void closeCurrentResources() {
        InputStream streamToClose;
        HttpURLConnection connectionToClose;
        synchronized (this) {
            streamToClose = currentStream;
            currentStream = null;
            connectionToClose = currentConnection;
            currentConnection = null;
        }
        closeResources(streamToClose, connectionToClose);
    }

    private void closeResources(InputStream streamToClose, HttpURLConnection connectionToClose) {
        if (streamToClose != null) {
            try {
                streamToClose.close();
            } catch (IOException ignored) {
            }
        }
        if (connectionToClose != null) {
            connectionToClose.disconnect();
        }
    }

    private void firePlaylistChanged() {
        SwingUtilities.invokeLater(playlistListener);
    }

    private synchronized void emitCurrentTrack() {
        String label;
        if (MODE_RADIO.equals(audioSourceMode)) {
            label = currentRadioIndex >= 0 && currentRadioIndex < radioStations.size() ? radioStations.get(currentRadioIndex).name : "No radio stream selected";
        } else {
            label = currentIndex >= 0 && currentIndex < localTracks.size() ? localTracks.get(currentIndex).displayName : "Nothing playing";
        }
        SwingUtilities.invokeLater(() -> currentTrackListener.accept(label));
    }

    private void emitPlaybackState() {
        boolean state = playing;
        SwingUtilities.invokeLater(() -> playbackStateListener.accept(state));
    }

    private void notifyStatus(String status) {
        SwingUtilities.invokeLater(() -> statusListener.accept(status));
    }

    public synchronized void close() {
        stopInternalSilently();
        executorService.shutdownNow();
    }

    @Override
    public synchronized void opened(Object stream, Map properties) {
    }

    @Override
    public synchronized void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
    }

    @Override
    public synchronized void stateUpdated(BasicPlayerEvent event) {
        int code = event.getCode();
        if (code == BasicPlayerEvent.PLAYING || code == BasicPlayerEvent.RESUMED) {
            playing = true;
            paused = false;
            emitPlaybackState();
            return;
        }
        if (code == BasicPlayerEvent.PAUSED) {
            playing = false;
            paused = true;
            emitPlaybackState();
            return;
        }
        if (code == BasicPlayerEvent.STOPPED) {
            playing = false;
            paused = false;
            closeCurrentResources();
            emitPlaybackState();
            return;
        }
        if (code == BasicPlayerEvent.EOM) {
            playing = false;
            paused = false;
            closeCurrentResources();
            emitPlaybackState();
            if (MODE_LOCAL.equals(activePlaybackMode)) {
                SwingUtilities.invokeLater(this::next);
            }
        }
    }

    @Override
    public void setController(BasicController controller) {
    }
}
