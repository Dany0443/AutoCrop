package com.danz.autocrop;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoCropConfigTest {
    @Test
    void oldConfigurationKeepsItsValuesAndDoesNotEnableEmptyPlotPlanting() {
        AutoCropConfig config = new Gson().fromJson(
            "{\"harvestMode\":\"MANUAL\",\"replantDelayTicks\":7,\"autoRefillSeeds\":false}",
            AutoCropConfig.class);
        assertEquals(AutoCropConfig.HarvestMode.MANUAL, config.harvestMode);
        assertEquals(7, config.replantDelayTicks);
        assertFalse(config.autoRefillSeeds);
        assertFalse(config.fillEmptyFarmland);
    }

    @Test
    void fileValuesBelowSliderRangesAreClamped() throws Exception {
        AutoCropConfig config = new AutoCropConfig();
        config.replantDelayTicks = -1;
        config.auraCooldownTicks = -1;
        config.riskBatchSize = 0;
        config.missedReplantRetryCooldownTicks = -1;
        config.validatePostLoad();
        assertAll(
            () -> assertEquals(0, config.replantDelayTicks),
            () -> assertEquals(1, config.auraCooldownTicks),
            () -> assertEquals(1, config.riskBatchSize),
            () -> assertEquals(1, config.missedReplantRetryCooldownTicks));
    }

    @Test
    void fileValuesAboveSliderRangesAreClamped() throws Exception {
        AutoCropConfig config = new AutoCropConfig();
        config.replantDelayTicks = Integer.MAX_VALUE;
        config.auraCooldownTicks = Integer.MAX_VALUE;
        config.riskBatchSize = Integer.MAX_VALUE;
        config.missedReplantRetryCooldownTicks = Integer.MAX_VALUE;
        config.validatePostLoad();
        assertAll(
            () -> assertEquals(10, config.replantDelayTicks),
            () -> assertEquals(20, config.auraCooldownTicks),
            () -> assertEquals(32, config.riskBatchSize),
            () -> assertEquals(200, config.missedReplantRetryCooldownTicks));
    }

    @Test
    void unknownModeInAConfigFileFallsBackToManual() throws Exception {
        AutoCropConfig config = new Gson().fromJson("{\"harvestMode\":\"UNKNOWN\"}", AutoCropConfig.class);
        config.validatePostLoad();
        assertEquals(AutoCropConfig.HarvestMode.MANUAL, config.harvestMode);
    }
    @Test
    void serverPresetHasShorterWaitsButStillOnlyBreaksOneCropAtATime() {
        AutoCropConfig config = new AutoCropConfig();
        config.applyPreset(AutoCropConfig.SetupPreset.SERVER);
        assertEquals(3, config.replantDelayTicks);
        assertEquals(10, config.auraCooldownTicks);
        assertEquals(20, config.missedReplantRetryCooldownTicks);
        assertEquals(1, config.riskBatchSize);
    }

    @Test
    void oldUntunedServerPresetGetsFasterWithoutReenablingDisabledMod() {
        AutoCropConfig config = oldServerConfig();
        config.harvestMode = AutoCropConfig.HarvestMode.DISABLED;
        config.validatePostLoad();
        assertEquals(AutoCropConfig.HarvestMode.DISABLED, config.harvestMode);
        assertEquals(AutoCropConfig.SetupPreset.SERVER, config.matchingPreset());
        assertEquals(10, config.auraCooldownTicks);
    }

    @Test
    void oldServerTimingsArePreservedWhenManualTuningIsOn() {
        AutoCropConfig config = oldServerConfig();
        config.manualTuning = true;
        config.validatePostLoad();
        assertEquals(20, config.auraCooldownTicks);
        assertEquals(5, config.replantDelayTicks);
        assertEquals(40, config.missedReplantRetryCooldownTicks);
    }

    private AutoCropConfig oldServerConfig() {
        AutoCropConfig config = new AutoCropConfig();
        config.replantDelayTicks = 5;
        config.auraCooldownTicks = 20;
        config.missedReplantRetryCooldownTicks = 40;
        return config;
    }

}
