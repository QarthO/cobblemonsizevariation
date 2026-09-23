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

`bash gradlew clean build` runs unit/regression tests for chance, repeat
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


## Public size inspection (server.2)

Anyone can run `/checkpokemonsize 3` for their third party slot. Slots are 1–6;
empty slots return a friendly error. Only the caller receives the colored chat
response. It shows nickname/species, category, scale multiplier, percentage of
normal scale, current wild roll bounds and wild size-roll chance. Species
bounds from datapacks override the global range. It reads data only: it never
rerolls, marks an assignment, or grants the editing command's privileges.

The range describes today's server configuration for that species, not proof
of a Pokémon's origin or the settings when it was caught. At a failed wild size
roll, a normal Pokémon stays 1×; external/admin sizes may lie outside the range.
Basic category definitions have small gaps; those values display `Unclassified`
rather than failing the command or inventing a category. The exact scale still
appears. The PC/summary UI still needs no addon and gains no custom label.

## Parent-influenced breeding (server.2)

With Cobbreeding **2.2.2** and its `cobblemonSizeVariationsCompatEnabled=true`, new
eggs with a known child species receive a size when their actual parent pair is
selected. Both parents (including Ditto) contribute their relative scale, not
absolute species height. The child species' bounds apply; each parent is clamped
to those bounds before averaging, so externally oversized parents cannot force
out-of-range children.

New size-mod config fields, added automatically to existing config files:

```json
"breedingSizeInheritance": true,
"breedingParentInfluence": 0.75
```

The calculation is `influence × parentAverage + (1 − influence) × randomRoll`.
The fresh random roll uses the configured Basic/Gen IX sampler and bias setting,
with the child's species override when present. At 0.75, two 1.5× parents with
bounds 0.2–2.0 produce sizes from about 1.175× to 1.625×. These are bounds, not a
promise of reaching either endpoint. This is a simple inherited-size model,
not a simulation of genes. Influence 0 gives independent rolls; 1 gives exactly
the clamped parent average. Valid influence is finite and between 0 and 1.

The result is stored as native `scale_modifier` in the egg's Pokémon properties.
It survives item serialization/encryption and is preserved at hatch, even if
inheritance is subsequently disabled. Hatching marks the size assigned to avoid
later rerolls. No parent IDs need to remain available when hatching. Other egg
properties, breeding compatibility, IVs, nature and shiny rolls are unchanged.
The wild 50% size-modification chance does not apply to breeding; Cobbreeding's
previous integration also rolled every child when enabled.

Old/admin eggs without a stored size retain Cobbreeding's previous independent
hatch roll. Explicitly sized eggs are preserved. Random-species eggs (e.g.
Ditto/Ditto random offspring) keep the legacy hatch roll because their species
is not yet resolved at the parent-selection hook. This feature is disabled on
other Cobbreeding versions, with a warning, rather than applying unverified
hooks. Cobbreeding remains optional; without it, size inspection and wild sizing
continue to work.

## Native Cobblemon support

For the pinned Cobblemon 1.7.3, `scaleModifier` defaults to 1.0 and native packets
render and store other scales. An operator can use
`/pokeedit 3 scale_modifier=1.25` without this addon or a datapack. This is manual
editing, not an ordinary-player growth system or automatic random wild-size
setting. Breeding in this pack comes from Cobbreeding. The addon supplies the
random sizing policy and the new parental inheritance policy.


### server.2 verification (2026-09-23)

- Clean Java 21 build: all 10 unit tests passed (seven sizing regressions plus
  three inheritance-policy tests); server-only release content verification passed.
- Disposable Fabric 1.21.1 / Cobblemon 1.7.3 runtime, with Cobbreeding 2.2.2:
  permission-zero command execution, private colored response, no Pokémon NBT
  mutation, empty/invalid slots, editing denied, category-gap fallback, species
  overrides, actual egg parent-selection hook, varying sibling sizes, encrypted
  property and egg-item round-trips, real hatch preservation (including exact
  1.0), assignment marker, and inheritance disable all passed.
- The same smoke build without Cobbreeding also passed. No hard breeding-mod
  dependency or custom client packet was introduced.
- A synthetic-player hatch fixture initially checked party capacity as if it
  were occupancy. Correcting that assertion exposed no production defect. The
  synthetic player also triggers missing player-data file warnings in the
  disposable world; no real player data was used.
- Runtime logs: `/tmp/size-inheritance-runtime.log` and
  `/tmp/size-no-breeding-runtime.log`. No M23 deployment for server.2 yet.

M23 configuration was inspected read-only: Basic algorithm, wild size chance
0.5, average bias off, Cobbreeding size compatibility enabled. Its existing
settings were not modified.
