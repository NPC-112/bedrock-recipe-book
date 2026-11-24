package dev.bedrockrecipebook.mixin.client;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class RecipeBookMouseScrollMixin {
	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$scrollCustomList(
		double mouseX,
		double mouseY,
		double horizontal,
		double vertical,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!((Object) this instanceof AbstractRecipeBookScreen<?> screen)) {
			return;
		}

		RecipeBookComponent<?> recipeBook =
			((RecipeBookScreenAccessor) screen).bedrockRecipeBook$getRecipeBookComponent();
		if (recipeBook instanceof BedrockCraftingRecipeBookComponent custom
			&& custom.isVisible()
			&& custom.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
			cir.setReturnValue(true);
		}
	}
}
