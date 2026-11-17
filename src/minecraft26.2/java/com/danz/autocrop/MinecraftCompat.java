package com.danz.autocrop;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Minecraft 26.2 API calls; gameplay behavior stays in shared sources. */
final class MinecraftCompat {
    private MinecraftCompat() {}

    static KeyMapping cycleKey(KeyMapping.Category category) {
        return new KeyMapping("autocrop.keybind.cycle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
    }

    static void swing(LocalPlayer player) {
        player.swing(InteractionHand.MAIN_HAND);
    }
}
