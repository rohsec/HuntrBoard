package com.rohsec.huntrboard.persistence;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rohsec.huntrboard.model.AppState;
import com.rohsec.huntrboard.model.NoteDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StateRepository {
    private static final String STATE_KEY = "huntrboard.state";

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
        state.notes = new ArrayList<>(List.of(NoteDocument.create("Global Scratchpad")));
        state.activeNoteId = state.notes.getFirst().id;
        return state;
    }

    private AppState sanitize(AppState state) {
        if (state.tracker == null) {
            state.tracker = new com.rohsec.huntrboard.model.TrackerData();
        }
        state.tracker.ensureMonthCount();
        if (state.notes == null || state.notes.isEmpty()) {
            state.notes = new ArrayList<>(List.of(NoteDocument.create("Global Scratchpad")));
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
        if (state.currentTrackIndex >= state.playlist.size()) {
            state.currentTrackIndex = state.playlist.isEmpty() ? -1 : 0;
        }
        if (state.currentTrackIndex < -1) {
            state.currentTrackIndex = -1;
        }
        if (state.playerVolume < 0.0d || state.playerVolume > 1.0d) {
            state.playerVolume = 0.75d;
        }
        return state;
    }
}
