package com.danz.autocrop;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.danz.autocrop.AutoCropConfig.HarvestMode.*;
import static com.danz.autocrop.AutoCropConfig.SetupPreset.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigEditorTest {
    @Test
    void theHarvestSelectorOffersExactlyMaxAndServer() {
        assertEquals(2, AutoCropConfig.SetupPreset.values().length);
        assertNotNull(MAX);
        assertNotNull(SERVER);
    }

    @Test
    void newPlayersStartWithMaxAndManualTuningOff() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        assertEquals(MAX, editor.preset);
        assertFalse(editor.manual);
        assertEquals(MAX, config.matchingPreset());
        assertFalse(config.manualTuning);
        assertEquals(HARVEST_RISKY, config.harvestMode);
    }

    @Test
    void serverUsesSmallAutomaticBatchesAndLongerDelaysThanMax() {
        var server = new AutoCropConfig();
        server.applyPreset(SERVER);
        var max = new AutoCropConfig();
        max.applyPreset(MAX);
        assertEquals(HARVEST_RISKY, server.harvestMode);
        assertEquals(1, server.riskBatchSize);
        assertTrue(server.replantDelayTicks > max.replantDelayTicks);
        assertTrue(server.auraCooldownTicks > max.auraCooldownTicks);
        assertFalse(server.fillEmptyFarmland);
    }

    @Test
    void maxUsesFastestSupportedTimingAndLargestBatch() {
        var config = new AutoCropConfig();
        config.applyPreset(MAX);
        assertEquals(HARVEST_RISKY, config.harvestMode);
        assertEquals(0, config.replantDelayTicks);
        assertEquals(1, config.auraCooldownTicks);
        assertEquals(32, config.riskBatchSize);
    }

    @ParameterizedTest
    @EnumSource(AutoCropConfig.SetupPreset.class)
    void presetsAreRecognizedAndNeverEnableUnrelatedPlantingOrOverrideSeedRefill(AutoCropConfig.SetupPreset preset) {
        var config = new AutoCropConfig();
        config.autoRefillSeeds = false;
        config.fillEmptyFarmland = true;
        config.applyPreset(preset);
        assertEquals(preset, config.matchingPreset());
        assertFalse(config.autoRefillSeeds);
        assertFalse(config.fillEmptyFarmland);
    }

    @Test
    void editingOrCancellingDoesNotChangeLiveConfiguration() {
        var config = new AutoCropConfig();
        String before = new Gson().toJson(config);
        var editor = new ConfigEditor(config);
        editor.preset = MAX;
        editor.manual = true;
        editor.values.riskBatchSize = 7;
        assertEquals(before, new Gson().toJson(config));
    }

    @Test
    void savingPresetIgnoresManualValuesWhenManualTuningIsOff() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        editor.preset = MAX;
        editor.values.riskBatchSize = 1;
        editor.values.autoRefillSeeds = false;
        editor.saveInto(config);
        assertEquals(MAX, config.matchingPreset());
        assertFalse(config.manualTuning);
        assertFalse(config.autoRefillSeeds);
    }

    @Test
    void manualTuningOverridesPresetAndValidatesValues() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        editor.preset = MAX;
        editor.manual = true;
        editor.values.riskBatchSize = 7;
        editor.values.replantDelayTicks = -5;
        editor.values.fillEmptyFarmland = true;
        editor.saveInto(config);
        assertTrue(config.manualTuning);
        assertEquals(MAX, config.setupPreset);
        assertEquals(7, config.riskBatchSize);
        assertEquals(0, config.replantDelayTicks);
        assertTrue(config.fillEmptyFarmland);
    }

    @Test
    void manualMenuStaysEnabledWhenItsValuesMatchAPreset() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        editor.manual = true;
        editor.saveInto(config);
        assertTrue(new ConfigEditor(config).manual);
    }

    @Test
    void switchingManualOffRestoresTheSelectedPreset() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        editor.manual = true;
        editor.preset = MAX;
        editor.values.riskBatchSize = 7;
        editor.saveInto(config);
        var reopened = new ConfigEditor(config);
        assertEquals(MAX, reopened.preset);
        reopened.manual = false;
        reopened.saveInto(config);
        assertEquals(32, config.riskBatchSize);
        assertFalse(config.manualTuning);
    }

    @Test
    void disablingAndEnablingRetainsThePreset() {
        var config = new AutoCropConfig();
        var editor = new ConfigEditor(config);
        editor.preset = MAX;
        editor.enabled = false;
        editor.saveInto(config);
        assertEquals(DISABLED, config.harvestMode);
        var reopened = new ConfigEditor(config);
        assertEquals(MAX, reopened.preset);
        reopened.enabled = true;
        reopened.saveInto(config);
        assertEquals(HARVEST_RISKY, config.harvestMode);
    }

    @Test
    void disablingManualReplantingDoesNotAccidentallyEnableAutomaticHarvesting() {
        var config = new AutoCropConfig();
        config.applyPreset(MAX);
        config.harvestMode = MANUAL;
        var editor = new ConfigEditor(config);
        assertTrue(editor.manual);
        editor.enabled = false;
        editor.saveInto(config);
        var reopened = new ConfigEditor(config);
        assertTrue(reopened.manual);
        reopened.enabled = true;
        reopened.saveInto(config);
        assertEquals(MANUAL, config.harvestMode);
    }

    @Test
    void existingCustomConfigurationsOpenTheManualMenuWithoutOverwritingValues() {
        var config = new AutoCropConfig();
        config.replantDelayTicks = 7;
        config.riskBatchSize = 11;
        var editor = new ConfigEditor(config);
        assertTrue(editor.manual);
        editor.saveInto(config);
        assertEquals(7, config.replantDelayTicks);
        assertEquals(11, config.riskBatchSize);
    }
}
