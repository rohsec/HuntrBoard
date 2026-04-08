package com.rohsec.huntrboard;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.rohsec.huntrboard.model.AppState;
import com.rohsec.huntrboard.persistence.StateRepository;
import com.rohsec.huntrboard.service.MusicPlayerManager;
import com.rohsec.huntrboard.ui.HuntrBoardPanel;

public class HuntrBoardExtension implements BurpExtension {
    private MusicPlayerManager musicPlayerManager;
    private HuntrBoardPanel panel;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        montoyaApi.extension().setName("HuntrBoard");

        StateRepository stateRepository = new StateRepository(montoyaApi.persistence(), montoyaApi.logging());
        AppState state = stateRepository.load();

        musicPlayerManager = new MusicPlayerManager(montoyaApi.logging());
        musicPlayerManager.setPlaylist(state.playlist);
        musicPlayerManager.setRadioStations(state.radioStations);
        musicPlayerManager.setAudioSourceMode(state.audioSourceMode);
        musicPlayerManager.setCurrentIndex(state.currentTrackIndex);
        musicPlayerManager.setCurrentRadioIndex(state.currentRadioStationIndex);
        musicPlayerManager.setVolume(state.playerVolume);

        panel = new HuntrBoardPanel(montoyaApi, stateRepository, state, musicPlayerManager);
        montoyaApi.userInterface().registerSuiteTab("HuntrBoard", panel);
        montoyaApi.extension().registerUnloadingHandler(() -> {
            if (panel != null) {
                panel.shutdown();
            }
            if (musicPlayerManager != null) {
                musicPlayerManager.close();
            }
        });

        montoyaApi.logging().logToOutput("HuntrBoard loaded successfully.");
    }
}
