package namidevelopment.kiriyaga.nami.impl.feature.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.network.protocol.Packet;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import static namidevelopment.kiriyaga.api.NamiApi.LOGGER;
import static namidevelopment.kiriyaga.api.NamiApi.CONFIG_SERVICE;

//@RegisterFeature
public class RandomFeature extends Feature {

    public final BoolSetting logReceive = addSetting(new BoolSetting("PacketReceiveLog", true));
    public final BoolSetting logSend = addSetting(new BoolSetting("PacketSendLog", true));

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private File receiveFile;
    private File sendFile;
    public RandomFeature() {
        super("RandomFeature", "Insane tech.", FeatureCategory.of("Client"));
    }

    @Override
    public void onEnable(){
        File baseDir = CONFIG_SERVICE.getDirectoryProvider().getBaseDir();

        File logDir = new File(baseDir, "packet_logs");
        if (!logDir.exists()) logDir.mkdirs();

        this.receiveFile = new File(logDir, "receive.jsonl");
        this.sendFile = new File(logDir, "send.jsonl");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!logReceive.get()) return;

        Packet<?> packet = event.getPacket();
        logPacket(packet, "RECEIVE", receiveFile);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketSend(PacketSendEvent event) {
        if (!logSend.get()) return;

        Packet<?> packet = event.getPacket();
        logPacket(packet, "SEND", sendFile);
    }

    private void logPacket(Packet<?> packet, String direction, File file) {
        try { //yo
            long timestamp = System.currentTimeMillis();

            JsonObject json = new JsonObject();
            json.addProperty("direction", direction);
            json.addProperty("timestamp", timestamp);
            json.addProperty("time", dateFormat.format(new Date(timestamp)));

            json.addProperty("packetClass", packet.getClass().getName());
            json.addProperty("packetSimpleName", packet.getClass().getSimpleName());

            JsonObject fieldsJson = new JsonObject();

            Class<?> clazz = packet.getClass();
            while (clazz != null && clazz != Object.class) {
                Field[] fields = clazz.getDeclaredFields();

                for (Field field : fields) {
                    field.setAccessible(true);

                    try {
                        Object value = field.get(packet);

                        if (value == null) {
                            fieldsJson.addProperty(field.getName(), "null");
                        } else {
                            fieldsJson.addProperty(field.getName(), safeToString(value));
                        }

                    } catch (Exception e) {
                        fieldsJson.addProperty(field.getName(), "[ERROR: " + e.getClass().getSimpleName() + "]");
                    }
                }

                clazz = clazz.getSuperclass();
            }

            json.add("fields", fieldsJson);

            writeJsonLine(file, json);

        } catch (Exception e) {
            LOGGER.error("Failed to log packet " + packet.getClass().getName(), e);
        }
    }

    private void writeJsonLine(File file, JsonObject json) {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, true)) {
            writer.write(gson.toJson(json));
            writer.write("\n");
        } catch (Exception e) {
            LOGGER.error("Failed to write packet log file: " + file.getName(), e);
        }
    }

    private String safeToString(Object obj) {
        try {
            if (obj.getClass().isArray()) {
                return "[Array: " + obj.getClass().getComponentType().getSimpleName() + "]";
            }
            return obj.toString();
        } catch (Exception e) {
            return "[toString ERROR: " + e.getClass().getSimpleName() + "]";
        }
    }
}
