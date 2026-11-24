package dev.bedrockrecipebook.compat.controlify;

import dev.bedrockrecipebook.client.BedrockCraftingRecipeBookComponent;
import dev.bedrockrecipebook.mixin.client.RecipeBookScreenAccessor;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;

/** Called from the processor Controlify actually installs on vanilla recipe-book screens. */
public final class BedrockRecipeBookControls {
	private BedrockRecipeBookControls() {
	}

	public static BedrockCraftingRecipeBookComponent component(Screen screen) {
		if (!(screen instanceof AbstractRecipeBookScreen<?>)) {
			return null;
		}
		var component = ((RecipeBookScreenAccessor) screen).bedrockRecipeBook$getRecipeBookComponent();
		return component instanceof BedrockCraftingRecipeBookComponent custom && custom.isVisible()
			? custom : null;
	}

	public static void navigate(BedrockCraftingRecipeBookComponent book, ControllerEntity controller) {
		if (book.isSearchInputFocused()) {
if (ControlifyBindings.GUI_NAVI_UP.on(controller).justPressed()) {
book.controllerExitSearchUp();
} else if (ControlifyBindings.GUI_NAVI_DOWN.on(controller).justPressed()) {
book.controllerExitSearchDown();
			}
			return;
		}
		if (ControlifyBindings.GUI_NAVI_UP.on(controller).justPressed()
			&& book.isSearchTabSelected()
			&& book.isSelectionAtTopOfRecipeList()) {
			book.controllerFocusSearch();
		} else if (ControlifyBindings.GUI_NAVI_LEFT.on(controller).justPressed()) {
			book.controllerMoveSelection(-1, 0);
		} else if (ControlifyBindings.GUI_NAVI_RIGHT.on(controller).justPressed()) {
			book.controllerMoveSelection(1, 0);
		} else if (ControlifyBindings.GUI_NAVI_UP.on(controller).justPressed()) {
			book.controllerMoveSelection(0, -1);
		} else if (ControlifyBindings.GUI_NAVI_DOWN.on(controller).justPressed()) {
			book.controllerMoveSelection(0, 1);
		}
	}

	public static void buttons(
		BedrockCraftingRecipeBookComponent book,
		ControllerEntity controller,
		Screen screen,
		ScreenProcessor<?> processor
	) {
		if (ControlifyBindings.GUI_PRESS.on(controller).justPressed()) {
			if (book.isSearchInputFocused()) {
book.controllerFocusSearch();
				processor.tryOpenKeyboard(controller, book.searchBoxForController());
			} else {
				book.controllerActivateSelected();
			}
		}
		if (ControlifyBindings.GUI_BACK.on(controller).justPressed()) {
			if (book.isSearchInputFocused()) {
				book.controllerBlurSearch();
			} else if (screen.shouldCloseOnEsc()) {
				screen.onClose();
			}
		}
		if (ControlifyBindings.GUI_ABSTRACT_ACTION_1.on(controller).justPressed()) {
			book.controllerToggleCraftableFilter();
		}
		if (ControlifyBindings.GUI_ABSTRACT_ACTION_2.on(controller).justPressed()) {
			book.controllerToggleActivationMode();
		}
	}

	public static void tabs(BedrockCraftingRecipeBookComponent book, ControllerEntity controller) {
		// Inventory screens use virtual-mouse bindings; regular GUI bindings cover focus mode.
		boolean next = ControlifyBindings.VMOUSE_PAGE_NEXT.on(controller).justPressed()
			|| ControlifyBindings.GUI_NEXT_TAB.on(controller).justPressed();
		boolean previous = ControlifyBindings.VMOUSE_PAGE_PREV.on(controller).justPressed()
			|| ControlifyBindings.GUI_PREV_TAB.on(controller).justPressed();
		if (next != previous) {
			book.controllerNextCategory(next ? 1 : -1);
		}
	}
}
