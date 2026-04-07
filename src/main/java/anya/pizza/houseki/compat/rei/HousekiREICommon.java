package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.Houseki;
import anya.pizza.houseki.recipe.CrusherRecipe;
import anya.pizza.houseki.recipe.ModRecipes;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.util.Identifier;

public class HousekiREICommon implements REICommonPlugin {
    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        registry.beginRecipeFiller(CrusherRecipe.class)
                .filterType(ModRecipes.CRUSHER_TYPE)
                .fill(entry -> new CrusherDisplay(entry));
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(Identifier.of(Houseki.MOD_ID, "crusher"), CrusherDisplay.SERIALIZER);
    }
}
