package me.kiriyaga.nami.feature.gui.newgui.entry;

import me.kiriyaga.nami.feature.gui.newgui.base.BaseEntry;
import net.minecraft.network.chat.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static me.kiriyaga.nami.Nami.CAT_FORMAT;

public class LogEntry extends BaseEntry {
    private final String message;
    private final LocalTime timestamp;

    public LogEntry(String message) {
        this.message = message;
        this.timestamp = LocalTime.now();
        displayText = formatMessage();
    }

    private Component formatMessage() {
        String time = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return CAT_FORMAT.format("[" + time + "] " + message);
    }

    @Override
    public Component getDisplayText() {
        return displayText;
    }

    @Override
    public void refreshEntry() {

    }

    public String getRawMessage() {
        return message;
    }
}
