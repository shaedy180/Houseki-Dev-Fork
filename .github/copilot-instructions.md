# Houseki Mod - AI Coding Instructions

## Project Overview

- Fabric mod "Houseki" for Minecraft, Java 21
- Gradle + fabric-loom build system
- Fork: `shaedy180/Houseki-Dev-Fork`, upstream: `AnyaPizza/Houseki`
- Multiple MC version branches (1.21.11, 26.1-new-update, etc.)

## Build Environment

- JAVA_HOME must be set before every Gradle command:
  `$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-25"`
- Build: `.\gradlew build`
- Datagen: `.\gradlew runDatagen`
- Compile check: `.\gradlew compileJava`

## Workflow: Before Every Commit

Follow this checklist strictly, in order. Do not skip steps.

1. **Compile check** - Run `.\gradlew compileJava` and fix all errors before proceeding.
2. **Datagen** - If any worldgen, recipe, model, tag, or blockstate code was changed, run `.\gradlew runDatagen` and verify it completes without errors.
3. **Full build** - Run `.\gradlew build` to produce the jar and confirm no failures.
4. **Review changes** - Run `git diff --stat` and `git diff` on modified Java files to verify nothing unintended was changed.
5. **Stage only intended files** - Use `git add src/` or specific paths. Never blindly `git add .` (avoid committing build artifacts, name clash dirs, IDE files).
6. **Commit** - See commit message rules below.
7. **Push** - Push to origin after commit.

## Commit Message Rules

- Write short, lowercase subject lines (50 chars max preferred)
- No emojis, no em dashes, no markdown formatting
- Use imperative mood: "fix X" not "fixed X" or "fixes X"
- First line is the summary. Add a blank line then bullet points for details if needed.
- Keep it human and concise. Examples:
  - `fix foundry screen rendering for MC 26.1`
  - `add missing meteoric iron textures`
  - `update placed features to fix duplicate registration`
  - `remove deprecated BlockRenderLayerMap calls`

## MC API Differences (26.1 vs 1.21.11)

The 26.1 branch uses mojmap/intermediary names. Key mappings from yarn (1.21.11):

### Rendering / Screens
- `DrawContext` -> `GuiGraphicsExtractor`
- `drawBackground(DrawContext, float, int, int)` -> `extractMenuBackground(GuiGraphicsExtractor)`
- `render(DrawContext, int, int, float)` -> `extractRenderState(GuiGraphicsExtractor, int, int, float)`
- `mouseClicked(Click, boolean)` -> `mouseClicked(MouseButtonEvent, boolean)`
- `handler` -> `menu`, `backgroundWidth/Height` -> `imageWidth/imageHeight`
- `drawTexture(...)` -> `graphics.blit(RenderPipelines.GUI_TEXTURED, ...)`
- `drawItem(stack, x, y)` -> `graphics.item(stack, x, y)`
- `drawTooltip(textRenderer, list, x, y)` -> `graphics.setComponentTooltipForNextFrame(font, list, x, y)`
- `Text.literal(...)` -> `Component.literal(...)`
- `textRenderer` -> `font`, `client` -> `minecraft`
- `interactionManager` -> `gameMode`
- `clickButton(syncId, id)` -> `handleInventoryButtonClick(containerId, id)`
- `isPointWithinBounds(...)` -> `isHovering(...)`
- `Slot.getStack()` -> `Slot.getItem()`

### Blocks / World
- `BlockState.isOf(block)` -> `BlockState.is(block)`
- `Block.getDefaultState()` -> `Block.defaultBlockState()`
- `world.setBlockState(pos, state, flags)` -> `level.setBlock(pos, state, flags)`
- `BlockPos.up()/down()` -> `BlockPos.above()/below()`
- `BlockPos.add(dx,dy,dz)` -> `BlockPos.offset(dx,dy,dz)`
- `BlockBox.contains(pos)` -> `BoundingBox.isInside(pos)`
- `world.getTopY(...)` / `world.getMaxY(...)` -> `level.getHeight(Heightmap.Types.X, x, z)`

### Render Layers
- `BlockRenderLayerMap` removed in Fabric API v23+
- Use `"force_translucent": true` in model JSON textures for translucent blocks
- Doors/trapdoors inherit cutout from vanilla parent models
- `RenderType.translucent()/cutout()` -> `ChunkSectionLayer.TRANSLUCENT/CUTOUT` (if needed elsewhere)

### Recipes
- `ItemStack.CODEC` for items -> `BuiltInRegistries.ITEM.byNameCodec()`
- `RecipeSerializer` is now a record: `new RecipeSerializer<>(mapCodec, streamCodec)`
- `RecipeBuilder.getResult()` no longer exists in the interface

## CI/CD

- `build.yml` runs on every push and PR, uploads jar artifacts
- `release.yml` creates a GitHub Release with the jar on every push to main branches
- Workflow files (.github/) are fork-specific. Do not include them in upstream PRs.

## Branch Management

- Feature work targeting upstream should branch from the upstream base commit
- CI/workflow commits are fork-only and must not be included in PRs to upstream (`AnyaPizza/Houseki`)
- Always rebase fork-only commits to the tip when preparing upstream PRs

## General Rules

- Ask questions via the vscode_askQuestions tool when intent is unclear
- Do not add unnecessary comments, docstrings, or type annotations to unchanged code
- Do not over-engineer. Only change what is needed.
- Prefer editing existing files over creating new ones
- Run the full build validation checklist before every commit
