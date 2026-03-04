package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import net.minecraft.world.phys.Vec3;

import static namidevelopment.kiriyaga.api.NamiApi.INPUT_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class FastFallFeature extends Feature {

    public final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 1.00, 0.10, 5.00));

    public FastFallFeature() {
        super("FastFall", "Fall from blocks faster.", FeatureCategory.of("Movement"), "fastfall");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onTick(PreTickEvent ev) {
        if (MC.level == null || MC.player == null) return;

        if (!INPUT_SERVICE.getInputCache().hasAnyInput() || INPUT_SERVICE.getInputCache().jump() || MC.player.isInPowderSnow || MC.player.isUnderWater() || MC.player.isInLava() || MC.player.isInWater() || MC.player.isFallFlying() || MC.player.isNoGravity())
            return;

        if (MC.player.onGround()) {
            Vec3 prev = MC.player.getDeltaMovement();
            MC.player.setDeltaMovement(prev.x, -speed.get(), prev.z);
        }
    }
}
