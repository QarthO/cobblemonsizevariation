package dev.cudzer.cobblemonsizevariation.sizing;

/** Policy separated from the game API so spawn decisions can be regression tested. */
public final class SizeAssignment {
    private SizeAssignment() {}

    public static boolean shouldRandomize(boolean assigned, float existingScale, float chance, float roll) {
        // Preserve externally assigned/boss sizes, and never reroll an already processed Pokemon.
        return !assigned && Float.compare(existingScale, 1.0f) == 0 && roll < chance;
    }

    public static void validateRange(float min, float max) {
        if (!Float.isFinite(min) || !Float.isFinite(max) || min <= 0 || max < min) {
            throw new IllegalArgumentException("Size bounds must be finite, positive, and min <= max");
        }
    }
}
