package dev.bedrockrecipebook.client;

import java.lang.ref.WeakReference;
import net.minecraft.client.gui.components.EditBox;

/**
 * Temporary, bounded diagnostics for in-game controller testing.
 * Logs identities and lengths only, never the query or typed characters.
 */
public final class SearchInputDiagnostics {
    private static WeakReference<BedrockCraftingRecipeBookComponent> activeBook = new WeakReference<>(null);
    private static WeakReference<EditBox> openingField = new WeakReference<>(null);
    private static int remaining;

    private SearchInputDiagnostics() {}

    public static void keyboardOpening(BedrockCraftingRecipeBookComponent book) {
        activeBook = new WeakReference<>(book);
        openingField = new WeakReference<>(book.searchBoxForController());
        remaining = 32;
        lifecycle(book, "opening native Controlify keyboard");
    }

    public static void lifecycle(BedrockCraftingRecipeBookComponent book, String phase) {
        if (activeBook.get() != book || remaining <= 0) {
            return;
        }
        remaining--;
        EditBox box = book.searchBoxForController();
        BedrockRecipeBookClient.LOGGER.info(
            "[search-input] {} field={} focused={} length={}",
            phase, System.identityHashCode(box), box != null && box.isFocused(),
            box == null ? -1 : box.getValue().length()
        );
    }

    public static void nativeCharacterResult(EditBox target, int previousLength) {
        BedrockCraftingRecipeBookComponent book = activeBook.get();
        if (book == null || remaining <= 0) {
            return;
        }
        EditBox current = book.searchBoxForController();
        if (target != current && target != openingField.get()) {
            return;
        }
        remaining--;
        BedrockRecipeBookClient.LOGGER.info(
            "[search-input] native character target={} current={} stale={} focused={} lengthBefore={} lengthAfter={}",
            System.identityHashCode(target), System.identityHashCode(current), target != current,
            target.isFocused(), previousLength, target.getValue().length()
        );
    }
}
