package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CourtRecord(
    @JsonProperty("court_name") String courtName,
    @JsonProperty("court_welsh_name") String courtWelshName,
    @JsonProperty("court_location_code") String courtLocationCode,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("cja_code") String cjaCode,
    @JsonProperty("division_code") String divisionCode,
    @JsonProperty("default_start_time") String defaultStartTime,
    @JsonProperty("default_duration") String defaultDuration,
    @JsonProperty("common_platform_uuid") String commonPlatformUuid,
    String notes,
    @JsonProperty("publishing_status") String publishingStatus,
    List<Address> addresses,
    @JsonProperty("contact_information") List<ContactInformation> contactInformation
) {
}
