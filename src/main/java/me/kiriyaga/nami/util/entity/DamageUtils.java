package me.kiriyaga.nami.util.entity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.kiriyaga.nami.util.EnchantmentUtils;
import net.minecraft.core.BlockPos;
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

import java.util.function.BiFunction;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.util.RotationUtils.getClosestPointToEye;

public class DamageUtils {

    public static final BlockRaycastProvider BLOCK_CHECK = (ctx, pos) -> {
        BlockState state = MC.world.getBlockState(pos);
        if (state.getBlock().getBlastResistance() < 600) return null;
        return state.getCollisionShape(MC.world, pos).raycast(ctx.start(), ctx.end(), pos);
    };

    public static float crystalDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 12f, raycastProvider);
    }

    public static float bedDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastProvider);
    }

    public static float anchorDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, BlockRaycastProvider raycastProvider) {
        return computeExplosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastProvider);
    }

    private static float computeExplosionDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float strength, BlockRaycastProvider raycastProvider) {
        Vec3 lookDir = getClosestPointToEye(explosionPos, target.getBoundingBox()).subtract(explosionPos).normalize();
        Vec3 rayEnd = explosionPos.add(lookDir.multiply(strength));

        if (target.getBoundingBox().raycast(explosionPos, rayEnd).isEmpty()) return 0f;

        double distance = targetPos.distanceTo(explosionPos);
        double exposure = calculateExposure(explosionPos, targetBox, raycastProvider);
        double impact = (1 - (distance / strength)) * exposure;
        float baseDamage = (float) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return applyReductions(baseDamage, target, MC.world.getDamageSources().explosion(null));
    }

    public static float applyReductions(float damage, Entity entity, DamageSource source) {
        if (source.isScaledWithDifficulty()) {
            switch (MC.world.getDifficulty()) {
                case b -> damage = Math.min(damage / 2 + 1, damage);
                case d -> damage *= 1.5f;
            }
        }

        if (!(entity instanceof LivingEntity living)) return Math.max(damage, 0);

        damage = CombatRules.getDamageAfterAbsorb(living, damage, source, (float) Math.floor(living.getAttributeValue(Attributes.ARMOR)),
                (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        damage = reduceByResistance(living, damage);
        damage = reduceByProtection(living, damage, source);

        return Math.max(damage, 0);
    }

    private static float reduceByProtection(LivingEntity entity, float damage, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return damage;

        int totalProtection = 0;

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack stack = entity.getEquippedStack(slot);
            int prot = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROTECTION);
            if (prot > 0) totalProtection += prot;

            if (source.isIn(DamageTypeTags.IS_FIRE)) totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION);
            if (source.isIn(DamageTypeTags.IS_EXPLOSION)) totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION);
            if (source.isIn(DamageTypeTags.IS_PROJECTILE)) totalProtection += 2 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROJECTILE_PROTECTION);
            if (source.isIn(DamageTypeTags.IS_FALL)) totalProtection += 3 * EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FEATHER_FALLING);
        }

        return CombatRules.getDamageAfterMagicAbsorb(damage, totalProtection);
    }

    private static float reduceByResistance(LivingEntity entity, float damage) {
        MobEffectInstance resistance = entity.getStatusEffect(MobEffects.RESISTANCE);
        if (resistance != null) damage *= 1 - 0.2f * (resistance.getAmplifier() + 1);
        return Math.max(damage, 0);
    }

    private static float calculateExposure(Vec3 source, AABB box, BlockRaycastProvider provider) {
        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();

        int steps = 2;
        int hits = 0, misses = 0;

        for (double x = 0; x <= dx; x += dx / steps) {
            for (double y = 0; y <= dy; y += dy / steps) {
                for (double z = 0; z <= dz; z += dz / steps) {
                    Vec3 pos = new Vec3(box.minX + x, box.minY + y, box.minZ + z);
                    if (raycast(new ExposureContext(pos, source), provider) == null) misses++;
                    hits++;
                }
            }
        }

        return hits == 0 ? 0f : (float) misses / hits;
    }

    private static BlockHitResult raycast(ExposureContext context, BlockRaycastProvider provider) {
        return BlockGetter.traverseBlocks(context.start, context.end, context, provider, ctx -> null);
    }

    public record ExposureContext(Vec3 start, Vec3 end) {}

    @FunctionalInterface
    public interface BlockRaycastProvider extends BiFunction<ExposureContext, BlockPos, BlockHitResult> {}
}
