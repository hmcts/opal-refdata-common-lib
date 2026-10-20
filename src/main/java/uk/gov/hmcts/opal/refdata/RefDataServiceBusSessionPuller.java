package uk.gov.hmcts.opal.refdata;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.refdata.config.RefDataServiceBusProperties;

@Slf4j(topic = "opal.RefDataServiceBusSessionPuller")
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "opal.ref-data.service-bus", name = "consumer-enabled", havingValue = "true")
public class RefDataServiceBusSessionPuller {

    private final ServiceBusSessionReceiverClient sessionReceiverClient;
    private final MessageProcessor processor;
    private final RefDataMessageDecoder decoder;
    private final RefDataServiceBusProperties properties;

    @Scheduled(
        initialDelayString = "${opal.ref-data.service-bus.initial-delay-seconds}",
        fixedDelayString = "${opal.ref-data.service-bus.fixed-delay-seconds}",
        timeUnit = TimeUnit.SECONDS
    )
    public void pullSessionMessages() {
        log.info("Pulling ref-data Service Bus session messages");
        try {
            ServiceBusReceiverClient sessionReceiver = sessionReceiverClient.acceptNextSession();
            if (sessionReceiver == null) {
                //should not occur in normal running, so defensive coding here
                log.debug("No active ref-data Service Bus message sessions found");
                return;
            }
            try (sessionReceiver) {
                processSessionMessages(sessionReceiver);
            }
        } catch (ServiceBusException ex) {
            log.info("ServiceBusException reason={} isTransient={}", ex.getReason(), ex.isTransient(), ex);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("com.microsoft:timeout")) {
                //Cannot distinguish empty queue from queue not accessible (!), waits for receive-timeout-seconds
                log.debug("Empty queue or ASB is not reachable.");
            } else {
                log.error("Unexpected state while pulling ref-data messages", ex);
            }
        } catch (RuntimeException ex) {
            log.error("Unexpected failure while pulling ref-data Service Bus messages", ex);
        }
    }

    private void processSessionMessages(ServiceBusReceiverClient sessionWorker) {
        String sessionId = sessionWorker.getSessionId();
        log.info("Acquired ref-data Service Bus session lock. sessionId={}", sessionId);

        IterableStream<ServiceBusReceivedMessage> messages = sessionWorker.receiveMessages(
            properties.getMaxMessagesPerSession(),
            Duration.ofSeconds(properties.getReceiveTimeoutSeconds())
        );

        for (ServiceBusReceivedMessage message : messages) {
            processMessage(sessionWorker, message);
        }
    }

    private void processMessage(ServiceBusReceiverClient sessionWorker, ServiceBusReceivedMessage message) {

        RefDataMessage<?, ?> refDataMessage;
        try {
            refDataMessage = decoder.decode(message.getBody().toString());
        } catch (RuntimeException ex) {
            log.info("Could not decode message " + message.getMessageId(), ex);
            throw ex;
        }

        if (refDataMessage.handler() != null) {
            try {
                log.info("Processing message: messageId={} type={} releasePackageId={} serviceBusMessageId={}",
                    refDataMessage.messageId(), refDataMessage.refDataType(),
                    refDataMessage.releasePackageId(), message.getMessageId());
                processor.processMessage(refDataMessage);
            } catch (RuntimeException ex) {
                log.warn("Process failed for messageId={}", refDataMessage.messageId(), ex);
                try {
                    sessionWorker.abandon(message);
                    log.info("Abandon succeeded for messageId={} ", refDataMessage.messageId());
                } catch (RuntimeException abandonException) {
                    ex.addSuppressed(abandonException);
                    log.warn("Abandon failed for messageId={}", refDataMessage.messageId(), ex);
                }
                throw ex;
            }
        }

        try {
            sessionWorker.complete(message);
            log.info("Complete succeeded for messageId={} ", refDataMessage.messageId());
        } catch (RuntimeException ex) {
            log.warn("Complete failed for messageId={}", refDataMessage.messageId(), ex);
            throw ex;
        }

    }
}
