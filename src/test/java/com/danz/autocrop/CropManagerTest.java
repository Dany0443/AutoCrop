package com.danz.autocrop;

import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CropManagerTest {
    private Minecraft client;
    private CropManager manager;
    private AutoCropConfig config;
    private MockedStatic<AutoCropConfig> configAccess;
    private Inventory inventory;
    private static final BlockPos CROP = new BlockPos(1, 0, 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setup() throws Exception {
        var constructor = CropManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        manager = constructor.newInstance();
        config = new AutoCropConfig();
        config.harvestMode = AutoCropConfig.HarvestMode.HARVEST_RISKY;
        config.rememberMissedReplants = false;
        config.riskBatchSize = 1;
        configAccess = mockStatic(AutoCropConfig.class);
        configAccess.when(AutoCropConfig::get).thenReturn(config);
        AutoCropMod.keyCycleMode = mock(KeyMapping.class);
        client = mock(Minecraft.class);
        client.player = mock(LocalPlayer.class);
        client.level = mock(ClientLevel.class);
        client.gameMode = mock(MultiPlayerGameMode.class);
        when(client.player.blockPosition()).thenReturn(BlockPos.ZERO);
        when(client.player.position()).thenReturn(Vec3.ZERO);
        inventory = mock(Inventory.class);
        when(client.player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(anyInt())).thenReturn(ItemStack.EMPTY);
        when(client.level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
    }

    @AfterEach
    void cleanup() {
        if (configAccess != null) configAccess.close();
    }

    private void giveSeeds() {
        ItemStack seeds = mock(ItemStack.class);
        when(seeds.is(Items.WHEAT_SEEDS)).thenReturn(true);
        when(inventory.getItem(0)).thenReturn(seeds);
    }

    private void mature(BlockPos pos) {
        when(client.level.getBlockState(pos)).thenReturn(
            Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) manager.onEndTick(client);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void harvestNeverBreaksMoreThanConfiguredBatch(int batchSize) {
        config.riskBatchSize = batchSize;
        mature(CROP);
        mature(new BlockPos(2, 0, 0));
        mature(new BlockPos(3, 0, 0));
        ticks(1);
        verify(client.gameMode, times(batchSize)).startDestroyBlock(any(), eq(Direction.UP));
    }

    @Test
    void changingWorldDiscardsPendingReplants() {
        mature(CROP);
        ticks(1);
        client.level = mock(ClientLevel.class);
        when(client.level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        ticks(10);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
    }

    @Test
    void disconnectDiscardsPendingReplantsEvenWhenSameLevelReturns() {
        mature(CROP);
        ticks(1);
        ClientLevel level = client.level;
        client.level = null;
        ticks(1);
        client.level = level;
        when(client.level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        ticks(10);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
    }

    @Test
    void disablingRememberedReplantsStopsExistingRetries() {
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        mature(CROP);
        ticks(1);
        when(client.level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        ticks(10); // Planting fails without seeds and the position is remembered.
        config.rememberMissedReplants = false;
        giveSeeds();
        ticks(10);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
    }

    @Test
    void rememberingReplantsDoesNotFillUnharvestedFarmlandByDefault() {
        config.rememberMissedReplants = true;
        when(client.level.getBlockState(CROP.below())).thenReturn(Blocks.FARMLAND.defaultBlockState());
        when(client.level.getBlockState(CROP.north())).thenReturn(Blocks.WHEAT.defaultBlockState());
        giveSeeds();
        ticks(40);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = AutoCropConfig.HarvestMode.class, names = {"MANUAL", "DISABLED"})
    void changingModeInSettingsDiscardsPendingAutomaticWork(AutoCropConfig.HarvestMode mode) {
        mature(CROP);
        ticks(1);
        config.harvestMode = mode;
        when(client.level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        ticks(10);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
    }

    @Test
    void emptyFarmPlotsCanStillBeFilledWhenExplicitlyEnabled() {
        config.rememberMissedReplants = true;
        config.fillEmptyFarmland = true;
        config.missedReplantRetryCooldownTicks = 1;
        when(client.level.getBlockState(CROP.below())).thenReturn(Blocks.FARMLAND.defaultBlockState());
        when(client.level.getBlockState(CROP.north())).thenReturn(Blocks.WHEAT.defaultBlockState());
        giveSeeds();
        ticks(10);
        verify(client.gameMode, atLeastOnce()).useItemOn(any(), any(), any());
    }

    @Test
    void manualHarvestStillReplantsTheCropThePlayerBroke() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        ticks(1);
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        ticks(10);
        verify(client.gameMode, times(1)).useItemOn(any(), any(), any());
        verify(client.gameMode, never()).startDestroyBlock(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = AutoCropConfig.HarvestMode.class, names = {"MANUAL", "HARVEST_RISKY"})
    void playerBreaksAreTrackedBeforeTheFirstTick(AutoCropConfig.HarvestMode mode) {
        config.harvestMode = mode;
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        ticks(15);
        verify(client.gameMode, times(1)).useItemOn(any(), any(), argThat(hit -> hit.getBlockPos().equals(CROP.below())));
    }

    @ParameterizedTest
    @EnumSource(value = AutoCropConfig.HarvestMode.class, names = {"MANUAL", "HARVEST_RISKY"})
    void retriesEveryBrokenPositionWhenSeedsArriveLater(AutoCropConfig.HarvestMode mode) {
        config.harvestMode = mode;
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        ticks(1);
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 0, 0);
            mature(pos);
            manager.onBlockAttack(client.player, client.level, pos);
            when(client.level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        }
        ticks(30); // Every initial attempt runs out of seeds.
        giveSeeds();
        when(client.gameMode.useItemOn(any(), any(), any())).thenAnswer(invocation -> {
            BlockHitResult hit = invocation.getArgument(2);
            when(client.level.getBlockState(hit.getBlockPos().above())).thenReturn(Blocks.WHEAT.defaultBlockState());
            return InteractionResult.SUCCESS;
        });
        ticks(50);
        for (int x = 1; x <= 3; x++) {
            BlockPos soil = new BlockPos(x, -1, 0);
            verify(client.gameMode, times(1)).useItemOn(any(), any(), argThat(hit -> hit.getBlockPos().equals(soil)));
        }
    }

    @Test
    void manualBreakWaitingForServerUpdateIsRetried() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        ticks(1);
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        giveSeeds();
        ticks(15); // The server has not removed the old mature crop yet.
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        ticks(15);
        verify(client.gameMode, atLeastOnce()).useItemOn(any(), any(), any());
    }

    @Test
    void temporarilyBlockedReplantIsNotForgotten() {
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        mature(CROP);
        ticks(1);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.STONE.defaultBlockState());
        giveSeeds();
        ticks(20);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        ticks(15);
        verify(client.gameMode, atLeastOnce()).useItemOn(any(), any(), any());
    }

    @Test
    void serverRollbackOfPredictedPlantingIsRetried() {
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        config.replantDelayTicks = 0;
        mature(CROP);
        ticks(1);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        when(client.gameMode.useItemOn(any(), any(), any())).thenAnswer(invocation -> {
            when(client.level.getBlockState(CROP)).thenReturn(Blocks.WHEAT.defaultBlockState());
            return InteractionResult.SUCCESS;
        });
        ticks(3);
        verify(client.gameMode, times(1)).useItemOn(any(), any(), any());
        ticks(3);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        ticks(15);
        verify(client.gameMode, times(2)).useItemOn(any(), any(), any());
    }

    @Test
    void fastManualBreaksKeepImmutablePositionsAndTheCorrectSeedForEachCrop() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        config.replantDelayTicks = 0;
        ticks(1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 1; x <= 3; x++) {
            cursor.set(x, 0, 0);
            BlockPos pos = cursor.immutable();
            when(client.level.getBlockState(pos)).thenReturn(
                (x == 2 ? Blocks.CARROTS : Blocks.WHEAT).defaultBlockState().setValue(CropBlock.AGE, 7));
            manager.onBlockAttack(client.player, client.level, cursor);
            manager.onBlockAttack(client.player, client.level, cursor); // Duplicate event.
            when(client.level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        }
        cursor.set(100, 0, 0);
        giveSeeds();
        ItemStack carrots = mock(ItemStack.class);
        when(carrots.is(Items.CARROT)).thenReturn(true);
        when(inventory.getItem(1)).thenReturn(carrots);
        ticks(12);
        verify(client.gameMode, times(3)).useItemOn(any(), any(), any());
        for (int x = 1; x <= 3; x++) {
            BlockPos soil = new BlockPos(x, -1, 0);
            verify(client.gameMode, times(1)).useItemOn(any(), any(), argThat(hit -> hit.getBlockPos().equals(soil)));
        }
        verify(inventory, times(1)).setSelectedSlot(1);
    }

    @Test
    void manualReplantRecoversAfterWalkingOutOfReachAndReturning() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        config.rememberMissedReplants = true;
        config.missedReplantRetryCooldownTicks = 1;
        ticks(1);
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        when(client.player.position()).thenReturn(new Vec3(50, 0, 0));
        giveSeeds();
        ticks(15);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
        when(client.player.position()).thenReturn(Vec3.ZERO);
        ticks(15);
        verify(client.gameMode, atLeastOnce()).useItemOn(any(), any(), any());
        verify(client.gameMode, never()).startDestroyBlock(any(), any());
    }

    @Test
    void cropAlreadyReplantedByPlayerIsLeftAlone() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        config.rememberMissedReplants = true;
        ticks(1);
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.CARROTS.defaultBlockState());
        giveSeeds();
        ticks(50);
        verify(client.gameMode, never()).useItemOn(any(), any(), any());
        verify(client.gameMode, never()).startDestroyBlock(any(), any());
    }

    @Test
    void disablingRecoveryDropsUnconfirmedPlacementsAsWell() {
        config.harvestMode = AutoCropConfig.HarvestMode.MANUAL;
        config.rememberMissedReplants = true;
        config.replantDelayTicks = 0;
        ticks(1);
        mature(CROP);
        manager.onBlockAttack(client.player, client.level, CROP);
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        giveSeeds();
        when(client.gameMode.useItemOn(any(), any(), any())).thenAnswer(invocation -> {
            when(client.level.getBlockState(CROP)).thenReturn(Blocks.WHEAT.defaultBlockState());
            return InteractionResult.SUCCESS;
        });
        ticks(3);
        config.rememberMissedReplants = false;
        when(client.level.getBlockState(CROP)).thenReturn(Blocks.AIR.defaultBlockState());
        ticks(50);
        verify(client.gameMode, times(1)).useItemOn(any(), any(), any());
    }

}
