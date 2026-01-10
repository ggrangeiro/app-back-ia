package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Controller("/api/usuarios")
public class TesteController {

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private ExerciseRepository exerciseRepository;

    @Inject
    private UsuarioExercicioRepository usuarioExercicioRepository;

    @Inject
    private TreinoRepository treinoRepository;

    /**
     * CADASTRO DE USUÁRIO
     */
    @Post("/")
    @Transactional
    public HttpResponse<?> cadastrar(
            @Body Usuario usuario,
            @Nullable @QueryValue Long requesterId,
            @Nullable @QueryValue String requesterRole) {
        try {
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                return HttpResponse.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Este e-mail já está em uso."));
            }

            if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                usuario.setRole("USER");
                usuario.setPersonalId(requesterId);
            } else if ("ADMIN".equalsIgnoreCase(requesterRole)) {
                if (usuario.getRole() == null)
                    usuario.setRole("USER");
            } else {
                usuario.setRole("USER");
            }

            usuario.setCredits(10);
            Usuario novoUsuario = usuarioRepository.save(usuario);

            List<Exercise> exerciciosCatalogo = exerciseRepository.findByActiveTrueOrderByNameAsc();
            Set<String> processedExercises = new HashSet<>();
            for (Exercise ex : exerciciosCatalogo) {
                if (ex.getName() != null && !processedExercises.contains(ex.getName())) {
                    UsuarioExercicio ue = new UsuarioExercicio();
                    ue.setExercicio(ex.getName());
                    ue.setUsuario(novoUsuario);
                    usuarioExercicioRepository.save(ue);
                    processedExercises.add(ex.getName());
                }
            }

            return HttpResponse.created(novoUsuario);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao cadastrar: " + e.getMessage()));
        }
    }

    /**
     * CONSUMIR CRÉDITO (-1)
     */
    @Post("/consume-credit/{userId}")
    @Transactional
    public HttpResponse<?> consumirCredito(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            int saldoAtual = (user.getCredits() != null) ? user.getCredits() : 0;

            if (saldoAtual <= 0) {
                return HttpResponse.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of("message", "Saldo insuficiente."));
            }

            // Update atômico: altera APENAS a coluna credits
            usuarioRepository.executeConsumeCredit(userId);

            return HttpResponse.ok(Map.of(
                    "message", "Crédito debitado com sucesso",
                    "novoSaldo", saldoAtual - 1));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * RECARGA DE CRÉDITOS (ADMIN APENAS)
     * CORREÇÃO: Removido o uso de .save() para evitar erro de Duplicate Entry no
     * Email.
     */
    @Post("/admin/add-credits/{userId}")
    @Transactional
    public HttpResponse<?> addCredits(
            @PathVariable Long userId,
            @Body Map<String, Integer> body,
            @QueryValue String requesterRole) {

        if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Apenas administradores."));
        }

        Integer amount = body.get("amount");
        if (amount == null || amount <= 0) {
            return HttpResponse.badRequest(Map.of("message", "Quantidade inválida."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            // CORREÇÃO: Usando a query direta para não disparar validação de e-mail
            // duplicado
            usuarioRepository.executeAddCredits(userId, amount);

            int novoSaldo = (user.getCredits() != null ? user.getCredits() : 0) + amount;

            return HttpResponse.ok(Map.of(
                    "message", "Recarga realizada com sucesso",
                    "novoSaldo", novoSaldo));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * LOGIN
     */
    @Post("/login")
    public HttpResponse<?> login(@Body Map<String, String> credentials) {
        String email = credentials.get("email");
        String senha = credentials.get("senha");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent() && usuarioOpt.get().getSenha().equals(senha)) {
            Usuario usuario = usuarioOpt.get();
            List<Treino> treinos = treinoRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId().toString());
            if (!treinos.isEmpty()) {
                usuario.setLatestWorkout(treinos.get(0));
            }
            return HttpResponse.ok(usuario);
        }
        return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciais inválidas."));
    }

    /**
     * LISTAGEM DE USUÁRIOS
     */
    @Get("/")
    public HttpResponse<?> listar(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if ("ADMIN".equalsIgnoreCase(requesterRole))
            return HttpResponse.ok(usuarioRepository.findAll());
        if ("PERSONAL".equalsIgnoreCase(requesterRole))
            return HttpResponse.ok(usuarioRepository.findByPersonalId(requesterId));
        return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Sem permissão."));
    }

    /**
     * LISTAR EXERCÍCIOS DO USUÁRIO
     */
    @Get("/exercises/{userId}")
    public HttpResponse<?> listarExercicios(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            List<UsuarioExercicio> lista = usuarioExercicioRepository.findByUsuario(user);
            List<Map<String, Object>> formatado = lista.stream().map(ue -> {
                String nomeBruto = ue.getExercicio();
                String nomeFormatado = (nomeBruto != null) ? nomeBruto.replace("_", " ").toLowerCase() : "Exercício";
                return Map.<String, Object>of(
                        "id", ue.getId(),
                        "exercicio", nomeBruto != null ? nomeBruto : "",
                        "nomeExibicao", nomeFormatado.substring(0, 1).toUpperCase() + nomeFormatado.substring(1));
            }).toList();
            return HttpResponse.ok(formatado);
        }).orElse(HttpResponse.notFound());
    }
}