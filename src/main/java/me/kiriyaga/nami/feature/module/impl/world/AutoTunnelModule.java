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
import me.kiriyaga.nami.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AutoTunnelModule extends Module {

    public enum TunnelMode {
        P1x1,
        P1x2,
        P1x3,
        P3x3
    }

    public final EnumSetting<TunnelMode> mode = addSetting(new EnumSetting<>("Mode", TunnelMode.P1x2));
    public final DoubleSetting distance = addSetting(new DoubleSetting("Range", 5.0, 1.0, 6.0));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", false));

    private final Set<BlockPos> cache = new HashSet<>();

    public AutoTunnelModule() {
        super("AutoTunnel", "Automatically tunnels blocks in front of you.", ModuleCategory.of("World"));
    }

    @Override
    public void onDisable() {
        cache.clear();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        BlockPos playerPos = MC.player.getBlockPos();
        Set<BlockPos> validTargets = new HashSet<>();

        BlockPos forward = playerPos.offset(MC.player.getHorizontalFacing(), 1);

        switch (mode.get()) {
            case P1x1 -> addBlockIfBreakable(validTargets, forward);
            case P1x2 -> {
                addBlockIfBreakable(validTargets, forward);
                addBlockIfBreakable(validTargets, forward.up());
            }
            case P1x3 -> {
                addBlockIfBreakable(validTargets, forward);
                addBlockIfBreakable(validTargets, forward.up());
                addBlockIfBreakable(validTargets, forward.up(2));
            }
            case P3x3 -> {
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos checkPos = forward.add(x, y, z);
                            addBlockIfBreakable(validTargets, checkPos);
                        }
                    }
                }
            }
        }

        BlockPos bestTarget = validTargets.stream()
                .min(Comparator.comparingDouble(a -> MC.player.squaredDistanceTo(
                        a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5)))
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

    private void addBlockIfBreakable(Set<BlockPos> set, BlockPos pos) {
        BlockState state = MC.world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || state.isAir()) return;
        set.add(pos);
    }
}
