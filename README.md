# Tom's Simple Storage: Extensions

## **Download the `0.2.1` Release**
# *[Curseforge](https://www.curseforge.com/minecraft/mc-mods/toms-extensions/files?page=1&pageSize=20&showAlphaFiles=hide)*
# *[Modrinth](https://modrinth.com/mod/toms-extensions/versions)*

An addon for Tom's Simple Storage Mod that adds trim slab variants and extends Tom's Paint Kit function.

`0.2.1` is the current release. It builds on Tom's Simple Storage Mod's custom solutions and focuses on adding the Inventory Trim slab variant. The Inventory Trim Slab carries Tom's Inventory Trim network function and is paintable. The Paint Kit function has been expanded for slab paint application and paint-sourcing including additional effects such as light emission and particle effects.

## Features

- Adds `Inventory Trim Slab`
- Supports bottom, top, and double slab states
- Supports painting trim slabs with Tom's existing paint kit flow
- Mirrors copied block textures onto slab geometry
- Mirrors copied light emission and particle effects
- Handles grass tint, leaves, glass-like facades, cutout rendering, and certain block-specific slab sampling
- Adds recipes for trim to trim slab and vice versa

## Supported Versions

- Minecraft `1.20.1`
- Forge `47.4.0+`
- Tom's Simple Storage `1.20-1.7.1+`

## Install

1. Install Minecraft Forge `47.4.0` or newer
2. Place Tom's Simple Storage Mod, `toms_storage-1.20-1.7.1` or newer, in your `mods` folder
3. Place `toms_extensions-0.2.1.jar` in your `mods` folder
4. Launch the game

## Configuration

Server config file:

- `config/toms_extensions-server.toml`

Important toggles:

- `enableTrimSlabs = true`
- `allowTrimSlabPlacement = true`
- `allowTrimSlabRecipes = true`
- `allowTrimSlabPainting = true`
- `mirrorPaintedBlockLightEmission = true`
- `mirrorPaintedBlockParticleEmission = true`

Recipe visibility is evaluated during recipe load. If you change recipe settings, restart the game/server or run `/reload`.

## Release Notes

Highlights in `0.2.1`:

- Deep paint kit integration for trim slabs
- Painted slab runtime facade profiling
- Mirrored block light and passive particle behavior from copied source blocks
- Improved grass, leaves, glass, magma, and cutout rendering
- Cleanup of the older prototype codebase

See CHANGELOG.md for the full release notes.

## Notes

- This is an addon mod and requires Tom's Simple Storage Mod.
- Existing beta saves from version `0.1.2` and `0.1.3` may not migrate cleanly.
- Always BACKUP worlds before moving from beta builds to `0.2.1`.

## Development

- Main source: `src/main/java/com/dp/toms_extensions`
- Resources: `src/main/resources`
- Local dependency jars belong in `libs/`

Build:

```powershell
.\gradlew.bat build
```

Run client:

```powershell
.\gradlew.bat runClient
```
