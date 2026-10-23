package uk.gov.hmcts.opal.refdata;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;

/**
 * Parses and validates incoming ref-data messages, then converts their
 * records into the DTO type declared by the matching handler.
 */
@Slf4j(topic = "opal.RefDataMessageDecoder")
@RequiredArgsConstructor
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

    @PostConstruct
    void validateMessageSchema() {
        schemaValidationService.checkSchemaExists(REF_DATA_MESSAGE_SCHEMA);
    }

    public RefDataMessage<Object> decode(String messagePayload) {
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
            return null;
        }
        var records = decodeRecords(maybeHandler.get(), payloadNode.path(RECORDS));
        return new RefDataMessage<>(messageId, dataProduct, releasePackageId, records);
    }

    private List<Object> decodeRecords(RefDataUpdateHandler<?, ?> handler, JsonNode recordsNode) {
        var records = new ArrayList<Object>();
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
