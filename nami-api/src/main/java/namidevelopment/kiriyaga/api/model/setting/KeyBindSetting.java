package namidevelopment.kiriyaga.api.model.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static namidevelopment.kiriyaga.api.util.KeyUtils.parseKey;

public class KeyBindSetting extends Setting<Integer> {

    public static final int KEY_NONE = -1;

    private boolean holdMode = false;

    public KeyBindSetting(String identifier, String name, int defaultKey) {
        super(identifier, name, defaultKey);
    }

    public KeyBindSetting(String identifier, String name, String defaultKeyName) {
        this(identifier, name, defaultKeyName != null ? parseKey(defaultKeyName) : KEY_NONE);
    }

    public KeyBindSetting(String name) {
        this(name, name, KEY_NONE);
    }

    public KeyBindSetting(String name, int defaultKey) {
        this(name, name, defaultKey);
    }

    public KeyBindSetting(String name, String defaultKeyName) {
        this(name, name, defaultKeyName);
    }

    public boolean isBound() {
        return value != null && value != KEY_NONE;
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }

    @Override
    public void set(Integer value) {
        this.value = value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("value", value);
        obj.addProperty("holdMode", holdMode);
        return obj;
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();

            this.value = obj.has("value") ? obj.get("value").getAsInt() : super.value;

            this.holdMode = obj.has("holdMode") && obj.get("holdMode").getAsBoolean();

        } else {
            this.value = super.value;
            this.holdMode = false;
        }
    }
}