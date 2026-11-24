package dev.bedrockrecipebook.navigation;

/** Recipe-panel sizing in GUI pixels; the crafting container is not resized or moved. */
public record RecipePanelLayout(int x, int y, int width, int height, int tileWidth) {
    public static final int COLUMNS = 8;
    public static final int ROW_HEIGHT = 22;
    private static final int TARGET_TILE_WIDTH = 22;
    private static final int MIN_SIDE_BY_SIDE_TILE_WIDTH = 18;
    private static final int HORIZONTAL_PADDING = 16;
    private static final int CONTAINER_GAP = 6;
    private static final int SCREEN_MARGIN = 4;
    private static final int TOP_MARGIN = 23; // Room for the tabs above the panel.
    private static final int EXTRA_HEIGHT = 32;

    public static RecipePanelLayout calculate(
        int screenWidth, int screenHeight, int containerX, int containerY, int containerHeight
    ) {
        int availableLeft = containerX - CONTAINER_GAP - SCREEN_MARGIN;
        int minimumWidth = COLUMNS * MIN_SIDE_BY_SIDE_TILE_WIDTH + HORIZONTAL_PADDING;
        // Preserve vanilla's overlay presentation when the window cannot fit both panels.
        boolean sideBySide = availableLeft >= minimumWidth;
        int availableWidth = sideBySide ? availableLeft : screenWidth - SCREEN_MARGIN * 2;
        int tileWidth = Math.clamp((availableWidth - HORIZONTAL_PADDING) / COLUMNS, 1, TARGET_TILE_WIDTH);
        int width = COLUMNS * tileWidth + HORIZONTAL_PADDING;
        int x = sideBySide ? containerX - CONTAINER_GAP - width : SCREEN_MARGIN;
        int height = Math.min(containerHeight + EXTRA_HEIGHT, Math.max(1, screenHeight - TOP_MARGIN - SCREEN_MARGIN));
        int y = Math.clamp(containerY - EXTRA_HEIGHT / 2, TOP_MARGIN, Math.max(TOP_MARGIN, screenHeight - height - SCREEN_MARGIN));
        return new RecipePanelLayout(x, y, width, height, tileWidth);
    }
}
