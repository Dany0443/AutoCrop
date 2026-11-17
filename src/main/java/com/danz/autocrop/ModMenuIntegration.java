package com.danz.autocrop;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::buildScreen;
    }

    private static Component text(String key) {
        return Component.translatable("config.autocrop." + key);
    }

    static Screen buildScreen(Screen parent) {
        AutoCropConfig current = AutoCropConfig.get();
        var holder = AutoConfig.getConfigHolder(AutoCropConfig.class);
        ConfigEditor editor = new ConfigEditor(current);
        AutoCropConfig values = editor.values;
        AutoCropConfig defaults = new AutoCropConfig();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(text("title"))
            .setSavingRunnable(() -> {
                editor.saveInto(current);
                holder.save();
            });
        var entries = builder.entryBuilder();
        var setup = builder.getOrCreateCategory(text("category.setup"));

        setup.addEntry(entries.startTextDescription(text("setup.description")).build());
        setup.addEntry(entries.startBooleanToggle(text("option.enabled"), editor.enabled)
            .setDefaultValue(true)
            .setTooltip(text("option.enabled.tooltip"))
            .setSaveConsumer(value -> editor.enabled = value)
            .build());

        var manual = entries.startBooleanToggle(text("option.manualTuning"), editor.manual)
            .setDefaultValue(false)
            .setTooltip(text("option.manualTuning.tooltip"))
            .setSaveConsumer(value -> editor.manual = value)
            .build();
        var preset = entries.startEnumSelector(text("option.setupPreset"), AutoCropConfig.SetupPreset.class, editor.preset)
            .setDefaultValue(AutoCropConfig.SetupPreset.SERVER)
            .setEnumNameProvider(value -> Component.translatable(((AutoCropConfig.SetupPreset) value).translationKey()))
            .setTooltipSupplier(value -> Optional.of(new Component[] {
                Component.translatable(value.translationKey() + ".description")
            }))
            .setRequirement(Requirement.isFalse(manual))
            .setSaveConsumer(value -> editor.preset = value)
            .build();
        setup.addEntry(preset);
        setup.addEntry(entries.startBooleanToggle(text("option.autoRefillSeeds"), values.autoRefillSeeds)
            .setDefaultValue(true)
            .setTooltip(text("option.autoRefillSeeds.tooltip"))
            .setSaveConsumer(value -> values.autoRefillSeeds = value)
            .build());
        setup.addEntry(manual);

        var advanced = entries.startSubCategory(text("advanced.title"))
            .setExpanded(true)
            .setDisplayRequirement(Requirement.isTrue(manual))
            .setTooltip(text("advanced.description"));
        advanced.add(entries.startTextDescription(text("advanced.description")).build());
        advanced.add(entries.startSelector(text("option.harvestMode"), new AutoCropConfig.HarvestMode[] {
                AutoCropConfig.HarvestMode.MANUAL, AutoCropConfig.HarvestMode.HARVEST_RISKY
            }, values.harvestMode)
            .setNameProvider(value -> Component.translatable(value.translationKey() + ".label"))
            .setDefaultValue(defaults.harvestMode)
            .setTooltip(text("option.harvestMode.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.harvestMode = value)
            .build());
        advanced.add(entries.startIntSlider(text("option.replantDelayTicks"), values.replantDelayTicks, 0, 10)
            .setDefaultValue(defaults.replantDelayTicks)
            .setTooltip(text("option.replantDelayTicks.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.replantDelayTicks = value)
            .build());
        advanced.add(entries.startIntSlider(text("option.auraCooldownTicks"), values.auraCooldownTicks, 1, 20)
            .setDefaultValue(defaults.auraCooldownTicks)
            .setTooltip(text("option.auraCooldownTicks.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.auraCooldownTicks = value)
            .build());
        advanced.add(entries.startIntSlider(text("option.riskBatchSize"), values.riskBatchSize, 1, 32)
            .setDefaultValue(defaults.riskBatchSize)
            .setTooltip(text("option.riskBatchSize.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.riskBatchSize = value)
            .build());
        advanced.add(entries.startBooleanToggle(text("option.rememberMissedReplants"), values.rememberMissedReplants)
            .setDefaultValue(defaults.rememberMissedReplants)
            .setTooltip(text("option.rememberMissedReplants.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.rememberMissedReplants = value)
            .build());
        advanced.add(entries.startIntSlider(text("option.missedReplantRetryCooldownTicks"), values.missedReplantRetryCooldownTicks, 1, 200)
            .setDefaultValue(defaults.missedReplantRetryCooldownTicks)
            .setTooltip(text("option.missedReplantRetryCooldownTicks.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.missedReplantRetryCooldownTicks = value)
            .build());
        advanced.add(entries.startBooleanToggle(text("option.fillEmptyFarmland"), values.fillEmptyFarmland)
            .setDefaultValue(false)
            .setTooltip(text("option.fillEmptyFarmland.tooltip"))
            .setRequirement(Requirement.isTrue(manual))
            .setSaveConsumer(value -> values.fillEmptyFarmland = value)
            .build());
        setup.addEntry(advanced.build());
        return builder.build();
    }
}
