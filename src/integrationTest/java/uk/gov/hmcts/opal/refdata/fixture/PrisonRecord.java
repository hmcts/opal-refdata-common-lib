package uk.gov.hmcts.opal.refdata.fixture;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PrisonRecord(
    String name,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("end_date") String endDate,
    String notes,
    List<OrgAddress> addresses
) {
}
