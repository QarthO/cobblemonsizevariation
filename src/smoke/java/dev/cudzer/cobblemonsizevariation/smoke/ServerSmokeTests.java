package dev.cudzer.cobblemonsizevariation.smoke;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.FossilRevivedEvent;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyPokemonPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import dev.cudzer.cobblemonsizevariation.config.ModConfig;
import dev.cudzer.cobblemonsizevariation.sizing.ServerSizeService;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Loaded only by -PserverSmoke in a disposable local server. */
public final class ServerSmokeTests implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server);
                Files.writeString(Path.of("csv-smoke-result.txt"), "PASS\n");
                System.out.println("CSV SERVER SMOKE: PASS");
            } catch (Throwable error) {
                error.printStackTrace();
                try { Files.writeString(Path.of("csv-smoke-result.txt"), "FAIL: " + error); }
                catch (Exception ignored) {}
            } finally {
                server.halt(false);
            }
        });
    }

    private static void checkPublicCommand(MinecraftServer server) throws RuntimeException {
        var messages = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        var player = new net.minecraft.server.level.ServerPlayer(server, server.overworld(),
            new com.mojang.authlib.GameProfile(UUID.randomUUID(), "size-check"),
            net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component text) { messages.add(text); }
        };
        var source = player.createCommandSourceStack().withPermission(0);
        check(!source.hasPermission(2), "fixture must not be an operator");
        var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
        var pokemon = create(); pokemon.setScaleModifier(1.35f);
        party.set(new PartyPosition(2), pokemon);
        var before = pokemon.saveToNBT(server.registryAccess(), new net.minecraft.nbt.CompoundTag());
        var dispatcher = server.getCommands().getDispatcher();
        try {
            check(dispatcher.execute("checkpokemonsize 3", source) == 1, "public command failed");
            check(messages.size() == 1, "private response missing");
            String text = messages.getFirst().getString();
            check(text.contains("(1.35)") && !text.contains("\n") && !text.contains("Wild") && !text.contains("×") && !text.contains("#3"), "wrong single-line output: " + text);
            var tooltip = messages.getFirst().getStyle().getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);
            check(tooltip != null && tooltip.getString().contains("1.35") && tooltip.getString().contains("Min") && tooltip.getString().contains("Max"), "range hover missing");
            String[] rows = tooltip.getString().split("\n");
            check(dev.cudzer.cobblemonsizevariation.command.SizeRangeTooltip.width(rows[0]) == 160
                && dev.cudzer.cobblemonsizevariation.command.SizeRangeTooltip.width(rows[1]) == 160, "tooltip pixel alignment incorrect");
            check(messages.getFirst().getStyle().getColor() != null, "missing colored output");
            check(before.equals(pokemon.saveToNBT(server.registryAccess(), new net.minecraft.nbt.CompoundTag())), "inspection changed Pokemon data");
            check(dispatcher.execute("checkpokemonsize 1", source) == 0, "empty slot accepted");
            for (String invalid : java.util.List.of("checkpokemonsize 0", "checkpokemonsize 7", "checkpokemonsize 3 otherPlayer", "pokesizer self Slot3 2", "pokemonsize reload")) {
                boolean rejected = false;
                try { dispatcher.execute(invalid, source); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { rejected = true; }
                check(rejected, "invalid/privileged command accepted: " + invalid);
            }
            pokemon.setScaleModifier(0.505f);
            check(dispatcher.execute("checkpokemonsize 3", source) == 1, "category gap crashed command");
            check(messages.getLast().getString().contains("Unclassified"), "missing category-gap fallback");
            SpeciesOverride.set(true);
            dispatcher.execute("checkpokemonsize 3", source);
            check(messages.getLast().getStyle().getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT).getString().contains("0.8"), "hover ignored species override");
            SpeciesOverride.set(false);
            checkMessageReload(server, source);
            System.out.println("CSV PUBLIC COMMAND: PASS (permission 0, slot 3, colored private output, range, read-only NBT, invalid slots, empty slot, editing denied, category gap)");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new RuntimeException(error); }
        finally { party.remove(pokemon); }
    }

    private static void checkMessageReload(MinecraftServer server, net.minecraft.commands.CommandSourceStack source) {
        Path path = dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation.platform.getConfigDirectory()
            .resolve("cobblemonsizevariation/messages.json");
        var dispatcher = server.getCommands().getDispatcher();
        String original = null;
        try {
            original = Files.readString(path);
            var json = com.google.gson.JsonParser.parseString(original).getAsJsonObject();
            var lines = new com.google.gson.JsonArray(); lines.add("Custom {pokemon}: {scale}×"); json.add("lines", lines);
            json.getAsJsonObject("colors").addProperty("text", "#123456");
            Files.writeString(path, json.toString());
            check(dispatcher.execute("pokemonsize reload", source.withPermission(2)) == 1, "operator reload failed");
            var rendered = dev.cudzer.cobblemonsizevariation.command.CheckPokemonSizeCommand.describe(create(), 1);
            check(rendered.getString().startsWith("Custom ") && !rendered.getString().contains("\n"), "new layout not applied");
            check(rendered.getStyle().getColor().getValue() == 0x123456, "new color not applied");
            for (String invalid : java.util.List.of("{bad json", json.toString().replace("#123456", "not-a-color"), json.toString().replace("{scale}", "{typo}"))) {
                Files.writeString(path, invalid);
                check(dispatcher.execute("pokemonsize reload", source.withPermission(2)) == 0, "invalid configuration accepted");
                check(dev.cudzer.cobblemonsizevariation.command.CheckPokemonSizeCommand.describe(create(), 1).getString().startsWith("Custom "), "invalid reload replaced working layout");
            }
            System.out.println("CSV MESSAGE RELOAD: PASS (operator reload, custom text/color, malformed JSON/color/placeholder rollback)");
        } catch (Exception error) { throw new RuntimeException(error); }
        finally {
            if (original != null) try {
                Files.writeString(path, original);
                dev.cudzer.cobblemonsizevariation.config.SizeMessages.reload();
            } catch (Exception error) { throw new RuntimeException(error); }
        }
    }

    private static void checkBreeding(MinecraftServer server) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cobbreeding")) return;
        try {
            Class<?> utilities = Class.forName("ludichat.cobbreeding.BreedingUtilities");
            var choose = utilities.getMethod("chooseEgg", java.util.Collection.class);
            Pokemon first = create(), second = create(); first.setScaleModifier(1.6f); second.setScaleModifier(1.8f);
            var pairs = java.util.List.of(new kotlin.Pair<>(first, second));
            var eggs = java.util.List.of(java.util.Map.entry(first.getForm(), pairs));
            java.util.Set<Float> sizes = new java.util.HashSet<>();
            PokemonProperties properties = null;
            for (int i = 0; i < 40; i++) {
                properties = (PokemonProperties)choose.invoke(null, eggs);
                check(properties.getScaleModifier() != null, "parent mixin did not store an egg size");
                float size = properties.getScaleModifier(); sizes.add(size);
                check(size >= 1.325f - 0.00001f && size <= 1.775f + 0.00001f, "parent influence range incorrect: " + size);
            }
            check(sizes.size() > 1, "siblings did not vary");
            SpeciesOverride.set(true);
            check(((PokemonProperties)choose.invoke(null, eggs)).getScaleModifier() == 0.8f, "offspring ignored species bounds");
            SpeciesOverride.set(false);
            Class<?> eggUtilities = Class.forName("ludichat.cobbreeding.EggUtilities");
            String encrypted = (String)eggUtilities.getMethod("encrypt", PokemonProperties.class).invoke(null, properties);
            var decoded = (PokemonProperties)eggUtilities.getMethod("decrypt", String.class).invoke(null, encrypted);
            check(properties.getScaleModifier().equals(decoded.getScaleModifier()), "encrypted egg lost scale");
            var eggItem = (net.minecraft.world.item.ItemStack)eggUtilities.getMethod("getEggFromPokemonProperties", PokemonProperties.class, Integer.class).invoke(null, properties, 600);
            var extracted = (PokemonProperties)eggUtilities.getMethod("extractProperties", net.minecraft.world.item.ItemStack.class).invoke(null, eggItem);
            check(properties.getScaleModifier().equals(extracted.getScaleModifier()), "egg item lost scale");
            var player = new net.minecraft.server.level.ServerPlayer(server, server.overworld(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hatch-check"), net.minecraft.server.level.ClientInformation.createDefault());
            var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
            var hatch = eggItem.getItem().getClass().getDeclaredMethod("hatchEgg", net.minecraft.world.entity.player.Player.class, PokemonProperties.class);
            hatch.setAccessible(true);
            hatch.invoke(eggItem.getItem(), player, extracted);
            check(party.get(0) != null, "egg did not hatch");
            Pokemon child = party.get(0);
            check(child.getScaleModifier() == properties.getScaleModifier(), "hatch rerolled inherited size");
            check(child.getPersistentData().getBoolean(ServerSizeService.ASSIGNED_KEY), "hatched size not marked assigned");
            party.remove(child);
            // Explicit 1.0 must also survive; comparing the Pokemon's value to 1 would lose this case.
            extracted.setScaleModifier(1f); hatch.invoke(eggItem.getItem(), player, extracted);
            check(party.get(0).getScaleModifier() == 1f, "exact-normal egg size was rerolled");
            party.remove(party.get(0));
            ModConfig.breedingSizeInheritance = false;
            check(((PokemonProperties)choose.invoke(null, eggs)).getScaleModifier() == null, "disabled inheritance still assigned");
            ModConfig.breedingSizeInheritance = true;
            System.out.println("CSV BREEDING: PASS (real parent selection, sibling variation, encryption, egg item, hatch preservation including 1.0, assigned marker, config disable)");
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private static class SpeciesOverride extends dev.cudzer.cobblemonsizevariation.data.CustomSizeDataManager {
        static void set(boolean enabled) {
            if (enabled) speciesSizeMap.put("bulbasaur", new dev.cudzer.cobblemonsizevariation.data.PokemonSize(java.util.List.of("bulbasaur"), 0.8f, 0.8f));
            else speciesSizeMap.remove("bulbasaur");
        }
    }

    private static Pokemon create() {
        return PokemonProperties.Companion.parse("bulbasaur", " ", "=").create();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void run(MinecraftServer server) {
        checkPublicCommand(server);
        checkBreeding(server);
        ModConfig.sizeModificationChance = 1;
        Pokemon wild = create();
        // Nullable player is intentional: ownerless fossils must not crash.
        CobblemonEvents.FOSSIL_REVIVED.post(new FossilRevivedEvent(wild, null));
        check(wild.getPersistentData().getBoolean(ServerSizeService.ASSIGNED_KEY), "spawn size assigned");
        float first = wild.getScaleModifier();
        check(first >= 0.2f && first <= 2, "spawn size within bounds");
        CobblemonEvents.FOSSIL_REVIVED.post(new FossilRevivedEvent(wild, null));
        check(first == wild.getScaleModifier(), "duplicate event rerolled size");

        ModConfig.sizeModificationChance = 0;
        Pokemon unchanged = create();
        CobblemonEvents.FOSSIL_REVIVED.post(new FossilRevivedEvent(unchanged, null));
        ModConfig.sizeModificationChance = 1;
        CobblemonEvents.FOSSIL_REVIVED.post(new FossilRevivedEvent(unchanged, null));
        check(unchanged.getScaleModifier() == 1, "failed chance must not be retried");
        Pokemon boss = create();
        boss.setScaleModifier(3);
        CobblemonEvents.FOSSIL_REVIVED.post(new FossilRevivedEvent(boss, null));
        check(boss.getScaleModifier() == 3, "external boss size was overwritten");

        var registry = server.registryAccess();
        Pokemon reloaded = Pokemon.Companion.loadFromNBT(registry, wild.saveToNBT(registry, new net.minecraft.nbt.CompoundTag()));
        check(reloaded.getScaleModifier() == first, "scale did not persist");
        check(reloaded.getPersistentData().getBoolean(ServerSizeService.ASSIGNED_KEY), "assignment marker did not persist");

        var party = new RecordingParty();
        party.set(new PartyPosition(0), wild);
        int before = party.changes;
        ServerSizeService.setSize(wild, 1.75f);
        check(party.changes > before, "store was not marked dirty");
        check(party.lastPacket instanceof SetPartyPokemonPacket, "expected native party packet");
        var packet = (SetPartyPokemonPacket) party.lastPacket;
        check(packet.getId().getNamespace().equals("cobblemon"), "custom addon packet leaked");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registry);
        try {
            packet.encode(buffer);
            var decoded = SetPartyPokemonPacket.Companion.decode(buffer);
            check(decoded.getPokemon().invoke(registry).getScaleModifier() == 1.75f, "native party packet lost scale");
        } finally { buffer.release(); }

        var entity = new PokemonEntity(server.overworld(), wild, com.cobblemon.mod.common.CobblemonEntities.POKEMON);
        check(server.overworld().addFreshEntity(entity), "test entity failed to spawn");
        wild.setState(new SentOutState(entity));
        ServerSizeService.setSize(wild, 0.5f);
        check(entity.getEntityData().get(PokemonEntity.getSCALE_MODIFIER()) == 0.5f, "native live metadata did not update");
        float smallWidth = entity.getBbWidth();
        ServerSizeService.setSize(wild, 2.0f);
        check(entity.getEntityData().get(PokemonEntity.getSCALE_MODIFIER()) == 2.0f, "second live resize did not update");
        check(entity.getBbWidth() > smallWidth, "live hitbox did not resize");
        check(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getNamespace().equals("cobblemonsizevariation")),
                "addon items must not be registered");
    }

    private static final class RecordingParty extends PartyStore {
        int changes;
        NetworkPacket<?> lastPacket;
        RecordingParty() { super(UUID.randomUUID()); }
        @Override public void onPokemonChanged(Pokemon pokemon) { changes++; super.onPokemonChanged(pokemon); }
        @Override public void sendPacketToObservers(NetworkPacket<?> packet) { lastPacket = packet; }
    }
}
