package me.kiriyaga.nami.feature.gui.newgui.component;

import me.kiriyaga.nami.feature.gui.oldgui.screen.ClickGuiScreen;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.feature.gui.newgui.base.PanelRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.ColorUtils.toRGBA;

public class NavigatePanelComponent {
    private static final int HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int TOP_OFFSET = 1;

    private final PanelRenderer renderer = new PanelRenderer();
    private final Map<String, Screen> screens = new LinkedHashMap<>();

    private final ColorModule colorModule;
    private final ClickGuiModule clickGuiModule;
    private String activeKey;

    public NavigatePanelComponent() {
        addScreen("ClickGui", CLICK_GUI);
        addScreen("HudEditor", HUD_EDITOR);
        addScreen("Friends", FRIEND);
        addScreen("Logs", LOG);
        this.colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        this.clickGuiModule = MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
        resetActive();
    }

    public void addScreen(String name, Screen screen) {
        screens.put(name, screen);
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(CLICK_GUI.scale, CLICK_GUI.scale);

        int scaledWidth = (int) (MC.getWindow().getScaledWidth() / CLICK_GUI.scale);
        int totalWidth = calcWidth();
        int x = (scaledWidth - totalWidth) / 2;
        int y = TOP_OFFSET;

        renderer.renderPanel(context, x, y, totalWidth, HEIGHT, 0, false);

        int offsetX = x + PADDING;
        for (Map.Entry<String, Screen> entry : screens.entrySet()) {
            String name = entry.getKey();
            boolean active = name.equals(activeKey);

            Color primary = colorModule.getStyledGlobalColor();
            Color textOff = colorModule.getStyledTextSecondColor(255);
            Color textCol = active
                    ? colorModule.getStyledTextColor(255)
                    : textOff;

            int textWidth = FONT_MANAGER.getWidth(name);
            FONT_MANAGER.drawText(
                    context,
                    name,
                    offsetX,
                    (y + (HEIGHT - FONT_MANAGER.getHeight()) / 2),
                    CLICK_GUI.applyFade(toRGBA(textCol)),
                    true
            );

            offsetX += textWidth + PADDING * 2;
        }

        context.getMatrices().popMatrix();
    }

    public void mouseClicked(double mouseX, double mouseY, Font textRenderer) {
        double scaledX = mouseX / CLICK_GUI.scale;
        double scaledY = mouseY / CLICK_GUI.scale;

        int scaledWidth = (int) (MC.getWindow().getScaledWidth() / CLICK_GUI.scale);
        int totalWidth = calcWidth();
        int x = (scaledWidth - totalWidth) / 2;
        int y = TOP_OFFSET;

        int offsetX = x + PADDING;
        for (Map.Entry<String, Screen> entry : screens.entrySet()) {
            String name = entry.getKey();
            Screen screen = entry.getValue();
            int textWidth = FONT_MANAGER.getWidth(name);

            int startX = offsetX;
            int endX = offsetX + textWidth + PADDING * 2;

            if (scaledX >= startX && scaledX <= endX && scaledY >= y && scaledY <= y + HEIGHT) {
                if (!name.equals(activeKey)) {
                    activeKey = name;
                    if (screen instanceof ClickGuiScreen screen1)
                        screen1.setPreviousScreen(MC.currentScreen);
                    MC.setScreen(screen);
                }
                return;
            }
            offsetX = endX;
        }
    }

    public int calcWidth() {
        int width = PADDING;
        for (String key : screens.keySet()) {
            width += FONT_MANAGER.getWidth(key) + PADDING * 2;
        }
        return width;
    }

    public void resetActive() {
        if (screens.isEmpty()) return;
        activeKey = screens.keySet().iterator().next();
    }
}
