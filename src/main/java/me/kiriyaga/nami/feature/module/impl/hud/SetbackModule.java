package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class SetbackModule extends HudElementModule {


    public SetbackModule() {
        super("Setback", "Displays setback warning once server cancelled any of your packets.", 0, 0, 100, 9);
    }

    @Override
    public Component getDisplayText() {

        if (!SERVER_MANAGER.hasElapsedSinceSetback(5000))
            return Component.empty();

        if (MC.isInSingleplayer() || MC.world == null) return Component.of("Setback:");

        long last = SERVER_MANAGER.getLastSetbackTime();
        double delta = (System.currentTimeMillis() - last) / 1000.0;
        double rounded = Math.round(delta * 100.0) / 100.0;
        String warningText = "Setback was: " + String.format("%.2f", rounded) + "s";

        width = FONT_MANAGER.getWidth(warningText);
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format("{bg}" + warningText);
    }
}
