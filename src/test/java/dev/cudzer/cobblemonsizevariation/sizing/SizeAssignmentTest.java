package dev.cudzer.cobblemonsizevariation.sizing;

import java.util.List;
import dev.cudzer.cobblemonsizevariation.config.ModConfig;
import dev.cudzer.cobblemonsizevariation.sizing.algorithms.BasicSizer;
import dev.cudzer.cobblemonsizevariation.sizing.algorithms.GenIXSizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SizeAssignmentTest {
    @Test void alreadyAssignedPokemonIsNeverRerolled() {
        assertFalse(SizeAssignment.shouldRandomize(true, 1, 1, 0));
    }
    @Test void externalSizesArePreserved() {
        assertFalse(SizeAssignment.shouldRandomize(false, 2, 1, 0));
    }
    @Test void chanceZeroNeverAssigns() {
        assertFalse(SizeAssignment.shouldRandomize(false, 1, 0, 0));
    }
    @Test void chanceOneAlwaysAssigns() {
        assertTrue(SizeAssignment.shouldRandomize(false, 1, 1, 0.999f));
    }
    @Test void chanceBoundaryIsExclusive() {
        assertFalse(SizeAssignment.shouldRandomize(false, 1, 0.5f, 0.5f));
        assertTrue(SizeAssignment.shouldRandomize(false, 1, 0.5f, 0.49f));
    }
    @Test void invalidBoundsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SizeAssignment.validateRange(0, 2));
        assertThrows(IllegalArgumentException.class, () -> SizeAssignment.validateRange(2, 1));
        assertThrows(IllegalArgumentException.class, () -> SizeAssignment.validateRange(Float.NaN, 2));
        assertThrows(IllegalArgumentException.class, () -> SizeAssignment.validateRange(1, Float.POSITIVE_INFINITY));
    }
    @Test void biasedAndUniformSamplesStayWithinConfiguredBounds() {
        var definition = new SizeDefinition("test", "1.5", "2.0", List.of());
        try {
            for (boolean biased : new boolean[] {false, true}) {
                ModConfig.biasSizeTowardAverage = biased;
                for (var sizer : List.of(new BasicSizer(definition), new GenIXSizer(definition))) {
                    for (int n = 0; n < 10000; n++) {
                        float size = sizer.getSize();
                        assertTrue(size >= 1.5f && size <= 2, "sample outside configured bounds: " + size);
                        assertEquals(1.25f, sizer.getSize(1.25f, 1.25f));
                    }
                }
            }
        } finally {
            ModConfig.biasSizeTowardAverage = false;
        }
    }
}
