package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.setting.impl.EnumSetting;
import me.kiriyaga.nami.setting.impl.IntSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.BambooBlock;
import net.minecraft.util.math.BlockPos;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class NukerModule extends Module {

    public enum Mode {
        FLOOR,
        AROUND,
        FARM,
        SUGAR_CANE
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("mode", Mode.AROUND));
    public final IntSetting radius = addSetting(new IntSetting("radius", 3, 1, 6));

    public NukerModule() {
        super("nuker", "Breaks blocks around or below you.", ModuleCategory.of("world"));
    }

    @Override
    public void onDisable() {
        BREAK_MANAGER.getRequestHandler().clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTickEvent(PreTickEvent ev) {
        if (MC.player == null || MC.world == null) return;

        BlockPos playerPos = MC.player.getBlockPos();
        int r = radius.get();

        switch (mode.get()) {
            case FLOOR -> {
                BlockPos base = playerPos.down();
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos checkPos = base.add(x, 0, z);
                        addBlockToBreak(checkPos);
                    }
                }
            }
            case AROUND -> {
                BlockPos base = playerPos;
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = base.add(x, y, z);
                            if (checkPos.getY() < playerPos.getY()) continue;
                            if (checkPos.equals(playerPos)) continue;
                            addBlockToBreak(checkPos);
                        }
                    }
                }
            }
            case FARM -> {
                BlockPos base = playerPos;
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = base.add(x, y, z);
                            if (checkPos.equals(playerPos)) continue;
                            addPlantBlockToBreak(checkPos);
                        }
                    }
                }
            }
            case SUGAR_CANE -> {
                BlockPos base = playerPos;
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = base.add(x, y, z);
                            if (checkPos.equals(playerPos)) continue;
                            addSugarCaneBlockToBreak(checkPos);
                        }
                    }
                }
            }
        }
    }

    private void addBlockToBreak(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
            BREAK_MANAGER.getRequestHandler().addBlock(pos);
        }
    }

    private void addPlantBlockToBreak(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return;

        if (block instanceof CropBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof TallPlantBlock) {
            BREAK_MANAGER.getRequestHandler().addBlock(pos);
        }
    }

    private void addSugarCaneBlockToBreak(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return;

        if (block instanceof SugarCaneBlock || block instanceof BambooBlock) {
            BlockPos belowPos = pos.down();
            BlockState belowState = MC.world.getBlockState(belowPos);
            Block belowBlock = belowState.getBlock();

            if (belowBlock == block) {
                BREAK_MANAGER.getRequestHandler().addBlock(pos);
            }
        }
    }
}