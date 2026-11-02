package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
    classes = RefDataConsumerDisabledIT.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "opal.ref-data.service-bus.consumer-enabled=false"
)
class RefDataConsumerDisabledIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotCreateRefDataConsumerWhenConsumerIsDisabled() {
        assertThat(applicationContext.getBeansOfType(RefDataServiceBusSessionPuller.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(RefDataMessageProcessor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ServiceBusSessionReceiverClient.class)).isEmpty();
    }

    @SpringBootApplication(
        scanBasePackages = "uk.gov.hmcts.opal.refdata",
        excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
    )
    static class TestApplication {
    }
}
