package edu.hm.hafner.coverage;

class LinesOfCodeTest extends IntegerValueTest {
    @Override
    IntegerValue createValue(final int value) {
        return new LinesOfCode(value);
    }
}
