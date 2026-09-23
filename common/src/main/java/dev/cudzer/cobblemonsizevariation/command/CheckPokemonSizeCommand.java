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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Public, self-only inspection. Never assigns, marks or synchronizes a size. */
public final class CheckPokemonSizeCommand {
    private CheckPokemonSizeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("checkpokemonsize")
            .then(argument("slot", IntegerArgumentType.integer(1, 6)).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                int slot = IntegerArgumentType.getInteger(context, "slot");
                Pokemon pokemon = PlayerExtensionsKt.party(player).get(slot - 1);
                if (pokemon == null) {
                    context.getSource().sendFailure(Component.literal("There is no Pokémon in party slot " + slot + "."));
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
        // Custom category definitions may contain gaps. Do not invent a category or fail inspection.
        MutableComponent label = Component.literal(category == null ? "Unclassified" : category.name());
        if (category != null) {
            TextColor color = TextColor.parseColor(category.color()).result().orElse(TextColor.fromLegacyFormat(ChatFormatting.WHITE));
            label.withStyle(style -> style.withColor(color));
        }
        return Component.literal("Pokémon size · Slot " + slot + " · ").withStyle(ChatFormatting.GOLD)
            .append(pokemon.getDisplayName(false).copy().withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\nSize: ").withStyle(ChatFormatting.GRAY)).append(label)
            .append(Component.literal(String.format(Locale.ROOT, " · %.3f× (%.1f%% of normal scale)", scale, scale * 100)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("\nCurrent wild roll range: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format(Locale.ROOT, "min %.3f× · max %.3f×", min, max)).withStyle(ChatFormatting.GREEN))
            .append(Component.literal(String.format(Locale.ROOT, "\nWild size roll chance: %.1f%% · Otherwise normal scale (1×).", ModConfig.sizeModificationChance * 100)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
