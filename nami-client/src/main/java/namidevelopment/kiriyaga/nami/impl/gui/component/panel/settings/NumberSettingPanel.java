package namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings;

import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public abstract class NumberSettingPanel<T extends Number> extends BasePanel {

    public static final int HEIGHT = 14;
    protected static final int PADDING = 3;
    protected static final int SLIDER_HEIGHT = 1;
    protected boolean waiting = false;
    protected String input = "";

    protected boolean dragging = false;

    public NumberSettingPanel() {
        this.height = HEIGHT;
    }

    protected abstract String getSettingName();
    protected abstract double getValue();
    protected abstract double getMin();
    protected abstract double getMax();
    protected abstract void setValueFromDouble(double value);
    protected abstract String formatValue(double value);

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        int textY = y + 2;
        int textX = x + PADDING + (hovered ? 1 : 0);

        FONT_SERVICE.drawText(context, getSettingName(), textX, textY, toRGBA(getTextColor()), true);

        String val;

        if (waiting) {
            val = input + "";
        } else {
            val = formatValue(getValue());
        }

        FONT_SERVICE.drawText(context, val, x + width - PADDING - FONT_SERVICE.getWidth(val), textY, toRGBA(getTextColor()), true);
        renderSlider(context);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;

        if (button == 0) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }

        if (button == 1) {
            waiting = true;
            input = "";
            dragging = false;
            return true;
        }

        return false;
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!waiting) return;
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            applyValue();
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            waiting = false;
            input = "";
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            return;
        }

        if (keyCode >= 48 && keyCode <= 57) {
            input += (char) keyCode;
            return;
        }

        if (keyCode >= 320 && keyCode <= 329) {
            int digit = keyCode - 320;
            input += digit;
            return;
        }

        if (keyCode == 46 && !input.contains(".")) {
            input += ".";
            return;
        }

        if (keyCode == 330 && !input.contains(".")) {
            input += ".";
            return;
        }

        if (keyCode == 45 && input.isEmpty()) {
            input += "-";
            return;
        }
        waiting = false;
        input = "";
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) dragging = false;
        return dragging;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (!waiting && dragging) {
            updateValue(mouseX);
        }
    }

    @Override
    protected String getName() {
        return getSettingName();
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected Color getTextColor() {
        return getColorFeature().getStyledTextColor(255);
    }

    private void renderSlider(GuiGraphics context) {
        ColorFeature colorFeature = getColorFeature();
        int sliderX = x + PADDING;
        int sliderY = y + height - SLIDER_HEIGHT - 2;
        int sliderWidth = width - PADDING * 2;
        double percent = (getValue() - getMin()) / (getMax() - getMin());
        percent = Math.max(0, Math.min(1, percent));

        int filled = (int) (sliderWidth * percent);

        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + SLIDER_HEIGHT, toRGBA(new Color(60,60,60,150)));
        context.fill(sliderX, sliderY, sliderX + filled, sliderY + SLIDER_HEIGHT, toRGBA(colorFeature.getStyledGlobalColor()));
    }

    private void applyValue() {
        try {
            if (input.isEmpty() || input.equals("-") || input.equals(".")) {
                waiting = false;
                input = "";
                return;
            }
            double value = Double.parseDouble(input);
            value = Math.max(getMin(), Math.min(getMax(), value));
            setValueFromDouble(value);
        } catch (Exception ignored) {
        }
        waiting = false;
        input = "";
    }

    private void updateValue(double mouseX) {
        double percent = (mouseX - (x + PADDING)) / (double)(width - PADDING * 2);
        percent = Math.max(0, Math.min(1, percent));

        double value = getMin() + percent * (getMax() - getMin());
        setValueFromDouble(value);
    }
}