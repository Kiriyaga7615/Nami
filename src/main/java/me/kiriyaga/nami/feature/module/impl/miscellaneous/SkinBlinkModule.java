package me.kiriyaga.nami.feature.module.impl.miscellaneous;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PostTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.GameOptionsAccessor;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class SkinBlinkModule extends Module {

    public enum Limb {
        HEAD(PlayerModelPart.CAPE, PlayerModelPart.HAT),
        BODY(PlayerModelPart.JACKET),
        ARMS(PlayerModelPart.LEFT_SLEEVE, PlayerModelPart.RIGHT_SLEEVE),
        LEGS(PlayerModelPart.LEFT_PANTS_LEG, PlayerModelPart.RIGHT_PANTS_LEG);

        private final PlayerModelPart[] parts;

        Limb(PlayerModelPart... parts) {
            this.parts = parts;
        }

        public PlayerModelPart[] getParts() {
            return parts;
        }
    }

    public final BoolSetting head = addSetting(new BoolSetting("Head", true));
    public final BoolSetting body = addSetting(new BoolSetting("Body", false));
    public final BoolSetting arms = addSetting(new BoolSetting("Arms", false));
    public final BoolSetting legs = addSetting(new BoolSetting("Legs", false));
    public final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 1.0, 0.1, 20.0));
    public final BoolSetting random = addSetting(new BoolSetting("Random", false));

    private Set<PlayerModelPart> enabledPlayerModelParts;
    private long lastBlinkTime = 0;
    private final Random randomizer = new Random();

    public SkinBlinkModule() {
        super("SkinBlink", "Blinks player model layer parts.", ModuleCategory.of("Miscellaneous"), "skinblink");
    }

    @Override
    public void onEnable() {
        if (MC.options == null) return;

        enabledPlayerModelParts = ((GameOptionsAccessor) MC.options).getPlayerModelParts();
    }

    @Override
    public void onDisable() {
        if (MC.options == null || enabledPlayerModelParts == null) return;

        for (PlayerModelPart part : PlayerModelPart.values()) {
            boolean shouldEnable = enabledPlayerModelParts.contains(part);
            MC.options.setPlayerModelPart(part, shouldEnable);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onTick(PostTickEvent event) {
        if (MC.options == null) return;

        long now = System.currentTimeMillis();
        if (now - lastBlinkTime < speed.get() * 50) return;
        lastBlinkTime = now;

        Set<PlayerModelPart> currentParts = ((GameOptionsAccessor) MC.options).getPlayerModelParts();
        Set<Limb> limbsToToggle = EnumSet.noneOf(Limb.class);

        if (head.get()) limbsToToggle.add(Limb.HEAD);
        if (body.get()) limbsToToggle.add(Limb.BODY);
        if (arms.get()) limbsToToggle.add(Limb.ARMS);
        if (legs.get()) limbsToToggle.add(Limb.LEGS);

        for (Limb limb : limbsToToggle) {
            for (PlayerModelPart part : limb.getParts()) {
                boolean isEnabled = currentParts.contains(part);
                boolean newValue = random.get() ? randomizer.nextBoolean() : !isEnabled;
                MC.options.setPlayerModelPart(part, newValue);
            }
        }
    }
}
