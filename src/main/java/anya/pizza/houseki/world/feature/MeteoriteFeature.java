package anya.pizza.houseki.world.feature;

import anya.pizza.houseki.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class MeteoriteFeature extends Feature<DefaultFeatureConfig> {
    private static final int MIN_RADIUS = 3;
    private static final int MAX_RADIUS = 5;
    private static final int CRATER_EXTRA = 3;

    public MeteoriteFeature() {
        super(DefaultFeatureConfig.CODEC);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        if (surfaceY <= world.getBottomY() + 10) return false;

        // Reject if surface block is a liquid
        BlockPos surfacePos = new BlockPos(origin.getX(), surfaceY - 1, origin.getZ());
        BlockState surfaceState = world.getBlockState(surfacePos);
        if (surfaceState.getFluidState().isIn(FluidTags.WATER) || surfaceState.getFluidState().isIn(FluidTags.LAVA)) {
            return false;
        }
        // Also check a few surrounding positions for water/lava
        for (int cx = -2; cx <= 2; cx += 2) {
            for (int cz = -2; cz <= 2; cz += 2) {
                int checkY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX() + cx, origin.getZ() + cz);
                BlockState check = world.getBlockState(new BlockPos(origin.getX() + cx, checkY - 1, origin.getZ() + cz));
                if (check.getFluidState().isIn(FluidTags.WATER) || check.getFluidState().isIn(FluidTags.LAVA)) {
                    return false;
                }
            }
        }

        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int craterRadius = radius + CRATER_EXTRA;
        int craterDepth = radius + 1 + random.nextInt(2);
        // Meteorite sits at the bottom of the crater
        BlockPos meteorCenter = new BlockPos(origin.getX(), surfaceY - craterDepth, origin.getZ());

        // Phase 1: Carve the bowl-shaped crater from the surface downward
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist > craterRadius) continue;

                // Parabolic crater profile: deeper toward center, shallower at edges
                double depthFraction = 1.0 - (horizDist / craterRadius);
                int localDepth = (int) (craterDepth * depthFraction * depthFraction);
                if (localDepth < 1) continue;

                int colSurfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);

                for (int y = colSurfaceY; y >= colSurfaceY - localDepth; y--) {
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.BEDROCK)) continue;
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        // Phase 2: Place the meteorite sphere at the bottom of the crater
        int coreRadius = Math.max(1, radius / 2);
        int placed = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double noisyDist = dist + (random.nextDouble() - 0.5) * 0.8;
                    if (noisyDist > radius) continue;

                    BlockPos pos = meteorCenter.add(dx, dy, dz);
                    BlockState existing = world.getBlockState(pos);
                    if (existing.isOf(Blocks.BEDROCK)) continue;

                    if (noisyDist <= coreRadius) {
                        world.setBlockState(pos, ModBlocks.METEORIC_IRON.getDefaultState(), 2);
                    } else {
                        world.setBlockState(pos, getShellBlock(random), 2);
                    }
                    placed++;
                }
            }
        }

        // Phase 3: Line the crater rim with scorched material
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist < craterRadius - 1.5 || horizDist > craterRadius + 1) continue;

                int rimY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);
                BlockPos rimPos = new BlockPos(origin.getX() + dx, rimY - 1, origin.getZ() + dz);
                BlockState rimState = world.getBlockState(rimPos);
                if (!rimState.isAir() && !rimState.isOf(Blocks.BEDROCK)
                        && !rimState.getFluidState().isIn(FluidTags.WATER)) {
                    world.setBlockState(rimPos, getShellBlock(random), 2);
                }
            }
        }

        return placed > 0;
    }

    private BlockState getShellBlock(Random random) {
        int roll = random.nextInt(10);
        if (roll < 4) return Blocks.STONE.getDefaultState();
        if (roll < 6) return Blocks.COBBLESTONE.getDefaultState();
        if (roll < 8) return Blocks.GRAVEL.getDefaultState();
        return Blocks.DEEPSLATE.getDefaultState();
    }
}
