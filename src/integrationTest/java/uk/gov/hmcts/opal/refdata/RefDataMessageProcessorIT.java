package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.gov.hmcts.opal.refdata.fixture.RefData;
import uk.gov.hmcts.opal.refdata.fixture.RefDataRepository;
import uk.gov.hmcts.opal.refdata.fixture.RefDataTestHandler;
import uk.gov.hmcts.opal.refdata.framework.MessageProcessor;
import uk.gov.hmcts.opal.refdata.framework.RefDataHandlerRegistry;
import uk.gov.hmcts.opal.refdata.framework.RefDataMessageProcessor;
import uk.gov.hmcts.opal.refdata.framework.SchemaValidationService;

@SpringBootTest(
    classes = RefDataMessageProcessorIT.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.generate-unique-name=true",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:ref-data-schema.sql",
        "spring.jpa.hibernate.ddl-auto=validate"
    }
)
class RefDataMessageProcessorIT {

    @Autowired
    private MessageProcessor processor;

    @Autowired
    private RefDataRepository repository;

    @Autowired
    private RefDataHandlerRegistry handlerRegistry;

    @Test
    void shouldLoadUpdateAndSaveExistingRefData() {
        assertThat(handlerRegistry.find("ref_data")).containsInstanceOf(RefDataTestHandler.class);

        repository.deleteAll();
        RefData seed = new RefData();
        seed.setId(1L);
        seed.setCode("OLD");
        seed.setName("Original name");
        seed.setDescription("Original description");
        seed.setActive(true);
        repository.save(seed);

        RefData original = repository.findById(1L).orElseThrow();
        assertThat(original.getCode()).isEqualTo("OLD");
        assertThat(original.getName()).isEqualTo("Original name");
        assertThat(original.getDescription()).isEqualTo("Original description");
        assertThat(original.isActive()).isTrue();

        processor.processMessage("""
            {
              "header": {"data_product": "ref_data"},
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "code": "NEW",
                    "name": "Updated name",
                    "description": "Updated description",
                    "active": false
                  }
                ]
              }
            }
            """);

        // With no test-level transaction, this reload uses a fresh persistence context after the processor commits.
        RefData updated = repository.findById(1L).orElseThrow();
        assertThat(repository.count()).isEqualTo(1L);
        assertThat(updated).isNotSameAs(original);
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getCode()).isEqualTo("NEW");
        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.isActive()).isFalse();
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = RefData.class)
    @EnableJpaRepositories(basePackageClasses = RefDataRepository.class)
    @Import({RefDataMessageProcessor.class, RefDataHandlerRegistry.class,
        SchemaValidationService.class, RefDataTestHandler.class})
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
