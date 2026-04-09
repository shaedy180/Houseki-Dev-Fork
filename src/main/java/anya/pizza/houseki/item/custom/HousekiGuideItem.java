package anya.pizza.houseki.item.custom;

import anya.pizza.houseki.screen.GuideBookScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class HousekiGuideItem extends Item {

    public HousekiGuideItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            openScreen();
        }
        return ActionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private void openScreen() {
        MinecraftClient.getInstance().setScreen(new GuideBookScreen());
    }
}
