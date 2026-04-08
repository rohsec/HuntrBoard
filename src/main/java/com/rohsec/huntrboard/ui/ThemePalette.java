package com.rohsec.huntrboard.ui;

import burp.api.montoya.ui.Theme;

import java.awt.Color;

public class ThemePalette {
    public final Color appBackground;
    public final Color cardBackground;
    public final Color cardBorder;
    public final Color accent;
    public final Color accentMuted;
    public final Color success;
    public final Color warning;
    public final Color danger;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color fieldBackground;
    public final Color fieldBorder;
    public final Color chartGrid;

    private ThemePalette(Color appBackground, Color cardBackground, Color cardBorder, Color accent,
                         Color accentMuted, Color success, Color warning, Color danger,
                         Color textPrimary, Color textSecondary, Color fieldBackground,
                         Color fieldBorder, Color chartGrid) {
        this.appBackground = appBackground;
        this.cardBackground = cardBackground;
        this.cardBorder = cardBorder;
        this.accent = accent;
        this.accentMuted = accentMuted;
        this.success = success;
        this.warning = warning;
        this.danger = danger;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.fieldBackground = fieldBackground;
        this.fieldBorder = fieldBorder;
        this.chartGrid = chartGrid;
    }

    public static ThemePalette forTheme(Theme theme) {
        if (theme == Theme.LIGHT) {
            return new ThemePalette(
                    new Color(242, 245, 250),
                    Color.WHITE,
                    new Color(220, 226, 236),
                    new Color(69, 126, 255),
                    new Color(104, 141, 218),
                    new Color(40, 180, 110),
                    new Color(225, 166, 43),
                    new Color(224, 86, 86),
                    new Color(20, 27, 39),
                    new Color(98, 110, 126),
                    new Color(248, 250, 252),
                    new Color(214, 220, 230),
                    new Color(228, 234, 241)
            );
        }
        return new ThemePalette(
                new Color(20, 22, 28),
                new Color(28, 31, 39),
                new Color(55, 61, 72),
                new Color(102, 153, 255),
                new Color(88, 119, 182),
                new Color(67, 194, 127),
                new Color(224, 175, 60),
                new Color(229, 94, 94),
                new Color(234, 238, 245),
                new Color(152, 161, 176),
                new Color(25, 28, 35),
                new Color(65, 72, 84),
                new Color(52, 57, 68)
        );
    }
}
