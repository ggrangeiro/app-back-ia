# Suporte a Imagens em E-mails Administrativos - Documentação Frontend

## Visão Geral

Esta funcionalidade permite que administradores anexem uma imagem de destaque (banner) aos e-mails enviados em massa pela plataforma. A imagem é exibida no topo do e-mail, antes do conteúdo textual.

---

## 1. Upload da Imagem

Antes de enviar o e-mail, a imagem deve ser enviada para o servidor para gerar uma URL pública.

**Endpoint:** `POST /api/usuarios/{id}/upload-asset`

**Requisitos:**
- **Role:** Apenas `ADMIN`
- **Type:** `email_image` (Novo tipo suportado)
- **Formato:** JPG ou PNG
- **Tamanho Máx:** 2MB

**Exemplo de Implementação:**

```typescript
async function uploadEmailImage(adminId: string, file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', 'email_image'); // ⚠️ Importante: usar este tipo exato

  const response = await fetch(
    `/api/usuarios/${adminId}/upload-asset?requesterId=${adminId}&requesterRole=ADMIN`,
    {
      method: 'POST',
      body: formData
    }
  );

  if (!response.ok) {
    throw new Error('Falha no upload da imagem');
  }

  const data = await response.json();
  return data.imageUrl; // Ex: "/api/assets/assets/email_image/uuid.jpg"
}
```

---

## 2. Envio do E-mail

O endpoint de envio de e-mail foi atualizado para aceitar o campo opcional `imageUrl`.

**Endpoint:** `POST /api/notifications/admin/send-email`

**Novo Payload:**

```typescript
interface AdminEmailRequest {
  targetAudience: 'ALL' | 'PERSONALS' | 'STUDENTS' | 'SPECIFIC';
  specificEmail?: string;
  subject: string;
  body: string;
  imageUrl?: string; // 🆕 Campo opcional (URL retornada pelo upload)
}
```

**Exemplo de Implementação:**

```typescript
async function sendBroadcastEmail(adminId: string, payload: AdminEmailRequest) {
  const response = await fetch(
    `/api/notifications/admin/send-email?requesterId=${adminId}&requesterRole=ADMIN`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    }
  );

  const data = await response.json();
  return data;
}
```

---

## 3. Fluxo Completo (Exemplo de Componente)

```typescript
const handleSendEmail = async () => {
  try {
    let finalImageUrl = null;

    // 1. Se houver imagem selecionada, fazer upload primeiro
    if (selectedFile) {
      finalImageUrl = await uploadEmailImage(currentUser.id, selectedFile);
    }

    // 2. Enviar e-mail com a URL da imagem (se houver)
    await sendBroadcastEmail(currentUser.id, {
      targetAudience: 'ALL',
      subject: 'Novidade da Semana',
      body: 'Confira nossos novos recursos...',
      imageUrl: finalImageUrl
    });

    alert('E-mail enviado com sucesso!');
    
  } catch (error) {
    console.error(error);
    alert('Erro ao enviar e-mail');
  }
};
```

## Notas Importantes

1. **Visualização:** A imagem será renderizada centralizada no topo do template de e-mail, com bordas arredondadas e largura máxima de 100%.
2. **Permissões:** Se um usuário não-ADMIN tentar fazer upload com `type=email_image`, receberá um erro `403 Forbidden`.
3. **URL:** O frontend deve enviar a URL **relativa** retornada pelo upload (ex: `/api/assets/...`). O backend automaticamente converte para a URL absoluta correta do servidor antes de enviar o e-mail, garantindo que a imagem carregue corretamente nos clientes de e-mail.
