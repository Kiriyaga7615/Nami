package namidevelopment.kiriyaga.nami.impl.feature.combat;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.AddEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.RemoveEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.RotationUtils;
import namidevelopment.kiriyaga.api.util.entity.DamageUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.world.SpeedMineFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Locale;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;

@RegisterFeature
public class AutoCrystalFeature extends Feature {
    public enum Page {PLACE, BREAK, DAMAGES, RENDER}
    public enum Sequential {NONE, FULL }

    public final EnumSetting<Page> page = addSetting(new EnumSetting<>("Page", Page.PLACE));

    //place
    public final BoolSetting doPlace = addSetting(new BoolSetting("Place", true));
    public final DoubleSetting placeRange = addSetting(new DoubleSetting("PlaceRange","Range", 6.0, 1.0, 6.0));
    public final IntSetting placeDelay = addSetting(new IntSetting("PlaceDelay","Delay", 0, 0, 20));
    public final BoolSetting placeRotate = addSetting(new BoolSetting("PlaceRotate","Rotate", true));
    public final BoolSetting placeSwing = addSetting(new BoolSetting("PlaceSwing","Swing", true));
    public final BoolSetting placeIgnoreItems = addSetting(new BoolSetting("PlaceIgnoreItems","IgnoreItems", true));
    public final BoolSetting placeIgnoreCrystals = addSetting(new BoolSetting("PlaceIgnoreCrystals","IgnoreCrystals", true));
    public final BoolSetting placeStrictDirection = addSetting(new BoolSetting("PlaceStrictDirection","StrictDirection", true));
    public final BoolSetting placeSwapBack = addSetting(new BoolSetting("PlaceSwapBack","SwapBack", true));
    public final BoolSetting placeMultitask = addSetting(new BoolSetting("PlaceMultitask","Multitask", false));
    public final BoolSetting placeIgnoreTerrain = addSetting(new BoolSetting("PlaceIgnoreTerrain","IgnoreTerrain", true));
    public final BoolSetting placeAntiFeetTrap = addSetting(new BoolSetting("PlaceAntiFeetTrap", "AntiFeetTrap", true));
    public final DoubleSetting placeAntiFeetTrapFactor = addSetting(new DoubleSetting("PlaceAntiFeetTrapFactor","Factor", 0.80, 0.5, 1.00));

    //break
    public final BoolSetting doBreak = addSetting(new BoolSetting("Break", true));
    public final DoubleSetting breakRange = addSetting(new DoubleSetting("BreakRange","Range", 3.0, 1.0, 7.0));
    public final IntSetting breakInhibit = addSetting(new IntSetting("Inhibit", 4, 1, 20));
    public final IntSetting breakDelay = addSetting(new IntSetting("BreakDelay","Delay", 0, 0, 20));
    public final BoolSetting breakRotate = addSetting(new BoolSetting("BreakRotate","Rotate", true));
    public final BoolSetting breakSwing = addSetting(new BoolSetting("BreakSwing","Swing", true));
    public final BoolSetting breakMultitask = addSetting(new BoolSetting("BreakMultitask","Multitask", false));
    public final IntSetting breakAge = addSetting(new IntSetting("Age", 0, 0, 20));
    public final EnumSetting<Sequential> breakSequential = addSetting(new EnumSetting<>("BreakSequential","Sequential", Sequential.NONE));

    //damages
    public final BoolSetting assumeBestArmor = addSetting(new BoolSetting("AssumeBestArmor", true));
    public final BoolSetting noSelfPop = addSetting(new BoolSetting("NoSelfPop", true));
    public final DoubleSetting minDamage = addSetting(new DoubleSetting("MinDamage", 4.0, 0.0, 36.0));
    public final DoubleSetting maxSelfDamage = addSetting(new DoubleSetting("MaxSelfDamage", 12.0, 0.0, 36.0));
    public final DoubleSetting maxFriendDamage = addSetting(new DoubleSetting("MaxFriendDamage", 12.0, 0.0, 36.0));

    //render
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private int breakTimer, placeTimer = 0; // i love it
    private PlaceTarget placeTarget = null;
    float lastTotalDamage, lastCalcTimeMs = 0;
    private final Int2IntOpenHashMap crystalHits = new Int2IntOpenHashMap();
    private final Long2IntOpenHashMap crystalPlaces = new Long2IntOpenHashMap();

    public AutoCrystalFeature() {
        super("AutoCrystal", "Automatically places and break crystals to kill people, if you are good enough!.", FeatureCategory.of("Combat"), "autocrystal", "ac", "crystalaura");
        doBreak.setShowCondition(() -> page.get() == Page.BREAK);
        breakRange.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakDelay.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakRotate.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakSwing.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakMultitask.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakAge.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakSequential.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakInhibit.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);

        doPlace.setShowCondition(() -> page.get() == Page.PLACE);
        placeRange.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeDelay.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeRotate.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeSwing.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreItems.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeMultitask.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeSwapBack.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreCrystals.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeStrictDirection.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreTerrain.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeAntiFeetTrap.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeAntiFeetTrapFactor.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE && placeAntiFeetTrap.get());

        noSelfPop.setShowCondition(() -> page.get() == Page.DAMAGES);
        minDamage.setShowCondition(() -> page.get() == Page.DAMAGES);
        maxSelfDamage.setShowCondition(() -> page.get() == Page.DAMAGES);
        maxFriendDamage.setShowCondition(() -> page.get() == Page.DAMAGES);
        assumeBestArmor.setShowCondition(() -> page.get() == Page.DAMAGES);

        render.setShowCondition(() -> page.get() == Page.RENDER);
    }

    @Override
    public void onEnable() {
        breakTimer = 0;
        placeTimer = 0;
        lastTotalDamage = 0;
        lastCalcTimeMs = 0;
        crystalPlaces.clear();
        crystalHits.clear();
    }

    @SubscribeEvent
    private void onRemoveEntityEvent(RemoveEntityEvent event) {
        ClientboundRemoveEntitiesPacket packet = event.getPacket();
        for (int id : packet.getEntityIds()) {
            if (crystalHits.containsKey(id)) {
                crystalHits.remove(id);
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onAddEntityEvent(AddEntityEvent event) {
        if (breakSequential.get() != Sequential.FULL) return;

        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            if (packet.getType() != EntityType.END_CRYSTAL) {
                return;
            }

            Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            EndCrystal fake = new EndCrystal(EntityType.END_CRYSTAL, MC.level);
            fake.setPos(pos);
            fake.setId(packet.getId());

            doBreakOnNetty(fake);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.player.isDeadOrDying()) return;
        lastCalcTimeMs = 0;

        updateMemory();

        if (doBreak.get()) {
            if (breakTimer > 0) {
                breakTimer--;
            } else
                doBreak();
        }

        if (doPlace.get()) {
            if (placeTimer > 0) {
                placeTimer--;
            } else {
                lastTotalDamage = 0;
                doPlace();
            }
        }

        this.clearDisplayInfo();
        this.addDisplayInfo(String.format(Locale.US, "%.2f", lastTotalDamage));
        this.addDisplayInfo(String.format(Locale.US, "%.4f", lastCalcTimeMs));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender3DEvent(Render3DEvent event) {
        if (MC.level == null || MC.player == null || placeTarget == null || !render.get()) return;

        BlockPos pos = placeTarget.pos.below();
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        Color color = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor();

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private void doBreakOnNetty(EndCrystal crystal) { // we are not on netty actually


        if (!breakMultitask.get() && MC.player.isUsingItem())  {
            return;
        }

        if (breakRotate.get()) {
            Vec3 hit = getClosestPointToEye(MC.player.getEyePosition(), crystal.getBoundingBox());

            float yaw = (float) getYawToVec(MC.player, hit);
            float pitch = (float) getPitchToVec(MC.player, hit);

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 9, yaw, pitch));
        }

        if (!canBreak(crystal)) {
            return;
        }

        MC.player.connection.send(ServerboundInteractPacket.createAttackPacket(crystal, MC.player.isShiftKeyDown()));

        if (breakSwing.get())
            MC.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        //breakTimer = breakDelay.get();
    }

    private void doBreak() {
        BreakTarget target = bestCrystal();
        if (target == null) return;

        if (!breakMultitask.get() && MC.player.isUsingItem()) return;

        if (breakRotate.get()) {
            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), target.crystal.getBoundingBox());
            float yaw = (float) getYawToVec(MC.player, pos);
            float pitch = (float) getPitchToVec(MC.player, pos);

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 9, yaw, pitch));
        }

        if (!canBreak(target.crystal)) return;

        int id = target.crystal.getId();
        int hits = crystalHits.get(id);

        if (hits >= breakInhibit.get() && target.crystal.tickCount < 20)
            return;

        MC.gameMode.attack(MC.player, target.crystal);

        if (breakSwing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        crystalHits.put(id, hits + 1);

        breakTimer = breakDelay.get();
    }

    private BreakTarget bestCrystal() {
        BreakTarget best = null;

        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.END_CRYSTALS, 10)) {
            if (!(e instanceof EndCrystal crystal)) continue;

            if (e.tickCount < breakAge.get()) continue;

            long posKey = crystal.blockPosition().asLong();

            if (crystal.tickCount < 20 && !crystalPlaces.containsKey(posKey))
                continue;

            //   if (MC.player.distanceToSqr(crystal) > 10 * 10) continue;

            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), crystal.getBoundingBox());
            float yaw = (float) getYawToVec(MC.player, pos);
            float pitch = (float) getPitchToVec(MC.player, pos);
            EntityHitResult perfect = raycastTarget(MC.player, crystal, breakRange.get(), yaw, pitch);
            boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition());

            if (!insideBox && perfect == null) continue;

            float totalDamage = damageOthers(crystal);
            if (totalDamage < minDamage.get())
                continue;

            if (best == null || totalDamage > best.totalDamage)
                best = new BreakTarget(crystal, totalDamage);
        }

        if (best != null && best.totalDamage < minDamage.get()/2)
            return null;

        return best;
    }

    private float damageOthers(EndCrystal crystal) {
        Vec3 pos = crystal.position();
        float totalDamage = 0f;

        float selfDamage = DamageUtils.crystalDamage(MC.player, MC.player.position(), MC.player.getBoundingBox(), pos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get());

        if (selfDamage > maxSelfDamage.get())
            return -1f;

        if (selfDamage + 1.5f >= MC.player.getHealth() + MC.player.getAbsorptionAmount())
            return -1f;


        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 15)) {
            if (!(e instanceof LivingEntity living)) continue;

            float dmg = DamageUtils.crystalDamage(living, living.position(), living.getBoundingBox(), pos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get(), ignoredBlocks(false));

            if (e == MC.player)
                continue;

            if (FRIEND_SERVICE.isFriend(e.getName().getString())) {
                if (dmg > maxFriendDamage.get())
                    return -1f;
            } else {
                totalDamage += dmg;
            }
        }
        return totalDamage;
    }

    private boolean canBreak(EndCrystal crystal) {
        if (!breakRotate.get())
            return true;

      //  ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 5, idealYaw, idealPitch));
        boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition(1.0f));
        EntityHitResult serverCheck = raycastTarget(MC.player, crystal, breakRange.get(), ROTATION_SERVICE.getStateHandler().getServerYaw(), ROTATION_SERVICE.getStateHandler().getServerPitch());
        return serverCheck != null || insideBox;
    }

    private void doPlace() {
        placeTarget = findBestPlace();
        if (placeTarget == null) return;
        if (placeTarget.totalDamage < minDamage.get()) return;

        InteractionUtils.interactBlockAt(placeTarget.pos.below(), Items.END_CRYSTAL, null, placeSwapBack.get(), placeMultitask.get(), placeRange.get(), placeRotate.get(), placeStrictDirection.get(), false, placeSwing.get(), AutoCrystalFeature.class.getName() + "_PLACE");

        crystalPlaces.put(placeTarget.pos.asLong(), 0);

        placeTimer = placeDelay.get();
    }

    private PlaceTarget findBestPlace() {
        long startTime = System.nanoTime();
        PlaceTarget best = null;
        BlockPos playerPos = MC.player.blockPosition();
        int r = (int) Math.ceil(placeRange.get());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockPos base = pos.below();

                    BlockState baseState = MC.level.getBlockState(base);
                    if (!baseState.is(Blocks.OBSIDIAN) && !baseState.is(Blocks.BEDROCK)) continue;
                    if (!MC.level.getBlockState(pos).isAir()) continue;

                    if (!canPlaceAt(pos)) continue;

                    EndCrystal fakeCrystal = new EndCrystal(EntityType.END_CRYSTAL, MC.level);
                    fakeCrystal.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

                    float totalDamage = calculatePlaceDamage(fakeCrystal.position());
                    if (totalDamage < minDamage.get()) continue;
                    lastTotalDamage = totalDamage;
                    if (best == null || totalDamage > best.totalDamage)
                        best = new PlaceTarget(pos, totalDamage);
                }
            }
        }

        lastCalcTimeMs = (System.nanoTime() - startTime) / 1_000_000f;
        if (best != null) {
            PlaceTarget ret = new PlaceTarget(best.pos, best.totalDamage);
            return ret;
        }
        return best; // always null here
    }

    private List<BlockPos> ignoredBlocks(boolean b) {
        List<BlockPos> ignored = new ArrayList<>();

        if (placeIgnoreTerrain.get()) {
            int r = placeRange.get().intValue()+2;
            BlockPos center = MC.player.blockPosition();

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos pos = center.offset(x, y, z);

                        BlockState state = MC.level.getBlockState(pos);
                        if (state.isAir()) continue;

                        if (state.getBlock().getExplosionResistance() < 600) {
                            ignored.add(pos);
                        }
                    }
                }
            }
        }

        if (b && placeAntiFeetTrap.get()) {
            SpeedMineFeature sm = FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class);

            if (sm != null) {
                if (sm.currentTask != null && sm.currentTask.getProgress() >= placeAntiFeetTrapFactor.get()) {
                    ignored.add(sm.currentTask.getBlockPos());
                }

                if (sm.doubleMineTask != null && sm.doubleMineTask.getProgress() >= placeAntiFeetTrapFactor.get()) {
                    ignored.add(sm.doubleMineTask.getBlockPos());
                }
            }
        }

        return ignored;
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockPos base = pos.below();

        BlockState baseState = MC.level.getBlockState(base);
        if (!baseState.is(Blocks.OBSIDIAN) && !baseState.is(Blocks.BEDROCK)) return false;

        if (!MC.level.getBlockState(pos).isAir()) return false;

        Vec3 eyePos = MC.player.getEyePosition();

        AABB blockBox = new AABB(pos);
        Vec3 point = getClosestPointToEye(eyePos, blockBox);
        float yaw = (float) getYawToVec(MC.player, point);
        float pitch = (float) getPitchToVec(MC.player, point);

        if (RotationUtils.raycastAABBFromPlayer(MC.player, blockBox, placeRange.get(), yaw, pitch) == null) {
            return false;
        }

        EndCrystal fakeCrystal = new EndCrystal(EntityType.END_CRYSTAL, MC.level);
        fakeCrystal.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        MC.level.addFreshEntity(fakeCrystal); //  thats crazy how raycast works

        AABB checkIntersects = new AABB(base.getX(), base.getY() + 1, base.getZ(), base.getX() + 1, base.getY() + 3, base.getZ() + 1);

        for (Entity e : MC.level.getEntities(null, checkIntersects)) {
            if (placeIgnoreItems.get() && e instanceof ItemEntity item && item.getAge() <= 5) continue;
            if (placeIgnoreCrystals.get() && e instanceof EndCrystal crystal && crystal.tickCount < 5) continue;
            if (e instanceof EndCrystal crystal && crystal.blockPosition().equals(pos)) continue;
            fakeCrystal.remove(Entity.RemovalReason.DISCARDED);
            return false;
        }

            Vec3 hitVec = getClosestPointToEye(eyePos, fakeCrystal.getBoundingBox());
            float idealYaw = (float) getYawToVec(MC.player, hitVec);
            float idealPitch = (float) getPitchToVec(MC.player, hitVec);
            EntityHitResult distanceCheck = raycastTarget(MC.player, fakeCrystal, breakRange.get(), idealYaw, idealPitch);
            boolean insideBox = fakeCrystal.getBoundingBox().contains(eyePos);

            if (!insideBox && distanceCheck == null) {
                fakeCrystal.remove(Entity.RemovalReason.DISCARDED);
                return false;
            }

        fakeCrystal.remove(Entity.RemovalReason.DISCARDED);
        return true;
    }


    private float calculatePlaceDamage(Vec3 crystalPos) {
        float totalDamage = 0f;

        float selfDamage = DamageUtils.crystalDamage(MC.player, MC.player.position(), MC.player.getBoundingBox(), crystalPos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get());

        if (selfDamage > maxSelfDamage.get())
            return -1f;

        if (selfDamage + 1.5f >= MC.player.getHealth() + MC.player.getAbsorptionAmount())
            return -1f;

        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 15)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e == MC.player) continue;

            float dmg = DamageUtils.crystalDamage(living, living.position(), living.getBoundingBox(), crystalPos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get(), ignoredBlocks(true));

            if (FRIEND_SERVICE.isFriend(e.getName().getString())) {
                if (dmg > maxFriendDamage.get())
                    return -1f;
                continue;
            }

            if (FRIEND_SERVICE.isFriend(e.getName().getString())) continue;

            if (dmg < minDamage.get())
                return -1f;

            totalDamage += dmg;
        }
        return totalDamage;
    }

    private void updateMemory() {
        LongIterator it = crystalPlaces.keySet().iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int age = crystalPlaces.get(key) + 1;

            if (age > 35) {
                it.remove();
            } else {
                crystalPlaces.put(key, age);
            }
        }
    }

    private record PlaceTarget(BlockPos pos, float totalDamage) {}
    private record BreakTarget(EndCrystal crystal, float totalDamage) {}
}
