package dev.bedrockrecipebook.catalog;

public final class BedrockRecipeCatalogChecks {
    public static void main(String[] args) {
        check(BedrockRecipeCatalog.size() > 1000, "full catalog was loaded");
        category("bread", BedrockRecipeCatalog.Category.EQUIPMENT);
        category("oak_sign", BedrockRecipeCatalog.Category.ITEMS);
        category("white_dye", BedrockRecipeCatalog.Category.NATURE);
        category("red_bed", BedrockRecipeCatalog.Category.ITEMS);
        category("oak_planks", BedrockRecipeCatalog.Category.CONSTRUCTION);
        group("oak_stairs", "stone_stairs");
        group("oak_slab", "cobblestone_slab");
        group("wooden_spear", "diamond_spear");
        group("leather_helmet", "diamond_helmet");
        check(!item("wooden_sword").group().equals(item("wooden_pickaxe").group()),
            "tools are grouped by type, not material");
        check(!item("iron_helmet").group().equals(item("iron_chestplate").group()),
            "armor is grouped by type, not material");
        before("oak_planks", "cobblestone_wall");
        before("cobblestone_wall", "oak_fence");
        before("oak_fence", "oak_fence_gate");
        before("stone_stairs", "cobblestone_stairs");
        before("wooden_sword", "wooden_spear");
        before("wooden_spear", "wooden_axe");
        before("white_bed", "light_gray_bed");
        // Bedrock banner data values use the reverse dye mapping, unlike beds.
        before("black_banner", "gray_banner");
        before("white_dye", "light_gray_dye");
        check(!item("crafting_table").isGrouped(), "loose entries remain loose");
        check(item("stone_bricks").label().equals("Decorative Stone"), "Bedrock group names are preserved");
        check(BedrockRecipeCatalog.find("example:custom_item") == null, "custom recipes use the caller's fallback");
        System.out.println("Bedrock catalog checks passed (tabs, groups, ordering, aliases, beds, banners, and loose entries).");
    }

    private static BedrockRecipeCatalog.Entry item(String path) {
        var result = BedrockRecipeCatalog.find("minecraft:" + path);
        if (result == null) {
            throw new AssertionError("Missing catalog item: " + path);
        }
        return result;
    }

    private static void category(String path, BedrockRecipeCatalog.Category category) {
        check(item(path).category() == category, "category of " + path);
    }

    private static void group(String first, String second) {
        check(item(first).isGrouped() && item(first).group().equals(item(second).group()),
            "group membership of " + first + " and " + second);
    }

    private static void before(String first, String second) {
        check(item(first).order() < item(second).order(), first + " appears before " + second);
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
