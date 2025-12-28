package me.kiriyaga.nami.feature.gui.newgui.base;

import me.kiriyaga.nami.feature.gui.newgui.widget.ActionWidget;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.util.render.ScissorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static me.kiriyaga.nami.Nami.*;

public class DataPanel<T extends BaseEntry> {
    protected final List<T> entries = new ArrayList<>();
    protected final Function<T, Component> displayMapper;

    protected final PanelRenderer panelRenderer = new PanelRenderer();
    protected final ActionWidget actionWidget = new ActionWidget();

    protected double scrollOffset = 0;
    protected double targetScrollOffset = 0;

    protected final int headerHeight = 20;
    protected final int inputHeight = 20;

    protected String name;
    protected int x, y, width, height;

    protected boolean dragging = false;
    protected int dragOffsetX = 0;
    protected int dragOffsetY = 0;

    public DataPanel(String name, int x, int y, int width, int height, Function<T, Component> displayMapper) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.displayMapper = displayMapper;
    }

    public void setEntries(List<T> items) {
        entries.clear();
        entries.addAll(items);
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        panelRenderer.renderPanel(context, x, y, width, height, headerHeight);
        panelRenderer.renderHeaderText(context, textRenderer, name, x, y, headerHeight, 4);

        int contentY = y + headerHeight + 4;
        int lineHeight = FONT_MANAGER.getHeight() + 4;
        int contentHeight = height - headerHeight - inputHeight - 8;
        int maxVisible = contentHeight / lineHeight;

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.3;
        double maxScroll = Math.max(0, entries.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int start = (int) Math.floor(scrollOffset);
        double partialOffset = scrollOffset - start;

        ScissorUtil.enable(context, x, contentY, x + width, contentY + contentHeight);
        int drawY = contentY - (int) (partialOffset * lineHeight);

        for (int i = start; i < Math.min(entries.size(), start + maxVisible + 1); i++) {
            T item = entries.get(i);
            Component display = displayMapper.apply(item);
            if (display != null) {
                FONT_MANAGER.drawText(context, display, x + 4, drawY, 0xFFFFFFFF, true);
            }
            drawY += lineHeight;
        }

        if (entries.size() > maxVisible) {
            int barX = x + width - 3;
            int barY = contentY;
            int barHeight = contentHeight;
            context.fill(barX, barY, barX + 1, barY + barHeight, MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledSecondColor().getRGB());
            float ratio = (float) maxVisible / entries.size();
            int whiteHeight = Math.max((int)(barHeight * ratio), 2);
            int whiteY = barY + (int)(scrollOffset / (entries.size() - maxVisible) * (barHeight - whiteHeight));
            context.fill(barX, whiteY, barX + 1, whiteY + whiteHeight, MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledGlobalColor().getRGB());
        }

        ScissorUtil.disable(context);

        if (actionWidget.isVisible()) {
            actionWidget.render(context, textRenderer, mouseX, mouseY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentY = y + headerHeight + 4;
        int contentHeight = height - headerHeight - inputHeight - 8;

        if (mouseX >= x && mouseX <= x + width && mouseY >= contentY && mouseY <= contentY + contentHeight) {
            int lineHeight = FONT_MANAGER.getHeight() + 4;
            int maxVisible = contentHeight / lineHeight;
            double maxScroll = Math.max(0, entries.size() - maxVisible);

            targetScrollOffset -= scrollDelta * 1.5;
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (actionWidget.isVisible() && actionWidget.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0 && isHeaderHovered(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int) (mouseX - x);
            dragOffsetY = (int) (mouseY - y);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    protected boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public ActionWidget getActionWidget() {
        return actionWidget;
    }

    public int getHeaderHeight() { return this.headerHeight; }
    public int getInputHeight() { return this.inputHeight; }
    public double getScrollOffset() { return this.scrollOffset; }
    public List<T> getEntries() { return this.entries; }
    public int getX() {return x;}
    public int getY() {return y;}
    public int getWidth() {return width;}
    public int getHeight() {return height;}
}
