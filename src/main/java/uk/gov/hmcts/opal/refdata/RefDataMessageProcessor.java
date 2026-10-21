package uk.gov.hmcts.opal.refdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "opal.RefDataMessageProcessor")
@Service
public class RefDataMessageProcessor implements MessageProcessor {

    private final CacheManager cacheManager;
    private final RefDataMessageDecoder decoder;
    private final RefDataHandlerRegistry handlerRegistry;

    public RefDataMessageProcessor(CacheManager cacheManager, RefDataMessageDecoder decoder,
        RefDataHandlerRegistry handlerRegistry) {
        this.cacheManager = cacheManager;
        this.decoder = decoder;
        this.handlerRegistry = handlerRegistry;
    }

    @Transactional
    @Override
    public void processMessage(String messagePayload) {
        RefDataMessage<?> message = decoder.decode(messagePayload);
        if (message == null) {
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
                message.messageId() ,message.refDataType(), handler.getBusinessKey(dto));
            applyUpdate(handler, dto);
        }
        );
        handler.cachesToClear().stream().distinct().forEach(this::clearCache);
    }

    private <T, E> void applyUpdate(RefDataUpdateHandler<T, E> handler, T dto) {
        handler.validateDto(dto);
        E entity = handler.findEntity(dto)
            .orElseGet(() -> handler.createEntity(dto));
        handler.updateEntityFromDto(dto, entity);
        handler.saveEntity(entity); //this could be a newly created entity
    }

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
