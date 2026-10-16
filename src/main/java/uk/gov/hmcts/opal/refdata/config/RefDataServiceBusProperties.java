package uk.gov.hmcts.opal.refdata.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opal.ref-data.service-bus")
@Data
public class RefDataServiceBusProperties {
    private String topicName;
    private String subscriptionName;
    private boolean consumerEnabled;
    private String connectionString;
    private String fullyQualifiedNamespace;
    private int maxMessagesPerSession = 50;
    private long initialDelayMilliseconds = 300_000L;
    private long fixedDelayMilliseconds = 300_000L;
    private Duration receiveTimeout = Duration.ofSeconds(3);
}
