package uk.gov.hmcts.opal.refdata;

import java.util.List;
import java.util.Optional;

/**
 * Defines how a consuming service applies one ref-data type to its database.
 *
 * <p>The consuming service should provide one Spring-managed implementation
 * of this interface for each ref-data type it wants to handle. The
 * {@link #refDataType()} value must match the {@code data_product} value in
 * incoming messages. The implementation supplies the DTO type used to decode
 * records, locates or creates the corresponding entity, applies the DTO to
 * that entity, and saves it.</p>
 *
 * @param <T> the DTO type decoded from a ref-data record
 * @param <E> the database entity type updated by the handler
 */
public interface RefDataUpdateHandler<T, E> {

    /**
     * Returns the incoming message data-product value handled by this
     * implementation.
     *
     * @return the ref-data type handled by this implementation
     */
    String refDataType();

    /**
     * Returns the DTO class used when converting an incoming record.
     *
     * @return the DTO class for this ref-data type
     */
    Class<T> payloadType();

    /**
     * Validates a decoded DTO before it is applied to an entity.
     * Implementations should throw a runtime exception when validation fails.
     *
     * @param dto the decoded ref-data record
     */
    void validateDto(T dto);

    /**
     * Finds the existing entity represented by the DTO.
     *
     * @param dto the decoded ref-data record
     * @return the matching entity, when one already exists
     */
    Optional<E> findEntity(T dto);

    /**
     * Creates a new entity for a DTO that has no existing match.
     * It should copy the DTO values onto the new entity.
     *
     * @param dto the decoded ref-data record
     * @return a new entity ready to receive the DTO values
     */
    E createEntityFromDto(T dto);

    /**
     * Copies the DTO values onto the entity.
     *
     * @param dto the decoded ref-data record
     * @param entity the entity to update
     */
    void updateEntityFromDto(T dto, E entity);

    /**
     * Persists the updated entity.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    E saveEntity(E entity);

    /**
     * Lists Spring caches by name whose contents can be affected by updates from this
     * handler. They are cleared after the surrounding transaction commits.
     *
     * @return cache names to clear, or an empty list when no cache is affected
     */
    default List<String> cachesToClear() {
        return List.of();
    }

    /**
     * Returns the business key used in processing logs to identify a record.
     *
     * @param dto the decoded ref-data record
     * @return a human-readable business key for the record
     */
    String getBusinessKey(T dto);
}
