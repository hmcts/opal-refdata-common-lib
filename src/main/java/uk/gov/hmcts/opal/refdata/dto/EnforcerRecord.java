package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EnforcerRecord(
    @JsonProperty("enforcer_code") Integer enforcerCode,
    String name,
    @JsonProperty("welsh_name") String welshName,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("publishing_status") String publishingStatus,
    List<OrgAddress> addresses,
    @JsonProperty("contact_information") List<OrgContactInformation> contactInformation
) {
}
