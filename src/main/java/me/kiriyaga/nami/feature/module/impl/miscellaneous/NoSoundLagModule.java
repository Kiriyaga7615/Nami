package me.kiriyaga.nami.feature.module.impl.miscellaneous;

import com.google.common.collect.Sets;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import 	net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class NoSoundLagModule extends Module { // TODO whitelist sounds

    public final BoolSetting always = addSetting(new BoolSetting("Always", false));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", true));
    public final BoolSetting withers = addSetting(new BoolSetting("Withers", true));
    public final BoolSetting firework = addSetting(new BoolSetting("Firework", false));
    public final BoolSetting elytra = addSetting(new BoolSetting("Elytra", true));

    private static final Set<Holder<SoundEvent>> ARMOR_SOUNDS = Sets.newHashSet(
            SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
            SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
            SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER
    );

    private static final Set<SoundEvent> FIREWORK_SOUNDS = Sets.newHashSet(
            SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
            SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE,
            SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
            SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR,
            SoundEvents.ENTITY_FIREWORK_ROCKET_SHOOT,
            SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST_FAR
    );

    private static final Set<SoundEvent> ELYTRA_SOUNDS = Sets.newHashSet(
            SoundEvents.ITEM_ELYTRA_FLYING
    );

    private static final Set<SoundEvent> WITHER_SOUNDS = Sets.newHashSet(
            SoundEvents.ENTITY_WITHER_AMBIENT,
            SoundEvents.ENTITY_WITHER_DEATH,
            SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
            SoundEvents.ENTITY_WITHER_HURT,
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundEvents.ENTITY_WITHER_SHOOT
    );

    private final Set<SoundEvent> activeSounds = ConcurrentHashMap.newKeySet();

    private long lastClearTime = System.currentTimeMillis();

    public NoSoundLagModule() {
        super("NoSoundLag", "Sound tweaks.", ModuleCategory.of("Miscellaneous"), "nosoundlag");
        elytra.setShowCondition(always::get );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastClearTime >= TimeUnit.SECONDS.toMillis(2)) {
            activeSounds.clear();
            lastClearTime = now;
        }

        if (event.getPacket() instanceof ClientboundSoundPacket packet) {
            SoundEvent sound = packet.getSound().comp_349();

            boolean cancel = false;

            if (always.get()) {
                if ((armor.get() && ARMOR_SOUNDS.contains(packet.getSound())) ||
                        (firework.get() && FIREWORK_SOUNDS.contains(sound)) ||
                        (elytra.get() && ELYTRA_SOUNDS.contains(sound)) ||
                        (withers.get() && WITHER_SOUNDS.contains(sound))) {
                    cancel = true;
                }
            } else {
                if (activeSounds.contains(sound)) {
                    cancel = true;
                }
            }

            if (cancel) {
                event.cancel();
            } else {
                activeSounds.add(sound);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (!isEnabled() || !elytra.get()) return;

        if (MC.player != null && MC.player.isGliding()) {
            for (SoundEvent sound : ELYTRA_SOUNDS) {
                Identifier id = Registries.SOUND_EVENT.getId(sound);
                MC.getSoundManager().stopSounds(id, SoundCategory.PLAYERS);
            }
        }
    }
}