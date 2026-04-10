package com.danz.autocrop;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class CropManager {

    public static final CropManager INSTANCE = new CropManager();

    private enum MachineState { IDLE, WAITING }

    private record PendingReplant(BlockPos pos, Item seed) {}

    private MachineState machineState = MachineState.IDLE;

    private final Deque<PendingReplant> queue = new ArrayDeque<>();

    private final Set<PendingReplant> missedReplants = new LinkedHashSet<>();

    private final Set<BlockPos> breaksSentThisSweep = new HashSet<>();

    private int delayTimer   = 0;
    private int auraTimer    = 0;
    private int previousSlot = -1;

    private CropManager() {}

    public void onEndTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        AutoCropConfig cfg = AutoCropConfig.get();

        if (AutoCropMod.keyCycleMode.consumeClick()) {
            cycleMode(client, cfg);
        }

        if (cfg.harvestMode == AutoCropConfig.HarvestMode.DISABLED) {
            queue.clear();
            missedReplants.clear();
            machineState = MachineState.IDLE;
            return;
        }

        if (cfg.harvestMode == AutoCropConfig.HarvestMode.HARVEST_RISKY) {
            tickRiskyAura(client, cfg);

            if (machineState == MachineState.IDLE && queue.isEmpty()) {
                retryMissedReplants(client, cfg);
            }
        }

        switch (machineState) {
            case IDLE -> {
                if (!queue.isEmpty()) {
                    delayTimer   = cfg.replantDelayTicks;
                    machineState = MachineState.WAITING;
                }
            }
            case WAITING -> {
                if (delayTimer > 0) delayTimer--;
                else                executeNextReplant(client, cfg);
            }
        }
    }

    public void onBlockAttack(Player player, Level level, BlockPos pos) {
        if (AutoCropConfig.get().harvestMode != AutoCropConfig.HarvestMode.MANUAL) return;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;

        getSeedForCrop(state.getBlock())
            .ifPresent(seed -> queue.add(new PendingReplant(pos.immutable(), seed)));
    }

    private void cycleMode(Minecraft client, AutoCropConfig cfg) {
        AutoCropConfig.HarvestMode next = cfg.harvestMode.next();
        cfg.harvestMode = next;

        AutoConfig.getConfigHolder(AutoCropConfig.class).save();

        queue.clear();
        machineState = MachineState.IDLE;
        if (next != AutoCropConfig.HarvestMode.HARVEST_RISKY) {
            missedReplants.clear();
        }

        client.gui.setOverlayMessage(
            Component.translatable(next.translationKey()),
            false
        );
    }

    private void tickRiskyAura(Minecraft client, AutoCropConfig cfg) {
        if (machineState != MachineState.IDLE || !queue.isEmpty()) return;

        if (auraTimer > 0) { auraTimer--; return; }

        breaksSentThisSweep.clear();

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;
        BlockPos    origin = player.blockPosition();
        int         found  = 0;
        int         limit  = Math.max(1, cfg.riskBatchSize);

        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-4, -1, -4),
                origin.offset( 4,  1,  4))) {

            if (found >= limit) break;

            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;

            Optional<Item> maybeSeed = getSeedForCrop(state.getBlock());
            if (maybeSeed.isEmpty()) continue;

            BlockPos immutablePos = cursor.immutable();
            if (breaksSentThisSweep.contains(immutablePos)) continue;

            client.gameMode.startDestroyBlock(immutablePos, Direction.UP);
            player.swing(InteractionHand.MAIN_HAND);
            breaksSentThisSweep.add(immutablePos);
            queue.add(new PendingReplant(immutablePos, maybeSeed.get()));
            found++;
        }

        if (found > 0) auraTimer = cfg.auraCooldownTicks;
    }

    private void retryMissedReplants(Minecraft client, AutoCropConfig cfg) {
        if (missedReplants.isEmpty()) return;

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;
        Iterator<PendingReplant> it = missedReplants.iterator();

        while (it.hasNext()) {
            PendingReplant missed = it.next();

            BlockState current = level.getBlockState(missed.pos());
            boolean stillEmpty = current.isAir()
                || (current.getBlock() instanceof CropBlock c && !c.isMaxAge(current));
            if (!stillEmpty) { it.remove(); continue; }

            boolean hasSeed = findSeedInHotbar(player, missed.seed()) != -1
                || (cfg.autoRefillSeeds && findSeedInInventory(player, missed.seed()) != -1);

            if (hasSeed) {
                queue.add(missed);
                it.remove();
                if (queue.size() >= Math.max(1, cfg.riskBatchSize)) break;
            }
        }
    }

    private void executeNextReplant(Minecraft client, AutoCropConfig cfg) {
        if (queue.isEmpty()) { machineState = MachineState.IDLE; return; }

        PendingReplant replant = queue.poll();
        LocalPlayer    player  = client.player;
        ClientLevel    level   = client.level;

        BlockState current = level.getBlockState(replant.pos());
        boolean posReady = current.isAir()
            || (current.getBlock() instanceof CropBlock c && !c.isMaxAge(current));

        if (!posReady) { advanceState(cfg); return; }

        int seedSlot = findSeedInHotbar(player, replant.seed());
        if (seedSlot == -1 && cfg.autoRefillSeeds) {
            seedSlot = swapSeedFromInventory(client, replant.seed());
        }

        if (seedSlot == -1) {
            if (cfg.harvestMode == AutoCropConfig.HarvestMode.HARVEST_RISKY
                    && cfg.rememberMissedReplants) {
                missedReplants.add(replant);
            }
            advanceState(cfg);
            return;
        }

        Inventory inv = player.getInventory();
        previousSlot  = inv.getSelectedSlot();
        inv.selected  = seedSlot;  

        BlockPos       farmland = replant.pos().below();
        Vec3           hitVec   = Vec3.atCenterOf(farmland).add(0, 0.5, 0);
        BlockHitResult hit      = new BlockHitResult(hitVec, Direction.UP, farmland, false);

        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);

        if (previousSlot >= 0 && previousSlot != seedSlot) {
            inv.selected = previousSlot;
        }
        previousSlot = -1;

        advanceState(cfg);
    }

    private void advanceState(AutoCropConfig cfg) {
        if (queue.isEmpty()) {
            machineState = MachineState.IDLE;
        } else {
            delayTimer   = cfg.replantDelayTicks;
            machineState = MachineState.WAITING;
        }
    }

    private int findSeedInHotbar(LocalPlayer player, Item target) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(target)) return i;
        }
        return -1;
    }

    private int findSeedInInventory(LocalPlayer player, Item target) {
        Inventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(target)) return i;
        }
        return -1;
    }

    private int swapSeedFromInventory(Minecraft client, Item seed) {
        LocalPlayer player      = client.player;
        int         invSlot     = findSeedInInventory(player, seed);
        if (invSlot == -1) return -1;

        Inventory inv          = player.getInventory();
        int       targetHotbar = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) { targetHotbar = i; break; }
        }
        if (targetHotbar == -1) targetHotbar = inv.getSelectedSlot();

        client.gameMode.handleContainerInput(
            player.inventoryMenu.containerId,   
            invSlot,
            targetHotbar,
            ContainerInput.SWAP,
            player
        );

        return targetHotbar;
    }

    private Optional<Item> getSeedForCrop(Block block) {
        return Optional.ofNullable(switch (block) {
            case CarrotBlock     _ -> Items.CARROT;
            case PotatoBlock     _ -> Items.POTATO;
            case BeetrootBlock   _ -> Items.BEETROOT_SEEDS;
            case NetherWartBlock _ -> Items.NETHER_WART;
            case CropBlock       _ -> Items.WHEAT_SEEDS;
            default                -> null;
        });
    }
}