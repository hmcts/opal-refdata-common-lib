package uk.gov.hmcts.opal.refdata.schemavalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SchemaValidationServiceTest {

    private static final String SCHEMA_FILE = "ref-data/ref_data_schema.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SchemaValidationService schemaValidationService;

    @BeforeEach
    void setUp() {
        schemaValidationService = new SchemaValidationService();
    }

    @Test
    void shouldValidateJsonAgainstSchema() throws Exception {
        var json = objectMapper.readTree("{}");

        assertThat(schemaValidationService.isValid(json, SCHEMA_FILE)).isFalse();
        assertThat(schemaValidationService.validate(json, SCHEMA_FILE)).isNotEmpty();
    }

    @Test
    void shouldValidateValidPrisonsPayload() throws Exception {
        var json = objectMapper.readTree("""
            {
              "header": {
                "message_id": "123e4567-e89b-12d3-a456-426614174000",
                "message_type": "ReferenceData",
                "data_product": "Prison",
                "operation": "PUBLISH",
                "source_system": "Semarchy",
                "created_date_time": "2026-01-01T12:00:00Z",
                "release_package_id": 1,
                "record_count": 1
              },
              "payload": {
                "records": [
                  {
                    "description": "Example Prison",
                    "publishing_status": "Active",
                    "start_date": "2026-01-01"
                  }
                ]
              }
            }
            """);

        assertThat(schemaValidationService.isValid(json, SCHEMA_FILE)).isTrue();
        assertThat(schemaValidationService.validate(json, SCHEMA_FILE)).isEmpty();
    }

    @Test
    void shouldReturnValidationErrorForInvalidPrisonPostcode() throws Exception {
        var json = objectMapper.readTree("""
            {
              "header": {
                "message_id": "123e4567-e89b-12d3-a456-426614174000",
                "message_type": "ReferenceData",
                "data_product": "Prison",
                "operation": "PUBLISH",
                "source_system": "Semarchy",
                "created_date_time": "2026-01-01T12:00:00Z",
                "release_package_id": 1,
                "record_count": 1
              },
              "payload": {
                "records": [
                  {
                    "description": "Example Prison",
                    "publishing_status": "Active",
                    "start_date": "2026-01-01",
                    "addresses": [
                      {
                        "post_code": "INVALID"
                      }
                    ]
                  }
                ]
              }
            }
            """);

        assertThat(schemaValidationService.validate(json, SCHEMA_FILE))
            .anyMatch(error -> error.contains("post_code") && error.contains("pattern"));
    }

    @Test
    void shouldAllowExistingSchemaToBeChecked() {
        schemaValidationService.checkSchemaExists(SCHEMA_FILE);
    }

    @Test
    void shouldThrowWhenJsonDoesNotMatchSchema() throws Exception {
        var json = objectMapper.readTree("{}");

        assertThatThrownBy(() -> schemaValidationService.validateOrError(json, SCHEMA_FILE))
            .isInstanceOf(JsonSchemaValidationException.class)
            .hasMessageContaining("Validating against JSON schema 'ref-data/ref_data_schema.json'")
            .hasMessageContaining("validation errors:");
    }

    @Test
    void shouldRejectBlankSchemaFilename() throws Exception {
        var json = objectMapper.readTree("{}");

        assertThatThrownBy(() -> schemaValidationService.isValid(json, " "))
            .isInstanceOf(SchemaConfigurationException.class)
            .hasMessage("A schema filename is required to validate a JSON document.");
    }
}
