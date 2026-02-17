package namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings;

import namidevelopment.kiriyaga.api.model.setting.ColorSetting;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.render.RectangleRenderState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class ColorSettingPanel extends BasePanel {

    private static final int HEIGHT = 13;
    private static final int PADDING = 3;

    private final ColorSetting setting;
    private boolean expanded = false;

    private static final int SV_SIZE = 80;
    private static final int HUE_HEIGHT = 6;
    private static final int ALPHA_WIDTH = 6;
    private static final int SV_HUE_PADDING = 3;

    private int lastSvX, lastSvY;
    private int lastHueX, lastHueY;
    private int lastAlphaX, lastAlphaY;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    public ColorSettingPanel(ColorSetting setting) {
        this.setting = setting;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        ColorFeature colorFeature = getColorFeature();
        Color textColor = colorFeature.getStyledTextColor(255);

        int textY = y + (HEIGHT - 8) / 2 + 1;
        int textX = x + PADDING + (hovered ? 1 : 0);
        FONT_SERVICE.drawText(context, setting.getName(), textX, textY, toRGBA(textColor), true);

        int colorSize = HEIGHT - 4;
        int colorX = x + width - PADDING - colorSize;
        int colorY = y + 2;
        fillRect(context, colorX, colorY, colorX + colorSize, colorY + colorSize, toRGBA(new Color(setting.getRed(), setting.getGreen(), setting.getBlue(), setting.getAlpha())));

        if (expanded) {
            int contentRight = x + width - PADDING - ALPHA_WIDTH;
            int contentWidth = contentRight - (x + PADDING);

            lastSvX = x + PADDING;
            lastSvY = y + HEIGHT + SV_HUE_PADDING;
            renderSVSquare(context, lastSvX, lastSvY, SV_SIZE);

            lastHueX = lastSvX;
            lastHueY = lastSvY + SV_SIZE + SV_HUE_PADDING;
            renderHueSlider(context, lastHueX, lastHueY, contentWidth, HUE_HEIGHT);

            lastAlphaX = x + width - PADDING - ALPHA_WIDTH;
            lastAlphaY = lastSvY;
            renderAlphaSlider(context, lastAlphaX, lastAlphaY, ALPHA_WIDTH, SV_SIZE);
        }
    }


    @Override
    public void onRightClick() {
        expanded = !expanded;
        height = expanded ? HEIGHT + SV_SIZE + HUE_HEIGHT + SV_HUE_PADDING * 2 : HEIGHT;
    }


    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 1 && isHovered(mouseX, mouseY)) {
            onRightClick();
            return true;
        }

        if (!expanded || button != 0) return false;

        if (mouseX >= lastHueX && mouseX <= lastHueX + (width - PADDING * 2 - ALPHA_WIDTH) && mouseY >= lastHueY && mouseY <= lastHueY + HUE_HEIGHT) {
            updateHue(mouseX);
            draggingHue = true;
            return true;
        }

        if (mouseX >= lastSvX && mouseX <= lastSvX + (width - PADDING * 2 - ALPHA_WIDTH) && mouseY >= lastSvY && mouseY <= lastSvY + SV_SIZE) {
            updateSV(mouseX, mouseY);
            draggingSV = true;
            return true;
        }

        if (mouseX >= lastAlphaX && mouseX <= lastAlphaX + ALPHA_WIDTH && mouseY >= lastAlphaY && mouseY <= lastAlphaY + SV_SIZE) {
            updateAlpha(mouseY);
            draggingAlpha = true;
            return true;
        }
        return false;
    }


    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (draggingSV) updateSV(mouseX, mouseY);
        if (draggingHue) updateHue(mouseX);
        if (draggingAlpha) updateAlpha(mouseY);
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        boolean wasDragging = draggingHue || draggingSV || draggingAlpha;
        draggingHue = false;
        draggingSV = false;
        draggingAlpha = false;
        return wasDragging;
    }

    private void updateSV(double mouseX, double mouseY) {
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float sat = (float)((mouseX - lastSvX) / (float)(width - PADDING * 2 - ALPHA_WIDTH));
        float bri = 1f - (float)((mouseY - lastSvY) / SV_SIZE);
        sat = Math.max(0f, Math.min(1f, sat));
        bri = Math.max(0f, Math.min(1f, bri));

        int rgb = Color.HSBtoRGB(hsb[0], sat, bri);
        Color c = new Color(rgb, true);
        setting.setValue(c.getRed(), c.getGreen(), c.getBlue(), setting.getAlpha());
    }

    private void updateHue(double mouseX) {
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float hue = (float)((mouseX - lastHueX) / (float)(width - PADDING * 2 - ALPHA_WIDTH));
        hue = Math.max(0f, Math.min(1f, hue));

        int rgb = Color.HSBtoRGB(hue, hsb[1], hsb[2]);
        Color c = new Color(rgb, true);
        setting.setValue(c.getRed(), c.getGreen(), c.getBlue(), setting.getAlpha());
    }

    private void updateAlpha(double mouseY) {
        float alpha = (float)(1f - (mouseY - lastAlphaY) / SV_SIZE);
        alpha = Math.max(0f, Math.min(1f, alpha));
        setting.setValue(setting.getRed(), setting.getGreen(), setting.getBlue(), (int)(alpha * 255));
    }

    private void renderSVSquare(GuiGraphics context, int x, int y, int size) {
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float hue = hsb[0];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float sat = i / (float) size;
                float bri = 1f - j / (float) size;
                int c = toRGBA(Color.getHSBColor(hue, sat, bri));
                fillRect(context, x + i, y + j, x + i + 1, y + j + 1, c);
            }
        }
    }

    private void renderHueSlider(GuiGraphics context, int x, int y, int width, int height) {
        float step = 1f / width;
        for (int i = 0; i < width; i++) {
            int c = toRGBA(Color.getHSBColor(i * step, 1f, 1f));
            fillRect(context, x + i, y, x + i + 1, y + height, c);
        }
    }

    private void renderAlphaSlider(GuiGraphics context, int x, int y, int width, int height) {
        int r = setting.getRed();
        int g = setting.getGreen();
        int b = setting.getBlue();
        for (int i = 0; i < height; i++) {
            float alpha = 1f - ((float)i / height);
            int c = toRGBA(new Color(r, g, b, (int)(alpha * 255)));
            fillRect(context, x, y + i, x + width, y + i + 1, c);
        }
    }

    private void fillRect(GuiGraphics context, int x1, int y1, int x2, int y2, int color) {
        context.guiRenderState.submitGlyphToCurrentLayer(
                new RectangleRenderState(new Matrix3x2f(context.pose()), x1, y1, x2, y2, color, color, color, color, context.scissorStack.peek()));
    }

    @Override
    protected String getName() { return setting.getName(); }

    @Override
    protected boolean isEnabled() { return true; }

    @Override
    protected Color getTextColor() { return getColorFeature().getStyledTextColor(255); }
}
