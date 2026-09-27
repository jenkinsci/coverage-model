package edu.hm.hafner.coverage;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TestCaseTest {
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(TestCase.class).verify();
    }
}
