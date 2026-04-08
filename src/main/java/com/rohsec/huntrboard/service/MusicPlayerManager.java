package com.rohsec.huntrboard.service;

import burp.api.montoya.logging.Logging;
import com.rohsec.huntrboard.model.MusicTrack;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MusicPlayerManager implements BasicPlayerListener {
    private final Logging logging;
    private final BasicPlayer player;
    private final List<MusicTrack> playlist = new ArrayList<>();

    private Consumer<String> currentTrackListener = ignored -> {};
    private Consumer<String> statusListener = ignored -> {};
    private Runnable playlistListener = () -> {};
    private int currentIndex = -1;
    private boolean paused;
    private double volume = 0.75d;

    public MusicPlayerManager(Logging logging) {
        this.logging = logging;
        this.player = new BasicPlayer();
        this.player.addBasicPlayerListener(this);
    }

    public synchronized void setCurrentTrackListener(Consumer<String> listener) {
        this.currentTrackListener = listener == null ? ignored -> {} : listener;
        this.currentTrackListener.accept(currentTrackLabel());
    }

    public synchronized void setStatusListener(Consumer<String> listener) {
        this.statusListener = listener == null ? ignored -> {} : listener;
    }

    public synchronized void setPlaylistListener(Runnable listener) {
        this.playlistListener = listener == null ? () -> {} : listener;
    }

    public synchronized void setPlaylist(List<MusicTrack> tracks) {
        playlist.clear();
        if (tracks != null) {
            playlist.addAll(tracks);
        }
        if (currentIndex >= playlist.size()) {
            currentIndex = playlist.isEmpty() ? -1 : 0;
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized List<MusicTrack> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized double getVolume() {
        return volume;
    }

    public synchronized void setCurrentIndex(int index) {
        if (index < -1 || index >= playlist.size()) {
            currentIndex = playlist.isEmpty() ? -1 : 0;
        } else {
            currentIndex = index;
        }
        emitCurrentTrack();
    }

    public synchronized void addTracks(List<Path> paths) {
        for (Path path : paths) {
            playlist.add(MusicTrack.fromPath(path));
        }
        if (currentIndex < 0 && !playlist.isEmpty()) {
            currentIndex = 0;
        }
        firePlaylistChanged();
    }

    public synchronized void removeTrack(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }
        boolean removingCurrent = index == currentIndex;
        playlist.remove(index);
        if (playlist.isEmpty()) {
            stop();
            currentIndex = -1;
        } else if (index < currentIndex) {
            currentIndex--;
        } else if (removingCurrent) {
            stop();
            currentIndex = Math.min(index, playlist.size() - 1);
        }
        firePlaylistChanged();
        emitCurrentTrack();
    }

    public synchronized void playAt(int index) {
        if (index < 0 || index >= playlist.size()) {
            notifyStatus("No track selected.");
            return;
        }
        currentIndex = index;
        paused = false;
        MusicTrack track = playlist.get(index);
        File file = Path.of(track.path).toFile();
        if (!file.isFile()) {
            notifyStatus("Track missing: " + track.displayName);
            return;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(MusicPlayerManager.class.getClassLoader());
            player.stop();
            player.open(file);
            player.play();
            applyGain();
            notifyStatus("Playing " + track.displayName);
            emitCurrentTrack();
        } catch (Exception exception) {
            logging.logToError("HuntrBoard player error for '" + track.displayName + "': " + exception);
            notifyStatus("Unable to play " + track.displayName + ". Check Burp's extension output for details.");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public synchronized void playOrResume() {
        try {
            if (paused) {
                player.resume();
                paused = false;
                notifyStatus("Resumed playback.");
                return;
            }
            if (currentIndex < 0 && !playlist.isEmpty()) {
                currentIndex = 0;
            }
            playAt(currentIndex);
        } catch (Exception exception) {
            logging.logToError("HuntrBoard resume error: " + exception);
            notifyStatus("Unable to resume playback.");
        }
    }

    public synchronized void pause() {
        try {
            player.pause();
            paused = true;
            notifyStatus("Playback paused.");
        } catch (Exception exception) {
            logging.logToError("HuntrBoard pause error: " + exception);
            notifyStatus("Unable to pause playback.");
        }
    }

    public synchronized void stop() {
        try {
            player.stop();
        } catch (Exception exception) {
            logging.logToError("HuntrBoard stop error: " + exception);
        }
        paused = false;
        notifyStatus("Playback stopped.");
    }

    public synchronized void next() {
        if (playlist.isEmpty()) {
            notifyStatus("Playlist is empty.");
            return;
        }
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % playlist.size();
        playAt(nextIndex);
    }

    public synchronized void previous() {
        if (playlist.isEmpty()) {
            notifyStatus("Playlist is empty.");
            return;
        }
        int previousIndex = currentIndex <= 0 ? playlist.size() - 1 : currentIndex - 1;
        playAt(previousIndex);
    }

    public synchronized void setVolume(double volume) {
        this.volume = Math.max(0.0d, Math.min(1.0d, volume));
        applyGain();
    }

    private void applyGain() {
        try {
            player.setGain(volume);
        } catch (Exception ignored) {
            // Some environments do not support gain control for the current decoder.
        }
    }

    private void firePlaylistChanged() {
        SwingUtilities.invokeLater(playlistListener);
    }

    private void emitCurrentTrack() {
        String label = currentTrackLabel();
        SwingUtilities.invokeLater(() -> currentTrackListener.accept(label));
    }

    private void notifyStatus(String status) {
        SwingUtilities.invokeLater(() -> statusListener.accept(status));
    }

    private String currentTrackLabel() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            return "Nothing playing";
        }
        return playlist.get(currentIndex).displayName;
    }

    public synchronized void close() {
        stop();
    }

    @Override
    public void opened(Object stream, Map properties) {
        notifyStatus("Decoder ready.");
    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
    }

    @Override
    public void stateUpdated(BasicPlayerEvent event) {
        if (event.getCode() == BasicPlayerEvent.EOM) {
            SwingUtilities.invokeLater(this::next);
        }
    }

    @Override
    public void setController(BasicController controller) {
    }
}
