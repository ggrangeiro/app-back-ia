# Documentação: Endpoint de Status do Usuário (Plano e Créditos)

**Objetivo:** Permitir que o frontend (Web e Mobile) atualize os dados de plano e consumo de créditos do usuário em tempo real, sem a necessidade de realizar um novo login.

Este endpoint é essencial para casos como:
1.  **Pós-Compra de Créditos:** O usuário compra créditos e o app precisa refletir o novo saldo imediatamente.
2.  **Renovação de Assinatura:** O status da assinatura mudou e o app precisa liberar o acesso.
3.  **Consumo de Créditos:** Após gerar um treino/dieta, o app quer mostrar o saldo atualizado.

---

## 🔗 Endpoint

**GET** `/api/usuarios/status`

### Parâmetros (Query Params)

| Parâmetro | Tipo | Obrigatório | Descrição |
| :--- | :--- | :--- | :--- |
| `requesterId` | `Long` | **Sim** | O ID do usuário logado (ou do usuário que se deseja consultar). |

---

## 📥 Resposta (JSON)

A resposta segue **estritamente a mesma estrutura** dos objetos `plan` e `usage` retornados no endpoint de **Login**.

### Exemplo de Sucesso (`200 OK`)

```json
{
  "id": 123,
  "role": "PERSONAL", // ou "PROFESSOR", "USER"
  "accessLevel": "FULL",

  // 📦 INFORMAÇÕES DO PLANO ATUAL
  "plan": {
    "type": "STARTER", // Tipos: FREE, STARTER, PRO, STUDIO
    "status": "ACTIVE", // Status: ACTIVE, INACTIVE, CANCELED
    "renewsAt": "2024-12-30T15:30:00" // Data da próxima renovação/expiração
  },

  // 📊 CONSUMO E CRÉDITOS
  "usage": {
    "credits": 55, // 💰 Saldo Total Disponível (subscription + purchased)
    
    // Detalhamento do saldo
    "subscriptionCredits": 50, // Créditos recorrentes do plano
    "purchasedCredits": 5, // Créditos avulsos comprados
    
    "generations": 8, // Quantidade de treinos/dietas gerados no ciclo atual
    "generationsLimit": 10 // Limite do plano (-1 para ilimitado, 0 para Free)
  }
}
```

---

## 🧠 Comportamentos Importantes

### 1. Professores (Users com role `PROFESSOR`)
O backend aplica automaticamente a **herança de plano**:
*   Ao consultar o status de um **Professor**, a API busca internamente o plano e os créditos do **Personal Trainer (Manager)** responsável por ele.
*   O objeto `usage` retornado refletirá o saldo **do Personal**, pois é este saldo que o professor consome ao atuar.
*   **Ação para o Front:** Nenhuma lógica extra necessária. Apenas exiba os dados retornados.

### 2. Alunos (Users com role `USER`)
*   Retorna o plano e créditos do próprio aluno (se houver lógica de créditos para alunos no futuro) ou os padrões do plano FREE.

### 3. Personal Trainers (Users com role `PERSONAL`)
*   Retorna os dados diretos da conta do personal.

---

## 💡 Exemplo de Integração (Frontend/Mobile)

**Cenário:** Usuário acabou de comprar um pacote de créditos via Pix.

1.  O App recebe o callback de sucesso do pagamento (ou o usuário clica em "Já paguei").
2.  **Imediatamente**, o App chama:
    `GET [BASE_URL]/api/usuarios/status?requesterId=USER_ID`
3.  O App recebe o JSON atualizado.
4.  O App atualiza o estado local (Redux, Context, Store) substituindo os objetos `plan` e `usage`.
5.  A UI de "Saldo" atualiza de "0" para "50" instantaneamente.
