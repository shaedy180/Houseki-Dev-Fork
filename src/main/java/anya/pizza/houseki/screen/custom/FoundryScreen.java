package anya.pizza.houseki.screen.custom;

import anya.pizza.houseki.Houseki;
import anya.pizza.houseki.block.entity.custom.FoundryBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoundryScreen extends AbstractContainerScreen<FoundryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(Houseki.MOD_ID, "textures/gui/foundry/foundry_gui.png");
    private static final Identifier ARROW_TEXTURE = Identifier.fromNamespaceAndPath(Houseki.MOD_ID, "textures/gui/foundry/foundry_arrow.png");
    private static final Identifier ARROW_TEXTURE2 = Identifier.fromNamespaceAndPath(Houseki.MOD_ID, "textures/gui/foundry/foundry_arrow2.png");
    private static final Identifier MELTING_TEXTURE = Identifier.fromNamespaceAndPath(Houseki.MOD_ID, "textures/gui/foundry/melting_progress.png");
    private static final Identifier FLUID_TEXTURE = Identifier.fromNamespaceAndPath(Houseki.MOD_ID, "textures/gui/foundry/foundry_metal.png");

    public FoundryScreen(FoundryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 176);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 80;
        titleLabelY = -4;
        inventoryLabelY = imageHeight - 104;
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, 176, 176, 256, 256);

        if (menu.isBurning()) {
            int fireHeight = menu.getScaledFuelProgress();
            graphics.blit(RenderPipelines.GUI_TEXTURED, MELTING_TEXTURE, x + 27, y + 36 + (14 - fireHeight), 0, 14 - fireHeight, 14, fireHeight, 14, 14);
        }

        if (menu.getSteelLevel() > 0 || menu.getMeteoricIronLevel() > 0) {
            int activeType = menu.getActiveMetalType();
            int displayLevel = activeType == FoundryBlockEntity.METAL_STEEL
                    ? menu.getSteelLevel() : menu.getMeteoricIronLevel();
            int maxLevel = menu.getMaxMetalLevel();

            if (displayLevel > 0) {
                int scaledFluidHeight = (int) ((float) displayLevel / maxLevel * 43);
                graphics.blit(RenderPipelines.GUI_TEXTURED, FLUID_TEXTURE, x + 80, y + 22 + (43 - scaledFluidHeight), 0, 43 - scaledFluidHeight, 16, scaledFluidHeight, 16, 43);

                // Blue tint overlay for meteoric iron to visually distinguish it
                if (activeType == FoundryBlockEntity.METAL_METEORIC_IRON) {
                    graphics.fill(x + 80, y + 22 + (43 - scaledFluidHeight), x + 96, y + 65, 0x603050B0);
                }
            }
        }

        if (menu.getMeltProgress() > 0) {
            int meltPixelWidth = (int) ((float) menu.getMeltProgress() / menu.getMaxMeltProgress() * 24);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, x + 49, y + 35, 0, 0, meltPixelWidth, 16, 24, 16);
        }

        if (menu.getCastProgress() > 0) {
            int castPixelWidth = (int) ((float) menu.getCastProgress() / menu.getMaxCastTime() * 24);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE2, x + 102, y + 35, 0, 0, castPixelWidth, 16, 24, 16);
        }

        ItemStack coolingStack = menu.getSlot(4).getItem();
        if (!coolingStack.isEmpty()) {
            graphics.item(coolingStack, x + 134, y + 18);

            float coolPercent = (float) menu.getCoolingProgress() / menu.getMaxCoolingProgress();
            int alpha  = (int) ((1.0f - coolPercent) * 100);
            int color = (alpha << 24) | 0xFFFFFF;
            graphics.fill(x + 134, y + 18, x + 134 + 16, y + 18 + 16, color);
        }

        if (menu.getCoolingProgress() > 0 && menu.getMaxCoolingProgress() > 0) {
            int barX = x + 131;
            int barY = y + 37;
            int barWidth = 22;
            int barHeight = 3;
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF404040);
            float barPercent = (float) menu.getCoolingProgress() / menu.getMaxCoolingProgress();
            int fillWidth = (int) (barPercent * barWidth);
            graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFF6BB5FF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        extractTooltip(graphics, mouseX, mouseY);

        //Tooltips for fluid tank
        if (isHovering(80, 21, 16, 45, mouseX, mouseY)) {
            int activeType = menu.getActiveMetalType();
            int steelLvl = menu.getSteelLevel();
            int miLvl = menu.getMeteoricIronLevel();
            int maxLvl = menu.getMaxMetalLevel();

            // Show both metals, mark the active one with an arrow
            String steelLine = (activeType == FoundryBlockEntity.METAL_STEEL ? "\u00A7e\u25B6 " : "  ")
                    + "Molten Steel: " + steelLvl + " / " + maxLvl + " mB";
            String miLine = (activeType == FoundryBlockEntity.METAL_METEORIC_IRON ? "\u00A79\u25B6 " : "  ")
                    + "Meteoric Iron: " + miLvl + " / " + maxLvl + " mB";

            boolean locked = menu.getCastProgress() > 0 || menu.getCoolingProgress() > 0;
            String switchHint = locked ? "\u00A7cLocked during casting" : "\u00A77Click to switch";

            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(steelLine),
                    Component.literal(miLine),
                    Component.literal(switchHint)
            ), mouseX, mouseY);
        }
        if (menu.getCoolingProgress() > 0 && menu.getMaxCoolingProgress() > 0
                && isHovering(130, 17, 24, 26, mouseX, mouseY)) {
            int percent = (int) ((float) menu.getCoolingProgress() / menu.getMaxCoolingProgress() * 100);
            graphics.setTooltipForNextFrame(font, Component.literal("Cooling: " + percent + "%"), mouseX, mouseY);
        }
    }

    // Click on the fluid tank to cycle through metal types
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (isHovering(80, 21, 16, 45, event.x(), event.y())) {
            // Don't allow switching during casting or cooling
            if (menu.getCastProgress() > 0 || menu.getCoolingProgress() > 0) return true;
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }
}