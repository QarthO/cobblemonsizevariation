# Cobblemon Size Variations — server-only Fabric fork

A server-only fork of [Cudzer/cobblemonsizevariation](https://github.com/Cudzer/cobblemonsizevariation),
for **Minecraft 1.21.1 / Fabric / Cobblemon 1.7.3**. Clients use the ordinary
Cobblemon modpack; they do **not** install Size Variations.

- Random sizes for naturally spawned, Poké Snack, starter and fossil Pokémon.
- Basic and Gen IX sampling, size bounds, chance, bias and species overrides.
- Public, read-only `/checkpokemonsize <1–6>` with single-line size and a hover range.
- Reloadable text, colors and hover width through operator `/pokemonsize reload`.
- Parent-influenced offspring sizes with optional Cobbreeding 2.2.2 integration.
- Operator `/pokesizer` commands and shoulder/riding size limits.
- Persistent assignments: duplicate events do not reroll a Pokémon or retry a
  failed chance roll. Existing non-default sizes from other mods are preserved.
- Live size changes use Cobblemon's native tracked entity data; party/PC updates
  use Cobblemon's built-in packets. No new item registrations or custom packets.

Essence items/recipes and the client PC/summary size labels are removed.
This fork builds **Fabric only**. Upstream's NeoForge target is not maintained here.

The default response is `Bulbasaur · Big (1.35)`. Hover to see Min/Max and the
current value positioned along the range. Hover is enabled by default at 160
pixels, aligned for Minecraft's default font. Existing message templates are
preserved; new installations get this layout automatically.

See [installation, commands and testing](docs/SERVER_ONLY.md).

## Build

JDK 21:

```sh
bash gradlew clean build --no-daemon --console=plain
python3 scripts/verify_server_jar.py
```

Install the remapped JAR from `build/libs/`. GitHub Actions publishes a
commit-named JAR artifact with a SHA-256 digest on each PR and default-branch push.

Original author: Cudzer. The upstream copyright/license is retained in LICENSE.txt.
