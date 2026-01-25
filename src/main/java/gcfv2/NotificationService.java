package gcfv2;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class NotificationService {

    @Inject
    private NotificationRepository notificationRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    public void createNotification(Long studentId, String type, String message) {
        // 1. Encontrar o aluno para pegar o nome e o personalId
        var studentOpt = usuarioRepository.findById(studentId);
        if (studentOpt.isEmpty())
            return;

        var student = studentOpt.get();
        Long personalId = student.getPersonalId();

        if (personalId == null)
            return; // Se não tem personal, ninguém recebe

        String studentName = student.getNome() != null ? student.getNome() : "Aluno";
        long timestamp = System.currentTimeMillis();

        // 2. Criar notificação para o Personal (Manager)
        Notification notePersonal = new Notification(
                personalId,
                studentId,
                studentName,
                type,
                message,
                timestamp);
        notificationRepository.save(notePersonal);

        // 3. Criar notificação para TODOS os professores da equipe deste Personal
        List<Usuario> professors = usuarioRepository.findProfessorsByManagerId(personalId);
        for (Usuario prof : professors) {
            Notification noteProf = new Notification(
                    prof.getId(),
                    studentId,
                    studentName,
                    type,
                    message,
                    timestamp);
            notificationRepository.save(noteProf);
        }
    }

    public List<Notification> listForRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByTimestampDesc(recipientId);
    }

    public void delete(Long id, Long recipientId) {
        var note = notificationRepository.findById(id);
        if (note.isPresent() && note.get().getRecipientId().equals(recipientId)) {
            notificationRepository.deleteById(id);
        }
    }

    public void deleteAllForRecipient(Long recipientId) {
        notificationRepository.deleteByRecipientId(recipientId);
    }

    /**
     * Criar notificação para um ALUNO quando um Personal/Professor fizer algo para
     * ele.
     * Usado para: geração de dieta, treino, análise corporal, etc.
     *
     * @param studentId  - ID do aluno que vai receber a notificação
     * @param senderId   - ID do personal/professor que fez a ação
     * @param senderName - Nome do personal/professor
     * @param type       - Tipo: DIET_GENERATED, WORKOUT_GENERATED, BODY_ANALYSIS
     * @param message    - Mensagem da notificação
     */
    public void createNotificationForStudent(Long studentId, Long senderId, String senderName, String type,
            String message) {
        long timestamp = System.currentTimeMillis();

        // Criar notificação onde recipientId = aluno
        // studentId e studentName serão usados para guardar info do sender (quem
        // criou)
        Notification notification = new Notification(
                studentId, // recipientId = aluno recebe
                senderId, // studentId usado como senderId (quem fez a ação)
                senderName, // studentName usado como senderName
                type,
                message,
                timestamp);
        notificationRepository.save(notification);
    }
}
