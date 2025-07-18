package me.kiriyaga.nami.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.kiriyaga.nami.feature.module.impl.client.RotationManagerModule;
import me.kiriyaga.nami.feature.module.impl.movement.ElytraFlyModule;
import me.kiriyaga.nami.feature.module.impl.movement.HighJumpModule;
import me.kiriyaga.nami.feature.module.impl.movement.NoJumpDelayModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.nami.Nami.*;


@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    private float originalYaw;
    @Shadow
    private int jumpingCooldown;
    private float originalPitch;

    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void travelPreHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class).moveFix.get())
            return;

        originalYaw = this.getYaw();
        originalPitch = this.getPitch();

        float spoofYaw = ROTATION_MANAGER.getRotationYaw();
        float spoofPitch = ROTATION_MANAGER.getRotationPitch();

        this.setYaw(spoofYaw);
        this.setPitch(spoofPitch);
        ((LivingEntityAccessor) this).setBodyYaw(spoofYaw);
        ((LivingEntityAccessor) this).setHeadYaw(spoofYaw);
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void travelPostHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class).moveFix.get())
            return;

        this.setYaw(originalYaw);
        this.setPitch(originalPitch);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), ordinal = 0)
    private Vec3d modifyMovementInput(Vec3d movementInput) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class).moveFix.get()) return movementInput;
        if (movementInput.lengthSquared() < 1e-4) return movementInput;

        float realYaw = originalYaw;
        float spoofYaw = ROTATION_MANAGER.getRotationYaw();

        float clampedSpoofYaw = findClosestValidYaw(spoofYaw);

        Vec3d globalMovement = localToGlobal(movementInput, realYaw);

        Vec3d clampedLocalMovement = globalToLocal(globalMovement, clampedSpoofYaw);

        //CHAT_MANAGER.sendRaw("modifyMovementInput Rotating: " +ROTATION_MANAGER.isRotating() + "\n spoof yaw: " + spoofYaw + "\nrealyaw: " + originalYaw + "\ninput: "+clampedLocalMovement);

        return clampedLocalMovement;
    }

    private float findClosestValidYaw(float yaw) {
        float[] allowedYawAngles = new float[]{0, 45, 90, 135, 180, 225, 270, 315};
        float bestYaw = allowedYawAngles[0];
        float minDiff = Float.MAX_VALUE;
        for (float allowedYaw : allowedYawAngles) {
            float diff = Math.abs(((allowedYaw - yaw + 540f) % 360f) - 180f);
            if (diff < minDiff) {
                minDiff = diff;
                bestYaw = allowedYaw;
            }
        }
        return bestYaw;
    }

    private Vec3d localToGlobal(Vec3d localVec, float yaw) {
        double rad = Math.toRadians(yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = localVec.x * cos - localVec.z * sin;
        double z = localVec.z * cos + localVec.x * sin;

        return new Vec3d(x, localVec.y, z);
    }

    private Vec3d globalToLocal(Vec3d globalVec, float yaw) {
        double rad = Math.toRadians(yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = globalVec.x * cos + globalVec.z * sin;
        double z = globalVec.z * cos - globalVec.x * sin;

        return new Vec3d(x, globalVec.y, z);
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float jumpFix(float originalYaw) {
        if ((Object)this != MinecraftClient.getInstance().player) return originalYaw;
        return ROTATION_MANAGER.getRotationYaw();
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 2, shift = At.Shift.BEFORE))
    private void doItemUse(CallbackInfo info) {
        if (MODULE_MANAGER.getStorage().getByClass(NoJumpDelayModule.class).isEnabled()) {
            jumpingCooldown = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "isGliding()Z", cancellable = true)
    private void isGlidingZ(CallbackInfoReturnable<Boolean> cir) {
        ElytraFlyModule elytraFlyModule = MODULE_MANAGER.getStorage().getByClass(ElytraFlyModule.class);
        if (MC.player != null && elytraFlyModule.mode.get() == ElytraFlyModule.FlyMode.bounce && (Object)this == MinecraftClient.getInstance().player && elytraFlyModule.isEnabled() && MC.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
            cir.setReturnValue(true);
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        HighJumpModule mod = MODULE_MANAGER.getStorage().getByClass(HighJumpModule.class);

        if (!mod.isEnabled())
            return;

        float jumpBoost = mod.height.get().floatValue();

        Vec3d vel = this.getVelocity();
        this.setVelocity(vel.x, Math.max(jumpBoost, vel.y), vel.z);

        if (this.isSprinting()) {
            float yawRad = this.getYaw() * 0.017453292F;
            this.addVelocityInternal(new Vec3d(-MathHelper.sin(yawRad) * 0.2, 0.0, MathHelper.cos(yawRad) * 0.2));
        }

        this.velocityDirty = true;

        ci.cancel();
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void setSprinting(boolean sprinting, CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player) return;

        if (!ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class).sprintFix.get())
            return;

        if (sprinting && MC.player.input != null) {
            Vec2f movement = MC.player.input.getMovementInput();
            float forward = movement.x;
            float sideways = movement.y;

            //CHAT_MANAGER.sendRaw("forward " + forward + ", sideways " + sideways);

            if (forward == 0 && sideways == 0) {
                ci.cancel();
                super.setSprinting(false);
                //CHAT_MANAGER.sendRaw("сancelled sprint no movement");
                return;
            }

            float spoofYaw = ROTATION_MANAGER.getRotationYaw();
            float realYaw = MC.player.getYaw();

            Vec3d localMovement = new Vec3d(sideways, 0, forward);
            Vec3d globalMovement = localToGlobal(localMovement, realYaw);

            //CHAT_MANAGER.sendRaw("global movement: X" + globalMovement.x + " Z" + globalMovement.z);

            Vec3d localRelativeToSpoof = globalToLocal(globalMovement, spoofYaw);

            //CHAT_MANAGER.sendRaw("local relative: X" + localRelativeToSpoof.x + " Z" + localRelativeToSpoof.z);

            double moveAngleRad = Math.atan2(localRelativeToSpoof.z, localRelativeToSpoof.x);
            float moveAngleDeg = (float) Math.toDegrees(moveAngleRad);
            moveAngleDeg = MathHelper.wrapDegrees(moveAngleDeg);

            //CHAT_MANAGER.sendRaw("move angle deg relative to spoof " + moveAngleDeg);


            if (Math.abs(moveAngleDeg) > 45f) {
                ci.cancel();
                super.setSprinting(false);
               // CHAT_MANAGER.sendRaw("cancelled sprint because angle more then 45");
            }
        }
    }
}
