package me.kiriyaga.nami.feature.gui.oldgui.screen;

import me.kiriyaga.nami.feature.gui.oldgui.components.CategoryPanel;
import me.kiriyaga.nami.feature.gui.oldgui.components.ModulePanel;
import me.kiriyaga.nami.feature.gui.oldgui.components.SettingPanel;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.*;
import java.awt.Point;

import static me.kiriyaga.nami.Nami.*;

public class ClickGuiScreen extends Screen {
    private final Map<ModuleCategory, Point> categoryPositions = new HashMap<>();
    private final Map<ModuleCategory, CategoryPanel> categoryPanels = new HashMap<>();
    private boolean draggingCategory = false;
    private ModuleCategory draggedModuleCategory = null;
    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;
    public float scale = 1;
    private Screen previousScreen = null;
    private static final long FADE_DURATION_MS = 122L;
    private long fadeStartMs = Util.getMillis();
    private boolean closing = false;
    public boolean searchActive = false;
    public String searchQuery = "";
    private long lastCursorBlink = 0L;

    private ClickGuiModule getClickGuiModule() {
        return MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
    }

    private final List<Component> statusMessages = Arrays.asList(
            Component.literal("Press Ctrl+F to search modules."),
            Component.literal("Middle-click a module to toggle its drawn state."),
            Component.literal("Middle-click a keybind to switch hold/toggle mode.")
    );

    public ClickGuiScreen() {
        super(Component.literal("NamiGui"));
        syncCategoryPositions();
        initCategoryPanels();
    }

    private void syncCategoryPositions() {
        int x = 20;
        int y = 20;
        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;
            categoryPositions.putIfAbsent(moduleCategory, new Point(x, y));
            x += CategoryPanel.WIDTH + 1;
        }
        categoryPositions.keySet().removeIf(cat -> !ModuleCategory.getAll().contains(cat));
    }

    private void initCategoryPanels() {
        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;
            categoryPanels.putIfAbsent(moduleCategory,
                    new CategoryPanel(moduleCategory));
        }
        categoryPanels.keySet().removeIf(cat -> !ModuleCategory.getAll().contains(cat));
    }

    @Override
    protected void init() {
        super.init();
        fadeStartMs = Util.getMillis();
        closing = false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        checkClose();
        syncCategoryPositions();

        if (previousScreen instanceof TitleScreen
                || previousScreen instanceof DisconnectedScreen
                || previousScreen instanceof JoinMultiplayerScreen) {
            previousScreen.render(context, -1, -1, delta);
        }

        ClickGuiModule clickGuiModule = getClickGuiModule();
        if (clickGuiModule != null && clickGuiModule.background.get()) {
            renderMenuBackground(context);
/*            int alpha = (clickGuiModule.backgroundAlpha.get() & 0xFF) << 24;
            int color = alpha | (MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledGlobalColor().getRGB() & 0xFFFFFF);
            context.fill(0, 0, this.width, this.height, applyFade(color));*/
        }

        NAVIGATE_PANEL.render(context, FONT_MANAGER.rendererProvider.getRenderer(), mouseX, mouseY);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);

        int startY = (scaledHeight - 1);
        for (int i = statusMessages.size() - 1; i >= 0; i--) {
            Component message = statusMessages.get(i);

            if (searchActive && message.getString().contains("Ctrl+F")) {
                continue;
            }

            int textWidth = FONT_MANAGER.getWidth(message);
            int textHeight = FONT_MANAGER.getHeight();

            int x = (int) (scaledWidth - textWidth - 1);
            int y = startY - textHeight;

            FONT_MANAGER.drawText(context, message, x, y, applyFade(MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextColor(255).getRGB()), true);
            startY = y;
        }

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        for (CategoryPanel panel : categoryPanels.values()) {
            panel.setModuleFilter(this::matchesSearch);
        }

        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;

            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(moduleCategory);
            if (panel != null)
                panel.render(context, FONT_MANAGER.rendererProvider.getRenderer(), pos.x, pos.y, scaledMouseX, scaledMouseY, scaledHeight);
        }

        if (clickGuiModule != null && clickGuiModule.descriptions.get()) {
            for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
                if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;

                Point pos = categoryPositions.get(moduleCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(moduleCategory);
                if (panel == null) continue;

                if (!panel.isMouseOverContent(scaledMouseX, scaledMouseY, pos.x, pos.y, scaledHeight)) {
                    continue;
                }

                double scrollOffset = panel.getScrollOffset();

                List<Module> modules = MODULE_MANAGER.getStorage().getByCategory(moduleCategory);
                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + ModulePanel.MODULE_SPACING + CategoryPanel.BOTTOM_MARGIN
                        - (int) scrollOffset;

                for (Module module : modules) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;
                    int modY = curY;

                    if (ModulePanel.isHovered(scaledMouseX, scaledMouseY, modX, modY)) {
                        String description = module.getDescription();
                        if (description != null && !description.isEmpty()) {
                            int descX = scaledMouseX + 5;
                            int descY = scaledMouseY;
                            int textWidth = FONT_MANAGER.getWidth(description);
                            int textHeight = 8;

                            context.fill(descX - 2, descY - 2, descX + textWidth + 2, descY + textHeight + 2, 0x7F000000);
                            FONT_MANAGER.drawText(context, description, descX, descY, applyFade(MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextColor(255).getRGB()), true);
                        }
                        break;
                    }

                    curY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;
                    if (module.isExpanded()) {
                        curY += SettingPanel.getSettingsHeight(module);
                    }
                }
            }
        }

        context.pose().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int i, int j, float f) {
        if (MC.level != null && MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).blur.get())
            this.renderBlurredBackground(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        syncCategoryPositions();

        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);
        int scaledHeight = (int) (this.height / scale);

        NAVIGATE_PANEL.mouseClicked(click.x(), click.y(), FONT_MANAGER.rendererProvider.getRenderer());

        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;

            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            if (CategoryPanel.isHeaderHovered(scaledMouseX, scaledMouseY, pos.x, pos.y)) {
                if (click.button() == 0) {
                    playClickSound();
                    draggingCategory = true;
                    draggedModuleCategory = moduleCategory;
                    dragStartX = scaledMouseX;
                    dragStartY = scaledMouseY;
                    initialCategoryX = pos.x;
                    initialCategoryY = pos.y;
                    return true;
                }
            }
        }

        if (!draggingCategory) {
            for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
                if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;

                Point pos = categoryPositions.get(moduleCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(moduleCategory);
                if (panel == null) continue;

                if (!panel.isMouseOverContent(scaledMouseX, scaledMouseY, pos.x, pos.y, scaledHeight)) {
                    continue;
                }

                double scrollOffset = panel.getScrollOffset();

                List<Module> modules = MODULE_MANAGER.getStorage().getByCategory(moduleCategory);

                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + ModulePanel.MODULE_SPACING + CategoryPanel.BOTTOM_MARGIN
                        - (int) scrollOffset;

                for (Module module : modules) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;

                    if (ModulePanel.isHovered(scaledMouseX, scaledMouseY, modX, curY)) {
                        if (click.button() == 0) {
                            playClickSound();
                            module.toggle();
                        } else if (click.button() == 1) {
                            if (module.isExpanded()) {
                                module.setExpanded(false);
                            } else {
                                module.setExpanded(true);
                            }
                            playClickSound();
                        } else if (click.button() == 2) {
                            playClickSound();
                            module.setDrawn(!module.isDrawn());
                        }
                        return true;
                    }

                    curY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;

                    if (module.isExpanded()) {
                        if (SettingPanel.mouseClicked(module, scaledMouseX, scaledMouseY, click.button(), modX, curY)) {
                            return true;
                        }
                        curY += SettingPanel.getSettingsHeight(module);
                    }
                }
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == 70 && (keyInput.modifiers() & 2) != 0) {
            searchActive = !searchActive;
            if (!searchActive) {
                searchQuery = "";
            } else {
                lastCursorBlink = Util.getMillis();
            }
            playClickSound();
            return true;
        }

        if (searchActive) {
            if (keyInput.input() == 256) {
                searchActive = false;
                searchQuery = "";
                playClickSound();
                return true;
            }

            if (keyInput.input() == 259 && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }

            return true;
        }

        if (keyInput.input() == MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).getKeyBind().get() && MC.screen == CLICK_GUI && MC.level != null) {
            beginClose();
            return true;
        }
        if (keyInput.input() == 256) {
            beginClose();
            return true;
        }

        if (SettingPanel.keyPressed(keyInput.input())) return true;
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharacterEvent charInput) {
        if (searchActive) {
            String character = charInput.codepointAsString();
            if (character != null && !character.isEmpty()) {
                searchQuery += character;
                return true;
            }
        }
        return super.charTyped(charInput);
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        fadeStartMs = Util.getMillis();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double d, double e) {
        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        if (draggingCategory && draggedModuleCategory != null) {
            Point currentPos = categoryPositions.get(draggedModuleCategory);
            if (currentPos != null) {
                currentPos.x = initialCategoryX + (scaledMouseX - dragStartX);
                currentPos.y = initialCategoryY + (scaledMouseY - dragStartY);
                return true;
            }
        }

        SettingPanel.mouseDragged(scaledMouseX, scaledMouseY);
        return super.mouseDragged(click,  d,  e);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingCategory = false;
        draggedModuleCategory = null;
        SettingPanel.mouseReleased(click);
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledHeight = (int) (this.height / scale);

        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;

            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(moduleCategory);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y, scaledHeight)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void playClickSound() {
        MC.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
        ));
    }

    public Screen getPreviousScreen() {
        return previousScreen;
    }

    public void setPreviousScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    private float getFadeFactor() {
        long elapsed = Util.getMillis() - fadeStartMs;
        float t = Math.min(1.0f, Math.max(0.0f, elapsed / (float) FADE_DURATION_MS));
        return closing ? (1.0f - t) : t;
    }

    private void checkClose() { // shitcode ikik
        ClickGuiModule clickGuiModule = MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
        if (!closing) return;

        if (clickGuiModule.fade.get()) {
            if (getFadeFactor() <= 0.0f) {
                MC.setScreen(null);
            }
        } else {
            MC.setScreen(null);
        }
    }

    public int applyFade(int argb) {
        if (!MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).fade.get())
            return argb;

        if ((previousScreen == HUD_EDITOR || previousScreen == FRIEND) && MC.screen != CLICK_GUI)
            return argb;

        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        float factor = getFadeFactor();
        int newA = Math.round(a * factor);
        if (newA < 0) newA = 0;
        if (newA > a) newA = a;
        return (newA << 24) | rgb;
    }

    public boolean matchesSearch(Module module) {
        if (module instanceof me.kiriyaga.nami.feature.module.impl.client.ModuleSearchModule) {
            return true;
        }
        if (!searchActive || searchQuery.isEmpty()) {
            return true;
        }
        String query = searchQuery.toLowerCase();
        return module.getName().toLowerCase().contains(query);
    }
}
