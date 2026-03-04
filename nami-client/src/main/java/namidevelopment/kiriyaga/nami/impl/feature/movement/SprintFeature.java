package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.client.player.LocalPlayer;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
// some crazy shit happened here
@RegisterFeature
public class SprintFeature extends Feature {

    public final BoolSetting inLiquid = addSetting(new BoolSetting("InLiquid", true));
/*
    public final BoolSetting twobtwot = addSetting(new BoolSetting("2b2t", false));
*/

    private int shouldSprintTicks = 0; // yes sorry

    public SprintFeature() {
        super("Sprint", "Automatically makes you sprint while moving.", FeatureCategory.of("Movement"));
    }

    public void stopSprinting(int i) {
        this.shouldSprintTicks = i;
    }

    private boolean shouldForceNoSprint() {
        return shouldSprintTicks > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTickEvent(PreTickEvent event) {
        LocalPlayer player = MC.player;
        if (player == null) return;

        if (shouldSprintTicks > 0) {
            shouldSprintTicks--;
        }

        if (!inLiquid.get() && (player.isUnderWater() || player.isInWater()))
            return;

        if (shouldForceNoSprint()) {
            player.setSprinting(false);
            return;
        }

        boolean canSprint = player.zza > 0 && !player.isPassenger();

        if (canSprint) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }
    }
}
