package anya.pizza.houseki.world.structure;

import anya.pizza.houseki.Houseki;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

public class ModStructures {
    public static final RegistryKey<Structure> METEORITE_KEY =
            RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(Houseki.MOD_ID, "meteorite"));

    public static final RegistryKey<StructureSet> METEORITE_SET_KEY =
            RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Houseki.MOD_ID, "meteorite"));

    public static final StructureType<MeteoriteStructure> METEORITE_TYPE =
            Registry.register(Registries.STRUCTURE_TYPE,
                    Identifier.of(Houseki.MOD_ID, "meteorite"),
                    () -> MeteoriteStructure.CODEC);

    public static final StructurePieceType METEORITE_PIECE_TYPE =
            Registry.register(Registries.STRUCTURE_PIECE,
                    Identifier.of(Houseki.MOD_ID, "meteorite"),
                    MeteoriteStructurePiece::new);

    public static void bootstrapStructure(Registerable<Structure> context) {
        var biomes = context.getRegistryLookup(RegistryKeys.BIOME);

        context.register(METEORITE_KEY, new MeteoriteStructure(
                new Structure.Config.Builder(biomes.getOrThrow(BiomeTags.IS_OVERWORLD)).build()
        ));
    }

    public static void bootstrapStructureSet(Registerable<StructureSet> context) {
        var structures = context.getRegistryLookup(RegistryKeys.STRUCTURE);

        context.register(METEORITE_SET_KEY, new StructureSet(
                structures.getOrThrow(METEORITE_KEY),
                new RandomSpreadStructurePlacement(96, 56, SpreadType.LINEAR, 948372615)
        ));
    }

    public static void register() {
        Houseki.LOGGER.info("Registering Structures for " + Houseki.MOD_ID);
    }
}
