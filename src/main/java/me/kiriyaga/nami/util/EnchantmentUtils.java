package me.kiriyaga.nami.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;

public class EnchantmentUtils {

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        var components = stack.getComponents();

        if (!components.contains(DataComponents.ENCHANTMENTS)) {
            return 0;
        }

        var enchantments = components.get(DataComponents.ENCHANTMENTS);

        for (Object2IntMap.Entry<ResourceKey<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            var keyOptional = entry.getKey().getKey();

            if (keyOptional.isPresent() && keyOptional.get().equals(enchantmentKey)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }
}
