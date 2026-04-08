package com.rohsec.huntrboard.ui;

import com.rohsec.huntrboard.model.MusicTrack;
import com.rohsec.huntrboard.model.RadioStation;
import com.rohsec.huntrboard.service.MusicPlayerManager;
import com.rohsec.huntrboard.ui.components.CardPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MusicPlayerPanel extends CardPanel {
    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> playlistList = new JList<>(listModel);
    private final JLabel trackLabel = new JLabel("No radio stream selected");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton playPauseButton;
    private final JComboBox<String> sourceSelector;
    private final JTextField stationNameField = new JTextField();
    private final JTextField stationUrlField = new JTextField();
    private final JPanel radioFormPanel = new JPanel(new GridBagLayout());

    public MusicPlayerPanel(ThemePalette palette, MusicPlayerManager manager,
                            Runnable persistCallback, Consumer<String> statusCallback) {
        super(palette);
        setLayout(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(420, 330));
        setMinimumSize(new Dimension(320, 300));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
        this.playPauseButton = UiSupport.createAccentButton("⏵", palette);
        playPauseButton.setToolTipText("Play or pause");
        this.sourceSelector = new JComboBox<>(new String[]{"Radio Streams", "Local MP3 Files"});
        sourceSelector.setSelectedItem(MusicPlayerManager.MODE_LOCAL.equals(manager.getAudioSourceMode()) ? "Local MP3 Files" : "Radio Streams");

        add(buildHeader(palette, manager, persistCallback), BorderLayout.NORTH);
        add(buildCenter(palette), BorderLayout.CENTER);
        add(buildFooter(palette, manager, persistCallback, statusCallback), BorderLayout.SOUTH);

        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistList.setVisibleRowCount(6);
        playlistList.setBackground(palette.fieldBackground);
        playlistList.setForeground(palette.textPrimary);
        playlistList.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));
        playlistList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setText(value == null ? "" : value.toString());
                return label;
            }
        });
        playlistList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    playSelected(manager);
                }
            }
        });

        styleField(stationNameField, palette);
        styleField(stationUrlField, palette);
        configureRadioForm(palette);

        sourceSelector.addActionListener(event -> {
            manager.setAudioSourceMode(sourceSelector.getSelectedIndex() == 0 ? MusicPlayerManager.MODE_RADIO : MusicPlayerManager.MODE_LOCAL);
            refreshVisibleSource(manager);
            persistCallback.run();
        });

        manager.setPlaylistListener(() -> refreshVisibleSource(manager));
        manager.setCurrentTrackListener(trackLabel::setText);
        manager.setPlaybackStateListener(this::updatePlayPauseButton);
        manager.setStatusListener(status -> {
            statusLabel.setText(status);
            statusCallback.accept(status);
        });
        refreshVisibleSource(manager);
        updatePlayPauseButton(manager.isPlaying());
    }

    private JPanel buildHeader(ThemePalette palette, MusicPlayerManager manager, Runnable persistCallback) {
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JPanel heading = UiSupport.createTitleLabel("♫", "JukeBox", palette, 15f);
        JLabel subtitle = new JLabel("Switch between internet radio streams and local MP3 sessions.");
        subtitle.setForeground(palette.textSecondary);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(3));
        titles.add(subtitle);

        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        switcher.setOpaque(false);
        JLabel modeLabel = new JLabel("Source");
        modeLabel.setForeground(palette.textSecondary);
        sourceSelector.setFocusable(false);
        switcher.add(modeLabel);
        switcher.add(sourceSelector);

        header.add(titles, BorderLayout.NORTH);
        header.add(switcher, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildCenter(ThemePalette palette) {
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);

        JLabel current = new JLabel("Now queued");
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
        info.add(Box.createVerticalStrut(6));
        info.add(radioFormPanel);

        JScrollPane scroll = new JScrollPane(playlistList);
        scroll.setBorder(BorderFactory.createLineBorder(palette.fieldBorder, 1, true));
        scroll.setPreferredSize(new Dimension(240, 108));
        scroll.setMinimumSize(new Dimension(220, 92));

        center.add(info, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFooter(ThemePalette palette, MusicPlayerManager manager,
                               Runnable persistCallback, Consumer<String> statusCallback) {
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);

        JPanel actionRow = new JPanel(new GridBagLayout());
        actionRow.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 0, 6);
        constraints.anchor = GridBagConstraints.WEST;

        JButton addButton = UiSupport.createIconButton("＋", "Add item", palette);
        JButton removeButton = UiSupport.createIconButton("－", "Remove selected item", palette);
        JButton previousButton = UiSupport.createIconButton("⏮", "Previous", palette);
        JButton shuffleButton = UiSupport.createIconButton("⤮", "Shuffle queue", palette);
        JButton nextButton = UiSupport.createIconButton("⏭", "Next", palette);

        addButton.addActionListener(event -> {
            if (MusicPlayerManager.MODE_RADIO.equals(manager.getAudioSourceMode())) {
                manager.addOrUpdateRadioStation(stationNameField.getText(), stationUrlField.getText());
                refreshVisibleSource(manager);
                persistCallback.run();
            } else {
                addTracks(manager, persistCallback);
            }
        });
        removeButton.addActionListener(event -> {
            int selectedIndex = playlistList.getSelectedIndex();
            if (selectedIndex < 0) {
                statusLabel.setText("Select an item to remove.");
                return;
            }
            removeSelected(manager, persistCallback, selectedIndex);
        });
        previousButton.addActionListener(event -> manager.previous());
        shuffleButton.addActionListener(event -> {
            manager.shuffle();
            persistCallback.run();
        });
        playPauseButton.addActionListener(event -> manager.togglePlayPause(playlistList.getSelectedIndex()));
        nextButton.addActionListener(event -> manager.next());

        JButton[] buttons = {addButton, removeButton, previousButton, shuffleButton, playPauseButton, nextButton};
        for (int index = 0; index < buttons.length; index++) {
            constraints.gridx = index;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            actionRow.add(buttons[index], constraints);
        }

        statusLabel.setForeground(palette.textSecondary);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        constraints.gridx = buttons.length;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 8, 0, 0);
        actionRow.add(statusLabel, constraints);

        JPanel volumeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        volumeRow.setOpaque(false);
        JLabel volumeLabel = new JLabel("Volume");
        volumeLabel.setForeground(palette.textSecondary);
        JSlider volumeSlider = new JSlider(0, 100, (int) Math.round(manager.getVolume() * 100.0d));
        volumeSlider.setOpaque(false);
        volumeSlider.setPreferredSize(new Dimension(96, 18));
        volumeSlider.setMaximumSize(new Dimension(96, 18));
        volumeSlider.addChangeListener(event -> {
            manager.setVolume(volumeSlider.getValue() / 100.0d);
            persistCallback.run();
        });
        volumeRow.add(volumeLabel);
        volumeRow.add(volumeSlider);

        footer.add(actionRow, BorderLayout.NORTH);
        footer.add(volumeRow, BorderLayout.CENTER);
        return footer;
    }

    private void configureRadioForm(ThemePalette palette) {
        radioFormPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        JLabel nameLabel = new JLabel("Name");
        nameLabel.setForeground(palette.textSecondary);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel urlLabel = new JLabel("URL");
        urlLabel.setForeground(palette.textSecondary);
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.PLAIN, 11f));

        gbc.gridx = 0;
        gbc.weightx = 0;
        radioFormPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        radioFormPanel.add(stationNameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        radioFormPanel.add(urlLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        radioFormPanel.add(stationUrlField, gbc);
    }

    private void styleField(JTextField field, ThemePalette palette) {
        field.setBackground(palette.fieldBackground);
        field.setForeground(palette.textPrimary);
        field.setCaretColor(palette.textPrimary);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.fieldBorder, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private void refreshVisibleSource(MusicPlayerManager manager) {
        boolean radioMode = MusicPlayerManager.MODE_RADIO.equals(manager.getAudioSourceMode());
        radioFormPanel.setVisible(radioMode);
        listModel.clear();
        if (radioMode) {
            List<RadioStation> stations = manager.getRadioStations();
            for (RadioStation station : stations) {
                listModel.addElement(station);
            }
            int current = manager.getCurrentRadioIndex();
            if (current >= 0 && current < listModel.size()) {
                playlistList.setSelectedIndex(current);
                Object selected = listModel.get(current);
                if (selected instanceof RadioStation station) {
                    stationNameField.setText(station.name == null ? "" : station.name);
                    stationUrlField.setText(station.url == null ? "" : station.url);
                }
            } else {
                stationNameField.setText("");
                stationUrlField.setText("");
            }
        } else {
            List<MusicTrack> tracks = manager.getPlaylist();
            for (MusicTrack track : tracks) {
                listModel.addElement(track);
            }
            int current = manager.getCurrentIndex();
            if (current >= 0 && current < listModel.size()) {
                playlistList.setSelectedIndex(current);
            }
            stationNameField.setText("");
            stationUrlField.setText("");
        }
        playlistList.revalidate();
        playlistList.repaint();
    }

    private void playSelected(MusicPlayerManager manager) {
        int selectedIndex = playlistList.getSelectedIndex();
        if (MusicPlayerManager.MODE_RADIO.equals(manager.getAudioSourceMode())) {
            manager.playRadioAt(selectedIndex >= 0 ? selectedIndex : manager.getCurrentRadioIndex());
        } else {
            manager.playAt(selectedIndex >= 0 ? selectedIndex : manager.getCurrentIndex());
        }
    }

    private void removeSelected(MusicPlayerManager manager, Runnable persistCallback, int selectedIndex) {
        Object selected = listModel.get(selectedIndex);
        String label = selected == null ? "selected item" : selected.toString();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove '" + label + "'?",
                "Remove item",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        manager.removeTrack(selectedIndex);
        refreshVisibleSource(manager);
        persistCallback.run();
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
        refreshVisibleSource(manager);
        persistCallback.run();
    }
}
