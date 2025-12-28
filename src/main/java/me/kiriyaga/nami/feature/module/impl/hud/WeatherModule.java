package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class WeatherModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public WeatherModule() {
        super("Weather", "Displays current weather.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        if (MC.world == null) return CAT_FORMAT.format("{bg}NaN");

        String weather;

        if (MC.world.isRaining()) {
            if (MC.world.isThundering()) {
                weather = "thunder";
            } else {
                weather = "rain";
            }
        } else {
            weather = "clear";
        }

        String text;
        if (displayLabel.get()) {
            text = "{bg}Weather: {bw}" + weather;
        } else {
            text = "{bw}" + weather;
        }

        width = FONT_MANAGER.getWidth(text.replace("{bg}", "").replace("{bw}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(text);
    }
}
