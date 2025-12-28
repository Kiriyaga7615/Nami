package me.kiriyaga.nami.util;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import java.util.function.IntFunction;
import net.minecraft.world.entity.LivingEntity;
public final class PredictMovementUtils {

    private PredictMovementUtils() {} // elytra is fucked up here

    public static class PredictedEntity {
        public Vec3 pos;
        public Vec3 velocity;
        public float yaw, pitch;
        public boolean onGround;
        public float standingEyeHeight;

        public boolean isGliding;
        public boolean isClimbing;
        public boolean horizontalCollision;
        public boolean wasInPowderSnow;

        public boolean hasLevitation;
        public int levitationAmplifier;
        public boolean hasSlowFalling;

        public boolean isSprinting;
        public boolean touchingWater;
        public boolean inLava;

        public double movementSpeed;
        public double waterMovementEfficiency;
        public double finalGravity;
        public double swimHeight;

        public PredictedEntity(Vec3 pos, Vec3 velocity, float yaw, float pitch, boolean onGround, float eyeHeight) {
            this.pos = pos;
            this.velocity = velocity;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.standingEyeHeight = eyeHeight;
        }

        public Vec3 getEyePos() {
            return pos.add(0, standingEyeHeight, 0);
        }
    }

    public static PredictedEntity toPredicted(LivingEntity entity) {
        PredictedEntity p = new PredictedEntity(
                entity.getEntityPos(),
                entity.getVelocity(),
                entity.getYaw(),
                entity.getPitch(),
                entity.isOnGround(),
                entity.getStandingEyeHeight()
        );

        p.isGliding = entity.isGliding();
        p.isClimbing = entity.isClimbing();
        p.horizontalCollision = entity.horizontalCollision;
        p.wasInPowderSnow = entity.wasInPowderSnow;

        p.hasLevitation = entity.hasStatusEffect(MobEffects.LEVITATION);
        p.levitationAmplifier = entity.hasStatusEffect(MobEffects.LEVITATION) ? entity.getStatusEffect(MobEffects.LEVITATION).getAmplifier() : 0;
        p.hasSlowFalling = entity.hasStatusEffect(MobEffects.SLOW_FALLING);

        p.isSprinting = entity.isSprinting();
        p.touchingWater = entity.isTouchingWater();
        p.inLava = entity.isInLava();

        p.movementSpeed = entity.getMovementSpeed();
        p.waterMovementEfficiency = (float) entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        p.finalGravity = entity.getFinalGravity();
        p.swimHeight = entity.getSwimHeight();

        return p;
    }

    public static PredictedEntity predict(PredictedEntity entity, int ticks, IntFunction<Vec3> inputProvider) {
        PredictedEntity fake = copy(entity);

        for (int t = 0; t < ticks; t++) {
            Vec3 input = inputProvider != null ? inputProvider.apply(t) : Vec3.ZERO;
            travel(fake, input);
        }

        return fake;
    }

    private static void travel(PredictedEntity e, Vec3 input) {
        if (e.onGround) {
            travelOnGround(e, input);
        } else if (e.touchingWater || e.inLava) {
            travelInFluid(e, input);
        } else if (e.isGliding) {
            travelGliding(e, input);
        } else {
            travelMidAir(e, input);
        }
    }

    private static void travelOnGround(PredictedEntity e, Vec3 input) {
        float friction = 0.91F; // TODO fix this
        float speed = (float) e.movementSpeed;

        Vec3 move = input.multiply(speed, 0, speed);

        double velX = e.velocity.x * friction + move.x;
        double velZ = e.velocity.z * friction + move.z;

        double velY = e.velocity.y;
        if (!e.onGround) {
            velY -= getEffectiveGravity(e);
        } else {
            velY = 0;
        }

        e.velocity = new Vec3(velX, velY, velZ);
        e.pos = e.pos.add(e.velocity);
    }

    private static void travelMidAir(PredictedEntity e, Vec3 input) {
        Vec3 move = input.multiply(e.movementSpeed, 1, e.movementSpeed);

        double yVel = e.velocity.y;

        if (e.hasLevitation) {
            yVel += 0.05 * (e.levitationAmplifier + 1);
        } else if (!e.onGround) {
            yVel -= getEffectiveGravity(e);
        }

        e.velocity = new Vec3(move.x, yVel, move.z);
        e.pos = e.pos.add(e.velocity);
    }

    private static void travelInFluid(PredictedEntity e, Vec3 input) {
        float speedMultiplier = e.touchingWater ? 0.8f : 0.5f;
        Vec3 move = input.multiply(speedMultiplier, 0.8, speedMultiplier);
        double yVel = e.velocity.y - (getEffectiveGravity(e) / 4.0);

        e.velocity = new Vec3(move.x, yVel, move.z);
        e.pos = e.pos.add(e.velocity);
    }

    private static void travelGliding(PredictedEntity e, Vec3 input) {
        double pitchRad = e.pitch * 0.017453292;
        double horizontalSpeed = Math.sqrt(e.velocity.x * e.velocity.x + e.velocity.z * e.velocity.z);

        double glideY = -getEffectiveGravity(e) + horizontalSpeed * -Math.sin(pitchRad) * 0.04;
        e.velocity = new Vec3(e.velocity.x + input.x, glideY, e.velocity.z + input.z);
        e.pos = e.pos.add(e.velocity.multiply(0.99, 0.98, 0.99));
    }

    private static double getEffectiveGravity(PredictedEntity e) {
        return (e.hasSlowFalling ? Math.min(e.finalGravity, 0.01) : e.finalGravity);
    }

    private static PredictedEntity copy(PredictedEntity e) {
        PredictedEntity c = new PredictedEntity(e.pos, e.velocity, e.yaw, e.pitch, e.onGround, e.standingEyeHeight);
        c.isGliding = e.isGliding;
        c.isClimbing = e.isClimbing;
        c.horizontalCollision = e.horizontalCollision;
        c.wasInPowderSnow = e.wasInPowderSnow;
        c.hasLevitation = e.hasLevitation;
        c.levitationAmplifier = e.levitationAmplifier;
        c.hasSlowFalling = e.hasSlowFalling;
        c.isSprinting = e.isSprinting;
        c.touchingWater = e.touchingWater;
        c.inLava = e.inLava;
        c.movementSpeed = e.movementSpeed;
        c.waterMovementEfficiency = e.waterMovementEfficiency;
        c.finalGravity = e.finalGravity;
        c.swimHeight = e.swimHeight;
        return c;
    }
}
