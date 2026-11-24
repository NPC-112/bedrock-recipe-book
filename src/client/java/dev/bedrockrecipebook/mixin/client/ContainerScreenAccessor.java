package dev.bedrockrecipebook.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int bedrockRecipeBook$getLeftPos();

	@Accessor("topPos")
	int bedrockRecipeBook$getTopPos();

	@Accessor("imageWidth")
	int bedrockRecipeBook$getImageWidth();

	@Accessor("imageHeight")
	int bedrockRecipeBook$getImageHeight();
}
