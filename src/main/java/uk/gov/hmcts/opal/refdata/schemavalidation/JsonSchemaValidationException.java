package uk.gov.hmcts.opal.refdata.schemavalidation;

import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;

@SuppressWarnings("java:S110")
public class JsonSchemaValidationException extends InternalServerErrorException {

    public JsonSchemaValidationException(String msg) {
        super("JSON schema validation failed", msg);
    }
}
