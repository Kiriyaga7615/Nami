package namidevelopment.kiriyaga.nami.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.event.impl.*;
import namidevelopment.kiriyaga.nami.impl.feature.movement.NoSlowFeature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.NoRenderFeature;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    @Shadow public abstract void move(MoverType type, Vec3 movement);

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHookPre(CallbackInfo ci) {

        EVENT_SERVICE.post(new PreTickEvent());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickHookPost(CallbackInfo ci) {

        EVENT_SERVICE.post(new PostTickEvent());
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At(value = "HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        BlockPushEvent pushOutOfBlocksEvent = new BlockPushEvent();
        EVENT_SERVICE.post(pushOutOfBlocksEvent);

        if (pushOutOfBlocksEvent.isCancelled())
            ci.cancel();
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void onMove(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        MoveEvent moveEvent = new MoveEvent(movementType, movement);
        EVENT_SERVICE.post(moveEvent);

        if (moveEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        Vec3 newMovement = moveEvent.getMovement();
        if (!newMovement.equals(movement)) {
            this.move(movementType, newMovement);
            ci.cancel();
        }
    }

    // VFP has weird @Redirect in modifyInput so we can do no slow down only like this
    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void onItemUseSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = (LocalPlayer)(Object)this;

        if (!player.isUsingItem())
            return;

        ItemUseSlowEvent event = new ItemUseSlowEvent(player, player.getUseItem());

        EVENT_SERVICE.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void shouldSlowDown(CallbackInfoReturnable<Boolean> info) {
        if (FEATURE_SERVICE == null || FEATURE_SERVICE.getStorage() == null || FEATURE_SERVICE.getStorage().getByClass(NoSlowFeature.class) == null || !FEATURE_SERVICE.getStorage().getByClass(NoSlowFeature.class).isEnabled() || !FEATURE_SERVICE.getStorage().getByClass(NoSlowFeature.class).fastCrawl.get())
            return;

        boolean b = !MC.player.isVisuallyCrawling();
        if (b) return;

        info.setReturnValue(b);
    }

    @ModifyExpressionValue(method = "handlePortalTransitionEffect", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private Screen tickNausea(Screen s) {
        if (FEATURE_SERVICE == null)
            return s;

        if (FEATURE_SERVICE.getStorage().getByClass(NoRenderFeature.class).isEnabled() && FEATURE_SERVICE.getStorage().getByClass(NoRenderFeature.class).portalGui.get())
            return null;

        return s;
    }
}
