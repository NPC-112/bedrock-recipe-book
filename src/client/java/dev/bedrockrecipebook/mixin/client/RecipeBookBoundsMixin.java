package dev.bedrockrecipebook.mixin.client;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookBoundsMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void bedrockRecipeBook$syncContainerBounds(
		GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callback
	) {
		RecipeBookComponent<?> component =
			((RecipeBookScreenAccessor) this).bedrockRecipeBook$getRecipeBookComponent();
		if (component instanceof BedrockCraftingRecipeBookComponent bedrockComponent) {
			ContainerScreenAccessor screen = (ContainerScreenAccessor) this;
			bedrockComponent.setContainerBounds(
				screen.bedrockRecipeBook$getLeftPos(),
				screen.bedrockRecipeBook$getTopPos(),
				screen.bedrockRecipeBook$getImageWidth(),
				screen.bedrockRecipeBook$getImageHeight()
			);
		}
	}
}
