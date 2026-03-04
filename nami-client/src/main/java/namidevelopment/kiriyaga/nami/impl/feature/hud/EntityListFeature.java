package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class EntityListFeature extends HudElementFeature {

    public enum SortMode {ALPHABETICAL, DESCENDING, ASCENDING}

    private final List<TextElement> elements = new ArrayList<>();
    private long lastUpdateTime = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private final long updateIntervalMs = 3000;

    private final WhitelistSetting whitelist = addSetting(new WhitelistSetting("Whitelist", false, WhitelistSetting.Type.ENTITY));
    public final EnumSetting<SortMode> sortMode = addSetting(new EnumSetting<>("Sort", SortMode.DESCENDING));
    public final BoolSetting onlyLiving = addSetting(new BoolSetting("OnlyLiving", false));

    public EntityListFeature() {
        super("EntityList", "Shows nearby entities", 0, 0, 50, 10);
    }

    @SubscribeEvent
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.level == null || MC.player == null)
            return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime < updateIntervalMs)
            return;

        lastUpdateTime = currentTime;

        elements.clear();
        Map<String, Integer> entityCounts = new HashMap<>();

        for (Entity entity : EntityUtils.getAllEntities()) {
            if (entity == MC.player) continue;
            if (onlyLiving.get() && !(entity instanceof LivingEntity)) continue;

            if (whitelist.get()) {
                Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (entityId == null || !whitelist.contains(entityId.toString())) {
                    continue;
                }
            }

            String name = entity.getName().getString();
            entityCounts.put(name, entityCounts.getOrDefault(name, 0) + 1);
        }

        List<String> sortedNames = new ArrayList<>(entityCounts.keySet());

        switch (sortMode.get()) {
            case ALPHABETICAL -> sortedNames.sort(String::compareToIgnoreCase);
            case DESCENDING -> sortedNames.sort((a, b) -> Integer.compare(getTextWidth(b, entityCounts.get(b)), getTextWidth(a, entityCounts.get(a))));
            case ASCENDING -> sortedNames.sort((a, b) -> Integer.compare(getTextWidth(a, entityCounts.get(a)), getTextWidth(b, entityCounts.get(b))));
        }

        int yOffset = 0;
        int maxWidth = 0;

        for (String name : sortedNames) {
            int count = entityCounts.get(name);
            Component text = CAT_FORMAT.format("{global}" + name + (count > 1 ? " {secondary}(x" + count + ")" : ""));

            int textWidth = FONT_SERVICE.getWidth(text);
            elements.add(new TextElement(text, 0, yOffset));
            maxWidth = Math.max(maxWidth, textWidth);
            yOffset += FONT_SERVICE.getHeight();
        }

        this.width = maxWidth;
        this.height = yOffset;
    }

    @Override
    public List<TextElement> getTextElements() {
        if (MC.level == null || MC.player == null) {
            elements.clear();
            return elements;
        }
        return elements;
    }

    private int getTextWidth(String name, int count) {
        Component text = CAT_FORMAT.format("{global}" + name + (count > 1 ? " {secondary}(x" + count + ")" : ""));
        return FONT_SERVICE.getWidth(text);
    }
}
