package uk.gov.hmcts.opal.refdata.schemavalidation;

public class JsonSchemaValidationException extends RuntimeException {

    public JsonSchemaValidationException(String msg) {
        super(msg);
    }
}
