# Bedrock Recipe Book

Bedrock Recipe Book is a client-side Fabric mod for Minecraft Java Edition 26.3. It replaces the recipe-book interface with a layout inspired by the Bedrock desktop UI while continuing to use Java's recipe data and normal server-validated crafting. Supports Controlify integration. Made for people that want their bedrock friends to play java. Might expand to more UIs in the future.

I am sorry this is vibecoded. Use at your own risk
As such, there is no issues page. 
Fork this repo and use Microsofts free AI in CodeSpaces to diagnose the issue, then have it apply the fixes. 
Make a pull request.
May or may not ever merge, I have notifications turned off.

## Features

- Bedrock-inspired recipe panel for both inventory crafting and crafting tables.
- Uses the java recipe book underneath. Does not mimic bedrock edition categories, the order they are in, or unique recipes from bedrock.
- Optional Controlify support adds controller navigation, category switching with shoulder buttons, and controller text input for search.
- Toggle between showing all recipes and craftable recipes only.
- Choose instant crafting or filling the crafting grid.
- collapsible related recipes.
- general ai vibecoded weirdness. Might not work for you like it does for me. Or at all possibly.

## Build

GitHub Actions runs the project checks and build on pushes and pull requests. Successful builds are available to download from the workflow run's **Artifacts** section.

To build locally from this directory:

```sh
./gradlew build
```

To launch a development client:

```sh
./gradlew runClient
```

The mod JAR is generated in `build/libs/`.

## Requirements

### Required to use the mod

- Minecraft Java Edition 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API 0.165.2+26.3

### Optional

- Controlify 3.5.3 for Minecraft 26.3 enables the enhanced controller support. The mod also works without Controlify.

### Required to build from source

- Java 25 or newer. The included Gradle wrapper downloads and runs the project's Gradle version.
