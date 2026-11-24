package dev.bedrockrecipebook.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BedrockRecipeBookClient implements ClientModInitializer {
	public static final String MOD_ID = "bedrock_recipe_book";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ModSettings.load();
		LOGGER.info("Bedrock Recipe Book client initialized for Minecraft 26.3");
	}
}
