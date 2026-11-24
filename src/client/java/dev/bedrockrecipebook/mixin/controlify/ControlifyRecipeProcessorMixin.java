package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.compat.controlify.BedrockRecipeBookControls;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.isxander.controlify.screenop.compat.vanilla.RecipeBookScreenProcessor", remap = false)
public abstract class ControlifyRecipeProcessorMixin {
	// Preserve the superclass inventory-slot handling, but never click the hidden vanilla book controls.
	@Inject(method = "handleScreenVMouse", at = @At(value = "INVOKE",
		target = "Ldev/isxander/controlify/screenop/compat/vanilla/AbstractContainerScreenProcessor;handleScreenVMouse(Ldev/isxander/controlify/controller/ControllerEntity;Ldev/isxander/controlify/virtualmouse/VirtualMouseHandler;)V",
		shift = At.Shift.AFTER), cancellable = true)
	private void bedrockRecipeBook$skipVanillaBook(
		ControllerEntity controller, VirtualMouseHandler mouse, CallbackInfo ci
	) {
		if (BedrockRecipeBookControls.component(((ScreenProcessor<?>) (Object) this).screen) != null) {
			ci.cancel();
		}
	}
}
