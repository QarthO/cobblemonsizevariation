package dev.cudzer.cobblemonsizevariation.sizing;

/** A simple parent-influenced model, not a simulation of individual genes. */
public final class SizeInheritance {
    private SizeInheritance() {}

    public static float combine(float first, float second, float randomSize, float min, float max, float influence) {
        SizeAssignment.validateRange(min, max);
        if (!Float.isFinite(influence) || influence < 0 || influence > 1)
            throw new IllegalArgumentException("breedingParentInfluence must be between 0 and 1");
        if (!Float.isFinite(first) || !Float.isFinite(second) || !Float.isFinite(randomSize))
            throw new IllegalArgumentException("Inheritance sizes must be finite");
        // Clamp each parent separately so an admin-sized parent cannot dominate the other.
        double average = (clamp(first, min, max) + (double)clamp(second, min, max)) / 2;
        return clamp((float)(influence * average + (1 - influence) * clamp(randomSize, min, max)), min, max);
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
