package namidevelopment.kiriyaga.api.mixin;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "getLookAngle()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void onGetRotationVector(CallbackInfoReturnable<Vec3> cir) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if ((Object) this != MC.player) return;
        if (ROTATION_SERVICE == null || !ROTATION_SERVICE.getStateHandler().isRotating() || config.isFutureRotations()) return;

        float spoofYaw = ROTATION_SERVICE.getStateHandler().getRotationYRot();
        float spoofPitch = ROTATION_SERVICE.getStateHandler().getRotationXRot();

        cir.setReturnValue(((Entity) (Object) this).calculateViewVector(spoofPitch, spoofYaw));
    }
}
