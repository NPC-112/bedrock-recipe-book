package dev.bedrockrecipebook.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.EditBox;

/** Identifies only our vanilla search widgets without retaining closed screens. Client-thread only. */
public final class RecipeSearchFields {
    private static final Set<EditBox> FIELDS = Collections.newSetFromMap(new WeakHashMap<>());

    private RecipeSearchFields() {}

    public static void register(EditBox field) {
        FIELDS.add(field);
    }

    public static boolean contains(EditBox field) {
        return FIELDS.contains(field);
    }
}
