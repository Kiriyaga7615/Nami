package me.kiriyaga.nami.feature.setting.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import static me.kiriyaga.nami.Nami.*;

public class WhitelistSetting extends BoolSetting {
    private final Set<Identifier> whitelist = new HashSet<>();
    public enum Type { ANY, BLOCK, ITEM, ENTITY, SOUND, PARTICLE }

    private final Set<Type> allowedTypes = new HashSet<>();

    public WhitelistSetting(String name, boolean defaultValue) {
        super(name, defaultValue);

        this.allowedTypes.add(Type.ANY);

//        try {
//            if (COMMAND_MANAGER.getStorage().getCommandByNameOrAlias(this.moduleName) == null) {
//                COMMAND_MANAGER.addCommand(new WhitelistCommand(moduleName));
//            }
//        } catch (Exception ignored) {}
    }

    public WhitelistSetting(String name, boolean defaultValue, Type... types) {
        this(name, defaultValue);
        this.allowedTypes.clear();
        if (types == null || types.length == 0) {
            this.allowedTypes.add(Type.ANY);
        } else {
            this.allowedTypes.addAll(Arrays.asList(types));
        }
    }

    public Set<Type> getAllowedTypes() {
        return Set.copyOf(allowedTypes);
    }

    public void setAllowedTypes(Type... types) {
        this.allowedTypes.clear();
        if (types == null || types.length == 0) {
            this.allowedTypes.add(Type.ANY);
        } else {
            this.allowedTypes.addAll(Arrays.asList(types));
        }
    }

    public boolean allows(Type t) {
        if (allowedTypes.contains(Type.ANY)) return true;
        return allowedTypes.contains(t);
    }

    public Set<Identifier> getWhitelist() {
        return whitelist;
    }

    public boolean isWhitelisted(Identifier id) {
        return whitelist.contains(id);
    }

    public boolean addToWhitelist(String idStr) {
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return false;
        return whitelist.add(id);
    }

    public boolean removeFromWhitelist(String idStr) {
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return false;
        return whitelist.remove(id);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (!json.isJsonObject()) return;
        JsonObject obj = json.getAsJsonObject();

        if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) {
            this.value = obj.get("enabled").getAsBoolean();
        }

        if (obj.has("items") && obj.get("items").isJsonArray()) {
            whitelist.clear();
            for (JsonElement element : obj.getAsJsonArray("items")) {
                if (element.isJsonPrimitive()) {
                    Identifier id = Identifier.tryParse(element.getAsString());
                    if (id != null) whitelist.add(id);
                }
            }
        }
        if (obj.has("types") && obj.get("types").isJsonArray()) {
            this.allowedTypes.clear();
            for (JsonElement e : obj.getAsJsonArray("types")) {
                if (!e.isJsonPrimitive()) continue;
                try {
                    String s = e.getAsString();
                    WhitelistSetting.Type t = WhitelistSetting.Type.valueOf(s.toUpperCase());
                    this.allowedTypes.add(t);
                } catch (Exception ignored) {}
            }
            if (this.allowedTypes.isEmpty()) this.allowedTypes.add(Type.ANY);
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", value);
        JsonArray items = new JsonArray();
        for (Identifier id : whitelist) {
            items.add(id.toString());
        }
        obj.add("items", items);
        JsonArray types = new JsonArray();
        for (Type t : allowedTypes) types.add(t.name());
        obj.add("types", types);
        return obj;
    }
}
