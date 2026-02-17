package namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings;

import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class EnumSettingPanel extends BasePanel {

    public static final int HEIGHT = 13;
    private static final int PADDING = 3;
    private final EnumSetting<?> setting;

    public EnumSettingPanel(EnumSetting<?> setting) {
        this.setting = setting;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        Color textColor = getTextColor();
        int textY = y + (height - 8) / 2 + 1;
        int textX = x + PADDING + (hovered ? 1 : 0);
        FONT_SERVICE.drawText(context, getName(), textX, textY, toRGBA(textColor), true);
        String valueStr = setting.get().toString();
        int valueX = x + width - PADDING - FONT_SERVICE.getWidth(valueStr);
        FONT_SERVICE.drawText(context, valueStr, valueX, textY, toRGBA(textColor), true);
    }

    @Override
    public void onLeftClick() {
        setting.cycle(false);
    }

    @Override
    public void onRightClick() {
        setting.cycle(true);
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
        ColorFeature colorFeature = getColorFeature();
        return colorFeature.getStyledTextColor(255);
    }
}
