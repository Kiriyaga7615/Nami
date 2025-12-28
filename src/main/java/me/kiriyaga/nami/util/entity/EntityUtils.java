package me.kiriyaga.nami.util.entity;

import me.kiriyaga.nami.mixin.AnimalEntityAccessor;
import me.kiriyaga.nami.mixin.PassiveEntityAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static me.kiriyaga.nami.Nami.*;

public class EntityUtils {

    public enum EntityTypeCategory {
        ALL,
        PLAYERS,
        OTHER_PLAYERS,
        HOSTILE,
        NEUTRAL,
        PASSIVE,
        PROJECTILES,
        DROPPED_ITEMS,
        END_CRYSTALS
    }

    public static List<Entity> getEntities(EntityTypeCategory category) {
        return getEntities(category, Double.MAX_VALUE, false, null);
    }

    public static List<Entity> getEntities(EntityTypeCategory category, double range) {
        return getEntities(category, range, false, null);
    }

    public static List<Entity> getEntities(EntityTypeCategory category, double range, boolean sortByDistance) {
        return getEntities(category, range, sortByDistance, null);
    }

    public static List<Entity> getEntities(EntityTypeCategory category, double range, boolean sortByDistance, Predicate<Entity> extraFilter) {
        if (MC.player == null || MC.world == null) return List.of();

        Vec3 playerPos = MC.player.getEntityPos();
        List<Entity> all = getEntitiesBase(category);

        return all.stream()
                .filter(e -> e != MC.player)
                .filter(e -> e.squaredDistanceTo(playerPos) <= range * range)
                .filter(e -> extraFilter == null || extraFilter.test(e))
                .sorted(sortByDistance ? Comparator.comparingDouble(e -> e.squaredDistanceTo(playerPos)) : (a, b) -> 0)
                .collect(Collectors.toList());
    }

    private static List<Entity> getEntitiesBase(EntityTypeCategory category) {
        return switch (category) {
            case ALL -> getAllEntities();
            case PLAYERS -> getPlayers().stream().map(p -> (Entity) p).toList();
            case OTHER_PLAYERS -> getOtherPlayers().stream().map(p -> (Entity) p).toList();
            case HOSTILE -> getAllEntities().stream().filter(HostileUtils::isHostile).toList();
            case NEUTRAL -> getAllEntities().stream().filter(HostileUtils::isNeutral).toList();
            case PASSIVE -> getAllEntities().stream().filter(HostileUtils::isPassive).toList();
            case PROJECTILES -> getAllEntities().stream().filter(HostileUtils::isProjectile).toList();
            case DROPPED_ITEMS -> getAllEntities().stream().filter(e -> e instanceof ItemEntity).toList();
            case END_CRYSTALS -> getAllEntities().stream().filter(e -> e instanceof EndCrystal).toList();
        };
    }

    public static List<Entity> getAllEntities() {
        ClientLevel world = MC.world;
        return world != null
                ? StreamSupport.stream(world.getEntities().spliterator(), false).collect(Collectors.toList())
                : List.of();
    }

    public static List<Player> getPlayers() {
        ClientLevel world = MC.world;
        if (world == null) return List.of();

        return StreamSupport.stream(world.getEntities().spliterator(), false)
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());
    }

    public static List<Player> getOtherPlayers() {
        LocalPlayer self = MC.player;
        return getPlayers().stream()
                .filter(p -> !p.isRemoved() && p != self)
                .collect(Collectors.toList());
    }

    public static boolean canBreed(Animal animal) {
        PassiveEntityAccessor a = (PassiveEntityAccessor) animal;
        AnimalEntityAccessor a1 = (AnimalEntityAccessor) animal;

        if (animal.isBaby()) return false;
        if (a.breedingAge() != 0) return false;
        if (a1.loveTicks() > 0) return false;

        return animal.canEat();
    }
}
