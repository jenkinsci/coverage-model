package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;

public class ParseUtil {

    protected static Value createValue(final String currentType, final int covered, final int missed) {
        if (currentType.equals("COMPLEXITY")) {
            return new CyclomaticComplexity(covered + missed);
        }
        else {
            var builder = new Coverage.CoverageBuilder();
            return builder.withMetric(Metric.valueOf(currentType))
                    .withCovered(covered)
                    .withMissed(missed).build();
        }
    }
}
