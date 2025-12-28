package me.kiriyaga.nami.feature.module.impl.client;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render2DEvent;
import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.ColorSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.ChatAnimationHelper;
import net.minecraft.client.gui.screens.ChatScreen;

import java.awt.*;
import java.util.ArrayList;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class HudModule extends Module {

    public final BoolSetting chatAnimation = addSetting(new BoolSetting("ChatAnimation", true));
    public final BoolSetting shadow = addSetting(new BoolSetting("Shadow", true));
    public final BoolSetting bounce = addSetting(new BoolSetting("Bounce", false));
    public final IntSetting bounceSpeed = addSetting(new IntSetting("Speed", 5, 1, 20));
    public final IntSetting bounceIntensity = addSetting(new IntSetting("Intensity", 30, 10, 100));
    public final BoolSetting accent = addSetting(new BoolSetting("Accent", false));
    public final ColorSetting globalColor = addSetting(new ColorSetting("Color", new Color(170, 170, 170, 255), true));

    private float bounceProgress = 0f;
    private boolean increasing = true;

    public HudModule() {
        super("HUD", "Renders in-game hud.", ModuleCategory.of("Client"));
        bounceIntensity.setShowCondition(() -> bounce.get());
        bounceSpeed.setShowCondition(() -> bounce.get());
        globalColor.setShowCondition(accent::get);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onUpdate(PreTickEvent event) {

        if (bounce.get()) {
            float step = bounceSpeed.get() / 100f;
            if (increasing) {
                bounceProgress += step;
                if (bounceProgress >= 1f) {
                    bounceProgress = 1f;
                    increasing = false;
                }
            } else {
                bounceProgress -= step;
                if (bounceProgress <= 0f) {
                    bounceProgress = 0f;
                    increasing = true;
                }
            }
        } else {
            bounceProgress = 0f;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender2D(Render2DEvent event) {
        boolean chatOpen = MC.currentScreen instanceof ChatScreen;
        ChatAnimationHelper.setChatOpen(chatOpen);
        ChatAnimationHelper.tick();

        if (chatAnimation.get()) {
            int offset = (int) ChatAnimationHelper.getAnimationOffset();
            if (offset > 0) {
                event.getDrawContext().fill(
                        2,
                        MC.getWindow().getScaledHeight() - offset,
                        MC.getWindow().getScaledWidth() - 2,
                        MC.getWindow().getScaledHeight() - 2,
                        MC.options.getTextBackgroundColor(Integer.MIN_VALUE)
                );
            }
        }

        int screenHeight = MC.getWindow().getScaledHeight();
        int chatZoneTop = screenHeight - (screenHeight / 8);
        int chatAnimationOffset = (int) ChatAnimationHelper.getAnimationOffset();

        if (MC.getDebugHud().shouldShowDebugHud())
            return;

        for (Module module : MODULE_MANAGER.getStorage().getAll()) {
            if (module instanceof HudElementModule hudElement && hudElement.isEnabled()) {
                int baseY = hudElement.getRenderY();

                for (HudElementModule.TextElement element : new ArrayList<>(hudElement.getTextElements())) { // its better be outdated then concurrent, btw maybe some atomic impl?
                    int drawX = hudElement.getRenderXForElement(element);
                    int drawY = baseY + element.offsetY();

                    boolean isInChatZone = (drawY + MC.textRenderer.fontHeight) >= chatZoneTop;
                    if (isInChatZone) {
                        drawY -= chatAnimationOffset;
                    }

//                    event.getDrawContext().drawText(
//                            MC.textRenderer,
//                            element.text(),
//                            drawX,
//                            drawY,
//                            0xFFFFFFFF,
//                            shadow.get()
//                    );

                    FONT_MANAGER.drawText(event.getDrawContext(), element.text(), drawX, drawY, shadow.get());
                }

                hudElement.renderItems(event.getDrawContext());
            }
        }
    }

    public Color getPulsingColor(Color originalColor) {
        if (!bounce.get()) return originalColor;

        float intensity = bounceIntensity.get() / 100f;
        float pulseFactor = (float) Math.sin(bounceProgress * Math.PI);

        float darkenFactor = 1f - intensity * pulseFactor;

        int r = (int) (originalColor.getRed() * darkenFactor);
        int g = (int) (originalColor.getGreen() * darkenFactor);
        int b = (int) (originalColor.getBlue() * darkenFactor);
        int a = originalColor.getAlpha();

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new Color(r, g, b, a);
    }
}