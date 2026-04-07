package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.recipe.FoundryCastingRecipe;
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

public class FoundryCastingDisplay extends BasicDisplay {
    public static final DisplaySerializer<FoundryCastingDisplay> SERIALIZER = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(FoundryCastingDisplay::getInputEntries),
                    EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(FoundryCastingDisplay::getOutputEntries),
                    Identifier.CODEC.optionalFieldOf("location").forGetter(FoundryCastingDisplay::getDisplayLocation)
            ).apply(instance, FoundryCastingDisplay::new)),
            PacketCodec.tuple(
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    FoundryCastingDisplay::getInputEntries,
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    FoundryCastingDisplay::getOutputEntries,
                    PacketCodecs.optional(Identifier.PACKET_CODEC),
                    FoundryCastingDisplay::getDisplayLocation,
                    FoundryCastingDisplay::new
            ));

    public FoundryCastingDisplay(RecipeEntry<FoundryCastingRecipe> recipe) {
        super(List.of(EntryIngredients.ofIngredient(recipe.value().inputCastingItem())),
                List.of(EntryIngredient.of(EntryStacks.of(recipe.value().output()))),
                Optional.of(recipe.id().getValue()));
    }

    public FoundryCastingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location) {
        super(inputs, outputs, location);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return FoundryCastingCategory.FOUNDRY_CASTING;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }
}
