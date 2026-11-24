package dev.bedrockrecipebook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/**
 * Sends recipe selection through Minecraft's native, server-validated placement path.
 *
 * <p>This requests filling the open crafting grid. Output collection is a separate vanilla
 * quick-move interaction so crafted items are deposited into inventory rather than the cursor.</p>
 */
public final class RecipePlacementBridge {
	private RecipePlacementBridge() {
	}

	public static PlacementResult requestPlacement(
		Minecraft client,
		AbstractCraftingMenu menu,
		RecipeDisplayId recipeDisplayId
	) {
		if (client.player == null) {
			return PlacementResult.NO_PLAYER;
		}
		if (client.gameMode == null) {
			return PlacementResult.NO_GAME_MODE;
		}
		if (client.player.containerMenu != menu) {
			return PlacementResult.MENU_NOT_OPEN;
		}

		client.gameMode.handlePlaceRecipe(menu.containerId, recipeDisplayId, false);
		return PlacementResult.REQUESTED;
	}

	public enum PlacementResult {
		REQUESTED,
		NO_PLAYER,
		NO_GAME_MODE,
		MENU_NOT_OPEN
	}
}
