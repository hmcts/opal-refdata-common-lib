package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CourtRecord(
    @JsonProperty("court_location_code") String courtLocationCode,
    @JsonProperty("common_platform_uuid") String commonPlatformUuid,
    @JsonProperty("court_welsh_name") String courtWelshName,
    @JsonProperty("court_type") String courtType,
    String notes,
    @JsonProperty("court_name") String courtName,
    @JsonProperty("cja_code") String cjaCode,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("default_duration") String defaultDuration,
    @JsonProperty("default_start_time") String defaultStartTime,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("publishing_status") String publishingStatus,
    @JsonProperty("division_code") String divisionCode,
    @JsonProperty("cluster_information_code") Integer clusterInformationCode,
    @JsonProperty("enforcement_area_code") Integer enforcementAreaCode,
    @JsonProperty("related_lja_code") Integer relatedLjaCode,
    @JsonProperty("region_code") Integer regionCode,
    List<Address> addresses,
    @JsonProperty("contact_information") List<ContactInformation> contactInformation
) {
}
