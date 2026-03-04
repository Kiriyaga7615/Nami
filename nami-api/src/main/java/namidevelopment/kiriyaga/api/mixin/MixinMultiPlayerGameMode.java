package namidevelopment.kiriyaga.api.mixin;

import namidevelopment.kiriyaga.api.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.NamiApi.ROTATION_SERVICE;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IClientPlayerInteractionManager {

    private float originalYRot, originalXRot;

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Override
    public void updateSlot() {
        this.ensureHasSentCarriedItem();
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void interactItem1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player != MC.player) return;
        if (!ROTATION_SERVICE.getStateHandler().isRotating()) return;

        originalYRot = player.getYRot();
        originalXRot = player.getXRot();

        float spoofYaw = ROTATION_SERVICE.getStateHandler().getRotationYRot();
        float spoofPitch = ROTATION_SERVICE.getStateHandler().getRotationXRot();

        player.setYRot(spoofYaw);
        player.setXRot(spoofPitch);
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void interactItem2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player != MC.player) return;
        if (!ROTATION_SERVICE.getStateHandler().isRotating()) return;

        player.setYRot(originalYRot);
        player.setXRot(originalXRot);
    }
}
