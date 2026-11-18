package uk.gov.hmcts.opal.refdata.schemavalidation;

import static java.lang.String.format;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Loads JSON schemas from the classpath, caches them, and validates incoming
 * JSON documents against the requested schema.
 */
@ConditionalOnProperty(prefix = "opal.ref-data.service-bus", name = "consumer-enabled", havingValue = "true")
@Service
public class SchemaValidationService {

    private static final String PATH_ROOT = "jsonSchemas";

    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

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
        Schema schema = getJsonSchema(jsonSchemaFileName);
        return schema.validate(jsonNode.toString(), InputFormat.JSON).stream()
            .map(Error::toString).collect(Collectors.toSet());
    }

    private Schema getJsonSchema(String schemaFileName) {
        if (schemaFileName.isBlank()) {
            throw new SchemaConfigurationException("A schema filename is required to validate a JSON document.");
        }

        return schemaCache.computeIfAbsent(schemaFileName, this::loadJsonSchema);
    }

    private Schema loadJsonSchema(String schemaFileName) {
        String filePath = Path.of(PATH_ROOT, schemaFileName).toString();
        ClassPathResource classPathResource = new ClassPathResource(filePath);

        if (!classPathResource.exists()) {
            throw new SchemaConfigurationException(
                format("No JSON Schema file found at '%s'", classPathResource.getPath()));
        }

        return schemaRegistry.getSchema(SchemaLocation.of("classpath:" + filePath));
    }
}
