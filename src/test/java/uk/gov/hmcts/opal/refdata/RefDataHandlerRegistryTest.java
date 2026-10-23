package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class RefDataHandlerRegistryTest {

    @Test
    void shouldFindHandlerByRefDataType() {
        // Given
        RefDataUpdateHandler<?, ?> handler = mock(RefDataUpdateHandler.class);
        when(handler.refDataType()).thenReturn("Prison");
        var registry = new RefDataHandlerRegistry(List.of(handler));

        // When
        var prisonHandler = registry.find("Prison");
        var courtHandler = registry.find("Court");
        var blankHandler = registry.find(" ");
        var nullHandler = registry.find(null);

        // Then
        assertThat(prisonHandler).containsSame(handler);
        assertThat(courtHandler).isEmpty();
        assertThat(blankHandler).isEmpty();
        assertThat(nullHandler).isEmpty();
    }
}
