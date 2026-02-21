package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.api.util.entity.PlayerUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isItemAWeapon;

@RegisterFeature
public class AutoTotemFeature extends Feature {

    private enum Offhand { CRYSTAL, GAPPLE, ITEMFRAME, MENDING, TOTEM}

    public final IntSetting health = addSetting(new IntSetting("Health", 12, 2, 36));
    public final BoolSetting offhandOverride = addSetting(new BoolSetting("Override", false));
    public final EnumSetting<Offhand> overrideItem = addSetting(new EnumSetting<>("Item", Offhand.TOTEM));
    public final BoolSetting swordGap = addSetting(new BoolSetting("SwordGap", true));
    public final BoolSetting fastSwap = addSetting(new BoolSetting("Alternative", true));
    public final BoolSetting mainhand = addSetting(new BoolSetting("Mainhand", false));
    public final BoolSetting mainhandGapple = addSetting(new BoolSetting("MainhandGapple", false));
    public final IntSetting mainhandSlot = addSetting(new IntSetting("Slot", 8, 0, 8));
    public final BoolSetting deathLog = addSetting(new BoolSetting("Log", false));

    private final Map<String, String> deathReasons = new ConcurrentHashMap<>();

    private long lastAttemptTime = 0;
    private int totemCount = 0;

    public boolean mainhandActive = false;

    public AutoTotemFeature() {
        super("AutoTotem", "Automatically places totem in your hand.", FeatureCategory.of("Combat"), "autototem");
        mainhandSlot.setShowCondition(mainhand::get);
        overrideItem.setShowCondition(offhandOverride::get);
        swordGap.setShowCondition(offhandOverride::get);
        mainhandGapple.setShowCondition(mainhand::get);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTick(PreTickEvent event) {
        if (MC.level == null || MC.player == null) return;
        this.clearDisplayInfo();
        mainhandActive = false;

        int totemCount = PlayerUtils.getTotemCount();

        this.addDisplayInfo(String.valueOf(totemCount));

        attemptPlaceOffhand();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketReceiveEvent event) {
        if (MC.level == null || MC.player == null) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEntity(MC.level) == MC.player && packet.getEventId() == 3 && deathLog.get()) {
                MC.execute(this::logDeathData);
            }
        }
    }

    private void attemptPlaceOffhand() {
        LocalPlayer player = MC.player;
        if (player == null) return;

        ItemStack offhandStack = player.getOffhandItem();
        ItemStack targetStack = null;

        if (offhandOverride.get()) {
            int effectiveHealth = (int) (player.getHealth() + player.getAbsorptionAmount());
            if (effectiveHealth >= health.get()) {
                targetStack = getOverrideStack();
            }
        }

        if (targetStack == null) {
            targetStack = findTotemStack();
            if (targetStack == null) return;
        }

        boolean lowHp = player.getHealth() + player.getAbsorptionAmount() <= health.get();
        if (mainhand.get() && lowHp) {
            mainhandActive = true;

            ItemStack mhStack = player.getInventory().getItem(mainhandSlot.get());

            if (mhStack.getItem() != Items.TOTEM_OF_UNDYING && offhandStack.getItem() == Items.TOTEM_OF_UNDYING) {
                int totemSlot = findInventorySlot(new ItemStack(Items.TOTEM_OF_UNDYING), mainhandSlot.get());
                if (totemSlot != -1) {
                    if (fastSwap.get()) {
                        INVENTORY_SERVICE.getClickHandler().swapSlot(convertSlot(totemSlot), mainhandSlot.get());
                        lastAttemptTime = System.currentTimeMillis();
                    } else {
                        clickSlot(totemSlot, convertSlot(mainhandSlot.get()));
                        lastAttemptTime = System.currentTimeMillis();
                    }
                }
            }

            boolean useGapple = mainhandGapple.get() && MC.options.keyUse.isDown();
            if (useGapple) {
                int gappleSlot = findInventorySlot(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), -1);
                if (gappleSlot == -1) gappleSlot = findInventorySlot(new ItemStack(Items.GOLDEN_APPLE), -1);

                if (gappleSlot != -1 && gappleSlot < 9) {
                    INVENTORY_SERVICE.getSwapHandler().attemptSwitch(gappleSlot, false);
                } else {
                    INVENTORY_SERVICE.getSwapHandler().attemptSwitch(mainhandSlot.get(), false);
                }
            } else {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(mainhandSlot.get(), false);
            }
        }

        int targetSlot;
        if (mainhandActive) {
            targetSlot = findInventorySlot(targetStack, mainhandSlot.get());
        } else {
            targetSlot = findInventorySlot(targetStack);
        }

        if (targetSlot == -1) return;
        if (offhandStack.getItem() == targetStack.getItem()) return;

        if (fastSwap.get()) {
            INVENTORY_SERVICE.getClickHandler().swapSlot(convertSlot(targetSlot), 40);
            lastAttemptTime = System.currentTimeMillis();
        } else {
            clickSlot(targetSlot, 45);
            lastAttemptTime = System.currentTimeMillis();
        }

        totemCount = countTotems();
        addDisplayInfo("" + totemCount);
    }

    private ItemStack findTotemStack() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) return stack;
        }
        return null;
    }

    private ItemStack getOverrideStack() {
        Offhand type = overrideItem.get();
        LocalPlayer player = MC.player;

        if (swordGap.get() && MC.player.getHealth() + MC.player.getAbsorptionAmount() >= health.get() && isItemAWeapon(MC.player.getInventory().getSelectedItem()) && MC.options.keyUse.isDown())
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);

        switch (type) {
            case TOTEM:
                return new ItemStack(Items.TOTEM_OF_UNDYING);
            case CRYSTAL:
                return new ItemStack(Items.END_CRYSTAL);
            case GAPPLE:
                return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case ITEMFRAME:
                return new ItemStack(Items.ITEM_FRAME);
            case MENDING:
                if (hasMending(MC.player.getOffhandItem()) && !isFullyRepaired(MC.player.getOffhandItem())) return null;

                for (int i = 0; i < 36; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) continue;

                    if (stack.is(ItemTags.HEAD_ARMOR)) continue;
                    if (stack.is(ItemTags.CHEST_ARMOR)) continue;
                    if (stack.is(ItemTags.LEG_ARMOR)) continue;
                    if (stack.is(ItemTags.FOOT_ARMOR)) continue;

                    if (!hasMending(stack)) continue;
                    if (isFullyRepaired(stack)) continue;

                    return stack;
                }
                break;
        }
        return null;
    }

    private void clickSlot(int invSlot, int index) {
        int realSlot = convertSlot(invSlot);

        ItemStack cursor = MC.player.containerMenu.getCarried();

        if (cursor.isEmpty()) {
            INVENTORY_SERVICE.getClickHandler().pickupSlot(realSlot);
            cursor = MC.player.containerMenu.getCarried();
        }

        if (!cursor.isEmpty()) {
            INVENTORY_SERVICE.getClickHandler().pickupSlot(index);
        }
    }

    private int countTotems() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack != null && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int convertSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public void addDeathReason(String key, String reasonDescription) {
        deathReasons.put(key, reasonDescription);
    }

    public void removeDeathReason(String key) {
        deathReasons.remove(key);
    }

    public void clearDeathReasons() {
        deathReasons.clear();
    }

    private void logDeathData() {
        LocalPlayer player = MC.player;
        if (player == null) return;

        int ping = SERVER_SERVICE.getPing();
        boolean hasTotem = totemCount > 0;
        long timeSinceLastSwap = System.currentTimeMillis() - lastAttemptTime;

        if (!hasTotem) {
            addDeathReason("notots", "NO_TOTEMS");
        } else {
            removeDeathReason("notots");
        }

        if (ping > 125) {
            addDeathReason("highping", "HIGH_PING " + ping + " ms");
        } else {
            removeDeathReason("highping");
        }

        if (deathReasons.isEmpty()) {
            addDeathReason("unknown", "UNKNOWN_CAUSE");
        } else {
            removeDeathReason("unknown");
        }

        StringBuilder reasonsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : deathReasons.entrySet()) {
            reasonsBuilder.append("- ").append(entry.getValue()).append("\n");
        }

        boolean pendingTotem = false;
        Component message = CAT_FORMAT.format(
                "\n{gray}=== {global}AutoTotem{gray} ===\n" +
                        "Death reasons:\n{global}" + reasonsBuilder.toString() + "{gray}\n" +
                        "Ping: {global}" + ping + " ms{gray}\n" +
                        "Totems Available: {global}" + totemCount + "{gray}\n" +
                        "Pending Totem: {global}" + pendingTotem + "{gray}\n" +
                        "Last Swap Attempt: {global}" + timeSinceLastSwap + " ms ago{gray}\n" +
                        "============================"
        );

        CHAT_SERVICE.sendPersistent(AutoTotemFeature.class.getName(), message);
    }

    private int findInventorySlot(ItemStack stack) {
        for (int i = 0; i < 36; i++) {
            if (MC.player.getInventory().getItem(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(ItemStack stack, int excluded) {
        for (int i = 0; i < 36; i++) {
            if (i == excluded) continue;
            if (MC.player.getInventory().getItem(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasMending(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0;
    }

    private boolean isFullyRepaired(ItemStack stack) {
        if (!stack.isDamageableItem()) return true;
        return stack.getDamageValue() == 0;
    }
}