package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.BreakBlockEvent;
import namidevelopment.kiriyaga.api.event.impl.PlaceBlockEvent;
import namidevelopment.kiriyaga.api.event.impl.StartBreakingBlockEvent;
import namidevelopment.kiriyaga.nami.impl.feature.world.NoBreakDelayFeature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Shadow
    private int destroyDelay;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> call){
        StartBreakingBlockEvent ev = new StartBreakingBlockEvent(blockPos, direction);
        EVENT_SERVICE.post(ev);

        if (ev.isCancelled())
            call.cancel();
    }

    @Inject(method = "useItemOn", at = @At(value = "HEAD"), cancellable = true)
    private void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlaceBlockEvent interactBlockEvent = new PlaceBlockEvent(player, hand, hitResult);
        EVENT_SERVICE.post(interactBlockEvent);

        if (interactBlockEvent.isCancelled()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At(value = "HEAD"), cancellable = true)
    private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BreakBlockEvent breakBlockEvent = new BreakBlockEvent(pos);
        EVENT_SERVICE.post(breakBlockEvent);
        if (breakBlockEvent.isCancelled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void disableBreakCooldown(CallbackInfoReturnable<Boolean> cir) {
        if (FEATURE_SERVICE.getStorage() == null) return;

        //CHAT_SERVICE.sendRaw(""+this.blockBreakingCooldown);

        NoBreakDelayFeature noBreakDelay = FEATURE_SERVICE.getStorage().getByClass(NoBreakDelayFeature.class);
        if (noBreakDelay != null && noBreakDelay.isEnabled()) {
            this.destroyDelay = 0;
        }
    }
}
