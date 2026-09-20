package dev.cudzer.cobblemonsizevariation.sizing;

import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.client.storage.pc.SetPCPokemonPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;

/** Changes size using only protocols already understood by stock Cobblemon 1.7.3. */
public final class ServerSizeService {
    public static final String ASSIGNED_KEY = "cobblemonsizevariation:assigned";

    private ServerSizeService() {}

    public static void setSize(Pokemon pokemon, float size) {
        if (!Float.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException("Pokemon size must be finite and positive");
        }
        pokemon.setScaleModifier(size);
        markAssigned(pokemon);
        var entity = pokemon.getEntity();
        if (entity != null && entity.getDelegate() instanceof PokemonServerDelegate delegate) {
            // Cobblemon's normal tracked-data path also respects transformations/disguises.
            // It updates every tracking client and refreshes the hitbox on both sides.
            delegate.updateTrackedValues();
        }
        var coordinates = pokemon.getStoreCoordinates().get();
        if (coordinates == null) {
            return; // New wild Pokemon are synchronized by their normal spawn packet.
        }
        var store = coordinates.getStore();
        if (coordinates.getPosition() instanceof PartyPosition position) {
            store.sendPacketToObservers(new SetPartyPokemonPacket(store.getUuid(), position, registry -> pokemon));
        } else if (coordinates.getPosition() instanceof PCPosition position) {
            store.sendPacketToObservers(new SetPCPokemonPacket(store.getUuid(), position, registry -> pokemon));
        }
    }

    public static void markAssigned(Pokemon pokemon) {
        pokemon.getPersistentData().putBoolean(ASSIGNED_KEY, true);
        // scaleModifier's setter does not mark a party/PC store dirty.
        pokemon.onChange(null);
    }
}
