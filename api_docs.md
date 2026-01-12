# 📖 Backend API: Guia de Integração Consolidado

Este documento resume todas as alterações recentes no backend relacionadas à **Segurança (Senhas)**, **Assinaturas (SaaS)** e **Gestão de Créditos**.

---

## 🛠️ 1. Banco de Dados (Migrations)

Certifique-se de que os seguintes scripts SQL foram executados (em ordem):

- **V2 (Reset Senha):** Cria tabela `password_reset_token`.
- **V3 (Assinaturas):** Adiciona `plan_type`, `subscription_status`, `subscription_end_date`, `generations_used_cycle` na tabela `usuario`.
- **V4 (Histórico):** Cria tabela `subscription_history`.
- **V5 (Créditos):** Adiciona `subscription_credits` e `purchased_credits` na tabela `usuario`.

---

## 🔐 2. Segurança e Senhas

### Autenticação BCrypt
- O backend agora utiliza **BCrypt** (`$2a$10$...`).
- **Auto-Migração:** Senhas antigas em texto puro são convertidas automaticamente para hash no primeiro login bem-sucedido.
- **Endpoints:**
    - `POST /api/usuarios/change-password`: Troca de senha logada.
    - `POST /api/usuarios/forgot-password`: Início do fluxo de esquecimento (envia e-mail).
    - `POST /api/usuarios/reset-password`: Redefinição via token do e-mail.
    - `POST /api/usuarios/admin/reset-password/{userId}`: Reset administrativo (suporta também `/api/usuarios/admin-reset-password/{userId}`).

---

## 💳 3. Assinaturas e Planos

### Definição de Planos

| ID | Nome | Gerações/Mês | Créditos Vídeo |
|----|------|--------------|----------------|
| **FREE** | Gratuito | 0 (BLOQUEADO) | 0 |
| **STARTER** | Starter | 10 | 30 |
| **PRO** | Pro | Ilimitado | 80 |
| **STUDIO** | Studio | Ilimitado | 200 |

### Endpoints de Assinatura
- `GET /api/plans`: Lista todos os planos, preços e features.
- `POST /api/subscriptions/subscribe?userId=X`: Ativa ou troca plano.
    - Body: `{ "planId": "PRO" }`
- `POST /api/subscriptions/cancel?userId=X`: Cancela renovação (mantém acesso até o fim do ciclo).
- `POST /api/webhooks/payment-gateway`: Webhook para renovação automática (reset de créditos e contadores).

---

## 🎁 4. Gestão de Créditos e Uso

### Separação de Créditos
Agora existem dois tipos de saldo que somam o total do usuário:
1. **subscriptionCredits**: Créditos recorrentes do plano (resetam todo mês).
2. **purchasedCredits**: Créditos avulsos comprados (nunca expiram).

**Lógica de Consumo:** O sistema debita primeiro do saldo do plano. Se esgotar, debita do saldo avulso.

### Usage Query (`/api/me`)
**Endpoint:** `GET /api/me?userId=X`  
Retorna o perfil completo com dados de uso:

```json
{
  "id": 123,
  "plan": {
    "type": "STARTER",
    "status": "ACTIVE",
    "renewsAt": "2024-02-15T00:00:00"
  },
  "usage": {
    "credits": 45,             // Total (Sub + Pur)
    "subscriptionCredits": 30, // Saldo do plano
    "purchasedCredits": 15,    // Saldo avulso
    "generations": 8,          // Usado no ciclo atual
    "generationsLimit": 10     // Limite do plano
  }
}
```

### Compra de Avulsos
**Endpoint:** `POST /api/credits/purchase?userId=X`
- Body: `{ "amount": 10 }`

---

## 🛡️ 5. Gatekeepers (Bloqueios)

O backend bloqueia automaticamente as seguintes rotas se o limite for excedido ou plano for insuficiente:
- `POST /api/treinos/`
- `POST /api/dietas/`
- `POST /api/usuarios/consume-credit/{userId}` (Valida saldo total > 0)

---

> [!IMPORTANT]
> **Dica para o Frontend:** Utilize o endpoint `GET /api/me` centralmente para gerenciar o estado global de permissões e visibilidade de botões do app.
