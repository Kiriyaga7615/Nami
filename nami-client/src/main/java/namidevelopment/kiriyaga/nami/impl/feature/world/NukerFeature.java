package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.StartBreakingBlockEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.util.RotationUtils.getClosestPointToEye;

@RegisterFeature
public class NukerFeature extends Feature {

    public enum NukerMode { SPHERE, SELECTIVE}
    public final EnumSetting<NukerMode> mode = addSetting(new EnumSetting<>("Mode", NukerMode.SPHERE));
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 1.0, 6.0));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting safeOnly = addSetting(new BoolSetting("SafeOnly", false));
    public final BoolSetting flatten = addSetting(new BoolSetting("Flatten", true));
    public final BoolSetting doubleMine = addSetting(new BoolSetting("DoubleMine", false));
    public final WhitelistSetting whitelist = addSetting(new WhitelistSetting("Whitelist", true, WhitelistSetting.Type.BLOCK));

    private Set<BlockPos> selectiveTargets = new HashSet<>();
    private Block blockBeingMined = null;
    private BlockPos target1 = null;
    private BlockPos target2 = null;
    private boolean b = false;

    public NukerFeature() {
        super("Nuker", "Automatically breaks blocks around you.", FeatureCategory.of("World"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        if (!doubleMine.get()) {
            BlockPos target = getTarget(null);
            if (target != null)
                InteractionUtils.breakBlock(target, range.get(), rotate.get(), swing.get(), grim.get(), strictDirection.get(), this.name);
            target1 = null;
            target2 = null;
            b = false;
            return;
        }

        if (target1 == null || !canBreak(target1)) {
            target1 = getTarget(null);
        }

        if (target2 == null || !canBreak(target2) || target2.equals(target1))
            target2 = getTarget(target1);

        BlockPos target = b ? target2 : target1;
        b = !b;
        if (target != null)
            InteractionUtils.breakBlock(target, range.get(), rotate.get(), swing.get(), grim.get(), strictDirection.get(), this.name);
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockStartBreak(StartBreakingBlockEvent event) {
        if (mode.get() != NukerMode.SELECTIVE) return;

        BlockState state = MC.level.getBlockState(event.blockPos);
        if (state.getBlock().defaultDestroyTime() == -1.0f || state.isAir()) return;

        if (selectiveTargets.isEmpty()) {
            blockBeingMined = state.getBlock();
            int radius = 5;
            BlockPos origin = event.blockPos;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        BlockState checkState = MC.level.getBlockState(pos);
                        if (checkState.getBlock() == blockBeingMined && canBreak(pos)) {
                            selectiveTargets.add(pos);
                        }
                    }
                }
            }
        }
    }

    private BlockPos getTarget(BlockPos exclude) {
        if (mode.get() == NukerMode.SPHERE) {
            BlockPos look = looking();
            if (look != null && !look.equals(exclude)) return look;
            return closest(exclude);
        }

        if (mode.get() == NukerMode.SELECTIVE) {
            selectiveTargets.removeIf(pos -> !canBreak(pos));
            if (selectiveTargets.isEmpty()) return null;

            if (exclude == null) {
                return closestSelective(null);
            } else {
                return closestSelective(exclude);
            }
        }

        return null;
    }
    private BlockPos looking() {
        HitResult hit = MC.player.pick(range.get(), 1.0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            if (canBreak(pos)) return pos;
        }
        return null;
    }

    private BlockPos closest(BlockPos exclude) {
        Vec3 eyePos = MC.player.getEyePosition(1.0f);
        int radius = 6;
        BlockPos playerPos = MC.player.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (exclude != null && pos.equals(exclude)) continue;
                    if (!canBreak(pos)) continue;

                    double dist = eyePos.distanceToSqr(Vec3.atCenterOf(pos));
                    if (dist < closestDistance) {
                        closest = pos;
                        closestDistance = dist;
                    }
                }
            }
        }
        return closest;
    }

    private BlockPos closestSelective(BlockPos exclude) {
        if (selectiveTargets.isEmpty()) return null;

        Vec3 eyePos = MC.player.getEyePosition(1.0f);
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        Iterator<BlockPos> it = selectiveTargets.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!canBreak(pos)) {
                it.remove();
                continue;
            }
            if (exclude != null && pos.equals(exclude)) continue;

            double dist = eyePos.distanceToSqr(Vec3.atCenterOf(pos));
            if (dist < closestDistance) {
                closest = pos;
                closestDistance = dist;
            }
        }
        return closest;
    }

    private boolean canBreak(BlockPos pos) {
        if (MC.level == null) return false;
        BlockState state = MC.level.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || state.isAir()) return false;
        if (flatten.get() && pos.getY() <= MC.player.getBlockY() - 1) return false;
        if (whitelist.get() && !whitelist.contains(BuiltInRegistries.BLOCK.getKey(block).toString())) return false;

        if (safeOnly.get()) {
            BlockState above = MC.level.getBlockState(pos.above());
            if (above.getBlock() == Blocks.SAND || above.getBlock() == Blocks.GRAVEL) return false;

            BlockPos[] adjacent = new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west(), pos.above()};
            for (BlockPos adj : adjacent) {
                BlockState adjState = MC.level.getBlockState(adj);
                if (adjState.getBlock() == Blocks.LAVA || adjState.getBlock() == Blocks.WATER) return false;
            }
        }

        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(pos);
        Vec3 lookDir = getClosestPointToEye(eyePos, blockBox).subtract(eyePos).normalize();
        Vec3 reachEnd = eyePos.add(lookDir.scale(range.get()));
        if (blockBox.clip(eyePos, reachEnd).isEmpty())
            return false;

        return true;
    }
}
