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
        if (student.getPersonalId() == null)
            return; // Se não tem personal, ninguém recebe

        // 2. Criar notificação para o Personal
        Notification note = new Notification(
                student.getPersonalId(),
                studentId,
                student.getNome() != null ? student.getNome() : "Aluno",
                type,
                message,
                System.currentTimeMillis());

        notificationRepository.save(note);
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
}
