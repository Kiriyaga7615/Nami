package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.world.entity.Entity;

public class EntityPushEvent extends Event {

    private final Entity target;
    private final Entity source;

    public EntityPushEvent(Entity target, Entity source)
    {
        this.target = target;
        this.source = source;
    }

    public Entity getTarget()
    {
        return target;
    }

    public Entity getSource()
    {
        return source;
    }
}
