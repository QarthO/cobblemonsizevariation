package dev.cudzer.cobblemonsizevariation.compat.mixin;

import dev.cudzer.cobblemonsizevariation.compat.BreedingSizeService;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.cudzer.cobblemonsizevariation.sizing.ServerSizeService;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "ludichat.cobbreeding.PokemonEgg", remap = false)
public abstract class BreedingHatchMixin {
    @Redirect(method = "hatchEgg", at = @At(value = "INVOKE", target = "Lkotlin/jvm/functions/Function1;invoke(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object csv$preserveEggScale(Function1<Object, Object> callback, Object argument,
                                       Player player, PokemonProperties properties) {
        if (properties.getScaleModifier() != null && argument instanceof Pokemon pokemon) {
            // The egg's native scale_modifier survives item serialization and encryption.
            // Preserve explicit sizes even if inheritance is switched off after laying the egg.
            ServerSizeService.setSize(pokemon, properties.getScaleModifier());
            return Unit.INSTANCE;
        }
        return callback.invoke(argument); // Old/admin eggs without a stored size keep the old random roll.
    }
}
