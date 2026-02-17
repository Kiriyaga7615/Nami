package namidevelopment.kiriyaga.api.model.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class WhitelistSetting extends BoolSetting {
    private final Set<Identifier> whitelist = new HashSet<>();
    public enum Type { ANY, BLOCK, ITEM, ENTITY, SOUND, PARTICLE }
    private final Set<Type> allowedTypes = new HashSet<>();

    public WhitelistSetting(String identifier, String name, boolean defaultValue) {
        super(identifier, name, defaultValue);
        this.allowedTypes.add(Type.ANY);
    }

    public WhitelistSetting(String name, boolean defaultValue) {
        this(name, name, defaultValue);
    }

    public WhitelistSetting(String name, boolean defaultValue, Type... types) {
        this(name, name, defaultValue, types);
    }

    public WhitelistSetting(String identifier, String name, boolean defaultValue, Type... types) {
        this(identifier, name, defaultValue);
        this.allowedTypes.clear();
        if (types == null || types.length == 0) this.allowedTypes.add(Type.ANY);
        else this.allowedTypes.addAll(Arrays.asList(types));
    }

    public Set<Type> getAllowedTypes() { return Set.copyOf(allowedTypes); }
    public void setAllowedTypes(Type... types) {
        allowedTypes.clear();
        if (types == null || types.length == 0) allowedTypes.add(Type.ANY);
        else allowedTypes.addAll(Arrays.asList(types));
    }
    public boolean allows(Type t) { return allowedTypes.contains(Type.ANY) || allowedTypes.contains(t); }

    public boolean contains(String name) {
        Identifier id = Identifier.tryParse(normalize(name));
        return id != null && whitelist.contains(id);
    }

    public boolean add(String name) {
        Identifier id = Identifier.tryParse(normalize(name));
        if (id == null) return false;
        return whitelist.add(id);
    }

    public boolean remove(String name) {
        Identifier id = Identifier.tryParse(normalize(name));
        if (id == null) return false;
        return whitelist.remove(id);
    }

    public Set<Identifier> getWhitelist() { return Set.copyOf(whitelist); }
    @Override
    public void fromJson(JsonElement json) {
        if (!json.isJsonObject()) return;
        JsonObject obj = json.getAsJsonObject();

        if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) {
            this.value = obj.get("enabled").getAsBoolean();
        }

        if (obj.has("items") && obj.get("items").isJsonArray()) {
            whitelist.clear();
            for (JsonElement e : obj.getAsJsonArray("items")) {
                Identifier id = Identifier.tryParse(e.getAsString());
                if (id != null) whitelist.add(id);
            }
        }

        if (obj.has("types") && obj.get("types").isJsonArray()) {
            allowedTypes.clear();
            for (JsonElement e : obj.getAsJsonArray("types")) {
                try { allowedTypes.add(Type.valueOf(e.getAsString().toUpperCase())); }
                catch (Exception ignored) {}
            }
            if (allowedTypes.isEmpty()) allowedTypes.add(Type.ANY);
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", value);
        JsonArray items = new JsonArray();
        for (Identifier id : whitelist) items.add(id.toString());
        obj.add("items", items);
        JsonArray types = new JsonArray();
        for (Type t : allowedTypes) types.add(t.name());
        obj.add("types", types);
        return obj;
    }

    private String normalize(String idStr) {
        idStr = idStr.trim().toLowerCase();
        if (!idStr.contains(":")) idStr = "minecraft:" + idStr;
        return idStr;
    }
}
