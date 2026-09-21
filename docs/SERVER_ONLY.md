# Server-only Fabric 1.21.1

## Installation

Install one fork JAR in the server's `mods/` directory. Remove the upstream Size
Variations JAR if present. Do not install this fork on clients. Keep the rest of the
Cobblemon pack installed as usual. Required: Java 21, Fabric Loader, Fabric API,
Fabric Language Kotlin and Cobblemon 1.7.3. Architectury is no longer a dependency
of this addon (other mods may still need it).

No essence items or recipes are registered. This is not an item-preserving
migration for a world that already contains upstream essence items. Take a backup
before replacing that build. Old `enableEssenceRecipes` config entries are ignored.

## Configuration and commands

Configuration remains in `config/cobblemonsizevariation/`:

- `config.json`: chance, algorithm, bias, shoulder/riding limits and permission levels.
- `sizes/basic.json` and `sizes/gen9.json`: minimum/maximum sizes and size categories.
- Datapacks: `data/<namespace>/custom_sizes/*.json` for species overrides.

Defaults are retained: 50% size-modification chance and basic bounds 0.2–2.0.
Restart after changing config files; datapack species definitions use normal reload.
The biased basic sampler now respects its minimum; Gen IX includes all values 0–255.
Invalid nonpositive, inverted or nonfinite sample ranges are rejected.

Examples (requires the permission levels in config):

```text
/pokesizer self Slot1 check
/pokesizer self Slot1 0.5
/pokesizer self Slot1 2.0
/pokesizer QarthO Slot1 1.0
```

Omit the size to randomize that party member; omit the member to randomize the team.
Wild Pokémon spawned through Cobblemon's natural spawner, Poké Snacks, starters,
and fossils get the size roll. This does not retroactively randomize an existing
world/party or hook every third-party/admin spawn command.

## Synchronization and persistence

`Pokemon.scaleModifier` is native Cobblemon data. Cobblemon 1.7.3's server delegate
already copies it into `PokemonEntity.SCALE_MODIFIER`; the stock client updates its
model and dimensions from this tracked field. This fork requests that normal
update immediately, preserving Cobblemon's transformation/disguise handling.
It neither respawns entities nor changes vanilla scale attributes.

For stored Pokémon, the fork marks the store dirty and sends the native
`SetPartyPokemonPacket` or `SetPCPokemonPacket` to that store's observers. Scale and
the `cobblemonsizevariation:assigned` marker persist in the Pokémon's normal data.
A failed chance roll is marked too, so duplicate spawn callbacks do not retry.

## Verification

`bash gradlew clean build` runs seven unit/regression tests for chance, repeat
assignment, existing external sizes, invalid bounds, and both samplers' bounds.
`python3 scripts/verify_server_jar.py` rejects a release containing client classes,
custom packets, recipes/assets or a smoke-test entrypoint.

A test-only entrypoint lives under `src/smoke/java`. Build it explicitly with
`bash gradlew remapJar -PserverSmoke` and place that JAR in a **disposable production
Fabric server** with Cobblemon 1.7.3, Fabric API and Fabric Language Kotlin. Use a
separate localhost-only world. It writes `csv-smoke-result.txt` and stops the
server after testing spawn events (including ownerless fossils), repeat events,
persistence, store dirty notifications, native packet serialization, live tracked
scale/hitbox updates and the absence of addon item registrations.

Do not ship the smoke JAR. Finish with a normal `clean build` and the release
verification script. The development `runServer` launcher is unsuitable here:
Cobblemon's published Kotlin metadata retains intermediary Minecraft names and
fails reflection in that named development environment.

Manual acceptance on the real pack: join without Size Variations, inspect several
new wild spawns, catch/recall/resend a Pokémon, reconnect, and resize a sent-out
party member with the commands above. Check another player's view, party/PC
persistence and riding/shoulder boundaries. Server tests cannot replace visual
client acceptance.
