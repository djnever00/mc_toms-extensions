# Changelog

## 0.2.1

This release fixes the dedicated-server startup issue in `0.2.0`.

### Fixed

- Fixed dedicated servers trying to load client-only classes during mod startup.

### Notes

- `0.2.1` is the current release.
- Back up worlds before loading saves from older beta builds.

## 0.2.0

This release is a major rewrite from the old beta `0.1.2` and `0.1.3` line It is the first release under the `toms_extensions` namespace.

### Rename

- Renamed the mod from `tss_trim_slab` / `toms_trim_slab` to `toms_extensions`.

### Added

- Trim slab variants for Tom's Simple Storage trim blocks.
- Painted trim slab support using Tom's paint kit workflow.
- Mirrored painted block light emission and passive particle effects.
- Shared facade profile capture and runtime routing for painted slabs.
- Dedicated magma trim slab path for correct vanilla-style magma rendering.
- Config toggles for painted block light and particle mirroring.
- Legacy namespace remaps for published `0.1.3` worlds.

### Changed

- Reworked painted slab rendering to use facade-specific routing for cutout, translucent, grass, light, and texture sampling behavior.
- Reworked lighting to be server-driven and blockstate-backed instead of relying on client-side relight hacks.
- Simplified glass and leaves handling back onto the stable generic painted slab path.
- Updated slab texture classification rules for side-sampled vs squished facade groups.

### Fixed

- Grass tint and layered grass-side rendering.
- Glass underside rendering, void-through transparency bugs, and skylight handling.
- Redstone ore and redstone lamp runtime texture and light syncing.
- Shroomlight, froglight, glowstone, sea lantern, crying obsidian, and other emissive facade consistency.
- Rain smoke for painted magma slabs.
- Dedicated server loading crash caused by client classes being loaded on the server.

### Removed

- Unused experimental compatibility code and stale lighting workarounds.
- Legacy transparent-host migration path that was never part of the published beta line.

### Breaking Changes

- Worlds from unpublished experimental builds may not retain those unpublished block IDs.
- Back up old saves before moving from beta builds to the `0.2.x` line.

## 0.1.2 and 0.1.3 Deprecation

- `0.1.2` and `0.1.3` were beta builds under the old namespace and are now deprecated.
- Published world migration should continue through legacy remaps for `trim_slab` and `painted_trim_slab`.
