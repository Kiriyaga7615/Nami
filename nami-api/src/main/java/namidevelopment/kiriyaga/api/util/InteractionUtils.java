package namidevelopment.kiriyaga.api.util;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.mixin.DuckMultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.BlockUtils.withinLevelHeight;
import static namidevelopment.kiriyaga.api.util.PacketUtils.sendSequencedPacket;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;
import static net.minecraft.world.InteractionHand.MAIN_HAND;
import static net.minecraft.world.InteractionHand.OFF_HAND;

public class InteractionUtils {

    private static BlockPos currentBreakingBlock = null;

    public static boolean interactWithEntity(Entity entity, Item item, boolean swapBack, boolean multitask, double range, boolean swing, boolean rotate, String rotationId) {
        if (MC.player == null || MC.gameMode == null || entity == null) return false;

        if (!multitask && MC.player.isUsingItem())
            return false;

        boolean isOffhand = false;
        if (MC.player.getOffhandItem().is(item))
            isOffhand = true;

        int slot = INVENTORY_SERVICE.getSwapHandler().findHotbarItem(stack -> stack.is(item));
        if (slot == -1 && !isOffhand)
            return false;
        
        Vec3 eyePos = MC.player.getEyePosition(1.0f);
        Vec3 closestPoint = getClosestPointToEye(eyePos, entity.getBoundingBox());
        float idealYaw = (float) getYRotToVec(MC.player, closestPoint);
        float idealPitch = (float) getXRotToVec(MC.player, closestPoint);

        if (eyePos.distanceTo(getClampClosestPoint(eyePos, entity.getBoundingBox())) > range)
            return false;

        if (rotate)
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 4, idealYaw, idealPitch));

        boolean insideBox = entity.getBoundingBox().contains(MC.player.getEyePosition());
        EntityHitResult hitResult = raycastTarget(MC.player, entity, range, ROTATION_SERVICE.getStateHandler().getServerYRot(), ROTATION_SERVICE.getStateHandler().getServerXRot());

        boolean completed = !rotate || insideBox || hitResult != null;

        if (!completed)
            return false;


        MC.gameMode.interactAt(MC.player, entity, hitResult, MAIN_HAND);
        MC.gameMode.interact(MC.player, entity, MAIN_HAND);

        if (swing)
            MC.player.swing(MAIN_HAND);


        if (!isOffhand) {
            INVENTORY_SERVICE.getSwapHandler().attemptSwitch(slot, swapBack);

            MC.gameMode.interactAt(MC.player, entity, hitResult, MAIN_HAND);
            MC.gameMode.interact(MC.player, entity, MAIN_HAND);

            if (swing)
                MC.player.swing(MAIN_HAND);
        }
        else {
            MC.gameMode.interactAt(MC.player, entity, hitResult, OFF_HAND);
            MC.gameMode.interact(MC.player, entity, OFF_HAND);

            if (swing)
                MC.player.swing(OFF_HAND);
        }

        return true;
    }

    public static void startUsingItem() {
        startUsingItem(MAIN_HAND);
    }

    public static void startUsingItem(InteractionHand hand) {
        if (!MC.player.isUsingItem())
            MC.player.startUsingItem(hand);
    }

    public static void stopUsingItem() {
        if (MC.player.isUsingItem())
            MC.player.releaseUsingItem();
    }

    // TODO: figure out how to place on interactable blocks without manually sneaking
    public static boolean placeBlock(BlockPos pos, Item item, boolean swapBack, double range, boolean rotate, boolean strictDirection, boolean simulate, boolean swing, String rotationId, boolean multitask) {
        if (!MC.level.getBlockState(pos).canBeReplaced())
            return false;

        if (!withinLevelHeight(pos))
            return false;

        if (!multitask && MC.player.isUsingItem())
            return false;

        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(pos);

        if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > range)
            return false;

        boolean isOffhand = false;
        if (MC.player.getOffhandItem().is(item))
            isOffhand = true;

        int slot = INVENTORY_SERVICE.getSwapHandler().findHotbarItem(stack -> stack.is(item));
        if (slot == -1 && !isOffhand)
            return false;

        List<Direction> directions = getBlockPlaceDir(pos);

        if (directions.isEmpty()) return false;

        Direction clickFace = null;
        BlockPos neighbor = null;

        // Simplified grim v2 PlacePosition check
        if (strictDirection) {
        for (Direction dir : directions) {
            BlockPos n = pos.relative(dir.getOpposite());
            boolean flag = switch (dir) {  // https://github.com/GrimAnticheat/Grim/blob/fb926ab0fbca081ad765389c541880a4a435fabb/common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/PositionPlace.java#L49
                case NORTH -> eyePos.z <= n.getZ() + 1e-3;
                case SOUTH -> eyePos.z >= n.getZ() + 1 - 1e-3;
                case WEST  -> eyePos.x <= n.getX() + 1e-3;
                case EAST  -> eyePos.x >= n.getX() + 1 - 1e-3;
                case DOWN  -> eyePos.y <= n.getY() + 1e-3;
                case UP    -> eyePos.y >= n.getY() + 1 - 1e-3;
            };

            if (flag) {
                clickFace = dir;
                neighbor = n;
                break;
            }
        }

        if (clickFace == null) return false;
        } else {
            clickFace = directions.get(0);
            neighbor = pos.relative(clickFace.getOpposite());
        }

        Vec3 playerPos = MC.player.position();
        double offX = playerPos.x - Math.floor(playerPos.x);
        double offY = playerPos.y - Math.floor(playerPos.y);
        double offZ = playerPos.z - Math.floor(playerPos.z);
        offX = Mth.clamp(offX, 0.2, 0.8);
        offY = Mth.clamp(offY, 0.2, 0.8);
        offZ = Mth.clamp(offZ, 0.2, 0.8);
        Vec3 hitVec = Vec3.atCenterOf(neighbor);

        switch (clickFace) {
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

        BlockHitResult hitResult = new BlockHitResult(hitVec, clickFace, neighbor, false);
        boolean canPlace = true;

        if (rotate) {
            float yaw = (float) getYRotToVec(MC.player, neighbor.getCenter());
            float pitch = (float) getXRotToVec(MC.player, neighbor.getCenter());

         //   if (getDefaultRotationMode() == RotationFeature.RotationMode.SILENT)
                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 8, yaw, pitch));
           // else
             //   ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 8, MC.player, hitVec));

            //canPlace = ROTATION_SERVICE.getRequestHandler().isCompleted(rotationId);

            // for some reason grim checks if you look at block, you gonna place, not on a block you click (i see logic here but still)
            AABB b = new AABB(neighbor);
            boolean insideBox = b.contains(MC.player.getEyePosition());

            EntityHitResult serverCheck = raycastAABBFromPlayer(
                    MC.player,
                    b,
                    range,
                    ROTATION_SERVICE.getStateHandler().getServerYRot(),
                    ROTATION_SERVICE.getStateHandler().getServerXRot()
            );


            canPlace = insideBox || serverCheck != null;
        }

        boolean result = false;
        if (canPlace) {

            if (!isOffhand) {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(slot, swapBack);

                if (simulate)
                    MC.gameMode.useItemOn(MC.player, MAIN_HAND, hitResult);
                else
                    sendSequencedPacket(id -> new ServerboundUseItemOnPacket(MAIN_HAND, hitResult, id));

                if (swing)
                    MC.player.swing(MAIN_HAND);

                result = true;
            }
            else {
                if (simulate)
                    MC.gameMode.useItemOn(MC.player, OFF_HAND, hitResult);
                else
                    sendSequencedPacket(id -> new ServerboundUseItemOnPacket(OFF_HAND, hitResult, id));

                if (swing)
                    MC.player.swing(OFF_HAND);

                result = true;
            }
        }

        return result;
    }

    public static boolean interactBlockAt(BlockPos pos, Item item, Direction direction, boolean swapBack, boolean multitask, double range, boolean rotate, boolean strictDirection, boolean simulate, boolean swing, String rotationId) {
        Vec3 eyePos = MC.player.getEyePosition();
        Vec3 playerPos = MC.player.position();
        Direction clickFace = Direction.UP;

        AABB blockBox = new AABB(pos);

        if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > range)
            return false;

        if (!multitask && MC.player.isUsingItem())
            return false;

        boolean isOffhand = false;
        if (MC.player.getOffhandItem().is(item))
            isOffhand = true;

        int slot = INVENTORY_SERVICE.getSwapHandler().findHotbarItem(stack -> stack.is(item));
        if (slot == -1 && !isOffhand)
            return false;

        double offX = playerPos.x - Math.floor(playerPos.x);
        double offY = playerPos.y - Math.floor(playerPos.y);
        double offZ = playerPos.z - Math.floor(playerPos.z);
        offX = Mth.clamp(offX, 0.2, 0.8);
        offY = Mth.clamp(offY, 0.2, 0.8);
        offZ = Mth.clamp(offZ, 0.2, 0.8);
        Vec3 hitVec = Vec3.atCenterOf(pos);

        if (strictDirection)
            clickFace = getBlockInteractDir(pos);

        if (direction != null)
            clickFace = direction;

        switch (clickFace) {
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

        if (strictDirection) {
            boolean flag = switch (clickFace) {
                case NORTH -> eyePos.z <= pos.getZ() + 1e-3;
                case SOUTH -> eyePos.z >= pos.getZ() + 1 - 1e-3;
                case WEST  -> eyePos.x <= pos.getX() + 1e-3;
                case EAST  -> eyePos.x >= pos.getX() + 1 - 1e-3;
                case DOWN  -> eyePos.y <= pos.getY() + 1e-3;
                case UP    -> eyePos.y >= pos.getY() + 1 - 1e-3;
            };
            if (!flag) {
             //   CHAT_SERVICE.sendRaw("interactBlockAt: failed strictDirection check");
                return false;
            }
        }

        BlockHitResult hit = new BlockHitResult(hitVec, clickFace, pos, false);

        boolean canInteract = true;

        if (rotate) {
            float yaw = (float) getYRotToVec(MC.player, pos.getCenter());
            float pitch = (float) getXRotToVec(MC.player, pos.getCenter());

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 8, yaw, pitch));
            // else
            //   ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 8, MC.player, hitVec));

            //canPlace = ROTATION_SERVICE.getRequestHandler().isCompleted(rotationId);

            // for some reason grim checks if you look at block, you gonna place, not on a block you click (i see logic here but still)
            AABB b = new AABB(pos);
            boolean insideBox = b.contains(MC.player.getEyePosition());

            EntityHitResult serverCheck = raycastAABBFromPlayer(
                    MC.player,
                    b,
                    range,
                    ROTATION_SERVICE.getStateHandler().getServerYRot(),
                    ROTATION_SERVICE.getStateHandler().getServerXRot()
            );


            canInteract = insideBox || serverCheck != null;
        }

        if (!canInteract) {
          //  CHAT_SERVICE.sendRaw("interactBlockAt: rotation incomplete");
            return false;
        }

        if (!isOffhand) {
            INVENTORY_SERVICE.getSwapHandler().attemptSwitch(slot, swapBack);

            if (simulate)
                MC.gameMode.useItemOn(MC.player, MAIN_HAND, hit);
            else
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(MAIN_HAND, hit, id));

            if (swing)
                MC.player.swing(MAIN_HAND);
        } else {
            if (simulate)
                MC.gameMode.useItemOn(MC.player, OFF_HAND, hit);
            else
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(OFF_HAND, hit, id));

            if (swing)
                MC.player.swing(OFF_HAND);
        }

        //CHAT_SERVICE.sendRaw("interactBlockAt: success");
        return true;
    }

    public static Direction getBlockInteractDir(BlockPos blockPos) {
        Vec3 playerPos = MC.player.getEyePosition();
        Vec3 blockCenter = Vec3.atCenterOf(blockPos);

        double dx = playerPos.x - blockCenter.x;
        double dy = playerPos.y - blockCenter.y;
        double dz = playerPos.z - blockCenter.z;

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        if (absX >= absY && absX >= absZ) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY >= absX && absY >= absZ) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public static List<Direction> getBlockPlaceDir(BlockPos blockPos) {
        List<Direction> dirs = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = blockPos.relative(direction);
            BlockState state = MC.level.getBlockState(neighborPos);

            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            Direction opposite = direction.getOpposite();

            dirs.add(opposite);
        }
        return dirs;
    }

    @Deprecated
    public static void airPlace(BlockHitResult target, boolean grim, boolean swing) {
        if (grim) {
            MC.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));

            MC.gameMode.useItemOn(MC.player, InteractionHand.OFF_HAND, target);
            if (swing)
                MC.player.swing(InteractionHand.MAIN_HAND, false);

            MC.getConnection().send(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
            MC.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        } else {
            MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, target);
            if (swing)
                MC.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static boolean airPlace(BlockPos pos, Item item, boolean swapBack, double range, boolean rotate, boolean grim, boolean simulate, boolean swing, String rotationId, boolean multitask) {
        return airPlace(pos, Direction.UP, item, swapBack, range, rotate, grim, simulate, swing, rotationId, multitask);
    }

        public static boolean airPlace(BlockPos pos, Direction direction, Item item, boolean swapBack, double range, boolean rotate, boolean grim, boolean simulate, boolean swing, String rotationId, boolean multitask) {
        if (!MC.level.getBlockState(pos).canBeReplaced())
            return false;

        if (!withinLevelHeight(pos))
            return false;

        if (!multitask && MC.player.isUsingItem())
            return false;

        boolean isOffhand = MC.player.getOffhandItem().is(item);

        if (grim)
            isOffhand = false;

        int slot = INVENTORY_SERVICE.getSwapHandler().findHotbarItem(stack -> stack.is(item));
        if (slot == -1 && !isOffhand)
            return false;

        Vec3 eyePos = MC.player.getEyePosition();
        Vec3 center = pos.getCenter();
        AABB blockBox = new AABB(pos);

        if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > range)
            return false;

        boolean canPlace = true;
        if (rotate) {
            float yaw = (float) getYRotToVec(MC.player, center);
            float pitch = (float) getXRotToVec(MC.player, center);
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(rotationId, 8, yaw, pitch));

            boolean insideBox = blockBox.contains(MC.player.getEyePosition());

            EntityHitResult serverCheck = raycastAABBFromPlayer(MC.player, blockBox, range, ROTATION_SERVICE.getStateHandler().getServerYRot(), ROTATION_SERVICE.getStateHandler().getServerXRot());


            canPlace = insideBox || serverCheck != null;
        }

        BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, pos, false);

        boolean result = false;

        if (canPlace) {
            if (!isOffhand) {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(slot, swapBack);

                if (grim) {
                    MC.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));

                    if (simulate)
                        MC.gameMode.useItemOn(MC.player, InteractionHand.OFF_HAND, hitResult);
                    else
                        sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND, hitResult, id));

                    if (swing)
                        MC.player.swing(InteractionHand.MAIN_HAND, false);

                    MC.getConnection().send(new ServerboundSwingPacket(InteractionHand.OFF_HAND));

                    MC.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));

                    result = true;
                } else {
                    if (simulate)
                        MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, hitResult);
                    else
                        sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, id));

                    if (swing)
                        MC.player.swing(InteractionHand.MAIN_HAND);

                    result = true;
                }
            } else {
                    if (simulate)
                        MC.gameMode.useItemOn(MC.player, InteractionHand.OFF_HAND, hitResult);
                    else
                        sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND, hitResult, id));

                    if (swing)
                        MC.player.swing(InteractionHand.OFF_HAND);

                    result = true;
            }
        }
        return result;
    }

    public static boolean breakBlock(BlockPos pos, double range, boolean rotate, boolean swing, boolean grim, boolean strictDirection, String rotationId) {
        if (MC.player == null || MC.gameMode == null)
            return false;

        //CHAT_SERVICE.sendRaw(((ClientPlayerInteractionSERVICEAccessor) MC.interactionSERVICE).getBlockBreakingCooldown()+"");

        if (BlockUtils.isBlockAirOrFluid(pos)) {
            if (currentBreakingBlock != null && currentBreakingBlock.equals(pos)) {
                currentBreakingBlock = null;
            }
            return false;
        }

        Vec3 eyePos = MC.player.getEyePosition();
        AABB blockBox = new AABB(pos);

        if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > range)
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
            Vec3 center = Vec3.atCenterOf(pos).add(
                    direction.getStepX() * 0.5,
                    direction.getStepY() * 0.5,
                    direction.getStepZ() * 0.5
            );

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                    rotationId,
                    3,
                    (float) getYRotToVec(MC.player, center),
                    (float) getXRotToVec(MC.player, center)
            ));

            if (!ROTATION_SERVICE.getRequestHandler().isCompleted(rotationId)) {
                return false;
            }
        }

        boolean success = MC.gameMode.continueDestroyBlock(pos, direction);
        if (swing)
            MC.player.swing(MAIN_HAND);

        if (grim && ((DuckMultiPlayerGameMode) MC.gameMode).getDestroyDelay() != 0) // https://github.com/GrimAnticheat/Grim/blob/def21633e2bfa52e2dd4afdf91aec3c0ec6d14e7/common/src/main/java/ac/grim/grimac/checks/impl/breaking/FastBreak.java#L28
            return false;

        if (BlockUtils.isBlockAirOrFluid(pos)) {  // somehow it happens https://github.com/GrimAnticheat/Grim/blob/def21633e2bfa52e2dd4afdf91aec3c0ec6d14e7/common/src/main/java/ac/grim/grimac/checks/impl/breaking/AirLiquidBreak.java#L18
            currentBreakingBlock = null;
            return false;
        }

        if (currentBreakingBlock == null || !currentBreakingBlock.equals(pos)) {
            currentBreakingBlock = pos;
            MC.gameMode.startDestroyBlock(pos, direction);
        } else {
            if (!success) {
                currentBreakingBlock = null;
                return false;
            }
        }

        return true;
    }
}
