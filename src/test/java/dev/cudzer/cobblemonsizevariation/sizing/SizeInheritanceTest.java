package dev.cudzer.cobblemonsizevariation.sizing;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SizeInheritanceTest {
    @Test void influenceEndpointsAndSymmetry() {
        assertEquals(0.7f, SizeInheritance.combine(1, 2, 0.7f, 0.2f, 2, 0), 0.00001);
        assertEquals(1.5f, SizeInheritance.combine(1, 2, 0.7f, 0.2f, 2, 1), 0.00001);
        assertEquals(SizeInheritance.combine(1, 2, 0.7f, 0.2f, 2, 0.75f), SizeInheritance.combine(2, 1, 0.7f, 0.2f, 2, 0.75f));
    }
    @Test void parentsShiftDistributionButSiblingsVary() {
        Random random = new Random(17); double small = 0, large = 0;
        float min = Float.POSITIVE_INFINITY, max = 0;
        for (int i = 0; i < 10000; i++) {
            float draw = 0.2f + random.nextFloat() * 1.8f;
            float child = SizeInheritance.combine(1.6f, 1.8f, draw, 0.2f, 2, 0.75f);
            large += child; small += SizeInheritance.combine(0.4f, 0.6f, draw, 0.2f, 2, 0.75f);
            assertTrue(child >= 0.2 && child <= 2); min = Math.min(min, child); max = Math.max(max, child);
        }
        assertEquals(0.9, (large - small) / 10000, 0.00001);
        assertTrue(max - min > 0.4, "siblings need variation");
    }
    @Test void customBoundsAndExtremeParents() {
        assertEquals(1.25f, SizeInheritance.combine(100, 0.01f, 1.25f, 0.5f, 2, 1));
        assertEquals(0.8f, SizeInheritance.combine(1, 2, 0.8f, 0.8f, 0.8f, 0.75f));
        assertThrows(IllegalArgumentException.class, () -> SizeInheritance.combine(1, 1, 1, 0.2f, 2, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> SizeInheritance.combine(1, 1, 1, 0.2f, 2, 1.1f));
    }
}
