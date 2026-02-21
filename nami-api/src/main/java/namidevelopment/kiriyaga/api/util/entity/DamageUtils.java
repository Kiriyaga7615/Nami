package namidevelopment.kiriyaga.api.util.entity;

import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import java.util.Collection;

import java.util.function.BiFunction;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.getClosestPointToEye;

public class DamageUtils {

    public static final BlockRaycastProvider BLOCK_CHECK = (ctx, pos) -> {
        BlockState state = MC.level.getBlockState(pos);
        if (!state.isSolid()) return null;
        return state.getCollisionShape(MC.level, pos).clip(ctx.start(), ctx.end(), pos);
    };

    public static float crystalDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor, Collection<BlockPos> ignoredBlocks) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 12f, wrapProviderWithIgnore(raycastProvider, ignoredBlocks), assumeBestArmor);
    }

    public static float bedDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor, Collection<BlockPos> ignoredBlocks) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, wrapProviderWithIgnore(raycastProvider, ignoredBlocks), assumeBestArmor);
    }

    public static float anchorDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor, Collection<BlockPos> ignoredBlocks) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, wrapProviderWithIgnore(raycastProvider, ignoredBlocks), assumeBestArmor);
    }

    public static float crystalDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 12f, raycastProvider, assumeBestArmor);
    }

    public static float bedDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastProvider, assumeBestArmor);
    }

    public static float anchorDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider, boolean assumeBestArmor) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastProvider, assumeBestArmor);
    }

    private static float computeExplosionDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float strength, BlockRaycastProvider raycastProvider, boolean assumeBestArmor) {
        double distance = targetPos.distanceTo(explosionPos);

        if (distance > strength) return 0f;

        double exposure = calculateExposure(explosionPos, targetBox, raycastProvider);
        double impact = (1 - (distance / strength)) * exposure;
        float baseDamage = (float) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return applyReductions(baseDamage, target, MC.level.damageSources().explosion(null), assumeBestArmor);
    }

    private static BlockRaycastProvider wrapProviderWithIgnore(BlockRaycastProvider provider, Collection<BlockPos> ignoredBlocks) {
        if (ignoredBlocks == null || ignoredBlocks.isEmpty()) return provider;

        return (ctx, pos) -> {
            if (ignoredBlocks.contains(pos)) return null;
            return provider.apply(ctx, pos);
        };
    }

    public static float applyReductions(float damage, Entity entity, DamageSource source, boolean assumeBestArmor) {
        if (source.scalesWithDifficulty()) {
            switch (MC.level.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2 + 1, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        if (!(entity instanceof LivingEntity living)) return Math.max(damage, 0);

        damage = CombatRules.getDamageAfterAbsorb(living, damage, source, (float) Math.floor(living.getAttributeValue(Attributes.ARMOR)),
                (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        damage = reduceByResistance(living, damage);
        damage = reduceByProtection(living, damage, source, assumeBestArmor);

        return Math.max(damage, 0);
    }

    private static float reduceByProtection(LivingEntity entity, float damage, DamageSource source, boolean assumeBestArmor) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return damage;

        int totalProtection = 0;

        if (assumeBestArmor) {
            for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (stack.isEmpty()) continue;

                if (slot != EquipmentSlot.LEGS)
                    totalProtection += 4;

                if (slot == EquipmentSlot.LEGS && source.is(DamageTypeTags.IS_EXPLOSION))
                    totalProtection += 8;
            }
            return CombatRules.getDamageAfterMagicAbsorb(damage, totalProtection);
        }

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            int prot = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROTECTION);
            if (prot > 0) totalProtection += prot;

            if (source.is(DamageTypeTags.IS_FIRE))
                totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION);

            if (source.is(DamageTypeTags.IS_EXPLOSION))
                totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION);

            if (source.is(DamageTypeTags.IS_PROJECTILE))
                totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROJECTILE_PROTECTION);

            if (source.is(DamageTypeTags.IS_FALL))
                totalProtection += 3 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FEATHER_FALLING);
        }

        return CombatRules.getDamageAfterMagicAbsorb(damage, totalProtection);
    }


    private static float reduceByResistance(LivingEntity entity, float damage) {
        MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) damage *= 1 - 0.2f * (resistance.getAmplifier() + 1);
        return Math.max(damage, 0);
    }

    private static float calculateExposure(Vec3 source, AABB box, BlockRaycastProvider provider) {
        double dx = box.getXsize();
        double dy = box.getYsize();
        double dz = box.getZsize();

        int steps = 2;
        int hits = 0, misses = 0;

        ExposureContext ctx = new ExposureContext(Vec3.ZERO, Vec3.ZERO);

        for (double x = 0; x <= dx; x += dx / steps) {
            for (double y = 0; y <= dy; y += dy / steps) {
                for (double z = 0; z <= dz; z += dz / steps) {

                    Vec3 pos = new Vec3(box.minX + x, box.minY + y, box.minZ + z);

                    ctx.set(pos, source);

                    if (raycast(ctx, provider) == null)
                        misses++;

                    hits++;
                }
            }
        }

        return hits == 0 ? 0f : (float) misses / hits;
    }

    private static BlockHitResult raycast(ExposureContext context, BlockRaycastProvider provider) {
        return BlockGetter.traverseBlocks(context.start, context.end, context, provider, ctx -> null);
    }

    public static final class ExposureContext {
        private Vec3 start;
        private Vec3 end;

        public ExposureContext(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }

        public void set(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }

        public Vec3 start() {
            return start;
        }

        public Vec3 end() {
            return end;
        }
    }

    @FunctionalInterface
    public interface BlockRaycastProvider extends BiFunction<ExposureContext, net.minecraft.core.BlockPos, BlockHitResult> {}
}
