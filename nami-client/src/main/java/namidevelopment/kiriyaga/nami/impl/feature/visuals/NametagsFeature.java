/*
Originally from:
https://github.com/NamiDevelopment/mint/blob/d8274468792503ccbfb1b374aaaeb74225a42056/src/main/java/net/melbourne/Features/impl/render/NametagsFeature.java

Licensed under MIT License
Copyright (c) 2026 Nami Development

https://github.com/NamiDevelopment/mint/blob/master/LICENSE
 */

package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render2DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.EntityUtils.getRenderPos;
import static namidevelopment.kiriyaga.api.util.render.RenderUtil.project;
import static namidevelopment.kiriyaga.api.util.render.RenderUtil.projectionVisible;

@RegisterFeature
public class NametagsFeature extends Feature {

    public final BoolSetting self = addSetting(new BoolSetting("Self", false));
    public final BoolSetting invisible = addSetting(new BoolSetting("Invisibles", true));
    public final BoolSetting dynamicScale = addSetting(new BoolSetting("DynamicScale", true));
    public final DoubleSetting scaling = addSetting(new DoubleSetting("Scaling", 1.00, 0.50, 1.50));
    public final BoolSetting gameMode = addSetting(new BoolSetting("GameMode", false));
    public final BoolSetting ping = addSetting(new BoolSetting("Ping", true));
    public final BoolSetting entityId = addSetting(new BoolSetting("EntityID", false));
    public final BoolSetting health = addSetting(new BoolSetting("Health", true));
    public final BoolSetting totemPops = addSetting(new BoolSetting("TotemPops", false));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", true));
    public final BoolSetting durability = addSetting(new BoolSetting("Durability", true));
    public final BoolSetting rectangle = addSetting(new BoolSetting("Background", true));
    public final BoolSetting items = addSetting(new BoolSetting("Items", false));
    public final BoolSetting pearls = addSetting(new BoolSetting("Pearls", true));

    //0, 0, 0, 100
    //19, 19, 19, 140

    public NametagsFeature() {
        super("Nametags", "Draws nametags above certain entities.", FeatureCategory.of("Render"));
    }

    @SubscribeEvent
    public void onRender2DEvent(Render2DEvent event) {
        if (MC.level == null) return;


        Matrix3x2fStack matrices = event.getDrawContext().pose();

        for (Entity ent : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS)) {
            if (ent == MC.player && (!self.get() || MC.options.getCameraType().isFirstPerson())) continue;

            Vec3 vec3d = getRenderPos(ent, MC.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            Vec3 projected = project(vec3d.add(0, ent.getDimensions(ent.getPose()).height() + 0.2, 0));

            if (!invisible.get() && ent.isInvisible()) continue;
            if (!ent.isAlive()) continue;
            if (!projectionVisible(projected)) continue;

            String ign = ent.getName().getString();

            String text = ign;

            if (gameMode.get()) {
                String gm = EntityUtils.getGameMode((Player) ent).getName().toUpperCase();
                text += "[" + (gm.isEmpty() ? "" : gm.substring(0, 1)) + "]";
            }

            if (ping.get())
                text += " " + EntityUtils.getLatency((Player) ent) + "ms";

            if (entityId.get())
                text += " ID:" + ent.getId();

            if (health.get())
                text += " " + EntityUtils.getHealthNumber(ent);

            if (totemPops.get()) {
                int pops = TOTEMCOUNTER_SERVICE.getPoppedTotemCount(ent.getId());

                if (pops > 0)
                    text += " -" + pops;
            }

            float width = FONT_SERVICE.getWidth(text);

            String colored = SOCIALS_SERVICE.isFriend(ign) ? "{friend}" + text : text;

            if (health.get())
                colored = colored.replace(" " + EntityUtils.getHealthNumber(ent),
                        " " + ColorUtils.getHealthColor(ent) + EntityUtils.getHealthNumber(ent));

            if (totemPops.get()) {
                int pops = TOTEMCOUNTER_SERVICE.getPoppedTotemCount(ent.getId());

                if (pops > 0)
                    colored = colored.replace(" -" + pops, " " + ColorUtils.getTotemColor(pops) + "-" + pops);
            }

            Component display = CAT_FORMAT.format(colored);
            float scale = 1.0f;

            if (dynamicScale.get()) {
                Vec3 camPos = MC.gameRenderer.getMainCamera().position();
                Vec3 entPos = vec3d;
                float dist = (float) camPos.distanceTo(entPos);
                float start = 10f;
                float end = 0.5f;
                float maxScale = 4f;

                if (dist < start) {
                    float t = (start - dist) / (start - end);
                    t = Math.max(0.0f, Math.min(1.0f, t));
                    t = (float) Math.pow(t, 4);
                    scale = 1.0f + t * (maxScale - 1.0f);
                } else {
                    scale = 1.0f;
                }
            }

            scale *= scaling.get().floatValue();

            matrices.pushMatrix();
            matrices.translate((float) projected.x, (float) projected.y);
            matrices.scale(scale, scale);

            if (rectangle.get()) {
                GuiGraphics ctx = event.getDrawContext();
                int x1 = (int) (-width / 2f - 1);
                int y1 = (int) (-FONT_SERVICE.getHeight());
                int x2 = (int) (width / 2 + 2);
                int y2 = 0;
                ctx.fill(x1, y1, x2, y2, 0x64000000);
                int color = (140 << 24) | (19 << 16) | (19 << 8) | 19;
                int thick = 1;
                ctx.fill(x1 + 1, y1, x2 - 1, y1 + thick, color);
                ctx.fill(x1 + 1, y2 - thick, x2 - 1, y2, color);
                ctx.fill(x1, y1, x1 + thick, y2, color);
                ctx.fill(x2 - thick, y1, x2, y2, color);
            }

            FONT_SERVICE.drawText(event.getDrawContext(), display, (int) (-FONT_SERVICE.getWidth(text) / 2.f), -FONT_SERVICE.getHeight(), true);

            if (armor.get()) {
                List<ItemStack> stacks = new ArrayList<>();
                ItemStack[] all = new ItemStack[]{
                        ((Player) ent).getItemBySlot(EquipmentSlot.MAINHAND),
                        ((Player) ent).getItemBySlot(EquipmentSlot.FEET),
                        ((Player) ent).getItemBySlot(EquipmentSlot.LEGS),
                        ((Player) ent).getItemBySlot(EquipmentSlot.CHEST),
                        ((Player) ent).getItemBySlot(EquipmentSlot.HEAD),
                        ((Player) ent).getItemBySlot(EquipmentSlot.OFFHAND)
                };

                for (ItemStack stack : all) {
                    if (!stack.isEmpty()) stacks.add(stack);
                }

                if (!stacks.isEmpty()) {
                    int totalWidth = (stacks.size() * 16) + ((stacks.size() - 1) * 2);
                    int x = -totalWidth / 2;
                    int y = -30;

                    for (int i = stacks.size() - 1; i >= 0; i--) {
                        ItemStack stack = stacks.get(i);
                        event.getDrawContext().renderItem(stack, x, y);
                        event.getDrawContext().renderItemDecorations(FONT_SERVICE.rendererProvider.getRenderer(), stack, x, y);

                        if (durability.get()) {
                            int damage = stack.getDamageValue();
                            int maxDamage = stack.getMaxDamage();

                            if (maxDamage > 0) {
                                event.getDrawContext().pose().pushMatrix();
                                event.getDrawContext().pose().translate(x + 8 - (FONT_SERVICE.getWidth((((maxDamage - damage) * 100) / maxDamage) + "%") * 0.75f) / 2.0F, y - (6 * 0.75f));
                                event.getDrawContext().pose().pushMatrix();
                                event.getDrawContext().pose().scale(0.75f, 0.75f);
                                float ratio = (maxDamage - damage) / (float) maxDamage;
                                int color = new Color(1.0f - ratio, ratio, 0).getRGB();
                                FONT_SERVICE.drawText(event.getDrawContext(), (((maxDamage - damage) * 100) / maxDamage) + "%", 0, 0, true, color);
                                event.getDrawContext().pose().popMatrix();
                                event.getDrawContext().pose().popMatrix();
                            }
                        }
                        x += 16 + 2;
                    }
                }
            }
            matrices.popMatrix();
        }

        if (items.get()) {
            for (Entity enttt : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.DROPPED_ITEMS)) {
                Vec3 pos = getRenderPos(enttt, MC.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                pos = pos.add(0, 0.2, 0);
                Vec3 proj = project(pos);
                if (!projectionVisible(proj)) continue;

                if (enttt instanceof ItemEntity item) {
                    ItemStack stack = item.getItem();
                    if (stack.isEmpty()) continue;

                    String name = stack.getItemName().getString();
                    int count = stack.getCount();
                    String display = name + (count > 1 ? " x" + count : "");

                    float scale = 1.0f;
                    if (dynamicScale.get()) {
                        Vec3 camPos = MC.gameRenderer.getMainCamera().position();
                        Vec3 entPos = pos;
                        float dist = (float) camPos.distanceTo(entPos);
                        float start = 10f;
                        float end = 0.5f;
                        float maxScale = 4f;

                        if (dist < start) {
                            float t = (start - dist) / (start - end);
                            t = Math.max(0.0f, Math.min(1.0f, t));
                            t = (float) Math.pow(t, 4);
                            scale = 1.0f + t * (maxScale - 1.0f);
                        } else {
                            scale = 1.0f;
                        }
                    }

                    scale = scale * scaling.get().floatValue();

                    matrices.pushMatrix();
                    matrices.translate((float) proj.x, (float) proj.y);
                    matrices.scale(scale, scale);
                    FONT_SERVICE.drawText(event.getDrawContext(), display, -FONT_SERVICE.getWidth(display) / 2, -FONT_SERVICE.getHeight(), true, Color.white.getRGB());
                    matrices.popMatrix();
                }
            }
        }

        if (pearls.get()) {
            for (Entity entt : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
                if (entt instanceof ThrownEnderpearl pearl) {
                    Vec3 pos = getRenderPos(pearl, MC.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                    Vec3 proj = project(pos.add(0, 0.25, 0));

                    if (!projectionVisible(proj)) continue;

                    if (pearl.getOwner() instanceof Player thrower) {
                        String display = thrower.getName().getString();

                        float scale = 1.0f;
                        if (dynamicScale.get()) {
                            Vec3 camPos = MC.gameRenderer.getMainCamera().position();
                            Vec3 entPos = pos;
                            float dist = (float) camPos.distanceTo(entPos);
                            float start = 10f;
                            float end = 0.5f;
                            float maxScale = 4f;

                            if (dist < start) {
                                float t = (start - dist) / (start - end);
                                t = Math.max(0.0f, Math.min(1.0f, t));
                                t = (float) Math.pow(t, 4);
                                scale = 1.0f + t * (maxScale - 1.0f);
                            } else {
                                scale = 1.0f;
                            }
                        }

                        matrices.pushMatrix();
                        matrices.translate((float) proj.x, (float) proj.y);
                        matrices.scale(scale, scale);
                        FONT_SERVICE.drawText(event.getDrawContext(), display, -FONT_SERVICE.getWidth(display) / 2, -FONT_SERVICE.getHeight(), true, Color.white.getRGB());
                        matrices.popMatrix();
                    }
                }
            }
        }
    }
}
