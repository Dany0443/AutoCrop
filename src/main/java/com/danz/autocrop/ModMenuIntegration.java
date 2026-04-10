package com.danz.autocrop;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::buildScreen;
    }

    private static Screen buildScreen(Screen parent) {
        AutoCropConfig cfg    = AutoCropConfig.get();
        var            holder = AutoConfig.getConfigHolder(AutoCropConfig.class);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.autocrop.title"))
            .setSavingRunnable(holder::save);

        ConfigEntryBuilder eb  = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
            Component.translatable("config.autocrop.category.general")
        );

        general.addEntry(
            eb.startEnumSelector(
                    Component.translatable("config.autocrop.option.harvestMode"),
                    AutoCropConfig.HarvestMode.class,
                    cfg.harvestMode
                )
                .setDefaultValue(AutoCropConfig.HarvestMode.MANUAL)
                .setTooltip(Component.translatable("config.autocrop.option.harvestMode.tooltip"))
                .setSaveConsumer(v -> cfg.harvestMode = v)
                .build()
        );

        general.addEntry(
            eb.startIntSlider(
                    Component.translatable("config.autocrop.option.replantDelayTicks"),
                    cfg.replantDelayTicks, 0, 10
                )
                .setDefaultValue(3)
                .setTooltip(Component.translatable("config.autocrop.option.replantDelayTicks.tooltip"))
                .setSaveConsumer(v -> cfg.replantDelayTicks = v)
                .build()
        );

        general.addEntry(
            eb.startBooleanToggle(
                    Component.translatable("config.autocrop.option.autoRefillSeeds"),
                    cfg.autoRefillSeeds
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.autocrop.option.autoRefillSeeds.tooltip"))
                .setSaveConsumer(v -> cfg.autoRefillSeeds = v)
                .build()
        );

        ConfigCategory risky = builder.getOrCreateCategory(
            Component.literal("⚠ Harvest (Risky) Settings")
        );

        risky.addEntry(
            eb.startTextDescription(
                Component.literal(
                    "§eThese options only apply in HARVEST_RISKY mode.\n" +
                    "§cDo NOT use Harvest (Risky) on servers with anti-cheat " +
                    "(e.g. GrimAC, Vulcan). Use only on private SMPs or your own server."
                )
            ).build()
        );

        risky.addEntry(
            eb.startIntSlider(
                    Component.translatable("config.autocrop.option.auraCooldownTicks"),
                    cfg.auraCooldownTicks, 1, 20
                )
                .setDefaultValue(5)
                .setTooltip(Component.translatable("config.autocrop.option.auraCooldownTicks.tooltip"))
                .setSaveConsumer(v -> cfg.auraCooldownTicks = v)
                .build()
        );

        risky.addEntry(
            eb.startIntSlider(
                    Component.translatable("config.autocrop.option.riskBatchSize"),
                    cfg.riskBatchSize, 1, 32
                )
                .setDefaultValue(5)
                .setTooltip(Component.translatable("config.autocrop.option.riskBatchSize.tooltip"))
                .setSaveConsumer(v -> cfg.riskBatchSize = v)
                .build()
        );

        risky.addEntry(
            eb.startBooleanToggle(
                    Component.translatable("config.autocrop.option.rememberMissedReplants"),
                    cfg.rememberMissedReplants
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.autocrop.option.rememberMissedReplants.tooltip"))
                .setSaveConsumer(v -> cfg.rememberMissedReplants = v)
                .build()
        );

        return builder.build();
    }
}