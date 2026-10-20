package uk.gov.hmcts.opal.refdata;

public interface MessageProcessor {

    <T, E> void processMessage(RefDataMessage<T, E> message);
}
