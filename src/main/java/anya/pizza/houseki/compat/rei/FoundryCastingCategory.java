package anya.pizza.houseki.compat.rei;

import anya.pizza.houseki.Houseki;
import anya.pizza.houseki.block.ModBlocks;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.text.Text;

import java.util.LinkedList;
import java.util.List;

public class FoundryCastingCategory implements DisplayCategory<BasicDisplay> {
    public static final CategoryIdentifier<FoundryCastingDisplay> FOUNDRY_CASTING =
            CategoryIdentifier.of(Houseki.MOD_ID, "foundry_casting");

    @Override
    public CategoryIdentifier<? extends BasicDisplay> getCategoryIdentifier() {
        return FOUNDRY_CASTING;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("category.houseki.foundry_casting");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModBlocks.FOUNDRY.asItem().getDefaultStack());
    }

    @Override
    public List<Widget> setupDisplay(BasicDisplay display, Rectangle bounds) {
        Point startPoint = new Point(bounds.getCenterX() - 41, bounds.getCenterY() - 13);
        List<Widget> widgets = new LinkedList<>();

        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createSlot(new Point(startPoint.x, startPoint.y)).entries(display.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 24, startPoint.y)).animationDurationTicks(200));
        widgets.add(Widgets.createResultSlotBackground(new Point(startPoint.x + 61, startPoint.y)));
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 61, startPoint.y)).entries(display.getOutputEntries().get(0)).disableBackground().markOutput());

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 36;
    }
}
