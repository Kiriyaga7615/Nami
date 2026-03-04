package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.StartBreakingBlockEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.world.SpeedMineFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;

import java.util.*;

import static namidevelopment.kiriyaga.api.NamiApi.EVENT_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isPlaceable;
import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isPhased;

@RegisterFeature
public class AutoMineFeature extends Feature {
    public enum Mode {GRIM }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Logic", Mode.GRIM));
    public final BoolSetting face = addSetting(new BoolSetting("Face", true));
    public final BoolSetting antiCrawl = addSetting(new BoolSetting("AntiCrawl", false));
    public final BoolSetting always = addSetting(new BoolSetting("Always", true));
    public final BoolSetting matchTargetY = addSetting(new BoolSetting("MatchTargetY", true));

    public AutoMineFeature() {
        super("AutoMine", "Manages which blocks shoulf SpeedMine Feature mine.", FeatureCategory.of("Combat"));
        always.setShowCondition(antiCrawl::get);
        matchTargetY.setShowCondition(antiCrawl::get);
    }

    boolean b = false;

    @Override
    public void onEnable() {
        b = false;
    }

    /// /// GRIM LOGIC START
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (mode.get() != Mode.GRIM)
            return;
        b = true;
        if (MC.level == null || MC.player == null) return;

        Entity target = TargetUtils.getTarget();

        if (!(target instanceof Player))
            return;

        SpeedMineFeature m = FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class);

/*        if (m.currentTask != null)
            CHAT_SERVICE.sendPersistent("1", "instantremine: " + m.currentTask.isInstantRemine());*/

        if (target == null) return;

        List<BlockPos> blocks = getPriorityBlocks(target);
        if (blocks.isEmpty()) return;

        if (m.currentTask == null || !blocks.get(0).equals(m.currentTask.getBlockPos()) && (!m.currentTask.isInstantRemine() && isSurroundblock(m.currentTask.getBlockPos()))) {
            sendToSpeedMine(blocks.get(0));
            b = false;
            return;
        }

        if (FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class).doubleMine.get() && b && blocks.size() > 1 && isPhased(target) || !m.instant.get()) {
            sendToSpeedMine(blocks.get(1));
        }
    }

    private interface PriorityTask {
        List<BlockPos> getBlocks(Entity target);
    }

    private final List<PriorityTask> priority = List.of(
            this::antiCrawlTask,
            this::standsShouldMinePhase,
            this::crouchingShouldMinePhase,
            this::surroundFeet,
            this::surroundFace

    );

    private List<BlockPos> antiCrawlTask(Entity target) {
        if (target == null) return Collections.emptyList();
        if (!antiCrawl.get()) return Collections.emptyList();

        if (MC.player == null) return Collections.emptyList();

        boolean crawling = MC.player.isVisuallyCrawling();

        if (!always.get() && !crawling) return Collections.emptyList();

        BlockPos feet = MC.player.blockPosition();
        BlockPos up = feet.above();
        BlockPos down = feet.below();
        BlockPos down2 = feet.below(2);

        List<BlockPos> blocks = new ArrayList<>();

        if (!matchTargetY.get()) {
            if (!MC.level.getBlockState(up).isAir())
                blocks.add(up);
            return blocks;
        }

        double playerY = MC.player.getY();
        double targetY = target.getY();

        if (playerY <= targetY) {
            if (!MC.level.getBlockState(up).isAir()) {
                blocks.add(up);
            } else {}

            if (!blocks.isEmpty() && !canBreak(blocks.get(0))) {
                blocks.clear();
                if (!MC.level.getBlockState(down).isAir() && canBreak(down))
                    blocks.add(down);
                if (!MC.level.getBlockState(down2).isAir() && canBreak(down2))
                    blocks.add(down2);
            }

            return blocks;
        }

        if (!MC.level.getBlockState(down).isAir() && canBreak(down))
            blocks.add(down);

        if (!MC.level.getBlockState(down2).isAir() && canBreak(down2))
            blocks.add(down2);

        return blocks;
    }


    private List<BlockPos> standsShouldMinePhase(Entity target) {
        if (target.isCrouching() || !isPhased(target)) return Collections.emptyList();

        AABB bb = target.getBoundingBox();
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.ceil(bb.maxX);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.ceil(bb.maxZ);
        int y = Mth.floor(bb.minY);

        List<BlockPos> blocks = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                BlockPos p = new BlockPos(x, y, z);
                if (!MC.level.getBlockState(p).isAir())
                    blocks.add(p);
            }
        }
        return blocks;
    }

    private List<BlockPos> crouchingShouldMinePhase(Entity target) {
        if (!target.isCrouching() || !isPhased(target)) return Collections.emptyList();

        AABB bb = target.getBoundingBox();
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.ceil(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.ceil(bb.maxY);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.ceil(bb.maxZ);

        List<BlockPos> blocks = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!MC.level.getBlockState(p).isAir())
                        blocks.add(p);
                }
            }
        }
        return blocks;
    }

    private List<BlockPos> surroundFeet(Entity target) {
        return getSurround(target, 0);
    }

    private List<BlockPos> surroundFace(Entity target) {
        if (!face.get())
            return Collections.emptyList();
        return getSurround(target, 1);
    }

    private List<BlockPos> getSurround(Entity player, int yOffset) {
        Set<BlockPos> positions = new HashSet<>();

        AABB bb = player.getBoundingBox();
        int yLegs = (int) Math.floor(player.getY()) + yOffset;

        List<BlockPos> inside = new ArrayList<>();
        for (int x = (int) Math.floor(bb.minX); x < Math.ceil(bb.maxX); x++) {
            for (int z = (int) Math.floor(bb.minZ); z < Math.ceil(bb.maxZ); z++) {
                inside.add(new BlockPos(x, yLegs, z));
            }
        }

        for (BlockPos base : inside)
            addSurroundForBase(base, positions);

        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : positions)
            if (!isPlaceable(pos) && isSurroundblock(pos))
                result.add(pos);

        return result;
    }

    private List<BlockPos> getPossibleSurround(Entity player, int yOffset) {
        Set<BlockPos> positions = new HashSet<>();

        AABB bb = player.getBoundingBox();
        int yLegs = (int) Math.floor(player.getY()) + yOffset;

        List<BlockPos> inside = new ArrayList<>();
        for (int x = (int) Math.floor(bb.minX); x < Math.ceil(bb.maxX); x++) {
            for (int z = (int) Math.floor(bb.minZ); z < Math.ceil(bb.maxZ); z++) {
                inside.add(new BlockPos(x, yLegs, z));
            }
        }

        for (BlockPos base : inside)
            if (!isPlaceable(base))
                addSurroundForBase(base, positions);

        return positions.stream().toList();
    }

    private void addSurroundForBase(BlockPos base, Set<BlockPos> positions) {
        positions.add(base.north());
        positions.add(base.south());
        positions.add(base.east());
        positions.add(base.west());
    }
    /// /// GRIM LOGIC END

    private void sendToSpeedMine(BlockPos main) {
        SpeedMineFeature speedMine = FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class);
        if (speedMine == null || !speedMine.isEnabled()) return;

        StartBreakingBlockEvent event1 = new StartBreakingBlockEvent(main, Direction.UP);
        EVENT_SERVICE.post(event1);
    }

    private List<BlockPos> getPriorityBlocks(Entity target) {
        List<BlockPos> result = new ArrayList<>(2);

        for (PriorityTask task : priority) {
            List<BlockPos> blocks = task.getBlocks(target);
            if (blocks == null || blocks.isEmpty()) continue;

            for (BlockPos pos : blocks) {
                if (!isSurroundblock(pos)) continue;

                result.add(pos);

                if (result.size() == 2)
                    return result;
            }
        }
        return result;
    }

    private boolean isSurroundblock(BlockPos pos) {
        if (MC.level == null) return false;

        BlockState state = MC.level.getBlockState(pos);

        if (!isPhased(TargetUtils.getTarget()) && isPlaceable(pos))
            return false;

        if (state.getBlock().getExplosionResistance() >= 600.0f && state.getBlock() != Blocks.BEDROCK) {
            return true;
        }

        if (state.isAir()) {
            Entity target = TargetUtils.getTarget();
            if (target == null) return false;

            return getPossibleSurround(target, 0).contains(pos);
        }

        return false;
    }

    private boolean canBreak(BlockPos pos) {
        BlockState state = MC.level.getBlockState(pos);

        if (state.isAir()) return false;
        if (state.getDestroySpeed(MC.level, pos) < 0) return false;

        return true;
    }
}
