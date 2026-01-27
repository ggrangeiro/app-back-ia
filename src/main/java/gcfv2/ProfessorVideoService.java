package gcfv2;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class ProfessorVideoService {

    private final ProfessorExerciseVideoRepository repository;

    public ProfessorVideoService(ProfessorExerciseVideoRepository repository) {
        this.repository = repository;
    }

    public List<ProfessorExerciseVideo> getVideosByProfessor(Long professorId) {
        return repository.findByProfessorId(professorId);
    }

    public ProfessorExerciseVideo saveOrUpdateVideo(Long professorId, String exerciseId, String videoUrl,
            String description) {
        Optional<ProfessorExerciseVideo> existing = repository.findByProfessorIdAndExerciseId(professorId, exerciseId);

        ProfessorExerciseVideo video;
        if (existing.isPresent()) {
            video = existing.get();
            video.setVideoUrl(videoUrl);
            video.setDescription(description);
        } else {
            video = new ProfessorExerciseVideo();
            video.setProfessorId(professorId);
            video.setExerciseId(exerciseId);
            video.setVideoUrl(videoUrl);
            video.setDescription(description);
            video.setVideoType("YOUTUBE"); // Default
        }

        return repository.save(video);
    }

    public void deleteVideo(Long professorId, String exerciseId) {
        Optional<ProfessorExerciseVideo> video = repository.findByProfessorIdAndExerciseId(professorId, exerciseId);
        video.ifPresent(repository::delete);
    }

    public Optional<String> getCustomVideoUrl(Long professorId, String exerciseId) {
        if (professorId == null || exerciseId == null)
            return Optional.empty();
        return repository.findByProfessorIdAndExerciseId(professorId, exerciseId)
                .map(ProfessorExerciseVideo::getVideoUrl);
    }
}
