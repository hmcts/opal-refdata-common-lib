package uk.gov.hmcts.opal.refdata.schemavalidation;

import static java.lang.String.format;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Loads JSON schemas from the classpath, caches them, and validates incoming
 * JSON documents against the requested schema.
 */
@Service
public class SchemaValidationService {

    private static final String PATH_ROOT = "jsonSchemas";

    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public boolean isValid(JsonNode jsonNode, String jsonSchemaFileName) {
        return validate(jsonNode, jsonSchemaFileName).isEmpty();
    }

    public void checkSchemaExists(String jsonSchemaFileName) {
        getJsonSchema(jsonSchemaFileName);
    }

    public void validateOrError(JsonNode jsonNode, String jsonSchemaFileName) {
        Set<String> errors = validate(jsonNode, jsonSchemaFileName);
        if (!errors.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder(errors.size() >> 7);
            messageBuilder.append("Validating against JSON schema '")
                .append(jsonSchemaFileName)
                .append("', found ")
                .append(errors.size())
                .append(" validation errors:");
            for (String message : errors) {
                messageBuilder.append("\n\t").append(message);
            }
            throw new JsonSchemaValidationException(messageBuilder.toString());
        }
    }

    public Set<String> validate(JsonNode jsonNode, String jsonSchemaFileName) {
        JsonSchema jsonSchema = getJsonSchema(jsonSchemaFileName);
        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode.toString(), InputFormat.JSON);
        return validationMessages.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
    }

    private JsonSchema getJsonSchema(String schemaFileName) {
        if (schemaFileName.isBlank()) {
            throw new SchemaConfigurationException("A schema filename is required to validate a JSON document.");
        }

        return schemaCache.computeIfAbsent(schemaFileName, this::loadJsonSchema);
    }

    private JsonSchema loadJsonSchema(String schemaFileName) {
        String filePath = Path.of(PATH_ROOT, schemaFileName).toString();
        ClassPathResource classPathResource = new ClassPathResource(filePath);

        if (!classPathResource.exists()) {
            throw new SchemaConfigurationException(
                format("No JSON Schema file found at '%s'", classPathResource.getPath()));
        }

        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
            return factory.getSchema(classPathResource.getURI());
        } catch (IOException e) {
            throw new SchemaConfigurationException(
                format("Problem reading JSON Schema from '%s'", filePath), e);
        }
    }
}
