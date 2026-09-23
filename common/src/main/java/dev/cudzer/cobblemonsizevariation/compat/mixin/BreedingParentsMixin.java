package dev.cudzer.cobblemonsizevariation.compat.mixin;

import dev.cudzer.cobblemonsizevariation.compat.BreedingSizeService;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ludichat.cobbreeding.BreedingUtilities", remap = false)
public abstract class BreedingParentsMixin {
    @Inject(method = "calcFeatures", at = @At("TAIL"))
    private void csv$inherit(Pair<Pokemon, Pokemon> parents, PokemonProperties properties, CallbackInfo ci) {
        BreedingSizeService.assign(parents.getFirst(), parents.getSecond(), properties);
    }
}
