package uk.gov.hmcts.opal.refdata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Discovers and indexes the handlers supplied by the consuming application.
 * A handler is selected by the data-product type in an incoming message.
 */
@Slf4j(topic = "opal.RefDataHandlerRegistry")
@Component
public class RefDataHandlerRegistry {

    /**
     * Handlers indexed by their incoming message data-product type. The
     * wildcard type parameters allow this registry to hold handlers for
     * different DTO and entity types; the concrete types are used by each
     * handler when processing its own ref-data records.
     */
    private final Map<String, RefDataUpdateHandler<?, ?>> handlersByType;

    public RefDataHandlerRegistry(List<RefDataUpdateHandler<?, ?>> handlers) {
        this.handlersByType = handlers.stream()
            .collect(Collectors.toMap(
                RefDataUpdateHandler::refDataType,
                Function.identity()
            ));
        log.info("Registered {} ref-data handler(s): {}", handlersByType.size(),
            handlersByType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " -> " + entry.getValue().getClass().getName())
                .collect(Collectors.joining(", ")));
    }

    public Optional<RefDataUpdateHandler<?, ?>> find(String refDataType) {
        if (refDataType == null || refDataType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlersByType.get(refDataType));
    }
}
