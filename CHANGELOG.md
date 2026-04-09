# Changelog

## 0.2.0

This release is a major rewrite from the last beta 0.1.3 tss_trim_slab build. BACKUP BEFORE LOADING A WORLD FROM 0.1.2-0.1.3.

### Rename

- Mod codebase and filenaming updated to toms-extensions

### Added

- Deep paint kit integration for trim slabs, including capture-time facade profiling.
- Painted trim slab block entity support for copied facade state, runtime facade behavior, and model data.
- Mirrored block light emission and particle emission from copied source blocks, with config toggles.
- Dedicated magma trim slab path for vanilla-matching magma visuals, lighting, and rain smoke without `hurtsToWalkOn`.
- Expanded facade rendering support for grass tint, leaves, cutout, translucent glass-like blocks, slab/full-block lookup, and half-block sampling exceptions.
- Modern resource/data setup including recipes, loot tables, tags, client wrappers, and registry cleanup.

### Changed

- Migrated from the original `tss_trim_slab` prototype into the `toms_extensions` mod namespace and package layout.
- Reworked painted slab rendering to use facade profiles and source-aware routing instead of the original simple sprite remap only.
- Reworked copied light handling to be server-authored and stored on blockstate, instead of relying on client-only refresh behavior.
- Reworked paint application flow so copied blocks can mirror more of their source behavior while staying on trim slab hosts.

### Fixed

- Grass tint and layered grass-side rendering on painted slabs.
- Glass and other transparent facade rendering, including underside visibility and skylight behavior.
- Leaves, shroomlight, and other facade-specific slab texture sampling paths.
- Redstone ore and redstone lamp runtime facade activation, lighting, and texture switching.

### Removed

- Old transparent painted slab compatibility host.
- Older generic lighting workaround code replaced for accurate mirrored-light system.

### Breaking Changes

- Saves from toms/tss-trim-slab with trim slab blocks from pre-0.2.0 worlds will lose paint or may break entirely on world load. BACKUP BEFORE TRYING.
- 0.1.2-0.1.3 migration and block conversion for legacy blocks is in place but not guaranteed. BACKUP BEFORE TRYING.

## 0.1.2 and 0.1.3 Deprecation

- Last published prototype build under the older `tss_trim_slab` / `toms_trim_slab` codebase.
- Included the original trim slab, painted trim slab, and basic painted slab renderer.
- Migration path in place (but not guaranteed) to keep placed slabs but any applied paint overlay will not migrate.
