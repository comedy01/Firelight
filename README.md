# Firelight

Flickering firelight for Minecraft. Torches, lanterns, candles, campfires and fire make the light around them dip and
flare softly, each flame on its own rhythm, on the ground, the walls and the mobs nearby.

Firelight is a small client-side mod for Minecraft on Fabric, NeoForge and Forge, compatible with Mod Menu on Fabric.

Supported versions: Fabric 1.18.2 and 1.19 to 26.3, NeoForge 1.21 to 26.3, Forge 1.16.5, 1.18.2, 1.19.2 and 1.20.1.
Works with Sodium and Embeddium.

## Features

- **Every flame flickers** - torches, soul torches, copper torches, lanterns, jack o'lanterns, candles, candle cakes,
  campfires, fire and lit furnaces, smokers and blast furnaces.
- **Each kind its own way** - campfires and fire flicker the most, torches less, lanterns stay calm behind their glass
  and furnaces glow with a slow pulse. Two torches side by side never flicker in step.
- **Mobs and players too** - anything standing near a flame flickers along with the ground under it, items and block
  entities like chests included.
- **Adjustable strength** - from a faint shimmer to a restless blaze, and each kind of flame can be turned off on its
  own.
- **Built to stay fast** - the flicker runs in the terrain shader, so no chunks are ever rebuilt and there is no
  measurable frame rate cost.
- **Client-side only** - nothing to install on a server.

## Install

**Fabric**

1. Install [Fabric Loader](https://fabricmc.net/use/) for your version of Minecraft.
2. Put the Fabric Firelight jar for your version in your `mods` folder. Fabric API is not needed.
3. Start the game.

Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to get a settings screen (*Mods > Firelight > Configure*).

**NeoForge**

1. Install NeoForge for your version of Minecraft.
2. Put the NeoForge Firelight jar in your `mods` folder.
3. Start the game. Settings are under *Mods > Firelight > Config*.

**Forge (1.16.5, 1.18.2, 1.19.2, 1.20.1)**

1. Install Forge for your version of Minecraft.
2. Put the Forge Firelight jar in your `mods` folder.
3. Start the game. Settings are under *Mods > Firelight > Config*.

## Notes

- With an Iris shader pack turned on, the shader pack draws the world and Firelight's terrain flicker pauses until you
  turn it off again.
- On Forge 1.20.1 with Embeddium, Embeddium writes a line to the log saying a mod changed its internals. That is
  Firelight adding the flicker to Embeddium's terrain shader; it is harmless.

## Building

`./gradlew build -Pminecraft_version=26.3` builds the Fabric jar; add `-Ploader=neoforge` or `-Ploader=forge` for the
other loaders (version configs in `versions/`). Forge 1.16.5 builds from `legacy-forge/`:
`cd legacy-forge && ./gradlew build`. `./gradlew runSelftest` launches the game and runs the in-world checks.

## License

MIT
