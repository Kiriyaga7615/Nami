package namidevelopment.kiriyaga.api.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RotationUtils {
    public static int wrapDegrees(int angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    public static float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    public static float yRotDifference(float targetYaw, float currentYaw) {
        float diff = (targetYaw - currentYaw) % 360f;
        if (diff >= 180f) diff -= 360f;
        if (diff < -180f) diff += 360f;
        return diff;
    }

    public static float alignYRot(float playerYaw, float currentYaw) {
        int wraps = Math.round((currentYaw - playerYaw) / 360f);
        return playerYaw + wraps * 360f;
    }

    public static Vec3 getEntityCenter(Entity entity) {
        AABB box = entity.getBoundingBox();
        double centerX = box.minX + (box.getXsize() / 2);
        double centerY = box.minY + (box.getYsize() / 2);
        double centerZ = box.minZ + (box.getZsize() / 2);
        return new Vec3(centerX, centerY, centerZ);
    }

    public static Vec3 getClosestPointToEye(Vec3 eyePos, AABB box) {
        double x = eyePos.x;
        double y = eyePos.y;
        double z = eyePos.z;

        final double VEC = 1.0 / 16.0;
        final double EPS = 1e-9;

        if (eyePos.x < box.minX) x = box.minX;
        else if (eyePos.x > box.maxX) x = box.maxX;

        if (eyePos.y < box.minY) y = box.minY;
        else if (eyePos.y > box.maxY) y = box.maxY;

        if (eyePos.z < box.minZ) z = box.minZ;
        else if (eyePos.z > box.maxZ) z = box.maxZ;

        // somehow, minecraft aabb corner/sides does not intersects with raycast, so we need to move result vec inside of aabb
        if (Math.abs(x - box.minX) < EPS) {
            x = Math.min(box.minX + VEC, box.maxX - EPS);
        } else if (Math.abs(x - box.maxX) < EPS) {
            x = Math.max(box.maxX - VEC, box.minX + EPS);
        }

        if (Math.abs(z - box.minZ) < EPS) {
            z = Math.min(box.minZ + VEC, box.maxZ - EPS);
        } else if (Math.abs(z - box.maxZ) < EPS) {
            z = Math.max(box.maxZ - VEC, box.minZ + EPS);
        }

        return new Vec3(x, y, z);
    }

    public static Vec3 getClampClosestPoint(Vec3 eyePos, AABB box) {
        double x = Mth.clamp(eyePos.x, box.minX, box.maxX);
        double y = Mth.clamp(eyePos.y, box.minY, box.maxY);
        double z = Mth.clamp(eyePos.z, box.minZ, box.maxZ);

        return new Vec3(x, y, z);
    }

    public static Vec3 predictMotion(LivingEntity player) {
        if (player == null) return Vec3.ZERO;

        PredictMovementUtils.PredictedEntity predicted = PredictMovementUtils.predict(
                PredictMovementUtils.toPredicted(player), 1, t -> Vec3.ZERO
        );

        return predicted.getEyePos();
    }

    public static int getYRotToVec(Entity from, Vec3 to) {
        double dx = to.x - from.getX();
        double dz = to.z - from.getZ();
        return wrapDegrees((int) Math.round(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
    }

    public static int getXRotToVec(Entity from, Vec3 to) {
        Vec3 eyePos = from.getEyePosition();
        double dx = to.x - eyePos.x;
        double dy = to.y - eyePos.y;
        double dz = to.z - eyePos.z;
        return (int) Math.round(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    public static float getYRotToVec(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    public static float getXRotToVec(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    public static EntityHitResult raycastTarget(Vec3 eyePos, Entity target, double reach, float yRot, float xRot) {
        Vec3 look = getLookVectorFromYRotXRot(yRot, xRot);
        Vec3 reachEnd = eyePos.add(look.scale(reach));

        AABB targetBox = target.getBoundingBox();

        if (targetBox.clip(eyePos, reachEnd).isPresent()) {
            return new EntityHitResult(target);
        }

        return null;
    }

    public static EntityHitResult raycastTarget(Entity player, Entity target, double reach, float yRot, float xRot) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 look = getLookVectorFromYRotXRot(yRot, xRot);
        Vec3 reachEnd = eyePos.add(look.scale(reach));

        AABB targetBox = target.getBoundingBox();

        if (targetBox.clip(eyePos, reachEnd).isPresent()) {
            return new EntityHitResult(target);
        }

        return null;
    }

    public static EntityHitResult raycastAABB(Vec3 start, Vec3 end, AABB box) {
        Optional<Vec3> clipped = box.clip(start, end);
        return clipped.map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
    }

    public static EntityHitResult raycastAABBFromPlayer(Entity player, AABB box, double reach, float yaw, float pitch) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = getLookVectorFromYRotXRot(yaw, pitch);
        Vec3 reachEnd = eyePos.add(lookVec.scale(reach));

        return raycastAABB(eyePos, reachEnd, box);
    }

    public static Vec3 getLookVectorFromYRotXRot(float yaw, float pitch) {
        float f = (float) Math.cos(-yaw * 0.017453292F - Math.PI);
        float g = (float) Math.sin(-yaw * 0.017453292F - Math.PI);
        float h = - (float) Math.cos(-pitch * 0.017453292F);
        float i = (float) Math.sin(-pitch * 0.017453292F);
        return new Vec3(g * h, i, f * h);
    }
}
