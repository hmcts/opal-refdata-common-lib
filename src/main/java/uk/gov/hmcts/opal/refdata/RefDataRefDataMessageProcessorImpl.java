package uk.gov.hmcts.opal.refdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies decoded ref-data records through the registered handlers in a
 * database transaction and clears affected caches after the transaction
 * commits successfully.
 */
@Slf4j(topic = "opal.RefDataMessageProcessor")
@RequiredArgsConstructor
@Service
public class RefDataRefDataMessageProcessorImpl implements RefDataMessageProcessor {

    private final CacheManager cacheManager;
    private final RefDataMessageDecoder decoder;
    private final RefDataHandlerRegistry handlerRegistry;

    @Transactional
    @Override
    public void processMessage(String messagePayload) {
        RefDataMessage<Object> message = decoder.decode(messagePayload);
        if (message == null) {
            //no handler found
            return;
        }
        RefDataUpdateHandler<?, ?> handler = handlerRegistry.find(message.refDataType())
            .orElseThrow(
                () -> new IllegalStateException("No handler found for refDataType: " + message.refDataType())
            );

        processDecodedMessage(message, handler);
    }

    // Type-safety bridge between the dynamically selected handler and Java’s generic type system
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processDecodedMessage(RefDataMessage<?> message, RefDataUpdateHandler<?, ?> handler) {
        processDecodedMessageWithHandler((RefDataMessage) message, (RefDataUpdateHandler) handler);
    }

    private <T, E> void processDecodedMessageWithHandler(RefDataMessage<T> message,
        RefDataUpdateHandler<T, E> handler) {
        message.records().forEach(dto -> {
            log.info("Processing record: messageId={} type={} businessKey={}",
                message.messageId(), message.refDataType(), handler.getBusinessKey(dto));
            applyUpdate(handler, dto);
        }
        );
        handler.cachesToClear().stream().distinct().forEach(this::clearCache);
    }

    private <T, E> void applyUpdate(RefDataUpdateHandler<T, E> handler, T dto) {
        handler.validateDto(dto);
        handler.findEntity(dto).ifPresentOrElse(
            entity -> handler.updateEntityFromDto(dto, entity),
            () -> handler.saveEntity(handler.createEntityFromDto(dto))
        );
    }

    // Clears Spring cache by name that holds ref data
    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Unable to clear ref-data cache because no cache is registered with name: {}", cacheName);
            return;
        }
        cache.clear();
        log.debug("Cleared ref-data cache: {}", cacheName);
    }
}
