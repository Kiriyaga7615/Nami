package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class ParkourModule extends Module {

    public ParkourModule() {
        super("Parkour", "Automatically jumps at the edge of blocks.", ModuleCategory.of("Movement"));
    }

    @Override
    public void onDisable() {
        setJumpHeld(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        boolean shouldJump =
                MC.player.isOnGround()
                        && !MC.player.isSneaking()
                        && MC.world.isSpaceEmpty(MC.player,
                        MC.player.getBoundingBox()
                                .offset(0.0, -0.5, 0.0)
                                .expand(-0.001, 0.0, -0.001));

        setJumpHeld(shouldJump);
    }

    private void setJumpHeld(boolean held) {
        KeyMapping jumpKey = MC.options.jumpKey;
        InputConstants.Key boundKey = ((KeyBindingAccessor) jumpKey).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputConstants.isKeyPressed(MC.getWindow(), keyCode);
        jumpKey.setPressed(physicallyPressed || held);
    }
}
