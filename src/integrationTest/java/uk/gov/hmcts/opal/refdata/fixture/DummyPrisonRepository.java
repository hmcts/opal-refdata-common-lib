package uk.gov.hmcts.opal.refdata.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DummyPrisonRepository extends JpaRepository<DummyPrison, Long> {
    Optional<DummyPrison> findByName(String name);
}
