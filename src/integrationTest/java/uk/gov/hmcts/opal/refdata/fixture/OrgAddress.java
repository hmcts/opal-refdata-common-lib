package uk.gov.hmcts.opal.refdata.fixture;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrgAddress(
    @JsonProperty("address_line_1") String addressLine1,
    @JsonProperty("address_line_2") String addressLine2,
    @JsonProperty("address_line_3") String addressLine3,
    @JsonProperty("address_line_4") String addressLine4,
    @JsonProperty("address_line_5") String addressLine5,
    String postcode,
    @JsonProperty("address_type") String addressType
) {
}
