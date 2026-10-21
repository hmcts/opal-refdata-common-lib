package uk.gov.hmcts.opal.refdata;

import java.util.ArrayList;
import java.util.List;
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
    private static final String HEADER = "header";
    private static final String MESSAGE_ID = "message_id";
    private static final String PAYLOAD = "payload";
    private static final String DATA_PRODUCT = "data_product";
    private static final String RELEASE_PACKAGE_ID = "release_package_id";
    private static final String RECORDS = "records";
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
        JsonNode messageNode;
        try {
            messageNode = objectMapper.readTree(messagePayload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse ref data message", ex);
        }
        schemaValidationService.validateOrError(messageNode, REF_DATA_MESSAGE_SCHEMA);
        JsonNode headerNode = messageNode.path(HEADER);
        String messageId = headerNode.path(MESSAGE_ID).asString(null);
        JsonNode payloadNode = messageNode.path(PAYLOAD);
        String dataProduct = headerNode.path(DATA_PRODUCT).asString(null);
        JsonNode releasePackageIdNode = headerNode.path(RELEASE_PACKAGE_ID);
        Integer releasePackageId = releasePackageIdNode.isIntegralNumber()
            ? releasePackageIdNode.intValue() : null;
        var maybeHandler = handlerRegistry.find(dataProduct);
        if (maybeHandler.isEmpty()) {
            log.info("No handler registered for type: {}", dataProduct);
            return new RefDataMessage<>(messageId, null, dataProduct, releasePackageId,
                List.of());
        }
        return decodeKnownMessage(
            messageId,
            dataProduct,
            releasePackageId,
            maybeHandler.get(),
            payloadNode.path(RECORDS)
        );
    }

    private <T, E> RefDataMessage<T, E> decodeKnownMessage(
        String messageId,
        String dataProduct,
        Integer releasePackageId,
        RefDataUpdateHandler<T, E> handler,
        JsonNode recordsNode
    ) {
        var records = decodeRecords(handler, recordsNode);
        return new RefDataMessage<>(messageId, handler, dataProduct, releasePackageId, records);
    }

    private <T, E> List<T> decodeRecords(RefDataUpdateHandler<T, E> handler, JsonNode recordsNode) {
        var records = new ArrayList<T>();
        recordsNode.forEach(recordNode -> {
            try {
                records.add(objectMapper.convertValue(recordNode, handler.payloadType()));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Unable to convert ref data payload", ex);
            }
        });
        return records;
    }

}
