package namidevelopment.kiriyaga.api.mixin;

import namidevelopment.kiriyaga.api.event.impl.KeyInputEvent;
import namidevelopment.kiriyaga.api.event.impl.MouseHandlerEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.api.NamiApi.EVENT_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@Mixin(value = MouseHandler.class)
public abstract class MixinMouseHandler {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window != MC.getWindow().handle()) return;

        MouseHandlerEvent ev =new MouseHandlerEvent(info.button(), action, info.modifiers());

        EVENT_SERVICE.post(ev);

        if (ev.isCancelled()) {
            ci.cancel();
        }
    }
}
