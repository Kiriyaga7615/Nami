package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.event.impl.StartBreakingBlockEvent;
import namidevelopment.kiriyaga.api.mixininterface.IClientPlayerInteractionManager;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.api.util.InventoryUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.movement.SneakFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;

import java.awt.*;

import static namidevelopment.kiriyaga.api.util.RotationUtils.*;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isBroken;
import static namidevelopment.kiriyaga.api.util.PacketUtils.sendSequencedPacket;

@RegisterFeature
public class SpeedMineFeature extends Feature {
    public enum Rotate { NORMAL, HOLD, NONE}
    public enum Swap { NONE, NORMAL, SILENT121, SILENT}

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 2.0, 7.0));
    public final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 1.0, 0.7, 1.0));
    public final EnumSetting<Swap> swap = addSetting(new EnumSetting<>("Swap", Swap.NORMAL));
    public final EnumSetting<Rotate> rotate = addSetting(new EnumSetting<>("Rotate", Rotate.NORMAL));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting doubleMine = addSetting(new BoolSetting("DoubleMine", false));
    public final BoolSetting instant = addSetting(new BoolSetting("Instant", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting multitask = addSetting(new BoolSetting("Multitask", false));
    public final BoolSetting allowOffhand = addSetting(new BoolSetting("AllowOffhand", false));


    public BlockBreakingTask currentTask;
    public BlockBreakingTask doubleMineTask;

    private int shouldSwapBack = -1;

    // Thats first packet mine i made like in my whole life, its bad, and there is issues, im gonna finish it, and maybe rewrite from scratch later
    public SpeedMineFeature() {
        super("SpeedMine", "Increases speed of mining.", FeatureCategory.of("World"));
        allowOffhand.setShowCondition(()-> !multitask.get());
    }

    @Override
    public void onDisable() {
        if (currentTask != null && currentTask.isStarted()) {
            abortMining(currentTask);
        }
        currentTask = null;
        doubleMineTask = null;
        shouldSwapBack = -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (MC.level == null || MC.player == null)
            return;

        if (shouldSwapBack != -1)
            InventoryUtils.attemptSwitch(shouldSwapBack);

        shouldSwapBack = -1;

        if (currentTask != null)
            handleMiningTick(currentTask);

        if (doubleMineTask != null)
            handleDoubleMine(doubleMineTask);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockStartBreak(StartBreakingBlockEvent event) {
        BlockState state = MC.level.getBlockState(event.blockPos);

        event.cancel();

        if (state.getBlock().defaultDestroyTime() == -1.0f || state.isAir()) {
            return;
        }

        if (doubleMineTask != null && doubleMineTask.getBlockPos().equals(event.blockPos))
            return;

        if (swing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        if (currentTask != null) {
            if (currentTask.getBlockPos().equals(event.blockPos)) return;

            if (doubleMineTask == null) {
                doubleMineTask = new BlockBreakingTask(currentTask.getBlockPos(), currentTask.getFacing(), 1.0f);
                doubleMineTask.setProgress(currentTask.getProgress());
            }
            abortMining(currentTask);
        }

        currentTask = new BlockBreakingTask(event.blockPos, event.direction, speed.get().floatValue());
        startMining(currentTask);

        float damageDelta = calculateBlockDamage(currentTask.getStartState(), MC.level, currentTask.getBlockPos());
        if (damageDelta >= 0.100f)
            finishMining(currentTask);
    }

    @SubscribeEvent
    public void onRender3DEvent(Render3DEvent event) {
        if (currentTask != null)
            renderProgress(event,currentTask);

        if (!doubleMine.get())
            return;

        if (doubleMineTask != null)
            renderProgress(event,doubleMineTask);
    }

    private void renderProgress(Render3DEvent event, BlockBreakingTask task) {
        BlockPos pos = task.getBlockPos();

        if (MC.level.getBlockState(pos).isAir())
            return;

        VoxelShape shape = task.isInstantRemine() ? Shapes.block() : task.getStartState().getShape(MC.level, pos);

        if (shape.isEmpty()) shape = Shapes.block();

        AABB bb = shape.bounds();
        AABB worldBox = new AABB(
                pos.getX() + bb.minX, pos.getY() + bb.minY, pos.getZ() + bb.minZ,
                pos.getX() + bb.maxX, pos.getY() + bb.maxY, pos.getZ() + bb.maxZ
        );

        Vec3 center = worldBox.getCenter();

        float partialTicks = event.getTickDelta();
        float currentProgress = task.getProgress();
        float previousProgress = task.getPreviousProgress();
        float interpolatedProgress = previousProgress + (currentProgress - previousProgress) * partialTicks;

        float scale = Mth.clamp(interpolatedProgress / task.getTargetSpeed(), 0, 1.0f);

        double dx = (bb.maxX - bb.minX) / 2.0;
        double dy = (bb.maxY - bb.minY) / 2.0;
        double dz = (bb.maxZ - bb.minZ) / 2.0;

        AABB box = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);

        float t = Mth.clamp((scale - 0.5f) * 2f, 0f, 1f);
        int maxColor = 200;
        int r = (int) (maxColor * (1 - t));
        int g = (int) (maxColor * t);
        int b = 0;

        Color color = new Color(r, g, b, 255);

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private void handleMiningTick(BlockBreakingTask task) {
        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(task.getBlockPos());
        Vec3 lookDir = getClosestPointToEye(eyePos, blockBox).subtract(eyePos).normalize();
        Vec3 reachEnd = eyePos.add(lookDir.scale(range.get()));
        boolean insideBox = blockBox.contains(eyePos);

        if (!insideBox && blockBox.clip(eyePos, reachEnd).isEmpty()) {
            abortMining(task);
            currentTask = null;
            return;
        }

        if (task.getBlockState().isAir()) {
            if (instant.get()) {
                task.markInstantRemine();
                task.setProgress(1.0f);
            } else {
                task.resetProgress();
            }
        }

        if (swing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        if (rotate.get() == Rotate.HOLD)
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.name, 8, getYawToVec(MC.player, getClosestPointToEye(eyePos, blockBox)), getPitchToVec(MC.player, getClosestPointToEye(eyePos, blockBox))));


        float damageDelta = calculateBlockDamage(task.getStartState(), MC.level, task.getBlockPos());
        if (task.incrementProgress(damageDelta) >= task.getTargetSpeed() || task.isInstantRemine()) {
            finishMining(task);
        }
    }

    private void handleDoubleMine(BlockBreakingTask task) {
        if (!doubleMine.get())
            return;

        if (task.getDoublemineHoldTicks() > 2) {
            doubleMineTask = null;
            return;
        }

        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(task.getBlockPos());
        Vec3 lookDir = getClosestPointToEye(eyePos, blockBox).subtract(eyePos).normalize();
        Vec3 reachEnd = eyePos.add(lookDir.scale(range.get()));
        boolean insideBox = blockBox.contains(eyePos);

        if (!insideBox && blockBox.clip(eyePos, reachEnd).isEmpty()) {
            doubleMineTask = null;
            return;
        }

        if (task.getBlockState().isAir()) {
            doubleMineTask = null;
            return;
        }

        float damageDelta = calculateBlockDamage(task.getBlockState(), MC.level, task.getBlockPos());

        if (task.incrementProgress(damageDelta) >= task.getTargetSpeed()) {
            if (!multitask.get() && MC.player.isUsingItem())return;

            if (swap.get() == Swap.SILENT121 || swap.get() == Swap.SILENT) {
                int slot = getSlot(task.getStartState());
                task.setDoublemineHoldTicks(task.doublemineHoldTicks+1);
                if (slot == MC.player.getInventory().getSelectedSlot())
                    return;

                shouldSwapBack = MC.player.getInventory().getSelectedSlot();
                InventoryUtils.attemptSwitch(slot);
            }
        }

//        if (swap.get() == Swap.SILENT) {
//            InventoryUtils.attemptSwitch(prev);
//            shouldSwapBack = -1;
//        }
    }

    private void startMining(BlockBreakingTask task) {
        if (task.getBlockState().isAir()) return;

        if (swap.get() == Swap.NORMAL)
            InventoryUtils.attemptSwitch(getSlot(task.getStartState()));

        if (grim.get())
            sendDestroyPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, task);

        sendDestroyPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, task);
        sendDestroyPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, task);

        task.markStarted();
    }

    private void abortMining(BlockBreakingTask task) {
        if (!task.isStarted() || task.getBlockState().isAir() || task.isInstantRemine() || task.getProgress() >= 1.0f)
            return;

        if (grim.get())
            sendDestroyPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, task);

        if (swing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        sendDestroyPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, task);
    }

    private void finishMining(BlockBreakingTask task) {
        if (!task.isStarted()) return;
        if (!multitask.get() && MC.player.isUsingItem()) {
            if (!(allowOffhand.get() && MC.player.getUsedItemHand() == InteractionHand.OFF_HAND)) { // yo somehow on some paper servers we can do it
                return;
            }
        }

        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(task.getBlockPos());

        if (rotate.get() == Rotate.NORMAL)
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.name, 8, getYawToVec(MC.player, getClosestPointToEye(eyePos, blockBox)), getPitchToVec(MC.player, getClosestPointToEye(eyePos, blockBox))));

        if (rotate.get() == Rotate.NORMAL && !ROTATION_SERVICE.getRequestHandler().isCompleted(this.name))
            return;

        int prev = MC.player.getInventory().getSelectedSlot();
        if (swap.get() == Swap.SILENT121 || swap.get() == Swap.SILENT) {
            int slot = getSlot(task.getStartState());
            if (slot != MC.player.getInventory().getSelectedSlot()) {
                if (currentTask.brokenCount < 2 || !currentTask.isInstantRemine())
                    if (swap.get() != Swap.SILENT)
                        shouldSwapBack = MC.player.getInventory().getSelectedSlot();

                InventoryUtils.attemptSwitch(slot);
            }
        }

        if (grim.get())
            sendDestroyPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, task);

        if (swing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        sendDestroyPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, task);
        //MC.level.destroyBlock(task.blockPos, false, MC.player, 512);

        if (swap.get() == Swap.SILENT121 && currentTask.isInstantRemine() && currentTask.brokenCount >= 2) {
            InventoryUtils.attemptSwitch(prev);

        }

        if (swap.get() == Swap.SILENT && shouldSwapBack == -1)
            InventoryUtils.attemptSwitch(prev);

        currentTask.markLastBroken();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (currentTask == null) return;

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket blockPacket) {
            BlockPos pos = blockPacket.getPos();

            if (pos.equals(currentTask.getBlockPos())) {
                currentTask.markBroken();
            }
        }
    }

    private void sendDestroyPacket(ServerboundPlayerActionPacket.Action action, BlockBreakingTask task) {
        sendSequencedPacket(id -> new ServerboundPlayerActionPacket(action, task.getBlockPos(), task.getFacing(), id));
    }

    private float calculateBlockDamage(BlockState state, BlockGetter world, BlockPos pos) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness == -1.0f) return 0.0f;

        int divisor = canHarvest(state) ? 30 : 100;
        return getMiningSpeed(state) / hardness / divisor;
    }

    private boolean canHarvest(BlockState state) {
        if (state.requiresCorrectToolForDrops()) {
            ItemStack held = MC.player.getMainHandItem();
            if (swap.get() == Swap.SILENT121 || swap.get() == Swap.SILENT) {
                held = MC.player.getInventory().getItem(getSlot(state));
            }
                return held.isCorrectToolForDrops(state);
        }
        return true;
    }

    private int getSlot(BlockState targetState) {
        int bestSlot = MC.player.getInventory().getSelectedSlot();
        float bestSpeed = 1.0f;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = MC.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;

            float speed = getToolSpeed(stack, targetState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private float getToolSpeed(ItemStack stack, BlockState state) {
        if (!stack.isCorrectToolForDrops(state)) return 1.0f;

        float efficiency = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
        return stack.getDestroySpeed(state) * (1 + efficiency * 0.2f);
    }

    private float getMiningSpeed(BlockState state) {
        ItemStack stack = MC.player.getMainHandItem();

        if (swap.get() == Swap.SILENT121 || swap.get() == Swap.SILENT)
            stack = MC.player.getInventory().getItem(getSlot(state));

        float speed = stack.getDestroySpeed(state);

        if (speed > 1.0f) {
            int level = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (level > 0 && !stack.isEmpty()) {
                speed += (level * level + 1);
            }
        }

        if (MobEffectUtil.hasDigSpeed(MC.player)) {
            int amplifier = MobEffectUtil.getDigSpeedAmplification(MC.player) + 1;
            speed *= 1.0f + amplifier * 0.2f;
        }

        if (MC.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float multiplier = switch (MC.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 0.00081f;
            };
            speed *= multiplier;
        }

        boolean noAquaAffinity = EnchantmentUtils.getEnchantmentLevel(
                MC.player.getItemBySlot(EquipmentSlot.HEAD), Enchantments.AQUA_AFFINITY) == 0;
        if (MC.player.isEyeInFluid(FluidTags.WATER) && noAquaAffinity) {
            speed /= 5.0f;
        }

        if (!MC.player.onGround()) {
            speed /= 5.0f;
        }

        return speed;
    }

    public static class BlockBreakingTask {
        private final BlockPos blockPos;
        private final Direction facing;
        private final float targetSpeed;

        private BlockState startState;

        private float progress;
        private float previousProgress;
        private boolean instantRemine;
        private boolean started;
        private int brokenCount;
        private int lastBrokenCount;
        private int doublemineHoldTicks;

        public BlockBreakingTask(BlockPos pos, Direction face, float speed) {
            this.blockPos = pos;
            this.facing = face;
            this.targetSpeed = speed;

            this.startState = MC.level.getBlockState(pos);

            brokenCount = 0;
            lastBrokenCount = -1;
            doublemineHoldTicks = 0;
        }

        public BlockPos getBlockPos() { return blockPos; }
        public Direction getFacing() { return facing; }
        public float getTargetSpeed() { return targetSpeed; }

        public BlockState getBlockState() { return MC.level.getBlockState(blockPos); }

        public BlockState getStartState() {
            BlockState b = getBlockState();

            if (!b.isAir() && b.getBlock() != startState.getBlock()) {
                startState = b;
            }

            return startState;
        }


        public boolean isStarted() { return started; }
        public void markStarted() { this.started = true; }

        public float getProgress() { return progress; }
        public float getPreviousProgress() { return previousProgress; }

        public float incrementProgress(float delta) {
            this.previousProgress = progress;
            return (progress += delta);
        }

        public void setProgress(float value) {
            this.previousProgress = progress;
            this.progress = value;
        }

        public void resetProgress() {
            this.progress = 0.0f;
            this.previousProgress = 0.0f;
            this.instantRemine = false;
        }

        public boolean isInstantRemine() { return instantRemine; }
        public void markInstantRemine() { this.instantRemine = true; }

        public int getBrokenCount() { return brokenCount; }
        public void markBroken() { brokenCount++; }

        public int getLastBrokenCount() { return lastBrokenCount; }
        public void markLastBroken() { lastBrokenCount = brokenCount; }

        public int getDoublemineHoldTicks() { return doublemineHoldTicks; }
        public void setDoublemineHoldTicks(int i) { doublemineHoldTicks = i; }
    }
}
