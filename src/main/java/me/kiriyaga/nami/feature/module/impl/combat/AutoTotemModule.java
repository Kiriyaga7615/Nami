package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.core.executable.model.ExecutableThreadType;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.EnchantmentUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AutoTotemModule extends Module {

    private enum Offhand { CRYSTAL, GAPPLE, ITEMFRAME, MENDING}

    private final IntSetting health = addSetting(new IntSetting("Health", 12, 10, 36));
    private final BoolSetting offhandOverride = addSetting(new BoolSetting("Override", false));
    private final EnumSetting<Offhand> overrideItem = addSetting(new EnumSetting<>("Item", Offhand.CRYSTAL));
    private final BoolSetting fastSwap = addSetting(new BoolSetting("Alternative", false));
    private final BoolSetting mainhand = addSetting(new BoolSetting("Mainhand", false));
    private final BoolSetting mainhandGapple = addSetting(new BoolSetting("MainhandGapple", false));
    private final IntSetting mainhandSlot = addSetting(new IntSetting("Slot", 8, 0, 8));
    private final BoolSetting deathLog = addSetting(new BoolSetting("Log", false));

    private final Map<String, String> deathReasons = new ConcurrentHashMap<>();

    private boolean pendingTotem = false;
    private long lastAttemptTime = 0;
    private int totemCount = 0;

    public AutoTotemModule() {
        super("AutoTotem", "Automatically places totem in your hand.", ModuleCategory.of("Combat"), "autototem");
        mainhandSlot.setShowCondition(mainhand::get);
        overrideItem.setShowCondition(offhandOverride::get);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.world == null || MC.player == null) return;

        int totemCount = 0;
        for (ItemStack stack : MC.player.getInventory().getMainStacks()) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }
        ItemStack offHandStack = MC.player.getOffHandStack();
        if (offHandStack.getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += offHandStack.getCount();
        }
        this.setDisplayInfo(String.valueOf(totemCount));

        attemptPlaceOffhand();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketReceiveEvent event) {
        if (MC.world == null || MC.player == null) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEntity(MC.world) == MC.player && packet.getStatus() == 3 && deathLog.get()) {
                EXECUTABLE_MANAGER.getRequestHandler().submit(this::logDeathData, 20, ExecutableThreadType.PRE_TICK);
            }
        }
    }

    private void attemptPlaceOffhand() {
        LocalPlayer player = MC.player;
        if (player == null) return;

        ItemStack offhandStack = player.getOffHandStack();
        ItemStack targetStack = null;
        boolean overrideActive = false;

        if (offhandOverride.get()) {
            int effectiveHealth = (int) (player.getHealth() + player.getAbsorptionAmount());
            if (effectiveHealth >= health.get()) {
                targetStack = getOverrideStack();
                if (targetStack != null) {
                    overrideActive = true;
                }
            }
        }

        if (targetStack == null) {
            targetStack = findTotemStack();
            if (targetStack == null) return;
        }

        if (mainhand.get()) {
            boolean useGapple = mainhandGapple.get() && MC.options.useKey.isPressed();

            if (useGapple) {
                int gappleSlot = findInventorySlot(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), mainhandSlot.get());
                if (gappleSlot == -1) {
                    gappleSlot = findInventorySlot(new ItemStack(Items.GOLDEN_APPLE), mainhandSlot.get());
                }

                if (gappleSlot != -1 && MC.player.getInventory().getStack(mainhandSlot.get()).getItem() != Items.ENCHANTED_GOLDEN_APPLE
                        && MC.player.getInventory().getStack(mainhandSlot.get()).getItem() != Items.GOLDEN_APPLE) {
                    if (fastSwap.get()) {
                        INVENTORY_MANAGER.getClickHandler().swapSlot(convertSlot(gappleSlot), mainhandSlot.get());
                        lastAttemptTime = System.currentTimeMillis();
                    } else {
                        clickSlot(gappleSlot, convertSlot(mainhandSlot.get()));
                        lastAttemptTime = System.currentTimeMillis();
                    }
                }
            } else {
                if (MC.player.getInventory().getStack(mainhandSlot.get()).getItem() != Items.TOTEM_OF_UNDYING) {
                    int totem = findInventorySlot(new ItemStack(Items.TOTEM_OF_UNDYING), mainhandSlot.get());
                    if (totem != -1) {
                        if (fastSwap.get()) {
                            INVENTORY_MANAGER.getClickHandler().swapSlot(convertSlot(totem), mainhandSlot.get());
                            lastAttemptTime = System.currentTimeMillis();
                        } else {
                            clickSlot(totem, convertSlot(mainhandSlot.get()));
                            lastAttemptTime = System.currentTimeMillis();
                        }
                    }
                }
            }


            if (MC.player.getHealth() + MC.player.getAbsorptionAmount() <= health.get() && MC.player.getInventory().getStack(mainhandSlot.get()).getItem() == Items.TOTEM_OF_UNDYING)
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(mainhandSlot.get());
        }

        int targetSlot;

        if (mainhand.get())
            targetSlot = findInventorySlot(targetStack, mainhandSlot.get());
        else
            targetSlot = findInventorySlot(targetStack);

        if (targetSlot == -1) return;

        if (offhandStack.getItem() == targetStack.getItem()) return;

        if (fastSwap.get()) {
            if (offhandStack.getItem() != targetStack.getItem()) {
                INVENTORY_MANAGER.getClickHandler().swapSlot(convertSlot(targetSlot), 40);
                lastAttemptTime = System.currentTimeMillis();
            }
        } else {
            clickSlot(targetSlot, 45);
            lastAttemptTime = System.currentTimeMillis();
        }

        totemCount = countTotems();
        setDisplayInfo("" + totemCount);
    }

    private ItemStack findTotemStack() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) return stack;
        }
        return null;
    }

    private ItemStack getOverrideStack() {
        Offhand type = overrideItem.get();
        LocalPlayer player = MC.player;

        switch (type) {
            case CRYSTAL:
                return new ItemStack(Items.END_CRYSTAL);
            case GAPPLE:
                return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case ITEMFRAME:
                return new ItemStack(Items.ITEM_FRAME);
            case MENDING:
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (stack.isEmpty()) continue;

                    if (stack.isIn(ItemTags.HEAD_ARMOR)) continue;
                    if (stack.isIn(ItemTags.CHEST_ARMOR)) continue;
                    if (stack.isIn(ItemTags.LEG_ARMOR)) continue;
                    if (stack.isIn(ItemTags.FOOT_ARMOR)) continue;

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

        ItemStack cursor = MC.player.currentScreenHandler.getCursorStack();

        if (cursor.isEmpty()) {
            INVENTORY_MANAGER.getClickHandler().pickupSlot(realSlot);
            cursor = MC.player.currentScreenHandler.getCursorStack();
        }

        if (!cursor.isEmpty()) {
            INVENTORY_MANAGER.getClickHandler().pickupSlot(index);
        }
    }

    private int countTotems() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
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

        int ping = SERVER_MANAGER.getPing();
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

        Component message = CAT_FORMAT.format(
                "\n=== {g}AutoTotem{reset} ===\n" +
                        "Death reasons:\n{g}" + reasonsBuilder.toString() + "{reset}\n" +
                        "Ping: {g}" + ping + " ms{reset}\n" +
                        "Totems Available: {g}" + totemCount + "{reset}\n" +
                        "Pending Totem: {g}" + pendingTotem + "{reset}\n" +
                        "Last Swap Attempt: {g}" + timeSinceLastSwap + " ms ago{reset}\n" +
                        "============================"
        );

        LOG.addEntry(this.name + ": " + message);

        CHAT_MANAGER.sendPersistent(AutoTotemModule.class.getName(), message);
    }

    private int findInventorySlot(ItemStack stack) {
        for (int i = 0; i < 36; i++) {
            if (MC.player.getInventory().getStack(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(ItemStack stack, int excluded) {
        for (int i = 0; i < 36; i++) {
            if (i == excluded) continue;
            if (MC.player.getInventory().getStack(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }


    private boolean hasMending(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0;
    }

    private boolean isFullyRepaired(ItemStack stack) {
        if (!stack.isDamageable()) return true;
        return stack.getDamage() == 0;
    }
}