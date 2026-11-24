package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.client.SearchInputDiagnostics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes Controlify's unmodified native handler; does not replace or consume input. */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.screenop.compat.vanilla.EditBoxComponentProcessor$EditBoxInputTarget", remap = false)
public abstract class ControlifySearchInputDiagnosticsMixin {
    @Shadow @Final private EditBox editBox;
    @Unique private static final ThreadLocal<Integer> bedrockRecipeBook$previousLength = new ThreadLocal<>();

    @Inject(method = "acceptChar", at = @At("HEAD"))
    private void bedrockRecipeBook$beforeCharacter(char character, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        bedrockRecipeBook$previousLength.set(this.editBox.getValue().length());
    }

    @Inject(method = "acceptChar", at = @At("RETURN"))
    private void bedrockRecipeBook$afterCharacter(char character, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Integer previousLength = bedrockRecipeBook$previousLength.get();
        bedrockRecipeBook$previousLength.remove();
        SearchInputDiagnostics.nativeCharacterResult(this.editBox, previousLength == null ? -1 : previousLength);
    }
}
