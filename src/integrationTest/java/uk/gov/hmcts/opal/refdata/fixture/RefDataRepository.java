package uk.gov.hmcts.opal.refdata.fixture;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefDataRepository extends JpaRepository<RefData, Long> {
}
