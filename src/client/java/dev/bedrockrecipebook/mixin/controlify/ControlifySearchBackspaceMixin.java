package dev.bedrockrecipebook.mixin.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bedrockrecipebook.client.RecipeSearchFields;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Fill the missing SDL shortcut code, leaving native editing and other text fields unchanged. */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.screenop.compat.vanilla.EditBoxComponentProcessor$EditBoxInputTarget", remap = false)
public abstract class ControlifySearchBackspaceMixin {
    @Shadow @Final private EditBox editBox;

    @ModifyArgs(
        method = "acceptKeyCode",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyEvent;<init>(III)V")
    )
    private void bedrockRecipeBook$supplyBackspaceShortcut(Args args) {
        int key = args.get(0);
        int shortcut = args.get(1);
        if (RecipeSearchFields.contains(this.editBox)
            && key == InputConstants.KEY_BACKSPACE && shortcut == 0) {
            args.set(1, InputConstants.KEYCODE_BACKSPACE);
        }
    }
}
