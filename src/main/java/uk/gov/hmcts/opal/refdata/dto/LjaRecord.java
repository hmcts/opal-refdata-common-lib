package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LjaRecord(
    @JsonProperty("lja_code") String ljaCode,
    @JsonProperty("lja_name") String ljaName,
    @JsonProperty("court_short_name") String courtShortName,
    @JsonProperty("lja_welsh_name") String ljaWelshName,
    @JsonProperty("lja_type") String ljaType,
    @JsonProperty("lja_location_code") String ljaLocationCode,
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
