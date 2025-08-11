package me.kiriyaga.nami.feature.gui.components;

import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.Set;

import static me.kiriyaga.nami.Nami.CLICK_GUI;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;
import static me.kiriyaga.nami.feature.gui.base.GuiConstants.*;

public class ModulePanel {
    public static final int WIDTH = 110 - CategoryPanel.BORDER_WIDTH * 2 - SettingPanel.INNER_PADDING * 2;
    public static final int HEIGHT = 14;
    public static final int PADDING = 3;
    public static final int MODULE_SPACING = 1;

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
        boolean enabled = module.isEnabled();
        boolean expanded = expandedModules.contains(module);

        Color primary = getColorModule().getStyledGlobalColor();
        Color secondary = getColorModule().getStyledSecondColor();
        Color textCol = new Color(255, 255, 255, 122);
        Color textColActivated = MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).moduleFill.get()
                ? new Color(255, 255, 255, 255)
                : new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 255);

        Color bgColor;
        if (MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).moduleFill.get())
            bgColor = enabled ? primary : secondary;
        else
            bgColor = new Color(30, 30, 30, 0);

        context.fill(expanded && MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).expandedIdentifier.get() ? x + 1 : x, y, x + WIDTH, y + HEIGHT, toRGBA(bgColor));

        if (expanded && MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).expandedIdentifier.get()) {
            context.fill(x, y, x + 1, y + HEIGHT, enabled ? primary.getRGB() : secondary.getRGB());
        }

        int textY = y + (HEIGHT - 8) / 2;
        int baseTextX = x + PADDING + (hovered ? 1 : 0);

        context.drawText(
                textRenderer,
                module.getName(),
                baseTextX,
                textY,
                toRGBA(enabled ? textColActivated : textCol),
                false
        );
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    private float approach(float current, float target, float maxDelta) {
        if (current < target) {
            current += maxDelta;
            if (current > target) current = target;
        } else if (current > target) {
            current -= maxDelta;
            if (current < target) current = target;
        }
        return current;
    }
}