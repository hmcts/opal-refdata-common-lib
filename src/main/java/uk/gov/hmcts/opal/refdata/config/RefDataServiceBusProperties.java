package uk.gov.hmcts.opal.refdata.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(prefix = "opal.ref-data.service-bus")
@Data
public class RefDataServiceBusProperties {
    private String topicName;
    private String subscriptionName;
    private boolean consumerEnabled;
    private String connectionString;
    private String fullyQualifiedNamespace;
    private int maxMessagesPerSession = 50;
    private long initialDelaySeconds = 300L;
    private long fixedDelaySeconds = 300L;
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration receiveTimeout = Duration.ofSeconds(3);
}
