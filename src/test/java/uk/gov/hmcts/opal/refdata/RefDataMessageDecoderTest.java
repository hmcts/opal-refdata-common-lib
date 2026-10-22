package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;

@SuppressWarnings("unchecked")
class RefDataMessageDecoderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaValidationService schemaValidationService = mock(SchemaValidationService.class);
    private final RefDataHandlerRegistry handlerRegistry = mock(RefDataHandlerRegistry.class);
    private RefDataMessageDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new RefDataMessageDecoder(objectMapper, schemaValidationService, handlerRegistry);
    }

    @Test
    void shouldValidateAndDecodeMessageUsingMatchingHandler() throws Exception {
        RefDataUpdateHandler<DecodedRecord, Object> handler = mock(RefDataUpdateHandler.class);
        when(handler.payloadType()).thenReturn(DecodedRecord.class);
        when(handlerRegistry.find("Prison")).thenReturn(Optional.of(handler));

        var decoded = decoder.decode("""
            {
              "header": {
                "message_id": "message-1",
                "data_product": "Prison",
                "release_package_id": 12
              },
              "payload": {
                "records": [{"description": "Example Prison"}]
              }
            }
            """);

        assertThat(decoded).isEqualTo(new RefDataMessage<>(
            "message-1", "Prison", 12, List.of(new DecodedRecord("Example Prison"))));
        verify(schemaValidationService).validateOrError(
            objectMapper.readTree("""
                {
                  "header": {
                    "message_id": "message-1",
                    "data_product": "Prison",
                    "release_package_id": 12
                  },
                  "payload": {
                    "records": [{"description": "Example Prison"}]
                  }
                }
                """), "ref-data/ref_data_schema.json");
    }

    @Test
    void shouldReturnNullWhenNoHandlerIsRegistered() {
        when(handlerRegistry.find("Prison")).thenReturn(Optional.empty());

        var decoded = decoder.decode("""
            {
              "header": {"message_id": "message-1", "data_product": "Prison"},
              "payload": {"records": []}
            }
            """);

        assertThat(decoded).isNull();
    }

    @Test
    void shouldThrowWhenRecordCannotBeConvertedToHandlerPayload() {
        RefDataUpdateHandler<Integer, Object> handler = mock(RefDataUpdateHandler.class);
        when(handler.payloadType()).thenReturn(Integer.class); //triggers the IllegalArgumentException
        when(handlerRegistry.find("Prison")).thenReturn(Optional.of(handler));

        assertThatThrownBy(() -> decoder.decode("""
            {
              "header": {"message_id": "message-1", "data_product": "Prison"},
              "payload": {"records": [{"description": "Example Prison"}]}
            }
            """))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to convert ref data payload");
    }

    @Test
    void shouldThrowWhenMessageCannotBeParsed() {
        assertThatThrownBy(() -> decoder.decode("not json"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to parse ref data message");
    }

    private record DecodedRecord(String description) {
    }
}
