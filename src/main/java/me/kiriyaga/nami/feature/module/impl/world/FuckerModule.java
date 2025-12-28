package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;


import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class FuckerModule extends Module {

    public enum Mode {
        FARM,
        SUGAR_CANE,
        GRASS
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.FARM));
    public final DoubleSetting distance = addSetting(new DoubleSetting("Range", 5.0, 1.0, 6.0));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", false));
    public final IntSetting radius = addSetting(new IntSetting("Radius", 3, 1, 6));

    private final Set<BlockPos> s = new HashSet<>();

    public FuckerModule() {
        super("Fucker", "Automatically breaks selected type of blocks around you.", ModuleCategory.of("World"));
    }

    @Override
    public void onDisable() {
        s.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent ev) {
        if (MC.player == null || MC.world == null) return;

        BlockPos playerPos = MC.player.getBlockPos();
        int r = radius.get();

        Set<BlockPos> validTargets = new HashSet<>();

        switch (mode.get()) {
            case FARM -> {
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = playerPos.add(x, y, z);
                            if (checkPos.equals(playerPos)) continue;
                            if (isFarmPlant(checkPos)) {
                                validTargets.add(checkPos);
                            }
                        }
                    }
                }
            }
            case SUGAR_CANE -> {
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = playerPos.add(x, y, z);
                            if (checkPos.equals(playerPos)) continue;
                            if (isSugarCaneBlock(checkPos)) {
                                validTargets.add(checkPos);
                            }
                        }
                    }
                }
            }
            case GRASS -> {
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos checkPos = playerPos.add(x, y, z);
                            if (checkPos.equals(playerPos)) continue;
                            if (isGrassLike(checkPos)) {
                                validTargets.add(checkPos);
                            }
                        }
                    }
                }
            }
        }

        BlockPos bestTarget = validTargets.stream()
                .min(Comparator.comparingDouble(a -> MC.player.squaredDistanceTo(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5)))
                .orElse(null);

        if (bestTarget != null) {
            InteractionUtils.breakBlock(
                    bestTarget,
                    distance.get(),
                    rotate.get(),
                    swing.get(),
                    grim.get(),
                    strictDirection.get(),
                    this.name
            );
        }
    }

    private boolean isFarmPlant(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return false;

        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMature(state);
        }

        if (block instanceof SweetBerryBushBlock) {
            Integer age = state.get(SweetBerryBushBlock.AGE);
            return age != null && age >= 3;
        }

        if (block instanceof NetherWartBlock) {
            Integer age = state.get(NetherWartBlock.AGE);
            return age != null && age >= 3;  // it's 3 for a fully grown netherwart
        }

        return false;
    }

    private boolean isSugarCaneBlock(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return false;

        if (block instanceof SugarCaneBlock || block instanceof BambooStalkBlock) {
            BlockPos belowPos = pos.down();
            Block belowBlock = MC.world.getBlockState(belowPos).getBlock();
            return belowBlock == block;
        }

        return false;
    }

    private boolean isGrassLike(BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return false;

        return block instanceof VegetationBlock;
    }
}