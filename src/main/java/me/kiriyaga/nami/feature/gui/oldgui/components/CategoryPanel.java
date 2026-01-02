package me.kiriyaga.nami.feature.gui.oldgui.components;

import me.kiriyaga.nami.feature.gui.newgui.base.PanelRenderer;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.util.render.ScissorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.feature.gui.oldgui.components.ModulePanel.MODULE_SPACING;

public class CategoryPanel {
    public static final int WIDTH = 100;
    public static final int HEADER_HEIGHT = 12;
    private static final int PADDING = 5;
    public static final int BORDER_WIDTH = 1;
    public static final int BOTTOM_MARGIN = 1;

    private final ModuleCategory moduleCategory;
    private final PanelRenderer renderer = new PanelRenderer();

    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private Predicate<Module> moduleFilter = module -> true;

    public CategoryPanel(ModuleCategory moduleCategory) {
        this.moduleCategory = moduleCategory;
    }

    public void setModuleFilter(Predicate<Module> filter) {
        this.moduleFilter = filter;
    }

    private List<Module> getModules() {
        return MODULE_MANAGER.getStorage().getByCategory(moduleCategory).stream()
                .filter(moduleFilter)
                .collect(Collectors.toList());
    }

    public void render(GuiGraphics context, Font textRenderer, int x, int y, int mouseX, int mouseY, int screenHeight) {

        List<Module> modules = getModules();

        int dynamicContentHeight = 0;
        for (Module module : modules) {
            dynamicContentHeight += ModulePanel.HEIGHT + MODULE_SPACING;
            if (module.isExpanded()) {
                dynamicContentHeight += SettingPanel.getSettingsHeight(module);
            }
        }

        int fullUnclampedHeight = HEADER_HEIGHT + BOTTOM_MARGIN + MODULE_SPACING + dynamicContentHeight + MODULE_SPACING;

        int maxAllowedHeight = screenHeight - y - 1;
        int basePanelHeight = Math.min(fullUnclampedHeight, maxAllowedHeight);

        renderer.renderPanel(context, x, y, WIDTH, basePanelHeight, HEADER_HEIGHT);
        renderer.renderHeaderText(context, textRenderer, moduleCategory.getName(), x, y, HEADER_HEIGHT, PADDING);
        int contentY = y + HEADER_HEIGHT + MODULE_SPACING + BOTTOM_MARGIN;
        int visibleHeight = Math.min(basePanelHeight - HEADER_HEIGHT - MODULE_SPACING - BOTTOM_MARGIN, screenHeight - contentY - 1);

        boolean anyExpanded = modules.stream().anyMatch(Module::isExpanded);
        if (anyExpanded) {
            visibleHeight -= 1;
            if (visibleHeight < 0) visibleHeight = 0;
        }

        int scrollableHeight = 0;
        for (Module module : modules) {
            scrollableHeight += ModulePanel.HEIGHT + MODULE_SPACING;
            if (module.isExpanded()) {
                scrollableHeight += SettingPanel.getSettingsHeight(module);
            }
        }

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.1;
        double maxScroll = Math.max(0, scrollableHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        ScissorUtil.enable(context, x, contentY, x + WIDTH, contentY + visibleHeight);

        int moduleY = contentY - (int) scrollOffset;

        for (Module module : modules) {

            ModulePanel modulePanel = new ModulePanel(module);

            int moduleX = x + BORDER_WIDTH + SettingPanel.INNER_PADDING;
            int panelOffset = 1;
            int startY = moduleY;

            int expandedHeight = 0;
            if (module.isExpanded()) {
                expandedHeight = SettingPanel.getSettingsHeight(module);
            }

            if (module.isExpanded()) {
                int panelX = moduleX - panelOffset + 1;
                int panelY = startY - panelOffset + 1;
                int panelHeight = ModulePanel.HEIGHT + expandedHeight + (panelOffset * 2) - 1;
                int panelWidth = WIDTH - (BORDER_WIDTH + SettingPanel.INNER_PADDING) * 2 + panelOffset * 2 - 2;

                if (MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).panels.get())
                    renderer.renderPanel(context, panelX, panelY, panelWidth, panelHeight, 0, false, true);
            }

            modulePanel.render(context, textRenderer, moduleX, moduleY, mouseX, mouseY);

            moduleY += ModulePanel.HEIGHT + MODULE_SPACING;

            if (module.isExpanded()) {
                SettingPanel.renderSettings(context, textRenderer, module, moduleX, moduleY, mouseX, mouseY);
                moduleY += expandedHeight;
            }
        }
        ScissorUtil.disable(context);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, int x, int y, int screenHeight) {
        List<Module> modules = getModules();

        int dynamicContentHeight = 0;
        for (Module module : modules) {
            dynamicContentHeight += ModulePanel.HEIGHT + MODULE_SPACING;
            if (module.isExpanded()) {
                dynamicContentHeight += SettingPanel.getSettingsHeight(module);
            }
        }

        int fullUnclampedHeight = HEADER_HEIGHT + BOTTOM_MARGIN + MODULE_SPACING + dynamicContentHeight + MODULE_SPACING;
        int maxAllowedHeight = screenHeight - y - 1;
        int basePanelHeight = Math.min(fullUnclampedHeight, maxAllowedHeight);

        int contentY = y + HEADER_HEIGHT + MODULE_SPACING + BOTTOM_MARGIN;

        int visibleHeight = Math.min(basePanelHeight - HEADER_HEIGHT - MODULE_SPACING - BOTTOM_MARGIN,
                screenHeight - contentY - 1);

        boolean anyExpanded = modules.stream().anyMatch(Module::isExpanded);
        if (anyExpanded) {
            visibleHeight -= 1;
            if (visibleHeight < 0) visibleHeight = 0;
        }

        if (mouseX >= x && mouseX <= x + WIDTH &&
                mouseY >= contentY && mouseY <= contentY + visibleHeight) {

            targetScrollOffset -= scrollDelta * 45;

            double maxScroll = Math.max(0, dynamicContentHeight - visibleHeight);
            if (targetScrollOffset < 0) targetScrollOffset = 0;
            if (targetScrollOffset > maxScroll) targetScrollOffset = maxScroll;

            return true;
        }

        return false;
    }


    public boolean isMouseOverContent(double mouseX, double mouseY, int x, int y, int screenHeight) {
        List<Module> modules = getModules();

        int dynamicContentHeight = 0;
        for (Module module : modules) {
            dynamicContentHeight += ModulePanel.HEIGHT + MODULE_SPACING;
            if (module.isExpanded()) {
                dynamicContentHeight += SettingPanel.getSettingsHeight(module);
            }
        }

        int fullUnclampedHeight = HEADER_HEIGHT + BOTTOM_MARGIN + MODULE_SPACING + dynamicContentHeight + MODULE_SPACING;
        int maxAllowedHeight = screenHeight - y - 1;
        int basePanelHeight = Math.min(fullUnclampedHeight, maxAllowedHeight);

        int contentY = y + HEADER_HEIGHT + MODULE_SPACING + BOTTOM_MARGIN;

        int visibleHeight = Math.min(basePanelHeight - HEADER_HEIGHT - MODULE_SPACING - BOTTOM_MARGIN,
                screenHeight - contentY - 1);

        boolean anyExpanded = modules.stream().anyMatch(Module::isExpanded);
        if (anyExpanded) {
            visibleHeight -= 1;
            if (visibleHeight < 0) visibleHeight = 0;
        }

        return mouseX >= x && mouseX <= x + WIDTH &&
                mouseY >= contentY && mouseY <= contentY + visibleHeight;
    }

    public static boolean isHeaderHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }
}
