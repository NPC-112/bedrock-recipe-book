package dev.bedrockrecipebook.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class ModSettings {
	private static final String ACTIVATION_MODE_KEY = "recipeActivation";
private static final String CRAFTABLE_ONLY_KEY = "craftableOnly";
	private static ActivationMode activationMode = ActivationMode.CRAFT_IMMEDIATELY;
private static boolean craftableOnly;
	private static Path settingsPath;

	private ModSettings() {
	}

	public static void load() {
		settingsPath = FabricLoader.getInstance().getConfigDir().resolve("bedrock-recipe-book.properties");
		if (!Files.exists(settingsPath)) {
			save();
			return;
		}

		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(settingsPath)) {
			properties.load(input);
			activationMode = ActivationMode.fromConfigValue(properties.getProperty(ACTIVATION_MODE_KEY));
craftableOnly = Boolean.parseBoolean(properties.getProperty(CRAFTABLE_ONLY_KEY, "false"));
		} catch (IOException exception) {
			BedrockRecipeBookClient.LOGGER.error("Could not read {}", settingsPath, exception);
		}
	}

	public static ActivationMode getActivationMode() {
		return activationMode;
	}

public static boolean isCraftableOnly() {
return craftableOnly;
}

public static void setCraftableOnly(boolean value) {
craftableOnly = value;
save();
}

	public static void setActivationMode(ActivationMode mode) {
		activationMode = mode;
		save();
	}

	private static void save() {
		if (settingsPath == null) {
			settingsPath = FabricLoader.getInstance().getConfigDir().resolve("bedrock-recipe-book.properties");
		}

		try {
			Files.createDirectories(settingsPath.getParent());
			Properties properties = new Properties();
			properties.setProperty(ACTIVATION_MODE_KEY, activationMode.configValue());
properties.setProperty(CRAFTABLE_ONLY_KEY, Boolean.toString(craftableOnly));
			try (OutputStream output = Files.newOutputStream(settingsPath)) {
				properties.store(output, "Bedrock Recipe Book settings");
			}
		} catch (IOException exception) {
			BedrockRecipeBookClient.LOGGER.error("Could not save {}", settingsPath, exception);
		}
	}

	public enum ActivationMode {
		CRAFT_IMMEDIATELY("craft_immediately"),
		FILL_GRID_ONLY("fill_grid_only");

		private final String configValue;

		ActivationMode(String configValue) {
			this.configValue = configValue;
		}

		public String configValue() {
			return configValue;
		}

		private static ActivationMode fromConfigValue(String value) {
			for (ActivationMode mode : values()) {
				if (mode.configValue.equals(value)) {
					return mode;
				}
			}
			return CRAFT_IMMEDIATELY;
		}
	}
}
