package namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.*;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.entity.DamageUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.world.SpeedMineFeature;
import namidevelopment.kiriyaga.nami.mixininterface.ILivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;
import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isBroken;

@RegisterFeature
public class AutoCrystalFeature extends Feature {
    public enum Sequential {NONE, FULL }

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
    public final IntSetting balance = addSetting(new IntSetting("Balance", 4, 2, 6));
    public final DoubleSetting healthBalance = addSetting(new DoubleSetting("HealthBalance", 0.20, 0.00, 1.00));
    public final DoubleSetting armorBalance = addSetting(new DoubleSetting("ArmorBalance", 0.20, 0.00, 1.00));
    public final BoolSetting antiFeetTrap = addSetting(new BoolSetting("AntiFeetTrap", true));
    public final DoubleSetting antiFeetTrapFactor = addSetting(new DoubleSetting("Factor", 0.80, 0.5, 1.00));

    //render
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));
    public final BoolSetting debug = addSetting(new BoolSetting("Debug", false));

    private int breakTimer, placeTimer = 0; // i love it
    private PlaceTarget lastPlaceTarget = null;
    public float lastTotalDamage;
    float lastCalcTimeMs = 0;

    private final Int2IntOpenHashMap crystalHits = new Int2IntOpenHashMap();
    private final Long2IntOpenHashMap crystalPlaces = new Long2IntOpenHashMap();
    private final Set<Integer> deadIds = ConcurrentHashMap.newKeySet();


    private final ExecutorService calcExecutor = Executors.newSingleThreadExecutor();
    private volatile Future<?> runningTask;
    private final AtomicReference<PlaceTarget> asyncBest = new AtomicReference<>();
    private volatile PlaceTarget bestPlace;

    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;
    private ChunkAccess cachedChunk;

    public AutoCrystalFeature() {
        super("AutoCrystal", "Automatically places and break crystals to kill people, if you are good enough!.", FeatureCategory.of("Combat"), "autocrystal", "ac", "crystalaura");
        debug.setShow(false);

        breakRange.setShowCondition(() -> doBreak.get());
        breakDelay.setShowCondition(() -> doBreak.get());
        breakRotate.setShowCondition(() -> doBreak.get());
        breakSwing.setShowCondition(() -> doBreak.get());
        breakMultitask.setShowCondition(() -> doBreak.get());
        breakAge.setShowCondition(() -> doBreak.get());
        breakSequential.setShowCondition(() -> doBreak.get());
        breakInhibit.setShowCondition(() -> doBreak.get());

        placeRange.setShowCondition(() -> doPlace.get());
        placeDelay.setShowCondition(() -> doPlace.get());
        placeRotate.setShowCondition(() -> doPlace.get());
        placeSwing.setShowCondition(() -> doPlace.get());
        placeIgnoreItems.setShowCondition(() -> doPlace.get());
        placeMultitask.setShowCondition(() -> doPlace.get());
        placeSwapBack.setShowCondition(() -> doPlace.get());
        placeIgnoreCrystals.setShowCondition(() -> doPlace.get());
        placeStrictDirection.setShowCondition(() -> doPlace.get());
        placeIgnoreTerrain.setShowCondition(() -> doPlace.get());
    }

    @Override
    public void onEnable() {
        breakTimer = 0;
        placeTimer = 0;
        lastTotalDamage = 0;
        lastCalcTimeMs = 0;
        lastPlaceTarget = null;
        crystalPlaces.clear();
        crystalHits.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTickEvent(PreTickEvent event) {
        long start = System.nanoTime();
        if (MC.player == null || MC.player.isDeadOrDying()) return;

        update();

        long tickId = MC.level.getGameTime();

        if (runningTask == null || runningTask.isDone()) {
            AutoCrystalSnapshot.AsyncDebugInfo dbg = new AutoCrystalSnapshot.AsyncDebugInfo(tickId);

            AutoCrystalSnapshot snap = doSnapshot(tickId, dbg);
            runningTask = calcExecutor.submit(() -> {
                PlaceTarget best = findNextPlaceTargetForSnapshot(snap, dbg);
                asyncBest.set(best);

                if (debug.get()) {
                    float ms = (System.nanoTime() - dbg.startNs) / 1_000_000f;

                    MC.execute(() -> {
                        CHAT_SERVICE.sendPersistent(
                                "AutoCrystalFeature#asyncCalc",
                                dbg.buildMessage(ms)
                        );
                    });
                }
            });
        }

        if (this.asyncBest.get() != null) {
            BlockPos pos = this.asyncBest.get().pos;
            BlockPos base = pos.below();

            Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

            float realDamage = calculateDamage(crystalPos);

            if (realDamage > 0.0f) {
                bestPlace = new PlaceTarget(pos, realDamage);
                lastTotalDamage = realDamage;
            } else {
                bestPlace = null;
            }
        } else {
            bestPlace = null;
        }

        if (doBreak.get()) {
            if (breakTimer > 0) breakTimer--;
            else doBreak();
        }

        if (doPlace.get()) {
            if (placeTimer > 0) placeTimer--;
            else {
                if (bestPlace != null) {
                    doPlace(bestPlace);
                }
            }
        }

        lastCalcTimeMs = (System.nanoTime() - start) / 1_000_000f;

        this.clearDisplayInfo();
        this.addDisplayInfo(String.format(Locale.US, "%.2f", lastTotalDamage));
        this.addDisplayInfo(String.format(Locale.US, "%.4f", lastCalcTimeMs));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
        if (packet.getEventId() != 3) return;

        // Author: cattyngmd
        MC.execute(() -> {
            Entity e = packet.getEntity(MC.level);
            if (e instanceof LivingEntity living) {
                ((ILivingEntity) living).setServerSideDead(true);
            }
            if (e instanceof Player player) {
                deadIds.add(e.getId());
            }
        });
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

    @SubscribeEvent(priority = EventPriority.HIGH)
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

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender3DEvent(Render3DEvent event) {
        if (MC.level == null || MC.player == null || lastPlaceTarget == null || !render.get()) return;

        BlockPos pos = lastPlaceTarget.pos.below();
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

            float yaw = (float) getYRotToVec(MC.player, hit);
            float pitch = (float) getXRotToVec(MC.player, hit);

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
        if (target == null || !breakMultitask.get() && MC.player.isUsingItem()) {
            reset();
            return;
        }

        if (breakRotate.get()) {
            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), target.crystal.getBoundingBox());
            float yaw = (float) getYRotToVec(MC.player, pos);
            float pitch = (float) getXRotToVec(MC.player, pos);

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

            if (crystal.tickCount < 5 && !crystalPlaces.containsKey(posKey))
                continue;

            //   if (MC.player.distanceToSqr(crystal) > 10 * 10) continue;

            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), crystal.getBoundingBox());
            float yaw = (float) getYRotToVec(MC.player, pos);
            float pitch = (float) getXRotToVec(MC.player, pos);
            EntityHitResult perfect = raycastTarget(MC.player, crystal, breakRange.get(), yaw, pitch);
            boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition());

            if (!insideBox && perfect == null) continue;

            float totalDamage = calculateDamage(crystal.position());
            if (totalDamage <= -0.9f)
                continue;

            if (best == null || totalDamage > best.totalDamage)
                best = new BreakTarget(crystal, totalDamage);
        }

        if (best != null && best.totalDamage <= 0.0f)
            return null;

        return best;
    }

    private boolean canBreak(EndCrystal crystal) {
        if (!breakRotate.get())
            return true;

      //  ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 5, idealYaw, idealPitch));
        boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition(1.0f));
        EntityHitResult serverCheck = raycastTarget(MC.player, crystal, breakRange.get(), ROTATION_SERVICE.getStateHandler().getServerYRot(), ROTATION_SERVICE.getStateHandler().getServerXRot());
        return serverCheck != null || insideBox;
    }

    private void doPlace(PlaceTarget target) {
        if (target == null || target.totalDamage < 0 || MC.player.isUsingItem() && !placeMultitask.get()){
            reset();
            return;
        }

        InteractionUtils.interactBlockAt(target.pos.below(), Items.END_CRYSTAL, null, placeSwapBack.get(), placeMultitask.get(), placeRange.get(), placeRotate.get(), placeStrictDirection.get(), false, placeSwing.get(), AutoCrystalFeature.class.getName() + "_PLACE");

        lastPlaceTarget = target;

        crystalPlaces.put(target.pos.asLong(), 0);
        placeTimer = placeDelay.get();
    }

    private AutoCrystalSnapshot doSnapshot(long tickId, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        Vec3 eyePos = MC.player.getEyePosition();
        BlockPos playerPos = MC.player.blockPosition();

        double pr = placeRange.get();
        double br = breakRange.get();
        double minDmg = minDamage.get();

        List<Player> entities = EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 12).stream().filter(e -> e instanceof LivingEntity).map(e -> (Player) e).toList();
        dbg.targetsTotal = entities.size();

        ArrayList<AutoCrystalSnapshot.TargetData> targets = new ArrayList<>();

        for (Player e : entities) {
            if (e.isDeadOrDying())
                continue;
            if (SOCIALS_SERVICE.isFriend(e.getName().getString()))
                continue;
            if (((ILivingEntity) e).isServerSideDead())
                continue;

            dbg.targetsValid++;

            int resistanceAmp = -1;
            MobEffectInstance eff = e.getEffect(MobEffects.RESISTANCE);
            if (eff != null) resistanceAmp = eff.getAmplifier();

            byte mask = 0;
            if (!e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) mask |= 1;
            if (!e.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) mask |= 2;
            if (!e.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) mask |= 4;
            if (!e.getItemBySlot(EquipmentSlot.FEET).isEmpty()) mask |= 8;

            int prot = 0;
            int blastProt = 0;

            if (!assumeBestArmor.get()) {
                for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                    ItemStack stack = e.getItemBySlot(slot);
                    if (stack.isEmpty())
                        continue;

                    prot += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROTECTION);
                    blastProt += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION);
                }
            }

            boolean broken = false;
            int threshold = (int) (armorBalance.get() * 100.0);

            for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                ItemStack stack = e.getItemBySlot(slot);
                if (stack.isEmpty()) continue;

                if (isBroken(stack, threshold)) {
                    broken = true;
                    break;
                }
            }

            targets.add(new AutoCrystalSnapshot.TargetData(e.getId(), e.position(), e.getBoundingBox(), (float) Math.floor(e.getAttributeValue(Attributes.ARMOR)), (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS), resistanceAmp, mask, prot, blastProt, e.getHealth(), e.getAbsorptionAmount(), broken));
        }

        int r = (int) Math.ceil(pr);
        int rr = r * r;

        ArrayList<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> ignored = ignoredBlocks();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {

                    if (x * x + y * y + z * z > rr)
                        continue;

                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockPos base = pos.below();
                    BlockState baseState = MC.level.getBlockState(base);
                    if (!baseState.is(Blocks.OBSIDIAN) && !baseState.is(Blocks.BEDROCK)) {
                        dbg.candidatesBadBase++;
                        continue;
                    }

                    BlockState state = MC.level.getBlockState(pos);
                    if (!state.isAir() && !(state.getBlock().equals(Blocks.FIRE) && MC.level.dimension() == Level.END)) {
                        dbg.candidatesNotAir++;
                        continue;
                    }

                    AABB blockBox = new AABB(pos);
                    if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > pr) {
                        dbg.candidatesOutPlaceRange++;
                        continue;
                    }

                    Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
                    AABB crystalBox = new AABB(crystalPos.x-1, crystalPos.y, crystalPos.z-1, crystalPos.x+1, crystalPos.y + 2.0, crystalPos.z + 1.0);

                    if (eyePos.distanceTo(getClampClosestPoint(eyePos, crystalBox)) > br) {
                        dbg.candidatesOutBreakRange++;
                        continue;
                    }

                    AABB checkIntersects = new AABB(
                            base.getX(), base.getY() + 1, base.getZ(),
                            base.getX() + 1, base.getY() + 2, base.getZ() + 1
                    );

                    boolean blocked = false;
                    for (Entity e : MC.level.getEntities(null, checkIntersects)) {
                        if (placeIgnoreItems.get() && e instanceof ItemEntity item && item.getAge() <= 5) continue;
                        if (placeIgnoreCrystals.get() && e instanceof EndCrystal crystal && crystal.tickCount < 5) continue;
                        if (e instanceof EndCrystal crystal && crystal.blockPosition().equals(pos)) continue;
                        blocked = true;
                        break;
                    }

                    if (blocked) {
                        dbg.candidatesBlocked++;
                        continue;
                    }
                    dbg.candidatesTotal++;
                    candidates.add(pos);
                }
            }
        }

        return new AutoCrystalSnapshot(tickId, MC.player.getId(), eyePos, playerPos, pr, br, minDmg, assumeBestArmor.get(), MC.level.getDifficulty(), true, MC.level, targets.toArray(new AutoCrystalSnapshot.TargetData[0]), candidates.toArray(new BlockPos[0]), ignored);
    }

    private PlaceTarget findNextPlaceTargetForSnapshot(AutoCrystalSnapshot snap, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        PlaceTarget best = null;

        for (BlockPos pos : snap.candidatePos()) {
            BlockPos base = pos.below();

            Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

            float dmg = calculateDamageForSnapshot(crystalPos, snap, dbg);
            if (dmg < 0) continue;

            if (best == null || dmg > best.totalDamage) {
                best = new PlaceTarget(pos, dmg);
            }
        }

        if (best != null)
            dbg.bestFound = 1;
        return best;
    }

    private float calculateDamageForSnapshot(Vec3 explosionPos, AutoCrystalSnapshot snap, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        float total = 0.0f;
        boolean any = false;

        for (var t : snap.targets()) {
            double dist = t.pos().distanceTo(explosionPos);
            if (dist > 12.0) continue;


            if (deadIds.contains(t.id()))
                continue;

            double exposure = calculateExposureForSnapshot(explosionPos, t.box(), snap);
            if (exposure <= 0.0) continue;

            double impact = (1.0 - (dist / 12.0)) * exposure;
            if (impact <= 0.0) continue;

            float baseDamage = (float)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
            float dmg = applyReductionsForSnapshot(baseDamage, t, snap);

            if (t.id() == snap.selfId()) {
                if (dmg > maxSelfDamage.get()) {
                    dbg.dmgRejectedSelf++;
                    return -1f;
                }

                if (noSelfPop.get() && dmg + 1.5f >= t.health() + t.absorption()) {
                    dbg.dmgRejectedNoSelfPop++;
                    return -1f;
                }

                continue;
            }
            double dynMin = getMinDamage(t.health(), t.absorption(), t.armorBroken());

            if (dmg < dynMin) {
                dbg.dmgRejectedMin++;
                continue;
            }

            total += dmg;
            any = true;
        }
        return any ? total : -1.0f;
    }

    private float applyReductionsForSnapshot(float damage, AutoCrystalSnapshot.TargetData t, AutoCrystalSnapshot snap) {
        if (snap.scalesWithDifficulty()) {
            switch (snap.difficulty()) {
                case EASY -> damage = Math.min(damage / 2f + 1f, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        damage = getDamageAfterAbsorbForSnapshot(damage, t.armor(), t.toughness());
        if (t.resistanceAmp() >= 0) {
            damage *= 1.0f - 0.2f * (t.resistanceAmp() + 1);
        }
        damage = reduceByProtectionForSnapshot(damage, t, snap.assumeBestArmor());
        return Math.max(damage, 0.0f);
    }

    private float reduceByProtectionForSnapshot(float damage, AutoCrystalSnapshot.TargetData t, boolean assumeBestArmor) {
        int totalProtection = 0;
        if (assumeBestArmor) {
            if ((t.armorMask() & 1) != 0) totalProtection += 4;
            if ((t.armorMask() & 2) != 0) totalProtection += 4;
            if ((t.armorMask() & 8) != 0) totalProtection += 4;
            if ((t.armorMask() & 4) != 0) totalProtection += 8;
        } else {
            totalProtection += t.prot();
            totalProtection += 2 * t.blastProt();
        }

        return CombatRules.getDamageAfterMagicAbsorb(damage, totalProtection);
    }

    private float calculateExposureForSnapshot(Vec3 source, AABB box, AutoCrystalSnapshot snap) {
        double dx = box.getXsize();
        double dy = box.getYsize();
        double dz = box.getZsize();

        int steps = 2;
        int hits = 0;
        int misses = 0;

        DamageUtils.ExposureContext ctx = new DamageUtils.ExposureContext(Vec3.ZERO, Vec3.ZERO);

        for (double x = 0; x <= dx; x += dx / steps) {
            for (double y = 0; y <= dy; y += dy / steps) {
                for (double z = 0; z <= dz; z += dz / steps) {

                    Vec3 pos = new Vec3(box.minX + x, box.minY + y, box.minZ + z);
                    ctx.set(pos, source);
                    if (raycastForSnapshot(ctx, snap) == null) {
                        misses++;
                    }

                    hits++;
                }
            }
        }

        return hits == 0 ? 0f : (float) misses / hits;
    }

    private BlockHitResult raycastForSnapshot(DamageUtils.ExposureContext ctx, AutoCrystalSnapshot snap) {
        return BlockGetter.traverseBlocks(
                ctx.start(), ctx.end(),
                ctx,
                (context, pos) -> {

                    if (snap.ignoredBlocks() != null && snap.ignoredBlocks().contains(pos))
                        return null;

                    BlockState state = getBlockFast(snap.level(), pos);
                    return state.getCollisionShape(snap.level(), pos).clip(context.start(), context.end(), pos);
                },
                context -> null
        );
    }

    public static float getDamageAfterAbsorbForSnapshot(float damage, float armor, float toughness) {  //package net.minecraft.world.damagesource;  class CombatRules
        float i = 2.0F + toughness / 4.0F;
        float j = Mth.clamp(armor - damage / i, armor * 0.2F, 20.0F);
        float k = j / 25.0F;
        return damage * (1.0F - k);
    }

    private Set<BlockPos> ignoredBlocks() {
        Set<BlockPos> ignored = new HashSet<>();

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

        if (antiFeetTrap.get()) {
            SpeedMineFeature sm = FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class);

            if (sm != null) {
                if (sm.currentTask != null && sm.currentTask.getProgress() >= antiFeetTrapFactor.get()) {
                    ignored.add(sm.currentTask.getBlockPos());
                }

                if (sm.doubleMineTask != null && sm.doubleMineTask.getProgress() >= antiFeetTrapFactor.get()) {
                    ignored.add(sm.doubleMineTask.getBlockPos());
                }
            }
        }

        return ignored;
    }

    private float calculateDamage(Vec3 crystalPos) {
        float total = 0f;
        boolean any = false;
        Set<BlockPos> ignored = ignoredBlocks();

        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 12)) {
            if (!(e instanceof Player player)) continue;

            float dmg = DamageUtils.crystalDamage(player, player.position(), player.getBoundingBox(), crystalPos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get(), ignored);

            if (e == MC.player) {
                if (dmg > maxSelfDamage.get())
                    return -1f;

                if (dmg + 1.5f >= MC.player.getHealth() + MC.player.getAbsorptionAmount())
                    return -1f;

                continue;
            }

            if (SOCIALS_SERVICE.isFriend(e.getName().getString())) continue;

            boolean armorBroken = isAnyArmorBroken(player);
            double dynMin = getMinDamage(player.getHealth(), player.getAbsorptionAmount(), armorBroken);

            if (dmg < dynMin)
                continue;


            total += dmg;
            any = true;
        }
        return any ? total : -1.0f;
    }

    private void update() {
        reset();

        if (MC.player.tickCount % 100 == 0)
            deadIds.clear();

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

    private void reset(){
        lastPlaceTarget = null;
        lastTotalDamage = 0;
        lastCalcTimeMs = 0;
    }

    private double getMinDamage(float health, float absorption, boolean armorBroken) {
        double min = minDamage.get();
        int div = Math.max(1, balance.get());

        float hp = health + absorption;
        float hpPercent = hp / 36.0f;

        if (hpPercent <= healthBalance.get()) {
            min /= div;
        }

        if (armorBroken) {
            min /= div;
        }

        return min;
    }

    private boolean isAnyArmorBroken(Player p) {
        int threshold = (int) (armorBalance.get() * 100.0);

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack stack = p.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            if (isBroken(stack, threshold))
                return true;
        }
        return false;
    }

    private BlockState getBlockFast(Level level, BlockPos pos) {

        if (level.isOutsideBuildHeight(pos.getY())) {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (cachedChunkX != chunkX || cachedChunkZ != chunkZ) {
            cachedChunk = level.getChunk(chunkX, chunkZ);
            cachedChunkX = chunkX;
            cachedChunkZ = chunkZ;
        }

        var chunk = cachedChunk;

        if (chunk != null) {
            var section = chunk.getSections()[level.getSectionIndex(pos.getY())];

            if (section != null && !section.hasOnlyAir()) {
                return section.getBlockState(
                        pos.getX() & 15,
                        pos.getY() & 15,
                        pos.getZ() & 15
                );
            }
        }

        return Blocks.AIR.defaultBlockState();
    }

    private record PlaceTarget(BlockPos pos, float totalDamage) {}
    private record BreakTarget(EndCrystal crystal, float totalDamage) {}
}
