package anya.pizza.houseki.world.structure;

import anya.pizza.houseki.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class MeteoriteStructurePiece extends StructurePiece {
    public static final int MIN_RADIUS = 5;
    public static final int MAX_RADIUS = 8;
    private static final int CRATER_EXTRA = 4;

    private final int centerX;
    private final int surfaceY;
    private final int centerZ;
    private final int meteorRadius;
    private final int craterDepth;
    private final long seed;

    public MeteoriteStructurePiece(int centerX, int surfaceY, int centerZ,
                                   int meteorRadius, int craterDepth, long seed) {
        super(ModStructures.METEORITE_PIECE_TYPE, 0,
                makeBounds(centerX, surfaceY, centerZ, meteorRadius, craterDepth));
        this.centerX = centerX;
        this.surfaceY = surfaceY;
        this.centerZ = centerZ;
        this.meteorRadius = meteorRadius;
        this.craterDepth = craterDepth;
        this.seed = seed;
    }

    public MeteoriteStructurePiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.METEORITE_PIECE_TYPE, nbt);
        this.centerX = nbt.getInt("cx").orElse(0);
        this.surfaceY = nbt.getInt("sy").orElse(0);
        this.centerZ = nbt.getInt("cz").orElse(0);
        this.meteorRadius = nbt.getInt("mr").orElse(5);
        this.craterDepth = nbt.getInt("cd").orElse(7);
        this.seed = nbt.getLong("seed").orElse(0L);
    }

    private static BlockBox makeBounds(int cx, int sy, int cz, int r, int depth) {
        int extent = r + CRATER_EXTRA + 2;
        return new BlockBox(cx - extent, sy - depth - r, cz - extent,
                cx + extent, sy + 3, cz + extent);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("cx", centerX);
        nbt.putInt("sy", surfaceY);
        nbt.putInt("cz", centerZ);
        nbt.putInt("mr", meteorRadius);
        nbt.putInt("cd", craterDepth);
        nbt.putLong("seed", seed);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor,
                         ChunkGenerator chunkGenerator, Random chunkRandom,
                         BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        // Deterministic random from stored seed for consistent cross-chunk generation
        Random random = Random.create(this.seed);
        int craterRadius = meteorRadius + CRATER_EXTRA;
        int meteorCenterY = surfaceY - craterDepth;
        BlockPos meteorCenter = new BlockPos(centerX, meteorCenterY, centerZ);

        // Reject liquid locations
        BlockPos surfacePos = new BlockPos(centerX, surfaceY - 1, centerZ);
        BlockState surfaceState = world.getBlockState(surfacePos);
        if (surfaceState.getFluidState().isIn(FluidTags.WATER)
                || surfaceState.getFluidState().isIn(FluidTags.LAVA)) {
            return;
        }

        // Phase 1: Carve bowl-shaped crater
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist > craterRadius) continue;

                double depthFraction = 1.0 - (horizDist / craterRadius);
                int localDepth = (int) (craterDepth * depthFraction * depthFraction);
                if (localDepth < 1) continue;

                for (int y = surfaceY + 3; y >= surfaceY - localDepth; y--) {
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    if (!chunkBox.contains(pos)) continue;
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.BEDROCK)) continue;
                    if (!state.isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Phase 2: Place meteorite sphere
        int coreRadius = Math.max(1, meteorRadius / 2);
        for (int dx = -meteorRadius; dx <= meteorRadius; dx++) {
            for (int dy = -meteorRadius; dy <= meteorRadius; dy++) {
                for (int dz = -meteorRadius; dz <= meteorRadius; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > meteorRadius + 0.5) continue;

                    // Always consume random for cross-chunk consistency
                    double noise = (random.nextDouble() - 0.5) * 0.8;
                    BlockState shellBlock = getShellBlock(random);

                    double noisyDist = dist + noise;
                    if (noisyDist > meteorRadius) continue;

                    BlockPos pos = meteorCenter.add(dx, dy, dz);
                    if (!chunkBox.contains(pos)) continue;
                    BlockState existing = world.getBlockState(pos);
                    if (existing.isOf(Blocks.BEDROCK)) continue;

                    if (noisyDist <= coreRadius) {
                        world.setBlockState(pos, ModBlocks.METEORIC_IRON.getDefaultState(), 2);
                    } else {
                        world.setBlockState(pos, shellBlock, 2);
                    }
                }
            }
        }

        // Phase 3: Scatter meteoric iron debris on crater floor
        int debrisCount = 2 + random.nextInt(4);
        for (int i = 0; i < debrisCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double d = 1.5 + random.nextDouble() * (craterRadius * 0.7 - 1.5);
            int dx = (int) Math.round(Math.cos(angle) * d);
            int dz = (int) Math.round(Math.sin(angle) * d);

            // Analytically compute crater floor Y at this position
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            double depthFraction = 1.0 - (horizDist / craterRadius);
            int localDepth = (int) (craterDepth * depthFraction * depthFraction);
            if (localDepth < 2) continue;

            int craterFloorY = surfaceY - localDepth + 1;
            BlockPos debrisPos = new BlockPos(centerX + dx, craterFloorY, centerZ + dz);
            if (!chunkBox.contains(debrisPos)) continue;
            if (world.getBlockState(debrisPos).isAir()) {
                world.setBlockState(debrisPos, ModBlocks.METEORIC_IRON.getDefaultState(), 2);
            }
        }

        // Phase 4: Scorched ejecta rim
        for (int dx = -craterRadius - 1; dx <= craterRadius + 1; dx++) {
            for (int dz = -craterRadius - 1; dz <= craterRadius + 1; dz++) {
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist < craterRadius - 2 || horizDist > craterRadius + 1.5) continue;

                // Always consume random for cross-chunk consistency
                BlockState rimBlock = getShellBlock(random);
                int rimRoll = random.nextInt(3);
                BlockState raisedBlock = getShellBlock(random);

                BlockPos rimPos = new BlockPos(centerX + dx, surfaceY - 1, centerZ + dz);
                if (chunkBox.contains(rimPos)) {
                    BlockState rimState = world.getBlockState(rimPos);
                    if (!rimState.isAir() && !rimState.isOf(Blocks.BEDROCK)
                            && !rimState.getFluidState().isIn(FluidTags.WATER)) {
                        world.setBlockState(rimPos, rimBlock, 2);
                    }
                }

                if (rimRoll == 0 && horizDist >= craterRadius - 0.5 && horizDist <= craterRadius + 0.5) {
                    BlockPos aboveRim = new BlockPos(centerX + dx, surfaceY, centerZ + dz);
                    if (chunkBox.contains(aboveRim) && world.getBlockState(aboveRim).isAir()) {
                        world.setBlockState(aboveRim, raisedBlock, 2);
                    }
                }
            }
        }
    }

    private BlockState getShellBlock(Random random) {
        int roll = random.nextInt(10);
        if (roll < 4) return Blocks.STONE.getDefaultState();
        if (roll < 6) return Blocks.COBBLESTONE.getDefaultState();
        if (roll < 8) return Blocks.GRAVEL.getDefaultState();
        return Blocks.DEEPSLATE.getDefaultState();
    }
}
