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
        // Given
        var json = objectMapper.readTree("{}");

        // When
        var valid = schemaValidationService.isValid(json, SCHEMA_FILE);
        var errors = schemaValidationService.validate(json, SCHEMA_FILE);

        // Then
        assertThat(valid).isFalse();
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldValidateValidPrisonsPayload() throws Exception {
        // Given
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
                    "name": "Example Prison",
                    "publishing_status": "Active",
                    "start_date": "2026-01-01"
                  }
                ]
              }
            }
            """);

        // When
        var valid = schemaValidationService.isValid(json, SCHEMA_FILE);
        var errors = schemaValidationService.validate(json, SCHEMA_FILE);

        // Then
        assertThat(valid).isTrue();
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnValidationErrorForInvalidPrisonPostcode() throws Exception {
        // Given
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
                    "name": "Example Prison",
                    "publishing_status": "Active",
                    "start_date": "2026-01-01",
                    "addresses": [
                      {
                        "postcode": "INVALID"
                      }
                    ]
                  }
                ]
              }
            }
            """);

        // When
        var errors = schemaValidationService.validate(json, SCHEMA_FILE);

        // Then
        assertThat(errors)
            .anyMatch(error -> error.contains("postcode") && error.contains("pattern"));
    }

    @Test
    void shouldAllowExistingSchemaToBeChecked() {
        // Given
        var schemaFile = SCHEMA_FILE;

        // When
        schemaValidationService.checkSchemaExists(schemaFile);

        // Then
        // No exception is thrown.
    }

    @Test
    void shouldThrowWhenJsonDoesNotMatchSchema() throws Exception {
        // Given
        var json = objectMapper.readTree("{}");

        // When
        var exception = assertThatThrownBy(() -> schemaValidationService.validateOrError(json, SCHEMA_FILE));

        // Then
        exception
            .isInstanceOf(JsonSchemaValidationException.class)
            .hasMessageContaining("Validating against JSON schema 'ref-data/ref_data_schema.json'")
            .hasMessageContaining("validation errors:");
    }

    @Test
    void shouldRejectBlankSchemaFilename() throws Exception {
        // Given
        var json = objectMapper.readTree("{}");

        // When
        var exception = assertThatThrownBy(() -> schemaValidationService.isValid(json, " "));

        // Then
        exception
            .isInstanceOf(SchemaConfigurationException.class)
            .hasMessage("A schema filename is required to validate a JSON document.");
    }
}
