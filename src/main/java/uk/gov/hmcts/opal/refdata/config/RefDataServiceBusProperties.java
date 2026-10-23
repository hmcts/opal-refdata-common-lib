package uk.gov.hmcts.opal.refdata.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "opal.ref-data.service-bus")
@Validated
@Data
public class RefDataServiceBusProperties {

    private String fullyQualifiedNamespace;

    private String topicName;

    private String subscriptionName;

    @NotNull
    private Boolean consumerEnabled;

    @Positive
    private Integer maxMessagesPerSession;

    @PositiveOrZero
    private Long initialDelaySeconds;

    @Positive
    private Long fixedDelaySeconds;

    @Positive
    private Long receiveTimeoutSeconds;

    @AssertTrue(message = "when consumer enabled, topic-name must be set")
    public boolean isTopicConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled) || (topicName != null && !topicName.isBlank());
    }

    @AssertTrue(message = "when consumer enabled, subscription-name must be set")
    public boolean isSubscriptionConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled)
            || (subscriptionName != null && !subscriptionName.isBlank());
    }

    @AssertTrue(message = "when consumer enabled, max-messages-per-session must be set")
    public boolean isMaxMessagesPerSessionConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled) || maxMessagesPerSession != null;
    }

    @AssertTrue(message = "when consumer enabled, initial-delay-seconds must be set")
    public boolean isInitialDelaySecondsConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled) || initialDelaySeconds != null;
    }

    @AssertTrue(message = "when consumer enabled, fixed-delay-seconds must be set")
    public boolean isFixedDelaySecondsConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled) || fixedDelaySeconds != null;
    }

    @AssertTrue(message = "when consumer enabled, receive-timeout-seconds must be set")
    public boolean isReceiveTimeoutSecondsConfigured() {
        return !Boolean.TRUE.equals(consumerEnabled) || receiveTimeoutSeconds != null;
    }
}
