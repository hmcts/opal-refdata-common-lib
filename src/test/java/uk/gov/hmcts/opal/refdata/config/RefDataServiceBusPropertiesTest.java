package uk.gov.hmcts.opal.refdata.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RefDataServiceBusPropertiesTest {

    private static final String PREFIX = "opal.ref-data.service-bus.";
    private static final String[] VALID_PROPERTIES = {
        PREFIX + "consumer-enabled=true",
        PREFIX + "topic-name=refdata",
        PREFIX + "subscription-name=refdata-subscription",
        PREFIX + "fully-qualified-namespace=refdata.servicebus.windows.net",
        PREFIX + "max-messages-per-session=50",
        PREFIX + "initial-delay-seconds=0",
        PREFIX + "fixed-delay-seconds=300",
        PREFIX + "receive-timeout-seconds=3"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(VALID_PROPERTIES);

    @Test
    void startsWithValidPropertiesAndBindsTimeoutInSeconds() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RefDataServiceBusProperties.class).getReceiveTimeoutSeconds())
                .isEqualTo(3L);
        });
    }

    @Test
    void acceptsConnectionStringInsteadOfNamespace() {
        contextRunner.withPropertyValues(
            "opal.ref-data.service-bus.fully-qualified-namespace=",
            "opal.ref-data.service-bus.connection-string=emulator-connection-string"
        ).run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsStartupWhenNeitherAuthenticationOptionIsSpecified() {
        contextRunner.withPropertyValues("opal.ref-data.service-bus.fully-qualified-namespace=")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining(
                        "exactly one of connection-string or fully-qualified-namespace must be set");
            });
    }

    @Test
    void failsStartupWhenBothAuthenticationOptionsAreSpecified() {
        contextRunner.withPropertyValues(PREFIX + "connection-string=emulator-connection-string")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining(
                        "exactly one of connection-string or fully-qualified-namespace must be set");
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "topic-name=",
        "subscription-name= ",
        "max-messages-per-session=0",
        "initial-delay-seconds=-1",
        "fixed-delay-seconds=0",
        "receive-timeout-seconds=0s",
        "receive-timeout-seconds=-1s"
    })
    void failsStartupForInvalidProperties(String property) {
        contextRunner.withPropertyValues("opal.ref-data.service-bus." + property)
            .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "consumer-enabled", "topic-name", "subscription-name", "max-messages-per-session",
        "initial-delay-seconds", "fixed-delay-seconds", "receive-timeout-seconds"
    })
    void failsStartupWhenRequiredPropertyIsMissing(String property) {
        String[] remainingProperties = Arrays.stream(VALID_PROPERTIES)
            .filter(value -> !value.startsWith(PREFIX + property + "="))
            .toArray(String[]::new);
        new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(remainingProperties)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("Binding validation errors");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RefDataServiceBusProperties.class)
    static class PropertiesConfiguration {
    }
}
