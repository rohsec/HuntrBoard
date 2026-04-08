package com.rohsec.huntrboard.model;

public class DashboardMetrics {
    public final double totalAchieved;
    public final double remainingTarget;
    public final double percentComplete;
    public final double averagePerMonth;
    public final int bestMonthIndex;
    public final int worstMonthIndex;

    public DashboardMetrics(double totalAchieved, double remainingTarget, double percentComplete,
                            double averagePerMonth, int bestMonthIndex, int worstMonthIndex) {
        this.totalAchieved = totalAchieved;
        this.remainingTarget = remainingTarget;
        this.percentComplete = percentComplete;
        this.averagePerMonth = averagePerMonth;
        this.bestMonthIndex = bestMonthIndex;
        this.worstMonthIndex = worstMonthIndex;
    }
}
