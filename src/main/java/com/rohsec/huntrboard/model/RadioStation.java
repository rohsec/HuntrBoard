package com.rohsec.huntrboard.model;

public class RadioStation {
    public String name;
    public String url;

    public RadioStation() {
    }

    public RadioStation(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String toString() {
        String safeName = name == null || name.isBlank() ? "Stream" : name;
        String safeUrl = url == null ? "" : url;
        return safeName + " — " + safeUrl;
    }
}
