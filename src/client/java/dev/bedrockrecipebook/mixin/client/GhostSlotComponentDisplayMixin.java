package dev.bedrockrecipebook.mixin.client;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.GhostSlots$GhostSlot")
public abstract class GhostSlotComponentDisplayMixin {
	@Shadow
	@Final
	private List<ItemStack> items;

	/** Prefer a data-component-bearing stack when a recipe offers it beside a plain item variant. */
	@Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
	private void bedrockRecipeBook$preferComponentStack(int selectedIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (this.items.size() < 2) {
			return;
		}

		ItemStack baseItem = this.items.getFirst();
		ItemStack preferred = baseItem;
		int preferredComponentCount = baseItem.getComponentsPatch().size();
		for (int i = 1; i < this.items.size(); i++) {
			ItemStack candidate = this.items.get(i);
			if (!ItemStack.isSameItem(baseItem, candidate)) {
				return;
			}

			int componentCount = candidate.getComponentsPatch().size();
			if (componentCount > preferredComponentCount) {
				preferred = candidate;
				preferredComponentCount = componentCount;
			}
		}

		if (preferred != baseItem) {
			cir.setReturnValue(preferred);
		}
	}
}
