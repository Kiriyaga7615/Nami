package me.kiriyaga.nami.feature.gui;

import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Set;

import static me.kiriyaga.nami.Nami.MODULE_MANAGER;
import static me.kiriyaga.nami.feature.gui.ClickGuiScreen.GUI_ALPHA;

public class ModulePanel {
    public static final int WIDTH = 130 - CategoryPanel.BORDER_WIDTH * 2 - SettingPanel.INNER_PADDING * 2;
    public static final int HEIGHT = 15;
    public static final int PADDING = 3;
    public static final int MODULE_SPACING = 2;

    private final Module module;
    private final Set<Module> expandedModules;

    private ColorModule getColorModule() {
        return MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
    }

    public ModulePanel(Module module, Set<Module> expandedModules) {
        this.module = module;
        this.expandedModules = expandedModules;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y);
        boolean expanded = expandedModules.contains(module);
        boolean enabled = module.isEnabled();

        Color primary = getColorModule().getStyledGlobalColor();
        Color secondary = getColorModule().getStyledSecondColor();
        Color textCol = new Color(255, 255, 255, GUI_ALPHA);
        Color textColActivated = new Color(255, 255, 255, 255);

        Color bgColor = hovered ? brighten(secondary, 0.4f) : (enabled ? primary : secondary);

        context.fill(x, y, x + WIDTH, y + HEIGHT, bgColor.getRGB());

        if (module.isEnabled())
            context.drawText(textRenderer, module.getName(), x + PADDING, y + (HEIGHT - 8) / 2, toRGBA(textColActivated), false);
        else
            context.drawText(textRenderer, module.getName(), x + PADDING, y + (HEIGHT - 8) / 2, toRGBA(textCol), false);

        String indicator = expanded ? "-" : "+";

        if (module.isEnabled())
            context.drawText(textRenderer, indicator, x + WIDTH - PADDING - textRenderer.getWidth(indicator), y + (HEIGHT - 8) / 2, toRGBA(textColActivated), false);
        else
            context.drawText(textRenderer, indicator, x + WIDTH - PADDING - textRenderer.getWidth(indicator), y + (HEIGHT - 8) / 2, toRGBA(textCol), false);
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y) {
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
}