package anya.pizza.houseki.screen;

import anya.pizza.houseki.block.ModBlocks;
import anya.pizza.houseki.item.ModItems;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GuideBookScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/book.png");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int PAGE_WIDTH = 114;
    private static final int PAGE_TOP = 18;
    private static final int PAGE_LEFT = 36;
    private static final int PAGE_MAX_HEIGHT = 148;

    private final List<Page> pages = new ArrayList<>();
    private int currentPage = 0;

    private ButtonWidget prevButton;
    private ButtonWidget nextButton;

    public GuideBookScreen() {
        super(Text.literal("Houseki Guide"));
    }

    private int bookX;
    private int bookY;

    @Override
    protected void init() {
        super.init();
        bookX = (width - BOOK_WIDTH) / 2;
        bookY = Math.max(2, (height - BOOK_HEIGHT) / 2);

        pages.clear();
        currentPage = Math.min(currentPage, 0);
        buildPages();

        prevButton = addDrawableChild(
                ButtonWidget.builder(Text.literal("<"), b -> flipPage(-1))
                        .dimensions(bookX + 26, bookY + BOOK_HEIGHT + 4, 20, 20)
                        .build()
        );
        nextButton = addDrawableChild(
                ButtonWidget.builder(Text.literal(">"), b -> flipPage(1))
                        .dimensions(bookX + BOOK_WIDTH - 46, bookY + BOOK_HEIGHT + 4, 20, 20)
                        .build()
        );
        updateButtons();
    }

    private void flipPage(int direction) {
        currentPage = Math.max(0, Math.min(pages.size() - 1, currentPage + direction));
        updateButtons();
    }

    private void updateButtons() {
        prevButton.active = currentPage > 0;
        nextButton.active = currentPage < pages.size() - 1;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_LEFT || keyInput.key() == GLFW.GLFW_KEY_PAGE_UP) {
            flipPage(-1);
            return true;
        }
        if (keyInput.key() == GLFW.GLFW_KEY_RIGHT || keyInput.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            flipPage(1);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background darkening + widget rendering (buttons are outside book area)
        super.render(context, mouseX, mouseY, delta);

        // Draw book background on top of darkening
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE,
                bookX, bookY, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, 256, 256);

        // Draw page content clipped to the page area so nothing overflows
        if (currentPage >= 0 && currentPage < pages.size()) {
            int contentX = bookX + PAGE_LEFT;
            int contentY = bookY + PAGE_TOP;
            context.enableScissor(contentX, contentY, contentX + PAGE_WIDTH, contentY + PAGE_MAX_HEIGHT);
            pages.get(currentPage).render(context, textRenderer, contentX, contentY, mouseX, mouseY);
            context.disableScissor();
        }

        // Draw page number at the bottom of the book
        String pageNum = (currentPage + 1) + " / " + pages.size();
        int pageNumWidth = textRenderer.getWidth(pageNum);
        context.drawText(textRenderer, pageNum,
                bookX + PAGE_LEFT + (PAGE_WIDTH - pageNumWidth) / 2, bookY + BOOK_HEIGHT - 16,
                0xFF404040, false);

        // Draw item tooltips after everything else (outside scissor so they show fully)
        if (currentPage >= 0 && currentPage < pages.size()) {
            int contentX = bookX + PAGE_LEFT;
            int contentY = bookY + PAGE_TOP;
            pages.get(currentPage).renderTooltips(context, textRenderer, contentX, contentY, mouseX, mouseY);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ---- Page builder system ----

    private static class Page {
        final List<PageEntry> entries;

        Page(List<PageEntry> entries) {
            this.entries = entries;
        }

        void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
            int cursorY = y;
            for (PageEntry entry : entries) {
                entry.render(ctx, tr, x, cursorY, mouseX, mouseY);
                cursorY += entry.getHeight(tr);
            }
        }

        void renderTooltips(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
            int cursorY = y;
            for (PageEntry entry : entries) {
                entry.renderTooltip(ctx, tr, x, cursorY, mouseX, mouseY);
                cursorY += entry.getHeight(tr);
            }
        }
    }

    private interface PageEntry {
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY);
        default void renderTooltip(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {}
        int getHeight(TextRenderer tr);
    }

    // Title entry
    private static PageEntry title(String text) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                Text t = Text.literal(text).formatted(Formatting.BOLD, Formatting.DARK_PURPLE);
                int w = tr.getWidth(t);
                ctx.drawText(tr, t, x + (PAGE_WIDTH - w) / 2, y, 0xFF5B1A7A, false);
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return 14;
            }
        };
    }

    // Subtitle entry
    private static PageEntry subtitle(String text) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                ctx.drawText(tr, Text.literal(text).formatted(Formatting.BOLD), x, y, 0xFF404040, false);
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return 12;
            }
        };
    }

    // Wrapped text paragraph
    private static PageEntry text(String text) {
        return new PageEntry() {
            private List<OrderedText> cachedLines;

            private List<OrderedText> getLines(TextRenderer tr) {
                if (cachedLines == null) {
                    cachedLines = tr.wrapLines(Text.literal(text), PAGE_WIDTH);
                }
                return cachedLines;
            }

            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                int lineY = y;
                for (OrderedText line : getLines(tr)) {
                    ctx.drawText(tr, line, x, lineY, 0xFF000000, false);
                    lineY += 9;
                }
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return getLines(tr).size() * 9 + 2;
            }
        };
    }

    // Item icon with label (16x16 icon + name beside it)
    private static PageEntry item(ItemStack stack, String description) {
        return new PageEntry() {
            private List<OrderedText> cachedLines;

            private List<OrderedText> getLines(TextRenderer tr) {
                if (cachedLines == null) {
                    cachedLines = tr.wrapLines(Text.literal(description), PAGE_WIDTH - 20);
                }
                return cachedLines;
            }

            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                ctx.drawItem(stack, x, y);
                int textY = y + (16 - 9) / 2; // vertically center first line with icon
                List<OrderedText> lines = getLines(tr);
                for (int i = 0; i < lines.size(); i++) {
                    ctx.drawText(tr, lines.get(i), x + 20, textY + i * 9, 0xFF000000, false);
                }
            }

            @Override
            public void renderTooltip(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    ctx.drawItemTooltip(tr, stack, mouseX, mouseY);
                }
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return Math.max(18, getLines(tr).size() * 9 + 4);
            }
        };
    }

    // Spacer
    private static PageEntry spacer(int px) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {}

            @Override
            public int getHeight(TextRenderer tr) {
                return px;
            }
        };
    }

    // Recipe row: up to 3 input items -> arrow -> output item (for crafting chains)
    private static PageEntry arrow(ItemStack[] inputs, ItemStack output, String label) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                if (label != null && !label.isEmpty()) {
                    ctx.drawText(tr, Text.literal(label).formatted(Formatting.DARK_GRAY), x, y, 0xFF404040, false);
                }
                int rowY = label != null && !label.isEmpty() ? y + 11 : y;
                int curX = x;
                for (ItemStack in : inputs) {
                    ctx.drawItem(in, curX, rowY);
                    curX += 18;
                }
                // Arrow
                ctx.drawText(tr, Text.literal("\u2192"), curX + 2, rowY + 4, 0xFF404040, false);
                curX += 14;
                ctx.drawItem(output, curX, rowY);
            }

            @Override
            public void renderTooltip(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                int rowY = label != null && !label.isEmpty() ? y + 11 : y;
                int curX = x;
                for (ItemStack in : inputs) {
                    if (mouseX >= curX && mouseX < curX + 16 && mouseY >= rowY && mouseY < rowY + 16) {
                        ctx.drawItemTooltip(tr, in, mouseX, mouseY);
                        return;
                    }
                    curX += 18;
                }
                curX += 14;
                if (mouseX >= curX && mouseX < curX + 16 && mouseY >= rowY && mouseY < rowY + 16) {
                    ctx.drawItemTooltip(tr, output, mouseX, mouseY);
                }
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return (label != null && !label.isEmpty() ? 11 : 0) + 20;
            }
        };
    }

    // Crafting grid (3x3 or smaller, + output) with hoverable item tooltips
    private static PageEntry craftingGrid(ItemStack[] grid, ItemStack output, String label) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                if (label != null && !label.isEmpty()) {
                    ctx.drawText(tr, Text.literal(label).formatted(Formatting.DARK_GRAY), x, y, 0xFF404040, false);
                }
                int startY = (label != null && !label.isEmpty()) ? y + 11 : y;

                // Draw grid background slots
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int slotX = x + col * 18;
                        int slotY = startY + row * 18;
                        ctx.fill(slotX, slotY, slotX + 16, slotY + 16, 0x30000000);
                        int idx = row * 3 + col;
                        if (idx < grid.length && !grid[idx].isEmpty()) {
                            ctx.drawItem(grid[idx], slotX, slotY);
                        }
                    }
                }

                // Arrow
                int arrowX = x + 58;
                int arrowY = startY + 18 + 4;
                ctx.drawText(tr, Text.literal("\u2192"), arrowX, arrowY, 0xFF404040, false);

                // Output
                int outX = x + 72;
                int outY = startY + 18;
                ctx.fill(outX, outY, outX + 16, outY + 16, 0x30000000);
                ctx.drawItem(output, outX, outY);
            }

            @Override
            public void renderTooltip(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                int startY = (label != null && !label.isEmpty()) ? y + 11 : y;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int slotX = x + col * 18;
                        int slotY = startY + row * 18;
                        int idx = row * 3 + col;
                        if (idx < grid.length && !grid[idx].isEmpty()
                                && mouseX >= slotX && mouseX < slotX + 16
                                && mouseY >= slotY && mouseY < slotY + 16) {
                            ctx.drawItemTooltip(tr, grid[idx], mouseX, mouseY);
                            return;
                        }
                    }
                }
                int outX = x + 72;
                int outY = startY + 18;
                if (mouseX >= outX && mouseX < outX + 16 && mouseY >= outY && mouseY < outY + 16) {
                    ctx.drawItemTooltip(tr, output, mouseX, mouseY);
                }
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return (label != null && !label.isEmpty() ? 11 : 0) + 56;
            }
        };
    }

    // Item row (multiple items inline with spacing, shows tooltips on hover)
    private static PageEntry itemRow(ItemStack... stacks) {
        return new PageEntry() {
            @Override
            public void render(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                int curX = x;
                for (ItemStack stack : stacks) {
                    ctx.drawItem(stack, curX, y);
                    curX += 18;
                }
            }

            @Override
            public void renderTooltip(DrawContext ctx, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
                int curX = x;
                for (ItemStack stack : stacks) {
                    if (mouseX >= curX && mouseX < curX + 16 && mouseY >= y && mouseY < y + 16) {
                        ctx.drawItemTooltip(tr, stack, mouseX, mouseY);
                        return;
                    }
                    curX += 18;
                }
            }

            @Override
            public int getHeight(TextRenderer tr) {
                return 20;
            }
        };
    }

    // Helper
    private static ItemStack stack(net.minecraft.item.Item item) {
        return new ItemStack(item);
    }

    private static ItemStack[] stacks(net.minecraft.item.Item... items) {
        ItemStack[] arr = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            arr[i] = new ItemStack(items[i]);
        }
        return arr;
    }

    // ---- Build all pages ----

    /** Add entries as one or more pages, auto-splitting when content exceeds PAGE_MAX_HEIGHT. */
    private void addAutoPages(List<PageEntry> entries) {
        List<PageEntry> current = new ArrayList<>();
        int usedHeight = 0;

        for (PageEntry entry : entries) {
            int h = entry.getHeight(textRenderer);
            if (!current.isEmpty() && usedHeight + h > PAGE_MAX_HEIGHT) {
                pages.add(new Page(List.copyOf(current)));
                current.clear();
                usedHeight = 0;
            }
            current.add(entry);
            usedHeight += h;
        }
        if (!current.isEmpty()) {
            pages.add(new Page(List.copyOf(current)));
        }
    }

    private void buildPages() {

        // PAGE 1: Welcome
        addAutoPages(List.of(
                title("Houseki Guide"),
                spacer(4),
                text("Welcome, traveler. This handbook covers the ores, machines, tools and materials added by Houseki."),
                spacer(4),
                text("Hover over any item icon to see its full name and details. Use arrow keys or the buttons to flip pages."),
                spacer(4),
                text("Flip through to learn how everything works, from your first ore to the strongest gear.")
        ));

        // PAGE 2: Early Ores overview with icons
        addAutoPages(List.of(
                title("Early Ores"),
                spacer(2),
                item(stack(ModItems.RAINBOW_PYRITE), "Rainbow Pyrite, a sparkling ore. Makes colorful tools and works as a trim material."),
                item(stack(ModItems.PINKU), "Pinku, a bright pink crystal. Drops shards when mined. Good starting gear."),
                item(stack(ModItems.PINKU_SHARD), "Pinku Shards, combine to get Pinku.")
        ));

        // PAGE 3: Tungsten chain
        addAutoPages(List.of(
                title("Tungsten"),
                spacer(2),
                text("Obtained from two ores. Crush either in the Crusher for Tungsten Powder. Then combine with Nickel Powder."),
                spacer(2),
                item(stack(ModItems.WOLFRAMITE), "Wolframite, crush for Tungsten Powder + 50% Quartz."),
                item(stack(ModItems.SCHEELITE), "Scheelite, crush for Tungsten Powder."),
                arrow(stacks(ModItems.TUNGSTEN_POWDER, ModItems.NICKEL_POWDER), stack(ModItems.TUNGSTEN), "Crafting:"),
                text("All Tungsten gear is fireproof.")
        ));

        // PAGE 4: Aluminum chain
        addAutoPages(List.of(
                title("Aluminum"),
                spacer(2),
                text("Made by processing Bauxite ore, found underground."),
                spacer(2),
                item(stack(ModBlocks.BAUXITE.asItem()), "Bauxite, the raw ore."),
                arrow(stacks(ModBlocks.BAUXITE.asItem()), stack(ModItems.CRUSHED_BAUXITE), "Crush in Crusher:"),
                arrow(stacks(ModItems.CRUSHED_BAUXITE), stack(ModItems.ALUMINUM), "Smelt:"),
                spacer(2),
                text("Also used for Aluminum Glass, Doors and Trapdoors.")
        ));

        // PAGE 5: Gemstones
        addAutoPages(List.of(
                title("Gemstones"),
                spacer(2),
                item(stack(ModItems.SAPPHIRE), "Sapphire, a blue gem. Makes fast, strong tools."),
                item(stack(ModItems.NEPHRITE), "Nephrite, a soft green jade found at depth."),
                item(stack(ModItems.JADEITE), "Jadeite, deeper green jade. A bit stronger than Nephrite.")
        ));

        // PAGE 6: Platinum & Sulfur
        addAutoPages(List.of(
                title("Platinum & Sulfur"),
                spacer(2),
                item(stack(ModItems.PLATINUM), "Platinum, a rare ore. Also crush Copper Ingots for a 3% chance at Platinum Nuggets."),
                item(stack(ModItems.PLATINUM_NUGGET), "Platinum Nugget, 9 make one ingot."),
                spacer(4),
                item(stack(ModItems.SULFUR), "Sulfur, found underground. Burns as fuel. Craft with Sand for TNT."),
                arrow(stacks(ModItems.SULFUR, Items.STICK), stack(Items.TORCH), "Torches:")
        ));

        // PAGE 7: Sugilite & Bismuth
        addAutoPages(List.of(
                title("Sugilite & Bismuth"),
                spacer(2),
                item(stack(ModItems.SUGILITE), "Sugilite, a purple gem. As an armor trim it grants the Sugilite Protection effect, boosting armor toughness."),
                spacer(6),
                item(stack(ModItems.BISMUTH), "Bismuth, a colorful crystal with special properties as a trim material.")
        ));

        // PAGE 8: The Crusher
        addAutoPages(List.of(
                title("The Crusher"),
                spacer(2),
                item(stack(ModBlocks.CRUSHER.asItem()), "A machine that grinds items into new forms."),
                spacer(2),
                text("Place an item in the input slot and provide fuel. Some recipes produce a bonus secondary output with a certain chance.")
        ));

        // PAGE 9: Crusher Recipes
        addAutoPages(List.of(
                title("Crusher Recipes"),
                spacer(2),
                arrow(stacks(ModBlocks.BAUXITE.asItem()), stack(ModItems.CRUSHED_BAUXITE), "Bauxite (+50% Clay):"),
                arrow(stacks(ModItems.WOLFRAMITE), stack(ModItems.TUNGSTEN_POWDER), "Wolframite (+50% Quartz):"),
                arrow(stacks(Items.COPPER_INGOT), stack(ModItems.PLATINUM_NUGGET), "Copper (3% chance):"),
                arrow(stacks(Items.COBBLESTONE), stack(Items.GRAVEL), "Cobblestone:"),
                arrow(stacks(Items.SANDSTONE), stack(Items.SAND), "Sandstone (+20% Calcite):")
        ));

        // PAGE 10: The Foundry
        addAutoPages(List.of(
                title("The Foundry"),
                spacer(2),
                item(stack(ModBlocks.FOUNDRY.asItem()), "A two-step machine for advanced metalwork."),
                spacer(2),
                text("Craft by surrounding a Blast Furnace with Steel on the cardinal sides and Deepslate Bricks on the corners.")
        ));

        // PAGE 11: Foundry crafting recipe
        ItemStack steel = stack(ModItems.STEEL);
        ItemStack dsb = stack(Items.DEEPSLATE_BRICKS);
        ItemStack bf = stack(Items.BLAST_FURNACE);
        addAutoPages(List.of(
                subtitle("Foundry Recipe"),
                craftingGrid(new ItemStack[]{
                        dsb, steel, dsb,
                        steel, bf, steel,
                        dsb, steel, dsb
                }, stack(ModBlocks.FOUNDRY.asItem()), null),
                spacer(4),
                text("The Foundry melts metal ingots and then pours molten metal into casts to produce tool and armor components.")
        ));

        // PAGE 12: Steel Chain
        addAutoPages(List.of(
                title("Steel Chain"),
                spacer(2),
                text("Producing steel takes two smelting steps."),
                spacer(4),
                arrow(stacks(Items.IRON_INGOT), stack(ModItems.CRUDE_IRON), "1. Blast Iron Ingot:"),
                arrow(stacks(ModItems.CRUDE_IRON), stack(ModItems.STEEL), "2. Blast Crude Iron:"),
                spacer(4),
                text("Steel is used to craft Cast molds and the Foundry. Melt it in the Foundry to produce Cast Steel gear.")
        ));

        // PAGE 13: Making Casts
        addAutoPages(List.of(
                title("Making Casts"),
                spacer(2),
                text("Surround a wooden tool (or iron armor piece) with 8 Steel ingots:"),
                craftingGrid(new ItemStack[]{
                        steel, steel, steel,
                        steel, stack(Items.WOODEN_PICKAXE), steel,
                        steel, steel, steel
                }, stack(ModItems.PICKAXE_HEAD_CAST), "Example (Pickaxe Cast):")
        ));

        // PAGE 14: Available Casts
        addAutoPages(List.of(
                title("Available Casts"),
                spacer(2),
                text("All casts that can be crafted and used in the Foundry:"),
                spacer(4),
                itemRow(stack(ModItems.INGOT_CAST), stack(ModItems.PICKAXE_HEAD_CAST), stack(ModItems.AXE_HEAD_CAST),
                        stack(ModItems.SHOVEL_HEAD_CAST), stack(ModItems.SWORD_HEAD_CAST), stack(ModItems.HOE_HEAD_CAST)),
                itemRow(stack(ModItems.SPEAR_HEAD_CAST), stack(ModItems.HELMET_CAST), stack(ModItems.CHESTPLATE_CAST),
                        stack(ModItems.LEGGINGS_CAST), stack(ModItems.BOOTS_CAST)),
                spacer(4),
                text("Place a cast into the Foundry along with molten metal to produce Cast Steel or Meteoric Iron parts.")
        ));

        // PAGE 15: Cast Steel tools
        addAutoPages(List.of(
                title("Cast Steel Tools"),
                spacer(2),
                text("Assemble Cast Steel tool heads with Sticks on a crafting table:"),
                spacer(4),
                itemRow(stack(ModItems.CS_PICKAXE_HEAD), stack(ModItems.CS_AXE_HEAD), stack(ModItems.CS_SHOVEL_HEAD),
                        stack(ModItems.CS_SWORD_HEAD), stack(ModItems.CS_HOE_HEAD), stack(ModItems.CS_SPEAR_HEAD)),
                spacer(4),
                itemRow(stack(ModItems.CAST_STEEL_PICKAXE), stack(ModItems.CAST_STEEL_AXE), stack(ModItems.CAST_STEEL_SHOVEL),
                        stack(ModItems.CAST_STEEL_SWORD), stack(ModItems.CAST_STEEL_HOE), stack(ModItems.CAST_STEEL_SPEAR))
        ));

        // PAGE 16: Meteoric Iron
        addAutoPages(List.of(
                title("Meteoric Iron"),
                spacer(2),
                item(stack(ModItems.METEORIC_IRON_INGOT), "Found inside rare meteorite craters in the Overworld. Very tough ore blocks."),
                spacer(2),
                text("Process it through the Foundry just like Cast Steel, but switch the Foundry to Meteoric Iron mode."),
                spacer(4),
                itemRow(stack(ModItems.MI_PICKAXE_HEAD), stack(ModItems.MI_AXE_HEAD), stack(ModItems.MI_SHOVEL_HEAD),
                        stack(ModItems.MI_SWORD_HEAD), stack(ModItems.MI_HOE_HEAD), stack(ModItems.MI_SPEAR_HEAD))
        ));

        // PAGE 17: Meteoric Iron tools
        addAutoPages(List.of(
                subtitle("Meteoric Iron Tools"),
                spacer(4),
                itemRow(stack(ModItems.METEORIC_IRON_PICKAXE), stack(ModItems.METEORIC_IRON_AXE), stack(ModItems.METEORIC_IRON_SHOVEL),
                        stack(ModItems.METEORIC_IRON_SWORD), stack(ModItems.METEORIC_IRON_HOE), stack(ModItems.METEORIC_IRON_SPEAR)),
                spacer(4),
                text("Assemble Meteoric Iron heads with Sticks just like Cast Steel tools."),
                spacer(4),
                itemRow(stack(ModItems.METEORIC_IRON_HELMET), stack(ModItems.METEORIC_IRON_CHESTPLATE),
                        stack(ModItems.METEORIC_IRON_LEGGINGS), stack(ModItems.METEORIC_IRON_BOOTS)),
                spacer(2),
                text("Armor is also cast using the helmet, chestplate, leggings and boots casts.")
        ));

        // PAGE 18: Drills overview
        addAutoPages(List.of(
                title("Drills"),
                spacer(2),
                text("Advanced mining tools. Each needs a Drill Bit and a Drill Head, assembled together (shapeless)."),
                spacer(4),
                item(stack(ModItems.TUNGSTEN_DRILL_BIT), "Tungsten Drill Bit, crafted from Tungsten + a Block of Tungsten."),
                item(stack(ModItems.DIAMOND_DRILL_BIT), "Diamond Drill Bit, crafted from 3 Diamond Blocks.")
        ));

        // PAGE 19: Drill Heads & Upgrades
        addAutoPages(List.of(
                subtitle("Drill Heads"),
                spacer(2),
                itemRow(stack(ModItems.SIMPLE_DRILL_HEAD), stack(ModItems.ENHANCED_DRILL_HEAD),
                        stack(ModItems.ADVANCED_DRILL_HEAD), stack(ModItems.PREMIUM_DRILL_HEAD)),
                spacer(2),
                text("Upgrade heads at a Smithing Table using a Drill Upgrade Template and a Block of Cast Steel."),
                spacer(2),
                item(stack(ModItems.DRILL_UPGRADE_SMITHING_TEMPLATE), "Drill Upgrade Template"),
                spacer(2),
                text("Enhanced drills mine a 3x3 area. Advanced and Premium drills mine even larger areas.")
        ));

        // PAGE 20: Assembled Drills
        addAutoPages(List.of(
                subtitle("Assembled Drills"),
                spacer(2),
                text("Tungsten Drills:"),
                itemRow(stack(ModItems.SIMPLE_TUNGSTEN_DRILL), stack(ModItems.ENHANCED_TUNGSTEN_DRILL),
                        stack(ModItems.ADVANCED_TUNGSTEN_DRILL), stack(ModItems.PREMIUM_TUNGSTEN_DRILL)),
                spacer(2),
                text("Diamond Drills:"),
                itemRow(stack(ModItems.SIMPLE_DIAMOND_DRILL), stack(ModItems.ENHANCED_DIAMOND_DRILL),
                        stack(ModItems.ADVANCED_DIAMOND_DRILL), stack(ModItems.PREMIUM_DIAMOND_DRILL))
        ));

        // PAGE 21: Armor Trims
        addAutoPages(List.of(
                title("Armor Trims"),
                spacer(2),
                text("8 new trim materials. Apply at a Smithing Table with any trim template:"),
                spacer(4),
                itemRow(stack(ModItems.RAINBOW_PYRITE), stack(ModItems.PINKU), stack(ModItems.SAPPHIRE),
                        stack(ModItems.NEPHRITE)),
                itemRow(stack(ModItems.JADEITE), stack(ModItems.CAST_STEEL), stack(ModItems.SUGILITE),
                        stack(ModItems.BISMUTH)),
                spacer(4),
                text("Sugilite trim grants added armor toughness. Bismuth trim also has a special property.")
        ));

        // PAGE 22: Building Blocks
        addAutoPages(List.of(
                title("Building Blocks"),
                spacer(2),
                item(stack(ModBlocks.LIMESTONE.asItem()), "Limestone, a light stone with brick, polished and chiseled variants."),
                item(stack(ModBlocks.SLATE.asItem()), "Slate, a dark stone with tile, polished and chiseled variants."),
                spacer(2),
                text("Both have stairs, slabs and walls. Use a Stonecutter for easy shaping."),
                spacer(4),
                item(stack(ModBlocks.ALUMINUM_GLASS.asItem()), "Aluminum Glass, translucent."),
                item(stack(ModBlocks.ALUMINUM_DOOR.asItem()), "Aluminum Door and Trapdoor.")
        ));

        // PAGE 23: World Generation
        addAutoPages(List.of(
                title("World Generation"),
                spacer(2),
                text("All Houseki ores spawn naturally in stone and deepslate throughout the Overworld."),
                spacer(4),
                subtitle("Meteorites"),
                text("Rare impact craters on the surface. They contain Meteoric Iron at the center. Keep an eye out while exploring!"),
                spacer(4),
                item(stack(ModBlocks.METEORIC_IRON.asItem()), "Meteoric Iron ore, found only in meteorite craters.")
        ));

        // PAGE 24: Progression
        addAutoPages(List.of(
                title("Progression"),
                spacer(2),
                text("A suggested path through Houseki:"),
                spacer(4),
                text("1. Mine early ores (Pinku, Rainbow Pyrite, Aluminum, Sapphire)"),
                text("2. Craft a Crusher"),
                text("3. Process Bauxite, Wolframite, and other ores"),
                text("4. Blast Iron into Crude Iron, then into Steel"),
                text("5. Build a Foundry"),
                text("6. Craft casts and make Cast Steel gear"),
                text("7. Find a meteorite crater"),
                text("8. Cast Meteoric Iron gear"),
                text("9. Upgrade to Drills"),
                spacer(4),
                text("Good luck out there.")
        ));
    }
}
