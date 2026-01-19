# Admin Email Dispatcher - Frontend Integration

## Overview

Este documento descreve como integrar o **Disparador de E-mails Administrativo** no frontend.

---

## Endpoint

```
POST /api/notifications/admin/send-email
```

### Query Parameters

| Param | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `requesterId` | Long | Sim | ID do usuário logado |
| `requesterRole` | String | Sim | Deve ser `"ADMIN"` |

### Request Body

```typescript
interface AdminEmailRequest {
  targetAudience: 'ALL' | 'PERSONALS' | 'PERSONALS_AND_PROFESSORS' | 'STUDENTS' | 'SPECIFIC';
  specificEmail?: string;  // Obrigatório se targetAudience === 'SPECIFIC'
  subject: string;         // Obrigatório
  body: string;            // Obrigatório (texto ou HTML básico)
}
```

### Response

```typescript
interface AdminEmailResponse {
  success: boolean;
  message: string;
  recipientCount: number;
}
```

---

## Exemplos

### Enviar para todos os usuários

```typescript
const response = await fetch(
  `${API_BASE_URL}/api/notifications/admin/send-email?requesterId=${userId}&requesterRole=ADMIN`,
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      targetAudience: 'ALL',
      subject: 'Atualização Importante',
      body: 'Olá! Temos novidades para você...'
    })
  }
);
```

### Enviar para e-mail específico

```typescript
const response = await fetch(
  `${API_BASE_URL}/api/notifications/admin/send-email?requesterId=${userId}&requesterRole=ADMIN`,
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      targetAudience: 'SPECIFIC',
      specificEmail: 'usuario@exemplo.com',
      subject: 'Mensagem Pessoal',
      body: '<p>Olá! Esta é uma mensagem <strong>personalizada</strong>.</p>'
    })
  }
);
```

---

## Códigos de Resposta

| Status | Cenário |
|--------|---------|
| `200` | Sucesso - e-mails sendo enviados |
| `400` | Campos obrigatórios faltando ou inválidos |
| `403` | Acesso negado (requesterRole ≠ ADMIN) |
| `500` | Erro interno do servidor |

---

## Valores de targetAudience

| Valor | Descrição |
|-------|-----------|
| `ALL` | Todos os usuários cadastrados |
| `PERSONALS` | Apenas Personal Trainers (role = 'personal') |
| `PERSONALS_AND_PROFESSORS` | Personal Trainers + Professores |
| `STUDENTS` | Apenas alunos (role = 'user') |
| `SPECIFIC` | Um e-mail específico (requer `specificEmail`) |

---

## Sugestão de UI (Select)

```tsx
const audienceOptions = [
  { value: 'ALL', label: 'Todos os Usuários' },
  { value: 'PERSONALS', label: 'Apenas Personais' },
  { value: 'PERSONALS_AND_PROFESSORS', label: 'Personais e Professores' },
  { value: 'STUDENTS', label: 'Apenas Alunos' },
  { value: 'SPECIFIC', label: 'E-mail Específico' }
];
```

---

---

## Guia de Uso de HTML no Corpo (Body)

O sistema aceita HTML básico para estilização customizada. As quebras de linha (`\n`) são convertidas automaticamente em `<br>` caso o corpo não contenha tags HTML.

### Tags Suportadas (Seguras)
- `<strong>`, `<b>`, `<i>`, `<em>`
- `<h1>`, `<h2>`, `<h3>`
- `<p>`, `<br>`
- `<ul>`, `<li>`
- `<a>` (Links externos)

### Exemplo de Corpo HTML Rico
```html
<h1>🎉 Novas Funcionalidades Chegaram!</h1>
<p>Olá atleta, temos o prazer de anunciar que o <strong>FitAI</strong> agora conta com:</p>
<ul>
  <li>Análise em tempo real</li>
  <li>Planos personalizados de dieta</li>
</ul>
<p>Confira no app!</p>
```

---

## Exemplo de Serviço TypeScript (apiService.ts)

```typescript
export const notificationService = {
  /**
   * Envia e-mail administrativo em massa ou para destinatário específico.
   */
  sendAdminEmail: async (
    requesterId: number,
    request: AdminEmailRequest
  ): Promise<AdminEmailResponse> => {
    const params = new URLSearchParams({
      requesterId: requesterId.toString(),
      requesterRole: 'ADMIN'
    });

    const response = await fetch(`${API_BASE_URL}/api/notifications/admin/send-email?${params}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // 'Authorization': `Bearer ${token}` // Se aplicável
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Erro ao enviar e-mail');
    }

    return response.json();
  }
};
```

---

## Notas Importantes

- **Segurança**: O backend valida o `requesterRole`. Se não for `ADMIN`, retornará `403 Forbidden`.
- **Desempenho**: O processamento é assíncrono. O servidor responde assim que a lista de destinatários é resolvida, sem esperar o envio final de cada e-mail via API externa.
- **Limites**: Para listas muito grandes (>10.000), o processamento inicial pode levar alguns segundos antes da resposta `200 OK`.
- **Template**: O sistema aplica automaticamente um template "Premium" com o branding da FitAI em volta do seu `body`.

