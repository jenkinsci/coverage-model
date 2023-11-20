package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class TestCaseTest {
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(TestCase.class).verify();
    }
}
