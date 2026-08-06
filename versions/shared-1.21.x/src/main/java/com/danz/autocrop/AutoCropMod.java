package com.danz.autocrop;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class AutoCropMod implements ClientModInitializer {

    public static final String MOD_ID = "autocrop";
    private static final String CYCLE_KEY = "autocrop.keybind.cycle";
    private static final String LEGACY_KEY_CATEGORY = "key.category.autocrop.keybind_category";

    public static KeyMapping keyCycleMode;

    @Override
    public void onInitializeClient() {

        AutoCropConfig.register();

        keyCycleMode = KeyBindingHelper.registerKeyBinding(createCycleKeyMapping());

        ClientTickEvents.END_CLIENT_TICK.register(CropManager.INSTANCE::onEndTick);

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide()) {
                CropManager.INSTANCE.onBlockAttack(player, level, pos);
            }
            return InteractionResult.PASS;
        });
    }

    private static KeyMapping createCycleKeyMapping() {
        try {
            Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
            Object category = categoryClass.getField("MISC").get(null);
            return (KeyMapping) KeyMapping.class
                .getConstructor(String.class, InputConstants.Type.class, int.class, categoryClass)
                .newInstance(CYCLE_KEY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category);
        } catch (ReflectiveOperationException ignored) {
            try {
                return (KeyMapping) KeyMapping.class
                    .getConstructor(String.class, InputConstants.Type.class, int.class, String.class)
                    .newInstance(CYCLE_KEY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, LEGACY_KEY_CATEGORY);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to construct key mapping for this Minecraft version.", e);
            }
        }
    }
}
