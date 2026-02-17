package namidevelopment.kiriyaga.nami.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import namidevelopment.kiriyaga.api.event.impl.DissconectEvent;
import namidevelopment.kiriyaga.api.event.impl.EntityDeathEvent;
import namidevelopment.kiriyaga.api.event.impl.InteractionEvent;
import namidevelopment.kiriyaga.api.event.impl.OpenScreenEvent;
import namidevelopment.kiriyaga.nami.impl.feature.combat.AuraFeature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.ESPFeature;
import namidevelopment.kiriyaga.nami.impl.feature.exploits.AirPlaceFeature;
import namidevelopment.kiriyaga.nami.impl.feature.world.AutoEatFeature;
import namidevelopment.kiriyaga.nami.impl.feature.world.FastPlaceFeature;
import namidevelopment.kiriyaga.nami.impl.feature.exploits.NoHitDelayFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    public int missTime;
    @Shadow private int rightClickDelay;
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Final
    public Options options;
    @Shadow @Nullable
    public MultiPlayerGameMode gameMode;
    private int holdTicks = 0;
    @Shadow public ClientLevel level;
    private final Set<Integer> deadList = new HashSet<>();

    @Inject(method = "disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(Component reason, CallbackInfo ci) {
        DissconectEvent ev = new DissconectEvent();
        EVENT_SERVICE.post(ev);

        if (ev.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void doItemUse(CallbackInfo info) {

        AirPlaceFeature airPlace = FEATURE_SERVICE.getStorage().getByClass(AirPlaceFeature.class);
        FastPlaceFeature fastPlace = FEATURE_SERVICE.getStorage().getByClass(FastPlaceFeature.class);

        if (airPlace == null || fastPlace == null) return;

        if (airPlace.isEnabled() && airPlace.cooldown <= 0) {
            rightClickDelay = airPlace.delay.get();
            return;
        }

        if (!fastPlace.isEnabled()) return;

        if (MC == null || MC.player == null) return;

        ItemStack heldStack = MC.player.getMainHandItem();
        if (heldStack == null) return;

        Item heldItem = heldStack.getItem();
        if (heldItem == null) return;

        Identifier heldId = BuiltInRegistries.ITEM.getKey(heldItem);
        if (heldId == null) return;

        if (fastPlace.whitelist.get() && !fastPlace.whitelist.contains(heldId.toString())) return;

        if (fastPlace.blacklist.get() && fastPlace.blacklist.contains(heldId.toString())) return;

        if (holdTicks >= fastPlace.startDelay.get()) {
            rightClickDelay = fastPlace.delay.get();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        FastPlaceFeature fastPlace = FEATURE_SERVICE.getStorage().getByClass(FastPlaceFeature.class);

        if (fastPlace == null) return;

        if (fastPlace.isEnabled() && MC != null && MC.options != null && MC.options.keyUse.isDown()) {
            holdTicks++;
        } else {
            holdTicks = 0;
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        OpenScreenEvent event = new OpenScreenEvent(screen);

        EVENT_SERVICE.post(event);

        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void doAttack(CallbackInfoReturnable<Boolean> info) {
        NoHitDelayFeature noHitDelay = FEATURE_SERVICE.getStorage().getByClass(NoHitDelayFeature.class);
        if (noHitDelay != null && noHitDelay.isEnabled()) {
            missTime = 0;
        }
    }

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), cancellable = true)
    private void handleBlockBreaking(boolean bl, CallbackInfo ci) {
        InteractionEvent ev = new InteractionEvent();
        EVENT_SERVICE.post(ev);
        if (ev.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"), cancellable = true)
    private void doItemUse2(CallbackInfo ci) {
        InteractionEvent ev = new InteractionEvent();
        EVENT_SERVICE.post(ev);
        if (ev.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick(CallbackInfo ci) {
        if (player == null && level == null)
            return;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity e) {
                if (e.isDeadOrDying() && !deadList.contains(e.getId())) {
                    EntityDeathEvent ev = new EntityDeathEvent(e);
                    EVENT_SERVICE.post(ev);
                    deadList.add(e.getId());
                } else if (!e.isDeadOrDying()) {
                    deadList.remove(e.getId());
                }
            }
        }
    }


    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ESPFeature esp = FEATURE_SERVICE.getStorage().getByClass(ESPFeature.class);
        if (esp != null && esp.isEnabled() && esp.renderMode.get() == ESPFeature.RenderMode.GLOW) {
            if (ESPFeature.getESPColor(entity) != null) {
                cir.setReturnValue(true);
            }
        }
    }

    // Author @cattyngmd
    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean handleInputEvents(boolean original) {
        if (FEATURE_SERVICE.getStorage().getByClass(AutoEatFeature.class).eating.get())
            return false;
        return original;
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void handleInputEvents3(CallbackInfo info) {
        if (FEATURE_SERVICE.getStorage().getByClass(AuraFeature.class).isEnabled() && FEATURE_SERVICE.getStorage().getByClass(AuraFeature.class).multitask() && player != null && player.isUsingItem()) {
            if (!options.keyUse.isDown()) {
                gameMode.releaseUsingItem(player);
            }
        }
    }
}