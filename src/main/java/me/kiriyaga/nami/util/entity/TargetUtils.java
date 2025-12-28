package me.kiriyaga.nami.util.entity;

import me.kiriyaga.nami.feature.module.impl.client.TargetModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;

public class TargetUtils {
    public static Entity getTarget() {
        TargetModule targetModule = MODULE_MANAGER.getStorage().getByClass(TargetModule.class);
        if (MC.player == null || MC.world == null || targetModule == null)
            return null;

        List<Entity> candidates = EntityUtils.getAllEntities().stream()
                .filter(e -> e != MC.player)
                .filter(e -> {
                    if (e instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) e;
                        if (!le.isAlive()) return false;
                        if (e.age < targetModule.minTicksExisted.get().intValue()) return false;
                        double distSq = e.squaredDistanceTo(MC.player);
                        if (distSq > targetModule.targetRange.get() * targetModule.targetRange.get()) return false;

                        return (targetModule.targetPlayers.get() && e instanceof Player && !FRIEND_MANAGER.isFriend(e.getName().getString()))
                                || (targetModule.targetHostiles.get() && HostileUtils.isHostile(e))
                                || (targetModule.targetNeutrals.get() && HostileUtils.isNeutral(e))
                                || (targetModule.targetPassives.get() && HostileUtils.isPassive(e));
                    }

                    if (targetModule.targetPrijectiles.get()) {
                        return (e instanceof ShulkerBullet) || (e instanceof LargeFireball);
                    }

                    return false;
                })
                .collect(Collectors.toList());

        switch (targetModule.priority.get()) {
            case HEALTH:
                return candidates.stream()
                        .filter(e -> e instanceof LivingEntity)
                        .min(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()))
                        .orElse(null);

            case DISTANCE:
                return candidates.stream()
                        .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player)))
                        .orElse(null);

            case SMART:
                List<Entity> players = candidates.stream()
                        .filter(e -> e instanceof Player && !FRIEND_MANAGER.isFriend(e.getName().getString()))
                        .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player)))
                        .toList();

                if (!players.isEmpty()) return players.get(0);

                List<Entity> creepers = candidates.stream()
                        .filter(e -> e instanceof Creeper)
                        .filter(e -> e.squaredDistanceTo(MC.player) <= 3 * 3) // yeah its not accurate at all, but its not required here i guess?
                        .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player)))
                        .toList();

                if (!creepers.isEmpty()) return creepers.get(0);

                List<Entity> projectiles = candidates.stream()
                        .filter(e -> e instanceof ShulkerBullet || e instanceof LargeFireball)
                        .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player)))
                        .toList();

                if (!projectiles.isEmpty()) return projectiles.get(0);

                List<Entity> others = candidates.stream()
                        .filter(e -> !(e instanceof Player)
                                && !(e instanceof ShulkerBullet)
                                && !(e instanceof LargeFireball))
                        .toList();

                return others.stream()
                        .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player)))
                        .orElse(null);
        }

        return null;
    }
}
