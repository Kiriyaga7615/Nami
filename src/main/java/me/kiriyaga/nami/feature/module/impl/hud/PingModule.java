package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class PingModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public PingModule() {
        super("Ping", "Displays current Ping.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        int ping = SERVER_MANAGER.getPing();
        String textStr;

        if (displayLabel.get()) {
            textStr = "Ping: " + ping;
        } else {
            textStr = String.valueOf(ping);
        }

        width = FONT_MANAGER.getWidth(textStr);
        height = FONT_MANAGER.getHeight();

        if (displayLabel.get()) {
            return CAT_FORMAT.format("{bg}Ping: {bw}" + ping);
        } else {
            return Component.literal(textStr);
        }
    }
}
