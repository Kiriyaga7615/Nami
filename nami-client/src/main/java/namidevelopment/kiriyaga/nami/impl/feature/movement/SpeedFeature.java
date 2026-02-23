package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class SpeedFeature extends Feature {

    private enum Mode {
        ROTATION
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.ROTATION));
    public final BoolSetting inLiquid = addSetting(new BoolSetting("InWater", true));

    public SpeedFeature() {
        super("Speed", "Increases movement speed.", FeatureCategory.of("Movement"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;
        this.clearDisplayInfo();

        if (MC.player.isVisuallyCrawling() || MC.player.isCrouching() || MC.player.isShiftKeyDown() || MC.player.isFallFlying())
            return; // this fallback need due to sprinting not apply for theese states
        // also we do not need swimming because swimming do apply speed for sprinitng

        if (!inLiquid.get() && MC.player.isInWater())
            return;

        this.addDisplayInfo(mode.get().toString());

        if (mode.get() == Mode.ROTATION && isMoving()) {
            float yaw = INPUT_SERVICE.getClientHandler().getDirection();
            float pitch = MC.player.getXRot();
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(SpeedFeature.class.getName(), 1, yaw, pitch, RotationsFeature.RotationMode.MOTION));
        }
    }

    private boolean isMoving() {
        return MC.options.keyUp.isDown() ||
                MC.options.keyDown.isDown() ||
                MC.options.keyLeft.isDown() ||
                MC.options.keyRight.isDown();
    }
}
