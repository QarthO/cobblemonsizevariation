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

    private static Pokemon create() {
        return PokemonProperties.Companion.parse("bulbasaur", " ", "=").create();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void run(MinecraftServer server) {
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
