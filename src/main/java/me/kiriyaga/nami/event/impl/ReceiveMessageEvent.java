package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public class ReceiveMessageEvent extends Event {

    private final Component message;
    private final MessageSignature signatureData;
    private final GuiMessageTag indicator;

    public ReceiveMessageEvent(Component message, MessageSignature signatureData, GuiMessageTag indicator) {
        this.message = message;
        this.signatureData = signatureData;
        this.indicator = indicator;
    }

    public Component getMessage() {
        return message;
    }

    public MessageSignature getSignatureData() {
        return signatureData;
    }

    public GuiMessageTag getIndicator() {
        return indicator;
    }
}
