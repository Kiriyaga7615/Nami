package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.text.Text;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class SpeedometerModule extends HudElementModule {

    public enum SpeedMode {
        KMH, BPS
    }

    public final BoolSetting displayLabel = addSetting(new BoolSetting("display label", true));
    public final EnumSetting<SpeedMode> mode = addSetting(new EnumSetting<>("mode", SpeedMode.KMH));

    private double speed = 0;
    private double lastX = 0;
    private double lastZ = 0;

    public SpeedometerModule() {
        super("speedometer", "Displays current player speed.", 0, 0, 50, 9);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onTick(PreTickEvent event) {
        if (MC.player == null) return;

        double x = MC.player.getX();
        double z = MC.player.getZ();

        if (lastX == 0 && lastZ == 0) {
            lastX = x;
            lastZ = z;
            return;
        }

        double dx = x - lastX;
        double dz = z - lastZ;

        double distPerTick = Math.sqrt(dx * dx + dz * dz);
        double speedBps = distPerTick * 20.0;

        if (mode.get() == SpeedMode.BPS) {
            speed = speedBps;
        } else {
            speed = speedBps * 3.6;
        }

        lastX = x;
        lastZ = z;
    }

    @Override
    public Text getDisplayText() {
        if (MC.player == null) return CAT_FORMAT.format("{bg}NaN");

        String speedStr = formatSpeedNumber(speed) + (mode.get() == SpeedMode.BPS ? " b/s" : " km/h");
        String textStr = displayLabel.get() ? "Speed: " + speedStr : speedStr;

        width = FONT_MANAGER.getWidth(textStr);
        height = FONT_MANAGER.getHeight();

        if (displayLabel.get()) {
            return CAT_FORMAT.format("{bg}Speed: {bw}" + speedStr);
        } else {
            return Text.literal(textStr);
        }
    }

    private String formatSpeedNumber(double val) {
        double rounded = Math.round(val * 10.0) / 10.0;
        return String.format("%.2f", rounded).replace(',', '.');
    }
}
