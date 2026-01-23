# Documentação de Integração: Exclusão de Conta

Esta documentação detalha a nova rota implementada para permitir que usuários excluam suas contas permanentemente, em conformidade com as diretrizes da LGPD e App Stores.

## Endpoint

**Rota:** `DELETE /api/usuarios/{id}`

**Autenticação:** Obrigatória (Bearer Token).

## Parâmetros

### URL Path
| Parâmetro | Tipo   | Obrigatório | Descrição |
|-----------|--------|-------------|-----------|
| `id`      | Integer| Sim         | ID do usuário a ser excluído. |

### Query Params (Obrigatórios para validação atual)
A API utiliza os seguintes parâmetros via Query String para validar a permissão (padrão atual do sistema):

| Parâmetro       | Tipo   | Obrigatório | Descrição |
|-----------------|--------|-------------|-----------|
| `requesterId`   | Integer| Sim         | ID do usuário logado que está fazendo a requisição. |
| `requesterRole` | String | Sim         | Role do usuário logado (ex: `USER`, `ADMIN`, `PERSONAL`). |

**Regras de Permissão:**
- O `requesterId` deve ser **igual** ao `id` da URL (o usuário excluindo a si mesmo).
- OU o `requesterId` deve ser o **Personal Trainer atrelado ao usuário** (`personalId` do usuário alvo deve ser igual ao `requesterId`).
- OU o `requesterRole` deve ser `ADMIN` (administrador excluindo qualquer usuário).

## Exemplo de Requisição

```http
DELETE /api/usuarios/123?requesterId=123&requesterRole=USER
Authorization: Bearer <seu_token_jwt>
```

### Exemplo em JavaScript (Fetch)

```javascript
/*
  Exemplo de função para deletar conta.
  Assumindo que você já tem o userId e token armazenados.
*/
async function deleteAccount(userId, token) {
  const url = `https://api.fitai.com/api/usuarios/${userId}?requesterId=${userId}&requesterRole=USER`;
  
  try {
    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        "Authorization": `Bearer ${token}`
      }
    });

    if (response.ok) {
      console.log("Conta excluída com sucesso.");
      // Realizar logout e redirecionar para login
    } else {
      const error = await response.json();
      console.error("Erro ao excluir:", error.message);
      // Exibir feedback ao usuário
    }
  } catch (err) {
    console.error("Erro de rede:", err);
  }
}
```

## Respostas Possíveis

### ✅ 200 OK
Conta excluída com sucesso. Todos os dados (treinos, dietas, histórico) foram removidos.
```json
{
  "message": "Conta excluída com sucesso."
}
```

### ⛔ 403 Forbidden
O usuário tentou excluir uma conta que não é dele (e não é admin).
```json
{
  "message": "Você não tem permissão para excluir este usuário."
}
```

### ❌ 404 Not Found
O ID de usuário informado não existe.
```json
{
  "message": "Usuário não encontrado."
}
```

### ⚠️ 500 Internal Server Error
Erro inesperado no servidor.
```json
{
  "message": "Erro ao excluir conta: [detalhes do erro]"
}
```

## Impacto nos Dados
Ao confirmar a exclusão, os seguintes dados são removidos permanentemente:
- Registro do Usuário
- Todos os Treinos e Dietas (Gerados e Estruturados)
- Histórico de Check-ins e Evolução
- Histórico de Consumo de Créditos
- Transações de Pagamento (Removidas para garantir "Exclusão Total")
- Acesso Imediato revogado
