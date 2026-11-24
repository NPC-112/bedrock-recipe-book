package dev.bedrockrecipebook.navigation;

/** Pure list-navigation state shared by keyboard and controller movement. */
public record RecipeGridViewport(int selection, int topRow) {
	public static RecipeGridViewport move(
		int selection, int topRow, int size, int columns, int visibleRows, int dx, int dy
	) {
		if (size == 0) {
			return new RecipeGridViewport(0, 0);
		}
		selection = Math.clamp(selection, 0, size - 1);
		int totalRows = (size + columns - 1) / columns;
		int row = Math.clamp(selection / columns + dy, 0, totalRows - 1);
		int column = Math.clamp(selection % columns + dx, 0, columns - 1);
		int next = Math.min(row * columns + column, size - 1);
		int selectedRow = next / columns;
		topRow = Math.clamp(topRow, 0, Math.max(0, totalRows - visibleRows));
		if (selectedRow < topRow) {
			topRow = selectedRow;
		} else if (selectedRow >= topRow + visibleRows) {
			topRow = selectedRow - visibleRows + 1;
		}
		return new RecipeGridViewport(next, topRow);
	}
}
