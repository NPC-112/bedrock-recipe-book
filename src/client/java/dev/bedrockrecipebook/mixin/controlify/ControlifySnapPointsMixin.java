package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.isxander.controlify.virtualmouse.SnapUtils", remap = false)
public abstract class ControlifySnapPointsMixin {
	@Inject(method = "addRecipeSnapPoints", at = @At("HEAD"), cancellable = true)
	private static void bedrockRecipeBook$customPoints(
		RecipeBookComponent<?> component, Consumer<SnapPoint> consumer, CallbackInfo ci
	) {
		if (component instanceof BedrockCraftingRecipeBookComponent book) {
			if (book.isVisible()) {
				for (var point : book.navigationPoints()) {
					consumer.accept(new SnapPoint(point.x(), point.y(), point.radius()));
				}
			}
			ci.cancel();
		}
	}
}
