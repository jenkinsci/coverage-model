package edu.hm.hafner.coverage;

class TestCountTest extends IntegerValueTest {
    @Override
    IntegerValue createValue(final int value) {
        return new TestCount(value);
    }
}
