package com.danz.autocrop;

/** Unsaved screen values are isolated from the live farming configuration. */
final class ConfigEditor {
    final AutoCropConfig values = new AutoCropConfig();
    AutoCropConfig.SetupPreset preset;
    boolean enabled;
    boolean manual;

    ConfigEditor(AutoCropConfig current) {
        values.copyFrom(current);
        AutoCropConfig.SetupPreset matching = current.matchingPreset();
        preset = matching != null ? matching : current.setupPreset;
        manual = current.manualTuning || matching == null;
        enabled = current.harvestMode != AutoCropConfig.HarvestMode.DISABLED;
        if (!enabled) values.harvestMode = current.resumeMode;
    }

    void saveInto(AutoCropConfig target) {
        AutoCropConfig result = new AutoCropConfig();
        result.copyFrom(values);
        if (!manual) result.applyPreset(preset);
        result.setupPreset = preset;
        result.manualTuning = manual;
        result.resumeMode = result.harvestMode;
        if (!enabled) result.harvestMode = AutoCropConfig.HarvestMode.DISABLED;
        result.validatePostLoad();
        target.copyFrom(result);
    }
}
