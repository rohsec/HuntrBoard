package com.rohsec.huntrboard.ui.filters;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DecimalDocumentFilter extends DocumentFilter {
    private static final String PATTERN = "^\\d*(\\.\\d{0,2})?$";

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        String next = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
        if (next.isBlank() || next.matches(PATTERN)) {
            fb.replace(offset, length, text, attrs);
        }
    }
}
