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

    public RefDataMessageProcessor(CacheManager cacheManager, RefDataMessageDecoder decoder) {
        this.cacheManager = cacheManager;
        this.decoder = decoder;
    }

    @Transactional
    @Override
    public void processMessage(String messagePayload) {
        RefDataMessage<?, ?> message = decoder.decode(messagePayload);
        if (message.handler() == null) {
            return;
        }
        processDecodedMessage(message);
    }

    private <T, E> void processDecodedMessage(RefDataMessage<T, E> message) {
        RefDataUpdateHandler<T, E> handler = message.handler();
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
