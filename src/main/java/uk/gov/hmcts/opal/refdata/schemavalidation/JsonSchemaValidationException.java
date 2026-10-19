package uk.gov.hmcts.opal.refdata.schemavalidation;

public class JsonSchemaValidationException extends RuntimeException {

    public JsonSchemaValidationException(String msg) {
        super(msg);
    }

    public JsonSchemaValidationException(Throwable t) {
        super(t);
    }

    public JsonSchemaValidationException(String message, Throwable t) {
        super(message, t);
    }

}
