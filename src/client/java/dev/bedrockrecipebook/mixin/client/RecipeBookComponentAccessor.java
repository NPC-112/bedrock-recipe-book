package dev.bedrockrecipebook.mixin.client;

import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("ghostSlots")
	GhostSlots bedrockRecipeBook$ghostSlots();

@Accessor("searchBox")
EditBox bedrockRecipeBook$searchBox();

@Accessor("searchBox")
void bedrockRecipeBook$setSearchBox(EditBox searchBox);
}
