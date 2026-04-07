package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.recipe.FoundryMeltingRecipe;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class FoundryMeltingDisplay extends BasicDisplay {
    public static final DisplaySerializer<FoundryMeltingDisplay> SERIALIZER = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(FoundryMeltingDisplay::getInputEntries),
                    EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(FoundryMeltingDisplay::getOutputEntries),
                    Identifier.CODEC.optionalFieldOf("location").forGetter(FoundryMeltingDisplay::getDisplayLocation)
            ).apply(instance, FoundryMeltingDisplay::new)),
            PacketCodec.tuple(
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    FoundryMeltingDisplay::getInputEntries,
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    FoundryMeltingDisplay::getOutputEntries,
                    PacketCodecs.optional(Identifier.PACKET_CODEC),
                    FoundryMeltingDisplay::getDisplayLocation,
                    FoundryMeltingDisplay::new
            ));

    public FoundryMeltingDisplay(RecipeEntry<FoundryMeltingRecipe> recipe) {
        super(List.of(EntryIngredients.ofIngredient(recipe.value().inputMeltingItem())),
                List.of(EntryIngredient.of(EntryStacks.of(recipe.value().output()))),
                Optional.of(recipe.id().getValue()));
    }

    public FoundryMeltingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location) {
        super(inputs, outputs, location);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return FoundryMeltingCategory.FOUNDRY_MELTING;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }
}
