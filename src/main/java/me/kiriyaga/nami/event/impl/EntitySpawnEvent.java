package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.world.entity.Entity;

public class EntitySpawnEvent extends Event {
    private final Entity e;


    public EntitySpawnEvent(Entity e) {
        this.e = e;
    }

    public Entity getEntity() {
        return e;
    }
}
