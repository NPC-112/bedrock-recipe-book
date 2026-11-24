package dev.bedrockrecipebook.navigation;

/** Pixel geometry shared by rendering, pointer input, and focused navigation. */
public final class PixelRecipeViewport {
private PixelRecipeViewport() {}

public static double clamp(double offset, int size, int columns, int rowHeight, int height) {
int contentHeight = ((size + columns - 1) / columns) * rowHeight;
return Math.clamp(offset, 0.0, Math.max(0, contentHeight - height));
}

public static double reveal(double offset, int selection, int size, int columns, int rowHeight, int height) {
int top = selection / columns * rowHeight;
if (top < offset) {
offset = top;
} else if (top + rowHeight > offset + height) {
offset = top + rowHeight - height;
}
return clamp(offset, size, columns, rowHeight, height);
}

public static int indexAt(double x, double y, double offset, int columns, int cellWidth, int rowHeight) {
return (int) Math.floor((y + offset) / rowHeight) * columns + (int) Math.floor(x / cellWidth);
}
}
