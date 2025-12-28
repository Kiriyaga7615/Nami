package me.kiriyaga.nami.util.container;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ShulkerInfo {
    final ItemStack shulker;
    final int slot;
    final List<ItemStack> stacks;
    final int rows;
    final int cols;

    public ShulkerInfo(ItemStack shulker, int slot, List<ItemStack> stacks) {
        this.shulker = shulker;
        this.slot = slot;
        this.stacks = stacks;
        int size = stacks.size();
        this.rows = (int) Math.ceil(size / 9f);
        this.cols = Math.min(9, size);
    }

    public static ShulkerInfo create(ItemStack stack, int slot, boolean compact) {
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        if (!(item.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock)) return null;

        List<ItemStack> items = new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY));
        var component = stack.getComponents().getOrDefault(net.minecraft.component.DataComponentTypes.CONTAINER, null);
        if (component == null) return null;

        List<ItemStack> input = component.stream().toList();
        for (int i = 0; i < input.size(); i++) items.set(i, input.get(i));

        if (compact) items = compactList(items);

        return new ShulkerInfo(stack, slot, items);
    }

    private static List<ItemStack> compactList(List<ItemStack> input) {
        Map<Item, Integer> map = new HashMap<>();
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            map.put(stack.getItem(), map.getOrDefault(stack.getItem(), 0) + stack.getCount());
        }

        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : map.entrySet()) {
            result.add(new ItemStack(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public int slot() {
        return slot;
    }

    public ItemStack shulker() {
        return shulker;
    }

    public List<ItemStack> stacks(){
        return stacks;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }
}