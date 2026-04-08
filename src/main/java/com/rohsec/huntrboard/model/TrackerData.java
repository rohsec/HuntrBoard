package com.rohsec.huntrboard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackerData {
    public double yearlyTarget;
    public List<Double> monthlyValues = new ArrayList<>();

    public TrackerData() {
        ensureMonthCount();
    }

    public void ensureMonthCount() {
        if (monthlyValues == null) {
            monthlyValues = new ArrayList<>();
        }
        while (monthlyValues.size() < 12) {
            monthlyValues.add(0.0d);
        }
        if (monthlyValues.size() > 12) {
            monthlyValues = new ArrayList<>(monthlyValues.subList(0, 12));
        }
        Collections.replaceAll(monthlyValues, null, 0.0d);
    }
}
