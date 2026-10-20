package uk.gov.hmcts.opal.refdata;

import java.util.List;

public record RefDataMessage<T, E>(String messageId, RefDataUpdateHandler<T, E> handler,
                                   String refDataType, Integer releasePackageId, List<T> records) {
}
