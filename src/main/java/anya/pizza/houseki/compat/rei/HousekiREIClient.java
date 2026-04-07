package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.block.ModBlocks;
import anya.pizza.houseki.screen.custom.CrusherScreen;
import anya.pizza.houseki.screen.custom.FoundryScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;

public class HousekiREIClient implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new CrusherCategory());
        registry.addWorkstations(CrusherCategory.CRUSHER, EntryStacks.of(ModBlocks.CRUSHER));

        registry.add(new FoundryMeltingCategory());
        registry.addWorkstations(FoundryMeltingCategory.FOUNDRY_MELTING, EntryStacks.of(ModBlocks.FOUNDRY));

        registry.add(new FoundryCastingCategory());
        registry.addWorkstations(FoundryCastingCategory.FOUNDRY_CASTING, EntryStacks.of(ModBlocks.FOUNDRY));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerClickArea(screen -> new Rectangle(((screen.width - 176) / 2) + 78, ((screen.height - 176) / 2) + 30, 20, 25),
                CrusherScreen.class, CrusherCategory.CRUSHER);
        registry.registerClickArea(screen -> new Rectangle(((screen.width - 176) / 2) + 49, ((screen.height - 176) / 2) + 35, 24, 16),
                FoundryScreen.class, FoundryMeltingCategory.FOUNDRY_MELTING);
        registry.registerClickArea(screen -> new Rectangle(((screen.width - 176) / 2) + 102, ((screen.height - 176) / 2) + 35, 24, 16),
                FoundryScreen.class, FoundryCastingCategory.FOUNDRY_CASTING);
    }
}
