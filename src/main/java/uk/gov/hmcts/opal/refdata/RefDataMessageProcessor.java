package uk.gov.hmcts.opal.refdata;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;

@Slf4j(topic = "opal.RefDataMessageProcessor")
@Service
public class RefDataMessageProcessor implements MessageProcessor {

    private static final String REF_DATA_MESSAGE_SCHEMA = "ref-data/ref_data_schema.json";

    private final ObjectMapper objectMapper;
    private final SchemaValidationService schemaValidationService;
    private final RefDataHandlerRegistry handlerRegistry;
    private final CacheManager cacheManager;

    public RefDataMessageProcessor(ObjectMapper objectMapper,
        SchemaValidationService schemaValidationService,
        RefDataHandlerRegistry handlerRegistry,
        CacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.schemaValidationService = schemaValidationService;
        this.handlerRegistry = handlerRegistry;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    void validateMessageSchema() {
        schemaValidationService.checkSchemaExists(REF_DATA_MESSAGE_SCHEMA);
    }

    @Transactional
    @Override
    public void processMessage(String messagePayload) {

        JsonNode messageNode = readMessageNode(messagePayload);

        schemaValidationService.validateOrError(messageNode, REF_DATA_MESSAGE_SCHEMA);

        String dataProduct = extractDataProduct(messageNode);

        Optional<RefDataUpdateHandler<?, ?>> maybeHandler = handlerRegistry.find(dataProduct);
        if (maybeHandler.isEmpty()) {
            log.debug("Ignoring ref-data message with no registered handler for type: {}",
                dataProduct);
            return;
        }
        RefDataUpdateHandler<?, ?> handler = maybeHandler.get();

        JsonNode payloadNode = messageNode.path("payload");
        JsonNode recordsNode = payloadNode.path("records");
        if (!recordsNode.isArray()) {
            return;
        }

        recordsNode.forEach(recordNode -> applyUpdate(handler, recordNode));

        List<String> associatedCaches = handler.cachesToClear();
        associatedCaches.forEach(
            this::clearCache
        );
        System.out.println("Finished processing of message payload: " + messagePayload);
    }

    private JsonNode readMessageNode(String messagePayload) {
        try {
            return objectMapper.readTree(messagePayload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse ref data message", ex);
        }
    }

    private String extractDataProduct(JsonNode messageNode) {
        return messageNode.path("header").path("data_product").asText(null);
    }

    @SuppressWarnings("unchecked")
    private <T, E> void applyUpdate(RefDataUpdateHandler<T, E> handler, Object payload) {
        T dto;
        try {
            dto = objectMapper.convertValue(payload, handler.payloadType());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unable to convert ref data payload", ex);
        }
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
