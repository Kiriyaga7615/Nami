package namidevelopment.kiriyaga.api.mixin;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(value = LocalPlayer.class, priority = Integer.MAX_VALUE)
public abstract class MixinLocalPlayer {
    private float originalSilentXRot;
    private float originalYRot, originalXRot;
    @Shadow private float xRotLast;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void preSendMovementPackets(CallbackInfo ci) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (!ROTATION_SERVICE.getStateHandler().isRotating() || config.isFutureRotations()) {
            ROTATION_SERVICE.getStateHandler().setServerDeltaYRot(0f); // delta without rotations almost always lower then 30, its almost impossible without hacks to reach
            return;
        }

        originalYRot = MC.player.getYRot();
        originalXRot = MC.player.getXRot();

        float newYaw = ROTATION_SERVICE.getStateHandler().getRotationYRot();
        float newPitch = ROTATION_SERVICE.getStateHandler().getRotationXRot();
        MC.player.setYRot(newYaw);
        MC.player.setXRot(newPitch);

        float deltaYaw = newYaw - ROTATION_SERVICE.getStateHandler().getServerYRot();
        //float deltaPitch = newPitch - ROTATION_SERVICE.getStateHandler().getServerXRot();

        ROTATION_SERVICE.getStateHandler().setServerDeltaYRot(deltaYaw);

        ROTATION_SERVICE.getStateHandler().setServerYRot(newYaw);
        ROTATION_SERVICE.getStateHandler().setServerXRot(newPitch);
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void postSendMovementPackets(CallbackInfo ci) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (config.isFutureRotations())
            return;

        ROTATION_SERVICE.getStateHandler().setServerYRot(MC.player.getYRot());
        ROTATION_SERVICE.getStateHandler().setServerXRot(MC.player.getXRot());


        if (!ROTATION_SERVICE.getStateHandler().isRotating()) {
            return;
        }

        MC.player.setYRot(originalYRot);
        MC.player.setXRot(originalXRot);
    }

    // Do not ask me exactly why is it so weird, it just works
    // overall silent rotations sucks, another super cool bypass, works really weirdly
    // i hope it gets fucking patched in 1.22/1.23
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void sendMovementPackets1(CallbackInfo ci) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (config.getRotationMode() == RotationsFeatureConfig.RotationMode.SILENT && ROTATION_SERVICE.getStateHandler().getSilentSyncRequired()) {
            this.originalSilentXRot = MC.player.getXRot();
            this.xRotLast -= 4;
            float f = (float)((Math.random() * 2.0 - 1.0) * 0.001f);
            float f2 = Mth.clamp(this.originalSilentXRot + f, -90.0F, 90.0F);
            MC.player.setXRot(f2);
        }
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void sendMovementPackets2(CallbackInfo ci) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (config.getRotationMode() == RotationsFeatureConfig.RotationMode.SILENT && ROTATION_SERVICE.getStateHandler().getSilentSyncRequired()) {
            MC.player.setXRot(this.originalSilentXRot);
            ROTATION_SERVICE.getStateHandler().setSilentSyncRequired(false);
        }
    }
}
