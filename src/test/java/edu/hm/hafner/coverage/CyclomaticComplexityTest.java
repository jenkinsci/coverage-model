package edu.hm.hafner.coverage;

class CyclomaticComplexityTest extends IntegerValueTest {
    @Override
    IntegerValue createValue(final int value) {
        return new CyclomaticComplexity(value);
    }
}
