package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class TpsModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public TpsModule() {
        super("TPS", "Displays server TPS.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        float avgTps = SERVER_MANAGER.getAverageTPS();
        float latestTps = SERVER_MANAGER.getLatestTPS();
        String tpsText = String.format(Locale.US, "%.2f {bg}[{bw}%.2f{bg}]", avgTps, latestTps); // locale is wild
        String text = displayLabel.get() ? "{bg}TPS: {bw}" + tpsText : "{bw}" + tpsText;

        width = FONT_MANAGER.getWidth(text.replace("{bg}", "").replace("{bw}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(text);
    }
}