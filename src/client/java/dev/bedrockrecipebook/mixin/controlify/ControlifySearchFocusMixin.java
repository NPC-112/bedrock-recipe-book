package dev.bedrockrecipebook.mixin.controlify;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.CustomFocus;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the nested vanilla widget without making it the screen's direct focus owner.
 * All text-input behavior, including focus preservation, remains Controlify's own.
 */
@Mixin(value = BedrockCraftingRecipeBookComponent.class, remap = false)
public abstract class ControlifySearchFocusMixin implements CustomFocus, ComponentProcessorProvider {
    @Shadow public abstract boolean isSearchInputFocused();
    @Shadow public abstract EditBox searchBoxForController();

    @Override
    public GuiEventListener getCustomFocus() {
        return this.isSearchInputFocused() ? this.searchBoxForController() : null;
    }

    @Override
    public ComponentProcessor componentProcessor() {
        return this.isSearchInputFocused()
            ? ComponentProcessorProvider.provide(this.searchBoxForController())
            : ComponentProcessor.EMPTY;
    }
}
