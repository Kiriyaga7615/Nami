package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class DirectionModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final BoolSetting precise = addSetting(new BoolSetting("Precise", false));

    public DirectionModule() {
        super("Direction", "Displays accurate yaw/pitch.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        String dirText;
        float yaw = 0;
        float pitch = 0;
        if (MC.getCameraEntity() != null) {
            yaw = MC.getCameraEntity().getYaw();
            pitch = MC.getCameraEntity().getPitch();
        }

        if (precise.get())
        dirText = String.format(Locale.US, "%.3f {bg}[{bw}%.3f{bg}]", yaw, pitch); // locale is wild
        else
            dirText = String.format(Locale.US, "%.0f {bg}[{bw}%.0f{bg}]", yaw, pitch); // locale is wild

        String text = displayLabel.get() ? "{bg}Direction: {bw}" + dirText : "{bw}" + dirText;

        width = FONT_MANAGER.getWidth(text.replace("{bg}", "").replace("{bw}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(text);
    }
}