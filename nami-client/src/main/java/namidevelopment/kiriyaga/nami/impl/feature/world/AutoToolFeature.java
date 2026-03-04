package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.StartBreakingBlockEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isBroken;

@RegisterFeature
public class AutoToolFeature extends Feature {

    public enum EchestPriority {FORTUNE, SILK}

    public final EnumSetting<EchestPriority> echestPriority = addSetting(new EnumSetting<>("Echest", EchestPriority.SILK));
    public final IntSetting damageThreshold = addSetting(new IntSetting("Durability", 3, 0, 15));

    public AutoToolFeature() {
        super("AutoTool", "Auto selects the currently best mining tool from your hotbar.", FeatureCategory.of("World"), "autotool");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (MC.player == null || MC.level == null || MC.player.gameMode() != GameType.SURVIVAL) {
            return;
        }

        MC.execute(() -> { // we are not on main thread!
            BlockPos targetPos = event.blockPos;
            BlockState targetState = MC.level.getBlockState(targetPos);

            int bestSlot = -1;
            float bestSpeed = 1.0f;

            int prioritySlot = -1;

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = MC.player.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                if (isBroken(stack, damageThreshold.get())) continue;

                boolean matchesPriority = false;
                switch (echestPriority.get()) {
                    case SILK:
                        matchesPriority = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SILK_TOUCH) > 0;
                        break;
                    case FORTUNE:
                        matchesPriority = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FORTUNE) > 0;
                        break;
                }

                if (matchesPriority) {
                    prioritySlot = slot;
                    break;
                }
            }

            if (prioritySlot != -1) {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(prioritySlot, false);
                return;
            }

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = MC.player.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                if (isBroken(stack, damageThreshold.get())) continue;

                float totalSpeed = 1.0f;
                if (stack.isCorrectToolForDrops(targetState)) {
                    float efficiencyLevel = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
                    float miningSpeed = stack.getDestroySpeed(targetState);
                    totalSpeed = miningSpeed * (1 + efficiencyLevel * 0.2f);
                }


                if (totalSpeed > bestSpeed) {
                    bestSpeed = totalSpeed;
                    bestSlot = slot;
                }
            }

            if (bestSlot != -1)
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(bestSlot, false);
        });
    }
}
