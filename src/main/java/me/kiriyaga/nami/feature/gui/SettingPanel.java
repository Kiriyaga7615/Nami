package me.kiriyaga.nami.feature.gui;

import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.setting.Setting;
import me.kiriyaga.nami.setting.impl.*;
import me.kiriyaga.nami.util.KeyUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

import static java.lang.Math.clamp;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;
import static me.kiriyaga.nami.feature.gui.ClickGuiScreen.GUI_ALPHA;

public class SettingPanel {
    public static final int HEIGHT = 15;
    private static final int PADDING = 3;
    public static final int INNER_PADDING = 2;
    private static final int WIDTH = 120 - CategoryPanel.BORDER_WIDTH * 2 - INNER_PADDING * 2;
    private static final int SLIDER_HEIGHT = 2;
    private static final int MODULE_SPACING = 2;

    private static KeyBindSetting waitingForKeyBind = null;

    private static ColorModule getColorModule() {
        return MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
    }

    public static int getSettingsHeight(Module module) {
        return module.getSettings().size() * (HEIGHT + MODULE_SPACING) + MODULE_SPACING;
    }

    public static int renderSettings(DrawContext context, TextRenderer textRenderer, Module module, int x, int y, int mouseX, int mouseY) {
        List<Setting<?>> settings = module.getSettings();
        int curY = y + MODULE_SPACING;

        for (Setting<?> setting : settings) {
            render(context, textRenderer, setting, x, curY, mouseX, mouseY);
            curY += HEIGHT + MODULE_SPACING;
        }
        return getSettingsHeight(module);
    }

    public static void render(DrawContext context, TextRenderer textRenderer, Setting<?> setting, int x, int y, int mouseX, int mouseY) {
        ColorModule colorModule = getColorModule();
        if (colorModule == null) return;

        boolean hovered = isHovered(mouseX, mouseY, x, y);
        Color primary = colorModule.getStyledGlobalColor();
        Color secondary = colorModule.getStyledSecondColor();
        Color textCol = new Color(255, 255, 255, GUI_ALPHA);
        Color textColActivated = new Color(255, 255, 255, 255);

        Color bgColor;

        if (setting instanceof BoolSetting boolSetting) {
            bgColor = boolSetting.get() ? primary : secondary;
            if (hovered) bgColor = brighten(bgColor, 0.3f);

            int bgColorInt = bgColor.getRGB();
            int textColorInt = boolSetting.get() ? toRGBA(textColActivated) : toRGBA(textCol);

            context.fill(x, y, x + WIDTH, y + HEIGHT, bgColorInt);
            context.drawText(textRenderer, setting.getName(), x + PADDING, y + (HEIGHT - 8) / 2, textColorInt, false);

            int checkboxSize = 8;
            int checkboxX = x + WIDTH - PADDING - checkboxSize;
            int checkboxY = y + (HEIGHT - checkboxSize) / 2;

            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, toRGBA(new Color(60, 60, 60)));
            if (boolSetting.get()) {
                String symbol = "✓";
                int symbolWidth = textRenderer.getWidth(symbol);
                int symbolHeight = 8;

                int centerX = checkboxX + (checkboxSize - symbolWidth) / 2;
                int centerY = checkboxY + (checkboxSize - symbolHeight) / 2;

                context.drawText(textRenderer, symbol, centerX, centerY, toRGBA(textCol), false);
            }



            return;
        } else if (setting instanceof ColorSetting colorSetting) {
            float[] hsb = Color.RGBtoHSB(colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue(), null);
            float hue = hsb[0];

            bgColor = hovered ? brighten(secondary, 0.3f) : secondary;

            int bgColorInt = bgColor.getRGB();
            int textColorInt = toRGBA(textCol);

            context.fill(x, y, x + WIDTH, y + HEIGHT, bgColorInt);

            String text = setting.getName();
            context.drawText(textRenderer, text, x + PADDING, y + (HEIGHT - 8) / 2, textColorInt, false);

            renderHueSlider(context, x + PADDING, y + HEIGHT - 4, WIDTH - 2 * PADDING, SLIDER_HEIGHT, hue);

            String hex = String.format("#%02X%02X%02X", colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue());
            context.drawText(textRenderer, hex, x + WIDTH - PADDING - textRenderer.getWidth(hex), y + (HEIGHT - 8) / 2, textColorInt, false);

            return;
        } else {
            bgColor = hovered ? brighten(secondary, 0.3f) : secondary;
        }

        int bgColorInt = bgColor.getRGB();
        int textColorInt = toRGBA(textCol);

        context.fill(x, y, x + WIDTH, y + HEIGHT, bgColorInt);
        context.drawText(textRenderer, setting.getName(), x + PADDING, y + (HEIGHT - 8) / 2, textColorInt, false);

        if (setting instanceof IntSetting intSetting) {
            renderSlider(context, x + PADDING, y + HEIGHT - 4,
                    WIDTH - 2 * PADDING, SLIDER_HEIGHT,
                    intSetting.get(), intSetting.getMin(), intSetting.getMax(), primary);

            String valStr = String.valueOf(intSetting.get());
            context.drawText(textRenderer, valStr, x + WIDTH - PADDING - textRenderer.getWidth(valStr), y + (HEIGHT - 8) / 2, textColorInt, false);
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            String valueStr = enumSetting.get().toString();
            context.drawText(textRenderer, valueStr, x + WIDTH - PADDING - textRenderer.getWidth(valueStr), y + (HEIGHT - 8) / 2, textColorInt, false);
        } else if (setting instanceof KeyBindSetting bindSetting) {
            String valueStr;
            if (waitingForKeyBind == bindSetting) {
                valueStr = "Press a key...";
            } else {
                valueStr = KeyUtils.getKeyName(bindSetting.get());
            }
            context.drawText(textRenderer, valueStr, x + WIDTH - PADDING - textRenderer.getWidth(valueStr), y + (HEIGHT - 8) / 2, textColorInt, false);
        } else if (setting instanceof DoubleSetting doubleSetting) {
            renderSlider(context, x + PADDING, y + HEIGHT - 4,
                    WIDTH - 2 * PADDING, SLIDER_HEIGHT,
                    doubleSetting.get(), doubleSetting.getMin(), doubleSetting.getMax(), primary);

            double val = doubleSetting.get();
            double range = doubleSetting.getMax() - doubleSetting.getMin();
            String valStr;

            if (range <= 0.1) { // insane ifelse statement
                valStr = String.format("%.3f", val);
            } else if (range <= 10) {
                valStr = String.format("%.2f", val);
            } else if (range <= 1000) {
                valStr = String.format("%.1f", val);
            } else {
                valStr = String.format("%.0f", val);
            }

            context.drawText(textRenderer, valStr, x + WIDTH - PADDING - textRenderer.getWidth(valStr), y + (HEIGHT - 8) / 2, textColorInt, false);
        }

        if (hovered) {
            context.drawText(textRenderer, setting.getName(), x + PADDING, y + (HEIGHT - 8) / 2, toRGBA(brighten(textCol, 0.5f)), false);
        }
    }

    private static void renderHueSlider(DrawContext context, int x, int y, int width, int height, float hue) {
        for (int i = 0; i < width; i++) {
            float h = i / (float) width;
            Color color = Color.getHSBColor(h, 1f, 1f);
            context.fill(x + i, y, x + i + 1, y + height, toRGBA(color));
        }
        int pos = (int) (hue * width);
        context.fill(x + pos - 1, y, x + pos + 1, y + height, toRGBA(Color.WHITE));
    }

    private static void renderSlider(DrawContext context, int x, int y, int width, int height,
                                     double value, double min, double max, Color color) {
        context.fill(x, y, x + width, y + height, toRGBA(new Color(60, 60, 60, 150)));

        value = Math.max(min, Math.min(max, value));

        double percent = (value - min) / (max - min);
        int filledWidth = (int) (width * percent);

        context.fill(x, y, x + filledWidth, y + height, toRGBA(color));
    }

    private static boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    private static int toRGBA(Color color) {
        return (color.getAlpha() << 24) |
                (color.getRed() << 16) |
                (color.getGreen() << 8) |
                color.getBlue();
    }

    private static Color brighten(Color color, float amount) {
        int r = Math.min(255, (int)(color.getRed() + 255 * amount));
        int g = Math.min(255, (int)(color.getGreen() + 255 * amount));
        int b = Math.min(255, (int)(color.getBlue() + 255 * amount));
        return new Color(r, g, b, color.getAlpha());
    }

    private static boolean dragging = false;
    private static Setting<?> draggedSetting = null;
    private static int dragStartX = 0;
    private static double startValue = 0;

    public static boolean mouseClicked(Module module, double mouseX, double mouseY, int button, int x, int y) {
        if (button != 0 && button != 1) return false;

        List<Setting<?>> settings = module.getSettings();
        int curY = y + MODULE_SPACING;

        for (Setting<?> setting : settings) {
            if (isHovered(mouseX, mouseY, x, curY)) {
                if (setting instanceof BoolSetting boolSetting) {
                    boolSetting.toggle();
                    return true;
                } else if (setting instanceof IntSetting intSetting) {
                    startDragging(setting, mouseX);
                    return true;
                } else if (setting instanceof DoubleSetting doubleSetting) {
                    startDragging(setting, mouseX);
                    return true;
                } else if (setting instanceof EnumSetting<?> enumSetting) {
                    enumSetting.cycle();
                    return true;
                } else if (setting instanceof ColorSetting colorSetting) {
                    startDragging(setting, mouseX);
                    return true;
                } else if (setting instanceof KeyBindSetting keyBindSetting) {
                    if (waitingForKeyBind == null) {
                        waitingForKeyBind = keyBindSetting;
                    } else if (waitingForKeyBind == keyBindSetting) {
                        waitingForKeyBind = null;
                    }
                    return true;
                }
            }
            curY += HEIGHT + MODULE_SPACING;
        }
        return false;
    }

    public static void mouseDragged(double mouseX) {
        if (!dragging || draggedSetting == null) return;

        double deltaX = mouseX - dragStartX;

        if (draggedSetting instanceof ColorSetting colorSetting) {
            float[] hsb = Color.RGBtoHSB(colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue(), null);
            float newHue = (float) (startValue + deltaX * 0.005);
            newHue = Math.min(1f, Math.max(0f, newHue));
            Color newColor = Color.getHSBColor(newHue, 1f, 1f);
            colorSetting.setValue(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), colorSetting.getAlpha());
        } else if (draggedSetting instanceof IntSetting intSetting) {
            int newValue = slideInt((int) startValue, deltaX);
            intSetting.set(newValue);
        } else if (draggedSetting instanceof DoubleSetting doubleSetting) {
            double newValue = slideDouble(startValue, deltaX);
            doubleSetting.set(newValue);
        }
    }

    public static void mouseReleased() {
        dragging = false;
        draggedSetting = null;
    }

    private static void startDragging(Setting<?> setting, double mouseX) {
        dragging = true;
        draggedSetting = setting;
        dragStartX = (int) mouseX;
        if (setting instanceof IntSetting intSetting) {
            startValue = intSetting.get();
        } else if (setting instanceof DoubleSetting doubleSetting) {
            startValue = doubleSetting.get();
        } else if (setting instanceof ColorSetting colorSetting) {
            float[] hsb = Color.RGBtoHSB(colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue(), null);
            startValue = hsb[0];
        }
    }

    private static int slideInt(int start, double deltaX) {
        if (!(draggedSetting instanceof IntSetting intSetting)) return start;

        int min = intSetting.getMin();
        int max = intSetting.getMax();
        int range = max - min;

        int step = Math.max(1, range / 15);

        double sensitivity = 0.01;
        int delta = (int) Math.round(deltaX * sensitivity * range);

        int newValue = start + delta;

        newValue = Math.round((float)newValue / step) * step;

        newValue = Math.max(min, Math.min(max, newValue));
        return newValue;
    }

    private static double slideDouble(double start, double deltaX) {
        if (!(draggedSetting instanceof DoubleSetting doubleSetting)) return start;

        double min = doubleSetting.getMin();
        double max = doubleSetting.getMax();
        double range = max - min;

        double step = range / 15.0;

        double sensitivity = 0.01;

        double delta = deltaX * sensitivity * range;

        double newValue = start + delta;

        newValue = Math.round(newValue / step) * step;

        newValue = Math.max(min, Math.min(max, newValue));
        return newValue;
    }

    public static boolean keyPressed(int keyCode) {
        if (waitingForKeyBind != null) {
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                waitingForKeyBind.set(-1);
            } else {
                waitingForKeyBind.set(keyCode);
            }
            waitingForKeyBind = null;
            return true;
        }
        return false;
    }
}