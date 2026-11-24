package dev.bedrockrecipebook.mixin.client;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Redirect(
		method = "<init>",
		at = @At(
			value = "NEW",
			target = "Lnet/minecraft/client/gui/screens/recipebook/CraftingRecipeBookComponent;"
		)
	)
	private static CraftingRecipeBookComponent bedrockRecipeBook$replaceRecipeBook(AbstractCraftingMenu menu) {
		return new BedrockCraftingRecipeBookComponent(menu);
	}
}
