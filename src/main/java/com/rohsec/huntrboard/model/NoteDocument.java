package com.rohsec.huntrboard.model;

import java.time.Instant;
import java.util.UUID;

public class NoteDocument {
    public String id;
    public String title;
    public String content;
    public long updatedAt;

    public NoteDocument() {
    }

    public static NoteDocument create(String title) {
        NoteDocument document = new NoteDocument();
        document.id = UUID.randomUUID().toString();
        document.title = title;
        document.content = "";
        document.updatedAt = Instant.now().toEpochMilli();
        return document;
    }

    @Override
    public String toString() {
        return title == null || title.isBlank() ? "Untitled" : title;
    }
}
