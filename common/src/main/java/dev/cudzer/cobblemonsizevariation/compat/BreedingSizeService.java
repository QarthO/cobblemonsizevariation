package dev.cudzer.cobblemonsizevariation.compat;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation;
import dev.cudzer.cobblemonsizevariation.config.ModConfig;
import dev.cudzer.cobblemonsizevariation.data.CustomSizeDataManager;
import dev.cudzer.cobblemonsizevariation.sizing.SizeInheritance;

public final class BreedingSizeService {
    private BreedingSizeService() {}

    public static void assign(Pokemon first, Pokemon second, PokemonProperties egg) {
        if (!ModConfig.breedingSizeInheritance || egg.getScaleModifier() != null || !compatEnabled()) return;
        var species = egg.getSpecies() == null ? null : PokemonSpecies.getByName(egg.getSpecies());
        // Random-species eggs have no final species yet; preserve Cobbreeding's existing hatch roll.
        if (species == null) return;
        var custom = CustomSizeDataManager.getCustomSizeFile(species);
        var sizer = CobblemonSizeVariation.SIZER;
        float min = custom == null ? sizer.getMinSizeModifier() : custom.getMinSize();
        float max = custom == null ? sizer.getMaxSizeModifier() : custom.getMaxSize();
        egg.setScaleModifier(SizeInheritance.combine(first.getScaleModifier(), second.getScaleModifier(),
            sizer.getSize(min, max), min, max, ModConfig.breedingParentInfluence));
    }

    private static boolean compatEnabled() {
        // No hard dependency: these calls only run inside the version-gated Cobbreeding mixin.
        try {
            var type = Class.forName("ludichat.cobbreeding.Cobbreeding");
            Object config = type.getMethod("getConfig").invoke(type.getField("INSTANCE").get(null));
            return (boolean)config.getClass().getMethod("getCobblemonSizeVariationsCompatEnabled").invoke(config);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Supported Cobbreeding configuration API changed", error);
        }
    }
}
