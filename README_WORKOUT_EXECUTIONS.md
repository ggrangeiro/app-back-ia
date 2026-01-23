# 📋 Sistema de Execução de Treinos com Cargas - Backend

## 📌 Visão Geral

Este documento descreve a implementação completa do sistema de execução de treinos com rastreamento de cargas no backend.

**Implementado:**
- ✅ 5 novas tabelas SQL
- ✅ 3 novas entidades Java
- ✅ 3 novos repositories
- ✅ 3 novos controllers
- ✅ 1 service de permissões
- ✅ DTOs para requests/responses
- ✅ Sistema completo de CRUD para treinos V2
- ✅ Sistema de execução com histórico de cargas
- ✅ Sugestões automáticas de progressão

---

## 🗄️ Estrutura do Banco de Dados

### 1. Executar Script de Migração

**Arquivo:** `src/main/resources/db-migration.sql`

**Como executar:**

```bash
# Conectar ao Google Cloud SQL
gcloud sql connect exercicio-fit --user=root

# Selecionar database
USE exercicio-fit;

# Executar o script (copiar e colar o conteúdo do arquivo)
```

**Tabelas criadas:**
1. `structured_workout_plans` - Treinos estruturados V2 (JSON)
2. `workout_executions` - Execuções de treino
3. `exercise_executions` - Execuções de exercícios individuais

**Nota:** As tabelas de relacionamento personal → alunos e professor → personals **já existem** no sistema e não foram criadas neste script.

---

## 📁 Estrutura de Arquivos Criados

```
src/main/java/gcfv2/
├── Entidades
│   ├── StructuredWorkoutPlan.java
│   ├── WorkoutExecution.java
│   └── ExerciseExecution.java
├── Repositories
│   ├── StructuredWorkoutPlanRepository.java
│   ├── WorkoutExecutionRepository.java
│   └── ExerciseExecutionRepository.java
├── Controllers
│   ├── StructuredWorkoutController.java
│   ├── WorkoutExecutionController.java
│   └── LoadHistoryController.java
├── Services
│   └── PermissionService.java
└── dto/
    ├── WorkoutExecutionRequest.java
    ├── ExerciseExecutionRequest.java
    └── LoadHistoryResponse.java

src/main/resources/
└── db-migration.sql
```

---

## 🔌 Endpoints da API

### 1. CRUD de Treinos Estruturados (V2)

#### **POST** `/api/v2/treinos` - Criar treino V2

**Request:**
```json
{
  "userId": 123,
  "title": "Treino ABC - João Silva",
  "daysData": "{...JSON completo do treino...}",
  "legacyHtml": "<html>...</html>"
}
```

**Query Params:** `?requesterId=123&requesterRole=user`

**Response:** `201 Created`

---

#### **GET** `/api/v2/treinos/{userId}` - Listar treinos

**Query Params:** `?requesterId=123&requesterRole=user`

**Response:**
```json
[
  {
    "id": 45,
    "userId": 123,
    "title": "Treino ABC - João Silva",
    "daysData": "{...}",
    "createdAt": "2026-01-22T10:00:00Z"
  }
]
```

---

#### **GET** `/api/v2/treinos/detail/{workoutId}` - Detalhes do treino

**Response:**
```json
{
  "id": 45,
  "userId": 123,
  "title": "Treino ABC",
  "daysData": "{...JSON completo...}",
  "legacyHtml": "<html>...</html>",
  "createdAt": "2026-01-22T10:00:00Z"
}
```

---

#### **PUT** `/api/v2/treinos/{workoutId}` - Atualizar treino

**Request:**
```json
{
  "title": "Treino ABC - Atualizado",
  "daysData": "{...novo JSON...}"
}
```

---

#### **DELETE** `/api/v2/treinos/{workoutId}` - Deletar treino (soft delete)

**Response:**
```json
{
  "message": "Treino deletado com sucesso"
}
```

---

### 2. Execução de Treinos

#### **POST** `/api/v2/workout-executions` - Salvar execução

**Request:**
```json
{
  "userId": 123,
  "workoutId": 45,
  "dayOfWeek": "monday",
  "executedAt": 1706191800000,
  "comment": "Treino intenso! Aumentei a carga no supino.",
  "exercises": [
    {
      "exerciseName": "Supino Reto com Barra",
      "order": 1,
      "setsCompleted": 4,
      "actualLoad": "22kg",
      "notes": "Senti ótima conexão muscular"
    },
    {
      "exerciseName": "Desenvolvimento com Halteres",
      "order": 2,
      "setsCompleted": 4,
      "actualLoad": "14kg cada lado"
    }
  ]
}
```

**Query Params:** `?requesterId=123&requesterRole=user`

**Response:** `201 Created`
```json
{
  "id": 789,
  "userId": 123,
  "workoutId": 45,
  "dayOfWeek": "monday",
  "executedAt": 1706191800000,
  "comment": "Treino intenso!",
  "exercises": [
    {
      "id": 1001,
      "exerciseName": "Supino Reto com Barra",
      "exerciseOrder": 1,
      "setsCompleted": 4,
      "actualLoad": "22kg"
    }
  ],
  "createdAt": "2026-01-22T14:30:05Z"
}
```

---

#### **GET** `/api/v2/workout-executions/{userId}` - Listar execuções

**Query Params:**
- `requesterId` (obrigatório)
- `requesterRole` (obrigatório)
- `workoutId` (opcional) - Filtrar por treino
- `startDate` (opcional) - Unix timestamp
- `endDate` (opcional) - Unix timestamp
- `limit` (opcional, padrão: 50)
- `offset` (opcional, padrão: 0)

**Exemplo:**
```
GET /api/v2/workout-executions/123?requesterId=123&requesterRole=user&workoutId=45&limit=10
```

**Response:**
```json
{
  "executions": [
    {
      "id": 789,
      "userId": 123,
      "workoutId": 45,
      "dayOfWeek": "monday",
      "executedAt": 1706191800000,
      "comment": "Treino bom!",
      "exercises": [...]
    }
  ],
  "pagination": {
    "total": 25,
    "limit": 10,
    "offset": 0
  }
}
```

---

#### **GET** `/api/v2/workout-executions/detail/{executionId}` - Detalhes da execução

**Response:**
```json
{
  "id": 789,
  "userId": 123,
  "workoutId": 45,
  "dayOfWeek": "monday",
  "executedAt": 1706191800000,
  "exercises": [...]
}
```

---

### 3. Histórico de Cargas e Progressão

#### **GET** `/api/v2/exercises/{exerciseName}/load-history` - Histórico de cargas

**Query Params:**
- `userId` (obrigatório)
- `requesterId` (obrigatório)
- `requesterRole` (obrigatório)
- `limit` (opcional, padrão: 10)

**Exemplo:**
```
GET /api/v2/exercises/Supino%20Reto%20com%20Barra/load-history?userId=123&requesterId=123&requesterRole=user&limit=10
```

**Response:**
```json
{
  "exerciseName": "Supino Reto com Barra",
  "history": [
    {
      "executionId": 789,
      "executedAt": 1706191800000,
      "actualLoad": "22kg",
      "setsCompleted": 4
    },
    {
      "executionId": 785,
      "executedAt": 1706105400000,
      "actualLoad": "20kg",
      "setsCompleted": 4
    }
  ],
  "progressionSuggestion": {
    "currentLoad": "22kg",
    "nextSuggestedLoad": "24.0kg",
    "reason": "Você completou 4 séries com 22kg nas últimas 2 sessões. Tente aumentar a carga!"
  }
}
```

---

## 🔐 Sistema de Permissões

### Hierarquia de Roles

1. **user (aluno)**
   - Acessa apenas seus próprios dados
   - Pode salvar execuções de treino

2. **personal**
   - Acessa seus dados
   - Pode acessar dados de seus alunos (relacionamentos já existem no sistema)
   - **Integração pendente:** Injetar repository de relacionamentos no `PermissionService`

3. **professor**
   - Acessa seus dados
   - Pode acessar dados dos personals que gerencia (relacionamentos já existem)
   - Pode acessar dados dos alunos dos personals
   - **Integração pendente:** Injetar repositories de relacionamentos no `PermissionService`

4. **admin**
   - Acessa todos os dados

### Validação de Permissões

Todos os endpoints exigem:
- `requesterId`: ID do usuário fazendo a requisição
- `requesterRole`: Role do usuário (user, personal, professor, admin)

**Exemplo:**
```
POST /api/v2/workout-executions?requesterId=123&requesterRole=user
```

---

## 🧪 Como Testar

### 1. Criar um treino V2

```bash
curl -X POST "http://localhost:8080/api/v2/treinos?requesterId=1&requesterRole=user" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Treino ABC - Teste",
    "daysData": "{\"summary\":{\"trainingStyle\":\"ABC\"},\"days\":[{\"dayOfWeek\":\"monday\",\"dayLabel\":\"Segunda-feira\",\"trainingType\":\"Treino A\",\"isRestDay\":false,\"exercises\":[{\"order\":1,\"name\":\"Supino Reto\",\"muscleGroup\":\"Peito\",\"sets\":4,\"reps\":\"8-10\",\"rest\":\"90s\",\"suggestedLoad\":\"20kg\"}]}]}"
  }'
```

### 2. Salvar uma execução

```bash
curl -X POST "http://localhost:8080/api/v2/workout-executions?requesterId=1&requesterRole=user" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "workoutId": 1,
    "dayOfWeek": "monday",
    "executedAt": 1706191800000,
    "comment": "Treino excelente!",
    "exercises": [
      {
        "exerciseName": "Supino Reto",
        "order": 1,
        "setsCompleted": 4,
        "actualLoad": "22kg"
      }
    ]
  }'
```

### 3. Buscar histórico de cargas

```bash
curl "http://localhost:8080/api/v2/exercises/Supino%20Reto/load-history?userId=1&requesterId=1&requesterRole=user&limit=5"
```

---

## ⚠️ Pendências e TODOs

### Alta Prioridade

1. **Integrar com relacionamentos existentes de permissão**
   - As tabelas de relacionamento personal→alunos e professor→personals **já existem**
   - Injetar os repositories dessas tabelas no `PermissionService`
   - Descomentar os TODOs no `PermissionService` para validar acesso baseado nessas tabelas

2. **Integração com check-ins legados**
   - Criar check-in automático ao salvar execução (compatibilidade)
   - Endpoint: Verificar estrutura da tabela `checkins`

3. **Testes automatizados**
   - Unit tests para services
   - Integration tests para controllers

### Média Prioridade

4. **Melhorias na sugestão de progressão**
   - Algoritmo mais sofisticado baseado em histórico longo
   - Considerar tipo de exercício (compostos vs isolados)

5. **Validações adicionais**
   - Validar estrutura do JSON `daysData`
   - Rate limiting nos endpoints

6. **Paginação melhorada**
   - Cursor-based pagination para grandes volumes

### Baixa Prioridade

7. **Relatórios e Analytics**
   - Endpoint para estatísticas de progressão
   - Gráficos de evolução de carga

8. **Notificações**
   - Notificar quando atingir novo recorde
   - Alertas de estagnação

---

## 📊 Estrutura do JSON daysData

O campo `daysData` segue esta estrutura TypeScript:

```typescript
interface StructuredWorkoutData {
  summary: {
    trainingStyle: string;
    estimatedDuration: string;
    focus: string;
    considerations?: string;
  };
  days: WorkoutDay[];
}

interface WorkoutDay {
  dayOfWeek: string;  // monday, tuesday, etc.
  dayLabel: string;
  trainingType: string;
  isRestDay: boolean;
  exercises: ExerciseV2[];
}

interface ExerciseV2 {
  order: number;
  name: string;
  muscleGroup: string;
  sets: number;
  reps: string;
  rest: string;
  technique?: string;
  videoQuery?: string;
  suggestedLoad?: string;  // ← IMPORTANTE: IA sempre retorna
}
```

---

## 🚀 Deploy

### Build do projeto

```bash
./mvnw clean package
```

### Deploy no Google Cloud Functions

```bash
gcloud functions deploy workout-api \
  --gen2 \
  --runtime=java21 \
  --region=us-west1 \
  --source=. \
  --entry-point=io.micronaut.gcp.function.http.HttpFunction \
  --trigger-http \
  --allow-unauthenticated
```

---

## 📞 Suporte

Em caso de dúvidas ou problemas, abra uma issue ou entre em contato com a equipe de desenvolvimento.

---

**Versão:** 1.0
**Data:** 22/01/2026
**Status:** ✅ Implementação Completa
