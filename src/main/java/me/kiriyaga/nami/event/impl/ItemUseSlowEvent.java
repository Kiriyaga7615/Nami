package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import 	net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemUseSlowEvent extends Event {
    private final Player player;
    private final ItemStack item;

    public ItemUseSlowEvent(Player player, ItemStack item) {
        this.player = player;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }
}
