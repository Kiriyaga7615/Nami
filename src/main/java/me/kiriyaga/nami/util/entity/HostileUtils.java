package me.kiriyaga.nami.util.entity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;

import static me.kiriyaga.nami.Nami.MC;

public class HostileUtils {
    public static boolean isProjectile(Entity e) {
        return e instanceof ShulkerBulletEntity
                || e instanceof FireballEntity
                || e instanceof WitherSkullEntity
                || e instanceof ArrowEntity
                || e instanceof TridentEntity
                || e instanceof ThrowableProjectile;
    }

    public static boolean isHostile(Entity e) {
        if (e instanceof CreeperEntity
                || e instanceof SkeletonEntity
                || e instanceof StrayEntity
                || e instanceof WitherSkeletonEntity
                || e instanceof ZombieEntity && !(e instanceof ZombifiedPiglinEntity) // yeah zombifiied piglin is inherited from zombie
                || e instanceof HuskEntity
                || e instanceof DrownedEntity
                || e instanceof VindicatorEntity
                || e instanceof BoggedEntity
                || e instanceof EvokerEntity
                || e instanceof PillagerEntity
                || e instanceof RavagerEntity
                || e instanceof VexEntity
                || e instanceof BreezeEntity
                || e instanceof BlazeEntity
                || e instanceof WitherBoss
                || e instanceof EnderDragon
                || e instanceof ShulkerEntity
                || e instanceof GuardianEntity
                || e instanceof ElderGuardianEntity
                || e instanceof GhastEntity
                || e instanceof HoglinEntity
                || e instanceof ZombieVillagerEntity
                || e instanceof MagmaCubeEntity
                || e instanceof SilverfishEntity
                || e instanceof SlimeEntity
                || e instanceof PhantomEntity
                || e instanceof IllusionerEntity
                || e instanceof WitchEntity) {
            return true;
        }

        return isNeutralEntityType(e) && isAggressiveNow(e);
    }

    public static boolean isNeutral(Entity e) {
        return isNeutralEntityType(e) && !isAggressiveNow(e);
    }

    public static boolean isPassive(Entity e) {
        return e instanceof PassiveEntity ||
                (e instanceof IronGolemEntity golem && golem.isPlayerCreated());
    }

    private static boolean isNeutralEntityType(Entity e) {
        return e instanceof EndermanEntity ||
                e instanceof PiglinEntity ||
                e instanceof ZombifiedPiglinEntity ||
                e instanceof SpiderEntity ||
                e instanceof CaveSpiderEntity ||
                e instanceof PolarBearEntity ||
                (e instanceof WolfEntity w && !w.isTamed()) ||
                e instanceof BeeEntity ||
                e instanceof GoatEntity ||
                (e instanceof IronGolemEntity g && !g.isPlayerCreated());
    }

    public static boolean isAggressiveNow(Entity e) {
        LocalPlayer player = MC.player;
        if (player == null || MC.world == null) return false;

        long timeOfDay = MC.world.getTimeOfDay() % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;

        if (e instanceof EndermanEntity enderman) return enderman.isAngry();
        if (e instanceof ZombifiedPiglinEntity piglin) return piglin.isAttacking();
        if (e instanceof PiglinEntity piglin) return !PlayerUtils.isPlayerWearingGold(player) || piglin.isAttacking();
        if (e instanceof SpiderEntity spider) return spider.isAttacking() || isNight;
        if (e instanceof CaveSpiderEntity) return true;
        if (e instanceof PolarBearEntity bear) return bear.isAttacking();
        if (e instanceof WolfEntity wolf) return wolf.isAttacking();
        if (e instanceof BeeEntity bee) return bee.hasAngerTime();

        return false;
    }
}
