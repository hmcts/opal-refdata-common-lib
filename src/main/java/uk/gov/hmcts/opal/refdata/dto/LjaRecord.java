package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LjaRecord(
    @JsonProperty("lja_location_code") String ljaLocationCode,
    @JsonProperty("common_platform_uuid") String commonPlatformUuid,
    @JsonProperty("court_short_name") String courtShortName,
    @JsonProperty("lja_welsh_name") String ljaWelshName,
    @JsonProperty("lja_code") String ljaCode,
    @JsonProperty("lja_type") String ljaType,
    String notes,
    @JsonProperty("lja_name") String ljaName,
    @JsonProperty("cja_code") String cjaCode,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("default_duration") String defaultDuration,
    @JsonProperty("default_start_time") String defaultStartTime,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("publishing_status") String publishingStatus,
    @JsonProperty("division_code") String divisionCode,
    @JsonProperty("cluster_information_code") Integer clusterInformationCode,
    @JsonProperty("enforcement_area_code") Integer enforcementAreaCode,
    @JsonProperty("region_code") Integer regionCode,
    List<Address> addresses,
    @JsonProperty("contact_information") List<ContactInformation> contactInformation
) {
}
