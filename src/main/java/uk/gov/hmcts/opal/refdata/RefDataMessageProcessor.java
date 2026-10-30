package uk.gov.hmcts.opal.refdata;

public interface RefDataMessageProcessor {

    /**
     * Processes a message received from the application's messaging layer.
     * The messaging layer invokes this method for each ref-data message it
     * receives and is responsible for acknowledgement and retry handling.
     *
     * @param messagePayload the raw message body received from the messaging layer
     */
    void processMessage(String messagePayload);
}
