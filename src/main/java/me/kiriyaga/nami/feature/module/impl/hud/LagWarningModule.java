package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class LagWarningModule extends HudElementModule {


    public LagWarningModule() {
        super("LagWarning", "Displays lag warning if connection unstable.", 0, 0, 100, 9);
    }

    @Override
    public Component getDisplayText() {
        if (!SERVER_MANAGER.isConnectionUnstable())
            return Component.empty();

        if (MC.isInSingleplayer() || MC.world == null) return Component.of("LagWarning:");

        double seconds = SERVER_MANAGER.getUnstableTime();
        double roundedSeconds = Math.round(seconds * 100.0) / 100.0;
        String warningText = "Server is not responding in " + String.format("%.2f", roundedSeconds) + "s";

        width = FONT_MANAGER.getWidth(warningText);
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format("{bg}" + warningText);
    }
}
