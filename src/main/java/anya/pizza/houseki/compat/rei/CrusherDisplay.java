package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.recipe.CrusherRecipe;
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

public class CrusherDisplay extends BasicDisplay {
    public static final DisplaySerializer<CrusherDisplay> SERIALIZER = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(CrusherDisplay::getInputEntries),
                    EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(CrusherDisplay::getOutputEntries),
                    Identifier.CODEC.optionalFieldOf("location").forGetter(CrusherDisplay::getDisplayLocation)
            ).apply(instance, CrusherDisplay::new)),
            PacketCodec.tuple(
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    CrusherDisplay::getInputEntries,
                    EntryIngredient.streamCodec().collect(PacketCodecs.toList()),
                    CrusherDisplay::getOutputEntries,
                    PacketCodecs.optional(Identifier.PACKET_CODEC),
                    CrusherDisplay::getDisplayLocation,
                    CrusherDisplay::new
            ));

    public CrusherDisplay(RecipeEntry<CrusherRecipe> recipe) {
        super(List.of(EntryIngredients.ofIngredient(recipe.value().getIngredients().get(0))),
                List.of(EntryIngredient.of(EntryStacks.of(recipe.value().getResult(null)))),
                Optional.of(recipe.id().getValue()));
    }

    public CrusherDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location) {
        super(inputs, outputs, location);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CrusherCategory.CRUSHER;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }
}
