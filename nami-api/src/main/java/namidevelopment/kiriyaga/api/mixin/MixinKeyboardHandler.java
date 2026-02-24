package namidevelopment.kiriyaga.api.mixin;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.event.impl.KeyInputEvent;
import namidevelopment.kiriyaga.api.event.impl.KeyboardHandlerEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@Mixin(value = KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void keyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (window != MC.getWindow().handle()) return;

        KeyboardHandlerEvent ev = new KeyboardHandlerEvent(keyEvent.key(), keyEvent.scancode(), action, keyEvent.modifiers());

        EVENT_SERVICE.post(ev);

        if (ev.isCancelled()) {
            ci.cancel();
        }
    }
}
