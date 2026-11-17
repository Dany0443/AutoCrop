package com.danz.autocrop;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = AutoCropMod.MOD_ID)
public class AutoCropConfig implements ConfigData {

    public enum HarvestMode {
        DISABLED,
        MANUAL,
        HARVEST_RISKY;

        public String translationKey() {
            return switch (this) {
                case DISABLED      -> "autocrop.mode.disabled";
                case MANUAL        -> "autocrop.mode.manual";
                case HARVEST_RISKY -> "autocrop.mode.harvest_risky";
            };
        }

        public HarvestMode next() {
            HarvestMode[] values = HarvestMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public enum SetupPreset {
        SERVER, MAX;

        public String translationKey() {
            return "config.autocrop.preset." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // New installations use the slower automatic Server preset.
    public HarvestMode harvestMode = HarvestMode.HARVEST_RISKY;
    public HarvestMode resumeMode = HarvestMode.HARVEST_RISKY;
    public SetupPreset setupPreset = SetupPreset.SERVER;
    public boolean manualTuning = false;
    public int replantDelayTicks = 5;
    public int auraCooldownTicks = 20;
    public int riskBatchSize = 1;
    public boolean autoRefillSeeds = true;
    public boolean rememberMissedReplants = true;
    public int missedReplantRetryCooldownTicks = 40;
    public boolean fillEmptyFarmland = false;

    public void applyPreset(SetupPreset preset) {
        switch (preset) {
            case SERVER -> {
                harvestMode = HarvestMode.HARVEST_RISKY;
                replantDelayTicks = 5;
                auraCooldownTicks = 20;
                riskBatchSize = 1;
                rememberMissedReplants = true;
                missedReplantRetryCooldownTicks = 40;
            }
            case MAX -> {
                harvestMode = HarvestMode.HARVEST_RISKY;
                replantDelayTicks = 0;
                auraCooldownTicks = 1;
                riskBatchSize = 32;
                rememberMissedReplants = true;
                missedReplantRetryCooldownTicks = 5;
            }
        }
        fillEmptyFarmland = false;
        resumeMode = harvestMode;
        setupPreset = preset;
    }

    public SetupPreset matchingPreset() {
        HarvestMode effectiveMode = harvestMode == HarvestMode.DISABLED ? resumeMode : harvestMode;
        for (SetupPreset preset : SetupPreset.values()) {
            AutoCropConfig expected = new AutoCropConfig();
            expected.applyPreset(preset);
            if (effectiveMode == expected.harvestMode
                && replantDelayTicks == expected.replantDelayTicks
                && auraCooldownTicks == expected.auraCooldownTicks
                && riskBatchSize == expected.riskBatchSize
                && rememberMissedReplants == expected.rememberMissedReplants
                && missedReplantRetryCooldownTicks == expected.missedReplantRetryCooldownTicks
                && fillEmptyFarmland == expected.fillEmptyFarmland) {
                return preset;
            }
        }
        return null;
    }

    void copyFrom(AutoCropConfig source) {
        harvestMode = source.harvestMode;
        resumeMode = source.resumeMode;
        setupPreset = source.setupPreset;
        manualTuning = source.manualTuning;
        replantDelayTicks = source.replantDelayTicks;
        auraCooldownTicks = source.auraCooldownTicks;
        riskBatchSize = source.riskBatchSize;
        autoRefillSeeds = source.autoRefillSeeds;
        rememberMissedReplants = source.rememberMissedReplants;
        missedReplantRetryCooldownTicks = source.missedReplantRetryCooldownTicks;
        fillEmptyFarmland = source.fillEmptyFarmland;
    }

    @Override
    public void validatePostLoad() {
        if (harvestMode == null) harvestMode = HarvestMode.MANUAL;
        if (setupPreset == null) setupPreset = SetupPreset.SERVER;
        if (resumeMode == null || resumeMode == HarvestMode.DISABLED) resumeMode = HarvestMode.MANUAL;
        replantDelayTicks = Math.clamp(replantDelayTicks, 0, 10);
        auraCooldownTicks = Math.clamp(auraCooldownTicks, 1, 20);
        riskBatchSize = Math.clamp(riskBatchSize, 1, 32);
        missedReplantRetryCooldownTicks = Math.clamp(missedReplantRetryCooldownTicks, 1, 200);
    }

    public static void register() {
        AutoConfig.register(AutoCropConfig.class, GsonConfigSerializer::new);
    }

    public static AutoCropConfig get() {
        return AutoConfig.getConfigHolder(AutoCropConfig.class).getConfig();
    }
}
