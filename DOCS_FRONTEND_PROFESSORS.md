# Hierarquia de Professores - Documentação para Frontend

## Visão Geral

O sistema agora suporta uma nova role **`professor`** que permite Personal Trainers delegarem tarefas para professores subordinados. Professores podem criar alunos, gerar treinos, dietas e realizar análises, mas **não podem cadastrar novos professores**.

### Nova Hierarquia

```
Admin
├── Personal Trainer
│   ├── Professor (novo)
│   │   └── Aluno/User
│   └── Aluno/User
```

### ⚠️ Compartilhamento de Alunos

**Importante**: Professores e o Personal compartilham **todos os alunos do ecossistema**:

- Um **Personal** vê: seus alunos diretos + alunos de todos os seus professores
- Um **Professor** vê: seus alunos + alunos do Personal + alunos de outros professores do mesmo Personal

**Isso significa que:**
1. O Personal pode gerar treino/dieta/análise para qualquer aluno (próprio ou de professor)
2. O Professor pode gerar treino/dieta/análise para qualquer aluno do ecossistema
3. A listagem de alunos (`GET /api/usuarios/`) retorna todos os alunos compartilhados

### 💰 Créditos Compartilhados

**Professores usam os créditos do Personal (manager)**:

- No **Login**: Professores recebem `plan` e `usage` (créditos) do Personal
- No **Consume-Credit**: Créditos são debitados da conta do Personal, não do professor
- O campo `managerId` é retornado no login para identificar o Personal responsável

**Resposta de login para professor inclui:**
```json
{
  "id": 210,
  "role": "PROFESSOR",
  "managerId": 105,
  "plan": { /* dados do Personal */ },
  "usage": { /* créditos do Personal */ }
}
```

---

## Tipos Atualizados

### UserRole

```typescript
export type UserRole = 'admin' | 'user' | 'personal' | 'professor';
```

### User Interface

```typescript
export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  avatar?: string;
  credits?: number;
  accessLevel?: 'FULL' | 'READONLY';
  personalId?: string;   // ID do responsável (para user)
  managerId?: string;    // ID do personal que gerencia (apenas para professor)
  // ... demais campos existentes
}
```

### Novos Tipos

```typescript
// Professor com estatísticas
export interface ProfessorWithStats {
  id: number;
  name: string;
  email: string;
  role: 'professor';
  managerId: number;
  credits: number;
  studentsCount: number;
  lastActivity: string | null;
  avatar?: string;
}

// Atividade de professor
export interface ProfessorActivity {
  id: number;
  professorId: number;
  professorName: string;
  actionType: 'STUDENT_CREATED' | 'WORKOUT_GENERATED' | 'DIET_GENERATED' | 'ANALYSIS_PERFORMED';
  targetUserId?: number;
  targetUserName?: string;
  resourceType?: 'TRAINING' | 'DIET' | 'ANALYSIS' | 'USER';
  resourceId?: number;
  metadata?: Record<string, any>;
  createdAt: string;
}

// Estatísticas de produtividade
export interface ProfessorStats {
  studentsCreated: number;
  workoutsGenerated: number;
  dietsGenerated: number;
  analysisPerformed: number;
  totalActions: number;
}

// Professor com produtividade (para dashboard)
export interface ProfessorProductivity {
  id: number;
  name: string;
  avatar?: string;
  stats: ProfessorStats;
  lastActivity: string | null;
}

// Resumo de produtividade
export interface ProductivitySummary {
  period: 'day' | 'week' | 'month';
  startDate: string;
  endDate: string;
  professors: ProfessorProductivity[];
  totals: ProfessorStats;
}
```

---

## Endpoints

### 1. Criar Professor

**`POST /api/usuarios/`**

Mesmo endpoint de criação de usuário, com `role: "professor"`.

**Request:**
```json
{
  "nome": "Professor João",
  "email": "joao@academia.com",
  "senha": "senha123",
  "telefone": "11999998888",
  "role": "professor"
}
```

**Query Params:**
- `requesterId`: ID do personal logado
- `requesterRole`: `PERSONAL` ou `ADMIN`

**Response (201 Created):**
```json
{
  "id": 210,
  "nome": "Professor João",
  "email": "joao@academia.com",
  "role": "PROFESSOR",
  "managerId": 105,
  "credits": 10,
  "accessLevel": "FULL"
}
```

> **Regras:**
> - Apenas `PERSONAL` ou `ADMIN` podem criar professores
> - `managerId` é definido automaticamente como o `requesterId` quando o requester é `PERSONAL`

---

### 2. Listar Professores

**`GET /api/usuarios/professors`**

Lista todos os professores de um personal com estatísticas.

**Query Params:**
- `managerId` (obrigatório): ID do personal
- `requesterId`: ID do usuário logado
- `requesterRole`: `PERSONAL` ou `ADMIN`

**Response (200 OK):**
```json
{
  "professors": [
    {
      "id": 210,
      "name": "Professor João",
      "email": "joao@academia.com",
      "role": "PROFESSOR",
      "managerId": 105,
      "credits": 50,
      "studentsCount": 12,
      "lastActivity": "2026-01-17T15:30:00",
      "avatar": null
    }
  ],
  "total": 1
}
```

---

### 3. Listar Atividades

**`GET /api/activities/professors`**

Lista atividades dos professores com filtros e paginação.

**Query Params:**
| Param | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `managerId` | number | ✅ | ID do personal |
| `requesterId` | number | ✅ | ID do usuário logado |
| `requesterRole` | string | ✅ | Role do usuário |
| `professorId` | number | ❌ | Filtrar por professor |
| `actionType` | string | ❌ | Filtrar por tipo de ação |
| `startDate` | ISO Date | ❌ | Data inicial (default: 30 dias atrás) |
| `endDate` | ISO Date | ❌ | Data final (default: hoje) |
| `page` | number | ❌ | Página (default: 0) |
| `size` | number | ❌ | Tamanho da página (default: 50, max: 100) |

**Response (200 OK):**
```json
{
  "activities": [
    {
      "id": 1001,
      "professorId": 210,
      "professorName": "Professor João",
      "actionType": "WORKOUT_GENERATED",
      "targetUserId": 350,
      "targetUserName": "Maria Silva",
      "resourceType": "TRAINING",
      "resourceId": 5001,
      "createdAt": "2026-01-17T15:30:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "totalElements": 156,
    "totalPages": 4
  }
}
```

---

### 4. Dashboard de Produtividade

**`GET /api/activities/professors/summary`**

Resumo agregado de produtividade dos professores.

**Query Params:**
| Param | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `managerId` | number | ✅ | ID do personal |
| `requesterId` | number | ✅ | ID do usuário logado |
| `requesterRole` | string | ✅ | Role do usuário |
| `period` | string | ❌ | `day` \| `week` \| `month` (default: week) |

**Response (200 OK):**
```json
{
  "period": "week",
  "startDate": "2026-01-11",
  "endDate": "2026-01-18",
  "professors": [
    {
      "id": 210,
      "name": "Professor João",
      "avatar": null,
      "stats": {
        "studentsCreated": 3,
        "workoutsGenerated": 15,
        "dietsGenerated": 8,
        "analysisPerformed": 22,
        "totalActions": 48
      },
      "lastActivity": "2026-01-17T15:30:00"
    }
  ],
  "totals": {
    "studentsCreated": 4,
    "workoutsGenerated": 23,
    "dietsGenerated": 13,
    "analysisPerformed": 32,
    "totalActions": 72
  }
}
```

---

## Matriz de Permissões Atualizada

| Ação | Admin | Personal | Professor | User |
|------|:-----:|:--------:|:---------:|:----:|
| Criar Professor | ✅ | ✅ | ❌ | ❌ |
| Editar Professor | ✅ | ✅ (próprios) | ❌ | ❌ |
| Deletar Professor | ✅ | ✅ (próprios) | ❌ | ❌ |
| Criar Aluno | ✅ | ✅ | ✅ | ❌ |
| Gerar Treino/Dieta | ✅ | ✅ | ✅ | Depende accessLevel |
| Realizar Análise | ✅ | ✅ | ✅ | Depende accessLevel |
| Ver Atividades Prof. | ✅ | ✅ (próprios) | ❌ | ❌ |
| Ver Dashboard Prod. | ✅ | ✅ (próprios) | ❌ | ❌ |

---

## Fluxo de Integração

### 1. Personal Cria Professor

```typescript
const response = await fetch(`/api/usuarios/?requesterId=${personalId}&requesterRole=PERSONAL`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    nome: 'Professor Novo',
    email: 'professor@email.com',
    senha: '123456',
    role: 'professor'
  })
});
```

### 2. Professor Cria Aluno

```typescript
const response = await fetch(`/api/usuarios/?requesterId=${professorId}&requesterRole=PROFESSOR`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    nome: 'Aluno Novo',
    email: 'aluno@email.com',
    senha: '123456'
  })
});
// O aluno será criado com personalId = professorId
```

### 3. Personal Acessa Dashboard

```typescript
// Listar professores
const professorsRes = await fetch(
  `/api/usuarios/professors?managerId=${personalId}&requesterId=${personalId}&requesterRole=PERSONAL`
);

// Dashboard de produtividade
const summaryRes = await fetch(
  `/api/activities/professors/summary?managerId=${personalId}&period=week&requesterId=${personalId}&requesterRole=PERSONAL`
);
```

---

## Tipos de Atividades Registradas

| action_type | Descrição | resource_type |
|-------------|-----------|---------------|
| `STUDENT_CREATED` | Professor cadastrou novo aluno | USER |
| `WORKOUT_GENERATED` | Treino gerado para aluno | TRAINING |
| `DIET_GENERATED` | Dieta gerada para aluno | DIET |
| `ANALYSIS_PERFORMED` | Análise de exercício realizada | ANALYSIS |

---

## Considerações para UI

### Nova Aba "Gerenciamento"

Recomenda-se adicionar na dashboard do Personal uma nova aba com:

1. **Lista de Professores**: Cards com métricas de produtividade
2. **Dashboard de Produtividade**: Gráficos e totais por período
3. **Timeline de Atividades**: Histórico recente
4. **Filtros**: Por professor, período e tipo de ação

### Login

O login já retorna `managerId` quando o usuário é professor, permitindo identificar a quem ele está subordinado.
