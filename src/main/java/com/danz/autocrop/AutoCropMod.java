package com.danz.autocrop;

import net.minecraft.resources.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;

public class AutoCropMod implements ClientModInitializer {

    public static final String MOD_ID = "autocrop";

    public static KeyMapping keyCycleMode;

    @Override
    public void onInitializeClient() {

        AutoCropConfig.register();

        KeyMapping.Category myCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autocrop", "keybind_category"));

        keyCycleMode = KeyMappingHelper.registerKeyMapping(MinecraftCompat.cycleKey(myCategory));

        ClientTickEvents.END_CLIENT_TICK.register(CropManager.INSTANCE::onEndTick);

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide()) {
                CropManager.INSTANCE.onBlockAttack(player, level, pos);
            }
            return InteractionResult.PASS;
        });
    }
}
