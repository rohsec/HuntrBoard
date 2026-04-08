package com.rohsec.huntrboard.model;

import java.util.ArrayList;
import java.util.List;

public class AppState {
    public TrackerData tracker = new TrackerData();
    public List<NoteDocument> notes = new ArrayList<>();
    public String activeNoteId;
    public String audioSourceMode = "RADIO";
    public List<MusicTrack> playlist = new ArrayList<>();
    public List<RadioStation> radioStations = new ArrayList<>();
    public int currentTrackIndex = -1;
    public int currentRadioStationIndex = -1;
    public double playerVolume = 0.75d;

    public AppState() {
    }
}
