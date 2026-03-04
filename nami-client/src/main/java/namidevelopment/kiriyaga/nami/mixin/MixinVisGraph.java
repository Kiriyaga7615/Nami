package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.VisGraphEvent;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.api.NamiApi.EVENT_SERVICE;

@Mixin(VisGraph.class)
public abstract class MixinVisGraph {

    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void setOpaque(BlockPos blockPos, CallbackInfo ci) {
        VisGraphEvent ev = new VisGraphEvent();
        EVENT_SERVICE.post(ev);
        if (ev.isCancelled())
            ci.cancel();
    }
}