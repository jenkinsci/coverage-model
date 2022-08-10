package edu.hm.hafner.mutation;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MutationResultTest {

    @Test
    void shouldReturnExactAmountWithAmountSetInCtor() {
        MutationResult result = new MutationResult(3, 8);

        assertThat(result)
                .hasKilled(3)
                .hasSurvived(8)
                .hasToString("MutationResult{killed=3, survived=8}");
    }

    @Test
    void shouldAddAnotherResult() {
        MutationResult result = new MutationResult();
        result = result.add(new MutationResult(2, 3));

        assertThat(result)
                .hasKilled(2)
                .hasSurvived(3);

        result = result.add(new MutationResult(9, 23));

        assertThat(result)
                .hasKilled(11)
                .hasSurvived(26);
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(MutationResult.class).withNonnullFields("survived", "killed").verify();
    }
}