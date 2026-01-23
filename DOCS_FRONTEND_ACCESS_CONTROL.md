# Documentação de Integração Frontend: Controle de Acesso (Leitura vs. Escrita)

**Data:** 17/01/2026
**Versão API:** v2
**Status:** Implementado (Backend)

---

## 1. Visão Geral
Esta funcionalidade permite que o Personal Trainer defina o nível de acesso de seus alunos.
*   **FULL (Padrão):** O aluno tem autonomia total (Gera treinos, dietas e faz análises).
*   **READONLY:** O aluno apenas visualiza o conteúdo criado pelo Personal. Ações de criação são bloqueadas.

---

## 2. Tipagem Sugerida (TypeScript)

Adicione o campo `accessLevel` à interface de Usuário.

```typescript
export type AccessLevel = 'FULL' | 'READONLY';

export interface User {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'PERSONAL' | 'ADMIN';
  accessLevel: AccessLevel; // <-- Novo campo
  // ... outros campos
}
```

---

## 3. Alterações nos Endpoints

### A. Login e Sessão (`POST /api/usuarios/login`)
A resposta do login agora inclui o campo `accessLevel`.

**Resposta de Exemplo:**
```json
{
  "id": 10,
  "name": "Aluno Exemplo",
  "email": "aluno@teste.com",
  "role": "USER",
  "accessLevel": "READONLY", // ou "FULL"
  "plan": { ... },
  "usage": { ... }
}
```

**Ação Frontend:**
Ao receber o login, armazene este valor no estado global/contexto/localStorage para uso em regras de exibição.

### B. Atualizar Permissão do Aluno (`PUT /api/usuarios/{id}`)
Este endpoint é usado pelo **Personal** (no Painel Administrativo) para mudar a permissão do aluno.

*   **URL:** `/api/usuarios/{id}`
*   **Método:** `PUT`
*   **Query Params Obrigatórios:** `requesterId`, `requesterRole` (ID e Role do Personal logado)
*   **Payload:**
    ```json
    {
       "accessLevel": "READONLY" // ou "FULL"
    }
    ```
    *Nota: Você pode enviar outros campos para atualizar (ex: `nome`) no mesmo payload se desejar.*

**Exemplo de Chamada (Axios):**
```typescript
const updatePermission = async (studentId: number, level: 'FULL' | 'READONLY') => {
  await api.put(`/usuarios/${studentId}`, 
    { accessLevel: level },
    { params: { requesterId: myPersonalId, requesterRole: 'PERSONAL' } }
  );
};
```

---

## 4. Tratamento de Erros e Bloqueios

O Backend impõe o bloqueio final. Se um usuário `READONLY` tentar burlar o frontend e chamar a API diretamente, ele receberá um erro **403 Forbidden**.

**Endpoints Protegidos:**
1.  **Gerar Treino V1:** `POST /api/treinos` -> 403
2.  **Gerar Treino V2:** `POST /api/v2/treinos` -> 403
3.  **Gerar Dieta V1:** `POST /api/dietas` -> 403
4.  **Gerar Dieta V2:** `POST /api/v2/dietas` -> 403
5.  **Análise de Vídeo:** `POST /api/historico` -> 403
6.  **Consumir Crédito:** `POST /api/usuarios/consume-credit/...` -> 403

**Mensagem de Erro Padrão:**
```json
{
  "message": "Seu nível de acesso não permite esta ação. Solicite ao seu Personal."
}
```

---

## 5. Checklist de Implementação Frontend

### Painel do Aluno (App Mobile/Web)
1.  [ ] **Global:** Verificar `user.accessLevel` no Contexto.
2.  [ ] **Tela de Treinos:**
    *   Se `READONLY`: Esconder botão "Gerar Novo Treino".
3.  [ ] **Tela de Dietas:**
    *   Se `READONLY`: Esconder botão "Gerar Nova Dieta".
4.  [ ] **Tela de Análise:**
    *   Se `READONLY`: Bloquear upload de vídeo ou esconder a opção. Exibir mensagem explicativa se tentar acessar.

### Painel do Personal (AdminDashboard)
1.  [ ] **Modal de Novo Aluno:**
    *   O Backend define 'FULL' por padrão se não enviado.
    *   (Opcional) Adicionar seletor no cadastro se quiser definir na criação.
2.  [ ] **Edição de Aluno:**
    *   Adicionar Radio Buttons ou Select: "Acesso Total" vs "Apenas Leitura".
    *   Ao salvar, chamar `PUT /api/usuarios/{id}` passando o novo `accessLevel`.

---

**Dúvidas?** Consulte a equipe de Backend.
