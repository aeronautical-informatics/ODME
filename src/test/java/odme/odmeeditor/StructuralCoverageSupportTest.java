package odme.odmeeditor;

import odme.odmeeditor.StructuralCoverageSupport.CoverageBin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static odme.odmeeditor.StructuralCoverageSupport.buildUniformBins;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralCoverageSupportTest {
    @ParameterizedTest
    @CsvSource({
            "0, 10, 1, 10",
            "0, 1, 0.1, 10",
            "0, 3, 0.3, 10",
            "-90, 90, 18, 10",
            "0, 0.7, 0.1, 7",
            "0, 0.30000000000000004, 0.1, 3",
            "5, 5, 1, 1",
            "0, 10000, 1, 10000"
    })
    void buildUniformBins_preservesCountAndEdges(double min, double max, double binSize, int expectedCount) {
        assertBinEdges(min, max, expectedCount, buildUniformBins(min, max, binSize));
    }

    @Test
    void buildUniformBins_keepsShorterLastBin() {
        List<CoverageBin> bins = buildUniformBins(0, 10, 3);

        assertBinEdges(0, 10, 4, bins);
        assertEquals(3.0, bins.get(0).upperInclusive());
        assertEquals(6.0, bins.get(1).upperInclusive());
        assertEquals(9.0, bins.get(2).upperInclusive());
    }

    @Test
    void buildUniformBins_rejectsMoreThanTenThousandBins() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildUniformBins(0, 10001, 1));

        assertTrue(exception.getMessage().contains("10000"));
    }

    private static void assertBinEdges(double min, double max, int expectedCount, List<CoverageBin> bins) {
        assertEquals(expectedCount, bins.size());
        assertEquals(min, bins.get(0).lowerInclusive());
        assertEquals(max, bins.get(bins.size() - 1).upperInclusive());
        for (int i = 1; i < bins.size(); i++) {
            assertEquals(bins.get(i - 1).upperInclusive(), bins.get(i).lowerInclusive(), "Edge " + i);
        }
    }
}
