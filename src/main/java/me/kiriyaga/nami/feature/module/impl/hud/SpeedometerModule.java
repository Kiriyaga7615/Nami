package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class SpeedometerModule extends HudElementModule {

    public enum SpeedMode {
        KMH, BPS
    }

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final IntSetting samples = addSetting(new IntSetting("Samples", 80, 10, 200));
    public final EnumSetting<SpeedMode> mode = addSetting(new EnumSetting<>("Mode", SpeedMode.KMH));

    private double speed = 0;
    private double[] speedSamples;
    private int speedSampleIndex = 0;
    private boolean speedBufferFilled = false;

    private double lastX = 0;
    private double lastZ = 0;

    public SpeedometerModule() {
        super("Speedometer", "Displays current player speed.", 0, 0, 50, 9);

        samples.setOnChanged(() -> {
            speedSamples = new double[samples.get()];
            speedSampleIndex = 0;
            speedBufferFilled = false;
        });

        speedSamples = new double[samples.get()];
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onTick(PreTickEvent event) {
        if (MC.player == null) return;

        int sampleCount = samples.get();
        if (speedSamples == null || speedSamples.length != sampleCount) {
            speedSamples = new double[sampleCount];
            speedSampleIndex = 0;
            speedBufferFilled = false;
        }

        double dx = MC.player.getX() - lastX;
        double dz = MC.player.getZ() - lastZ;

        double instantSpeed = Math.sqrt(dx * dx + dz * dz) * 20;

        speedSamples[speedSampleIndex] = instantSpeed;
        speedSampleIndex = (speedSampleIndex + 1) % sampleCount;

        if (speedSampleIndex == 0) speedBufferFilled = true;

        int count = speedBufferFilled ? sampleCount : speedSampleIndex;
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += speedSamples[i];
        }

        speed = count > 0 ? sum / count : 0;

        lastX = MC.player.getX();
        lastZ = MC.player.getZ();
    }

    @Override
    public Component getDisplayText() {
        if (MC.player == null) return CAT_FORMAT.format("{bg}NaN");

        String speedStr;
        if (mode.get() == SpeedMode.BPS) {
            speedStr = formatSpeedNumber(speed) + " bp/s";
        } else {
            speedStr = formatSpeedNumber(speed * 3.6) + " km/h";
        }

        String textStr = displayLabel.get() ? "Speed: " + speedStr : speedStr;

        width = FONT_MANAGER.getWidth(textStr);
        height = FONT_MANAGER.getHeight();

        if (displayLabel.get()) {
            return CAT_FORMAT.format("{bg}Speed: {bw}" + speedStr);
        } else {
            return Component.literal(textStr);
        }
    }

    private String formatSpeedNumber(double val) {
        double rounded = Math.round(val * 10.0) / 10.0;
        return String.format("%.2f", rounded).replace(',', '.');
    }
}