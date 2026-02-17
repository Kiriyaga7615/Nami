package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.RenderSlotsEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.container.ShulkerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.Item;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class BetterInventoryFeature extends Feature {

    private final WhitelistSetting highlightSlots = addSetting(new WhitelistSetting("HighlightSlots", true, WhitelistSetting.Type.ITEM));
    public final BoolSetting shulkerFillBar = addSetting(new BoolSetting("ShulkerFill", true));
    public final BoolSetting dominantItem = addSetting(new BoolSetting("DominantItem", false));

    public BetterInventoryFeature() {
        super("BetterInventory", "Quality of life features to improve inventory managment.", FeatureCategory.of("Render"), "betterinventory");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderSlots(RenderSlotsEvent event) {
        if (!(MC.screen instanceof AbstractContainerScreen<?>)) return;

        GuiGraphics ctx = event.graphics();
        List<Slot> slots = event.slots();

        Color primary = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor(90);

        if (highlightSlots.get()) {
            for (Slot slot : slots) {
                if (!slot.hasItem()) continue;


                Identifier id = BuiltInRegistries.ITEM.getKey(slot.getItem().getItem());

                if (highlightSlots.contains(id.toString())) {
                    int x = slot.x;
                    int y = slot.y;
                    ctx.fill(x, y, x + 16, y + 16, primary.getRGB());
                }
            }
        }

        if (shulkerFillBar.get() || dominantItem.get()) {
            for (Slot slot : slots) {
                ItemStack stack = slot.getItem();
                if (!(stack.getItem() instanceof BlockItem bi) || stack.isEmpty() || !(bi.getBlock() instanceof ShulkerBoxBlock)) continue;

                ShulkerInfo info = ShulkerInfo.create(stack, slot.index, false);
                if (info == null) continue;

                if (dominantItem.get())
                    renderDominantItem(ctx, slot.x, slot.y, info);

                if (shulkerFillBar.get())
                    renderShulkerFill(ctx, slot.x, slot.y, info);
            }
        }
    }

    private void renderShulkerFill(GuiGraphics ctx, int slotX, int slotY, ShulkerInfo info) {
        int x = slotX;
        int y = slotY + 15;

        float usedSlots = 0f;
        for (ItemStack s : info.stacks()) {
            if (s.isEmpty()) continue;

            int max = s.getMaxStackSize();
            int count = s.getCount();
            float slotUsage = (float) count / (float) max;
            slotUsage = Math.min(1f, slotUsage);
            usedSlots += slotUsage;
        }

        float percent = usedSlots / 27f;
        percent = Math.min(1f, Math.max(0f, percent));
        int width = 13;
        int height = 2;

        int startX = x + 2;
        ctx.fill(startX, y, startX + width, y - height, 0xFF000000);
        int r = (int) (255 * (1f - percent));
        int g = (int) (255 * percent);
        int color = (0xFF << 24) | (r << 16) | (g << 8);
        int fill = Math.round(width * percent);
        fill = Math.max(0, Math.min(fill, width));
        ctx.fill(startX, y - 1, startX + fill, y - height, color);
    }

    private void renderDominantItem(GuiGraphics ctx, int slotX, int slotY, ShulkerInfo info) {
        Map<Item, Integer> map = new HashMap<>();
        for (ItemStack s : info.stacks()) {
            if (s.isEmpty()) continue;
            map.put(s.getItem(), map.getOrDefault(s.getItem(), 0) + s.getCount());
        }

        if (map.isEmpty()) return;

        Item dominant = null;
        int maxCount = 0;
        for (Map.Entry<Item, Integer> e : map.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                dominant = e.getKey();
            }
        }

        if (dominant == null) return;

        ItemStack dominantStack = new ItemStack(dominant, 1);
        int centerX = slotX + 8;
        int centerY = slotY + 8;

        ctx.pose().pushMatrix();
        ctx.pose().translate(centerX, centerY);
        ctx.pose().scale(0.85f, 0.85f);
        ctx.renderItem(dominantStack, -8, -8);
        ctx.renderItemDecorations(MC.font, dominantStack, -8, -8, null);
        ctx.pose().popMatrix();
    }
}
