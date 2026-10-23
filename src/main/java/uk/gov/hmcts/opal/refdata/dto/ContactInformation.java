package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContactInformation(
    @JsonProperty("contact_value") String contactValue,
    @JsonProperty("contact_sub_type") String contactSubType,
    @JsonProperty("contact_type") String contactType
) {
}
