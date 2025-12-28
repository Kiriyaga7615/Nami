package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class MoveEvent extends Event {
    private MoverType movementType;
    private Vec3 movement;

    public MoveEvent(MoverType movementType, Vec3 movement) {
        this.movementType = movementType;
        this.movement = movement;
    }

    public MoverType getMovementType() {
        return movementType;
    }

    public void setMovementType(MoverType movementType) {
        this.movementType = movementType;
    }

    public Vec3  getMovement() {
        return movement;
    }

    public void setMovement(Vec3 movement) {
        this.movement = movement;
    }
}
