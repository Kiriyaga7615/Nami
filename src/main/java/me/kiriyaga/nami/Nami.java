package me.kiriyaga.nami;

import me.kiriyaga.nami.core.cat.FabricCatFormat;
import me.kiriyaga.nami.core.command.CommandManager;
import me.kiriyaga.nami.core.config.ConfigManager;
import me.kiriyaga.nami.core.executable.ExecutableManager;
import me.kiriyaga.nami.core.font.FontManager;
import me.kiriyaga.nami.core.inventory.InventoryManager;
import me.kiriyaga.nami.core.macro.MacroManager;
import me.kiriyaga.nami.core.rotation.RotationManager;
import me.kiriyaga.nami.feature.gui.newgui.component.NavigatePanelComponent;
import me.kiriyaga.nami.feature.gui.newgui.screen.LogScreen;
import me.kiriyaga.nami.feature.gui.oldgui.screen.ClickGuiScreen;
import me.kiriyaga.nami.core.*;
import me.kiriyaga.nami.core.module.ModuleManager;
import me.kiriyaga.nami.feature.gui.newgui.screen.FriendScreen;
import me.kiriyaga.nami.feature.gui.oldgui.screen.HudEditorScreen;
import me.kiriyaga.nami.util.CatStyles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.ServerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.Tuple;

public class Nami implements ClientModInitializer {
    public static String NAME = "nami";
    public static String DISPLAY_NAME = "Nami";
    public static long START_TIME = 0;
    public static final String VERSION;
    static {
        ModContainer mod = FabricLoader.getInstance().getModContainer("nami").orElse(null);
        if (mod != null) {
            VERSION = mod.getMetadata().getVersion().getFriendlyString();
        } else {
            VERSION = "dev-environment";
        }
    }

    public static final Minecraft MC = Minecraft.getInstance();

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static final EventManager EVENT_MANAGER = new EventManager();
    public static final MacroManager MACRO_MANAGER = new MacroManager();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static final ModuleManager MODULE_MANAGER = new ModuleManager();
    public static final FontManager FONT_MANAGER = new FontManager();
    public static final ExecutableManager EXECUTABLE_MANAGER = new ExecutableManager();
    public static final CommandManager COMMAND_MANAGER = new CommandManager();
    public static final ChatManager CHAT_MANAGER = new ChatManager();
    public static final FriendManager FRIEND_MANAGER = new FriendManager(CONFIG_MANAGER);
    public static final RotationManager ROTATION_MANAGER = new RotationManager();
    public static final InventoryManager INVENTORY_MANAGER = new InventoryManager();
    public static final ServerManager SERVER_MANAGER = new ServerManager();
    public static final InputManager INPUT_MANAGER = new InputManager();

    public static Tuple<ServerAddress, ServerInfo> LAST_CONNECTION = null;
    public static FabricCatFormat CAT_FORMAT = new FabricCatFormat();
    public static ClickGuiScreen CLICK_GUI;
    public static HudEditorScreen HUD_EDITOR;
    public static FriendScreen FRIEND;
    public static LogScreen LOG;
    public static NavigatePanelComponent NAVIGATE_PANEL;



    @Override
    public void onInitializeClient() {
        MODULE_MANAGER.init();
        COMMAND_MANAGER.init();
        COMMAND_MANAGER.getSuggester().updateDispatcher();
        //FONT_MANAGER.init();
        ROTATION_MANAGER.init();
        INVENTORY_MANAGER.init();
        EXECUTABLE_MANAGER.init();
        SERVER_MANAGER.init();
        CHAT_MANAGER.init();
        INPUT_MANAGER.init();

        CAT_FORMAT.add(new CatStyles());

        CLICK_GUI = new ClickGuiScreen();
        HUD_EDITOR = new HudEditorScreen();
        FRIEND = new FriendScreen();
        LOG = new LogScreen();
        NAVIGATE_PANEL = new NavigatePanelComponent();

        FRIEND_MANAGER.load();

        LOGGER.info(NAME + "\n " + VERSION + " has been initialized\n");

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FONT_MANAGER.init(); // font is making glyph textures, it should be after game loaded not on initialize

            START_TIME = System.currentTimeMillis();

            CONFIG_MANAGER.loadModules();
            CONFIG_MANAGER.loadFriends();
            if (CONFIG_MANAGER.loadName() == null)
                CONFIG_MANAGER.saveName(DISPLAY_NAME);
            else
                DISPLAY_NAME = CONFIG_MANAGER.loadName();
            COMMAND_MANAGER.getExecutor().setPrefix(CONFIG_MANAGER.loadPrefix());
            CONFIG_MANAGER.loadMacros();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CONFIG_MANAGER.saveModules();
            CONFIG_MANAGER.saveMacros();
        });

    }
}
