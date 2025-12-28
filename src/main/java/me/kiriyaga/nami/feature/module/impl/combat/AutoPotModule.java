package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.util.Timer;
import me.kiriyaga.nami.util.entity.TargetUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AutoPotModule extends Module {

    public enum SwapMode { NORMAL, SILENT }
    public enum ThrowMode { ABOVE, UNDER }

    private final EnumSetting<Pot> potEffect = addSetting(new EnumSetting<>("Effect", Pot.RESISTANCE));
    private final EnumSetting<ThrowMode> throwMode = addSetting(new EnumSetting<>("Throw", ThrowMode.UNDER));
    private final BoolSetting whenNoTarget = addSetting(new BoolSetting("NoTarget", false));
    private final BoolSetting onlyPhased = addSetting(new BoolSetting("OnlyPhased", false));
    private final EnumSetting<SwapMode> swapMode = addSetting(new EnumSetting<>("Swap", SwapMode.NORMAL));
    private final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", true));

    private final Timer throwTimer = new Timer();

    public AutoPotModule() {
        super("AutoPot", "Throws specified splash potion under/above you.", ModuleCategory.of("Combat"), "autopot");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onPreTick(PreTickEvent ev) {
        if (!isEnabled() || MC.player == null || MC.world == null) return;

        if (MC.player.hasStatusEffect(potEffect.get().getEffect())) {
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
        if (potSlot == -1) {
            if (selfToggle.get())
                toggle();
            return;
        }

        if (!throwTimer.hasElapsed(5000)) return;

        float pitch = 90.00f;

        switch (throwMode.get()) {
            case ABOVE -> pitch = -90.00f;
            case UNDER -> pitch = 90.00f;
            default -> pitch = 90.00f;
        }

        ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                this.name,
                6,
                MC.player.getYaw(),
                pitch,
                RotationModule.RotationMode.MOTION
        ));

        if (!ROTATION_MANAGER.getRequestHandler().isCompleted(this.name)) return;

        int prevSlot = MC.player.getInventory().getSelectedSlot();
        throwTimer.reset();

        switch (swapMode.get()) {
            case NORMAL -> {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(potSlot);
                MC.interactionManager.interactItem(MC.player, InteractionHand.MAIN_HAND);
            }
            case SILENT -> {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(potSlot);
                MC.interactionManager.interactItem(MC.player, InteractionHand.MAIN_HAND);
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(prevSlot);
            }
        }
    }

    private int getSlot(Pot targetEffect) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) continue;

            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;

            for (MobEffectInstance inst : contents.getEffects()) {
                if (inst.getEffectType() == targetEffect.getEffect()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public enum Pot {
        STRENGTH(MobEffects.STRENGTH, Items.SPLASH_POTION),
        SPEED(MobEffects.SPEED, Items.SPLASH_POTION),
        JUMP_BOOST(MobEffects.JUMP_BOOST, Items.SPLASH_POTION),
        RESISTANCE(MobEffects.RESISTANCE, Items.SPLASH_POTION);

        private final Holder effect;
        private final Item item;

        Pot(Holder effect, Item item) {
            this.effect = effect;
            this.item = item;
        }

        public Holder getEffect() {
            return effect;
        }

        public Item getItem() {
            return item;
        }
    }

    private boolean isPhased() {
        LocalPlayer player = MC.player;
        if (player == null || MC.world == null) return false;

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
                    VoxelShape shape = MC.world.getBlockState(pos).getCollisionShape(MC.world, pos);
                    if (!shape.isEmpty() && shape.getBoundingBox().offset(pos).intersects(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
