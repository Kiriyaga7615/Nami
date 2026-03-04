package namidevelopment.kiriyaga.api.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.core.macro.model.Macro;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import namidevelopment.kiriyaga.api.model.feature.Feature;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @ModifyArg(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;logFrameDuration(J)V"),index = 0)
    private long runTick(long frameDurationNs) {
        SERVER_SERVICE.setLastFrameDurationNs(frameDurationNs);
        return frameDurationNs;
    }

    // For futre rotations
    @Inject(method = "tick", at = @At("HEAD"))
    private void tick$exportNamiRotationsToShared(CallbackInfo ci,
                                                  @Share(namespace = "shared_rotations", value = "target_rotation")
                                                  final LocalRef<Vec2> targetRotation,

                                                  @Share(namespace = "shared_rotations", value = "target_rotation_priority")
                                                  final LocalRef<Vector2i> targetRotationPriority
    ) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (!ROTATION_SERVICE.getStateHandler().isRotating() || !config.isFutureRotations()) {
            return;
        }
        float yaw = ROTATION_SERVICE.getStateHandler().getRotationYRot();
        float pitch = ROTATION_SERVICE.getStateHandler().getRotationXRot();
        targetRotation.set(new Vec2(yaw, pitch));
        var request = ROTATION_SERVICE.getRequestHandler().getActiveRequest();
        if (request == null) {
            targetRotationPriority.set(new Vector2i(Integer.MIN_VALUE, Integer.MIN_VALUE));
            return;
        }

        Integer priority = request.priority;
        int p = (priority == null || priority == 0) ? Integer.MIN_VALUE : priority;

        targetRotationPriority.set(new Vector2i(p, p));

    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=gameRenderer"))
    private void tick$postInputTick(CallbackInfo ci,
                                    @Share(namespace = "shared_rotations", value = "target_rotation")
                                    final LocalRef<Vec2> _targetRotation) {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);

        if (!config.isFutureRotations() || _targetRotation == null || !ROTATION_SERVICE.getStateHandler().isRotating())
            return;

        Vec2 rot = _targetRotation.get();

        if (rot == null) {
            return;
        }

        ROTATION_SERVICE.getStateHandler().setServerYRot(rot.x);
        ROTATION_SERVICE.getStateHandler().setServerXRot(rot.y);
    }
}