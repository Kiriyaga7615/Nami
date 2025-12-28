package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.world.InteractionHand;

import java.util.concurrent.atomic.AtomicBoolean;

import static me.kiriyaga.nami.Nami.INVENTORY_MANAGER;
import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class AutoEatModule extends Module {

    private final IntSetting swapDelayTicksSetting = addSetting(new IntSetting("Delay", 5, 1, 20));
    private final DoubleSetting minHunger = addSetting(new DoubleSetting("Hunger", 19.0, 0.0, 19.0));
    private final DoubleSetting minHealth = addSetting(new DoubleSetting("Health", 0.0, 0.0, 19.0));
    private final BoolSetting allowGapples = addSetting(new BoolSetting("Gapples", true));
    private final BoolSetting allowPoisoned = addSetting(new BoolSetting("Poisoned", false));

    public final AtomicBoolean eating = new AtomicBoolean(false);
    private volatile int swapCooldown = 0;

    public AutoEatModule() {
        super("AutoEat", "Automatically eats best food.", ModuleCategory.of("World"), "autoeat");
    }

    @Override
    public void onDisable() {
        eating.set(false);
        eating.set(false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;

        if (eating.get())
            MC.interactionManager.interactItem(MC.player, InteractionHand.MAIN_HAND);

        double hunger = MC.player.getHungerManager().getFoodLevel();
        double health = MC.player.getHealth();
        boolean needsEat = hunger < minHunger.get() || health < minHealth.get();
        if (!needsEat) {
            eating.set(false);
            return;
        }

        int bestSlot = getBestFoodSlot();
        if (bestSlot == -1) {
            eating.set(false);
            return;
        }

        int currentSlot = MC.player.getInventory().getSelectedSlot();

        if (currentSlot == bestSlot) {
            eating.set(true);
        } else {
            if (swapCooldown > 0) {
                swapCooldown--;
            } else {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(bestSlot);
                swapCooldown = swapDelayTicksSetting.get();
            }
            eating.set(false);
        }
    }

//            @SubscribeEvent(priority = EventPriority.HIGHEST)
//        private void onPlaceBlock(PlaceBlockEvent event) {
//            if (MC.player != null && MC.world != null && eating.get())
//                event.cancel();
//        }

    private int getBestFoodSlot() {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            float score = getFoodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private float getFoodScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.getItem().getComponents().contains(DataComponents.FOOD)) {
            return -1;
        }

        Item item = stack.getItem();

        if (!allowPoisoned.get() && isPoisonedFood(item)) {
            return -1;
        }

        if (!allowGapples.get() && isGapple(item)) {
            return -1;
        }

        FoodProperties food = item.getComponents().get(DataComponents.FOOD);
        float nutrition = food.comp_2491();
        float saturation = food.comp_2492();
        float totalValue = nutrition + saturation;

        if (isGapple(item)) {
            return allowGapples.get() ? totalValue - 0.5f : -1;
        }

        if (isPoisonedFood(item)) {
            return allowPoisoned.get() ? totalValue - 1.5f : -1;
        }

        return totalValue;
    }

    private boolean isGapple(Item item) {
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean isPoisonedFood(Item item) {
        return item == Items.ROTTEN_FLESH
                || item == Items.PUFFERFISH
                || item == Items.SPIDER_EYE
                || item == Items.CHORUS_FRUIT;
    }
}
