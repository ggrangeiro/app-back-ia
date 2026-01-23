# 🔗 Guia de Integração Frontend ↔ Backend

## ✅ Status da Implementação Backend

**COMPLETO!** Todos os endpoints especificados foram implementados.

---

## 📋 Checklist de Integração

### Passo 1: Executar Migração do Banco de Dados

**IMPORTANTE:** Execute o script SQL antes de testar os endpoints.

```bash
# Arquivo: src/main/resources/db-migration.sql
# Conectar ao Google Cloud SQL e executar o script
```

Este script cria:
- ✅ Tabela `structured_workout_plans`
- ✅ Tabela `workout_executions`
- ✅ Tabela `exercise_executions`
- ✅ Tabela `personal_students` (relacionamentos)
- ✅ Tabela `professor_personals` (relacionamentos)

---

### Passo 2: Deploy do Backend

Após executar o SQL, fazer deploy do código:

```bash
./mvnw clean package
# Deploy no Google Cloud Functions
```

---

### Passo 3: Fluxo de Integração Frontend

## 🔄 Fluxo Completo de Uso

### 1. Geração Dupla de Treinos (V1 + V2)

Quando a IA gera um treino, o frontend deve:

**a) Salvar versão V1 (HTML) - Sistema Antigo**
```javascript
// POST /api/trainings
{
  userId: "123",
  content: "<html>...</html>",  // HTML gerado
  // ... outros campos
}
```

**b) Salvar versão V2 (Estruturado) - Sistema Novo**
```javascript
// POST /api/v2/treinos?requesterId=123&requesterRole=user
{
  userId: 123,  // ← LONG, não String
  title: "Treino ABC - João Silva",
  daysData: JSON.stringify(structuredData),  // JSON como STRING
  legacyHtml: "<html>...</html>"  // Cópia do HTML V1
}
```

**IMPORTANTE:** `daysData` deve ser **string JSON**, não objeto!

**Exemplo completo:**
```javascript
const structuredData = {
  summary: {
    trainingStyle: "ABC",
    estimatedDuration: "60-75 min",
    focus: "Hipertrofia"
  },
  days: [
    {
      dayOfWeek: "monday",
      dayLabel: "Segunda-feira",
      trainingType: "Treino A - Peito e Tríceps",
      isRestDay: false,
      exercises: [
        {
          order: 1,
          name: "Supino Reto com Barra",
          muscleGroup: "Peito",
          sets: 4,
          reps: "8-10",
          rest: "90s",
          technique: "Desça controlado",
          videoQuery: "supino reto execução",
          suggestedLoad: "40kg"  // ← IA SEMPRE retorna
        }
      ]
    }
  ]
};

// Salvar V2
fetch('/api/v2/treinos?requesterId=123&requesterRole=user', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: 123,
    title: "Treino ABC - João Silva",
    daysData: JSON.stringify(structuredData),  // ← STRING
    legacyHtml: htmlContent
  })
});
```

---

### 2. Visualização do Treino

**Opção A: Ver HTML (V1) - Sistema antigo**
```javascript
// GET /api/trainings/{userId}
// Retorna HTML para impressão/visualização
```

**Opção B: Ver Estruturado (V2) - Sistema novo**
```javascript
// GET /api/v2/treinos/{userId}?requesterId=123&requesterRole=user
// Retorna lista de treinos estruturados
```

---

### 3. Iniciar Execução do Treino

Quando o aluno clica em "Iniciar Treino":

**a) Buscar o treino estruturado**
```javascript
const response = await fetch(
  `/api/v2/treinos/detail/${workoutId}?requesterId=${userId}&requesterRole=user`
);
const workout = await response.json();
const daysData = JSON.parse(workout.daysData);  // Parse do JSON
```

**b) Aluno seleciona qual dia executar**
```javascript
// Frontend exibe lista de dias
const selectedDay = daysData.days.find(d => d.dayOfWeek === 'monday');
```

**c) Mostrar apenas exercícios daquele dia**
```javascript
selectedDay.exercises.forEach(exercise => {
  // Renderizar card do exercício com:
  // - Nome: exercise.name
  // - Séries: exercise.sets
  // - Repetições: exercise.reps
  // - Carga sugerida: exercise.suggestedLoad
  // - Input para carga real utilizada
});
```

---

### 4. Durante a Execução

Aluno edita as cargas em tempo real:

```javascript
const executionData = {
  exercises: [
    {
      exerciseName: "Supino Reto com Barra",
      order: 1,
      setsCompleted: 4,
      actualLoad: "22kg",  // ← Aluno editou (era 40kg)
      notes: "Senti ótima conexão muscular"  // Opcional
    },
    {
      exerciseName: "Desenvolvimento Halteres",
      order: 2,
      setsCompleted: 3,  // ← Completou apenas 3 das 4 séries
      actualLoad: "14kg cada lado"
    }
  ]
};
```

---

### 5. Finalizar Treino

```javascript
// POST /api/v2/workout-executions?requesterId=123&requesterRole=user
const payload = {
  userId: 123,  // ← LONG
  workoutId: 45,  // ← LONG
  dayOfWeek: "monday",  // ← lowercase
  executedAt: Date.now(),  // ← Unix timestamp em milissegundos
  comment: "Treino excelente! Consegui aumentar carga no supino.",
  exercises: executionData.exercises
};

const response = await fetch('/api/v2/workout-executions?requesterId=123&requesterRole=user', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload)
});

// Response 201 Created
const savedExecution = await response.json();
console.log('Execução salva com ID:', savedExecution.id);
```

---

### 6. Histórico e Progressão

**Listar execuções do aluno:**
```javascript
// GET /api/v2/workout-executions/123?requesterId=123&requesterRole=user&limit=10
const { executions, pagination } = await response.json();

executions.forEach(exec => {
  console.log(`Treino em ${new Date(exec.executedAt).toLocaleDateString()}`);
  console.log('Exercícios:', exec.exercises);
});
```

**Buscar histórico de um exercício específico:**
```javascript
// GET /api/v2/exercises/Supino%20Reto%20com%20Barra/load-history?userId=123&requesterId=123&requesterRole=user
const { history, progressionSuggestion } = await response.json();

// Renderizar gráfico de evolução
history.forEach(entry => {
  console.log(`${new Date(entry.executedAt).toLocaleDateString()}: ${entry.actualLoad}`);
});

// Mostrar sugestão
console.log('Próxima carga sugerida:', progressionSuggestion.nextSuggestedLoad);
console.log('Motivo:', progressionSuggestion.reason);
```

---

## ⚠️ Pontos de Atenção

### 1. Tipos de Dados

```typescript
// ❌ ERRADO
userId: "123"  // String

// ✅ CORRETO
userId: 123  // Number (Long no Java)
```

### 2. DaysData como String

```typescript
// ❌ ERRADO
daysData: { summary: {...}, days: [...] }  // Objeto

// ✅ CORRETO
daysData: JSON.stringify({ summary: {...}, days: [...] })  // String
```

### 3. DayOfWeek Lowercase

```typescript
// ❌ ERRADO
dayOfWeek: "Monday"

// ✅ CORRETO
dayOfWeek: "monday"
```

### 4. ExecutedAt em Milissegundos

```typescript
// ✅ CORRETO
executedAt: Date.now()  // 1706191800000

// ❌ ERRADO
executedAt: new Date().toISOString()  // "2026-01-22T10:00:00Z"
```

### 5. Query Parameters Sempre Necessários

```typescript
// ❌ ERRADO
fetch('/api/v2/workout-executions/123')

// ✅ CORRETO
fetch('/api/v2/workout-executions/123?requesterId=123&requesterRole=user')
```

---

## 🧪 Testando os Endpoints

### Teste 1: Criar Treino V2

```javascript
const testWorkout = {
  userId: 1,
  title: "Treino Teste ABC",
  daysData: JSON.stringify({
    summary: {
      trainingStyle: "ABC",
      estimatedDuration: "60 min",
      focus: "Hipertrofia"
    },
    days: [{
      dayOfWeek: "monday",
      dayLabel: "Segunda-feira",
      trainingType: "Treino A",
      isRestDay: false,
      exercises: [{
        order: 1,
        name: "Supino Reto",
        muscleGroup: "Peito",
        sets: 4,
        reps: "8-10",
        rest: "90s",
        suggestedLoad: "20kg"
      }]
    }]
  })
};

fetch('/api/v2/treinos?requesterId=1&requesterRole=user', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(testWorkout)
})
.then(r => r.json())
.then(data => console.log('Treino criado:', data.id));
```

### Teste 2: Salvar Execução

```javascript
const execution = {
  userId: 1,
  workoutId: 1,  // ID retornado do teste 1
  dayOfWeek: "monday",
  executedAt: Date.now(),
  comment: "Teste de execução",
  exercises: [{
    exerciseName: "Supino Reto",
    order: 1,
    setsCompleted: 4,
    actualLoad: "22kg"
  }]
};

fetch('/api/v2/workout-executions?requesterId=1&requesterRole=user', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(execution)
})
.then(r => r.json())
.then(data => console.log('Execução salva:', data));
```

### Teste 3: Ver Histórico

```javascript
fetch('/api/v2/exercises/Supino%20Reto/load-history?userId=1&requesterId=1&requesterRole=user')
  .then(r => r.json())
  .then(data => {
    console.log('Histórico:', data.history);
    console.log('Sugestão:', data.progressionSuggestion);
  });
```

---

## 📊 Exemplo de Interface Sugerida

### Tela de Execução de Treino

```
┌─────────────────────────────────────┐
│ Treino A - Segunda-feira            │
│ 5 exercícios                        │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ 1. Supino Reto com Barra            │
│                                     │
│ 📊 4 séries × 8-10 reps             │
│ ⏱️  Descanso: 90s                   │
│                                     │
│ 💪 Carga Sugerida: 40kg             │
│ ✏️  Carga Usada: [22kg] ← Input     │
│                                     │
│ 📝 Séries completadas: [4] ← Input  │
│                                     │
│ 💬 Notas: [opcional]                │
│                                     │
│ ▶️  Ver vídeo demonstrativo         │
└─────────────────────────────────────┘

... outros exercícios ...

[Finalizar Treino] ← Salva tudo
```

---

## 🚨 Próximos Passos

1. ✅ **Execute o script SQL** no banco de dados
2. ✅ **Faça o deploy** do backend atualizado
3. ✅ **Teste os endpoints** com Postman/Insomnia
4. ✅ **Integre no frontend** seguindo este guia
5. ⏳ **Implemente interface** de execução de treinos
6. ⏳ **Teste o fluxo completo** end-to-end

---

## 📞 Suporte

Dúvidas sobre a integração? Entre em contato com o time de backend.

**Endpoints Disponíveis:**
- ✅ POST `/api/v2/treinos` - Criar treino V2
- ✅ GET `/api/v2/treinos/{userId}` - Listar treinos
- ✅ GET `/api/v2/treinos/detail/{workoutId}` - Detalhes do treino
- ✅ POST `/api/v2/workout-executions` - Salvar execução
- ✅ GET `/api/v2/workout-executions/{userId}` - Listar execuções
- ✅ GET `/api/v2/exercises/{exerciseName}/load-history` - Histórico de cargas

**Status:** 🟢 PRONTO PARA INTEGRAÇÃO
