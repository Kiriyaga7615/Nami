package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.Timer;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;

import static namidevelopment.kiriyaga.api.NamiApi.INVENTORY_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.ROTATION_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class AutoPotFeature extends Feature { // TODO: refactor this

    public enum SwapMode { NORMAL, SILENT }
    public enum ThrowMode { ABOVE, UNDER }

    public final EnumSetting<Pot> potEffect = addSetting(new EnumSetting<>("Effect", Pot.RESISTANCE));
    public final IntSetting amplifier = addSetting(new IntSetting("Amplifier", 1, 0, 4));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", false));
    public final EnumSetting<ThrowMode> throwMode = addSetting(new EnumSetting<>("Throw", ThrowMode.UNDER));
    public final BoolSetting whenNoTarget = addSetting(new BoolSetting("NoTarget", false));
    public final BoolSetting onlyPhased = addSetting(new BoolSetting("OnlyPhased", false));
    public final EnumSetting<SwapMode> swapMode = addSetting(new EnumSetting<>("Swap", SwapMode.SILENT));
    public final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", true));

    private final Timer throwTimer = new Timer();

    public AutoPotFeature() {
        super("AutoPot", "Throws specified splash potion under/above you.", FeatureCategory.of("Combat"), "autopot");
        throwMode.setShowCondition(rotate::get);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onPreTick(PreTickEvent ev) {
        if (MC.player == null || MC.level == null) return;
        if (MC.player.getEffect(potEffect.get().getEffect()) != null && MC.player.getEffect(potEffect.get().getEffect()).getAmplifier() >= amplifier.get()) {
            if (selfToggle.get())
                toggle();
            return;
        }

        if (whenNoTarget.get() && TargetUtils.getTarget() != null) {
            if (selfToggle.get())
                toggle();
            return;
        }

        if (onlyPhased.get() && !isPhased()) {
            if (selfToggle.get())
                toggle();

            return;
        }

        int potSlot = getSlot(potEffect.get());
        int potInvSlot = findPot(potEffect.get());
        int prev = MC.player.getInventory().getSelectedSlot();
        if (potSlot == -1) {
            if (potInvSlot != -1) {
                if (!throwTimer.hasElapsed(5000)) return;

                float pitch = 90.00f;

                switch (throwMode.get()) {
                    case ABOVE -> pitch = -90.00f;
                    case UNDER -> pitch = 90.00f;
                    default -> pitch = 90.00f;
                }

                if (rotate.get()) {
                    ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.name, 6, MC.player.getYRot(), pitch, RotationsFeature.RotationMode.MOTION));

                    if (!ROTATION_SERVICE.getRequestHandler().isCompleted(this.name)) return;
                }

                move(potInvSlot, prev);

                throwTimer.reset();

                switch (swapMode.get()) {
                    case NORMAL -> {
                        INVENTORY_SERVICE.getSwapHandler().attemptSwitch(potSlot, false);
                        MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
                    }
                    case SILENT -> {
                        INVENTORY_SERVICE.getSwapHandler().attemptSwitch(potSlot, true);
                        MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
                    }
                }

                move(potInvSlot, prev);
            } else {
                if (selfToggle.get())
                    toggle();
                return;
            }
        }

        if (!throwTimer.hasElapsed(5000)) return;

        float pitch = 90.00f;

        switch (throwMode.get()) {
            case ABOVE -> pitch = -90.00f;
            case UNDER -> pitch = 90.00f;
            default -> pitch = 90.00f;
        }

        if (rotate.get()) {
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                    this.name,
                    6,
                    MC.player.getYRot(),
                    pitch,
                    RotationsFeature.RotationMode.MOTION
            ));

            if (!ROTATION_SERVICE.getRequestHandler().isCompleted(this.name)) return;
        }

        throwTimer.reset();

        switch (swapMode.get()) {
            case NORMAL -> {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(potSlot, false);
                MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            }
            case SILENT -> {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(potSlot, true);
                MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            }
        }
    }

    private int getSlot(Pot targetEffect) {
        int requiredAmp = amplifier.get();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;

            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.getEffect() == targetEffect.getEffect() && inst.getAmplifier() >= requiredAmp) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void move(int invSlot, int hotbarSlot) {
        int realInv = invSlot < 9 ? invSlot + 36 : invSlot;
        int realHotbar = hotbarSlot + 36;

        INVENTORY_SERVICE.getClickHandler().pickupSlot(realInv);
        INVENTORY_SERVICE.getClickHandler().pickupSlot(realHotbar);
        INVENTORY_SERVICE.getClickHandler().pickupSlot(realInv);
    }

    private int findPot(Pot targetEffect) {
        int requiredAmp = amplifier.get();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;

            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.getEffect() == targetEffect.getEffect() && inst.getAmplifier() >= requiredAmp) {
                    return i;
                }
            }
        }
        return -1;
    }


    public enum Pot {
        STRENGTH(MobEffects.STRENGTH),
        SPEED(MobEffects.SPEED),
        JUMP_BOOST(MobEffects.JUMP_BOOST),
        RESISTANCE(MobEffects.RESISTANCE);

        private final Holder<MobEffect> effect;

        Pot(Holder<MobEffect> effect) {
            this.effect = effect;
        }

        public Holder<MobEffect> getEffect() {
            return effect;
        }
    }

    private boolean isPhased() {
        LocalPlayer player = MC.player;
        if (player == null || MC.level == null) return false;

        AABB box = player.getBoundingBox();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    VoxelShape shape = MC.level.getBlockState(pos).getCollisionShape(MC.level, pos);
                    if (!shape.isEmpty() && shape.bounds().move(pos).intersects(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
