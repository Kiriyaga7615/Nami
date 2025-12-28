package me.kiriyaga.nami.feature.gui.newgui.component;

import me.kiriyaga.nami.feature.gui.newgui.base.BaseEntry;
import me.kiriyaga.nami.feature.gui.newgui.base.DataPanel;
import me.kiriyaga.nami.feature.gui.newgui.entry.FriendEntry;
import me.kiriyaga.nami.feature.gui.newgui.widget.ButtonWidget;
import me.kiriyaga.nami.feature.gui.newgui.widget.TextBoxWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Function;

public class ConsolePanelComponent<T extends BaseEntry> extends DataPanel<T> {
    private final Consumer<T> onAdd;
    private final Consumer<T> onRemove;
    private final Consumer<T> onClick;

    private final ButtonWidget enterButton;
    private final TextBoxWidget inputBox;
    private final Function<String, T> entryFactory;

    public ConsolePanelComponent(String name, int x, int y, int width, int height,
                                 Consumer<T> onAdd, Consumer<T> onRemove, Consumer<T> onClick,
                                 Function<String, T> entryFactory) {
        super(name, x, y, width, height, BaseEntry::getDisplayText);
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.onClick = onClick;
        this.entryFactory = entryFactory;

        int inputY = y + height - inputHeight - 2;
        int inputX = x + 2;
        int inputWidth = width - 2 - 50 - 4;

        this.inputBox = new TextBoxWidget(inputX, inputY, inputWidth, inputHeight);
        this.enterButton = new ButtonWidget("Enter",
                x + width - 2 - 50,
                inputY,
                50,
                inputHeight,
                true,
                this::submitInput);

        this.inputBox.setOnEnter(this::submitInput);
    }

    private void submitInput() {
        String text = inputBox.getText().trim();
        if (!text.isEmpty() && onAdd != null && entryFactory != null) {
            T entry = entryFactory.apply(text);
            onAdd.accept(entry);
            inputBox.clear();
        }
    }

    protected T createEntryFromText(String text) {
        return null;
    }

    public void addEntry(T entry) {
        if (entry != null) {
            getEntries().add(entry);
        }

        while (getEntries().size() > 250) {
            getEntries().removeFirst();
        }
    }

    @Override
    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        super.render(context, textRenderer, mouseX, mouseY);

        inputBox.setPosition(x + 2, y + height - inputHeight - 2);
        enterButton.setPosition(x + width - 2 - enterButton.getWidth(), y + height - inputHeight - 2);

        inputBox.render(context, textRenderer, mouseX, mouseY);
        enterButton.render(context, textRenderer, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (enterButton.mouseClicked((int) mouseX, (int) mouseY, button)) return true;
        if (inputBox.mouseClicked((int) mouseX, (int) mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (inputBox.charTyped(chr, modifiers)) return true;
        return false;
    }
}
