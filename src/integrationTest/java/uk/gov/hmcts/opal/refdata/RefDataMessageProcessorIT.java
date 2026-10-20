package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.opal.refdata.fixture.RefDataTestHandler.REF_DATA_CACHE;
import static uk.gov.hmcts.opal.refdata.fixture.RefDataTestHandler.REF_DATA_LOOKUP_CACHE;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import uk.gov.hmcts.opal.refdata.fixture.RefData;
import uk.gov.hmcts.opal.refdata.fixture.RefDataRepository;
import uk.gov.hmcts.opal.refdata.fixture.RefDataTestHandler;
import uk.gov.hmcts.opal.refdata.schemavalidation.JsonSchemaValidationException;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;

@SpringBootTest(
    classes = RefDataMessageProcessorIT.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:ref-data-schema.sql",
        "spring.jpa.hibernate.ddl-auto=validate"
    }
)
@Testcontainers
class RefDataMessageProcessorIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MessageProcessor processor;

    @Autowired
    private RefDataMessageDecoder decoder;

    @Autowired
    private RefDataRepository repository;

    @Autowired
    private RefDataHandlerRegistry handlerRegistry;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        assertThat(handlerRegistry.find("ref_data")).containsInstanceOf(RefDataTestHandler.class);

        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        repository.deleteAll();
        RefData seed = new RefData();
        seed.setId(1L);
        seed.setCode("OLD");
        seed.setName("Original name");
        seed.setDescription("Original description");
        seed.setActive(true);
        repository.save(seed);
    }

    @Test
    void shouldLoadUpdateAndSaveExistingRefData() {
        RefData original = repository.findById(1L).orElseThrow();
        assertThat(original.getCode()).isEqualTo("OLD");
        assertThat(original.getName()).isEqualTo("Original name");
        assertThat(original.getDescription()).isEqualTo("Original description");
        assertThat(original.isActive()).isTrue();

        process("""
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "ref_data", "release_package_id": 1},
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

    @Test
    void processMessage_clearsAllAssociatedCachesAndPreservesUnrelatedCaches() {
        for (String cacheName : List.of(REF_DATA_CACHE, REF_DATA_LOOKUP_CACHE)) {
            var cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            cache.put(1L, "Original name");
            cache.put(2L, "Another cached record");
            assertThat(cache.get(1L, String.class)).isEqualTo("Original name");
            assertThat(cache.get(2L, String.class)).isEqualTo("Another cached record");
        }
        var unrelatedCache = cacheManager.getCache("unrelated");
        assertThat(unrelatedCache).isNotNull();
        unrelatedCache.put("key", "Retained value");

        process("""
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "ref_data", "release_package_id": 1},
              "payload": {
                "records": [{
                  "id": 1,
                  "code": "NEW",
                  "name": "Updated name",
                  "description": "Updated description",
                  "active": false
                }]
              }
            }
            """);

        assertThat(repository.findById(1L).orElseThrow().getName()).isEqualTo("Updated name");
        for (String cacheName : List.of(REF_DATA_CACHE, REF_DATA_LOOKUP_CACHE)) {
            var cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            assertThat(cache.get(1L)).isNull();
            assertThat(cache.get(2L)).isNull();
        }
        assertThat(unrelatedCache.get("key", String.class)).isEqualTo("Retained value");
    }

    @Test
    void shouldCreateRefDataWhenNotFound() {
        assertThat(repository.findById(2L)).isEmpty();

        process("""
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "ref_data", "release_package_id": 1},
              "payload": {
                "records": [
                  {
                    "id": 2,
                    "code": "CREATED",
                    "name": "New name",
                    "description": "New description",
                    "active": true
                  }
                ]
              }
            }
            """);

        RefData created = repository.findById(2L).orElseThrow();
        assertThat(repository.count()).isEqualTo(2L);
        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getCode()).isEqualTo("CREATED");
        assertThat(created.getName()).isEqualTo("New name");
        assertThat(created.getDescription()).isEqualTo("New description");
        assertThat(created.isActive()).isTrue();

        RefData existing = repository.findById(1L).orElseThrow();
        assertThat(existing.getCode()).isEqualTo("OLD");
        assertThat(existing.getName()).isEqualTo("Original name");
        assertThat(existing.getDescription()).isEqualTo("Original description");
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void processMessage_rollsBackAllRecordsWhenAnyRecordFails() {
        // The blank code passes JSON schema validation but fails handler validation after the valid records.
        String message = """
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "ref_data", "release_package_id": 1},
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "code": "UPDATED",
                    "name": "Updated name",
                    "description": "Updated description",
                    "active": false
                  },
                  {
                    "id": 2,
                    "code": "CREATED",
                    "name": "New name",
                    "description": "New description",
                    "active": true
                  },
                  {
                    "id": 3,
                    "code": "",
                    "name": "Invalid record",
                    "description": "Missing code",
                    "active": true
                  }
                ]
              }
            }
            """;

        assertThatThrownBy(() -> process(message))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("An id and code are required");

        // Reload after the processor transaction has rolled back; the test has no enclosing transaction.
        RefData existing = repository.findById(1L).orElseThrow();
        assertThat(existing.getCode()).isEqualTo("OLD");
        assertThat(existing.getName()).isEqualTo("Original name");
        assertThat(existing.getDescription()).isEqualTo("Original description");
        assertThat(existing.isActive()).isTrue();
        assertThat(repository.findById(2L)).isEmpty();
        assertThat(repository.findById(3L)).isEmpty();
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void processMessage_throwsExceptionWhenMessageFailsValidation() {
        // The message is valid JSON but omits the required payload.records property.
        String message = """
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "ref_data"},
              "payload": {}
            }
            """;

        assertThatThrownBy(() -> process(message))
            .isInstanceOf(JsonSchemaValidationException.class)
            .hasMessageContaining("ref-data/ref_data_schema.json")
            .hasMessageContaining("records");

        assertThat(repository.count()).isEqualTo(1L);
        RefData existing = repository.findById(1L).orElseThrow();
        assertThat(existing.getCode()).isEqualTo("OLD");
        assertThat(existing.getName()).isEqualTo("Original name");
        assertThat(existing.getDescription()).isEqualTo("Original description");
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void processMessage_ignoresMessageWhenNoHandlerIsFound() {
        assertThat(handlerRegistry.find("unknown_ref_data")).isEmpty();

        String message = """
            {
              "header": {"message_id": "00000000-0000-0000-0000-000000000001", "data_product": "unknown_ref_data", "release_package_id": 1},
              "payload": {
                "records": [
                  {
                    "id": 1,
                    "code": "UPDATED",
                    "name": "Updated name",
                    "description": "Updated description",
                    "active": false
                  },
                  {
                    "id": 2,
                    "code": "CREATED",
                    "name": "New name",
                    "description": "New description",
                    "active": true
                  }
                ]
              }
            }
            """;

        assertThatCode(() -> process(message)).doesNotThrowAnyException();

        assertThat(repository.count()).isEqualTo(1L);
        RefData existing = repository.findById(1L).orElseThrow();
        assertThat(existing.getCode()).isEqualTo("OLD");
        assertThat(existing.getName()).isEqualTo("Original name");
        assertThat(existing.getDescription()).isEqualTo("Original description");
        assertThat(existing.isActive()).isTrue();
        assertThat(repository.findById(2L)).isEmpty();
    }

    private void process(String payload) {
        decoder.decode(payload).ifPresent(processor::processMessage);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = RefData.class)
    @EnableJpaRepositories(basePackageClasses = RefDataRepository.class)
    @Import({RefDataMessageProcessor.class, RefDataMessageDecoder.class, RefDataHandlerRegistry.class,
        SchemaValidationService.class, RefDataTestHandler.class})
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
