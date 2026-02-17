package namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings;

import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class IntSettingPanel extends BasePanel {

    public static final int HEIGHT = 14;
    private static final int PADDING = 3;
    private static final int SLIDER_HEIGHT = 1;

    private final IntSetting setting;
    private boolean dragging = false;

    public IntSettingPanel(IntSetting setting) {
        this.setting = setting;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        int textY = y + 2;
        int textX = x + PADDING + (hovered ? 1 : 0);
        FONT_SERVICE.drawText(context, setting.getName(), textX, textY, toRGBA(getTextColor()), true);
        String val = String.valueOf(setting.get());
        FONT_SERVICE.drawText(context, val, x + width - PADDING - FONT_SERVICE.getWidth(val), textY, toRGBA(getTextColor()), true);

        renderSlider(context);
    }

    private void renderSlider(GuiGraphics context) {
        ColorFeature colorFeature = getColorFeature();
        int sliderX = x + PADDING;
        int sliderY = y + height - SLIDER_HEIGHT - 2;
        int sliderWidth = width - PADDING * 2;
        int min = setting.getMin();
        int max = setting.getMax();
        int value = setting.get();

        double percent = (value - min) / (double)Math.max(1, max - min);
        percent = Math.max(0, Math.min(1, percent));
        int filled = (int)(sliderWidth * percent);

        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + SLIDER_HEIGHT, toRGBA(new Color(60,60,60,150)));

        context.fill(sliderX, sliderY, sliderX + filled, sliderY + SLIDER_HEIGHT, toRGBA(colorFeature.getStyledGlobalColor()));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;

        dragging = true;
        updateValue(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) dragging = false;
        return dragging;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (dragging) updateValue(mouseX);
    }

    private void updateValue(double mouseX) {
        int min = setting.getMin();
        int max = setting.getMax();

        double percent = (mouseX - (x + PADDING)) / (double)(width - PADDING * 2);
        percent = Math.max(0, Math.min(1, percent));

        int value = (int)Math.round(min + percent * (max - min));
        setting.set(value);
    }

    @Override
    protected String getName() {
        return setting.getName();
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected Color getTextColor() {
        return getColorFeature().getStyledTextColor(255);
    }
}
