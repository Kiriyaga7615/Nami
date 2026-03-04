package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.util.RotationUtils.alignYRot;

@RegisterFeature
public class YawFeature extends Feature {

    public final IntSetting directions = addSetting(new IntSetting("Directions", 8, 4, 16));

    public YawFeature() {
        super("Yaw", "Snap player yaw to nearest fixed angle.", FeatureCategory.of("Movement"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        float s = 360f / directions.get();
        float targetYaw = Math.round(MC.player.getYRot() / s) * s;

        targetYaw = alignYRot(targetYaw, MC.player.getYRot());
        MC.player.setYRot(targetYaw);
    }

}