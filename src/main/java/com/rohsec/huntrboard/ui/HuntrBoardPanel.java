package com.rohsec.huntrboard.ui;

import burp.api.montoya.MontoyaApi;
import com.rohsec.huntrboard.model.AppState;
import com.rohsec.huntrboard.model.DashboardMetrics;
import com.rohsec.huntrboard.model.TrackerData;
import com.rohsec.huntrboard.persistence.StateRepository;
import com.rohsec.huntrboard.service.MusicPlayerManager;
import com.rohsec.huntrboard.service.TrackerService;
import com.rohsec.huntrboard.ui.charts.CompletionRingPanel;
import com.rohsec.huntrboard.ui.charts.MonthlyBarChartPanel;
import com.rohsec.huntrboard.ui.components.CardPanel;
import com.rohsec.huntrboard.ui.components.StatCardPanel;
import com.rohsec.huntrboard.ui.filters.DecimalDocumentFilter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class HuntrBoardPanel extends JPanel {
    private final MontoyaApi api;
    private final StateRepository stateRepository;
    private final AppState state;
    private final MusicPlayerManager musicPlayerManager;
    private final TrackerService trackerService = new TrackerService();
    private final ThemePalette palette;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

    private final JTextField yearlyTargetField = new JTextField();
    private final JTextField[] monthFields = new JTextField[12];
    private final JLabel trackerStatusLabel = new JLabel("Ready");
    private final JLabel appStatusLabel = new JLabel("HuntrBoard ready.");
    private final StatCardPanel achievedCard;
    private final StatCardPanel remainingCard;
    private final StatCardPanel completionCard;
    private final StatCardPanel averageCard;
    private final StatCardPanel performanceCard;
    private final MonthlyBarChartPanel monthlyBarChartPanel;
    private final CompletionRingPanel completionRingPanel;
    private final NotesPanel notesPanel;
    private final MusicPlayerPanel musicPlayerPanel;

    public HuntrBoardPanel(MontoyaApi api, StateRepository stateRepository, AppState state,
                           MusicPlayerManager musicPlayerManager) {
        super(new BorderLayout(0, 12));
        this.api = api;
        this.stateRepository = stateRepository;
        this.state = state;
        this.musicPlayerManager = musicPlayerManager;
        this.palette = ThemePalette.forTheme(api.userInterface().currentTheme());

        setOpaque(true);
        setBackground(palette.appBackground);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        achievedCard = new StatCardPanel(palette, "Achieved");
        remainingCard = new StatCardPanel(palette, "Remaining");
        completionCard = new StatCardPanel(palette, "Completion");
        averageCard = new StatCardPanel(palette, "Avg / Month");
        performanceCard = new StatCardPanel(palette, "Best / Worst");
        monthlyBarChartPanel = new MonthlyBarChartPanel(palette);
        completionRingPanel = new CompletionRingPanel(palette);
        notesPanel = new NotesPanel(palette, state.notes, state.activeNoteId, this::setStatus, () -> persistState("Notes saved."));
        musicPlayerPanel = new MusicPlayerPanel(palette, musicPlayerManager, () -> persistState("Playlist saved."), this::setStatus);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadTrackerFields();
        refreshDashboard(false);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JPanel heading = UiSupport.createTitleLabel("⌖", "HuntrBoard", palette, 24f);
        JLabel subtitle = new JLabel("A tidy bounty dashboard, notes pad, and focus player inside Burp Suite.");
        subtitle.setForeground(palette.textSecondary);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(3));
        titles.add(subtitle);

        header.add(titles, BorderLayout.WEST);
        header.add(UiSupport.createSignalBadge(palette), BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildBody() {
        JPanel dashboardColumn = new JPanel();
        dashboardColumn.setOpaque(false);
        dashboardColumn.setLayout(new BoxLayout(dashboardColumn, BoxLayout.Y_AXIS));
        dashboardColumn.add(buildStatsRow());
        dashboardColumn.add(Box.createVerticalStrut(12));
        dashboardColumn.add(buildTrackerCard());
        dashboardColumn.add(Box.createVerticalStrut(12));
        dashboardColumn.add(buildChartsRow());
        dashboardColumn.add(Box.createVerticalGlue());

        JScrollPane dashboardScroll = new JScrollPane(dashboardColumn);
        dashboardScroll.setBorder(null);
        dashboardScroll.setOpaque(false);
        dashboardScroll.getViewport().setOpaque(false);
        dashboardScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel sideColumn = new JPanel();
        sideColumn.setOpaque(false);
        sideColumn.setLayout(new BoxLayout(sideColumn, BoxLayout.Y_AXIS));
        sideColumn.add(notesPanel);
        sideColumn.add(Box.createVerticalStrut(12));
        sideColumn.add(musicPlayerPanel);

        JScrollPane sideScroll = new JScrollPane(sideColumn);
        sideScroll.setBorder(null);
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dashboardScroll, sideScroll);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0.66d);
        splitPane.setDividerLocation(900);
        splitPane.setDividerSize(8);
        return splitPane;
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.add(achievedCard);
        row.add(remainingCard);
        row.add(completionCard);
        row.add(averageCard);
        row.add(performanceCard);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return row;
    }

    private CardPanel buildTrackerCard() {
        CardPanel card = new CardPanel(palette);
        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Yearly Target Tracker");
        heading.setForeground(palette.textPrimary);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        JLabel subtitle = new JLabel("Set a yearly target, edit months, and watch metrics refresh instantly.");
        subtitle.setForeground(palette.textSecondary);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        styleField(yearlyTargetField);
        yearlyTargetField.setColumns(10);
        ((AbstractDocument) yearlyTargetField.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        JButton saveButton = UiSupport.createAccentButton("⭳", palette);
        saveButton.setToolTipText("Save tracker");
        saveButton.addActionListener(event -> persistState("Tracker saved."));
        actions.add(labeled("Year target", yearlyTargetField), BorderLayout.CENTER);
        actions.add(saveButton, BorderLayout.EAST);

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(4, 3, 12, 12));
        grid.setOpaque(false);
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshDashboard(false); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshDashboard(false); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshDashboard(false); }
        };

        for (int index = 0; index < monthFields.length; index++) {
            JTextField field = new JTextField();
            styleField(field);
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
            field.getDocument().addDocumentListener(listener);
            monthFields[index] = field;
            grid.add(labeled(TrackerService.MONTHS[index], field));
        }
        yearlyTargetField.getDocument().addDocumentListener(listener);

        trackerStatusLabel.setForeground(palette.textSecondary);
        trackerStatusLabel.setFont(trackerStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        container.add(header, BorderLayout.NORTH);
        container.add(grid, BorderLayout.CENTER);
        container.add(trackerStatusLabel, BorderLayout.SOUTH);
        card.add(container, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);

        CardPanel monthlyCard = new CardPanel(palette);
        monthlyCard.setPreferredSize(new Dimension(320, 260));
        monthlyCard.add(monthlyBarChartPanel, BorderLayout.CENTER);

        CardPanel completionCard = new CardPanel(palette);
        completionCard.setPreferredSize(new Dimension(280, 260));
        completionCard.add(completionRingPanel, BorderLayout.CENTER);

        row.add(monthlyCard);
        row.add(completionCard);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        return row;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        appStatusLabel.setForeground(palette.textSecondary);
        appStatusLabel.setFont(appStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        footer.add(appStatusLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel credit = new JLabel("Developed by");
        credit.setForeground(palette.textSecondary);
        credit.setFont(credit.getFont().deriveFont(Font.PLAIN, 11f));
        JButton rohitLink = UiSupport.createLinkButton("ROHIT", palette);
        rohitLink.addActionListener(event -> UiSupport.openLink("https://x.com/rohsec"));
        JButton coffeeButton = UiSupport.createAccentButton("Buy me a coffee", palette);
        coffeeButton.addActionListener(event -> UiSupport.openLink("https://ko-fi.com/rohsec"));
        coffeeButton.setFont(coffeeButton.getFont().deriveFont(Font.BOLD, 11f));
        right.add(credit);
        right.add(rohitLink);
        right.add(coffeeButton);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private JPanel labeled(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setForeground(palette.textSecondary);
        caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(caption, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void styleField(JTextField field) {
        field.setBackground(palette.fieldBackground);
        field.setForeground(palette.textPrimary);
        field.setCaretColor(palette.textPrimary);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.fieldBorder, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void loadTrackerFields() {
        TrackerData tracker = state.tracker;
        yearlyTargetField.setText(formatEditable(tracker.yearlyTarget));
        for (int index = 0; index < monthFields.length; index++) {
            monthFields[index].setText(formatEditable(tracker.monthlyValues.get(index)));
        }
    }

    private String formatEditable(double value) {
        return value == 0.0d ? "" : String.format(Locale.US, "%.2f", value).replaceAll("\\.00$", "");
    }

    private double parseAmount(String text) {
        if (text == null || text.isBlank() || ".".equals(text)) {
            return 0.0d;
        }
        return Math.max(0.0d, Double.parseDouble(text));
    }

    private TrackerData collectTracker() {
        TrackerData tracker = new TrackerData();
        tracker.yearlyTarget = parseAmount(yearlyTargetField.getText());
        tracker.monthlyValues = new ArrayList<>();
        for (JTextField field : monthFields) {
            tracker.monthlyValues.add(parseAmount(field.getText()));
        }
        tracker.ensureMonthCount();
        return tracker;
    }

    private void refreshDashboard(boolean persist) {
        try {
            state.tracker = collectTracker();
            DashboardMetrics metrics = trackerService.calculate(state.tracker);
            achievedCard.setValue(currencyFormatter.format(metrics.totalAchieved));
            achievedCard.setSubtitle("From 12 monthly entries");
            remainingCard.setValue(currencyFormatter.format(metrics.remainingTarget));
            remainingCard.setSubtitle(state.tracker.yearlyTarget <= 0 ? "Set a target to track burn-down" : "Target minus achieved");
            completionCard.setValue(String.format(Locale.US, "%.1f%%", metrics.percentComplete));
            completionCard.setSubtitle(state.tracker.yearlyTarget <= 0 ? "No target defined yet" : currencyFormatter.format(state.tracker.yearlyTarget) + " annual target");
            averageCard.setValue(currencyFormatter.format(metrics.averagePerMonth));
            averageCard.setSubtitle("Average across all 12 months");
            performanceCard.setValue(TrackerService.MONTHS[metrics.bestMonthIndex]);
            performanceCard.setSubtitle("Worst: " + TrackerService.MONTHS[metrics.worstMonthIndex]);

            monthlyBarChartPanel.setValues(state.tracker.monthlyValues);
            completionRingPanel.setProgress(metrics.percentComplete,
                    state.tracker.yearlyTarget <= 0 ? "Add a yearly target to unlock the ring" :
                            currencyFormatter.format(metrics.totalAchieved) + " / " + currencyFormatter.format(state.tracker.yearlyTarget));
            trackerStatusLabel.setText(metrics.percentComplete >= 100.0d
                    ? "Target crushed — you are over the line."
                    : "Totals refresh as you type. Use the save icon to persist.");
            if (persist) {
                persistState("Tracker saved.");
            }
        } catch (NumberFormatException exception) {
            trackerStatusLabel.setText("Only numbers with up to 2 decimals are allowed.");
        }
    }

    private void setStatus(String message) {
        appStatusLabel.setText(message);
    }

    private void persistState(String status) {
        state.tracker = collectTracker();
        notesPanel.flushCurrentEditor();
        state.notes = notesPanel.getNotes();
        state.activeNoteId = notesPanel.getActiveNoteId();
        state.playlist = musicPlayerManager.getPlaylist();
        state.currentTrackIndex = musicPlayerManager.getCurrentIndex();
        state.playerVolume = musicPlayerManager.getVolume();
        stateRepository.save(state);
        setStatus(status);
    }

    public void shutdown() {
        persistState("HuntrBoard state persisted.");
    }
}
