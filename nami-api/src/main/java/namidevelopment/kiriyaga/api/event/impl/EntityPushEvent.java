package namidevelopment.kiriyaga.api.event.impl;

import namidevelopment.kiriyaga.api.event.Event;
import net.minecraft.world.entity.Entity;

public class EntityPushEvent extends Event {

    private final Entity target;
    private final Entity source;

    public EntityPushEvent(Entity target, Entity source) {
        this.target = target;
        this.source = source;
    }

    public Entity getTarget() {
        return target;
    }

    public Entity getSource() {
        return source;
    }
}
