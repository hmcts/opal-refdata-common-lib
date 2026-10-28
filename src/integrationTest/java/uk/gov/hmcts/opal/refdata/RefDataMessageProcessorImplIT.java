package uk.gov.hmcts.opal.refdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.opal.refdata.fixture.DummyPrisonTestHandler.PRISON_CACHE;
import static uk.gov.hmcts.opal.refdata.fixture.DummyPrisonTestHandler.PRISON_LOOKUP_CACHE;
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
import uk.gov.hmcts.opal.refdata.fixture.DummyPrison;
import uk.gov.hmcts.opal.refdata.fixture.DummyPrisonRepository;
import uk.gov.hmcts.opal.refdata.fixture.DummyPrisonTestHandler;
import uk.gov.hmcts.opal.refdata.schemavalidation.SchemaValidationService;
import uk.gov.hmcts.opal.refdata.schemavalidation.JsonSchemaValidationException;

@SpringBootTest(classes = RefDataMessageProcessorImplIT.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:ref-data-schema.sql",
        "spring.jpa.hibernate.ddl-auto=validate","opal.ref-data.service-bus.consumer-enabled=true"})
@Testcontainers
class RefDataMessageProcessorImplIT {
    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    RefDataMessageProcessor processor;
    @Autowired DummyPrisonRepository repository;
    @Autowired RefDataHandlerRegistry handlerRegistry;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        assertThat(handlerRegistry.find("Prison")).containsInstanceOf(DummyPrisonTestHandler.class);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        repository.deleteAll();
        DummyPrison seed = new DummyPrison();
        seed.setId(1234L);
        seed.setName("Bristol Prison");
        seed.setAddressLine1("1 High Street");
        seed.setPostcode("BS1 1AA");
        repository.save(seed);
    }

    @Test
    void shouldLoadUpdateAndSaveExistingRefData() {
        process(message(prisonRecord("Bristol Prison", "2 Court Road", "BS22BB")));
        DummyPrison updated = repository.findById(1234L).orElseThrow();
        assertThat(updated.getId()).isEqualTo(1234L);
        assertThat(updated.getName()).isEqualTo("Bristol Prison");
        assertThat(updated.getAddressLine1()).isEqualTo("2 Court Road");
    }

    @Test
    void processMessage_clearsAssociatedCachesAndPreservesUnrelatedCaches() {
        for (String name : List.of(PRISON_CACHE, PRISON_LOOKUP_CACHE)) {
            cacheManager.getCache(name).put(1234L, "Original name");
            cacheManager.getCache(name).put(9999L, "Another cached record");
        }
        var unrelated = cacheManager.getCache("unrelated");
        unrelated.put("key", "Retained value");

        process(message(prisonRecord("Bristol Prison", "1 Court Road", "BS11AA")));

        assertThat(cacheManager.getCache(PRISON_CACHE).get(1234L)).isNull();
        assertThat(cacheManager.getCache(PRISON_LOOKUP_CACHE).get(9999L)).isNull();
        assertThat(unrelated.get("key", String.class)).isEqualTo("Retained value");
    }

    @Test
    void shouldCreateDummyPrisonWhenNotFound() {
        repository.deleteAll();
        process(message(prisonRecord("New Prison", "2 Court Road", "BS22BB")));
        DummyPrison created = repository.findByName("New Prison").orElseThrow();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(created.getName()).isEqualTo("New Prison");
        assertThat(created.getAddressLine1()).isEqualTo("2 Court Road");
    }

    @Test
    void processMessage_rollsBackAllRecordsWhenAnyRecordFails() {
        String message = message(
            prisonRecord("Bristol Prison", "1 Court Road", "BS11AA"),
            prisonRecord("X Prison", "1 High Street", "BS11AA"));
        assertThatThrownBy(() -> process(message)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A prison name cannot contain the letter X");
        assertThat(repository.findByName("Bristol Prison").orElseThrow().getName()).isEqualTo("Bristol Prison");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void processMessage_throwsExceptionWhenMessageFailsValidation() {
        assertThatThrownBy(() -> process("""
            {
              "header": {
                "message_id": "00000000-0000-0000-0000-000000000001",
                "data_product": "Prison"
              },
              "payload": {}
            }
            """))
            .isInstanceOf(JsonSchemaValidationException.class).hasMessageContaining("records");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void processMessage_reportsInvalidPrisonPostcodeValidationError() {
        assertThatThrownBy(() -> process(message(
            prisonRecord("Invalid Postcode Prison", "1 Court Road", "INVALID"))))
            .isInstanceOf(JsonSchemaValidationException.class)
            .hasMessageContaining("$.payload.records[0].addresses[0].postcode")
            .hasMessageContaining("does not match the regex pattern");
    }

    @Test
    void processMessage_throwsExceptionWhenMessageIsMalformedJson() {
        assertThatThrownBy(() -> process("{not-json"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unable to parse ref data message");
    }

    @Test
    void processMessage_rejectsPrisonNameContainingX() {
        assertThatThrownBy(() -> process(message(
            prisonRecord("X Prison", "1 Court Road", "BS11AA"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A prison name cannot contain the letter X");
        assertThat(repository.findByName("X Prison")).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void processMessage_throwsValidationErrorWhenRecordsAreEmpty() {
        assertThatThrownBy(() -> process("""
            {
              "header": {
                "message_id": "00000000-0000-0000-0000-000000000001",
                "message_type": "ReferenceData",
                "data_product": "Prison",
                "operation": "PUBLISH",
                "source_system": "Semarchy",
                "created_date_time": "2026-01-01T00:00:00Z",
                "release_package_id": 1,
                "record_count": 1
              },
              "payload": {
                "records": []
              }
            }
            """))
            .isInstanceOf(JsonSchemaValidationException.class)
            .hasMessageContaining("$.payload.records: must have at least 1 items but found 0");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void processMessage_updatesAndCreatesMultipleRecordsSuccessfully() {
        process(message(
            prisonRecord("Bristol Prison", "3 Court Road", "BS22BB"),
            prisonRecord("Created Prison", "4 Court Road", "BS22BB")));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findByName("Bristol Prison").orElseThrow().getName()).isEqualTo("Bristol Prison");
        assertThat(repository.findByName("Created Prison").orElseThrow().getName()).isEqualTo("Created Prison");
    }

    @Test
    void processMessage_ignoresUnknownHandlerLookup() {
        assertThat(handlerRegistry.find("unknown_ref_data")).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
    }

    private void process(String message) {
        processor.processMessage(message);
    }

    private String message(String... records) {
        return """
            {
              "header": {
                "message_id": "00000000-0000-0000-0000-000000000001",
                "message_type": "ReferenceData",
                "data_product": "Prison",
                "operation": "PUBLISH",
                "source_system": "Semarchy",
                "created_date_time": "2026-01-01T00:00:00Z",
                "release_package_id": 1,
                "record_count": %d
              },
              "payload": {
                "records": [%s]
              }
            }
            """.formatted(records.length, String.join(",", records));
    }

    private String prisonRecord(String description, String addressLine1, String postcode) {
        return """
            {
              "name": "%s",
              "start_date": "2026-01-01",
              "publishing_status": "Active",
              "addresses": [
                {
                  "address_type": "PRIS",
                  "address_line_1": "%s",
                  "postcode": "%s"
                }
              ]
            }
            """.formatted(description, addressLine1, postcode);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = DummyPrison.class)
    @EnableJpaRepositories(basePackageClasses = DummyPrisonRepository.class)
    @Import({RefDataRefDataMessageProcessorImpl.class, RefDataMessageDecoder.class, RefDataHandlerRegistry.class,
        SchemaValidationService.class, DummyPrisonTestHandler.class})
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
