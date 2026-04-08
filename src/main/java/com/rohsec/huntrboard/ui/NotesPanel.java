package com.rohsec.huntrboard.ui;

import com.rohsec.huntrboard.model.NoteDocument;
import com.rohsec.huntrboard.ui.components.CardPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.AbstractAction;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotesPanel extends CardPanel {
    private final DefaultListModel<NoteDocument> listModel = new DefaultListModel<>();
    private final JList<NoteDocument> noteList = new JList<>(listModel);
    private final JTextArea editorArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final Consumer<String> statusCallback;
    private final Runnable persistCallback;
    private final UndoManager undoManager = new UndoManager();
    private boolean switching;
    private boolean resettingUndo;
    private String lastSelectedNoteId;

    public NotesPanel(ThemePalette palette, List<NoteDocument> notes, String activeNoteId,
                      Consumer<String> statusCallback, Runnable persistCallback) {
        super(palette);
        this.statusCallback = statusCallback;
        this.persistCallback = persistCallback;
        setLayout(new BorderLayout(0, 12));

        add(buildHeader(palette), BorderLayout.NORTH);
        add(buildContent(palette), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(420, 510));
        setMinimumSize(new Dimension(320, 460));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 510));
        statusLabel.setForeground(palette.textSecondary);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.addListSelectionListener(this::handleSelection);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editorArea.setBackground(palette.fieldBackground);
        editorArea.setForeground(palette.textPrimary);
        editorArea.setCaretColor(palette.textPrimary);
        editorArea.setSelectionColor(palette.accent);
        editorArea.getDocument().addUndoableEditListener(event -> {
            if (!resettingUndo) {
                undoManager.addEdit(event.getEdit());
            }
        });
        installUndoRedoBindings();
        noteList.setBackground(palette.fieldBackground);
        noteList.setForeground(palette.textPrimary);
        noteList.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));

        setNotes(notes, activeNoteId);
    }

    private JPanel buildHeader(ThemePalette palette) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setAlignmentX(LEFT_ALIGNMENT);
        JPanel heading = UiSupport.createTitleLabel("✎", "Notepad", palette, 16f);
        JLabel subtitle = new JLabel("Quick scratchpads that persist across Burp sessions.");
        subtitle.setForeground(palette.textSecondary);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        JButton newButton = UiSupport.createIconButton("＋", "New note", palette);
        JButton renameButton = UiSupport.createIconButton("✎", "Rename note", palette);
        JButton saveButton = UiSupport.createAccentButton("⭳", palette);
        saveButton.setToolTipText("Save note");
        JButton deleteButton = UiSupport.createIconButton("⌦", "Delete note", palette);
        newButton.addActionListener(event -> createNote());
        renameButton.addActionListener(event -> renameNote());
        saveButton.addActionListener(event -> saveCurrentNote());
        deleteButton.addActionListener(event -> deleteNote());
        actions.add(newButton);
        actions.add(renameButton);
        actions.add(saveButton);
        actions.add(deleteButton);

        header.add(titles);
        header.add(Box.createVerticalStrut(8));
        header.add(actions);
        return header;
    }

    private JPanel buildContent(ThemePalette palette) {
        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);
        JScrollPane listScroll = new JScrollPane(noteList);
        listScroll.setPreferredSize(new Dimension(160, 380));
        listScroll.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));

        JScrollPane editorScroll = new JScrollPane(editorArea);
        editorScroll.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));
        editorScroll.setPreferredSize(new Dimension(320, 380));
        editorScroll.setMinimumSize(new Dimension(240, 300));

        content.add(listScroll, BorderLayout.WEST);
        content.add(editorScroll, BorderLayout.CENTER);
        content.setPreferredSize(new Dimension(0, 380));
        content.setMinimumSize(new Dimension(0, 380));
        return content;
    }

    private void handleSelection(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || switching) {
            return;
        }
        NoteDocument previous = findNoteById(lastSelectedNoteId);
        if (previous != null) {
            previous.content = editorArea.getText();
        }
        NoteDocument selected = noteList.getSelectedValue();
        if (selected != null) {
            setEditorText(selected.content == null ? "" : selected.content);
            lastSelectedNoteId = selected.id;
        } else {
            setEditorText("");
            lastSelectedNoteId = null;
        }
        updateStatus("Loaded note.");
    }

    public void setNotes(List<NoteDocument> notes, String activeNoteId) {
        switching = true;
        listModel.clear();
        for (NoteDocument note : notes) {
            listModel.addElement(note);
        }
        if (!listModel.isEmpty()) {
            int selectedIndex = 0;
            if (activeNoteId != null) {
                for (int index = 0; index < listModel.size(); index++) {
                    if (activeNoteId.equals(listModel.get(index).id)) {
                        selectedIndex = index;
                        break;
                    }
                }
            }
            noteList.setSelectedIndex(selectedIndex);
            setEditorText(listModel.get(selectedIndex).content == null ? "" : listModel.get(selectedIndex).content);
            lastSelectedNoteId = listModel.get(selectedIndex).id;
        } else {
            setEditorText("");
        }
        switching = false;
    }

    public List<NoteDocument> getNotes() {
        flushCurrentEditor();
        List<NoteDocument> notes = new ArrayList<>();
        for (int index = 0; index < listModel.size(); index++) {
            notes.add(listModel.get(index));
        }
        return notes;
    }

    public String getActiveNoteId() {
        NoteDocument note = noteList.getSelectedValue();
        return note == null ? null : note.id;
    }

    public void flushCurrentEditor() {
        NoteDocument note = noteList.getSelectedValue();
        if (note != null) {
            note.content = editorArea.getText();
        }
    }

    private void createNote() {
        String name = JOptionPane.showInputDialog(this, "New note title:", "Create note", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        NoteDocument note = NoteDocument.create(name.trim());
        listModel.add(0, note);
        noteList.setSelectedIndex(0);
        setEditorText("");
        saveCurrentNote();
        updateStatus("Created note '" + note.title + "'.");
    }

    private void renameNote() {
        NoteDocument note = noteList.getSelectedValue();
        if (note == null) {
            updateStatus("No note selected.");
            return;
        }
        String next = JOptionPane.showInputDialog(this, "Rename note:", note.title);
        if (next == null || next.isBlank()) {
            return;
        }
        note.title = next.trim();
        note.updatedAt = Instant.now().toEpochMilli();
        noteList.repaint();
        persistCallback.run();
        updateStatus("Renamed note.");
    }

    private void deleteNote() {
        NoteDocument note = noteList.getSelectedValue();
        if (note == null) {
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Delete note '" + note.title + "'?",
                "Delete note",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        int selected = noteList.getSelectedIndex();
        listModel.remove(selected);
        if (listModel.isEmpty()) {
            listModel.addElement(NoteDocument.create("Global Scratchpad"));
        }
        noteList.setSelectedIndex(Math.max(0, selected - 1));
        saveCurrentNote();
        updateStatus("Deleted note.");
    }

    private void saveCurrentNote() {
        flushCurrentEditor();
        NoteDocument note = noteList.getSelectedValue();
        if (note == null) {
            updateStatus("No note selected.");
            return;
        }
        note.updatedAt = Instant.now().toEpochMilli();
        noteList.repaint();
        persistCallback.run();
        updateStatus("Saved '" + note.title + "'.");
    }

    private void setEditorText(String text) {
        resettingUndo = true;
        try {
            editorArea.setText(text);
            undoManager.discardAllEdits();
            editorArea.setCaretPosition(0);
        } finally {
            resettingUndo = false;
        }
    }

    private void installUndoRedoBindings() {
        editorArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "huntrboard-undo");
        editorArea.getActionMap().put("huntrboard-undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException ignored) {
                }
            }
        });
        editorArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "huntrboard-redo");
        editorArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "huntrboard-redo");
        editorArea.getActionMap().put("huntrboard-redo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotRedoException ignored) {
                }
            }
        });
    }

    private NoteDocument findNoteById(String noteId) {
        if (noteId == null) {
            return null;
        }
        for (int index = 0; index < listModel.size(); index++) {
            NoteDocument note = listModel.get(index);
            if (noteId.equals(note.id)) {
                return note;
            }
        }
        return null;
    }

    private void updateStatus(String status) {
        statusLabel.setText(status);
        statusCallback.accept(status);
    }
}
