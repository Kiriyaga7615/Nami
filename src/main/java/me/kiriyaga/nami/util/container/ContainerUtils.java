package me.kiriyaga.nami.util.container;

import me.kiriyaga.nami.feature.module.impl.client.DebugModule;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ARGB;

import java.util.*;

import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

public class ContainerUtils {

    public static boolean hasItems(ItemStack stack) {
        DataComponentMap components = stack.getComponents();
        return components.contains(DataComponents.CONTAINER);
    }

    public static boolean openContainer(ItemStack stack) {
        if (!hasItems(stack)) {
            MODULE_MANAGER.getStorage().getByClass(DebugModule.class).debugPeek(Component.of("peek not a container " + stack));
            return false;
        }

        ItemStack[] contents = new ItemStack[27];
        getItemsInContainerItem(stack, contents);

        ContainerScreen.open(stack, contents);

        MODULE_MANAGER.getStorage().getByClass(DebugModule.class).debugPeek(Component.of("peek opened container preview for " + stack));
        return true;
    }

    public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
        Arrays.fill(items, ItemStack.EMPTY);

        DataComponentMap components = itemStack.getComponents();

        if (components.contains(DataComponents.CONTAINER)) {
            Object comp = components.get(DataComponents.CONTAINER);

            if (comp instanceof ItemContainerContents container) {
                NonNullList<ItemStack> stacks = NonNullList.ofSize(items.length, ItemStack.EMPTY);
                container.copyTo(stacks);

                for (int i = 0; i < stacks.size() && i < items.length; i++) {
                    items[i] = stacks.get(i);
                }

                MODULE_MANAGER.getStorage()
                        .getByClass(DebugModule.class)
                        .debugPeek(Component.of("peek got " + container.streamNonEmpty().count() + " items from container " + itemStack));
            }
        }
    }

    public static int DyeColorToARGB(DyeColor color) {
        switch (color) {
            case WHITE: return ARGB.color(255, 255, 255, 255);
            case ORANGE: return ARGB.color(255, 216, 127, 51);
            case MAGENTA: return ARGB.color(255, 178, 76, 216);
            case LIGHT_BLUE: return ARGB.color(255, 102, 153, 216);
            case YELLOW: return ARGB.color(255, 229, 229, 51);
            case LIME: return ARGB.color(255, 127, 204, 25);
            case PINK: return ARGB.color(255, 242, 127, 165);
            case GRAY: return ARGB.color(255, 76, 76, 76);
            case LIGHT_GRAY: return ARGB.color(255, 153, 153, 153);
            case CYAN: return ARGB.color(255, 76, 127, 153);
            case PURPLE: return ARGB.color(255, 127, 63, 178);
            case BLUE: return ARGB.color(255, 51, 76, 178);
            case BROWN: return ARGB.color(255, 102, 76, 51);
            case GREEN: return ARGB.color(255, 102, 127, 51);
            case RED: return ARGB.color(255, 153, 51, 51);
            case BLACK: return ARGB.color(255, 25, 25, 25);
            default: return ARGB.color(255, 128, 128, 128);
        }
    }
}
