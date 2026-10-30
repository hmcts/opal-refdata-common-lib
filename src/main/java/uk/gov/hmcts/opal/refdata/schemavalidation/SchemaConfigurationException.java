package uk.gov.hmcts.opal.refdata.schemavalidation;

import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;

public class SchemaConfigurationException extends InternalServerErrorException {

    public SchemaConfigurationException(String msg) {
        super("Schema configuration error", msg);
    }
}
