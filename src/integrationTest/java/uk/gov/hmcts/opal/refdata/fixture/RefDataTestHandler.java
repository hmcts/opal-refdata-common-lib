package uk.gov.hmcts.opal.refdata.fixture;

import java.util.Optional;
import uk.gov.hmcts.opal.refdata.framework.RefDataUpdateHandler;

public class RefDataTestHandler implements RefDataUpdateHandler<RefDataPayload, RefData> {

    private final RefDataRepository repository;

    public RefDataTestHandler(RefDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public String refDataType() {
        return "ref_data";
    }

    @Override
    public Class<RefDataPayload> payloadType() {
        return RefDataPayload.class;
    }

    @Override
    public void validateDto(RefDataPayload dto) {
        if (dto.id() == null || dto.code() == null || dto.code().isBlank()) {
            throw new IllegalArgumentException("An id and code are required");
        }
    }

    @Override
    public Optional<RefData> findEntity(RefDataPayload dto) {
        return repository.findById(dto.id());
    }

    @Override
    public RefData createEntity(RefDataPayload dto) {
        RefData entity = new RefData();
        entity.setId(dto.id());
        return entity;
    }

    @Override
    public void updateEntityFromDto(RefDataPayload dto, RefData entity) {
        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setActive(dto.active());
    }

    @Override
    public RefData saveEntity(RefData entity) {
        return repository.save(entity);
    }
}
