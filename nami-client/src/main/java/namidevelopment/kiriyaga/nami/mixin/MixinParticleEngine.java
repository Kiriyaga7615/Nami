package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.ParticleEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.EVENT_SERVICE;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {

    @Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("RETURN"), cancellable = true)
    private void onAddParticle(ParticleOptions particleOptions, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {

        Particle particle = cir.getReturnValue();
        ParticleEvent event = new ParticleEvent(particleOptions);
        EVENT_SERVICE.post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(null);
            return;
        }

        if (particle == null)
            return;

        cir.setReturnValue(particle);
    }
}