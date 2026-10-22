package uk.gov.hmcts.opal.refdata;

import java.util.List;

public record RefDataMessage<T>(String messageId, String refDataType, Integer releasePackageId, List<T> records) {
}
