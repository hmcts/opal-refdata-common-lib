package uk.gov.hmcts.opal.refdata.schemavalidation;

public class SchemaConfigurationException extends RuntimeException {

    public SchemaConfigurationException(String msg) {
        super(msg);
    }

    public SchemaConfigurationException(Throwable t) {
        super(t);
    }

    public SchemaConfigurationException(String message, Throwable t) {
        super(message, t);
    }

}
