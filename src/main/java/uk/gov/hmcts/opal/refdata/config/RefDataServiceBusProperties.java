package uk.gov.hmcts.opal.refdata.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "opal.ref-data.service-bus")
@Validated
@Data
public class RefDataServiceBusProperties {
    @NotBlank
    private String topicName;
    @NotBlank
    private String subscriptionName;
    @NotNull
    private Boolean consumerEnabled;
    private String connectionString;
    private String fullyQualifiedNamespace;
    @NotNull
    @Positive
    private Integer maxMessagesPerSession;
    @NotNull
    @PositiveOrZero
    private Long initialDelaySeconds;
    @NotNull
    @Positive
    private Long fixedDelaySeconds;
    @NotNull
    @DurationMin(nanos = 1)
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration receiveTimeout;

    @AssertTrue(message = "exactly one of connection-string or fully-qualified-namespace must be set")
    public boolean isAuthenticationConfigured() {
        return (connectionString != null && !connectionString.isBlank())
            ^ (fullyQualifiedNamespace != null && !fullyQualifiedNamespace.isBlank());
    }
}
