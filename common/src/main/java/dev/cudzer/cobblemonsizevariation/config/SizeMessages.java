package dev.cudzer.cobblemonsizevariation.config;

import com.google.gson.GsonBuilder;
import dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** Reloads presentation only; invalid edits leave the last working configuration active. */
public final class SizeMessages {
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]+)}");
    private static final Set<String> VALUES = Set.of("pokemon", "slot", "category", "scale", "percent", "min", "max", "chance");
    private static final Map<String, String> DEFAULT_COLORS = Map.ofEntries(
        Map.entry("text", "#AAAAAA"), Map.entry("pokemon", "#FFFF55"), Map.entry("slot", "#AAAAAA"),
        Map.entry("category", "auto"), Map.entry("scale", "#55FFFF"), Map.entry("percent", "#55FFFF"),
        Map.entry("min", "#55FF55"), Map.entry("max", "#55FF55"), Map.entry("chance", "#FFFFFF"));
    private static final Format DEFAULT = new Format(List.of(
        "{pokemon} · #{slot} · {category}",
        "{scale}× ({percent}% normal) · Wild {min}–{max}×",
        "Wild roll {chance}% · Otherwise 1×"), DEFAULT_COLORS,
        "No Pokémon in slot {slot}.", "Pokémon size text reloaded.",
        "Could not reload size text: {error}. Previous text retained.", "Unclassified");
    private static Format active = DEFAULT;
    private static Path file;

    public record Format(List<String> lines, Map<String, String> colors, String emptySlot,
                         String reloadSuccess, String reloadFailure, String unclassified) {}
    private SizeMessages() {}

    public static void init(Path configDirectory) {
        file = configDirectory.resolve("cobblemonsizevariation/messages.json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(DEFAULT) + "\n");
            }
            reload();
        } catch (Exception error) {
            CobblemonSizeVariation.LOGGER.warn("Could not load size messages; using defaults", error);
        }
    }

    public static void reload() throws IOException {
        Format candidate;
        try { candidate = new GsonBuilder().create().fromJson(Files.readString(file), Format.class); }
        catch (com.google.gson.JsonParseException error) { throw new IllegalArgumentException("invalid JSON", error); }
        if (candidate == null || candidate.lines() == null || candidate.lines().isEmpty() || candidate.colors() == null)
            throw new IllegalArgumentException("lines and colors are required");
        for (String line : candidate.lines()) validate(line, VALUES);
        validate(candidate.emptySlot(), Set.of("slot"));
        validate(candidate.reloadSuccess(), Set.of());
        validate(candidate.reloadFailure(), Set.of("error"));
        validate(candidate.unclassified(), Set.of());
        for (String key : DEFAULT_COLORS.keySet()) {
            String color = candidate.colors().get(key);
            if (color == null || !(color.matches("#[0-9a-fA-F]{6}") || key.equals("category") && color.equals("auto")))
                throw new IllegalArgumentException("invalid color for " + key + " (use #RRGGBB)");
        }
        active = new Format(List.copyOf(candidate.lines()), Map.copyOf(candidate.colors()), candidate.emptySlot(),
            candidate.reloadSuccess(), candidate.reloadFailure(), candidate.unclassified());
    }

    private static void validate(String template, Set<String> allowed) {
        if (template == null) throw new IllegalArgumentException("missing message field");
        var matcher = TOKEN.matcher(template);
        while (matcher.find()) if (!allowed.contains(matcher.group(1)))
            throw new IllegalArgumentException("unknown placeholder {" + matcher.group(1) + "}");
    }

    public static String unclassified() { return active.unclassified(); }
    public static Component emptySlot(int slot) { return plain(active.emptySlot(), Map.of("slot", String.valueOf(slot))); }
    public static Component reloadSuccess() { return Component.literal(active.reloadSuccess()); }
    public static Component reloadFailure(String error) { return plain(active.reloadFailure(), Map.of("error", error)); }

    private static Component plain(String template, Map<String, String> values) {
        var output = Component.empty(); var matcher = TOKEN.matcher(template); int end = 0;
        while (matcher.find()) {
            output.append(template.substring(end, matcher.start())).append(values.get(matcher.group(1)));
            end = matcher.end();
        }
        return output.append(template.substring(end));
    }

    public static Component render(Map<String, Component> values, TextColor categoryColor) {
        var format = active;
        MutableComponent output = Component.empty().withStyle(s -> s.withColor(color(format.colors().get("text"))));
        for (int i = 0; i < format.lines().size(); i++) {
            if (i > 0) output.append("\n");
            String line = format.lines().get(i); var matcher = TOKEN.matcher(line); int end = 0;
            while (matcher.find()) {
                output.append(line.substring(end, matcher.start()));
                String key = matcher.group(1), configured = format.colors().get(key);
                TextColor tint = configured.equals("auto") ? categoryColor : color(configured);
                output.append(values.get(key).copy().withStyle(s -> s.withColor(tint)));
                end = matcher.end();
            }
            output.append(line.substring(end));
        }
        return output;
    }
    private static TextColor color(String hex) { return TextColor.fromRgb(Integer.parseInt(hex.substring(1), 16)); }
}
