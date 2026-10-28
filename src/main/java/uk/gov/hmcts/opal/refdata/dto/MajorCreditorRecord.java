package uk.gov.hmcts.opal.refdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MajorCreditorRecord(
    String name,
    @JsonProperty("welsh_name") String welshName,
    @JsonProperty("start_date") String startDate,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("publishing_status") String publishingStatus,
    @JsonProperty("business_unit") String businessUnit,
    @JsonProperty("account_name") String accountName,
    @JsonProperty("sort_code") String sortCode,
    @JsonProperty("bank_account_number") String bankAccountNumber,
    @JsonProperty("short_name") String shortName,
    List<OrgAddress> addresses,
    @JsonProperty("contact_information") List<OrgContactInformation> contactInformation
) {
}
