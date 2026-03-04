package namidevelopment.kiriyaga.api.core.inventory;

public class InventoryService {

    private final InventoryClickHandler clickHandler = new InventoryClickHandler();
    private final InventorySwapHandler swapHandler = new InventorySwapHandler();

    public void init() {
        swapHandler.init();
    }


    public InventoryClickHandler getClickHandler() {
        return clickHandler;
    }

    public InventorySwapHandler getSwapHandler() {
        return swapHandler;
    }
}
