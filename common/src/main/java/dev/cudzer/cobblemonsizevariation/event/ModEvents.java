package dev.cudzer.cobblemonsizevariation.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent;
import com.cobblemon.mod.common.api.events.pokemon.FossilRevivedEvent;
import com.cobblemon.mod.common.api.events.pokemon.ShoulderMountEvent;
import com.cobblemon.mod.common.api.events.pokemon.RidePokemonEvent;
import com.cobblemon.mod.common.api.events.starter.StarterChosenEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation;
import dev.cudzer.cobblemonsizevariation.config.ModConfig;
import dev.cudzer.cobblemonsizevariation.data.CustomSizeDataManager;
import dev.cudzer.cobblemonsizevariation.sizing.ServerSizeService;
import dev.cudzer.cobblemonsizevariation.sizing.SizeAssignment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Random;

public class ModEvents {
    private static final Random random = new Random();

    public static void registerEvents(){
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, ModEvents::onCobblemonSpawn);
        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.subscribe(Priority.NORMAL, ModEvents::onSnackSpawn);
        CobblemonEvents.SHOULDER_MOUNT.subscribe(Priority.NORMAL, ModEvents::onShoulderMount);
        CobblemonEvents.STARTER_CHOSEN.subscribe(Priority.NORMAL, ModEvents::onStarterChosen);
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.NORMAL, ModEvents::onFossilRevived);
        CobblemonEvents.RIDE_EVENT_PRE.subscribe(Priority.NORMAL, ModEvents::onAttemptRide);
    }

    private static void onCobblemonSpawn(SpawnEvent<PokemonEntity> event){
        resizer(event.getEntity().getPokemon());
    }

    private static void onSnackSpawn(PokeSnackSpawnPokemonEvent.Post event){
        resizer(event.getPokemonEntity().getPokemon());
    }

    private static void onShoulderMount(ShoulderMountEvent event){
        Pokemon p = event.getPokemon();
        if(p.getScaleModifier() > ModConfig.preventShoulderMountSize){
            MutableComponent tooHeavyMessage = Component.literal("This Cobblemon is too chonky to sit on your shoulder!");
            event.getPlayer().sendSystemMessage(tooHeavyMessage);
            event.cancel();
        }
    }

    private static void onAttemptRide(RidePokemonEvent.Pre event){
        Pokemon p = event.getPokemon().getPokemon();
        MutableComponent message = Component.empty();
        boolean ridable = true;
        if(p.getScaleModifier() > ModConfig.preventRidingMaxSize){
            message = Component.literal("This Cobblemon is too big to ride!");
            ridable = false;
        }
        else if(p.getScaleModifier() < ModConfig.preventRidingMinSize){
            message = Component.literal("This Cobblemon is too small to ride!");
            ridable = false;
        }

        if(!ridable){
            event.getPlayer().sendSystemMessage(message);
            event.cancel();
        }
    }

    private static void onStarterChosen(StarterChosenEvent event){
        resizer(event.getPokemon());
    }

    private static void onFossilRevived(FossilRevivedEvent event){
        resizer(event.getPokemon());
    }

    private static void resizer(Pokemon pokemon) {
        boolean assigned = pokemon.getPersistentData().getBoolean(ServerSizeService.ASSIGNED_KEY);
        if (assigned) {
            return;
        }
        if (SizeAssignment.shouldRandomize(false, pokemon.getScaleModifier(),
                ModConfig.sizeModificationChance, random.nextFloat())) {
            var customSize = CustomSizeDataManager.getCustomSizeFile(pokemon.getSpecies());
            float size = customSize == null ? CobblemonSizeVariation.SIZER.getSize()
                    : CobblemonSizeVariation.SIZER.getSize(customSize.getMinSize(), customSize.getMaxSize());
            ServerSizeService.setSize(pokemon, size);
        } else {
            // Record a failed chance roll too; later spawn callbacks must not retry it.
            ServerSizeService.markAssigned(pokemon);
        }
    }
}
