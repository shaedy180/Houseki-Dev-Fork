# Meteorite Structure System - Full Code Documentation

This document explains every file involved in the Meteorite world generation feature of the Houseki mod.
It covers what each file does, what each section of code is responsible for, and how you could extend
or change things in the future.

---

## Table of Contents

1. [Overview and Architecture](#1-overview-and-architecture)
2. [ModStructures.java - Registration](#2-modstructuresjava---registration)
3. [MeteoriteStructure.java - Positioning Logic](#3-meteoritestructurejava---positioning-logic)
4. [MeteoriteStructurePiece.java - Block Placement](#4-meteoritestructurepiecejava---block-placement)
5. [meteorite.json (Structure)](#5-meteoritejson-structure)
6. [meteorite.json (Structure Set)](#6-meteoritejson-structure-set)
7. [ModBlocks.METEORIC_IRON](#7-modblocksmeteoric_iron)
8. [How It All Connects](#8-how-it-all-connects)
9. [Ideas for Future Extension](#9-ideas-for-future-extension)

---

## 1. Overview and Architecture

The Meteorite system is a custom Fabric structure that procedurally generates a meteorite impact site
in the Overworld. It consists of three Java files and two generated JSON files:

| File | What it does |
|---|---|
| `ModStructures.java` | Registers the structure type, piece type, registry keys, and provides bootstrap methods for datagen |
| `MeteoriteStructure.java` | Decides where a meteorite spawns (chunk position, surface height) and what parameters it has |
| `MeteoriteStructurePiece.java` | Contains all the actual block-placing logic: the crater, the sphere, debris, and rim |
| `worldgen/structure/meteorite.json` | Generated JSON that tells Minecraft this structure exists and where it can appear (biomes) |
| `worldgen/structure_set/meteorite.json` | Generated JSON that controls how frequently and how spread out meteorites are |

The general flow looks like this:
Minecraft's world generator checks the structure set JSON for placement, then calls
`MeteoriteStructure.getStructurePosition()`, which creates a `MeteoriteStructurePiece`,
and finally `generate()` gets called to place all the blocks.

---

## 2. ModStructures.java - Registration

**Path:** `src/main/java/anya/pizza/houseki/world/structure/ModStructures.java`

This file is the entry point for the entire meteorite structure system. It registers everything
needed with Minecraft's registries so the game knows the structure exists.

### Full Source with Annotations

```java
public class ModStructures {
```

A utility class that holds all static registrations. No instances are ever created.

---

```java
    public static final RegistryKey<Structure> METEORITE_KEY =
            RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(Houseki.MOD_ID, "meteorite"));
```

`METEORITE_KEY` is a unique registry key to identify the meteorite structure. The identifier
resolves to `houseki:meteorite`. This is referenced later when registering the actual structure
instance during datagen bootstrap.

---

```java
    public static final RegistryKey<StructureSet> METEORITE_SET_KEY =
            RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Houseki.MOD_ID, "meteorite"));
```

`METEORITE_SET_KEY` is a registry key for the structure set, which is the placement configuration
that tells Minecraft how frequently to attempt spawning meteorites.

---

```java
    public static final StructureType<MeteoriteStructure> METEORITE_TYPE =
            Registry.register(Registries.STRUCTURE_TYPE,
                    Identifier.of(Houseki.MOD_ID, "meteorite"),
                    () -> MeteoriteStructure.CODEC);
```

`METEORITE_TYPE` registers a new `StructureType` with the game. A `StructureType` is essentially a
factory/codec pair. The lambda `() -> MeteoriteStructure.CODEC` provides the serialization codec so
Minecraft can read/write the structure from/to JSON. This registration happens at class load time
(static initializer).

---

```java
    public static final StructurePieceType METEORITE_PIECE_TYPE =
            Registry.register(Registries.STRUCTURE_PIECE,
                    Identifier.of(Houseki.MOD_ID, "meteorite"),
                    MeteoriteStructurePiece::new);
```

`METEORITE_PIECE_TYPE` registers the structure piece type. A structure piece is the unit that
actually places blocks. The method reference `MeteoriteStructurePiece::new` points to the NBT
constructor `(StructureContext, NbtCompound)`, which Minecraft uses to deserialize saved structure
pieces from disk.

---

```java
    public static void bootstrapStructure(Registerable<Structure> context) {
        var biomes = context.getRegistryLookup(RegistryKeys.BIOME);

        context.register(METEORITE_KEY, new MeteoriteStructure(
                new Structure.Config.Builder(biomes.getOrThrow(BiomeTags.IS_OVERWORLD)).build()
        ));
    }
```

`bootstrapStructure()` is called during data generation. This method creates the actual structure
instance and assigns it to `METEORITE_KEY`.
- `biomes.getOrThrow(BiomeTags.IS_OVERWORLD)` restricts the structure to only generate in Overworld
  biomes. The tag `#minecraft:is_overworld` includes all Overworld biomes.
- `new Structure.Config.Builder(...).build()` creates the structure configuration with defaults
  (no mob spawn overrides, etc.).
- This is what generates the `worldgen/structure/meteorite.json` file.

---

```java
    public static void bootstrapStructureSet(Registerable<StructureSet> context) {
        var structures = context.getRegistryLookup(RegistryKeys.STRUCTURE);

        context.register(METEORITE_SET_KEY, new StructureSet(
                structures.getOrThrow(METEORITE_KEY),
                new RandomSpreadStructurePlacement(96, 56, SpreadType.LINEAR, 948372615)
        ));
    }
```

`bootstrapStructureSet()` is also called during data generation. It defines how often and how
spread out the meteorites are.
- `96` = spacing. The game divides the world into a grid of 96x96 chunk regions. At most one
  meteorite can attempt to spawn per region.
- `56` = separation. The minimum distance (in chunks) between two meteorites. This prevents them
  from clustering in adjacent grid cells.
- `SpreadType.LINEAR` means the random offset within each grid cell is uniformly distributed
  (as opposed to `TRIANGULAR` which clusters toward the center).
- `948372615` = salt. A unique number that seeds the RNG for placement. Different structures use
  different salts so they don't all end up in the same locations.
- This generates the `worldgen/structure_set/meteorite.json` file.

---

```java
    public static void register() {
        Houseki.LOGGER.info("Registering Structures for " + Houseki.MOD_ID);
    }
```

`register()` is called from the mod initializer. Its main purpose is to force this class to load,
which triggers all the static field initializations above (the `Registry.register(...)` calls).
The log message is just for debugging.

---

## 3. MeteoriteStructure.java - Positioning Logic

**Path:** `src/main/java/anya/pizza/houseki/world/structure/MeteoriteStructure.java`

This class extends Minecraft's `Structure` and is responsible for deciding whether a meteorite should
spawn at a given location and what parameters it should have (radius, crater depth).

### Full Source with Annotations

```java
public class MeteoriteStructure extends Structure {
    public static final MapCodec<MeteoriteStructure> CODEC = createCodec(MeteoriteStructure::new);
```

`CODEC` is the serialization codec. `createCodec(MeteoriteStructure::new)` uses the base `Structure`
codec (which handles the `Config` parameter) and wraps it with the constructor. This is what
`METEORITE_TYPE` references. Since there are no custom JSON fields, the codec just handles the
standard config (biomes, spawn overrides, etc.).

---

```java
    @Override
    public Optional<StructurePosition> getStructurePosition(Context context) {
```

This is the core method. Minecraft calls this when it wants to know if a meteorite should generate
at a specific chunk. Returning `Optional.empty()` means "don't generate here." Returning a
`StructurePosition` means "yes, generate at this position."

---

```java
        int x = context.chunkPos().getCenterX();
        int z = context.chunkPos().getCenterZ();
```

Gets the world-space X/Z block coordinates of the center of the chunk being evaluated.

---

```java
        int surfaceY = context.chunkGenerator().getHeightOnGround(
                x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
```

Queries the chunk generator for the surface height at this position. `WORLD_SURFACE_WG` is the
heightmap type used during world generation. This gives us the Y coordinate of the topmost solid
block.

---

```java
        if (surfaceY <= context.world().getBottomY() + 10) {
            return Optional.empty();
        }
```

Safety check: if the surface is extremely low (within 10 blocks of the world bottom, around Y = -54
in the Overworld), don't generate. This prevents meteorites from spawning in void areas or deep
caves.

---

```java
        int radius = MeteoriteStructurePiece.MIN_RADIUS
                + random.nextInt(MeteoriteStructurePiece.MAX_RADIUS - MeteoriteStructurePiece.MIN_RADIUS + 1);
```

Picks a random radius for the meteorite sphere between `MIN_RADIUS` (7) and `MAX_RADIUS` (11),
inclusive. So the meteorite can be 7-11 blocks in radius, resulting in a diameter of 14-22 blocks.

---

```java
        int craterDepth = radius + 3 + random.nextInt(4);
```

Calculates the crater depth using the formula: `radius + 3 + random(0..3)`. For a radius-7
meteorite, the crater is 10-13 blocks deep. For radius-11, it's 14-17 blocks deep. Larger
meteorites naturally create deeper craters.

---

```java
        long pieceSeed = random.nextLong();
```

A unique seed derived from the structure's random. This seed is passed to the piece so it can
recreate the exact same random sequence during generation. This matters because structure pieces
can be generated across multiple chunk-generation passes (one chunk at a time), and the output
must be deterministic.

---

```java
        return Optional.of(new StructurePosition(pos, collector -> {
            collector.addPiece(new MeteoriteStructurePiece(
                    x, surfaceY, z, radius, craterDepth, pieceSeed));
        }));
```

Returns a `StructurePosition` with the position and a piece generator lambda. The lambda creates
a single `MeteoriteStructurePiece` with all the calculated parameters. Minecraft calls this lambda
to collect the pieces that make up the structure.

---

## 4. MeteoriteStructurePiece.java - Block Placement

**Path:** `src/main/java/anya/pizza/houseki/world/structure/MeteoriteStructurePiece.java`

This is the largest and most complex file. It contains all the logic for actually placing blocks
in the world to create the meteorite, its crater, debris, and rim.

### Constants

```java
public static final int MIN_RADIUS = 7;
public static final int MAX_RADIUS = 11;
```

Range of possible meteorite sphere sizes (in blocks). Used by `MeteoriteStructure` when picking
a random radius.

```java
private static final int CRATER_EXTRA = 6;
```

The crater extends 6 blocks beyond the meteorite radius. So a radius-7 meteorite gets a radius-13
crater.

```java
private static final int TREE_CLEAR_HEIGHT = 20;
```

When clearing the crater area, blocks up to 20 blocks above the surface are removed. This makes
sure tall trees and vegetation above the impact zone are completely cleared.

### Constructors

The first constructor is called when the structure is first created during world generation.
`super(...)` passes the piece type and a bounding box (computed by `makeBounds`). The bounding box
tells Minecraft which chunks this structure overlaps, so it knows to call `generate()` for each one.

The second constructor (from NBT) is called when loading from a saved game. It reads back all the
parameters from NBT. The `.orElse(...)` calls provide fallback defaults in case data is missing
(for example from older saves).

### makeBounds() - Bounding Box

```java
private static BlockBox makeBounds(int cx, int sy, int cz, int r, int depth) {
    int extent = r + CRATER_EXTRA + 3;
    return new BlockBox(cx - extent, sy - depth - r - 2, cz - extent,
                        cx + extent, sy + TREE_CLEAR_HEIGHT, cz + extent);
}
```

Computes the axis-aligned bounding box that fully encloses the structure.
- `extent = radius + 6 + 3` covers the meteorite, the crater, and a small buffer for the ejecta rim.
- Vertically it goes from `surfaceY - depth - radius - 2` (deep enough for the meteorite sphere at
  the bottom of the crater) to `surfaceY + 20` (high enough to clear trees).

### writeNbt() - Serialization

Saves all parameters to NBT so the structure can be reconstructed when loading a saved game.
Short key names (`cx`, `sy`, etc.) are used to save space.

### getCraterFloorY() - Crater Shape

```java
private int getCraterFloorY(double horizDist, int craterRadius) {
    if (horizDist > craterRadius) return surfaceY;
    double depthFraction = 1.0 - (horizDist / craterRadius);
    int localDepth = (int) (craterDepth * depthFraction * depthFraction);
    return surfaceY - localDepth;
}
```

Calculates the Y coordinate of the crater floor at a given horizontal distance from the center.
If outside the crater radius, it just returns `surfaceY` (no crater). The quadratic curve
(`depthFraction * depthFraction`) creates a smooth, bowl-shaped crater that is deepest at the center
and gradually rises to the surface at the edges. This looks much more natural than a flat-bottomed
or linear hole.

### generate() - The Main Block-Placement Method

This is called once per chunk that overlaps the structure's bounding box. The `chunkBox` parameter
restricts which blocks can be placed during this particular call.

#### Setup

```java
Random random = Random.create(this.seed);
```

Creates a deterministic random from the stored seed. This is critical because `generate()` is called
separately for each chunk, and we need the same random sequence every time so the structure looks
identical regardless of which chunks load first.

#### Liquid Rejection

Checks the block just below the surface at the center. If it's water or lava, the entire generation
is aborted. This prevents meteorites from appearing in oceans, lakes, or lava pools where they
would look wrong.

#### Phase 1: Crater Excavation

Iterates over a circular area and uses `getCraterFloorY()` to dig a bowl shape. Inside the crater,
everything from 20 blocks above the surface down to the crater floor is replaced with air. This
removes terrain, trees, flowers, and anything else.

After clearing, the crater floor is lined with 2 layers of scorched material (stone, cobblestone,
gravel, or cobbled deepslate). Only non-stone blocks (dirt, sand, grass) get replaced. Stone blocks
are left as-is since they already look right for a crater.

Near the rim (just outside the crater radius), only blocks above the terrain are cleared (trees,
leaves). The terrain itself is left alone, creating a clean transition zone around the lip.

#### Phase 2: Crater Wall Lining

After excavation, the crater walls may expose dirt, sand, or other soft blocks. This phase scans
every block in the crater walls and checks if it has an air neighbor (meaning it's visible to the
player). If so, and if it's not already a stone-type block, it gets replaced with crater liner
material. This gives the crater a realistic, scorched look.

#### Phase 3: Meteorite Sphere

Iterates over a 3D cube and uses spherical distance to create a sphere centered at `meteorCenter`.
A random noise offset of about +/-0.4 blocks is added to each position's distance calculation.
This creates an irregular, rough surface rather than a perfect mathematical sphere, making the
meteorite look natural.

The sphere is split into two zones:
- **Core** (inner half): Filled with `METEORIC_IRON`, the mod's custom block (strength 50, blast
  resistance 1200). This is the valuable resource players mine.
- **Shell** (outer half): Filled with a random mix of stone, cobblestone, gravel, and deepslate.
  This represents the fused rock outer layer.

#### Phase 4: Debris Scatter

Scatters 3-7 pieces of `METEORIC_IRON` on the crater floor at random positions. Each piece is
placed at a random angle and distance (between 2 blocks and 65% of the crater radius from center).
Only placed if the target position is air (won't overwrite existing blocks). This simulates
fragments that broke off during impact.

#### Phase 5: Ejecta Rim

Affects a ring-shaped area around the crater edge (from 2.5 blocks inside the rim to 2 blocks
outside). Replaces the surface block with shell-type material (stone mix), creating a scorched ring.

With a 1-in-3 chance, an elevated block is placed on top of the rim. This creates the uneven,
raised crater lip you would expect from a real impact site.

### Helper Methods

**`isStoneType(BlockState)`** returns `true` if the block is any natural stone variant (stone,
cobblestone, deepslate, gravel, andesite, diorite, granite, tuff). Used to decide whether crater
wall/floor blocks need to be replaced. Stone blocks are left as-is since they already look fine.

**`isExposedToAir(...)`** checks all 6 neighbors (up, down, north, south, east, west). If any
neighbor is air and within the chunk box, the block is considered "exposed." Used in Phase 2 to
only replace blocks that players can actually see.

**`getCraterLiner(Random)`** is a weighted random block palette for crater floors and walls:
50% stone, 20% cobblestone, 20% gravel, 10% cobbled deepslate.

**`getShellBlock(Random)`** is a block palette for the meteorite's outer shell and the rim:
40% stone, 20% cobblestone, 20% gravel, 20% deepslate. Similar to the crater liner but with
deepslate instead of cobbled deepslate, and slightly different weights.

---

## 5. meteorite.json (Structure)

**Path:** `src/main/generated/data/houseki/worldgen/structure/meteorite.json`

```json
{
  "type": "houseki:meteorite",
  "biomes": "#minecraft:is_overworld",
  "spawn_overrides": {},
  "step": "surface_structures"
}
```

| Field | Meaning |
|---|---|
| `type` | References the registered `StructureType`. `houseki:meteorite` maps to `METEORITE_TYPE` |
| `biomes` | `#minecraft:is_overworld` means the structure can appear in any Overworld biome |
| `spawn_overrides` | Empty, so no mob spawning is modified inside the structure |
| `step` | `surface_structures` means this generates during the surface structures phase of world gen |

This file is auto-generated by datagen from `bootstrapStructure()`. Do not edit it manually.

---

## 6. meteorite.json (Structure Set)

**Path:** `src/main/generated/data/houseki/worldgen/structure_set/meteorite.json`

```json
{
  "placement": {
    "type": "minecraft:random_spread",
    "salt": 948372615,
    "separation": 56,
    "spacing": 96
  },
  "structures": [
    {
      "structure": "houseki:meteorite",
      "weight": 1
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `spacing` | 96 chunks (1536 blocks). The world is divided into a grid with cells this size. |
| `separation` | 56 chunks (896 blocks). Minimum distance between two meteorites. |
| `salt` | 948372615. Unique seed modifier so meteorites don't overlap with other structures' grids. |
| `weight` | 1. Since there's only one structure in the set, weight doesn't matter here. If you added variants, weights would control their relative frequency. |

In practice, meteorites appear roughly every 96 chunks apart (about 1500 blocks), and never closer
than 56 chunks (about 900 blocks). They are relatively rare.

This file is auto-generated by datagen from `bootstrapStructureSet()`. Do not edit it manually.

---

## 7. ModBlocks.METEORIC_IRON

**Path:** `src/main/java/anya/pizza/houseki/block/ModBlocks.java` (line 102)

```java
public static final Block METEORIC_IRON = registerBlock("meteoric_iron",
    properties -> new Block(properties
        .mapColor(MapColor.BLACK)
        .instrument(NoteBlockInstrument.DRAGON)
        .requiresTool()
        .strength(50F, 1200F)));
```

- `mapColor(MapColor.BLACK)` makes it appear black on maps.
- `instrument(NoteBlockInstrument.DRAGON)` makes a dragon sound when used with a note block.
- `requiresTool()` means it must be mined with the correct tool; hand-mining drops nothing.
- `strength(50F, 1200F)` makes it very hard to mine (50) and extremely blast-resistant (1200,
  similar to obsidian). This makes the meteorite feel like a durable, alien material.

---

## 8. How It All Connects

```
Mod Initialization
  |-- ModStructures.register()
  |     \-- Forces class load, which registers METEORITE_TYPE and METEORITE_PIECE_TYPE

Data Generation
  |-- bootstrapStructure()   -> generates structure/meteorite.json
  |-- bootstrapStructureSet() -> generates structure_set/meteorite.json

World Generation (runtime)
  1. Minecraft checks structure_set/meteorite.json placement rules
  2. For each valid chunk, calls MeteoriteStructure.getStructurePosition()
  3. If position is valid, creates MeteoriteStructurePiece with random parameters
  4. For each chunk overlapping the piece's bounding box:
     \-- Calls MeteoriteStructurePiece.generate()
         |-- Phase 1: Excavate crater (bowl shape + clear trees)
         |-- Phase 2: Line exposed crater walls with stone
         |-- Phase 3: Place meteorite sphere (iron core + stone shell)
         |-- Phase 4: Scatter iron debris on crater floor
         |-- Phase 5: Create scorched ejecta rim
```

---

## 9. Ideas for Future Extension

### Adjusting Frequency / Size

| Change | Where to modify |
|---|---|
| Make meteorites more/less common | Change `spacing` and `separation` in `bootstrapStructureSet()` (lower = more common) |
| Change meteorite size range | Adjust `MIN_RADIUS` and `MAX_RADIUS` in `MeteoriteStructurePiece` |
| Adjust crater depth | Modify the formula `radius + 3 + random.nextInt(4)` in `MeteoriteStructure.getStructurePosition()` |
| Allow in Nether/End | Change `BiomeTags.IS_OVERWORLD` in `bootstrapStructure()` to a different biome tag |

### Adding New Features

- **Loot chests**: In Phase 4 (debris scatter), you could place a `Blocks.CHEST` with a loot table
  instead of some debris blocks. Cast `world.getBlockEntity(pos)` to a chest and set a loot table.

- **Fire / smoke particles**: Add a fire phase between the rim and debris phases that places
  `Blocks.FIRE` or `Blocks.SOUL_FIRE` at random spots on the crater floor. Note that fire will
  go out over time unless placed on appropriate blocks.

- **Multiple meteorite variants**: Create additional structure types (e.g., `SmallMeteoriteStructure`,
  `GiantMeteoriteStructure`) with different radius/depth constants, block palettes, and spawn rates.
  Register them all in `ModStructures` with separate keys.

- **Custom ore distribution**: Instead of a uniform `METEORIC_IRON` core, you could embed rare ores
  (diamonds, custom gems) as small veins within the sphere. Add another roll in Phase 3 for
  `noisyDist` close to the core/shell boundary.

- **Biome-specific palettes**: Pass the biome to the piece and vary `getCraterLiner()` /
  `getShellBlock()` based on biome. Desert meteorites could use sandstone, snowy ones could add
  snow/ice layers.

- **Structure NBT templates**: For decorative elements (alien machinery, obsidian pillars), you could
  load `.nbt` structure files and place them at the meteorite center using `StructureTemplate`.

- **Explosion particles on first discovery**: Use a custom game event or advancement trigger so
  clients near the meteorite play a particle/sound effect the first time a player enters the
  bounding box.

### Code Quality Improvements

- **Block tag for `isStoneType()`**: Instead of hardcoding each block, create a block tag
  (`houseki:crater_resistant`) and use `state.isIn(tag)`. This makes it data-driven and extendable
  via datapacks.

- **Block tag for shell/crater palettes**: The `getCraterLiner()` and `getShellBlock()` methods
  could read from weighted tag entries, making the palettes configurable without code changes.

- **Config file**: Expose `MIN_RADIUS`, `MAX_RADIUS`, `CRATER_EXTRA`, spacing, and separation as
  config values (for example via a Cloth Config screen or a simple JSON config) so players and
  modpack creators can tune the generation without touching code.
