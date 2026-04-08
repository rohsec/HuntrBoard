package com.rohsec.huntrboard.model;

import java.nio.file.Path;

public class MusicTrack {
    public String displayName;
    public String path;

    public MusicTrack() {
    }

    public MusicTrack(String displayName, String path) {
        this.displayName = displayName;
        this.path = path;
    }

    public static MusicTrack fromPath(Path path) {
        return new MusicTrack(path.getFileName().toString(), path.toAbsolutePath().toString());
    }

    @Override
    public String toString() {
        return displayName == null || displayName.isBlank() ? path : displayName;
    }
}
