package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.compat.controlify.BedrockRecipeBookControls;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.isxander.controlify.virtualmouse.VirtualMouseHandler", remap = false)
public abstract class ControlifySnapMovementMixin {
	@Inject(method = "snapInDirection", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$scrollGrid(ScreenDirection direction, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		var book = BedrockRecipeBookControls.component(client.gui.screen());
		if (book == null) {
			return;
		}
		var mouse = (VirtualMouseHandler) (Object) this;
		var window = client.getWindow();
		double scaleX = (double) window.getGuiScaledWidth() / window.getScreenWidth();
		double scaleY = (double) window.getGuiScaledHeight() / window.getScreenHeight();
double guiX = mouse.getCurrentX(1) * scaleX;
double guiY = mouse.getCurrentY(1) * scaleY;
		int dx = direction == ScreenDirection.LEFT ? -1 : direction == ScreenDirection.RIGHT ? 1 : 0;
		int dy = direction == ScreenDirection.UP ? -1 : direction == ScreenDirection.DOWN ? 1 : 0;
var point = book.navigateControlAt(guiX, guiY, dx, dy);
if (point == null) {
point = book.navigateRecipeAt(guiX, guiY, dx, dy);
}
		if (point != null) {
			mouse.snapToPoint(new SnapPoint(point.x(), point.y(), point.radius()), new Vector2d(scaleX, scaleY));
			ci.cancel();
return;
}
boolean heldUp = direction == ScreenDirection.UP
&& Controlify.instance().getCurrentController()
.map(controller -> ControlifyBindings.VMOUSE_SNAP_UP.on(controller).digitalPrev())
.orElse(false);
if (heldUp && book.isAtTopOfRecipeList(guiX, guiY)) {
ci.cancel();
return;
}
boolean heldDown = direction == ScreenDirection.DOWN
&& Controlify.instance().getCurrentController()
.map(controller -> ControlifyBindings.VMOUSE_SNAP_DOWN.on(controller).digitalPrev())
.orElse(false);
if (heldDown && book.isAtBottomOfRecipeList(guiX, guiY)) {
ci.cancel();
		}
	}
}
