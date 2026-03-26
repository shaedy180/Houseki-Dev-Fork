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
    private static final int MIN_RADIUS = 5;
    private static final int MAX_RADIUS = 8;
    private static final int CRATER_EXTRA = 4;

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
        int craterDepth = radius + 2 + random.nextInt(3);
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

        // Phase 3: Scatter meteoric iron debris on the crater floor
        int debrisCount = 2 + random.nextInt(4); // 2-5 pieces
        for (int i = 0; i < debrisCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = (1.5 + random.nextDouble() * (craterRadius - 2));
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);

            // Find the new surface after the crater was carved
            int debrisX = origin.getX() + dx;
            int debrisZ = origin.getZ() + dz;
            for (int y = surfaceY; y >= meteorCenter.getY(); y--) {
                BlockPos check = new BlockPos(debrisX, y, debrisZ);
                BlockPos above = check.up();
                if (!world.getBlockState(check).isAir() && world.getBlockState(above).isAir()) {
                    world.setBlockState(above, ModBlocks.METEORIC_IRON.getDefaultState(), 2);
                    break;
                }
            }
        }

        // Phase 4: Line the crater rim with scorched ejecta
        for (int dx = -craterRadius - 1; dx <= craterRadius + 1; dx++) {
            for (int dz = -craterRadius - 1; dz <= craterRadius + 1; dz++) {
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (horizDist < craterRadius - 2 || horizDist > craterRadius + 1.5) continue;

                int rimY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);
                BlockPos rimPos = new BlockPos(origin.getX() + dx, rimY - 1, origin.getZ() + dz);
                BlockState rimState = world.getBlockState(rimPos);
                if (!rimState.isAir() && !rimState.isOf(Blocks.BEDROCK)
                        && !rimState.getFluidState().isIn(FluidTags.WATER)) {
                    world.setBlockState(rimPos, getShellBlock(random), 2);
                }
                // Raised rim: occasionally place a block on top of the rim
                if (horizDist >= craterRadius - 0.5 && horizDist <= craterRadius + 0.5
                        && random.nextInt(3) == 0) {
                    BlockPos aboveRim = new BlockPos(origin.getX() + dx, rimY, origin.getZ() + dz);
                    if (world.getBlockState(aboveRim).isAir()) {
                        world.setBlockState(aboveRim, getShellBlock(random), 2);
                    }
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
