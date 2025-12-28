package me.kiriyaga.nami.feature.gui.oldgui.screen;

import me.kiriyaga.nami.feature.gui.oldgui.components.CategoryPanel;
import me.kiriyaga.nami.feature.gui.oldgui.components.ModulePanel;
import me.kiriyaga.nami.feature.gui.oldgui.components.SettingPanel;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import bhs.bhs;

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
    private long fadeStartMs = bhs.getMeasuringTimeMs();
    private boolean closing = false;

    private ClickGuiModule getClickGuiModule() {
        return MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
    }

    private final List<Text> statusMessages = Arrays.asList(
            Text.literal("Middle-click a module to toggle its drawn state."),
            Text.literal("Middle-click a keybind to switch hold/toggle mode.")
    );

    public ClickGuiScreen() {
        super(Text.literal("NamiGui"));
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
        fadeStartMs = bhs.getMeasuringTimeMs();
        closing = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        checkClose();
        syncCategoryPositions();

        if (previousScreen instanceof TitleScreen
                || previousScreen instanceof DisconnectedScreen
                || previousScreen instanceof MultiplayerScreen) {
            previousScreen.render(context, -1, -1, delta);
        }

        ClickGuiModule clickGuiModule = getClickGuiModule();
        if (clickGuiModule != null && clickGuiModule.background.get()) {
            renderDarkening(context);
/*            int alpha = (clickGuiModule.backgroundAlpha.get() & 0xFF) << 24;
            int color = alpha | (MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledGlobalColor().getRGB() & 0xFFFFFF);
            context.fill(0, 0, this.width, this.height, applyFade(color));*/
        }

        NAVIGATE_PANEL.render(context, FONT_MANAGER.rendererProvider.getRenderer(), mouseX, mouseY);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);

        int startY = (scaledHeight - 1);
        for (int i = statusMessages.size() - 1; i >= 0; i--) {
            Text message = statusMessages.get(i);
            int textWidth = FONT_MANAGER.getWidth(message);
            int textHeight = FONT_MANAGER.getHeight();

            int x = (int) (scaledWidth - textWidth - 1);
            int y = startY - textHeight;

            FONT_MANAGER.drawText(context, message, x, y, applyFade(MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextColor(255).getRGB()), true);
            startY = y;
        }

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

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
                        context.getMatrices().popMatrix();
                        return;
                    }

                    curY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;
                    if (module.isExpanded()) {
                        curY += SettingPanel.getSettingsHeight(module);
                    }
                }
            }
        }

        context.getMatrices().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int i, int j, float f) {
        if (MC.world != null && MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).blur.get())
            this.applyBlur(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        syncCategoryPositions();

        int scaledMouseX = (int) (click.comp_4798() / scale);
        int scaledMouseY = (int) (click.comp_4799() / scale);
        int scaledHeight = (int) (this.height / scale);

        NAVIGATE_PANEL.mouseClicked(click.comp_4798(), click.comp_4799(), FONT_MANAGER.rendererProvider.getRenderer());

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
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.getKeycode() == MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).getKeyBind().get() && MC.currentScreen == CLICK_GUI && MC.world != null) {
            beginClose();
            return true;
        }
        if (keyInput.getKeycode() == 256) {
            beginClose();
            return true;
        }

        if (SettingPanel.keyPressed(keyInput.getKeycode())) return true;
        return super.keyPressed(keyInput);
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        fadeStartMs = bhs.getMeasuringTimeMs();
    }

    @Override
    public boolean mouseDragged(Click click, double d, double e) {
        int scaledMouseX = (int) (click.comp_4798() / scale);
        int scaledMouseY = (int) (click.comp_4799() / scale);

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
    public boolean mouseReleased(Click click) {
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
    public boolean shouldPause() {
        return false;
    }

    private void playClickSound() {
        MC.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f
        ));
    }

    public Screen getPreviousScreen() {
        return previousScreen;
    }

    public void setPreviousScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    private float getFadeFactor() {
        long elapsed = bhs.getMeasuringTimeMs() - fadeStartMs;
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

        if ((previousScreen == HUD_EDITOR || previousScreen == FRIEND) && MC.currentScreen != CLICK_GUI)
            return argb;

        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        float factor = getFadeFactor();
        int newA = Math.round(a * factor);
        if (newA < 0) newA = 0;
        if (newA > a) newA = a;
        return (newA << 24) | rgb;
    }
}
