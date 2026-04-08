package com.rohsec.huntrboard.ui.components;

import com.rohsec.huntrboard.ui.ThemePalette;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CardPanel extends JPanel {
    public CardPanel(ThemePalette palette) {
        super(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(palette.cardBackground);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.cardBorder, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
    }
}
