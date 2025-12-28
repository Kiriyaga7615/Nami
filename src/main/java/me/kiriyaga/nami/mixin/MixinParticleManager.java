package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.event.impl.ParticleEvent;
import me.kiriyaga.nami.feature.module.impl.visuals.NoRenderModule;
import net.minecraft.block.BlockState;
import hlq.hlq;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.nami.Nami.EVENT_MANAGER;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lhlq;", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleEffect particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<hlq> ci) {
        ParticleEvent ev = new ParticleEvent(particle);

        EVENT_MANAGER.post(ev);

        if (ev.isCancelled())
            ci.cancel();
    }
}