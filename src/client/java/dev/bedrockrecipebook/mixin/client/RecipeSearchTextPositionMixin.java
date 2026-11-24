package dev.bedrockrecipebook.mixin.client;

import dev.bedrockrecipebook.client.RecipeSearchFields;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla text centered while the widget bounds match the entire search row. */
@Mixin(EditBox.class)
public abstract class RecipeSearchTextPositionMixin {
    @Shadow private int textY;

    @Inject(method = "updateTextPosition", at = @At("RETURN"))
    private void bedrockRecipeBook$centerSearchText(CallbackInfo ci) {
        EditBox box = (EditBox) (Object) this;
        if (RecipeSearchFields.contains(box)) {
            this.textY = box.getY() + (box.getHeight() - 9) / 2;
        }
    }
}
