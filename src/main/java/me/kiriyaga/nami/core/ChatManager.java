package me.kiriyaga.nami.core;

import me.kiriyaga.nami.core.executable.model.ExecutableThreadType;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.mixin.ChatHudAccessor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;

import java.security.SecureRandom;
import java.util.*;

import static me.kiriyaga.nami.Nami.*;

public class ChatManager {

    /**
     * Usage:
     * .sendRaw() just sends raw message without tracking
     *
     * .sendPersistent() non timed/replaced by default message, but is tracked and we can delete it by its key
     * Why? Because we want to track session joins/leaves, for example, and clean "outdated" info, but without
     * affecting other messages. Can be replaced by key.
     *
     * .removePersistent() all by its name
     *
     * .sendTransient() transient message, that will be deleted and replaced while new transient message appears     *
     *
     * .clear() clears all messages (contained by us btw)
     *
     */

    public final Map<String, MessageSignature> persistentMessages = new HashMap<>();
    public MessageSignature transientSignature = null;

    private final List<Component> allMessages = new ArrayList<>(); // theese are always key == null

    public void init(){
        EVENT_MANAGER.register(this);
    }

    private MessageSignature generateSignature() {
        byte[] data = new byte[256];
        new SecureRandom().nextBytes(data);
        return new MessageSignature(data);
    }

    public void sendRaw(String message) {
        sendRaw(Component.literal(message), true);
    }

    public void sendRaw(String message, boolean prefix) {
        sendRaw(Component.literal(message), prefix);
    }

    public void sendRaw(Component message) {
        sendRaw(message, true);
    }

    public void sendRaw(Component message, boolean prefix) {
        retry(() -> {
            if (MC == null || MC.inGameHud == null || getChatHud() == null) return;
            Component text = prefix ? prefix().copy().append(message) : message;
            getChatHud().addMessage(text);
        });
    }


    public void sendPersistent(String key, String message) {
        sendPersistent(key, Component.literal(message), true);
    }

    public void sendPersistent(String key, String message, boolean prefix) {
        sendPersistent(key, Component.literal(message), prefix);
    }

    public void sendPersistent(String key, Component message) {
        sendPersistent(key, message, true);
    }

    public void sendPersistent(String key, Component message, boolean prefix) {
        retry(() -> {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

            ChatComponent chatHud = getChatHud();

        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
        }

            Component text = prefix ? prefix().copy().append(message) : message;
            MessageSignature signature = generateSignature();
            GuiMessageTag indicator = indicator();

        chatHud.addMessage(text, signature, indicator);
        persistentMessages.put(key, signature);
        });
    }

    public void sendTransient(String message) {
        sendTransient(Component.literal(message), true);
    }

    public void sendTransient(String message, boolean prefix) {
        sendTransient(Component.literal(message), prefix);
    }

    public void sendTransient(Component message) {
        sendTransient(message, true);
    }

    public void sendTransient(Component message, boolean prefix) {
        retry(() -> {
            if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

            ChatComponent chatHud = getChatHud();

        if (transientSignature != null) {
            removeSilently(transientSignature);
            transientSignature = null;
        }

            Component text = prefix ? prefix().copy().append(message) : message;
            MessageSignature signature = generateSignature();
            GuiMessageTag indicator = indicator();

        chatHud.addMessage(text, signature, indicator);
        transientSignature = signature;
        });
    }

    public void removePersistent(String key) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
            persistentMessages.remove(key);
        }
    }

    public void clear() {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatComponent chatHud = getChatHud();
        for (MessageSignature sig : persistentMessages.values()) {
            chatHud.removeMessage(sig);
            removeSilently(sig);
        }
        persistentMessages.clear();

        if (transientSignature != null) {
            removeSilently(transientSignature);
            transientSignature = null;
        }

        allMessages.clear();
    }

    private ChatComponent getChatHud() {
        return MC.inGameHud.getChatHud();
    }

    public void removeSilently(MessageSignature signature) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatComponent hud = MC.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        accessor.getMessages().removeIf(line -> signature.equals(line.comp_915()));

        accessor.getVisibleMessages().removeIf(visible -> {
            for (GuiMessage line : accessor.getMessages()) {
                if (signature.equals(line.comp_915())) {
                    return visible.comp_896().equals(line.comp_893());
                }
            }
            return false;
        });
        accessor.callRefresh();

        allMessages.remove(signature);
    }

    private GuiMessageTag indicator() {
        int global = MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledGlobalColor().getRGB() & 0x00FFFFFF;
        return new GuiMessageTag(global, null, Component.literal(DISPLAY_NAME), NAME);
    }

    private Component prefix() {
        return CAT_FORMAT.format("{s}[{g}" + NAME + "{s}] {reset}");
    }

    public void removeByText(String text) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatComponent hud = getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        accessor.getMessages().removeIf(line -> line.comp_893().getString().equals(text));

        accessor.getVisibleMessages().removeIf(visible -> visible.comp_896().toString().equals(text));

        allMessages.removeIf(t -> t.getString().equals(text));
    }

    private void retry(Runnable task) {
        boolean b =
                MC == null || MC.world == null || MC.player == null || MC.player.isDead() || MC.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen;

        if (b) {
            EXECUTABLE_MANAGER.getRequestHandler().submit(() -> retry(task), 1, ExecutableThreadType.PRE_TICK);
            return;
        }
        task.run();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent ev) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatComponent hud = getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        allMessages.clear();
        for (GuiMessage line : accessor.getMessages()) {
            Component text = line.comp_893();

            if (persistentMessages.containsValue(line.comp_915())) continue;
            if (transientSignature != null && transientSignature.equals(line.comp_915())) continue;

            allMessages.add(text);
        }
    }

    public List<Component> getAllMessages() {
        return allMessages;
    }
}