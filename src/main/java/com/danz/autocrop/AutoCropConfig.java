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

    public HarvestMode harvestMode = HarvestMode.MANUAL;

    public int replantDelayTicks = 3;

    public int auraCooldownTicks = 5;

    public int riskBatchSize = 5;

    public boolean autoRefillSeeds = true;

    public boolean rememberMissedReplants = true;

    public int missedReplantRetryCooldownTicks = 20;

    // Existing config files also default to leaving unrelated empty plots alone.
    public boolean fillEmptyFarmland = false;

    @Override
    public void validatePostLoad() {
        if (harvestMode == null) harvestMode = HarvestMode.MANUAL;
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
