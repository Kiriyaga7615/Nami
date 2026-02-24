/*
 Originally made by @cattyngmd
 https://github.com/cattyngmd/shulker-view
 MIT (2024)
*/
package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.MouseScrollEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.RenderScreenEvent;
import namidevelopment.kiriyaga.api.event.impl.RenderTooltipEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import namidevelopment.kiriyaga.api.util.container.ShulkerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.DyeColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.container.ContainerUtils.DyeColorToARGB;

@RegisterFeature
public class ShulkerViewFeature extends Feature {

    public enum Mode { MULTI, SINGLE }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.SINGLE));
    public final BoolSetting tooltip = addSetting(new BoolSetting("Tooltip", true));
    public final BoolSetting compact = addSetting(new BoolSetting("Compact", false));
    public final BoolSetting bothSides = addSetting(new BoolSetting("BothSides", true));
    public final BoolSetting borders = addSetting(new BoolSetting("Borders", true));
    public final DoubleSetting scale = addSetting(new DoubleSetting("Scale", 1, 0.5, 1.5));
    public final DoubleSetting scrollSensitivity = addSetting(new DoubleSetting("Sensitivity", 1, 0.5, 3));
    public final KeyBindSetting freezeKey = addSetting(new KeyBindSetting("FreezeKey", "LSHIFT"));

    private static final int GRID_WIDTH = 18;
    private static final int GRID_HEIGHT = 18;
    private static final int MARGIN = 2;

    private final List<ShulkerInfo> shulkerList = new ArrayList<>();

    @SuppressWarnings("FieldCanBeLocal")
    private int currentY;
    @SuppressWarnings("FieldCanBeLocal")
    private int startX;
    private int offset;
    private int totalHeight;
    private boolean frozen = false;
    private int frozenX;
    private int frozenY;
    private ItemStack frozenStack = ItemStack.EMPTY;


    public ShulkerViewFeature() {super("ShulkerView", "Shows shulker content preview.", FeatureCategory.of("Render"), "shulkerview");
        bothSides.setShowCondition(() -> mode.get() == Mode.MULTI);
        scrollSensitivity.setShowCondition(() -> mode.get() == Mode.MULTI);
        freezeKey.setShowCondition(() -> mode.get() == Mode.SINGLE);
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTick(PreTickEvent event) {
        shulkerList.clear();

        if (mode.get() != Mode.MULTI) return;
        if (!(MC.screen instanceof AbstractContainerScreen<?> screen)) return;

        for (Slot slot : screen.getMenu().slots) {
            ShulkerInfo info = ShulkerInfo.create(slot.getItem(), slot.index, compact.get());
            if (info != null) {
                shulkerList.add(info);
            }
        }
    }

    @SubscribeEvent
    public void onRenderTooltipEvent(RenderTooltipEvent event) {
        if (!(MC.screen instanceof AbstractContainerScreen<?>)) return;

        if (mode.get() == Mode.SINGLE) {
            renderSingle(event);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderScreenEvent(RenderScreenEvent event) {
        if (!(MC.screen instanceof AbstractContainerScreen<?>)) return;

        if (mode.get() == Mode.MULTI) {
            renderMulti(event);
        }
    }

    private void renderSingle(RenderTooltipEvent event) {
        ItemStack hovered = event.hoveredStack();
        boolean freezePressed = KEYBIND_SERVICE.isPressed(freezeKey);

        if ((hovered == null || hovered.isEmpty()) && !frozen) return;
        if (!freezePressed) {
            frozen = false;
            frozenStack = ItemStack.EMPTY;
        }
        if (freezePressed && !frozen) {
            ShulkerInfo test = ShulkerInfo.create(hovered, -1, compact.get());
            if (test != null) {
                frozen = true;
                frozenX = event.mouseX();
                frozenY = event.mouseY();
                frozenStack = hovered;
            }
        }

        ItemStack stack = frozen ? frozenStack : hovered;
        if (stack == null || stack.isEmpty()) return;

        ShulkerInfo info = ShulkerInfo.create(stack, -1, compact.get());
        if (info == null) return;

        event.cancel();

        GuiGraphics ctx = event.graphics();
        float scale = this.scale.get().floatValue();
        int cols = info.cols();
        int rows = info.rows();
        int width  = cols * GRID_WIDTH + MARGIN * (cols-1);
        int height = rows * GRID_HEIGHT + MARGIN * (rows-1);
        int baseX = frozen ? frozenX : event.mouseX();
        int baseY = frozen ? frozenY : event.mouseY();
        int x = (int) (baseX / scale);
        int y = (int) (baseY / scale);
        double mouseX = event.mouseX() / scale;
        double mouseY = event.mouseY() / scale;

        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);

        ctx.fill(x, y, x + width, y + height, new Color(0, 0, 0, 90).getRGB());

        if (borders.get()) {
            drawBorder(ctx, x, y, width, height, getShulkerColor(stack));
        }

        ItemStack tooltipStack = ItemStack.EMPTY;
        int index = 0;

        for (ItemStack content : info.stacks()) {
            int ix = x + (index % cols) * GRID_WIDTH + MARGIN;
            int iy = y + (index / cols) * GRID_HEIGHT + MARGIN;

            ctx.renderItem(content, ix, iy);
            ctx.renderItemDecorations(MC.font, content, ix, iy, null);

            if (!content.isEmpty()
                    && mouseX >= ix && mouseX <= ix + 16
                    && mouseY >= iy && mouseY <= iy + 16) {
                tooltipStack = content;
            }

            index++;
        }
        if (tooltip.get() && !tooltipStack.isEmpty()) {
            ctx.setTooltipForNextFrame(MC.font, tooltipStack, event.mouseX(), event.mouseY());
        }
        ctx.pose().popMatrix();
    }

    private void renderMulti(RenderScreenEvent event) {
        GuiGraphics ctx = event.getDrawContext();
        float scale = this.scale.get().floatValue();

        boolean right = false;
        int edgePadding = 6;

        currentY = bothSides.get() ? edgePadding : edgePadding + offset;
        startX = edgePadding;

        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);

        for (ShulkerInfo info : shulkerList) {
            int width  = info.cols() * GRID_WIDTH + MARGIN * (info.cols()-1);
            int height = info.rows() * GRID_HEIGHT + MARGIN * (info.rows()-1);


            if (currentY + height > MC.getWindow().getGuiScaledHeight() / scale && bothSides.get() && !right) {
                right = true;
                currentY = edgePadding + offset;
            }

            if (right) {
                startX = (int) ((MC.getWindow().getGuiScaledWidth() - width - edgePadding) / scale);
            }

            ctx.fill(startX, currentY, startX + width, currentY + height, new Color(0, 0, 0, 75).getRGB());

            if (borders.get()) {
                drawBorder(ctx, startX, currentY, width, height, getShulkerColor(info.shulker()));
            }

            int index = 0;
            for (ItemStack stack : info.stacks()) {
                if (compact.get() && stack.isEmpty()) break;

                int x = startX + (index % info.cols()) * GRID_WIDTH + MARGIN;
                int y = currentY + (index / info.cols()) * GRID_HEIGHT + MARGIN;

                ctx.renderItem(stack, x, y);
                ctx.renderItemDecorations(MC.font, stack, x, y, null);

                if (tooltip.get() && !stack.isEmpty()
                        && isHovered(event.getMouseX(), event.getMouseY(), x, y, 16, 16, scale)) {
                    ctx.setTooltipForNextFrame(MC.font, stack,event.getMouseX(), event.getMouseY());
                }
                index++;
            }
            currentY += height + MARGIN;
        }

        ctx.pose().popMatrix();
        totalHeight = currentY - offset;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onScroll(MouseScrollEvent event) {
        if (mode.get() != Mode.MULTI) return;

        float maxOffset = Math.min(-totalHeight + MC.getWindow().getGuiScaledHeight() / scale.get().floatValue(), 0);
        offset = (int) Mth.clamp(offset + Math.ceil(event.amount()) * (scrollSensitivity.get() * 10), maxOffset, 0);
    }

    private void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int getShulkerColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) {
            return ARGB.color(255, 128, 128, 128);
        }

        if (!(bi.getBlock() instanceof ShulkerBoxBlock shulker)) {
            return ARGB.color(255, 128, 128, 128);
        }

        DyeColor color = shulker.getColor();
        return color == null
                ? ARGB.color(255, 128, 0, 128)
                : DyeColorToARGB(color);
    }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h, float scale) {
        mx /= scale;
        my /= scale;
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
