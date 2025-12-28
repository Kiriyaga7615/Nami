/*
 Originally made by @cattyngmd
 https://github.com/cattyngmd/shulker-view
 MIT (2024)
*/
package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.*;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;

import me.kiriyaga.nami.util.container.ContainerUtils;
import me.kiriyaga.nami.util.container.ShulkerInfo;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.container.ContainerUtils.DyeColorToARGB;
import static me.kiriyaga.nami.util.container.ContainerUtils.openContainer;

@RegisterModule
public class ShulkerViewModule extends Module {

    public final BoolSetting tooltip = addSetting(new BoolSetting("Tooltip", true));
    public final BoolSetting compact = addSetting(new BoolSetting("Compact", true));
    public final BoolSetting bothSides = addSetting(new BoolSetting("BothSides", true));
    public final BoolSetting borders = addSetting(new BoolSetting("Borders", true));
    public final BoolSetting middleOpen = addSetting(new BoolSetting("MiddleclickOpen", false));
    public final DoubleSetting scale = addSetting(new DoubleSetting("Scale", 1, 0.5, 1.5));
    public final DoubleSetting scrollsensitivity = addSetting(new DoubleSetting("Sensitivity", 1, 0.5, 3));

    private final List<ShulkerInfo> shulkerList = new ArrayList<>();
    private final int GRID_WIDTH = 20;
    private final int GRID_HEIGHT = 18;
    private final int MARGIN = 2;

    private int currentY = 0;
    private int startX = 0;
    private int offset = 0;
    private int totalHeight = 0;

    private double clickedX = -1, clickedY = -1;
    private int button = -1;

    public ShulkerViewModule() {
        super("ShulkerView", "Improves shulker management.", ModuleCategory.of("Render"),"shulkerview");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTick(PreTickEvent event) {
        shulkerList.clear();

        if (!(MC.currentScreen instanceof HandledScreen<?> screen)) return;

        for (Slot slot : screen.getScreenHandler().slots) {
            ShulkerInfo info = ShulkerInfo.create(slot.getStack(), slot.id, compact.get());
            if (info != null) shulkerList.add(info);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(RenderScreenEvent event) {
        if (!(MC.currentScreen instanceof HandledScreen)) return;

        DrawContext context = event.getDrawContext();
        boolean right = false;
        int edgePadding = 6;
        currentY = bothSides.get() ? edgePadding : edgePadding + offset;
        startX = edgePadding;
        float scale = this.scale.get().floatValue();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        for (ShulkerInfo info : shulkerList) {
            int rows = info.rows();
            int cols = info.cols();

            int width = cols * GRID_WIDTH + MARGIN * cols;
            int height = rows * GRID_HEIGHT + MARGIN * rows;

            if (currentY + height > MC.getWindow().getScaledHeight() / scale && bothSides.get() && !right) {
                right = true;
                currentY = edgePadding + offset;
            }

            if (right) {
                startX = (int) ((MC.getWindow().getScaledWidth() - width - edgePadding) / scale);
            }

            context.fill(startX, currentY, startX + width, currentY + height, new Color(0, 0, 0, 75).getRGB());

            if (borders.get()){
                int borderColor = getShulkerColor(info.shulker());
                drawBorder(context, startX, currentY, width, height, borderColor);
            }

            int count = 0;
            for (ItemStack stack : info.stacks()) {
                if (compact.get() && stack.isEmpty()) break;
                int x = startX + (count % info.cols()) * GRID_WIDTH + MARGIN;
                int y = currentY + (count / info.cols()) * GRID_HEIGHT + MARGIN;

                context.drawItem(stack, x, y);
                context.drawStackOverlay(MC.textRenderer, stack, x, y, null);

                if (tooltip.get() && !stack.isEmpty() && isHovered(event.getMouseX(), event.getMouseY(), x, y, 16, 16, scale)) {
                    context.drawItemTooltip(MC.textRenderer, stack, (int) event.getMouseX(), (int) event.getMouseY());
                }

                count++;
            }

            if (button != -1 && clickedX != -1 && clickedY != -1 && isHovered(clickedX, clickedY, startX, currentY, width, height, scale)) {
                if (button == 0)
                    INVENTORY_MANAGER.getClickHandler().pickupSlot(info.slot(), true);

                if (button == 2 && middleOpen.get())
                    openContainer(info.shulker());

                clickedX = clickedY = button = -1;
            }

            currentY += height + MARGIN;
        }

        context.getMatrices().popMatrix();
        totalHeight = currentY - offset;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClick(MouseClickEvent event) {
        if (event.button() == 0 || event.button() == 2) {
            clickedX = event.mouseX();
            clickedY = event.mouseY();
            button = event.button();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScroll(MouseScrollEvent event) {
      //  CHAT_MANAGER.sendRaw("mouse scroll event called");
        float maxOffset = Math.min(-totalHeight + MC.getWindow().getScaledHeight() / (scale.get()).floatValue(), 0);
        offset = (int) MathHelper.clamp(offset + (int) Math.ceil(event.amount()) * (scrollsensitivity.get() * 10), maxOffset, 0);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private int getShulkerColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return ColorHelper.getArgb(255, 128, 128, 128);

        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock shulker)) return ColorHelper.getArgb(255, 128, 128, 128);

        DyeColor color = shulker.getColor();
        if (color == null) return ColorHelper.getArgb(255, 128, 0, 128);

        return DyeColorToARGB(color);
    }

    private boolean isHovered(double mx, double my, int x, int y, int width, int height, float scale) {
        mx /= scale;
        my /= scale;
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
}
