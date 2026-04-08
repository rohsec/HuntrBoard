package com.rohsec.huntrboard.service;

import com.rohsec.huntrboard.model.DashboardMetrics;
import com.rohsec.huntrboard.model.TrackerData;

import java.util.List;

public class TrackerService {
    public static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    public DashboardMetrics calculate(TrackerData trackerData) {
        trackerData.ensureMonthCount();
        List<Double> values = trackerData.monthlyValues;

        double total = 0.0d;
        double bestValue = Double.NEGATIVE_INFINITY;
        double worstValue = Double.POSITIVE_INFINITY;
        int bestMonth = 0;
        int worstMonth = 0;

        for (int index = 0; index < values.size(); index++) {
            double value = safe(values.get(index));
            total += value;
            if (value > bestValue) {
                bestValue = value;
                bestMonth = index;
            }
            if (value < worstValue) {
                worstValue = value;
                worstMonth = index;
            }
        }

        double target = Math.max(0.0d, trackerData.yearlyTarget);
        double remaining = Math.max(0.0d, target - total);
        double percent = target <= 0.0d ? 0.0d : Math.min(100.0d, (total / target) * 100.0d);
        double average = total / 12.0d;

        return new DashboardMetrics(total, remaining, percent, average, bestMonth, worstMonth);
    }

    public double safe(Double value) {
        return value == null ? 0.0d : Math.max(0.0d, value);
    }
}
