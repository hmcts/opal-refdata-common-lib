package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MajorCreditorRecord(
    @JsonProperty("short_name") String shortName,
    @JsonProperty("second_language_name") String secondLanguageName,
    @JsonProperty("end_date") String endDate,
    String description,
    @JsonProperty("publishing_status") String publishingStatus,
    @JsonProperty("business_unit") String businessUnit,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("bank_account_number") String bankAccountNumber,
    @JsonProperty("bank_account_name") String bankAccountName,
    @JsonProperty("bank_sort_code") String bankSortCode,
    @JsonProperty("organisation_welsh_address") String organisationWelshAddress,
    List<OrgAddress> addresses,
    @JsonProperty("contact_information") List<OrgContactInformation> contactInformation
) {
}
