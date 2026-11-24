package dev.bedrockrecipebook.navigation;

public final class RecipeGridViewportChecks {
	public static void main(String[] args) {
		check(RecipeGridViewport.move(15, 0, 100, 5, 4, 0, 1), 20, 1);
		check(RecipeGridViewport.move(20, 1, 100, 5, 4, 0, 1), 25, 2);
		check(RecipeGridViewport.move(5, 1, 100, 5, 4, 0, -1), 0, 0);
		check(RecipeGridViewport.move(39, 4, 43, 5, 4, 0, 1), 42, 5);
		check(RecipeGridViewport.move(42, 5, 43, 5, 4, 0, 1), 42, 5);
		check(RecipeGridViewport.move(41, 0, 84, 7, 6, 0, 1), 48, 1);
		check(RecipeGridViewport.move(48, 1, 84, 7, 6, 0, -1), 41, 1);
		check(RecipeGridViewport.move(41, 1, 84, 7, 6, 0, -1), 34, 1);
		check(RecipeGridViewport.move(13, 1, 84, 7, 6, 0, -1), 6, 0);
		check(RecipeGridViewport.move(80, 12, 7, 5, 4, 0, 0), 6, 0);
		check(RecipeGridViewport.move(5, 2, 0, 5, 4, 0, 1), 0, 0);
		check(RecipeGridViewport.move(0, 0, 50, 5, 4, -1, 0), 0, 0);
		var state = new RecipeGridViewport(0, 0);
		for (int row = 1; row <= 19; row++) {
			state = RecipeGridViewport.move(state.selection(), state.topRow(), 100, 5, 4, 0, 1);
			check(state, row * 5, Math.max(0, row - 3));
		}
		for (int row = 18; row >= 0; row--) {
			state = RecipeGridViewport.move(state.selection(), state.topRow(), 100, 5, 4, 0, -1);
			if (state.selection() != row * 5 || state.topRow() > row || state.topRow() + 4 <= row) {
				throw new AssertionError("Upward navigation hid the selection: " + state);
			}
		}
checkPixels();
checkLargerPanel();
System.out.println("Recipe scrolling checks passed (row edges, pixel scrolling, partial rows, eight-column layout, pointer alignment, and compact windows).");
	}

private static void checkLargerPanel() {
var normal = RecipePanelLayout.calculate(480, 270, 238, 52, 166);
if (normal.width() != 192 || normal.height() != 198 || normal.tileWidth() != 22) {
throw new AssertionError("The larger panel did not use eight full-size columns: " + normal);
}
for (int screenWidth : new int[] {320, 360, 379, 426, 480, 640, 960}) {
for (int screenHeight : new int[] {180, 240, 270, 360, 540}) {
int containerX = (screenWidth - 176) / 2 + (screenWidth >= 379 ? 86 : 0);
int containerY = (screenHeight - 166) / 2;
var layout = RecipePanelLayout.calculate(screenWidth, screenHeight, containerX, containerY, 166);
if (layout.x() < 4 || layout.x() + layout.width() > screenWidth - 4
|| layout.y() - 19 < 4 || layout.y() + layout.height() > screenHeight - 4
|| layout.width() != 8 * layout.tileWidth() + 16
|| (containerX - 10 >= 160 && layout.x() + layout.width() > containerX - 6)) {
throw new AssertionError("Panel escaped its available bounds: " + layout);
}
for (int index = 0; index < 83; index++) {
int viewportHeight = layout.height() - 46 - 5;
double offset = PixelRecipeViewport.reveal(0, index, 83, 8, 22, viewportHeight);
int top = index / 8 * 22;
if (top < offset || top + 22 > offset + viewportHeight
|| PixelRecipeViewport.indexAt(index % 8 * layout.tileWidth() + layout.tileWidth() / 2,
top - offset + 11, offset, 8, layout.tileWidth(), 22) != index) {
throw new AssertionError("Eight-column scrolling or pointer targeting lost recipe " + index);
}
}
}
}
check(RecipeGridViewport.move(7, 0, 83, 8, 6, 0, 1), 15, 0);
check(RecipeGridViewport.move(7, 0, 83, 8, 6, 1, 0), 7, 0);
check(RecipeGridViewport.move(82, 5, 83, 8, 6, 0, 1), 82, 5);
}

private static void checkPixels() {
// Five 20px rows in an 87px viewport: all 87 pixels are usable, not just four rows.
pixel(PixelRecipeViewport.clamp(100, 35, 7, 20, 87), 13);
pixel(PixelRecipeViewport.clamp(-8, 35, 7, 20, 87), 0);
pixel(PixelRecipeViewport.clamp(12, 0, 7, 20, 87), 0);
pixel(PixelRecipeViewport.clamp(200, 8, 7, 20, 87), 0);
pixel(PixelRecipeViewport.clamp(1000, 36, 7, 20, 87), 33);
pixel(PixelRecipeViewport.clamp(4.75, 35, 7, 20, 87), 4.75);
pixel(PixelRecipeViewport.reveal(0, 34, 35, 7, 20, 87), 13);
pixel(PixelRecipeViewport.reveal(13, 0, 35, 7, 20, 87), 0);
pixel(PixelRecipeViewport.reveal(13, 14, 35, 7, 20, 87), 13);
pixel(PixelRecipeViewport.reveal(500, 0, 0, 7, 20, 87), 0);
if (PixelRecipeViewport.indexAt(19, 0, 13, 7, 19, 20) != 1
|| PixelRecipeViewport.indexAt(19, 7, 13, 7, 19, 20) != 8
|| PixelRecipeViewport.indexAt(0, 86, 13, 7, 19, 20) != 28) {
throw new AssertionError("Pointer mapping disagreed with clipped, pixel-scrolled rows");
}
// Moving focus reveals a whole tile even when only part of the final row was visible.
for (int index = 0; index < 80; index++) {
double offset = PixelRecipeViewport.reveal(0, index, 80, 7, 20, 87);
int top = index / 7 * 20;
if (top < offset || top + 20 > offset + 87) {
throw new AssertionError("Pixel viewport hid focused recipe " + index);
}
}
}

private static void pixel(double actual, double expected) {
if (actual != expected) {
throw new AssertionError("Expected pixel offset " + expected + "; got " + actual);
}
}

	private static void check(RecipeGridViewport actual, int selection, int topRow) {
		if (actual.selection() != selection || actual.topRow() != topRow) {
			throw new AssertionError("Expected selection=" + selection + ", topRow=" + topRow + "; got " + actual);
		}
	}
}
