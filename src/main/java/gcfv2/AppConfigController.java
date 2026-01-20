package gcfv2;

import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;

import java.util.Map;

@Controller("/api/config")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
                "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
                "https://fitanalizer.com.br",
                "http://localhost:3000",
                "http://localhost:5173" })
public class AppConfigController {

        @Inject
        private AppConfigRepository appConfigRepository;

        @Inject
        private UsuarioRepository usuarioRepository;

        @Inject
        private Environment environment;

        // Mapeamento de chaves de config para propriedades do application.yml
        private static final Map<String, String> CONFIG_KEY_TO_PROPERTY = Map.of(
                        "MP_PUBLIC_KEY", "mercadopago.public-key",
                        "GEMINI_API_KEY", "gemini.api-key");

        /**
         * GET /api/config/{key}
         * Busca uma configuração pela chave.
         * Primeiro tenta o banco de dados, depois fallback para application.yml.
         * Requer autenticação via query params.
         */
        @Get("/{key}")
        public HttpResponse<?> getConfig(
                        @PathVariable String key,
                        @QueryValue Long requesterId,
                        @QueryValue String requesterRole) {

                // Verificar se o usuário existe
                if (usuarioRepository.findById(requesterId).isEmpty()) {
                        return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "UNAUTHORIZED", "message", "Usuário não encontrado"));
                }

                // 1. Tentar buscar do banco de dados primeiro
                var dbConfig = appConfigRepository.findByConfigKey(key);
                if (dbConfig.isPresent()) {
                        return HttpResponse.ok(Map.of(
                                        "key", dbConfig.get().getConfigKey(),
                                        "value", dbConfig.get().getConfigValue()));
                }

                // 2. Fallback: buscar do application.yml se a chave estiver no mapeamento
                String propertyPath = CONFIG_KEY_TO_PROPERTY.get(key);
                if (propertyPath != null) {
                        String envValue = environment.getProperty(propertyPath, String.class).orElse(null);
                        if (envValue != null && !envValue.isBlank()) {
                                return HttpResponse.ok(Map.of(
                                                "key", key,
                                                "value", envValue));
                        }
                }

                // 3. Não encontrado em nenhum lugar
                return HttpResponse.notFound(Map.of(
                                "error", "CONFIG_NOT_FOUND",
                                "message", "Configuração não encontrada"));
        }

        /**
         * PUT /api/config/{key}
         * Atualiza uma configuração.
         * Apenas ADMIN pode atualizar.
         */
        @Put("/{key}")
        @Transactional
        public HttpResponse<?> updateConfig(
                        @PathVariable String key,
                        @Body Map<String, String> body,
                        @QueryValue Long requesterId,
                        @QueryValue String requesterRole) {

                // Verificar se é ADMIN
                if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                        return HttpResponse.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error", "FORBIDDEN",
                                                        "message",
                                                        "Apenas administradores podem atualizar configurações"));
                }

                String newValue = body.get("value");
                if (newValue == null || newValue.isBlank()) {
                        return HttpResponse.badRequest(Map.of(
                                        "error", "INVALID_VALUE",
                                        "message", "O valor não pode ser vazio"));
                }

                return appConfigRepository.findByConfigKey(key)
                                .map(config -> {
                                        appConfigRepository.updateConfigValue(key, newValue);

                                        // Buscar novamente para retornar o valor atualizado
                                        return appConfigRepository.findByConfigKey(key)
                                                        .map(updated -> HttpResponse.ok(Map.of(
                                                                        "key", updated.getConfigKey(),
                                                                        "value", updated.getConfigValue(),
                                                                        "updatedAt",
                                                                        updated.getUpdatedAt() != null
                                                                                        ? updated.getUpdatedAt()
                                                                                                        .toString()
                                                                                        : "")))
                                                        .orElse(HttpResponse.ok(Map.of(
                                                                        "key", key,
                                                                        "value", newValue,
                                                                        "message", "Configuração atualizada")));
                                })
                                .orElse(HttpResponse.notFound(Map.of(
                                                "error", "CONFIG_NOT_FOUND",
                                                "message", "Configuração não encontrada")));
        }

        /**
         * POST /api/config
         * Cria uma nova configuração.
         * Apenas ADMIN pode criar.
         */
        @Post("/")
        @Transactional
        public HttpResponse<?> createConfig(
                        @Body Map<String, Object> body,
                        @QueryValue Long requesterId,
                        @QueryValue String requesterRole) {

                // Verificar se é ADMIN
                if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                        return HttpResponse.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error", "FORBIDDEN",
                                                        "message", "Apenas administradores podem criar configurações"));
                }

                String key = (String) body.get("key");
                String value = (String) body.get("value");
                String description = (String) body.getOrDefault("description", "");
                Boolean isSensitive = (Boolean) body.getOrDefault("isSensitive", false);

                if (key == null || key.isBlank() || value == null || value.isBlank()) {
                        return HttpResponse.badRequest(Map.of(
                                        "error", "INVALID_INPUT",
                                        "message", "Key e value são obrigatórios"));
                }

                // Verificar se já existe
                if (appConfigRepository.findByConfigKey(key).isPresent()) {
                        return HttpResponse.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "ALREADY_EXISTS",
                                                        "message", "Configuração já existe"));
                }

                AppConfig config = new AppConfig(key, value, description, isSensitive);
                AppConfig saved = appConfigRepository.save(config);

                return HttpResponse.created(Map.of(
                                "key", saved.getConfigKey(),
                                "value", saved.getConfigValue(),
                                "message", "Configuração criada com sucesso"));
        }
}
