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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CropManager {

    public static final CropManager INSTANCE = new CropManager();

    private enum MachineState { IDLE, WAITING }

    private record PendingReplant(BlockPos pos, Item seed) {}

    private MachineState machineState = MachineState.IDLE;

    private final Deque<PendingReplant> queue = new ArrayDeque<>();

    private final Set<BlockPos> queuedPositions = new HashSet<>();

    private final Set<PendingReplant> missedReplants = new LinkedHashSet<>();

    private final Map<PendingReplant, Long> missedRetryAfterTick = new HashMap<>();

    private final Set<BlockPos> breaksSentThisSweep = new HashSet<>();

    private int delayTimer   = 0;
    private int auraTimer    = 0;
    private int previousSlot = -1;
    private long tickCounter = 0;
    private static final double MAX_ACTION_REACH = 4.5D;
    private static final int MAX_REMEMBERED_REPLANTS = 2048;

    private CropManager() {}

    public void onEndTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        tickCounter++;

        AutoCropConfig cfg = AutoCropConfig.get();

        if (AutoCropMod.keyCycleMode.consumeClick()) {
            cycleMode(client, cfg);
        }

        if (cfg.harvestMode == AutoCropConfig.HarvestMode.DISABLED) {
            clearQueue();
            clearRememberedReplants();
            machineState = MachineState.IDLE;
            return;
        }

        if (cfg.harvestMode == AutoCropConfig.HarvestMode.HARVEST_RISKY) {
            scanForUntrackedHoles(client, cfg);
            retryMissedReplants(client, cfg);
            tickRiskyAura(client, cfg);
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
            .ifPresent(seed -> enqueueReplant(new PendingReplant(pos.immutable(), seed)));
    }

    private void cycleMode(Minecraft client, AutoCropConfig cfg) {
        AutoCropConfig.HarvestMode next = cfg.harvestMode.next();
        cfg.harvestMode = next;

        AutoConfig.getConfigHolder(AutoCropConfig.class).save();

        clearQueue();
        machineState = MachineState.IDLE;
        if (next != AutoCropConfig.HarvestMode.HARVEST_RISKY) {
            clearRememberedReplants();
        }

        showOverlayMessage(client, Component.translatable(next.translationKey()));
    }

    private static void showOverlayMessage(Minecraft client, Component message) {
        try {
            java.lang.reflect.Method m = client.gui.getClass().getMethod("setOverlayMessage", Component.class, boolean.class);
            m.invoke(client.gui, message, false);
        } catch (Exception e1) {
            try {
                java.lang.reflect.Method m = client.gui.getClass().getMethod("setOverlayMessage", Component.class);
                m.invoke(client.gui, message);
            } catch (Exception e2) {
                if (client.player != null) {
                    try {
                        java.lang.reflect.Method m = client.player.getClass().getMethod("displayClientMessage", Component.class, boolean.class);
                        m.invoke(client.player, message, true);
                    } catch (Exception e3) {
                        try {
                            java.lang.reflect.Method m = client.player.getClass().getMethod("sendSystemMessage", Component.class);
                            m.invoke(client.player, message);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void tickRiskyAura(Minecraft client, AutoCropConfig cfg) {
        if (machineState != MachineState.IDLE || !queue.isEmpty()) return;

        if (auraTimer > 0) { auraTimer--; return; }

        breaksSentThisSweep.clear();

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;
        BlockPos    origin = player.blockPosition();
        int         found  = 0;

        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-4, -1, -4),
                origin.offset( 4,  1,  4))) {

            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;

            Optional<Item> maybeSeed = getSeedForCrop(state.getBlock());
            if (maybeSeed.isEmpty()) continue;

            BlockPos immutablePos = cursor.immutable();
            if (breaksSentThisSweep.contains(immutablePos)) continue;
            if (queuedPositions.contains(immutablePos)) continue;
            if (!isWithinActionReach(player, immutablePos)) continue;

            client.gameMode.startDestroyBlock(immutablePos, Direction.UP);
            client.gameMode.destroyBlock(immutablePos);
            breaksSentThisSweep.add(immutablePos);

            enqueueReplant(new PendingReplant(immutablePos, maybeSeed.get()));

            found++;
            if (found >= cfg.riskBatchSize) break;
        }

        if (found > 0) {
            auraTimer = cfg.auraCooldownTicks;
        }
    }

    private void retryMissedReplants(Minecraft client, AutoCropConfig cfg) {
        if (!cfg.rememberMissedReplants || missedReplants.isEmpty()) return;
        if (machineState != MachineState.IDLE || !queue.isEmpty()) return;

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;
        int limit = Math.max(1, cfg.riskBatchSize);
        int queuedCount = 0;

        Iterator<PendingReplant> it = missedReplants.iterator();
        while (it.hasNext() && queuedCount < limit) {
            PendingReplant replant = it.next();

            if (!isWithinActionReach(player, replant.pos())) continue;

            BlockState current = level.getBlockState(replant.pos());
            if (isPlantedCrop(current)) {
                it.remove();
                missedRetryAfterTick.remove(replant);
                continue;
            }

            if (!current.isAir()) continue;

            BlockState below = level.getBlockState(replant.pos().below());
            if (!isValidFarmlandForSeed(below, replant.seed())) continue;

            if (!canRetryNow(replant)) continue;

            if (!hasSeedAvailable(player, cfg, replant.seed())) {
                scheduleRetry(replant, cfg);
                continue;
            }

            if (enqueueReplant(replant)) {
                queuedCount++;
                it.remove();
                missedRetryAfterTick.remove(replant);
            }
        }
    }

    private boolean isValidFarmlandForSeed(BlockState belowState, Item seed) {
        if (seed == Items.NETHER_WART) {
            return belowState.is(Blocks.SOUL_SAND);
        }
        return belowState.is(Blocks.FARMLAND);
    }

    private void executeNextReplant(Minecraft client, AutoCropConfig cfg) {
        PendingReplant replant = queue.poll();
        if (replant == null) {
            machineState = MachineState.IDLE;
            return;
        }
        queuedPositions.remove(replant.pos());

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;

        BlockState current = level.getBlockState(replant.pos());

        if (isPlantedCrop(current)) {
            clearRemembered(replant);
            advanceState(cfg);
            return;
        }

        if (!current.isAir()) {
            advanceState(cfg);
            return;
        }

        BlockState below = level.getBlockState(replant.pos().below());
        if (!isValidFarmlandForSeed(below, replant.seed())) {
            if (below.isAir()) {
                clearRemembered(replant);
            }
            advanceState(cfg);
            return;
        }

        if (!isWithinActionReach(player, replant.pos())) {
            rememberForRetry(replant, cfg);
            advanceState(cfg);
            return;
        }

        int seedSlot = findSeedInHotbar(player, replant.seed());
        if (seedSlot == -1 && cfg.autoRefillSeeds) {
            seedSlot = swapSeedFromInventory(client, replant.seed());
        }

        if (seedSlot == -1) {
            rememberForRetry(replant, cfg);
            advanceState(cfg);
            return;
        }

        Inventory inv = player.getInventory();
        previousSlot = getSelectedSlot(inv);
        setSelectedSlot(inv, seedSlot);

        BlockPos       farmland = replant.pos().below();
        Vec3           hitVec   = Vec3.atCenterOf(farmland).add(0, 0.5, 0);
        BlockHitResult hit      = new BlockHitResult(hitVec, Direction.UP, farmland, false);

        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);

        if (previousSlot >= 0 && previousSlot != seedSlot) {
            setSelectedSlot(inv, previousSlot);
        }
        previousSlot = -1;

        BlockState afterState = level.getBlockState(replant.pos());
        if (isPlantedCrop(afterState)) {
            clearRemembered(replant);
        } else {
            rememberForRetry(replant, cfg);
        }

        advanceState(cfg);
    }

    private void scanForUntrackedHoles(Minecraft client, AutoCropConfig cfg) {
        if (!cfg.rememberMissedReplants) return;
        if (queue.size() >= Math.max(1, cfg.riskBatchSize)) return;

        LocalPlayer player = client.player;
        ClientLevel level  = client.level;
        BlockPos origin = player.blockPosition();
        int limit = Math.max(1, cfg.riskBatchSize);
        int found = 0;

        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-4, -1, -4),
                origin.offset( 4,  1,  4))) {

            if (found >= limit) break;
            if (queue.size() >= limit) break;

            BlockPos pos = cursor.immutable();
            if (queuedPositions.contains(pos)) continue;
            if (hasRememberedPosition(pos)) continue;
            if (!isWithinActionReach(player, pos)) continue;

            BlockState current = level.getBlockState(pos);
            if (!current.isAir()) continue;

            Optional<Item> maybeSeed = inferSeedForHole(level, pos);
            if (maybeSeed.isEmpty()) continue;

            rememberForRetry(new PendingReplant(pos, maybeSeed.get()), cfg);
            found++;
        }
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
        LocalPlayer player  = client.player;
        int         invSlot = findSeedInInventory(player, seed);
        if (invSlot == -1) return -1;

        Inventory inv          = player.getInventory();
        int       targetHotbar = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) { targetHotbar = i; break; }
        }
        if (targetHotbar == -1) targetHotbar = getSelectedSlot(inv);

        client.gameMode.handleInventoryMouseClick(
            player.inventoryMenu.containerId,
            invSlot,
            targetHotbar,
            ClickType.SWAP,
            player
        );

        return targetHotbar;
    }

    private static int getSelectedSlot(Inventory inv) {
        try {
            java.lang.reflect.Method m = inv.getClass().getMethod("getSelectedSlot");
            return (int) m.invoke(inv);
        } catch (Exception ignored) {
            try {
                java.lang.reflect.Field f = inv.getClass().getField("selected");
                return f.getInt(inv);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field f = inv.getClass().getDeclaredField("selected");
                    f.setAccessible(true);
                    return f.getInt(inv);
                } catch (Exception ex) {
                    return 0;
                }
            }
        }
    }

    private static void setSelectedSlot(Inventory inv, int slot) {
        try {
            java.lang.reflect.Method m = inv.getClass().getMethod("setSelectedSlot", int.class);
            m.invoke(inv, slot);
        } catch (Exception ignored) {
            try {
                java.lang.reflect.Field f = inv.getClass().getField("selected");
                f.setInt(inv, slot);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field f = inv.getClass().getDeclaredField("selected");
                    f.setAccessible(true);
                    f.setInt(inv, slot);
                } catch (Exception ignored2) {}
            }
        }
    }

    private boolean enqueueReplant(PendingReplant replant) {
        if (!queuedPositions.add(replant.pos())) return false;
        queue.add(replant);
        return true;
    }

    private void clearQueue() {
        queue.clear();
        queuedPositions.clear();
    }

    private void clearRememberedReplants() {
        missedReplants.clear();
        missedRetryAfterTick.clear();
    }

    private void clearRemembered(PendingReplant replant) {
        clearRememberedAtPos(replant.pos());
    }

    private void clearRememberedAtPos(BlockPos pos) {
        Iterator<PendingReplant> it = missedReplants.iterator();
        while (it.hasNext()) {
            PendingReplant remembered = it.next();
            if (!remembered.pos().equals(pos)) continue;
            it.remove();
            missedRetryAfterTick.remove(remembered);
        }
    }

    private boolean hasRememberedPosition(BlockPos pos) {
        for (PendingReplant remembered : missedReplants) {
            if (remembered.pos().equals(pos)) return true;
        }
        return false;
    }

    private void rememberForRetry(PendingReplant replant, AutoCropConfig cfg) {
        if (cfg.harvestMode != AutoCropConfig.HarvestMode.HARVEST_RISKY
            || !cfg.rememberMissedReplants) {
            return;
        }

        boolean alreadyRememberedAtPos = hasRememberedPosition(replant.pos());
        if (alreadyRememberedAtPos) {
            clearRememberedAtPos(replant.pos());
        } else if (missedReplants.size() >= MAX_REMEMBERED_REPLANTS) {
            Iterator<PendingReplant> it = missedReplants.iterator();
            if (it.hasNext()) {
                PendingReplant oldest = it.next();
                it.remove();
                missedRetryAfterTick.remove(oldest);
            }
        }

        missedReplants.add(replant);
        scheduleRetry(replant, cfg);
    }

    private void scheduleRetry(PendingReplant replant, AutoCropConfig cfg) {
        long retryDelay = Math.max(1, cfg.missedReplantRetryCooldownTicks);
        missedRetryAfterTick.put(replant, tickCounter + retryDelay);
    }

    private boolean canRetryNow(PendingReplant replant) {
        return tickCounter >= missedRetryAfterTick.getOrDefault(replant, 0L);
    }

    private boolean hasSeedAvailable(LocalPlayer player, AutoCropConfig cfg, Item seed) {
        return findSeedInHotbar(player, seed) != -1
            || (cfg.autoRefillSeeds && findSeedInInventory(player, seed) != -1);
    }

    private boolean isPlantedCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state);
    }

    private boolean isWithinActionReach(LocalPlayer player, BlockPos pos) {
        return player.position().distanceToSqr(Vec3.atCenterOf(pos))
            <= MAX_ACTION_REACH * MAX_ACTION_REACH;
    }

    private Optional<Item> inferSeedForHole(ClientLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.SOUL_SAND)) return Optional.of(Items.NETHER_WART);
        if (!below.is(Blocks.FARMLAND)) return Optional.empty();

        for (Direction direction : new Direction[] {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        }) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (!(neighbor.getBlock() instanceof CropBlock)) continue;
            Optional<Item> seed = getSeedForCrop(neighbor.getBlock());
            if (seed.isPresent()) return seed;
        }

        return Optional.empty();
    }

    private Optional<Item> getSeedForCrop(Block block) {
        return Optional.ofNullable(switch (block) {
            case CarrotBlock carrotBlock -> Items.CARROT;
            case PotatoBlock potatoBlock -> Items.POTATO;
            case BeetrootBlock beetrootBlock -> Items.BEETROOT_SEEDS;
            case NetherWartBlock netherWartBlock -> Items.NETHER_WART;
            case CropBlock cropBlock -> Items.WHEAT_SEEDS;
            default -> null;
        });
    }
}
