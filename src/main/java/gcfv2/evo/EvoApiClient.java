package gcfv2.evo;

import gcfv2.dto.evo.EvoMemberDTO;
import gcfv2.dto.evo.EvoEmployeeDTO;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.micronaut.json.JsonMapper;

/**
 * Cliente HTTP para comunicação com a API EVO Academia.
 * Documentação: https://w12.evofit.com.br/api/swagger/ui/index
 */
@Singleton
public class EvoApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(EvoApiClient.class);
    private static final String DEFAULT_EVO_BASE_URL = "https://evo-integracao.w12app.com.br";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;

    public EvoApiClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    }

    /**
     * Testa a conexão com as credenciais fornecidas.
     * Usa o endpoint de membros ativos como teste.
     */
    public EvoConnectionResult testConnection(String username, String password, String baseUrl) {
        try {
            String url = getBaseUrl(baseUrl) + "/api/v1/members/basic?take=1";
            HttpRequest request = buildRequest(url, username, password);
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return new EvoConnectionResult(true, "Conexão estabelecida com sucesso!", null);
            } else if (response.statusCode() == 401) {
                return new EvoConnectionResult(false, "Credenciais inválidas", "INVALID_CREDENTIALS");
            } else {
                return new EvoConnectionResult(false, "Erro ao conectar: HTTP " + response.statusCode(), "HTTP_ERROR");
            }
        } catch (Exception e) {
            LOG.error("Erro ao testar conexão EVO", e);
            return new EvoConnectionResult(false, "Erro de conexão: " + e.getMessage(), "CONNECTION_ERROR");
        }
    }

    /**
     * Busca lista de membros do EVO.
     * GET /api/v2/members
     */
    public EvoApiResponse<List<EvoMemberDTO>> getMembers(EvoIntegration integration, int skip, int take) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/members?skip=" + skip + "&take=" + take;
            
            // Adiciona filtro de filial se configurado
            if (integration.getEvoBranchId() != null && !integration.getEvoBranchId().isEmpty()) {
                url += "&idBranch=" + integration.getEvoBranchId();
            }
            
            HttpRequest request = buildRequest(url, integration.getEvoUsername(), integration.getEvoPassword());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                List<EvoMemberDTO> members = parseMembers(response.body());
                return new EvoApiResponse<>(true, members, null);
            } else {
                LOG.error("Erro ao buscar membros EVO: HTTP {}", response.statusCode());
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar membros EVO", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * Busca membros ativos do EVO.
     * GET /api/v1/members/active-members
     */
    public EvoApiResponse<List<EvoMemberDTO>> getActiveMembers(EvoIntegration integration) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v1/members/active-members";
            
            if (integration.getEvoBranchId() != null && !integration.getEvoBranchId().isEmpty()) {
                url += "?idBranch=" + integration.getEvoBranchId();
            }
            
            HttpRequest request = buildRequest(url, integration.getEvoUsername(), integration.getEvoPassword());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                List<EvoMemberDTO> members = parseMembers(response.body());
                return new EvoApiResponse<>(true, members, null);
            } else {
                LOG.error("Erro ao buscar membros ativos EVO: HTTP {}", response.statusCode());
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar membros ativos EVO", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * Busca detalhes de um membro específico.
     * GET /api/v2/members/{idMember}
     */
    public EvoApiResponse<EvoMemberDTO> getMemberById(EvoIntegration integration, Long idMember) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/members/" + idMember;
            
            HttpRequest request = buildRequest(url, integration.getEvoUsername(), integration.getEvoPassword());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                EvoMemberDTO member = jsonMapper.readValue(response.body(), EvoMemberDTO.class);
                return new EvoApiResponse<>(true, member, null);
            } else {
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar membro EVO por ID", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * Busca lista de funcionários do EVO.
     * GET /api/v2/employees
     */
    public EvoApiResponse<List<EvoEmployeeDTO>> getEmployees(EvoIntegration integration) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/employees";
            
            HttpRequest request = buildRequest(url, integration.getEvoUsername(), integration.getEvoPassword());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                List<EvoEmployeeDTO> employees = parseEmployees(response.body());
                return new EvoApiResponse<>(true, employees, null);
            } else {
                LOG.error("Erro ao buscar funcionários EVO: HTTP {}", response.statusCode());
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar funcionários EVO", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    // ========== Métodos de Treino (Workout) ==========

    /**
     * Busca lista de exercícios disponíveis no EVO.
     * GET /api/v2/workout/exercises
     */
    public EvoApiResponse<List<Map<String, Object>>> getExercises(EvoIntegration integration) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/workout/exercises";
            
            HttpRequest request = buildRequest(url, integration.getEvoUsername(), integration.getEvoPassword());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exercises = parseList(response.body());
                return new EvoApiResponse<>(true, exercises, null);
            } else {
                LOG.error("Erro ao buscar exercícios EVO: HTTP {}", response.statusCode());
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar exercícios EVO", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * Cria um novo treino no EVO.
     * POST /api/v2/workout/create-workout
     */
    public EvoApiResponse<Map<String, Object>> createWorkout(EvoIntegration integration, 
                                                              gcfv2.dto.evo.EvoWorkoutDTO workout) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/workout/create-workout";
            String body = jsonMapper.writeValueAsString(workout);
            
            HttpRequest request = buildPostRequest(url, integration.getEvoUsername(), 
                                                    integration.getEvoPassword(), body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = jsonMapper.readValue(response.body(), Map.class);
                LOG.info("Treino criado no EVO com sucesso: {}", result);
                return new EvoApiResponse<>(true, result, null);
            } else {
                String errorBody = response.body();
                LOG.error("Erro ao criar treino EVO: HTTP {} - {}", response.statusCode(), errorBody);
                return new EvoApiResponse<>(false, null, "HTTP " + response.statusCode() + ": " + errorBody);
            }
        } catch (Exception e) {
            LOG.error("Erro ao criar treino EVO", e);
            return new EvoApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * Vincula um treino existente a um membro no EVO.
     * POST /api/v2/workout/link-workout-to-client
     */
    public EvoApiResponse<Boolean> linkWorkoutToMember(EvoIntegration integration, 
                                                        Long idWorkout, Long idMember) {
        try {
            String url = getBaseUrl(integration.getEvoBaseUrl()) + "/api/v2/workout/link-workout-to-client";
            String body = jsonMapper.writeValueAsString(Map.of(
                "idWorkout", idWorkout,
                "idMember", idMember
            ));
            
            HttpRequest request = buildPostRequest(url, integration.getEvoUsername(), 
                                                    integration.getEvoPassword(), body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                LOG.info("Treino {} vinculado ao membro {} no EVO", idWorkout, idMember);
                return new EvoApiResponse<>(true, true, null);
            } else {
                LOG.error("Erro ao vincular treino EVO: HTTP {}", response.statusCode());
                return new EvoApiResponse<>(false, false, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("Erro ao vincular treino EVO", e);
            return new EvoApiResponse<>(false, false, e.getMessage());
        }
    }

    // ========== Métodos Auxiliares ==========

    private String getBaseUrl(String customUrl) {
        return (customUrl != null && !customUrl.isEmpty()) ? customUrl : DEFAULT_EVO_BASE_URL;
    }

    private HttpRequest buildRequest(String url, String username, String password) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT)
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .GET()
            .build();
    }

    private HttpRequest buildPostRequest(String url, String username, String password, String body) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT)
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(String json) {
        try {
            if (json.trim().startsWith("[")) {
                return jsonMapper.readValue(json, List.class);
            } else {
                Map<String, Object> wrapper = jsonMapper.readValue(json, Map.class);
                if (wrapper.containsKey("exercises")) {
                    return (List<Map<String, Object>>) wrapper.get("exercises");
                } else if (wrapper.containsKey("data")) {
                    return (List<Map<String, Object>>) wrapper.get("data");
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.error("Erro ao parsear lista EVO: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<EvoMemberDTO> parseMembers(String json) {
        try {
            // A API EVO pode retornar um array ou um objeto com array
            if (json.trim().startsWith("[")) {
                return jsonMapper.readValue(json, 
                    jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoMemberDTO.class));
            } else {
                // Tenta extrair array de um objeto wrapper
                @SuppressWarnings("unchecked")
                Map<String, Object> wrapper = jsonMapper.readValue(json, Map.class);
                if (wrapper.containsKey("members")) {
                    String membersJson = jsonMapper.writeValueAsString(wrapper.get("members"));
                    return jsonMapper.readValue(membersJson,
                        jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoMemberDTO.class));
                } else if (wrapper.containsKey("data")) {
                    String dataJson = jsonMapper.writeValueAsString(wrapper.get("data"));
                    return jsonMapper.readValue(dataJson,
                        jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoMemberDTO.class));
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.error("Erro ao parsear membros EVO: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<EvoEmployeeDTO> parseEmployees(String json) {
        try {
            if (json.trim().startsWith("[")) {
                return jsonMapper.readValue(json,
                    jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoEmployeeDTO.class));
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> wrapper = jsonMapper.readValue(json, Map.class);
                if (wrapper.containsKey("employees")) {
                    String employeesJson = jsonMapper.writeValueAsString(wrapper.get("employees"));
                    return jsonMapper.readValue(employeesJson,
                        jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoEmployeeDTO.class));
                } else if (wrapper.containsKey("data")) {
                    String dataJson = jsonMapper.writeValueAsString(wrapper.get("data"));
                    return jsonMapper.readValue(dataJson,
                        jsonMapper.getTypeFactory().constructCollectionType(List.class, EvoEmployeeDTO.class));
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.error("Erro ao parsear funcionários EVO: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== Classes de Resposta ==========

    @Serdeable
    public static class EvoConnectionResult {
        private final boolean success;
        private final String message;
        private final String errorCode;

        public EvoConnectionResult(boolean success, String message, String errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
    }

    @Serdeable
    public static class EvoApiResponse<T> {
        private final boolean success;
        private final T data;
        private final String error;

        public EvoApiResponse(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getError() { return error; }
    }
}
