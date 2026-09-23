package dev.cudzer.cobblemonsizevariation.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Pixel advances from Minecraft 1.21.1's default bitmap font, not character counts. */
public final class SizeRangeTooltip {
    private SizeRangeTooltip() {}
    public static int width(String value) {
        int width = 0;
        for (char c : value.toCharArray()) width += c == '.' ? 2 : c == ' ' ? 4 : c == 'i' ? 2 : 6;
        return width;
    }
    public static int leftDots(float value, float min, float max, String low, String current, String high, int pixels) {
        int start = (int)Math.round((width(low)/2.0 + Math.clamp((value-min)/(max-min), 0, 1)
            * (pixels-width(low)/2.0-width(high)/2.0) - width(current)/2.0) / 2) * 2;
        start = Math.clamp(start, width(low)+8, pixels-width(high)-width(current)-8);
        return (start-width(low)-8)/2;
    }
    public static Component create(float value, float min, float max, Map<String, Component> values,
                                   int pixels, TextColor text, TextColor selected, TextColor minimum, TextColor maximum) {
        String low=values.get("min").getString(), current=values.get("scale").getString(), high=values.get("max").getString();
        // Grow for unusually long custom scales. Header spacing remains a multiple of four pixels.
        pixels = Math.max(pixels, ((width(low)+width(current)+width(high)+32+3)/4)*4);
        var output = Component.empty().withStyle(s -> s.withFont(ResourceLocation.withDefaultNamespace("default"))
            .withBold(false).withItalic(false).withColor(text));
        output.append(Component.literal("Min").withStyle(s -> s.withColor(minimum)))
            .append(" ".repeat((pixels-width("Min")-width("Max"))/4))
            .append(Component.literal("Max").withStyle(s -> s.withColor(maximum))).append("\n");
        if (current.equals(low) || current.equals(high)) {
            output.append(Component.literal(low).withStyle(s -> s.withColor(current.equals(low) ? selected : minimum)))
                .append(" " + ".".repeat((pixels-width(low)-width(high)-8)/2) + " ")
                .append(Component.literal(high).withStyle(s -> s.withColor(current.equals(high) ? selected : maximum)));
        } else {
            int left = max==min ? (pixels-width(low)-width(high)-width(current)-16)/4
                : leftDots(value,min,max,low,current,high,pixels);
            int right = (pixels-width(low)-width(current)-width(high)-16)/2-left;
            output.append(Component.literal(low).withStyle(s -> s.withColor(minimum)))
                .append(" " + ".".repeat(left) + " ")
                .append(Component.literal(current).withStyle(s -> s.withColor(selected)))
                .append(" " + ".".repeat(right) + " ")
                .append(Component.literal(high).withStyle(s -> s.withColor(maximum)));
        }
        if (value<min || value>max) output.append("\nOutside configured range");
        return output;
    }
}
