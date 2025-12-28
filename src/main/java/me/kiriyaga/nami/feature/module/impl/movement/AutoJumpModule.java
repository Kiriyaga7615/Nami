package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import 	com.mojang.blaze3d.platform.InputConstants;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class AutoJumpModule extends Module {

    public AutoJumpModule() {
        super("AutoJump", "Automatically makes you jump.", ModuleCategory.of("Movement"),"autojump");
    }

    @Override
    public void onDisable() {
        setJumpHeld(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdateEvent(PreTickEvent event) {
        setJumpHeld(true);
    }

    private void setJumpHeld(boolean held) {
        KeyMapping jumpKey = MC.options.jumpKey;
        InputConstants.Key boundKey = ((KeyBindingAccessor) jumpKey).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputUtil.isKeyPressed(MC.getWindow(), keyCode);
        jumpKey.setPressed(physicallyPressed || held);
    }
}
