package uk.gov.hmcts.opal.refdata;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
            if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                //Cannot distinguish empty queue from queue not accessible (!), waits for receive-timeout-seconds
                log.debug("Empty queue or ASB is not reachable.");
            } else {
                log.error("Unexpected state while pulling ref-data messages", ex);
            }
        } catch (RuntimeException ex) {
            log.error("Unexpected failure while pulling ref-data Service Bus messages", ex);
            throw ex;
        }
    }

    private void processSessionMessages(ServiceBusReceiverClient sessionReceiver) {
        String sessionId = sessionReceiver.getSessionId();
        log.info("Acquired ref-data Service Bus session lock. sessionId={}", sessionId);

        IterableStream<ServiceBusReceivedMessage> messages = sessionReceiver.receiveMessages(
            properties.getMaxMessagesPerSession(),
            Duration.ofSeconds(properties.getReceiveTimeoutSeconds())
        );

        for (ServiceBusReceivedMessage message : messages) {
            processMessage(sessionReceiver, message);
        }
    }

    private void processMessage(ServiceBusReceiverClient sessionReceiver, ServiceBusReceivedMessage message) {

        try {
            log.info("Processing Service Bus message: serviceBusMessageId={}", message.getMessageId());
            processor.processMessage(message.getBody().toString());
        } catch (RuntimeException ex) {
            log.warn("Process failed for serviceBusMessageId={}", message.getMessageId(), ex);
            abandonMessage(sessionReceiver, message, ex, message.getMessageId());
            throw ex;
        }

        completeMessage(sessionReceiver, message);
    }

    private static void completeMessage(ServiceBusReceiverClient sessionReceiver, ServiceBusReceivedMessage message) {
        try {
            sessionReceiver.complete(message);
            log.info("Complete succeeded for serviceBusMessageId={}", message.getMessageId());
        } catch (RuntimeException ex) {
            log.warn("Complete failed for serviceBusMessageId={}", message.getMessageId(), ex);
            throw ex;
        }
    }

    private static void abandonMessage(ServiceBusReceiverClient sessionReceiver, ServiceBusReceivedMessage message,
        RuntimeException ex, String messageId) {
        try {
            sessionReceiver.abandon(message);
            log.info("Abandon succeeded for messageId={} ", messageId);
        } catch (RuntimeException abandonException) {
            ex.addSuppressed(abandonException);
            log.warn("Abandon failed for messageId={}", messageId, ex);
        }
    }
}
