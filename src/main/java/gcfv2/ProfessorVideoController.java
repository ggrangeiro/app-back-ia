package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Controller("/api/professors/videos")
public class ProfessorVideoController {

    private final ProfessorVideoService service;

    public ProfessorVideoController(ProfessorVideoService service) {
        this.service = service;
    }

    @Get("/{professorId}")
    public List<ProfessorExerciseVideo> listVideos(Long professorId) {
        return service.getVideosByProfessor(professorId);
    }

    @Post("/{professorId}")
    public HttpResponse<ProfessorExerciseVideo> saveVideo(Long professorId, @Body VideoRequest request) {
        ProfessorExerciseVideo saved = service.saveOrUpdateVideo(professorId, request.exerciseId(), request.videoUrl(),
                request.description());
        return HttpResponse.ok(saved);
    }

    @Delete("/{professorId}/{exerciseId}")
    public HttpResponse<?> deleteVideo(Long professorId, String exerciseId) {
        service.deleteVideo(professorId, exerciseId);
        return HttpResponse.noContent();
    }

    @Serdeable
    public record VideoRequest(String exerciseId, String videoUrl, String description) {
    }
}
