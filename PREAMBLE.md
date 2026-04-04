Tom’s Simple Storage: Extensions is a Forge addon for **[Tom’s Simple Storage Mod](https://github.com/tom5454/Toms-Storage)** that adds an **Inventory Trim Slab** variant to Minecraft 1.20.1. **More block variants planned.**

## **Download the Beta Release - out now**
# *[Curseforge](https://www.curseforge.com/minecraft/mc-mods/toms-extensions/files?page=1&pageSize=20&showAlphaFiles=hide)*
# *[Modrinth](https://modrinth.com/mod/toms-extensions/versions)*

## Requires

*   **[Tom’s Simple Storage Mod](https://github.com/tom5454/Toms-Storage)**
*   **Forge 47.4.0**
*   **Minecraft 1.20.1**

## Features

*   Adds bottom, top, and double stack Inventory Trim Slabs
*   Inventory Trim network functions through the Inventory Trim Slab variant
*   Paintable Inventory Trim Slabs with smart texture mapping and rendering from source block/slab
*   Adds Paint Kit function: can copy slab textures as paint source
*   Heuristic slab/full-block matching for many common naming patterns when choosing paint source
*   User-configurable facade override mappings for edge-case modded blocks
*   Planned: floor/ceiling/wall/corner cable variants, some framed

**Paint Hint:** Copying a slab for slab targets and a full block for full-block targets is preferred.

## Notes

*   This is an **addon** for [Tom’s Simple Storage Mod](https://www.curseforge.com/minecraft/mc-mods/toms-storage).
*   For best paint results, copy a real slab variant when one exists.
*   When no slab variant is found, the mod will attempt to render a slab-style fallback (or stacked slab for block) from the source texture.
*   Some unusual or highly custom block models may still need manual config overrides.

## Config

Server config file:

*   `toms_extensions-server.toml`

Legacy config files:

*   `toms_trim_slab-server.toml`
*   `tss_trim_slab-common.toml`

Example override format:

*   `full_block_id=slab_block_id`

Examples:

*   `minecraft:bricks=minecraft:brick_slab`
*   `minecraft:quartz_block=minecraft:quartz_slab`
*   `some_mod:pavers=some_mod:paver_slab`

## Credits

Built as an addon for **[Tom’s Simple Storage Mod](https://github.com/tom5454/Toms-Storage)**.  
Please download the original mod separately.  
Thanks to **[tom5454](https://github.com/tom5454)** for the original mod!

Project code and image texture work were created with AI assistance.
