package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class UptimeModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final BoolSetting days = addSetting(new BoolSetting("Days", true));
    public final BoolSetting hours = addSetting(new BoolSetting("Hours", true));
    public final BoolSetting minutes = addSetting(new BoolSetting("Minutes", true));
    public final BoolSetting seconds = addSetting(new BoolSetting("Seconds", true));

    public UptimeModule() {
        super("Uptime", "Displays total time of minecraft run time.", 0, 0, 120, 10);
    }

    @Override
    public Component getDisplayText() {
        long uptimeMillis = System.currentTimeMillis() - START_TIME;
        long totalSeconds = uptimeMillis / 1000;

        long displayDays = 0, displayHours = 0, displayMinutes = 0, displaySeconds = 0;

        if (this.days.get()) {
            displayDays = totalSeconds / 86400;
            totalSeconds %= 86400;
        }
        if (this.hours.get()) {
            displayHours = totalSeconds / 3600;
            totalSeconds %= 3600;
        } else if (!this.days.get()) {
            displayHours = totalSeconds / 3600;
            totalSeconds %= 3600;
        } else {
            totalSeconds += (totalSeconds / 3600) * 3600;
        }

        if (this.minutes.get()) {
            displayMinutes = totalSeconds / 60;
            totalSeconds %= 60;
        } else {
            totalSeconds += (totalSeconds / 60) * 60;
        }
        if (this.seconds.get()) {
            displaySeconds = totalSeconds;
        }

        StringBuilder sb = new StringBuilder();

        if (displayLabel.get()) sb.append("{bg}Uptime: {bw}");

        boolean first = true;

        if (this.days.get() && displayDays > 0) {
            sb.append(displayDays).append(" days{bg},{bw}");
            first = false;
        }
        if (this.hours.get() && displayHours > 0) {
            if (!first) sb.append(" ");
            sb.append(displayHours).append(" hours{bg},{bw}");
            first = false;
        }
        if (this.minutes.get() && displayMinutes > 0) {
            if (!first) sb.append(" ");
            sb.append(displayMinutes).append(" minutes{bg},{bw}");
            first = false;
        }
        if (this.seconds.get() && displaySeconds > 0) {
            if (!first) sb.append(" ");
            sb.append(displaySeconds).append(" seconds");
        }

        String formatted = sb.toString();
        width = FONT_MANAGER.getWidth(formatted.replace("{bg}", "").replace("{bw}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(formatted);
    }
}
