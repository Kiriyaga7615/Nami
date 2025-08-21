package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.core.rotation.*;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.Debug;
import me.kiriyaga.nami.setting.impl.BoolSetting;
import me.kiriyaga.nami.setting.impl.DoubleSetting;
import me.kiriyaga.nami.setting.impl.IntSetting;
import me.kiriyaga.nami.util.EnchantmentUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.EntityHitResult;

import java.awt.*;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AuraModule extends Module {

    public final DoubleSetting rotateRange = addSetting(new DoubleSetting("rotate", 3.00, 1.0, 6.0));
    public final DoubleSetting attackRange = addSetting(new DoubleSetting("attack", 3.00, 1.0, 6.0));
    public final BoolSetting swordOnly = addSetting(new BoolSetting("weap only", false));
    public final BoolSetting render = addSetting(new BoolSetting("render", true));
    public final BoolSetting tpsSync = addSetting(new BoolSetting("tps sync", false));
    public final BoolSetting multiTask = addSetting(new BoolSetting("multitask", false));
    public final BoolSetting raycast = addSetting(new BoolSetting("raycast", true));
    private final IntSetting rotationPriority = addSetting(new IntSetting("rotation", 5, 1, 10));
    public final DoubleSetting preRotate = addSetting(new DoubleSetting("pre rotate", 0.1, 0.0, 1.0));

    private Entity currentTarget = null;

    public AuraModule() {
        super("aura", "Attacks certain targets automatically.", ModuleCategory.of("combat"), "killaura", "ara", "killara");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        //ROTATION_MANAGER.cancelRequest(AuraModule.class.getName()); //no
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;
        if (!multiTask.get() && MC.player.isUsingItem()) return;

        long startTime = System.nanoTime();

        ItemStack stack = MC.player.getMainHandStack();

        Entity target = ENTITY_MANAGER.getTarget();

        Debug debugModule = MODULE_MANAGER.getStorage().getByClass(Debug.class);

        if (target == null || (swordOnly.get() && !(stack.getItem() instanceof AxeItem || stack.isIn(ItemTags.SWORDS) || stack.getItem() instanceof TridentItem || stack.getItem() instanceof MaceItem))) {
            currentTarget = null;
            this.setDisplayInfo("");
            return;
        }

        currentTarget = target;

        this.setDisplayInfo(target.getName().getString());

        long auraLogicStart = System.nanoTime();

        float cooldown = MC.player.getAttackCooldownProgress(0f);
        float tps = 20f;
        if (tpsSync.get() && MC.getServer() != null) {
            double tickTimeMs = MC.getServer().getAverageTickTime() / 1_000_000.0;
            tps = (float) Math.min(20.0, 1000.0 / tickTimeMs);
        }

        boolean skipCooldown = false;

        if (target instanceof ShulkerBulletEntity) {
            skipCooldown = true;
        } else {
            ItemStack held = MC.player.getMainHandStack();
            float attackDamage = 1.0f; // without charge the damage always 1 i guess
//
//            if (held.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
//                AttributeModifiersComponent modifiers = held.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
//                for (var entry : modifiers.comp_2393()) {
//                    if (entry.comp_2395().matches(EntityAttributes.ATTACK_DAMAGE)) {
//                        attackDamage += (float) entry.comp_2396().value();
//                    }
//                }
//            }

            // idk that shit doesnt apply, i thought it should
//            int sharpnessLevel = EnchantmentUtils.getEnchantmentLevel(held, Enchantments.SHARPNESS);
//            if (sharpnessLevel > 0) {
//                attackDamage += 0.5f * sharpnessLevel + 0.5f;
//            }

            if (MC.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                var strength = MC.player.getStatusEffect(StatusEffects.STRENGTH);
                attackDamage += 3.0f * (strength.getAmplifier() + 1);
            }

            if (MC.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
                var weakness = MC.player.getStatusEffect(StatusEffects.WEAKNESS);
                attackDamage -= 4.0f * (weakness.getAmplifier() + 1); // im not sure is it 4 or 3 btw
            }

            if (target instanceof LivingEntity living) {
                if (living.getMaxHealth() <= attackDamage) {
                    skipCooldown = true;
                }
            }
        }

        float ticksUntilReady = skipCooldown ? (1.0f - cooldown) * tps / 6.0f : (1.0f - cooldown) * tps;

        double eyeDist = getClosestEyeDistance(MC.player.getEyePos(), target.getBoundingBox());

        if (eyeDist <= rotateRange.get() && ticksUntilReady <= preRotate.get() * tps) {
            Vec3d rotationTarget;
            if (raycast.get()) {
                Vec3d eyePos = MC.player.getCameraPosVec(1.0f);
                rotationTarget = getClosestPointToEye(eyePos, target.getBoundingBox());
            } else {
                rotationTarget = getEntityCenter(target);
            }

            ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                    AuraModule.class.getName(),
                    rotationPriority.get(),
                    (float) getYawToVec(MC.player, rotationTarget),
                    (float) getPitchToVec(MC.player, rotationTarget)
            ));
        }

        if (!ROTATION_MANAGER.getRequestHandler().isCompleted(AuraModule.class.getName()) && !raycast.get()) return;

        boolean canAttack;
        if (raycast.get()) {
            EntityHitResult attackHit = raycastTarget(MC.player, target, attackRange.get());
            canAttack = attackHit != null && attackHit.getEntity() == target;
        } else {
            double dist = MC.player.getPos().distanceTo(getEntityCenter(target));
            canAttack = dist <= attackRange.get();
        }

        if (!canAttack) return;

        if (!skipCooldown) {
            if (cooldown < (tpsSync.get() ? 1.0f * (20f / tps) : 1.0f)) return;
        }

        MC.interactionManager.attackEntity(MC.player, target);
        MC.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        long auraLogicDuration = System.nanoTime() - auraLogicStart;
        debugModule.debugAura(Text.of(String.format("logic time: %.3f ms", auraLogicDuration / 1_000_000.0)));

        long totalDuration = System.nanoTime() - startTime;
        debugModule.debugAura(Text.of(String.format("total %.3f ms", totalDuration / 1_000_000.0)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null) return;

        double eyeDist = getClosestEyeDistance(MC.player.getEyePos(), currentTarget.getBoundingBox());

        if (eyeDist > rotateRange.get()) return;

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        drawBox(currentTarget, colorModule.getStyledGlobalColor(), event.getMatrices(), event.getTickDelta());
    }

    private void drawBox(Entity entity, Color color, MatrixStack matrices, float partialTicks) {
        double interpX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double interpY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double interpZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        Box box = entity.getBoundingBox().offset(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());
        RenderUtil.drawBoxFilled(matrices, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 75));
    }

    private static int getYawToVec(Entity from, Vec3d to) {
        double dx = to.x - from.getX();
        double dz = to.z - from.getZ();
        return wrapDegrees((int) Math.round(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
    }

    private static int getPitchToVec(Entity from, Vec3d to) {
        Vec3d eyePos = from.getEyePos();
        double dx = to.x - eyePos.x;
        double dy = to.y - eyePos.y;
        double dz = to.z - eyePos.z;
        return (int) Math.round(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static int wrapDegrees(int angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private static Vec3d getEntityCenter(Entity entity) {
        Box box = entity.getBoundingBox();
        double centerX = box.minX + (box.getLengthX() / 2);
        double centerY = box.minY + (box.getLengthY() / 2);
        double centerZ = box.minZ + (box.getLengthZ() / 2);
        return new Vec3d(centerX, centerY, centerZ);
    }

    private static double getClosestEyeDistance(Vec3d eyePos, Box box) {
        Vec3d closest = getClosestPointToEye(eyePos, box);
        return eyePos.distanceTo(closest);
    }

    private static Vec3d getClosestPointToEye(Vec3d eyePos, Box box) {
        double x = eyePos.x;
        double y = eyePos.y;
        double z = eyePos.z;

        if (eyePos.x < box.minX) x = box.minX;
        else if (eyePos.x > box.maxX) x = box.maxX;

        if (eyePos.y < box.minY) y = box.minY;
        else if (eyePos.y > box.maxY) y = box.maxY;

        if (eyePos.z < box.minZ) z = box.minZ;
        else if (eyePos.z > box.maxZ) z = box.maxZ;

        return new Vec3d(x, y, z);
    }


    private static EntityHitResult raycastTarget(Entity player, Entity target, double reach) {
        float yaw = ROTATION_MANAGER.getStateHandler().getRotationYaw();
        float pitch = ROTATION_MANAGER.getStateHandler().getRotationPitch();

        Vec3d eyePos = player.getCameraPosVec(1.0f);

        Vec3d look = getLookVectorFromYawPitch(yaw, pitch);

        Vec3d reachEnd = eyePos.add(look.multiply(reach));

        Box targetBox = target.getBoundingBox();

        if (targetBox.raycast(eyePos, reachEnd).isPresent()) {
            return new EntityHitResult(target);
        }

        return null;
    }

    private static Vec3d getLookVectorFromYawPitch(float yaw, float pitch) {
        float fYaw = (float) Math.toRadians(yaw);
        float fPitch = (float) Math.toRadians(pitch);

        double x = -Math.cos(fPitch) * Math.sin(fYaw);
        double y = -Math.sin(fPitch);
        double z = Math.cos(fPitch) * Math.cos(fYaw);

        return new Vec3d(x, y, z).normalize();
    }
}
