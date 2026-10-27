package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SuppressWarnings("unchecked")
class RefDataRefDataMessageProcessorImplTest {

    private final CacheManager cacheManager = mock(CacheManager.class);
    private final RefDataMessageDecoder decoder = mock(RefDataMessageDecoder.class);
    private final RefDataHandlerRegistry handlerRegistry = mock(RefDataHandlerRegistry.class);
    private final Cache cache = mock(Cache.class);
    private RefDataRefDataMessageProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new RefDataRefDataMessageProcessorImpl(cacheManager, decoder, handlerRegistry);
    }

    @Test
    void shouldApplyUpdatesAndClearHandlerCaches() {
        // Given
        var dto = new TestDto("Example");
        var entity = new TestEntity();
        RefDataUpdateHandler<TestDto, TestEntity> handler = mock(RefDataUpdateHandler.class);
        var message = new RefDataMessage<>("message-1", "Prison", 1, List.of(dto));
        doReturn(message).when(decoder).decode("payload");
        when(handlerRegistry.find("Prison"))
            .thenReturn(Optional.<RefDataUpdateHandler<?, ?>>of(handler));
        when(handler.getBusinessKey(dto)).thenReturn("Example");
        when(handler.findEntity(dto)).thenReturn(Optional.of(entity));
        when(handler.cachesToClear()).thenReturn(List.of("prisons", "prisons"));
        when(cacheManager.getCache("prisons")).thenReturn(cache);

        // When
        processor.processMessage("payload");

        // Then
        verify(handler).validateDto(dto);
        verify(handler).findEntity(dto);
        verify(handler).updateEntityFromDto(dto, entity);
        verify(handler, never()).saveEntity(entity);
        verify(cache).clear();
    }

    @Test
    void shouldCreateEntityWhenNoExistingEntityIsFound() {
        // Given
        var dto = new TestDto("Example");
        var entity = new TestEntity();
        RefDataUpdateHandler<TestDto, TestEntity> handler = mock(RefDataUpdateHandler.class);
        doReturn(new RefDataMessage<>("message-1", "Prison", 1, List.of(dto)))
            .when(decoder).decode("payload");
        when(handlerRegistry.find("Prison"))
            .thenReturn(Optional.<RefDataUpdateHandler<?, ?>>of(handler));
        when(handler.getBusinessKey(dto)).thenReturn("Example");
        when(handler.findEntity(dto)).thenReturn(Optional.empty());
        when(handler.createEntityFromDto(dto)).thenReturn(entity);

        // When
        processor.processMessage("payload");

        // Then
        verify(handler).createEntityFromDto(dto);
        verify(handler, never()).updateEntityFromDto(dto, entity);
        verify(handler).saveEntity(entity);
    }

    @Test
    void shouldIgnoreMessageWhenDecoderFindsNoHandler() {
        // Given
        when(decoder.decode("payload")).thenReturn(null);

        // When
        processor.processMessage("payload");

        // Then
        verify(handlerRegistry, never()).find(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldThrowWhenHandlerIsMissingAfterDecoding() {
        // Given
        var message = new RefDataMessage<>("message-1", "Prison", 1, List.of());
        doReturn(message).when(decoder).decode("payload");
        when(handlerRegistry.find("Prison")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> processor.processMessage("payload"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No handler found for refDataType: Prison");
    }

    private record TestDto(String description) {
    }

    private static class TestEntity {
    }
}
