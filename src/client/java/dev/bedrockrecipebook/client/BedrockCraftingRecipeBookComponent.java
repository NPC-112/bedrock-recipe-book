package dev.bedrockrecipebook.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bedrockrecipebook.mixin.client.RecipeBookComponentAccessor;
import dev.bedrockrecipebook.navigation.RecipeGridViewport;
import dev.bedrockrecipebook.navigation.PixelRecipeViewport;
import dev.bedrockrecipebook.navigation.RecipePanelLayout;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/**
 * First custom presentation layer for the vanilla crafting recipe book.
 *
 * <p>Recipe discovery and availability still come from the synchronized vanilla book.
 * Recipe activation goes back through Minecraft's native placement packet.</p>
 */
public final class BedrockCraftingRecipeBookComponent extends CraftingRecipeBookComponent {
private static final int COLUMNS = RecipePanelLayout.COLUMNS;
private static final int TILE_HEIGHT = RecipePanelLayout.ROW_HEIGHT;
private static final int GRID_X = 4;
private static final int GRID_TOP_CATEGORIES = 26;
private static final int GRID_TOP_SEARCH = 46;
private static final int TAB_TOP_OFFSET = -19;
private static final int TAB_HEIGHT = 21;
private static final int HEADER_Y = 5;
private static final int SEARCH_ROW_Y = 25;
private static final int SEARCH_ROW_HEIGHT = 17;
private static final int FILTER_BUTTON_WIDTH = 17;
private static final int MODE_BUTTON_WIDTH = 17;
private static final int CONTROL_GAP = 3;
private static final int CONTROL_LEFT = 5;
private static final int PANEL_INSET = 5;
private static final int SCROLL_TRACK_WIDTH = 3;
private static final int SCROLL_TRACK_RIGHT = 5;
private static final int PANEL_BOTTOM_INSET = 5;
private static final int MAX_QUEUED_CRAFTS = 64;
private static final int CATEGORY_TAB_COUNT = 4;
private static final int SEARCH_TAB_WIDTH = 24;
private static final int SEARCH_TAB_GAP = 9;
private static final int SEARCH_TAB_INDEX = CATEGORY_TAB_COUNT;

	private final StackedItemContents stackedContents = new StackedItemContents();
	private final List<RecipeTile> visibleEntries = new ArrayList<>();
private final Deque<QueuedCraft> queuedCrafts = new ArrayDeque<>();
	private final Set<RecipeGroupKey> expandedGroups = new HashSet<>();
private List<CategoryTab> tabs = List.of();
	private EditBox searchBox;
	private int screenWidth;
	private int screenHeight;
	private int panelX;
	private int panelY;
private int panelWidth = 192;
private int panelHeight = 198;
private int containerLeft;
private int containerTop;
private int containerWidth = 176;
private int containerHeight = 166;
private boolean containerBoundsSet;
private int tileWidth = 22;
private int tabWidth = 24;
private int visibleRows = 6;
private int selectedTab = SEARCH_TAB_INDEX;
	private int selectedRecipe;
private double scrollOffset;
private double scrollTarget;
private long lastScrollFrame;
	private int lastInventoryChange = -1;
	private int lastMenuState = -1;
private boolean craftableOnly = ModSettings.isCraftableOnly();
	private boolean focused;
private KeyboardFocus keyboardFocus = KeyboardFocus.RECIPES;
	private PendingCraft pendingCraft;

	public BedrockCraftingRecipeBookComponent(AbstractCraftingMenu menu) {
		super(menu);
	}

	@Override
	public void init(int width, int height, Minecraft minecraft, boolean widthTooNarrow) {
EditBox previousSearchBox = this.searchBox;
boolean searchWasFocused = previousSearchBox != null && previousSearchBox.isFocused();
String oldSearch = this.searchBox == null ? "" : this.searchBox.getValue();
		super.init(width, height, minecraft, widthTooNarrow);
		this.screenWidth = width;
		this.screenHeight = height;
this.containerBoundsSet = false;
		this.updatePanelPosition();
this.searchBox = ((RecipeBookComponentAccessor) (Object) this).bedrockRecipeBook$searchBox();
// Controlify's native input target holds an EditBox reference. Keep that same vanilla
// widget across screen rebuilds, rather than leaving the keyboard writing to an old one.
if (previousSearchBox != null) {
this.searchBox = previousSearchBox;
((RecipeBookComponentAccessor) (Object) this).bedrockRecipeBook$setSearchBox(previousSearchBox);
}
RecipeSearchFields.register(this.searchBox);
		this.searchBox.setMaxLength(50);
		this.searchBox.setBordered(false);
		this.searchBox.setResponder(value -> {
			this.selectedRecipe = 0;
this.resetScroll();
			this.refreshRecipes();
		});
this.searchBox.setValue(oldSearch);
this.updateSearchBoxPosition();
		this.craftableOnly = ModSettings.isCraftableOnly();
		this.refreshRecipes();
if (searchWasFocused && this.isSearchTab()) {
this.searchBox.setFocused(true);
}
SearchInputDiagnostics.lifecycle(this, "reinitialized");
	}

	private void updatePanelPosition() {
		int containerWidth = this.containerBoundsSet ? this.containerWidth : 176;
		int containerHeight = this.containerBoundsSet ? this.containerHeight : 166;
		int containerX = this.containerBoundsSet ? this.containerLeft : (this.screenWidth - containerWidth) / 2;
		int containerY = this.containerBoundsSet ? this.containerTop : (this.screenHeight - containerHeight) / 2;

RecipePanelLayout layout = RecipePanelLayout.calculate(
this.screenWidth, this.screenHeight, containerX, containerY, containerHeight);
this.panelWidth = layout.width();
this.panelHeight = layout.height();
this.panelX = layout.x();
this.panelY = layout.y();
this.tileWidth = layout.tileWidth();
this.tabWidth = Math.max(
			14,
			(this.panelWidth - PANEL_INSET * 2 - SEARCH_TAB_WIDTH - SEARCH_TAB_GAP) / CATEGORY_TAB_COUNT
);
		this.visibleRows = Math.max(1, (this.panelHeight - this.gridTop() - PANEL_BOTTOM_INSET) / TILE_HEIGHT);
		this.updateSearchBoxPosition();
	}

	public void setContainerBounds(int left, int top, int width, int height) {
if (this.containerBoundsSet && this.containerLeft == left && this.containerTop == top
&& this.containerWidth == width && this.containerHeight == height) {
return;
}
		this.containerLeft = left;
		this.containerTop = top;
		this.containerWidth = width;
		this.containerHeight = height;
		this.containerBoundsSet = true;
		this.updatePanelPosition();
	}

private int searchFieldWidth() {
		return Math.max(40, this.panelWidth - 42);
	}

	private int gridTop() {
		return this.isSearchTab() ? GRID_TOP_SEARCH : GRID_TOP_CATEGORIES;
	}

private int scrollTrackHeight() {
return Math.max(1, this.panelHeight - this.gridTop() - PANEL_BOTTOM_INSET);
}

private void resetScroll() {
this.scrollOffset = 0;
this.scrollTarget = 0;
}

private double clampScroll(double value) {
return PixelRecipeViewport.clamp(value, this.visibleEntries.size(), COLUMNS, TILE_HEIGHT, this.scrollTrackHeight());
}

private int recipeY(int index) {
return this.panelY + this.gridTop() + index / COLUMNS * TILE_HEIGHT - (int) Math.round(this.scrollOffset);
}

private void animateScroll() {
this.scrollTarget = this.clampScroll(this.scrollTarget);
this.scrollOffset = this.clampScroll(this.scrollOffset);
long now = System.nanoTime();
double elapsed = this.lastScrollFrame == 0 ? 0 : Math.min(0.1, (now - this.lastScrollFrame) / 1_000_000_000.0);
this.lastScrollFrame = now;
this.scrollOffset += (this.scrollTarget - this.scrollOffset) * (1 - Math.exp(-22 * elapsed));
if (Math.abs(this.scrollTarget - this.scrollOffset) < 0.2) {
this.scrollOffset = this.scrollTarget;
}
}

	private void updateSearchBoxPosition() {
		if (this.searchBox != null) {
// Controlify positions its A hint from the widget top, with its own four-pixel inset.
// Use the bar bounds here; RecipeSearchTextPositionMixin centers vanilla's text separately.
this.searchBox.setSize(this.searchFieldWidth(), SEARCH_ROW_HEIGHT);
this.searchBox.setPosition(this.panelX + 22, this.panelY + SEARCH_ROW_Y);
		}
}

private int tabX(int index) {
return index == SEARCH_TAB_INDEX
			? this.panelX + this.panelWidth - PANEL_INSET - SEARCH_TAB_WIDTH
			: this.panelX + PANEL_INSET + index * this.tabWidth;
}

private int tabButtonWidth(int index) {
return index == SEARCH_TAB_INDEX ? SEARCH_TAB_WIDTH : this.tabWidth;
}

private boolean isSearchTab() {
return !this.tabs.isEmpty() && this.tabs.get(this.selectedTab).group() == RecipeGroup.SEARCH;
}

private void selectTab(int tab) {
this.selectedTab = tab;
this.selectedRecipe = 0;
this.resetScroll();
this.updatePanelPosition();
if (this.searchBox != null && this.searchBox.isFocused()) {
this.blurSearch();
}
this.refreshRecipes();
}

	@Override
	public void tick() {
		super.tick();
		if (this.minecraft == null || this.minecraft.player == null) {
			this.pendingCraft = null;
			this.queuedCrafts.clear();
			return;
		}

		this.finishPendingCraftIfReady();
		int inventoryChange = this.minecraft.player.getInventory().getTimesChanged();
		int menuState = this.menu.getStateId();
		if (inventoryChange != this.lastInventoryChange || menuState != this.lastMenuState) {
			this.lastInventoryChange = inventoryChange;
			this.lastMenuState = menuState;
			this.refreshRecipes();
		}
		this.startNextQueuedCraft();
	}

	private void finishPendingCraftIfReady() {
		if (this.pendingCraft == null) {
			return;
		}
		if (this.minecraft.player.containerMenu != this.menu || !this.menu.getCarried().isEmpty()) {
			this.pendingCraft = null;
			this.queuedCrafts.clear();
			return;
		}

		PendingCraft pending = this.pendingCraft;
		if (pending.outputRequested()) {
			if (this.menu.getStateId() != pending.outputMenuState()) {
				this.pendingCraft = null;
			} else if (++pending.waitTicks > 80) {
				this.pendingCraft = null;
				this.queuedCrafts.clear();
			}
			return;
		}

		if (this.menu.getStateId() == pending.initialMenuState()) {
			if (++pending.waitTicks > 80) {
				this.pendingCraft = null;
				this.queuedCrafts.clear();
			}
			return;
		}

		Slot resultSlot = this.menu.getResultSlot();
		ItemStack result = resultSlot.getItem();
		if (!result.isEmpty() && ItemStack.isSameItemSameComponents(result, pending.expectedResult())) {
			this.minecraft.gameMode.handleContainerInput(
				this.menu.containerId,
				resultSlot.index,
				0,
				ContainerInput.QUICK_MOVE,
				this.minecraft.player
			);
			pending.markOutputRequested(this.menu.getStateId());
		} else {
			if (++pending.waitTicks > 80) {
				this.pendingCraft = null;
				this.queuedCrafts.clear();
			}
		}
	}

	private void startNextQueuedCraft() {
		if (this.pendingCraft != null || this.queuedCrafts.isEmpty()) {
			return;
		}
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null
			|| this.minecraft.player.containerMenu != this.menu || !this.menu.getCarried().isEmpty()) {
			this.queuedCrafts.clear();
			return;
		}

		while (!this.queuedCrafts.isEmpty()) {
			QueuedCraft queued = this.queuedCrafts.removeFirst();
			if (!queued.instantCraft()) {
				RecipePlacementBridge.requestPlacement(this.minecraft, this.menu, queued.entry().id());
				this.queuedCrafts.clear();
				return;
			}
			if (!queued.entry().canCraft(this.stackedContents)) {
				continue;
			}
			RecipePlacementBridge.PlacementResult result =
				RecipePlacementBridge.requestPlacement(this.minecraft, this.menu, queued.entry().id());
			if (result == RecipePlacementBridge.PlacementResult.REQUESTED) {
				this.pendingCraft = new PendingCraft(queued.expectedResult(), this.menu.getStateId());
			}
			return;
		}
	}

	private void refreshRecipes() {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
			this.visibleEntries.clear();
			return;
		}

		this.stackedContents.clear();
		this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
		this.menu.fillCraftSlotsStackedContents(this.stackedContents);
	this.refreshTabs();
this.updatePanelPosition();

CategoryTab tab = this.tabs.get(this.selectedTab);
String query = tab.group() == RecipeGroup.SEARCH && this.searchBox != null
? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT)
: "";
		ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
List<RecipeTile> updated = new ArrayList<>();
List<ItemStack> displayedOutputs = new ArrayList<>();
		ClientRecipeBook book = this.minecraft.player.getRecipeBook();
ExtendedRecipeBookCategory category = switch (tab.group()) {
case CONSTRUCTION -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
case ITEMS -> RecipeBookCategories.CRAFTING_REDSTONE;
case NATURE -> RecipeBookCategories.CRAFTING_MISC;
case SEARCH -> SearchRecipeBookCategory.CRAFTING;
};
Set<RecipeCollection> searchMatches = null;
if (!query.isEmpty() && this.minecraft.getConnection() != null) {
searchMatches = new HashSet<>(this.minecraft.getConnection().searchTrees().recipes().search(query));
}
		for (RecipeCollection collection : book.getCollection(category)) {
collection.selectRecipes(this.stackedContents, this::canDisplay);
if (searchMatches != null && !searchMatches.contains(collection)) {
					continue;
				}
List<RecipeDisplayEntry> entries = collection.getSelectedRecipes(this.craftableOnly
? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY);
entries = this.uniqueOutputRecipes(entries, displayedOutputs, context);
if (entries.isEmpty()) {
					continue;
				}
if (entries.size() == 1) {
updated.add(new RecipeTile(entries.getFirst(), null, false));
continue;
			}
// The synchronized Java collection is the group; never infer membership from item names.
RecipeGroupKey key = new RecipeGroupKey("collection:" + collection.getRecipes().getFirst().id().index(), null);
RecipeGroupModel group = new RecipeGroupModel(key, List.copyOf(entries));
RecipeDisplayEntry representative = entries.stream()
.filter(entry -> collection.isCraftable(entry.id()))
.findFirst().orElse(entries.getFirst());
updated.add(new RecipeTile(representative, group, true));
if (this.expandedGroups.contains(key)) {
for (RecipeDisplayEntry entry : entries) {
updated.add(new RecipeTile(entry, group, false));
}
}
		}

		this.visibleEntries.clear();
this.visibleEntries.addAll(updated);
		this.selectedRecipe = Math.max(0, Math.min(this.selectedRecipe, this.visibleEntries.size() - 1));
		this.revealSelection();
	}

private void refreshTabs() {
	List<CategoryTab> availableTabs = new ArrayList<>();
availableTabs.add(new CategoryTab("Building Blocks", new ItemStack(Items.BRICKS), RecipeGroup.CONSTRUCTION));
availableTabs.add(new CategoryTab("Equipment", new ItemStack(Items.IRON_SWORD), RecipeGroup.EQUIPMENT));
availableTabs.add(new CategoryTab("Redstone", new ItemStack(Items.REDSTONE), RecipeGroup.ITEMS));
availableTabs.add(new CategoryTab("Miscellaneous", new ItemStack(Items.LAVA_BUCKET), RecipeGroup.NATURE));
availableTabs.add(new CategoryTab("All items", new ItemStack(Items.SPYGLASS), RecipeGroup.SEARCH));
	this.tabs = availableTabs;
	this.selectedTab = Math.max(0, Math.min(this.selectedTab, this.tabs.size() - 1));
}

private boolean canDisplay(RecipeDisplay display) {
return switch (display) {
			case ShapedCraftingRecipeDisplay shaped ->
				this.menu.getGridWidth() >= shaped.width() && this.menu.getGridHeight() >= shaped.height();
			case ShapelessCraftingRecipeDisplay shapeless ->
				this.menu.getGridWidth() * this.menu.getGridHeight() >= shapeless.ingredients().size();
			default -> false;
		};
	}

private List<RecipeDisplayEntry> uniqueOutputRecipes(
List<RecipeDisplayEntry> entries, List<ItemStack> displayedOutputs, ContextMap context
) {
List<RecipeDisplayEntry> unique = new ArrayList<>();
for (RecipeDisplayEntry entry : entries) {
ItemStack output = entry.resultItems(context).stream()
.filter(stack -> !stack.isEmpty())
.findFirst()
.orElse(ItemStack.EMPTY);
if (output.isEmpty()) {
unique.add(entry);
continue;
}
boolean duplicate = displayedOutputs.stream().anyMatch(existing ->
existing.getCount() == output.getCount()
&& ItemStack.isSameItemSameComponents(existing, output));
if (!duplicate) {
displayedOutputs.add(output.copy());
unique.add(entry);
}
}
return unique;
}

	private String recipeGroupLabel(RecipeGroupModel group, ContextMap context) {
		if (group.key().label() != null) {
			return group.key().label();
		}
		List<List<String>> itemPaths = new ArrayList<>();
		for (RecipeDisplayEntry entry : group.entries()) {
			ItemStack result = entry.resultItems(context).stream()
				.filter(stack -> !stack.isEmpty())
				.findFirst()
				.orElse(ItemStack.EMPTY);
			if (!result.isEmpty()) {
				itemPaths.add(List.of(BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().split("_")));
			}
		}
		if (itemPaths.isEmpty()) {
			return "Similar";
		}

		int sharedSuffixLength = 0;
		boolean hasSharedWord = true;
		while (hasSharedWord) {
			int candidateIndex = itemPaths.getFirst().size() - sharedSuffixLength - 1;
			if (candidateIndex < 0) {
				break;
			}
			String candidateWord = itemPaths.getFirst().get(candidateIndex);
			for (List<String> path : itemPaths) {
				if (candidateIndex >= path.size()
					|| !candidateWord.equals(path.get(path.size() - sharedSuffixLength - 1))) {
					hasSharedWord = false;
					break;
				}
			}
			if (hasSharedWord) {
				sharedSuffixLength++;
			}
		}

		if (sharedSuffixLength == 0) {
			return "Similar";
		}
		List<String> words = new ArrayList<>(
			itemPaths.getFirst().subList(itemPaths.getFirst().size() - sharedSuffixLength, itemPaths.getFirst().size())
		);
		int last = words.size() - 1;
		words.set(last, singularizeGroupWord(words.get(last)));
		return words.stream().map(BedrockCraftingRecipeBookComponent::capitalizeGroupWord)
			.collect(Collectors.joining(" "));
	}

	private static String singularizeGroupWord(String word) {
		if (word.equals("planks")) {
			return "plank";
		}
		if (word.endsWith("ies") && word.length() > 4) {
			return word.substring(0, word.length() - 3) + "y";
		}
		if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 3) {
			return word.substring(0, word.length() - 1);
		}
		return word;
	}

	private static String capitalizeGroupWord(String word) {
		return word.isEmpty()
			? word
			: Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	private void toggleRecipeGroup(RecipeGroupModel group) {
		if (!this.expandedGroups.add(group.key())) {
			this.expandedGroups.remove(group.key());
		}
		this.refreshRecipes();
	}

	private void activateTile(RecipeTile tile) {
		if (tile.groupHeader()) {
			this.toggleRecipeGroup(tile.group());
		} else {
			this.activateRecipe(tile.entry());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (!this.isVisible()) {
			return;
		}

		this.updatePanelPosition();
this.animateScroll();
		int x = this.panelX;
		int y = this.panelY;
		int tabTop = y + TAB_TOP_OFFSET;
		graphics.fill(x - 2, y - 2, x + this.panelWidth + 2, y + this.panelHeight + 2, 0xFF151515);
		graphics.fill(x, y, x + this.panelWidth, y + this.panelHeight, 0xFFC6C6C6);
		graphics.fill(x + 1, y + 1, x + this.panelWidth - 1, y + 2, 0xFFF0F0F0);
		graphics.fill(x + 1, y + 1, x + 2, y + this.panelHeight - 1, 0xFFF0F0F0);
		graphics.fill(x + 1, y + this.panelHeight - 2, x + this.panelWidth - 1, y + this.panelHeight - 1, 0xFF666666);
		graphics.fill(x + this.panelWidth - 2, y + 1, x + this.panelWidth - 1, y + this.panelHeight - 1, 0xFF666666);

		this.drawButton(graphics, this.activationButtonX(), y + 4, MODE_BUTTON_WIDTH, SEARCH_ROW_HEIGHT - 1,
			"⏩", ModSettings.getActivationMode() == ModSettings.ActivationMode.CRAFT_IMMEDIATELY
				? 0xFF68834D : 0xFF777777);
		this.drawButton(graphics, this.filterButtonX(), y + 4, FILTER_BUTTON_WIDTH, SEARCH_ROW_HEIGHT - 1,
			"", this.craftableOnly ? 0xFF68834D : 0xFF777777);
		graphics.item(new ItemStack(Items.CRAFTING_TABLE), this.filterButtonX() + 1, y + 4, 0);
		if (this.keyboardFocus == KeyboardFocus.ACTIVATION) {
			graphics.outline(this.activationButtonX(), y + 4,
				MODE_BUTTON_WIDTH, SEARCH_ROW_HEIGHT - 1, 0xFFFFFFFF);
		} else if (this.keyboardFocus == KeyboardFocus.FILTER) {
			graphics.outline(this.filterButtonX(), y + 4,
				FILTER_BUTTON_WIDTH, SEARCH_ROW_HEIGHT - 1, 0xFFFFFFFF);
		}
		if (this.isButtonHovered(mouseX, mouseY, this.activationButtonX(), y + 4)) {
			graphics.setTooltipForNextFrame(
				this.minecraft.font,
				Component.literal(ModSettings.getActivationMode() == ModSettings.ActivationMode.CRAFT_IMMEDIATELY
					? "Instant Craft: ON" : "Instant Craft: OFF"),
				mouseX,
				mouseY
			);
		} else if (this.isButtonHovered(mouseX, mouseY, this.filterButtonX(), y + 4)) {
			graphics.setTooltipForNextFrame(
				this.minecraft.font,
				Component.literal(this.craftableOnly ? "Craftable Toggle: ON" : "Craftable Toggle: OFF"),
				mouseX,
				mouseY
			);
		}

		CategoryTab selectedTab = this.tabs.get(this.selectedTab);
		String heading = selectedTab.name();
		int headingX = x + this.panelWidth - PANEL_INSET - this.minecraft.font.width(heading);
		graphics.text(this.minecraft.font, heading, headingX, y + HEADER_Y, 0xFFFFFFFF);

for (int i = 0; i < this.tabs.size(); i++) {
int tabX = this.tabX(i);
			int tabWidth = this.tabButtonWidth(i);
			int currentTabTop = tabTop;
			int tabBottom = currentTabTop + TAB_HEIGHT;
			boolean selected = i == this.selectedTab;
			boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth
				&& mouseY >= currentTabTop && mouseY < tabBottom;
			int tabFace = selected ? 0xFFC6C6C6 : (hovered ? 0xFF9A9A9A : 0xFF888888);
			graphics.fill(tabX, currentTabTop, tabX + tabWidth, tabBottom, 0xFF333333);
			graphics.fill(tabX + 1, currentTabTop + 1, tabX + tabWidth - 1, tabBottom - 1, tabFace);
			graphics.fill(tabX + 2, currentTabTop + 2, tabX + tabWidth - 2, currentTabTop + 3, 0xFFDADADA);
			if (selected) {
				graphics.fill(tabX + 1, y - 1, tabX + tabWidth - 1, y + 2, 0xFFC6C6C6);
			}
			if (selected && this.keyboardFocus == KeyboardFocus.TABS) {
				graphics.outline(tabX, currentTabTop, tabWidth, TAB_HEIGHT, 0xFFFFFFFF);
			}
ItemStack icon = this.tabs.get(i).icon();
if (!icon.isEmpty()) {
				graphics.item(icon, tabX + (tabWidth - 16) / 2, currentTabTop + (TAB_HEIGHT - 16) / 2, 0);
}
			if (hovered) {
graphics.setTooltipForNextFrame(
this.minecraft.font,
Component.literal(this.tabs.get(i).name()),
mouseX,
mouseY
);
}
		}

if (this.isSearchTab()) {
			int searchX = x + PANEL_INSET;
			int searchY = y + SEARCH_ROW_Y;
			int searchRight = x + this.panelWidth - PANEL_INSET;
			graphics.fill(searchX, searchY, searchRight, searchY + SEARCH_ROW_HEIGHT, 0xFF777777);
			graphics.fill(searchX + 1, searchY + 1, searchRight - 1, searchY + SEARCH_ROW_HEIGHT - 1, 0xFF101010);
			graphics.item(new ItemStack(Items.SPYGLASS), searchX + 1, searchY, 0);
			this.updateSearchBoxPosition();
			this.searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
			if (this.keyboardFocus == KeyboardFocus.SEARCH) {
				graphics.outline(searchX, searchY, searchRight - searchX, SEARCH_ROW_HEIGHT, 0xFFFFFFFF);
			}
			if (!this.searchBox.getValue().isEmpty()) {
				int clearX = x + this.panelWidth - 17;
				graphics.fill(clearX, searchY + 3, clearX + 11, searchY + 14, 0xFF333333);
				graphics.text(this.minecraft.font, "×", clearX + 2, searchY + 2, 0xFFFFFFFF);
			}
			// The vanilla search box is registered with the screen, so it receives keyboard and IME input.
}

		int gridTop = y + this.gridTop();
graphics.fill(x + 2, gridTop - 2, x + this.panelWidth - 2, gridTop + this.scrollTrackHeight(), 0xFF777777);
this.renderRecipeTiles(graphics, mouseX, mouseY);
		this.renderScrollStatus(graphics);
	}

private void renderRecipeTiles(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
int firstRow = (int) this.scrollOffset / TILE_HEIGHT;
int start = firstRow * COLUMNS;
int drawCount = COLUMNS * ((this.scrollTrackHeight() + TILE_HEIGHT - 1) / TILE_HEIGHT + 1);
graphics.enableScissor(this.panelX + GRID_X, this.panelY + this.gridTop(),
this.panelX + GRID_X + COLUMNS * this.tileWidth, this.panelY + this.gridTop() + this.scrollTrackHeight());
for (int i = 0; i < drawCount; i++) {
			int entryIndex = start + i;
			int column = i % COLUMNS;
int x = this.panelX + GRID_X + column * this.tileWidth;
int y = this.recipeY(entryIndex);
if (y >= this.panelY + this.gridTop() + this.scrollTrackHeight()) {
break;
}
			if (entryIndex >= this.visibleEntries.size()) {
				continue;
			}

			RecipeTile tile = this.visibleEntries.get(entryIndex);
			RecipeDisplayEntry entry = tile.entry();
			boolean craftable = entry.canCraft(this.stackedContents);
			boolean selected = entryIndex == this.selectedRecipe;
			boolean expandedHeader = tile.groupHeader() && this.expandedGroups.contains(tile.group().key());
			boolean expandedMember = tile.group() != null && !tile.groupHeader()
				&& this.expandedGroups.contains(tile.group().key());
boolean hovered = this.containsRecipeGrid(mouseX, mouseY)
&& mouseX >= x && mouseX < x + this.tileWidth - 2
				&& mouseY >= y && mouseY < y + TILE_HEIGHT - 2;
			int background = !craftable ? 0xFFC94E4E
				: (expandedHeader || expandedMember ? 0xFF666666 : 0xFF888888);
			if (hovered && craftable) {
				background = 0xFF72865B;
			} else if (selected && craftable) {
				background = 0xFF9A9A9A;
			}
			graphics.fill(x, y, x + this.tileWidth - 2, y + TILE_HEIGHT - 2, 0xFF383838);
			graphics.fill(x + 1, y + 1, x + this.tileWidth - 3, y + TILE_HEIGHT - 3, background);
			graphics.fill(x + 1, y + 1, x + this.tileWidth - 3, y + 2,
				craftable ? 0xFFBDBDBD : 0xFFE58A8A);
			graphics.fill(x + 1, y + TILE_HEIGHT - 4, x + this.tileWidth - 3, y + TILE_HEIGHT - 3, 0xFF555555);
			if (selected) {
				graphics.fill(x, y, x + this.tileWidth - 2, y + 1, 0xFFEAEAEA);
				graphics.fill(x, y + 1, x + 1, y + TILE_HEIGHT - 2, 0xFFEAEAEA);
				graphics.fill(x + this.tileWidth - 3, y + 1, x + this.tileWidth - 2, y + TILE_HEIGHT - 2, 0xFF444444);
				graphics.fill(x + 1, y + TILE_HEIGHT - 3, x + this.tileWidth - 2, y + TILE_HEIGHT - 2, 0xFF444444);
			}
			List<ItemStack> results = entry.resultItems(context);
			if (!results.isEmpty() && !results.get(0).isEmpty()) {
graphics.item(results.get(0), x + (this.tileWidth - 16) / 2, y + (TILE_HEIGHT - 16) / 2, entry.id().index());
				if (tile.groupHeader()) {
					boolean expanded = this.expandedGroups.contains(tile.group().key());
					int badgeX = x + this.tileWidth - 9;
					int badgeY = y + TILE_HEIGHT - 9;
graphics.fill(badgeX, badgeY, badgeX + 7, badgeY + 7, 0xFF222222);
graphics.text(this.minecraft.font, expanded ? "−" : "+", badgeX + 1, badgeY, 0xFFFFFFFF);
				}
				if (hovered) {
					if (tile.groupHeader()) {
						graphics.setTooltipForNextFrame(
							this.minecraft.font,
							Component.literal(this.recipeGroupLabel(tile.group(), context)),
							mouseX,
							mouseY
						);
					} else {
						graphics.setTooltipForNextFrame(this.minecraft.font, results.get(0), mouseX, mouseY);
					}
}
}
		}
graphics.disableScissor();
	}

	private void renderScrollStatus(GuiGraphicsExtractor graphics) {
int contentHeight = ((this.visibleEntries.size() + COLUMNS - 1) / COLUMNS) * TILE_HEIGHT;
if (contentHeight > this.scrollTrackHeight()) {
			int trackX = this.panelX + this.panelWidth - SCROLL_TRACK_RIGHT - SCROLL_TRACK_WIDTH;
			int trackY = this.panelY + this.gridTop();
			int trackHeight = this.scrollTrackHeight();
int thumbHeight = Math.max(8, trackHeight * trackHeight / contentHeight);
int thumbY = trackY + (int) Math.round((trackHeight - thumbHeight) * this.scrollOffset / (contentHeight - trackHeight));
			graphics.fill(trackX, trackY, trackX + SCROLL_TRACK_WIDTH, trackY + trackHeight, 0xFF555555);
			graphics.fill(trackX, thumbY, trackX + SCROLL_TRACK_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
		}
	}

	private int activationButtonX() {
		return this.panelX + CONTROL_LEFT;
	}

	private int filterButtonX() {
		return this.activationButtonX() + MODE_BUTTON_WIDTH + CONTROL_GAP;
	}

private void toggleCraftableFilter() {
this.craftableOnly = !this.craftableOnly;
ModSettings.setCraftableOnly(this.craftableOnly);
this.selectedRecipe = 0;
this.resetScroll();
this.refreshRecipes();
}

	private boolean isButtonHovered(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + FILTER_BUTTON_WIDTH
			&& mouseY >= y && mouseY < y + SEARCH_ROW_HEIGHT - 1;
	}

	private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String text, int background) {
graphics.fill(x, y, x + width, y + height, 0xFF343434);
graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFD6D6D6);
graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
		int maxWidth = width - 6;
		String displayText = this.minecraft.font.plainSubstrByWidth(text, maxWidth);
		int textX = x + (width - this.minecraft.font.width(displayText)) / 2;
		int textY = y + (height - 8) / 2;
		graphics.text(this.minecraft.font, displayText, textX, textY, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!this.isVisible()) {
			return false;
		}

		double mouseX = event.x();
		double mouseY = event.y();
if (!this.containsPanel(mouseX, mouseY)) {
if (this.isSearchInputFocused()) {
this.blurSearch();
}
			return false;
		}

		this.focused = true;
		int relativeX = (int) mouseX - this.panelX;
		int relativeY = (int) mouseY - this.panelY;
if (this.isSearchTab() && relativeY >= SEARCH_ROW_Y
&& relativeY < SEARCH_ROW_Y + SEARCH_ROW_HEIGHT) {
			this.keyboardFocus = KeyboardFocus.SEARCH;
if (!this.searchBox.getValue().isEmpty()
&& relativeX >= this.panelWidth - 18 && relativeX < this.panelWidth - PANEL_INSET) {
this.searchBox.setValue("");
				this.focusSearch();
return true;
}
if (this.searchBox.mouseClicked(event, doubleClick)) {
				this.focusSearch();
return true;
}
			this.focusSearch();
return true;
}
if (this.isSearchTab()) {
			this.blurSearch();
}

if (relativeY >= 4 && relativeY < 4 + SEARCH_ROW_HEIGHT - 1
&& relativeX >= CONTROL_LEFT && relativeX < CONTROL_LEFT + MODE_BUTTON_WIDTH) {
			this.keyboardFocus = KeyboardFocus.ACTIVATION;
ModSettings.ActivationMode current = ModSettings.getActivationMode();
ModSettings.setActivationMode(current == ModSettings.ActivationMode.CRAFT_IMMEDIATELY
? ModSettings.ActivationMode.FILL_GRID_ONLY
: ModSettings.ActivationMode.CRAFT_IMMEDIATELY);
			return true;
		}
if (relativeY >= 4 && relativeY < 4 + SEARCH_ROW_HEIGHT - 1
&& relativeX >= CONTROL_LEFT + MODE_BUTTON_WIDTH + CONTROL_GAP
&& relativeX < CONTROL_LEFT + MODE_BUTTON_WIDTH + CONTROL_GAP + FILTER_BUTTON_WIDTH) {
			this.keyboardFocus = KeyboardFocus.FILTER;
this.toggleCraftableFilter();
return true;
}
if (relativeY >= TAB_TOP_OFFSET && relativeY < TAB_TOP_OFFSET + TAB_HEIGHT) {
for (int tab = 0; tab < this.tabs.size(); tab++) {
int tabLeft = this.tabX(tab) - this.panelX;
int tabWidth = this.tabButtonWidth(tab);
if (relativeX >= tabLeft && relativeX < tabLeft + tabWidth) {
				this.keyboardFocus = KeyboardFocus.TABS;
this.selectTab(tab);
return true;
			}
}
			return true;
		}
if (relativeX >= this.panelWidth - SCROLL_TRACK_RIGHT - SCROLL_TRACK_WIDTH
&& relativeY >= this.gridTop()
&& relativeY < this.gridTop() + this.scrollTrackHeight()) {
this.scrollToTrackPosition(relativeY - this.gridTop());
			return true;
		}
		this.keyboardFocus = KeyboardFocus.RECIPES;
if (relativeY >= this.gridTop() && relativeY < this.gridTop() + this.scrollTrackHeight()
&& relativeX >= GRID_X && relativeX < GRID_X + COLUMNS * this.tileWidth) {
int clickedIndex = PixelRecipeViewport.indexAt(relativeX - GRID_X, relativeY - this.gridTop(),
Math.round(this.scrollOffset), COLUMNS, this.tileWidth, TILE_HEIGHT);
			if (clickedIndex >= 0 && clickedIndex < this.visibleEntries.size()) {
				this.selectedRecipe = clickedIndex;
				this.activateTile(this.visibleEntries.get(clickedIndex));
			}
			return true;
		}
		return true;
	}

	private void activateRecipe(RecipeDisplayEntry entry) {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null) {
			return;
		}

		boolean canCraft = entry.canCraft(this.stackedContents);
		ItemStack expectedResult = this.firstResult(entry);
		boolean instantCraft = ModSettings.getActivationMode() == ModSettings.ActivationMode.CRAFT_IMMEDIATELY
			&& canCraft
			&& !expectedResult.isEmpty()
			&& this.menu.getCarried().isEmpty();
		if (canCraft) {
			((RecipeBookComponentAccessor) (Object) this).bedrockRecipeBook$ghostSlots().clear();
		}

		if (this.pendingCraft != null || !this.queuedCrafts.isEmpty()) {
			if (this.queuedCrafts.size() < MAX_QUEUED_CRAFTS) {
				this.queuedCrafts.addLast(new QueuedCraft(entry, expectedResult, instantCraft));
			}
			return;
		}

		if (instantCraft) {
			this.queuedCrafts.addLast(new QueuedCraft(entry, expectedResult, true));
			this.startNextQueuedCraft();
			return;
		}

		RecipePlacementBridge.PlacementResult result =
			RecipePlacementBridge.requestPlacement(this.minecraft, this.menu, entry.id());
		if (result != RecipePlacementBridge.PlacementResult.REQUESTED) {
			return;
		}
		if (canCraft) {
			((RecipeBookComponentAccessor) (Object) this).bedrockRecipeBook$ghostSlots().clear();
		}
	}

	private ItemStack firstResult(RecipeDisplayEntry entry) {
		if (this.minecraft == null || this.minecraft.level == null) {
			return ItemStack.EMPTY;
		}
		return entry.resultItems(SlotDisplayContext.fromLevel(this.minecraft.level)).stream()
			.findFirst()
			.map(ItemStack::copy)
			.orElse(ItemStack.EMPTY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.isVisible()) {
			return false;
		}
		int key = event.input();
		if (this.isSearchTab() && this.searchBox.isFocused()) {
			if (key == InputConstants.KEY_UP) {
				this.blurSearch();
				this.keyboardFocus = KeyboardFocus.ACTIVATION;
				return true;
			}
			if (key == InputConstants.KEY_DOWN) {
				this.blurSearch();
				this.keyboardFocus = KeyboardFocus.RECIPES;
				this.moveSelection(0, 0);
				return true;
			}
			if (this.searchBox.keyPressed(event)) {
				return true;
			}
			if (key == InputConstants.KEY_ESCAPE) {
				this.blurSearch();
				this.keyboardFocus = KeyboardFocus.ACTIVATION;
				return true;
			}
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				return true;
			}
			return true;
		}

		if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) {
			int direction = key == InputConstants.KEY_LEFT ? -1 : 1;
			if (this.keyboardFocus == KeyboardFocus.TABS) {
				this.selectTab(Math.floorMod(this.selectedTab + direction, this.tabs.size()));
			} else if (this.keyboardFocus == KeyboardFocus.ACTIVATION) {
				this.keyboardFocus = KeyboardFocus.FILTER;
			} else if (this.keyboardFocus == KeyboardFocus.FILTER) {
				this.keyboardFocus = KeyboardFocus.ACTIVATION;
			} else if (this.keyboardFocus == KeyboardFocus.RECIPES) {
				this.moveSelection(direction, 0);
			}
			return true;
		}
		if (key == InputConstants.KEY_UP) {
			switch (this.keyboardFocus) {
				case RECIPES -> {
					if (this.selectedRecipe < COLUMNS && !this.visibleEntries.isEmpty()) {
						if (this.isSearchTab()) {
							this.keyboardFocus = KeyboardFocus.SEARCH;
							this.focusSearch();
						} else {
							this.keyboardFocus = this.preferredToggleForRecipe(this.selectedRecipe);
						}
					} else {
						this.moveSelection(0, -1);
					}
				}
				case SEARCH -> {
					this.blurSearch();
					this.keyboardFocus = KeyboardFocus.ACTIVATION;
				}
				case ACTIVATION, FILTER -> this.keyboardFocus = KeyboardFocus.TABS;
				case TABS -> {
				}
			}
			return true;
		}
		if (key == InputConstants.KEY_DOWN) {
			switch (this.keyboardFocus) {
				case TABS -> this.keyboardFocus = KeyboardFocus.ACTIVATION;
				case ACTIVATION, FILTER -> {
					if (this.isSearchTab()) {
						this.keyboardFocus = KeyboardFocus.SEARCH;
						this.focusSearch();
					} else {
						this.keyboardFocus = KeyboardFocus.RECIPES;
						this.moveSelection(0, 0);
					}
				}
				case SEARCH -> {
					this.blurSearch();
					this.keyboardFocus = KeyboardFocus.RECIPES;
				}
				case RECIPES -> this.moveSelection(0, 1);
			}
			return true;
		}
		if (key == InputConstants.KEY_PAGEUP) {
			if (this.keyboardFocus != KeyboardFocus.RECIPES) {
				return true;
			}
this.moveSelection(0, -this.visibleRows);
			return true;
		}
		if (key == InputConstants.KEY_PAGEDOWN) {
			if (this.keyboardFocus != KeyboardFocus.RECIPES) {
				return true;
			}
this.moveSelection(0, this.visibleRows);
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_SPACE) {
			if (this.keyboardFocus == KeyboardFocus.ACTIVATION) {
				this.toggleActivationMode();
				return true;
			}
			if (this.keyboardFocus == KeyboardFocus.FILTER) {
				this.toggleCraftableFilter();
				return true;
			}
			if (this.keyboardFocus == KeyboardFocus.TABS) {
				this.selectTab(this.selectedTab);
				return true;
			}
			if (!this.visibleEntries.isEmpty()) {
				this.activateTile(this.visibleEntries.get(this.selectedRecipe));
			}
			return true;
		}
		if (key == InputConstants.KEY_TAB) {
			if (!this.isSearchTab()) {
				return false;
			}
			if (this.searchBox.isFocused()) {
				this.blurSearch();
				this.keyboardFocus = KeyboardFocus.RECIPES;
			} else {
				this.keyboardFocus = KeyboardFocus.SEARCH;
				this.focusSearch();
			}
			return true;
		}
		return false;
	}

	private KeyboardFocus preferredToggleForRecipe(int recipeIndex) {
		int column = recipeIndex % COLUMNS;
		int recipeX = this.panelX + GRID_X + column * this.tileWidth + this.tileWidth / 2;
		int activationX = this.activationButtonX() + MODE_BUTTON_WIDTH / 2;
		int filterX = this.filterButtonX() + FILTER_BUTTON_WIDTH / 2;
		return Math.abs(recipeX - activationX) <= Math.abs(recipeX - filterX)
			? KeyboardFocus.ACTIVATION : KeyboardFocus.FILTER;
	}

	private void moveSelection(int dx, int dy) {
		this.keyboardFocus = KeyboardFocus.RECIPES;
		RecipeGridViewport viewport = RecipeGridViewport.move(
this.selectedRecipe, 0, this.visibleEntries.size(), COLUMNS, this.visibleRows, dx, dy);
		this.selectedRecipe = viewport.selection();
this.scrollTarget = PixelRecipeViewport.reveal(this.scrollTarget, this.selectedRecipe,
this.visibleEntries.size(), COLUMNS, TILE_HEIGHT, this.scrollTrackHeight());
	}

	private void revealSelection() {
		this.moveSelection(0, 0);
	}

	@Override
public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
if (!this.isVisible() || !this.containsRecipeGrid(mouseX, mouseY) || vertical == 0) {
			return false;
		}
this.scrollTarget = this.clampScroll(this.scrollTarget - vertical * TILE_HEIGHT);
if (!this.visibleEntries.isEmpty()) {
int firstFullRow = (int) Math.ceil(this.scrollTarget / TILE_HEIGHT);
int lastFullRow = Math.max(firstFullRow,
(int) Math.floor((this.scrollTarget + this.scrollTrackHeight()) / TILE_HEIGHT) - 1);
this.selectedRecipe = Math.clamp(this.selectedRecipe,
Math.min(this.visibleEntries.size() - 1, firstFullRow * COLUMNS),
Math.min(this.visibleEntries.size() - 1, (lastFullRow + 1) * COLUMNS - 1));
}
		return true;
	}

private boolean containsPanel(double x, double y) {
return x >= this.panelX && x < this.panelX + this.panelWidth
&& y >= this.panelY + TAB_TOP_OFFSET && y < this.panelY + this.panelHeight;
}

private boolean containsRecipeGrid(double x, double y) {
return x >= this.panelX + GRID_X
&& x < this.panelX + GRID_X + COLUMNS * this.tileWidth
&& y >= this.panelY + this.gridTop()
&& y < this.panelY + this.gridTop() + this.scrollTrackHeight();
}

private void scrollToTrackPosition(int offsetY) {
this.scrollTarget = this.clampScroll(offsetY * this.clampScroll(Double.MAX_VALUE) / this.scrollTrackHeight());
this.scrollOffset = this.scrollTarget;
}

	public List<NavigationPoint> navigationPoints() {
		List<NavigationPoint> points = new ArrayList<>();
points.add(new NavigationPoint(
this.activationButtonX() + MODE_BUTTON_WIDTH / 2,
this.panelY + 4 + (SEARCH_ROW_HEIGHT - 1) / 2,
9
));
points.add(new NavigationPoint(
this.filterButtonX() + FILTER_BUTTON_WIDTH / 2,
this.panelY + 4 + (SEARCH_ROW_HEIGHT - 1) / 2,
9
));
for (int tab = 0; tab < this.tabs.size(); tab++) {
points.add(new NavigationPoint(
this.tabX(tab) + this.tabButtonWidth(tab) / 2,
this.panelY + TAB_TOP_OFFSET + TAB_HEIGHT / 2,
Math.min(10, this.tabButtonWidth(tab) / 2)
));
		}
if (this.isSearchTab()) {
points.add(new NavigationPoint(
this.panelX + 22 + this.searchFieldWidth() / 2,
this.panelY + SEARCH_ROW_Y + SEARCH_ROW_HEIGHT / 2,
8
));
			if (!this.searchBox.getValue().isEmpty()) {
				points.add(new NavigationPoint(
					this.panelX + this.panelWidth - 11,
					this.panelY + SEARCH_ROW_Y + SEARCH_ROW_HEIGHT / 2,
					6
				));
			}
}
for (int index = 0; index < this.visibleEntries.size(); index++) {
int top = this.recipeY(index);
if (top >= this.panelY + this.gridTop()
&& top + TILE_HEIGHT <= this.panelY + this.gridTop() + this.scrollTrackHeight()) {
points.add(this.recipePoint(index));
}
		}
		return points;
	}

	private NavigationPoint recipePoint(int index) {
return new NavigationPoint(
this.panelX + GRID_X + index % COLUMNS * this.tileWidth + this.tileWidth / 2,
this.recipeY(index) + TILE_HEIGHT / 2,
Math.min(12, this.tileWidth / 2)
);
	}

	/** Move the virtual cursor through the actual recipe list, including off-screen rows. */
	public NavigationPoint navigateRecipeAt(double x, double y, int dx, int dy) {
int column = (int) Math.floor((x - this.panelX - GRID_X) / this.tileWidth);
int row = (int) Math.floor((y - this.panelY - this.gridTop() + Math.round(this.scrollOffset)) / TILE_HEIGHT);
if (!this.containsRecipeGrid(x, y) || column < 0 || column >= COLUMNS) {
			return null;
		}
int index = row * COLUMNS + column;
		if (dy < 0 && index >= 0 && index < COLUMNS) {
			if (this.isSearchTab()) {
				this.focusSearch();
				return new NavigationPoint(
					this.panelX + 22 + this.searchFieldWidth() / 2,
					this.panelY + SEARCH_ROW_Y + SEARCH_ROW_HEIGHT / 2,
					8
				);
			}
			KeyboardFocus toggle = this.preferredToggleForRecipe(index);
			this.keyboardFocus = toggle;
			int targetX = toggle == KeyboardFocus.ACTIVATION
				? this.activationButtonX() + MODE_BUTTON_WIDTH / 2
				: this.filterButtonX() + FILTER_BUTTON_WIDTH / 2;
			return new NavigationPoint(targetX,
				this.panelY + 4 + (SEARCH_ROW_HEIGHT - 1) / 2, 9);
		}
		if (index >= this.visibleEntries.size()
			|| (dx < 0 && column == 0) || (dx > 0 && column == COLUMNS - 1)
			|| (dy > 0 && index / COLUMNS == (this.visibleEntries.size() - 1) / COLUMNS)) {
			return null;
		}
		this.selectedRecipe = index;
		this.moveSelection(dx, dy);
// A virtual cursor must land on the visible tile now, not its future animated position.
this.scrollOffset = this.scrollTarget;
		return this.recipePoint(this.selectedRecipe);
	}

	/** Explicitly links the search, toggles, tabs, and recipe grid for controller focus. */
	public NavigationPoint navigateControlAt(double x, double y, int dx, int dy) {
		int controlY = this.panelY + 4;
		boolean onControls = x >= this.activationButtonX() && x < this.filterButtonX() + FILTER_BUTTON_WIDTH
			&& y >= controlY && y < controlY + SEARCH_ROW_HEIGHT - 1;
		boolean onSearch = this.isSearchTab()
			&& x >= this.panelX + PANEL_INSET && x < this.panelX + this.panelWidth - PANEL_INSET
			&& y >= this.panelY + SEARCH_ROW_Y && y < this.panelY + SEARCH_ROW_Y + SEARCH_ROW_HEIGHT;
		int currentTab = this.tabAt(x, y);

		if (onControls) {
			boolean onActivation = x < this.filterButtonX();
			this.keyboardFocus = onActivation ? KeyboardFocus.ACTIVATION : KeyboardFocus.FILTER;
			if (dx != 0) {
				KeyboardFocus next = onActivation ? KeyboardFocus.FILTER : KeyboardFocus.ACTIVATION;
				this.keyboardFocus = next;
				int targetX = next == KeyboardFocus.ACTIVATION
					? this.activationButtonX() + MODE_BUTTON_WIDTH / 2
					: this.filterButtonX() + FILTER_BUTTON_WIDTH / 2;
				return new NavigationPoint(targetX, controlY + (SEARCH_ROW_HEIGHT - 1) / 2, 9);
			}
			if (dy < 0) {
				this.keyboardFocus = KeyboardFocus.TABS;
				return this.tabPoint(this.selectedTab);
			}
			if (dy > 0) {
				if (this.isSearchTab()) {
					this.focusSearch();
					return this.searchPoint();
				}
				this.keyboardFocus = KeyboardFocus.RECIPES;
				this.moveSelection(0, 0);
				return this.recipePoint(this.selectedRecipe);
			}
		} else if (onSearch) {
			this.keyboardFocus = KeyboardFocus.SEARCH;
			if (dy < 0) {
				this.blurSearch();
				KeyboardFocus next = this.preferredToggleForX(x);
				this.keyboardFocus = next;
				return this.togglePoint(next);
			}
			if (dy > 0) {
				this.blurSearch();
				this.keyboardFocus = KeyboardFocus.RECIPES;
				this.moveSelection(0, 0);
				return this.recipePoint(this.selectedRecipe);
			}
		} else if (currentTab >= 0) {
			this.keyboardFocus = KeyboardFocus.TABS;
			if (dy > 0) {
				KeyboardFocus next = this.preferredToggleForX(x);
				this.keyboardFocus = next;
				return this.togglePoint(next);
			}
			if (dx != 0) {
				int nextTab = Math.clamp(currentTab + dx, 0, this.tabs.size() - 1);
				this.selectTab(nextTab);
				this.keyboardFocus = KeyboardFocus.TABS;
				return this.tabPoint(nextTab);
			}
		}
		return null;
	}

	private int tabAt(double x, double y) {
		if (y < this.panelY + TAB_TOP_OFFSET || y >= this.panelY + TAB_TOP_OFFSET + TAB_HEIGHT) {
			return -1;
		}
		for (int tab = 0; tab < this.tabs.size(); tab++) {
			int left = this.tabX(tab);
			if (x >= left && x < left + this.tabButtonWidth(tab)) {
				return tab;
			}
		}
		return -1;
	}

	private NavigationPoint tabPoint(int tab) {
		return new NavigationPoint(this.tabX(tab) + this.tabButtonWidth(tab) / 2,
			this.panelY + TAB_TOP_OFFSET + TAB_HEIGHT / 2,
			Math.min(10, this.tabButtonWidth(tab) / 2));
	}

	private NavigationPoint searchPoint() {
		return new NavigationPoint(this.panelX + 22 + this.searchFieldWidth() / 2,
			this.panelY + SEARCH_ROW_Y + SEARCH_ROW_HEIGHT / 2, 8);
	}

	private KeyboardFocus preferredToggleForX(double x) {
		int activationX = this.activationButtonX() + MODE_BUTTON_WIDTH / 2;
		int filterX = this.filterButtonX() + FILTER_BUTTON_WIDTH / 2;
		return Math.abs(x - activationX) <= Math.abs(x - filterX)
			? KeyboardFocus.ACTIVATION : KeyboardFocus.FILTER;
	}

	private NavigationPoint togglePoint(KeyboardFocus toggle) {
		int targetX = toggle == KeyboardFocus.ACTIVATION
			? this.activationButtonX() + MODE_BUTTON_WIDTH / 2
			: this.filterButtonX() + FILTER_BUTTON_WIDTH / 2;
		return new NavigationPoint(targetX,
			this.panelY + 4 + (SEARCH_ROW_HEIGHT - 1) / 2, 9);
	}

public boolean isAtTopOfRecipeList(double x, double y) {
int column = (int) Math.floor((x - this.panelX - GRID_X) / this.tileWidth);
int row = (int) Math.floor((y - this.panelY - this.gridTop() + Math.round(this.scrollOffset)) / TILE_HEIGHT);
return this.scrollOffset == 0
&& row == 0
&& column >= 0
&& column < COLUMNS
&& column < this.visibleEntries.size();
}

public boolean isAtBottomOfRecipeList(double x, double y) {
int column = (int) Math.floor((x - this.panelX - GRID_X) / this.tileWidth);
int row = (int) Math.floor((y - this.panelY - this.gridTop() + Math.round(this.scrollOffset)) / TILE_HEIGHT);
if (!this.containsRecipeGrid(x, y) || column < 0 || column >= COLUMNS) {
return false;
}
int index = row * COLUMNS + column;
return index < this.visibleEntries.size()
&& index / COLUMNS == (this.visibleEntries.size() - 1) / COLUMNS;
}

	public void controllerMoveSelection(int dx, int dy) {
if (this.keyboardFocus == KeyboardFocus.ACTIVATION || this.keyboardFocus == KeyboardFocus.FILTER) {
if (dx != 0) {
this.keyboardFocus = this.keyboardFocus == KeyboardFocus.ACTIVATION
? KeyboardFocus.FILTER : KeyboardFocus.ACTIVATION;
} else if (dy < 0) {
this.keyboardFocus = KeyboardFocus.TABS;
} else if (dy > 0) {
if (this.isSearchTab()) {
this.focusSearch();
} else {
this.keyboardFocus = KeyboardFocus.RECIPES;
this.moveSelection(0, 0);
}
}
return;
}
if (this.keyboardFocus == KeyboardFocus.TABS) {
if (dx != 0) {
this.controllerNextCategory(dx);
} else if (dy > 0) {
this.keyboardFocus = KeyboardFocus.ACTIVATION;
}
return;
}
if (this.keyboardFocus == KeyboardFocus.SEARCH) {
if (dy < 0) {
this.controllerExitSearchUp();
} else if (dy > 0) {
this.controllerExitSearchDown();
}
return;
}
if (dy < 0 && this.selectedRecipe < COLUMNS && !this.visibleEntries.isEmpty()) {
if (this.isSearchTab()) {
this.focusSearch();
} else {
this.keyboardFocus = this.preferredToggleForRecipe(this.selectedRecipe);
}
return;
}
		this.moveSelection(dx, dy);
	}

public void controllerExitSearchUp() {
this.blurSearch();
this.keyboardFocus = KeyboardFocus.ACTIVATION;
}

public void controllerExitSearchDown() {
this.blurSearch();
this.keyboardFocus = KeyboardFocus.RECIPES;
this.moveSelection(0, 0);
}

	public void controllerActivateSelected() {
		if (this.keyboardFocus == KeyboardFocus.ACTIVATION) {
			this.toggleActivationMode();
			return;
		}
		if (this.keyboardFocus == KeyboardFocus.FILTER) {
			this.toggleCraftableFilter();
			return;
		}
		if (this.keyboardFocus == KeyboardFocus.SEARCH) {
			this.focusSearch();
			return;
		}
		if (this.keyboardFocus == KeyboardFocus.TABS) {
			return;
		}
		if (!this.visibleEntries.isEmpty()) {
			this.activateTile(this.visibleEntries.get(this.selectedRecipe));
		}
	}

	public boolean isSearchInputFocused() {
		return this.isSearchTab() && this.searchBox != null && this.searchBox.isFocused();
	}

	public EditBox searchBoxForController() {
		return this.searchBox;
	}

	public void controllerFocusSearch() {
		if (this.isSearchTab() && this.searchBox != null) {
			this.focusSearch();
		}
	}

	private void focusSearch() {
		if (this.searchBox == null || !this.isSearchTab()) {
			return;
		}
		this.keyboardFocus = KeyboardFocus.SEARCH;
		if (this.minecraft != null && this.minecraft.gui.screen() != null) {
			this.minecraft.gui.screen().setFocused(this);
		}
		// Setting screen focus can blur the registered vanilla EditBox, so refocus it last.
		this.searchBox.setFocused(true);
	}

	private void blurSearch() {
		if (this.searchBox != null) {
			this.searchBox.setFocused(false);
		}
		if (this.minecraft != null && this.minecraft.gui.screen() != null
			&& this.minecraft.gui.screen().getFocused() == this) {
			this.minecraft.gui.screen().setFocused(null);
		}
	}

	public void controllerBlurSearch() {
this.blurSearch();
	}

	public boolean isSearchTabSelected() {
		return this.isSearchTab();
	}

	public boolean isSelectionAtTopOfRecipeList() {
		return !this.visibleEntries.isEmpty()
&& this.scrollOffset == 0
			&& this.selectedRecipe >= 0
			&& this.selectedRecipe < COLUMNS;
	}

	public void controllerToggleCraftableFilter() {
this.toggleCraftableFilter();
	}

	public void controllerNextCategory(int direction) {
this.keyboardFocus = KeyboardFocus.TABS;
		this.selectTab(Math.floorMod(this.selectedTab + direction, this.tabs.size()));
	}

	public void controllerToggleActivationMode() {
		this.toggleActivationMode();
	}

	private void toggleActivationMode() {
		ModSettings.ActivationMode current = ModSettings.getActivationMode();
ModSettings.setActivationMode(current == ModSettings.ActivationMode.CRAFT_IMMEDIATELY
? ModSettings.ActivationMode.FILL_GRID_ONLY
: ModSettings.ActivationMode.CRAFT_IMMEDIATELY);
	}

	@Override
	public boolean capturesInput() {
		return this.isVisible() && this.isSearchTab() && this.searchBox != null && this.searchBox.isFocused();
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return this.capturesInput() && this.searchBox.charTyped(event);
	}

	@Override
	public boolean preeditUpdated(PreeditEvent event) {
		return this.capturesInput() && this.searchBox.preeditUpdated(event);
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
if (!focused && this.searchBox != null && this.searchBox.isFocused()) {
// Like vanilla RecipeBookComponent, do not let an outer focus change blur the
// text field. Explicit user navigation/clicks call blurSearch instead.
SearchInputDiagnostics.lifecycle(this, "panel lost focus; text field preserved");
		}
	}

	@Override
	public boolean isFocused() {
		return this.focused || (this.isSearchTab() && this.searchBox != null && this.searchBox.isFocused());
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal("Crafting recipes"));
		if (!this.visibleEntries.isEmpty()) {
			output.add(
				net.minecraft.client.gui.narration.NarratedElementType.USAGE,
				Component.literal("Selected recipe " + (this.selectedRecipe + 1) + " of " + this.visibleEntries.size())
			);
		}
	}

	@Override
	public void recipesUpdated() {
		super.recipesUpdated();
		this.refreshRecipes();
	}

	private record CategoryTab(String name, ItemStack icon, RecipeGroup group) {
	}

	private record RecipeGroupKey(String value, String label) {
	}

	private record RecipeGroupModel(RecipeGroupKey key, List<RecipeDisplayEntry> entries) {
	}

	private record RecipeTile(RecipeDisplayEntry entry, RecipeGroupModel group, boolean groupHeader) {
	}

private enum RecipeGroup {
CONSTRUCTION,
EQUIPMENT,
ITEMS,
NATURE,
SEARCH
}

private enum KeyboardFocus {
RECIPES,
SEARCH,
ACTIVATION,
FILTER,
TABS
}

	public record NavigationPoint(int x, int y, int radius) {
	}

	private record QueuedCraft(RecipeDisplayEntry entry, ItemStack expectedResult, boolean instantCraft) {
	}

	private static final class PendingCraft {
		private final ItemStack expectedResult;
		private final int initialMenuState;
		private int waitTicks;
		private int outputMenuState;
		private boolean outputRequested;

		private PendingCraft(ItemStack expectedResult, int initialMenuState) {
			this.expectedResult = expectedResult;
			this.initialMenuState = initialMenuState;
		}

		private ItemStack expectedResult() {
			return this.expectedResult;
		}

		private int initialMenuState() {
			return this.initialMenuState;
		}

		private boolean outputRequested() {
			return this.outputRequested;
		}

		private int outputMenuState() {
			return this.outputMenuState;
		}

		private void markOutputRequested(int menuState) {
			this.outputRequested = true;
			this.outputMenuState = menuState;
			this.waitTicks = 0;
		}
	}
}
