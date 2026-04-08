package com.rohsec.huntrboard.ui;

import com.rohsec.huntrboard.model.MusicTrack;
import com.rohsec.huntrboard.service.MusicPlayerManager;
import com.rohsec.huntrboard.ui.components.CardPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

public class MusicPlayerPanel extends CardPanel {
    private final DefaultListModel<MusicTrack> listModel = new DefaultListModel<>();
    private final JList<MusicTrack> playlistList = new JList<>(listModel);
    private final JLabel trackLabel = new JLabel("Nothing playing");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton playPauseButton;

    public MusicPlayerPanel(ThemePalette palette, MusicPlayerManager manager,
                            Runnable persistCallback, Consumer<String> statusCallback) {
        super(palette);
        setLayout(new BorderLayout(0, 10));
        this.playPauseButton = UiSupport.createAccentButton("⏵", palette);
        playPauseButton.setToolTipText("Play or pause");

        add(buildHeader(palette), BorderLayout.NORTH);
        add(buildCenter(palette), BorderLayout.CENTER);
        add(buildFooter(palette, manager, persistCallback, statusCallback), BorderLayout.SOUTH);

        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistList.setVisibleRowCount(6);
        playlistList.setBackground(palette.fieldBackground);
        playlistList.setForeground(palette.textPrimary);
        playlistList.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));
        playlistList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    manager.playAt(playlistList.getSelectedIndex());
                }
            }
        });

        manager.setPlaylistListener(() -> refreshPlaylist(manager));
        manager.setCurrentTrackListener(trackLabel::setText);
        manager.setPlaybackStateListener(this::updatePlayPauseButton);
        manager.setStatusListener(status -> {
            statusLabel.setText(status);
            statusCallback.accept(status);
        });
        refreshPlaylist(manager);
        updatePlayPauseButton(manager.isPlaying());
    }

    private JPanel buildHeader(ThemePalette palette) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("🎵 JukeBox");
        heading.setForeground(palette.textPrimary);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
        JLabel subtitle = new JLabel("Local MP3 playlist for focus sessions inside Burp.");
        subtitle.setForeground(palette.textSecondary);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(3));
        titles.add(subtitle);

        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildCenter(ThemePalette palette) {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);

        JLabel current = new JLabel("Now playing");
        current.setForeground(palette.textSecondary);
        current.setFont(current.getFont().deriveFont(Font.PLAIN, 11f));
        trackLabel.setForeground(palette.textPrimary);
        trackLabel.setFont(trackLabel.getFont().deriveFont(Font.BOLD, 13f));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(current);
        info.add(Box.createVerticalStrut(2));
        info.add(trackLabel);

        JScrollPane scroll = new JScrollPane(playlistList);
        scroll.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));
        scroll.setPreferredSize(new Dimension(240, 128));

        center.add(info, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFooter(ThemePalette palette, MusicPlayerManager manager,
                               Runnable persistCallback, Consumer<String> statusCallback) {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionRow.setOpaque(false);
        JButton addButton = UiSupport.createIconButton("＋", "Add MP3 files", palette);
        JButton removeButton = UiSupport.createIconButton("－", "Remove selected track", palette);
        JButton previousButton = UiSupport.createIconButton("⏮", "Previous", palette);
        JButton shuffleButton = UiSupport.createIconButton("⤮", "Shuffle playlist", palette);
        JButton nextButton = UiSupport.createIconButton("⏭", "Next", palette);

        addButton.addActionListener(event -> addTracks(manager, persistCallback));
        removeButton.addActionListener(event -> removeTrack(manager, persistCallback));
        previousButton.addActionListener(event -> manager.previous());
        shuffleButton.addActionListener(event -> {
            manager.shuffle();
            persistCallback.run();
        });
        playPauseButton.addActionListener(event -> manager.togglePlayPause(playlistList.getSelectedIndex()));
        nextButton.addActionListener(event -> manager.next());

        actionRow.add(addButton);
        actionRow.add(removeButton);
        actionRow.add(previousButton);
        actionRow.add(shuffleButton);
        actionRow.add(playPauseButton);
        actionRow.add(nextButton);

        JPanel volumeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        volumeRow.setOpaque(false);
        JLabel volumeLabel = new JLabel("Volume");
        volumeLabel.setForeground(palette.textSecondary);
        JSlider volumeSlider = new JSlider(0, 100, (int) Math.round(manager.getVolume() * 100.0d));
        volumeSlider.setOpaque(false);
        volumeSlider.setPreferredSize(new Dimension(110, 20));
        volumeSlider.setMaximumSize(new Dimension(110, 20));
        volumeSlider.addChangeListener(event -> {
            manager.setVolume(volumeSlider.getValue() / 100.0d);
            persistCallback.run();
        });
        volumeRow.add(volumeLabel);
        volumeRow.add(volumeSlider);

        statusLabel.setForeground(palette.textSecondary);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        footer.add(actionRow, BorderLayout.NORTH);
        footer.add(volumeRow, BorderLayout.CENTER);
        footer.add(statusLabel, BorderLayout.SOUTH);
        return footer;
    }

    private void updatePlayPauseButton(boolean playing) {
        playPauseButton.setText(playing ? "⏸" : "⏵");
        playPauseButton.setToolTipText(playing ? "Pause" : "Play");
    }

    private void addTracks(MusicPlayerManager manager, Runnable persistCallback) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Add MP3 files");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("MP3 Audio", "mp3"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        manager.addTracks(Arrays.stream(chooser.getSelectedFiles()).map(file -> Path.of(file.getAbsolutePath())).toList());
        persistCallback.run();
    }

    private void removeTrack(MusicPlayerManager manager, Runnable persistCallback) {
        int selectedIndex = playlistList.getSelectedIndex();
        if (selectedIndex < 0) {
            statusLabel.setText("Select a track to remove.");
            return;
        }
        MusicTrack track = listModel.get(selectedIndex);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove '" + track.displayName + "' from the playlist?",
                "Remove track",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        manager.removeTrack(selectedIndex);
        persistCallback.run();
    }

    private void refreshPlaylist(MusicPlayerManager manager) {
        listModel.clear();
        for (MusicTrack track : manager.getPlaylist()) {
            listModel.addElement(track);
        }
        int currentIndex = manager.getCurrentIndex();
        if (currentIndex >= 0 && currentIndex < listModel.size()) {
            playlistList.setSelectedIndex(currentIndex);
        }
    }
}
