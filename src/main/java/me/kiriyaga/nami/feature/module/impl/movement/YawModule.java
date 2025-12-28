package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.util.RotationUtils.alignYaw;
import static me.kiriyaga.nami.util.RotationUtils.yawDifference;

@RegisterModule
public class YawModule extends Module {

    private final IntSetting directions = addSetting(new IntSetting("Directions", 8, 4, 16));

    public YawModule() {
        super("Yaw", "Snap player yaw to nearest fixed angle.", ModuleCategory.of("Movement"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        float s = 360f / directions.get();
        float targetYaw = Math.round(MC.player.getYaw() / s) * s;

        targetYaw = alignYaw(targetYaw, MC.player.getYaw());
        MC.player.setYaw(targetYaw);
    }

}