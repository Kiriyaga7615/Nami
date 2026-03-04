package namidevelopment.kiriyaga.nami.impl.feature.miscellaneous;

import com.google.common.collect.Sets;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class NoSoundLagFeature extends Feature { // TODO whitelist sounds
// todo: rewrite this garbage
    public final BoolSetting always = addSetting(new BoolSetting("Always", false));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", true));
    public final BoolSetting withers = addSetting(new BoolSetting("Withers", true));
    public final BoolSetting firework = addSetting(new BoolSetting("Firework", false));
    public final BoolSetting elytra = addSetting(new BoolSetting("Elytra", true));

    private static final Set<Holder<SoundEvent>> ARMOR_SOUNDS = Sets.newHashSet(
            SoundEvents.ARMOR_EQUIP_GENERIC,
            SoundEvents.ARMOR_EQUIP_ELYTRA,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            SoundEvents.ARMOR_EQUIP_IRON,
            SoundEvents.ARMOR_EQUIP_GOLD,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            SoundEvents.ARMOR_EQUIP_LEATHER
    );

    private static final Set<SoundEvent> FIREWORK_SOUNDS = Sets.newHashSet(
            SoundEvents.FIREWORK_ROCKET_LAUNCH,
            SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundEvents.FIREWORK_ROCKET_TWINKLE,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR,
            SoundEvents.FIREWORK_ROCKET_SHOOT,
            SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR,
            SoundEvents.FIREWORK_ROCKET_BLAST_FAR
    );

    private static final Set<SoundEvent> ELYTRA_SOUNDS = Sets.newHashSet(
            SoundEvents.ELYTRA_FLYING
    );

    private static final Set<SoundEvent> WITHER_SOUNDS = Sets.newHashSet(
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.WITHER_DEATH,
            SoundEvents.WITHER_BREAK_BLOCK,
            SoundEvents.WITHER_HURT,
            SoundEvents.WITHER_SPAWN,
            SoundEvents.WITHER_SHOOT
    );

    private final Set<SoundEvent> activeSounds = ConcurrentHashMap.newKeySet();

    private long lastClearTime = System.currentTimeMillis();

    public NoSoundLagFeature() {
        super("NoSoundLag", "Sound tweaks.", FeatureCategory.of("Miscellaneous"), "nosoundlag");
        elytra.setShowCondition(always::get );
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastClearTime >= TimeUnit.SECONDS.toMillis(2)) {
            activeSounds.clear();
            lastClearTime = now;
        }

        if (event.getPacket() instanceof ClientboundSoundPacket packet) {
            SoundEvent sound = packet.getSound().value();

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

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (!isEnabled() || !elytra.get()) return;

        if (MC.player != null && MC.player.isFallFlying()) {
            for (SoundEvent sound : ELYTRA_SOUNDS) {
                Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
                MC.getSoundManager().stop(id, SoundSource.PLAYERS);
            }
        }
    }
}