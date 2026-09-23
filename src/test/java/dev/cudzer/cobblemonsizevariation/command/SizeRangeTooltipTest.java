package dev.cudzer.cobblemonsizevariation.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SizeRangeTooltipTest {
    @Test void measuredFontWidths() {
        assertEquals(14, SizeRangeTooltip.width("Min"));
        assertEquals(18, SizeRangeTooltip.width("Max"));
        assertEquals(20, SizeRangeTooltip.width("1.35"));
        assertNotEquals(SizeRangeTooltip.width("Min"), SizeRangeTooltip.width("Max"));
    }
    @Test void selectionMovesAndNeverOverlaps() {
        int previous = -1;
        for (int i=1; i<180; i++) {
            float value = 0.2f + i/100f;
            int dots = SizeRangeTooltip.leftDots(value, 0.2f, 2, "0.2", "1.35", "2", 160);
            assertTrue(dots >= previous); assertTrue(dots >= 0);
            int right = (160-14-20-6-16)/2-dots;
            assertTrue(right >= 0);
            assertEquals(160, 14+4+2*dots+4+20+4+2*right+4+6);
            previous = dots;
        }
        assertTrue(SizeRangeTooltip.leftDots(1.35f,0.2f,2,"0.2","1.35","2",160) >
            SizeRangeTooltip.leftDots(0.5f,0.2f,2,"0.2","0.5","2",160));
    }
}
