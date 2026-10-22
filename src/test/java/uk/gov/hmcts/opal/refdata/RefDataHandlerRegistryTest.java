package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class RefDataHandlerRegistryTest {

    @Test
    void shouldFindHandlerByRefDataType() {
        RefDataUpdateHandler<?, ?> handler = mock(RefDataUpdateHandler.class);
        when(handler.refDataType()).thenReturn("Prison");
        var registry = new RefDataHandlerRegistry(List.of(handler));

        assertThat(registry.find("Prison")).containsSame(handler);
        assertThat(registry.find("Court")).isEmpty();
        assertThat(registry.find(" ")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }
}
