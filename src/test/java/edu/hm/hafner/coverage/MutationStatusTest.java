package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MutationStatusTest {

    @Test
    void shouldReturnCorrectIsDetected() {
        // Use method-calls instead of custom-assertions to invoke correct methods
        assertThat(MutationStatus.KILLED.isDetected()).isTrue();
        assertThat(MutationStatus.SURVIVED.isDetected()).isFalse();
    }

    @Test
    void shouldReturnCorrectIsNotDetected() {
        // Use method-calls instead of custom-assertions to invoke correct methods
        assertThat(MutationStatus.KILLED.isNotDetected()).isFalse();
        assertThat(MutationStatus.SURVIVED.isNotDetected()).isTrue();
        assertThat(MutationStatus.NO_COVERAGE.isNotDetected()).isTrue();
    }
}