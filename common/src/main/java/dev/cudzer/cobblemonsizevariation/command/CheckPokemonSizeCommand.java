package dev.cudzer.cobblemonsizevariation.command;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation;
import dev.cudzer.cobblemonsizevariation.config.ModConfig;
import dev.cudzer.cobblemonsizevariation.data.CustomSizeDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import java.util.Locale;
import java.util.Map;
import dev.cudzer.cobblemonsizevariation.config.SizeMessages;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Public, self-only inspection. Never assigns, marks or synchronizes a size. */
public final class CheckPokemonSizeCommand {
    private CheckPokemonSizeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("pokemonsize").requires(source -> source.hasPermission(2))
            .then(literal("reload").executes(context -> {
                try {
                    SizeMessages.reload();
                    context.getSource().sendSuccess(SizeMessages::reloadSuccess, false);
                    return 1;
                } catch (Exception error) {
                    context.getSource().sendFailure(SizeMessages.reloadFailure(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
                    return 0;
                }
            })));
        dispatcher.register(literal("checkpokemonsize")
            .then(argument("slot", IntegerArgumentType.integer(1, 6)).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                int slot = IntegerArgumentType.getInteger(context, "slot");
                Pokemon pokemon = PlayerExtensionsKt.party(player).get(slot - 1);
                if (pokemon == null) {
                    context.getSource().sendFailure(SizeMessages.emptySlot(slot));
                    return 0;
                }
                context.getSource().sendSuccess(() -> describe(pokemon, slot), false);
                return 1;
            })));
    }

    public static Component describe(Pokemon pokemon, int slot) {
        var sizer = CobblemonSizeVariation.SIZER;
        float scale = pokemon.getScaleModifier();
        var category = sizer.getSizeInformation(scale);
        var custom = CustomSizeDataManager.getCustomSizeFile(pokemon.getSpecies());
        float min = custom == null ? sizer.getMinSizeModifier() : custom.getMinSize();
        float max = custom == null ? sizer.getMaxSizeModifier() : custom.getMaxSize();
        TextColor categoryColor = category == null ? TextColor.fromLegacyFormat(ChatFormatting.WHITE)
            : TextColor.parseColor(category.color()).result().orElse(TextColor.fromLegacyFormat(ChatFormatting.WHITE));
        return SizeMessages.render(Map.of(
            "pokemon", pokemon.getDisplayName(false), "slot", Component.literal(String.valueOf(slot)),
            "category", Component.literal(category == null ? SizeMessages.unclassified() : category.name()),
            "scale", number(scale, 3), "percent", number(scale * 100, 1),
            "min", number(min, 3), "max", number(max, 3), "chance", number(ModConfig.sizeModificationChance * 100, 1)), categoryColor);
    }

    private static Component number(float value, int precision) {
        return Component.literal(String.format(Locale.ROOT, "%." + precision + "f", value).replaceFirst("\\.?0+$", ""));
    }
}
