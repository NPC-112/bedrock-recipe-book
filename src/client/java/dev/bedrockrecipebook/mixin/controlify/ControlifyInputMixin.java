package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.compat.controlify.BedrockRecipeBookControls;
import dev.bedrockrecipebook.client.SearchInputDiagnostics;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.isxander.controlify.screenop.ScreenProcessor", remap = false)
public abstract class ControlifyInputMixin {
	@Shadow @Final public Screen screen;

@Inject(method = "tryOpenKeyboard", at = @At("HEAD"))
private void bedrockRecipeBook$observeKeyboardOpen(
ControllerEntity controller, GuiEventListener target, CallbackInfoReturnable<Boolean> cir
) {
var book = BedrockRecipeBookControls.component(this.screen);
if (book != null && (target == book || target == book.searchBoxForController())) {
SearchInputDiagnostics.keyboardOpening(book);
}
}

@Inject(method = "tryOpenKeyboard", at = @At("RETURN"))
private void bedrockRecipeBook$observeKeyboardReady(
ControllerEntity controller, GuiEventListener target, CallbackInfoReturnable<Boolean> cir
) {
var book = BedrockRecipeBookControls.component(this.screen);
if (book != null && (target == book || target == book.searchBoxForController())) {
SearchInputDiagnostics.lifecycle(book, "native keyboard open returned");
}
}

	@Inject(method = "handleComponentNavOverride", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$navigate(ControllerEntity controller, CallbackInfoReturnable<Boolean> cir) {
		var book = BedrockRecipeBookControls.component(this.screen);
		if (book != null) {
			BedrockRecipeBookControls.navigate(book, controller);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "handleComponentButtonOverride", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$buttons(ControllerEntity controller, CallbackInfoReturnable<Boolean> cir) {
		var book = BedrockRecipeBookControls.component(this.screen);
		if (book != null) {
			BedrockRecipeBookControls.buttons(
				book, controller, this.screen, (ScreenProcessor<?>) (Object) this
			);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "handleTabNavigation", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$tabs(ControllerEntity controller, CallbackInfo ci) {
		var book = BedrockRecipeBookControls.component(this.screen);
		if (book != null) {
			BedrockRecipeBookControls.tabs(book, controller);
			ci.cancel();
		}
	}
}
