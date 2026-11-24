package dev.bedrockrecipebook.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Offline Bedrock catalog order, translated into Java identifiers at import time. */
public final class BedrockRecipeCatalog {
    public enum Category {
        CONSTRUCTION, EQUIPMENT, ITEMS, NATURE
    }

    public record Entry(Category category, String group, String label, int order) {
        public boolean isGrouped() {
            return !this.group.isEmpty();
        }
    }

    private static final Map<String, Entry> ENTRIES = load();

    private BedrockRecipeCatalog() {
    }

    public static Entry find(String javaItemId) {
        return ENTRIES.get(javaItemId);
    }

    public static int size() {
        return ENTRIES.size();
    }

    private static Map<String, Entry> load() {
        var stream = BedrockRecipeCatalog.class.getResourceAsStream("/bedrock_recipe_book/catalog.tsv");
        if (stream == null) {
            throw new IllegalStateException("Missing bundled Bedrock recipe catalog");
        }
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int order = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length != 4) {
                    throw new IllegalStateException("Malformed Bedrock catalog row: " + line);
                }
                var category = Category.valueOf(columns[0].toUpperCase(java.util.Locale.ROOT));
                String group = columns[1].equals("-") ? "" : columns[1];
                String label = columns[2].equals("-") ? "" : columns[2];
                if (entries.putIfAbsent(columns[3], new Entry(category, group, label, order++)) != null) {
                    throw new IllegalStateException("Duplicate Bedrock catalog item: " + columns[3]);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled Bedrock recipe catalog", exception);
        }
        return Collections.unmodifiableMap(entries);
    }
}
