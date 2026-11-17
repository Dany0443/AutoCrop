package com.danz.autocrop;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;

/** Minecraft 26.2 API calls; gameplay behavior stays in shared sources. */
final class MinecraftCompat {
    private MinecraftCompat() {}

    static KeyMapping registerCycleKey() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autocrop", "keybind_category"));
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "autocrop.keybind.cycle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
    }

    static void swing(LocalPlayer player) {
        player.swing(InteractionHand.MAIN_HAND);
    }

    static void swapInventory(Minecraft client, int inventorySlot, int hotbarSlot) {
        client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,
            inventorySlot, hotbarSlot, ContainerInput.SWAP, client.player);
    }
}
