package uk.gov.hmcts.opal.refdata.schemavalidation;

import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;

public class JsonSchemaValidationException extends InternalServerErrorException {

    public JsonSchemaValidationException(String msg) {
        super("JSON schema validation failed", msg);
    }
}
