package uk.gov.hmcts.opal.refdata.fixture;

import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.opal.refdata.RefDataUpdateHandler;
import uk.gov.hmcts.opal.refdata.dto.PrisonRecord;

public class DummyPrisonTestHandler implements RefDataUpdateHandler<PrisonRecord, DummyPrison> {
    public static final String PRISON_CACHE = "prison";
    public static final String PRISON_LOOKUP_CACHE = "prisonLookup";

    private final DummyPrisonRepository repository;

    public DummyPrisonTestHandler(DummyPrisonRepository repository) {
        this.repository = repository;
    }

    public String refDataType() {
        return "Prison";
    }

    public List<String> cachesToClear() {
        return List.of(PRISON_CACHE, PRISON_LOOKUP_CACHE);
    }

    public String getBusinessKey(PrisonRecord dto) {
        return dto.description();
    }

    public Class<PrisonRecord> payloadType() {
        return PrisonRecord.class;
    }

    public void validateDto(PrisonRecord dto) {
        if (dto.description().contains("X")) {
            throw new IllegalArgumentException("A prison name cannot contain the letter X");
        }
    }

    public Optional<DummyPrison> findEntity(PrisonRecord dto) {
        return repository.findByName(dto.description());
    }

    public DummyPrison createEntity(PrisonRecord dto) {
        DummyPrison entity = new DummyPrison();
        entity.setId(prisonId(dto));
        return entity;
    }

    public void updateEntityFromDto(PrisonRecord dto, DummyPrison e) {
        var address = dto.addresses().get(0);
        e.setName(dto.description());
        e.setAddressLine1(address.addressLine1());
        e.setAddressLine2(address.addressLine2());
        e.setAddressLine3(address.addressLine3());
        e.setPostcode(address.postCode());
        e.setAddressLine4(address.addressLine4());
        e.setAddressLine5(address.addressLine5());
    }

    public DummyPrison saveEntity(DummyPrison entity) {
        return repository.save(entity);
    }

    private Long prisonId(PrisonRecord dto) {
        return (long) Math.abs(dto.description().hashCode());
    }
}
