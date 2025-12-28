package me.kiriyaga.nami.util.container;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

import static me.kiriyaga.nami.Nami.MC;
import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;

// TODO: unblock actions, and change the slot count, so people can see shulkers and manage inventory
public class ContainerScreen extends ShulkerBoxScreen {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
    private final ItemStack[] contents;
    private final ItemStack containerStack;
    private static final Deque<Screen> screenStack = new ArrayDeque<>();

    public ContainerScreen(ItemStack containerStack, ItemStack[] contents) {
        super(new ShulkerBoxMenu(0, MC.player.getInventory(), new SimpleContainer(contents)),
                MC.player.getInventory(),
                Component.translatable(containerStack.getItemName().getString()));
        this.containerStack = containerStack;
        this.contents = contents;
    }

    @Override
    protected void drawBackground(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    public static void open(ItemStack stack, ItemStack[] contents) {
        Screen current = MC.currentScreen;
        if (current != null && !(current instanceof ContainerScreen)) {
            screenStack.push(current);
        }

        MC.setScreen(new ContainerScreen(stack, contents));
    }

    @Override
    public void close() {
        if (!screenStack.isEmpty()) {
            MC.setScreen(screenStack.pop());
        } else
            super.close();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        if (click.button() == 1 || click.button() == 0)
             return false;

        super.mouseClicked(click, bl);
        return true;
    }

     @Override
     public boolean keyPressed(KeyEvent keyInput) {
         KeyMapping keyBindEscape = MC.options.inventoryKey;
         int escKey = keyBindEscape.getDefaultKey().getCode();

         int keyCode = keyInput.getKeycode();
         int scanCode = keyInput.comp_4796();
         int modifiers = keyInput.comp_4797();

         if (keyCode == escKey || keyCode == GLFW.GLFW_KEY_ESCAPE) {
             return super.keyPressed(keyInput);
         }

         return true;
     }
}
