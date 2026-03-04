package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.mixin.DuckKeyMapping;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class ParkourFeature extends Feature {

    public ParkourFeature() {
        super("Parkour", "Automatically jumps at the edge of blocks.", FeatureCategory.of("Movement"));
    }

    @Override
    public void onDisable() {
        setJumpHeld(false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        boolean shouldJump = MC.player.onGround() && !MC.player.isShiftKeyDown() && MC.level.noCollision(MC.player, MC.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-0.001, 0.0, -0.001));

        setJumpHeld(shouldJump);
    }

    private void setJumpHeld(boolean held) {
        KeyMapping jumpKey = MC.options.keyJump;
        InputConstants.Key boundKey = ((DuckKeyMapping) jumpKey).getKey();
        int keyCode = boundKey.getValue();
        boolean physicallyPressed = InputConstants.isKeyDown(MC.getWindow(), keyCode);
        jumpKey.setDown(physicallyPressed || held);
    }
}
