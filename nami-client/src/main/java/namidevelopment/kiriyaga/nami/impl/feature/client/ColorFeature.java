package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.contract.feature.ColorFeatureConfig;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render2DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.ColorSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;

import java.awt.*;


@RegisterFeature
public class ColorFeature extends Feature implements ColorFeatureConfig {

    public final ColorSetting globalColor = addSetting(new ColorSetting("Global", new Color(50, 137, 179, 255), true));
    public final BoolSetting rainbowEnabled = addSetting(new BoolSetting("Rainbow", false));
    public final DoubleSetting rainbowSpeed = addSetting(new DoubleSetting("Speed", 0.005, 0.0001, 1.50));

    public final ColorSetting friendColor = addSetting(new ColorSetting("Friend", new Color(85, 255, 255, 255), true));
    public final ColorSetting enemyColor = addSetting(new ColorSetting("Enemy", new Color(255, 85, 85, 255), true));

    private int phase = 0;

    public ColorFeature() {
        super("Color", "Customizes color scheme.", FeatureCategory.of("Client"), "colr", "c", "colors", "clitor");
        if (!this.isEnabled())
            this.toggle();
        rainbowSpeed.setShowCondition(() -> rainbowEnabled.get());
    }

    @Override
    public void onDisable() {
        if (!this.isEnabled())
            this.toggle();
    }

    private int getAlpha255() {
        return 130;
    }

    public Color applySaturation(Color base, double saturationFactor) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float saturation = (float) (hsb[1] * saturationFactor);
        saturation = Math.max(0f, Math.min(1f, saturation));
        return Color.getHSBColor(hsb[0], saturation, hsb[2]);
    }

    public Color applyDarkness(Color base, double darknessFactor) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);

        float brightness = hsb[2] * (float)(1.0 - darknessFactor);

        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], Math.max(0f, Math.min(1f, brightness)));

        return new Color((rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                base.getAlpha());
    }

    public Color getStyledColor(Color base, double saturation, double darkness) {
        Color adjusted = applyDarkness(applySaturation(base, saturation), darkness);
        return new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), getAlpha255());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onRender(Render2DEvent ev){
        if (!rainbowEnabled.get()) return;

        Color current = globalColor.get();

        float[] hsb = Color.RGBtoHSB(current.getRed(), current.getGreen(), current.getBlue(), null);
        float hue = hsb[0];
        float sat = hsb[1];
        float bri = hsb[2];

        int step = (int) Math.max(1, rainbowSpeed.get() * 4);
        switch (phase) {
            case 0: hue += step / 255f; if (hue >= 1f) { hue = 1f; phase = 1; } break;
            case 1: hue -= step / 255f; if (hue <= 0f) { hue = 0f; phase = 2; } break;
            case 2: hue += step / 255f; if (hue >= 1f) { hue = 1f; phase = 3; } break;
            case 3: hue -= step / 255f; if (hue <= 0f) { hue = 0f; phase = 4; } break;
            case 4: hue += step / 255f; if (hue >= 1f) { hue = 1f; phase = 5; } break;
            case 5: hue -= step / 255f; if (hue <= 0f) { hue = 0f; phase = 0; } break;
        }

        int rgb = Color.HSBtoRGB(hue, sat, bri);
        Color c = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, getAlpha255());
        globalColor.set(c);
    }

    public Color getStyledGlobalColor() {
        return getStyledColor(globalColor.get(), 1.00, 0.00);
    }

    @Override
    public Color getGlobalColor() {
        return globalColor.get();
    }

    @Override
    public Color getFriendColor() {
        return getStyledColor(friendColor.get(), 1.00, 0.00);
    }

    @Override
    public Color getEnemyColor() {
        return getStyledColor(enemyColor.get(), 1.00, 0.00);
    }

    @Override
    public boolean isRainbowEnabled() {
        return rainbowEnabled.get();
    }

    @Override
    public double getRainbowSpeed() {
        return rainbowSpeed.get();
    }

    public Color getFriendTextColor(int alpha) {
            Color base = getFriendColor();
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), clampAlpha(alpha));
    }

    public Color getStyledSecondColor() {
        return applyDarkness(getStyledGlobalColor(), 0.35);
    }

    public Color getStyledGlobalColor(int alpha) {
        Color base = getStyledGlobalColor();
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clampAlpha(alpha));
    }

    public Color getStyledSecondColor(int alpha) {
        Color base = getStyledSecondColor();
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clampAlpha(alpha));
    }

    public Color getStyledTextColor(int alpha) {
        return new Color(255, 255, 255, clampAlpha(alpha));
    }

    public Color getStyledTextSecondColor(int alpha) {
        return applyDarkness(getStyledTextColor(alpha), 0.35);
    }

    private int clampAlpha(int alpha) {
        if (alpha < 0) return 0;
        if (alpha > 255) return 255;
        return alpha;
    }
}
