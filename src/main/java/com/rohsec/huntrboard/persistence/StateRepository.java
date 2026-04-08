package com.rohsec.huntrboard.persistence;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rohsec.huntrboard.model.AppState;
import com.rohsec.huntrboard.model.NoteDocument;
import com.rohsec.huntrboard.model.RadioStation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StateRepository {
    private static final String STATE_KEY = "huntrboard.state";
    private static final String BRANDING_NOTE = """
 _   _             _       ____                       _ 
| | | |_   _ _ __ | |_ _ _| __ )  ___   __ _ _ __ __| |
| |_| | | | | '_ \\| __| '__|  _ \\ / _ \\ / _` | '__/ _` |
|  _  | |_| | | | | |_| |  | |_) | (_) | (_| | | | (_| |
|_| |_|\\__,_|_| |_|\\__|_|  |____/ \\___/ \\__,_|_|  \\__,_|

Version: 1.0.0
Built for Burp Suite with Montoya API + Java 21
Focus: targets, notes, and JukeBox workflow in one tab.
""";

    private final Persistence persistence;
    private final Logging logging;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public StateRepository(Persistence persistence, Logging logging) {
        this.persistence = persistence;
        this.logging = logging;
    }

    public AppState load() {
        try {
            String json = persistence.preferences().getString(STATE_KEY);
            if (json == null || json.isBlank()) {
                return defaultState();
            }
            AppState state = gson.fromJson(json, AppState.class);
            return sanitize(state == null ? new AppState() : state);
        } catch (Exception exception) {
            logging.logToError("HuntrBoard failed to load persisted state: " + exception.getMessage());
            return defaultState();
        }
    }

    public void save(AppState state) {
        try {
            persistence.preferences().setString(STATE_KEY, gson.toJson(sanitize(state)));
        } catch (Exception exception) {
            logging.logToError("HuntrBoard failed to save state: " + exception.getMessage());
        }
    }

    private AppState defaultState() {
        AppState state = new AppState();
        state.notes = new ArrayList<>(List.of(createDefaultScratchpad()));
        state.activeNoteId = state.notes.getFirst().id;
        state.audioSourceMode = "RADIO";
        state.radioStations = new ArrayList<>(defaultRadioStations());
        state.currentRadioStationIndex = state.radioStations.isEmpty() ? -1 : 0;
        return state;
    }

    private AppState sanitize(AppState state) {
        if (state.tracker == null) {
            state.tracker = new com.rohsec.huntrboard.model.TrackerData();
        }
        state.tracker.ensureMonthCount();
        if (state.notes == null || state.notes.isEmpty()) {
            state.notes = new ArrayList<>(List.of(createDefaultScratchpad()));
        }
        for (int index = 0; index < state.notes.size(); index++) {
            NoteDocument note = state.notes.get(index);
            if (note == null) {
                note = NoteDocument.create("Untitled Note " + (index + 1));
                state.notes.set(index, note);
            }
            if (note.id == null || note.id.isBlank()) {
                note.id = NoteDocument.create(note.title == null ? "Untitled" : note.title).id;
            }
            if (note.title == null || note.title.isBlank()) {
                note.title = "Untitled Note " + (index + 1);
            }
            if (note.content == null) {
                note.content = "";
            }
            if ("Global Scratchpad".equals(note.title) && note.content.isBlank()) {
                note.content = BRANDING_NOTE;
            }
        }
        state.notes.sort(Comparator.comparingLong(note -> -note.updatedAt));
        if (state.activeNoteId == null || state.notes.stream().noneMatch(note -> note.id.equals(state.activeNoteId))) {
            state.activeNoteId = state.notes.getFirst().id;
        }
        if (state.playlist == null) {
            state.playlist = new ArrayList<>();
        }
        state.playlist.removeIf(track -> track == null || track.path == null || track.path.isBlank());
        for (var track : state.playlist) {
            if (track.displayName == null || track.displayName.isBlank()) {
                track.displayName = track.path;
            }
        }
        if (state.radioStations == null || state.radioStations.isEmpty()) {
            state.radioStations = new ArrayList<>(defaultRadioStations());
        }
        state.radioStations.removeIf(station -> station == null || station.url == null || station.url.isBlank());
        for (var station : state.radioStations) {
            if (station.name == null || station.name.isBlank()) {
                station.name = station.url;
            }
        }
        if (!"LOCAL".equalsIgnoreCase(state.audioSourceMode)) {
            state.audioSourceMode = "RADIO";
        } else {
            state.audioSourceMode = "LOCAL";
        }
        if (state.currentTrackIndex >= state.playlist.size()) {
            state.currentTrackIndex = state.playlist.isEmpty() ? -1 : 0;
        }
        if (state.currentTrackIndex < -1) {
            state.currentTrackIndex = -1;
        }
        if (state.currentRadioStationIndex >= state.radioStations.size()) {
            state.currentRadioStationIndex = state.radioStations.isEmpty() ? -1 : 0;
        }
        if (state.currentRadioStationIndex < -1) {
            state.currentRadioStationIndex = -1;
        }
        if (state.playerVolume < 0.0d || state.playerVolume > 1.0d) {
            state.playerVolume = 0.75d;
        }
        return state;
    }

    private NoteDocument createDefaultScratchpad() {
        NoteDocument note = NoteDocument.create("Global Scratchpad");
        note.content = BRANDING_NOTE;
        return note;
    }

    private List<RadioStation> defaultRadioStations() {
        return List.of(
                new RadioStation("Lofi Radio", "https://boxradio-edge-00.streamafrica.net/lofi"),
                new RadioStation("Chillhop Radio", "https://stream.zeno.fm/f3wvbbqmdg8uv"),
                new RadioStation("Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3"),
                new RadioStation("Mixify New Hits", "https://server.mixify.in/listen/new_hits/radio.mp3")
        );
    }
}
