# Documentação de Atualizações do Backend (Frontend Spec)
**Data:** 17/01/2026
**Status:** Implementado no ambiente de dev/staging

Esta documentação detalha as correções e novas implementações realizadas no Backend que impactam a integração com o Frontend.

---

## 1. Correção no Update de Usuário (Erro 400/403)

Havia um problema ao tentar atualizar o nível de acesso do aluno, causado por campos extras no JSON e falta de parâmetros na URL.

### Endpoint: `PUT /api/usuarios/{id}`

-   **Mudança 1 (Payload JSON):**
    -   O backend agora **aceita e ignora** o campo `assignedExercises` (ou qualquer outro campo desconhecido).
    -   **Motivo:** Evitar erro 400 quando o frontend envia o objeto completo do usuário de volta.
    -   **Ação Frontend:** Não é necessário remover `assignedExercises` do payload. Pode enviar o objeto como está.

-   **Mudança 2 (Query Params Obrigatórios):**
    -   **CRÍTICO:** A rota exige `requesterId` e `requesterRole` na URL para validar permissão.
    -   **Comportamento:** Se faltarem, o backend retorna **403 Forbidden** com a mensagem `"requesterId e requesterRole são obrigatórios"`.
    -   **Ação Frontend:** Certifique-se de passar os parâmetros na query string.

    **Exemplo Correto:**
    ```javascript
    // PUT /api/usuarios/32?requesterId=5&requesterRole=ADMIN
    await api.put(`/usuarios/${id}`, {
        accessLevel: "READONLY",
        // outros campos...
    }, {
        params: {
            requesterId: loggedUserId,
            requesterRole: loggedUserRole
        }
    });
    ```

---

## 2. Exposição do Nível de Acesso (`accessLevel`)

Para permitir que a interface bloqueie/libere botões de acordo com a permissão, o campo `accessLevel` agora é onipresente.

### Comportamento Padrão
*   O valor padrão para usuários antigos ou novos é `"FULL"`.
*   Valores possíveis: `"FULL"` | `"READONLY"`.

### Endpoints Impactados

#### A. Listagem de Usuários (`GET /api/usuarios`)
*   O objeto de cada usuário na lista agora inclui explicitamente `accessLevel`.

#### B. Login (`POST /api/usuarios/login`)
*   A resposta de sucesso já inclui `accessLevel`.

#### C. **[NOVO]** Detalhes do Usuário (`GET /api/usuarios/{id}`)
*   Nova rota criada para buscar dados atualizados de um usuário específico.
*   **Query Params:** `requesterId`, `requesterRole` (obrigatórios se não for o próprio usuário).
*   **Retorno:** Objeto `Usuario` completo.

    **Exemplo de Resposta:**
    ```json
    {
        "id": 32,
        "nome": "Carla Teste",
        "email": "carla@teste.com",
        "accessLevel": "FULL",  // <-- Campo garantido
        "planType": "FREE",
        ...
    }
    ```

---

## 3. Configuração de CORS (Localhost)

*   **Status:** Corrigido.
*   **Origens Liberadas:** `http://localhost:3000` e `http://localhost:5173` foram adicionadas explicitamente à whitelist do `@CrossOrigin`.
*   **Ação Frontend:** O desenvolvimento local deve funcionar sem erros de Network/CORS.

---

## Resumo para Check-out

1.  [ ] **Update User:** Verificar se chamado `PUT` inclui `params: { requesterId, requesterRole }`.
2.  [ ] **Permissões:** Ler `user.accessLevel` do login ou do `GET /usuarios/{id}` para esconder botões "Gerar Treino/Dieta" se for `READONLY`.
