package me.kiriyaga.nami.util;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.world.level.block.BedBlock;
import 	net.minecraft.world.level.block.Block;
import 	net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Hand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.math.*;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.PacketUtils.sendSequencedPacket;
import static me.kiriyaga.nami.util.RotationUtils.*;
import static net.minecraft.util.Hand.MAIN_HAND;

public class InteractionUtils {

    private static BlockPos currentBreakingBlock = null;

    public static boolean interactWithEntity(Entity entity, double range, boolean swing, boolean rotate, String rotationId) {
        if (MC.player == null || MC.interactionManager == null || entity == null) return false;

        Vec3d eyePos = MC.player.getCameraPosVec(1.0f);
        Vec3d closestPoint = getClosestPointToEye(eyePos, entity.getBoundingBox());
        float idealYaw = (float) getYawToVec(MC.player, closestPoint);
        float idealPitch = (float) getPitchToVec(MC.player, closestPoint);

        boolean insideBox = entity.getBoundingBox().contains(MC.player.getEyePos());
        EntityHitResult hitResult = raycastTarget(MC.player, entity, range, idealYaw, idealPitch);
        if (!insideBox && hitResult == null) {
            return false;
        }

        if (rotate)
            ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(rotationId, 4, idealYaw, idealPitch));

        boolean completed = !rotate || ROTATION_MANAGER.getRequestHandler().isCompleted(rotationId);

        if (!completed)
            return false;

        MC.interactionManager.interactEntityAtLocation(MC.player, entity, hitResult, MAIN_HAND);
        MC.interactionManager.interactEntity(MC.player, entity, MAIN_HAND);

        if (swing)
            MC.player.swingHand(MAIN_HAND);

        return true;
    }


    public static EntityHitResult raycastTarget(Entity player, Entity target, double reach, float yaw, float pitch) {
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d look = getLookVectorFromYawPitch(yaw, pitch);
        Vec3d reachEnd = eyePos.add(look.multiply(reach));

        Box targetBox = target.getBoundingBox();

        if (targetBox.raycast(eyePos, reachEnd).isPresent()) {
            return new EntityHitResult(target);
        }

        return null;
    }

    public static void startUsingItem() {
        startUsingItem(MAIN_HAND);
    }

    public static void startUsingItem(Hand hand) {
        if (!MC.player.isUsingItem())
            MC.player.setCurrentHand(hand);
    }

    public static void stopUsingItem() {
        if (MC.player.isUsingItem())
            MC.player.stopUsingItem();
    }

    // TODO: figure out how to place on interactable blocks without manually sneaking

    public static boolean placeBlock(BlockPos pos, int slot,double range, boolean rotate, boolean strictDirection, boolean simulate, boolean swing, String rotationId) {
        if (!MC.world.getBlockState(pos).isReplaceable())
            return false;

        Direction direction = getDirection(pos);
        if (direction == null) {
            return false;
        }
        BlockPos neighbor = pos.offset(direction.getOpposite());
        Direction clickFace = direction;

        Vec3d playerPos = MC.player.getEntityPos();
        double offX = playerPos.x - Math.floor(playerPos.x);
        double offY = playerPos.y - Math.floor(playerPos.y);
        double offZ = playerPos.z - Math.floor(playerPos.z);
        offX = MathHelper.clamp(offX, 0.2, 0.8);
        offY = MathHelper.clamp(offY, 0.2, 0.8);
        offZ = MathHelper.clamp(offZ, 0.2, 0.8);
        Vec3d hitVec = Vec3d.ofCenter(neighbor);

        switch (clickFace) { //todo: refactor this
            case UP, DOWN -> hitVec = hitVec.add(
                    offX - 0.5,
                    clickFace == Direction.UP ? 0.5 : -0.5,
                    offZ - 0.5
            );

            case NORTH, SOUTH -> hitVec = hitVec.add(
                    offX - 0.5,
                    offY - 0.5,
                    clickFace == Direction.SOUTH ? 0.5 : -0.5
            );

            case EAST, WEST -> hitVec = hitVec.add(
                    clickFace == Direction.EAST ? 0.5 : -0.5,
                    offY - 0.5,
                    offZ - 0.5
            );
        }

        // Simplified grim v2 PlacePosition check
        // we do not use all possible eye positions because its just unnecessary
        if (strictDirection) { // todo something while phased
            Vec3d eyePos = MC.player.getEyePos();

            boolean flag = switch (clickFace) { // https://github.com/GrimAnticheat/Grim/blob/fb926ab0fbca081ad765389c541880a4a435fabb/common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/PositionPlace.java#L49
                case NORTH -> eyePos.z <= neighbor.getZ() + 1e-3;
                case SOUTH -> eyePos.z >= neighbor.getZ() + 1 - 1e-3;
                case WEST  -> eyePos.x <= neighbor.getX() + 1e-3;
                case EAST  -> eyePos.x >= neighbor.getX() + 1 - 1e-3;
                case DOWN  -> eyePos.y <= neighbor.getY() + 1e-3;
                case UP    -> eyePos.y >= neighbor.getY() + 1 - 1e-3;
                default -> false;
            };

            if (!flag) {
                return false;
            }
        }

        Vec3d eyePos = MC.player.getEyePos();
        Box blockBox = new Box(neighbor);
        Vec3d lookDir = getClosestPointToEye(eyePos, blockBox).subtract(eyePos).normalize();
        Vec3d reachEnd = eyePos.add(lookDir.multiply(range));

        if (blockBox.raycast(eyePos, reachEnd).isEmpty())
            return false;

        BlockHitResult hitResult = new BlockHitResult(hitVec, clickFace, neighbor, false);
        boolean canPlace = true;

        if (rotate) {
            float yaw = (float) getYawToVec(MC.player, hitVec);
            float pitch = (float) getPitchToVec(MC.player, hitVec);

            if (getDefaultRotationMode() == RotationModule.RotationMode.SILENT)
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(rotationId, 8, yaw, pitch));
            else
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(rotationId, 8, MC.player, hitVec));

            canPlace = ROTATION_MANAGER.getRequestHandler().isCompleted(rotationId);
        }

        boolean result = false;
        if (canPlace) {
            int prev = MC.player.getInventory().getSelectedSlot();
            INVENTORY_MANAGER.getSlotHandler().attemptSwitch(slot);

            if (simulate)
                MC.interactionManager.interactBlock(MC.player, MAIN_HAND, hitResult);
            else
                sendSequencedPacket(id -> new ServerboundPlayerActionPacket(MAIN_HAND, hitResult, id));

            if (swing)
                MC.player.swingHand(MAIN_HAND);

            result = true;

            INVENTORY_MANAGER.getSlotHandler().attemptSwitch(prev);
        }

        return result;
    }

    public static boolean interactBlockAt(BlockPos pos, int slot, double range, boolean rotate, boolean strictDirection, boolean simulate, boolean swing, String rotationId) {

        Vec3d eyePos = MC.player.getEyePos();
        Vec3d hitVec = Vec3d.ofCenter(pos);

        Direction clickFace = Direction.getFacing(hitVec.x - eyePos.x, hitVec.y - eyePos.y, hitVec.z - eyePos.z);

        if (strictDirection) {
            boolean flag = switch (clickFace) {
                case NORTH -> eyePos.z <= pos.getZ() + 1e-3;
                case SOUTH -> eyePos.z >= pos.getZ() + 1 - 1e-3;
                case WEST  -> eyePos.x <= pos.getX() + 1e-3;
                case EAST  -> eyePos.x >= pos.getX() + 1 - 1e-3;
                case DOWN  -> eyePos.y <= pos.getY() + 1e-3;
                case UP    -> eyePos.y >= pos.getY() + 1 - 1e-3;
            };
            if (!flag)
                return false;
        }

       Box box = new Box(pos);
        Vec3d lookDir = getClosestPointToEye(eyePos, box).subtract(eyePos).normalize();
        Vec3d reachEnd = eyePos.add(lookDir.multiply(range));

        if (box.raycast(eyePos, reachEnd).isEmpty())
            return false;

        BlockHitResult hit = new BlockHitResult(hitVec, clickFace, pos, false);

        boolean canInteract = true;

        if (rotate) {
            float yaw = (float) getYawToVec(MC.player, hitVec);
            float pitch = (float) getPitchToVec(MC.player, hitVec);

            if (getDefaultRotationMode() == RotationModule.RotationMode.SILENT)
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(rotationId, 8, yaw, pitch));
            else
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(rotationId, 8, MC.player, hitVec));

            canInteract = ROTATION_MANAGER.getRequestHandler().isCompleted(rotationId);
        }

        if (!canInteract)
            return false;

        int prev = MC.player.getInventory().getSelectedSlot();
        INVENTORY_MANAGER.getSlotHandler().attemptSwitch(slot);

        if (simulate)
            MC.interactionManager.interactBlock(MC.player, MAIN_HAND, hit);
        else
            sendSequencedPacket(id -> new ServerboundPlayerActionPacket(MAIN_HAND, hit, id));

        if (swing)
            MC.player.swingHand(MAIN_HAND);

        INVENTORY_MANAGER.getSlotHandler().attemptSwitch(prev);
        return true;
    }

    public static Direction getDirection(BlockPos blockPos) {
        for (final Direction direction : Direction.values()) {
            final BlockState state = MC.world.getBlockState(blockPos.offset(direction));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }

            Direction opposite = direction.getOpposite();

            return opposite;
        }
        return null;
    }

    private static RotationModule.RotationMode getDefaultRotationMode() {
        RotationModule module = MODULE_MANAGER.getStorage().getByClass(RotationModule.class);
        return module != null ? module.rotation.get() : RotationModule.RotationMode.MOTION;
    }

    public static void airPlace(BlockHitResult target, boolean grim, boolean swing) {
        if (grim) {
            MC.getNetworkHandler().sendPacket(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));

            MC.interactionManager.interactBlock(MC.player, Hand.OFF_HAND, target);
            if (swing)
                MC.player.swingHand(Hand.MAIN_HAND, false);

            MC.getNetworkHandler().sendPacket(new ServerboundSwingPacket(Hand.OFF_HAND));
            MC.getNetworkHandler().sendPacket(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        } else {
            MC.interactionManager.interactBlock(MC.player, Hand.MAIN_HAND, target);
            if (swing)
                MC.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static boolean breakBlock(BlockPos pos, double range, boolean rotate, boolean swing, boolean grim, boolean strictDirection, String rotationId) {
        if (MC.player == null || MC.interactionManager == null)
            return false;

        //CHAT_MANAGER.sendRaw(((ClientPlayerInteractionManagerAccessor) MC.interactionManager).getBlockBreakingCooldown()+"");

        if (isBlockAirOrFluid(pos)) {
            if (currentBreakingBlock != null && currentBreakingBlock.equals(pos)) {
                currentBreakingBlock = null;
            }
            return false;
        }

        Vec3d eyePos = MC.player.getEyePos();
        Box blockBox = new Box(pos);
        Vec3d lookDir = getClosestPointToEye(eyePos, blockBox).subtract(eyePos).normalize();
        Vec3d reachEnd = eyePos.add(lookDir.multiply(range));

        if (blockBox.raycast(eyePos, reachEnd).isEmpty())
            return false;

        double dx = eyePos.x - blockBox.getCenter().x;
        double dy = eyePos.y - blockBox.getCenter().y;
        double dz = eyePos.z - blockBox.getCenter().z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        Direction direction = Direction.UP;

        if (strictDirection) {

            if (absY > absX && absY > absZ) direction = dy > 0 ? Direction.UP : Direction.DOWN;
            else if (absX > absZ) direction = dx > 0 ? Direction.EAST : Direction.WEST;
            else direction = dz > 0 ? Direction.SOUTH : Direction.NORTH;

            boolean flag = switch (direction) {
                case NORTH -> eyePos.z <= pos.getZ() + 1e-3;
                case SOUTH -> eyePos.z >= pos.getZ() + 1 - 1e-3;
                case WEST  -> eyePos.x <= pos.getX() + 1e-3;
                case EAST  -> eyePos.x >= pos.getX() + 1 - 1e-3;
                case DOWN  -> eyePos.y <= pos.getY() + 1e-3;
                case UP    -> eyePos.y >= pos.getY() + 1 - 1e-3;
                default -> false;
            };

            if (!flag) {
                return false;
            }
        }


        if (rotate) {
            Vec3d center = Vec3d.ofCenter(pos).add(
                    direction.getOffsetX() * 0.5,
                    direction.getOffsetY() * 0.5,
                    direction.getOffsetZ() * 0.5
            );

            ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                    rotationId,
                    3,
                    (float) getYawToVec(MC.player, center),
                    (float) getPitchToVec(MC.player, center)
            ));

            if (!ROTATION_MANAGER.getRequestHandler().isCompleted(rotationId)) {
                return false;
            }
        }

        boolean success = MC.interactionManager.updateBlockBreakingProgress(pos, direction);
        if (swing)
            MC.player.swingHand(MAIN_HAND);

        if (grim && ((ClientPlayerInteractionManagerAccessor) MC.interactionManager).getBlockBreakingCooldown() != 0) // https://github.com/GrimAnticheat/Grim/blob/def21633e2bfa52e2dd4afdf91aec3c0ec6d14e7/common/src/main/java/ac/grim/grimac/checks/impl/breaking/FastBreak.java#L28
            return false;

        if (isBlockAirOrFluid(pos)) {  // somehow it happens https://github.com/GrimAnticheat/Grim/blob/def21633e2bfa52e2dd4afdf91aec3c0ec6d14e7/common/src/main/java/ac/grim/grimac/checks/impl/breaking/AirLiquidBreak.java#L18
            currentBreakingBlock = null;
            return false;
        }

        if (currentBreakingBlock == null || !currentBreakingBlock.equals(pos)) {
            currentBreakingBlock = pos;
            MC.interactionManager.attackBlock(pos, direction);
        } else {
            if (!success) {
                currentBreakingBlock = null;
                return false;
            }
        }

        return true;
    }

    private static boolean isBlockAirOrFluid(BlockPos pos) {
        if (MC.world.getBlockState(pos).isAir()) {
            return true;
        }
        FluidState fluidState = MC.world.getFluidState(pos);
        return !fluidState.isEmpty();
    }

    public static boolean isPlaceable(BlockPos pos) {
        return isPlaceable(pos, 10);
    }

    public static boolean isPlaceable(BlockPos pos, int distance) {
        Box blockBox = new Box(pos);
        for (Entity entity : MC.world.getEntities()) {
            if (entity.squaredDistanceTo(MC.player) > distance) continue;
            if (entity instanceof EndCrystal) continue;
            if (entity instanceof ItemEntity) continue;
            if (entity instanceof Arrow) continue;

            if (entity.getBoundingBox().intersects(blockBox)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReplaceable(BlockPos pos) {
        return MC.world.getBlockState(pos).isReplaceable();
    }

    public static boolean isBed(Block block) {
        return block instanceof BedBlock;
    }
}
