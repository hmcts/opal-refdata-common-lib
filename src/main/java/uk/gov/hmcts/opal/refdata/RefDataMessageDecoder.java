package uk.gov.hmcts.opal.refdata;

import java.util.ArrayList;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;

@Slf4j(topic = "opal.RefDataMessageDecoder")
@Service
public class RefDataMessageDecoder {
    private static final String REF_DATA_MESSAGE_SCHEMA = "ref-data/ref_data_schema.json";
    private final ObjectMapper objectMapper;
    private final SchemaValidationService schemaValidationService;
    private final RefDataHandlerRegistry handlerRegistry;

    public RefDataMessageDecoder(ObjectMapper objectMapper, SchemaValidationService schemaValidationService,
        RefDataHandlerRegistry handlerRegistry) {
        this.objectMapper = objectMapper;
        this.schemaValidationService = schemaValidationService;
        this.handlerRegistry = handlerRegistry;
    }

    @PostConstruct
    void validateMessageSchema() {
        schemaValidationService.checkSchemaExists(REF_DATA_MESSAGE_SCHEMA);
    }

    public RefDataMessage<?, ?> decode(String messagePayload) {
        JsonNode messageNode = readMessageNode(messagePayload);
        schemaValidationService.validateOrError(messageNode, REF_DATA_MESSAGE_SCHEMA);
        JsonNode headerNode = messageNode.path("header");
        String messageId = headerNode.path("message_id").asText(null);
        JsonNode payloadNode = messageNode.path("payload");
        String dataProduct = headerNode.path("data_product").asText(null);
        JsonNode releasePackageIdNode = headerNode.path("release_package_id");
        Integer releasePackageId = releasePackageIdNode.isIntegralNumber()
            ? releasePackageIdNode.intValue() : null;
        var handler = handlerRegistry.find(dataProduct);
        if (handler.isEmpty()) {
            log.info("No handler registered for type: {}", dataProduct);
            return new RefDataMessage<>(messageId, null, dataProduct, releasePackageId,
                java.util.List.of());
        }
        JsonNode recordsNode = payloadNode.path("records");
        return decodeKnownMessage(messageId, dataProduct, releasePackageId, handler.get(), recordsNode);
    }

    private <T, E> RefDataMessage<T, E> decodeKnownMessage(String messageId, String refDataType,
        Integer releasePackageId,
        RefDataUpdateHandler<T, E> handler, JsonNode recordsNode) {
        var records = new ArrayList<T>();
        recordsNode.forEach(recordNode -> records.add(convert(recordNode, handler)));
        return new RefDataMessage<>(messageId, handler, refDataType, releasePackageId, records);
    }

    private JsonNode readMessageNode(String messagePayload) {
        try {
            return objectMapper.readTree(messagePayload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse ref data message", ex);
        }
    }

    private <T> T convert(JsonNode recordNode, RefDataUpdateHandler<T, ?> handler) {
        try {
            return objectMapper.convertValue(recordNode, handler.payloadType());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unable to convert ref data payload", ex);
        }
    }
}
