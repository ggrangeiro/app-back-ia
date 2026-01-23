package gcfv2.converter;

import gcfv2.dto.anamnese.AnamnesisDTO;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class AnamnesisConverter implements AttributeConverter<AnamnesisDTO, String> {

    private final ObjectMapper objectMapper;

    @Inject
    public AnamnesisConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToPersistedValue(AnamnesisDTO entityValue,
            @NonNull io.micronaut.core.convert.ConversionContext context) {
        if (entityValue == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(entityValue);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao serializar AnamnesisDTO para JSON", e);
        }
    }

    @Override
    public AnamnesisDTO convertToEntityValue(String persistedValue,
            @NonNull io.micronaut.core.convert.ConversionContext context) {
        if (persistedValue == null || persistedValue.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(persistedValue, AnamnesisDTO.class);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao deserializar JSON para AnamnesisDTO", e);
        }
    }
}
