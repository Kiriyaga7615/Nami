package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketSendEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.PlayerInteractEntityC2SPacketAccessor;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.client.player.LocalPlayer;

import static me.kiriyaga.nami.Nami.MC;
// some crazy shit happened here
@RegisterModule
public class SprintModule extends Module {

    private final BoolSetting inLiquid = addSetting(new BoolSetting("InLiquid", true));
/*
    public final BoolSetting twobtwot = addSetting(new BoolSetting("2b2t", false));
*/

    private int shouldSprintTicks = 0; // yes sorry

    public SprintModule() {
        super("Sprint", "Automatically makes you sprint while moving.", ModuleCategory.of("Movement"));
    }

    public void stopSprinting(int i) {
        this.shouldSprintTicks = i;
    }

    private boolean shouldForceNoSprint() {
        return shouldSprintTicks > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTickEvent(PreTickEvent event) {
        LocalPlayer player = MC.player;
        if (player == null) return;

        if (shouldSprintTicks > 0) {
            shouldSprintTicks--;
        }

        if (!inLiquid.get() && (player.isSubmergedInWater() || player.isTouchingWater()))
            return;

        if (shouldForceNoSprint()) {
            player.setSprinting(false);
            return;
        }

        boolean canSprint = player.forwardSpeed > 0 && !player.hasVehicle();

        if (canSprint) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }
    }
}
