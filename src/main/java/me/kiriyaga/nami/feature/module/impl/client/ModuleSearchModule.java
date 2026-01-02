package me.kiriyaga.nami.feature.module.impl.client;

import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class ModuleSearchModule extends Module {

    public ModuleSearchModule() {
        super("Search", "Displays module search progress.", ModuleCategory.of("Client"));
        this.setEnabled(true);
    }

    @Override
    public String getName() {
        if (CLICK_GUI == null || !CLICK_GUI.searchActive) {
            return "Search";
        }

        String query = CLICK_GUI.searchQuery;
        return "Search: " + query + "_";
    }
}
