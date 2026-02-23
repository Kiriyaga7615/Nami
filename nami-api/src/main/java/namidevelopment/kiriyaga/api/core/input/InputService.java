package namidevelopment.kiriyaga.api.core.input;

public class InputService {

    private final ClientInputHandler clientHandler = new ClientInputHandler();
    private final ServerInputHandler serverHandler = new ServerInputHandler();
    private final InputCache inputCache = new InputCache();

    public void init() {
        clientHandler.init(inputCache);
        serverHandler.init();
    }

    public ClientInputHandler getClientHandler() {
        return clientHandler;
    }

    public ServerInputHandler getServerHandler() {
        return serverHandler;
    }

    public InputCache getInputCache() {
        return inputCache;
    }
}