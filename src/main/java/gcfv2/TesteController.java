package gcfv2;

import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.transaction.annotation.Transactional;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

@Controller("/api/usuarios")
public class TesteController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioExercicioRepository usuarioExercicioRepository;
    private final ExerciseRepository exerciseRepository;

    // Lista de nomes para carga inicial de novos usuários (padrão legado)
    private static final List<String> EXERCICIOS_PADRAO = Arrays.asList(
        "Agachamento (Squat)", "Flexão de Braço (Push-up)", "Afundo (Lunge)", "Burpee",
        "Prancha (Plank)", "Polichinelo (Jumping Jacks)", "Escalador (Mountain Climber)",
        "Abdominal Supra (Crunch)", "Barra Fixa (Pull-up)", "Elevação Pélvica (Glute Bridge)",
        "Agachamento Búlgaro", "Levantamento Terra (Deadlift)", "Tríceps Banco (Dips)",
        "Rosca Direta (Bicep Curl)", "Crucifixo no Cross Over", 
        "Análise de Postura (Biofeedback)", "Análise Corporal (Biotipo & Gordura)"
    );

    // Construtor único para Injeção de Dependência
    public TesteController(UsuarioRepository usuarioRepository, 
                           UsuarioExercicioRepository usuarioExercicioRepository,
                           ExerciseRepository exerciseRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioExercicioRepository = usuarioExercicioRepository;
        this.exerciseRepository = exerciseRepository;
    }

    /**
     * RETORNA O CATÁLOGO GLOBAL DE EXERCÍCIOS (Migrado do Front)
     */
    @CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
    @Get("/exercises")
    public List<Exercise> listarExerciciosDisponiveis() {
        return exerciseRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * LISTA TODOS OS USUÁRIOS
     */
    @CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
    @Get("/")
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.listAll();
    }

    /**
     * LOGIN DE USUÁRIO
     */
    @CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
    @Post("/login")
    public HttpResponse<Usuario> login(@Body Usuario loginRequest) {
        return usuarioRepository.findByEmailAndSenha(loginRequest.getEmail(), loginRequest.getSenha())
                .map(usuario -> HttpResponse.ok(usuario))
                .orElse(HttpResponse.status(HttpStatus.UNAUTHORIZED));
    }

    /**
     * CADASTRO DE NOVO USUÁRIO + CARGA DE EXERCÍCIOS PADRÃO
     */
    @CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
    @Post("/")
    @Transactional
    public HttpResponse<Usuario> cadastrar(@Body Usuario usuario) {
        try {
            // Valores padrão
            usuario.setRole("user");
            if (usuario.getAvatar() == null || usuario.getAvatar().isEmpty()) {
                usuario.setAvatar("https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y");
            }

            // 1. Salva o usuário
            Usuario novoUsuario = usuarioRepository.save(usuario);

            // 2. Atrela os exercícios padrão da lista estática
            for (String nomeExercicio : EXERCICIOS_PADRAO) {
                UsuarioExercicio ue = new UsuarioExercicio();
                ue.setExercicio(nomeExercicio);
                ue.setUsuario(novoUsuario);
                usuarioExercicioRepository.save(ue);
            }

            return HttpResponse.created(novoUsuario);
            
        } catch (Exception e) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * RETORNA OS EXERCÍCIOS ESPECÍFICOS DE UM USUÁRIO
     */
    /**
     * RETORNA OS EXERCÍCIOS ESPECÍFICOS DE UM USUÁRIO
     */
    @CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
    @Get("/{id}/exercicios")
    public HttpResponse<List<UsuarioExercicio>> listarExerciciosPorUsuario(@PathVariable Long id) {
        // O usuarioRepository.findById(id) agora usará o Join que definimos
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    List<UsuarioExercicio> lista = usuario.getAssignedExercisesList();
                    System.out.println("Exercícios encontrados para o user " + id + ": " + (lista != null ? lista.size() : 0));
                    return HttpResponse.ok(lista);
                })
                .orElse(HttpResponse.notFound());
    }
}