# API V2 - Structured Data Documentation

> **Backend deployed at**: `https://services-testeai-backend-732767853162.us-central1.run.app`

## Overview

New V2 endpoints that return/accept **JSON structured data** instead of HTML. These endpoints coexist with the current V1 routes - **zero impact on existing flow**.

| Version | Endpoints | Data Format |
|---------|-----------|-------------|
| V1 (current) | `/api/dietas/`, `/api/treinos/` | HTML string |
| V2 (new) | `/api/v2/dietas/`, `/api/v2/treinos/` | JSON structured |

---

## 🍽️ Structured Diet API

### Create Diet (POST)

```http
POST /api/v2/dietas/?requesterId={id}&requesterRole={role}
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "123",
  "goal": "WEIGHT_LOSS",
  "totalCalories": 1800,
  "protein": 130,
  "carbohydrates": 180,
  "fats": 60,
  "fiber": 25,
  "water": "2-3L",
  "observations": "Evitar lactose",
  "daysData": "{\"days\": [...]}",
  "legacyHtml": "<div>...</div>"
}
```

**`daysData` JSON Structure:**
```json
{
  "days": [
    {
      "dayOfWeek": "monday",
      "dayLabel": "Segunda-feira",
      "isRestDay": false,
      "note": null,
      "meals": [
        {
          "type": "breakfast",
          "label": "Café da Manhã",
          "icon": "☀️",
          "time": "07:00",
          "items": [
            {
              "name": "Ovos mexidos",
              "quantity": "2 unidades",
              "calories": 140,
              "protein": 12,
              "notes": "Pode substituir por clara"
            }
          ]
        }
      ]
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userId": "123",
  "goal": "WEIGHT_LOSS",
  "createdAt": "2026-01-14T18:45:00",
  "totalCalories": 1800,
  "protein": 130,
  "carbohydrates": 180,
  "fats": 60,
  "fiber": 25,
  "water": "2-3L",
  "daysData": "{...}",
  "observations": "Evitar lactose",
  "legacyHtml": "<div>...</div>"
}
```

### List Diets (GET)

```http
GET /api/v2/dietas/{userId}?requesterId={id}&requesterRole={role}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": "123",
    "goal": "WEIGHT_LOSS",
    "createdAt": "2026-01-14T18:45:00",
    "totalCalories": 1800,
    "protein": 130,
    "carbohydrates": 180,
    "fats": 60,
    "daysData": "{...}",
    "observations": "...",
    "legacyHtml": "..."
  }
]
```

### Delete Diet (DELETE)

```http
DELETE /api/v2/dietas/{dietId}?requesterId={id}&requesterRole={role}
```

**Response (200 OK):**
```json
{ "message": "Dieta estruturada excluída com sucesso." }
```

---

## 🏋️ Structured Training API

### Create Training (POST)

```http
POST /api/v2/treinos/?requesterId={id}&requesterRole={role}
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "123",
  "goal": "HYPERTROPHY",
  "level": "intermediate",
  "frequency": 4,
  "trainingStyle": "Push/Pull/Legs",
  "estimatedDuration": "60-75 min",
  "focus": "Hipertrofia,Força",
  "observations": "Evitar exercícios de impacto",
  "daysData": "{\"days\": [...]}",
  "legacyHtml": "<div>...</div>"
}
```

**`daysData` JSON Structure:**
```json
{
  "days": [
    {
      "dayOfWeek": "monday",
      "dayLabel": "Segunda-feira",
      "trainingType": "Push (Peito, Ombro, Tríceps)",
      "isRestDay": false,
      "note": "Aquecimento: 10 min esteira",
      "exercises": [
        {
          "order": 1,
          "name": "Supino Reto com Barra",
          "muscleGroup": "Peito",
          "sets": 4,
          "reps": "10-12",
          "rest": "90s",
          "technique": "Foco na descida controlada",
          "videoQuery": "supino reto com barra execução correta"
        }
      ]
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userId": "123",
  "goal": "HYPERTROPHY",
  "level": "intermediate",
  "frequency": 4,
  "createdAt": "2026-01-14T18:45:00",
  "trainingStyle": "Push/Pull/Legs",
  "estimatedDuration": "60-75 min",
  "focus": "Hipertrofia,Força",
  "daysData": "{...}",
  "observations": "...",
  "legacyHtml": "..."
}
```

### List Trainings (GET)

```http
GET /api/v2/treinos/{userId}?requesterId={id}&requesterRole={role}
```

### Delete Training (DELETE)

```http
DELETE /api/v2/treinos/{treinoId}?requesterId={id}&requesterRole={role}
```

---

## 🔌 Frontend Integration Guide

### 1. Types (types.ts)

```typescript
// ========== DIET TYPES ==========

interface StructuredDietPlan {
  id: number;
  userId: string;
  goal: string;
  createdAt: string;
  
  // Summary
  totalCalories: number;
  protein: number;
  carbohydrates: number;
  fats: number;
  fiber?: number;
  water?: string;
  
  // Full data
  daysData: string; // JSON string - parse it!
  observations?: string;
  legacyHtml?: string;
}

interface DietDay {
  dayOfWeek: 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday' | 'saturday' | 'sunday';
  dayLabel: string;
  isRestDay: boolean;
  note?: string;
  meals: Meal[];
}

interface Meal {
  type: 'breakfast' | 'morning_snack' | 'lunch' | 'afternoon_snack' | 'dinner' | 'supper';
  label: string;
  icon?: string;
  time?: string;
  items: MealItem[];
}

interface MealItem {
  name: string;
  quantity: string;
  calories?: number;
  protein?: number;
  notes?: string;
}

// ========== TRAINING TYPES ==========

interface StructuredWorkoutPlan {
  id: number;
  userId: string;
  goal: string;
  level: 'beginner' | 'intermediate' | 'advanced';
  frequency: number;
  createdAt: string;
  
  // Summary
  trainingStyle: string;
  estimatedDuration: string;
  focus: string; // comma-separated
  
  // Full data
  daysData: string; // JSON string - parse it!
  observations?: string;
  legacyHtml?: string;
}

interface WorkoutDay {
  dayOfWeek: string;
  dayLabel: string;
  trainingType: string;
  isRestDay: boolean;
  note?: string;
  exercises: Exercise[];
}

interface Exercise {
  order: number;
  name: string;
  muscleGroup: string;
  sets: number;
  reps: string;
  rest: string;
  technique?: string;
  videoQuery?: string;
}
```

### 2. API Service (apiService.ts)

```typescript
// ========== V2 STRUCTURED DIET ==========

createDietV2: async (
  requesterId: number,
  requesterRole: string,
  dietData: Omit<StructuredDietPlan, 'id' | 'createdAt'>
): Promise<StructuredDietPlan> => {
  const url = `${API_BASE_URL}/api/v2/dietas/?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dietData)
  });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
},

getDietsV2: async (
  userId: string,
  requesterId: number,
  requesterRole: string
): Promise<StructuredDietPlan[]> => {
  const url = `${API_BASE_URL}/api/v2/dietas/${userId}?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error(await response.text());
  return response.json();
},

deleteDietV2: async (
  dietId: number,
  requesterId: number,
  requesterRole: string
): Promise<void> => {
  const url = `${API_BASE_URL}/api/v2/dietas/${dietId}?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url, { method: 'DELETE' });
  if (!response.ok) throw new Error(await response.text());
},

// ========== V2 STRUCTURED TRAINING ==========

createTrainingV2: async (
  requesterId: number,
  requesterRole: string,
  trainingData: Omit<StructuredWorkoutPlan, 'id' | 'createdAt'>
): Promise<StructuredWorkoutPlan> => {
  const url = `${API_BASE_URL}/api/v2/treinos/?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(trainingData)
  });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
},

getTrainingsV2: async (
  userId: string,
  requesterId: number,
  requesterRole: string
): Promise<StructuredWorkoutPlan[]> => {
  const url = `${API_BASE_URL}/api/v2/treinos/${userId}?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error(await response.text());
  return response.json();
},

deleteTrainingV2: async (
  trainingId: number,
  requesterId: number,
  requesterRole: string
): Promise<void> => {
  const url = `${API_BASE_URL}/api/v2/treinos/${trainingId}?requesterId=${requesterId}&requesterRole=${requesterRole}`;
  const response = await fetch(url, { method: 'DELETE' });
  if (!response.ok) throw new Error(await response.text());
}
```

### 3. Gemini Service (geminiService.ts)

```typescript
export const generateStructuredDietPlan = async (
  userData: DietFormData
): Promise<{ daysData: DietDay[], summary: DietSummary }> => {
  const ai = await getAiClient();
  
  const prompt = `
    Atue como um nutricionista esportivo.
    
    **IMPORTANTE**: Retorne APENAS um JSON válido, sem markdown, sem \`\`\`json.
    
    PERFIL DO USUÁRIO:
    - Peso: ${userData.weight}kg | Altura: ${userData.height}cm
    - Objetivo: ${userData.goal}
    - Sexo: ${userData.gender}
    - Observações: ${userData.observations || 'Nenhuma'}
    
    RETORNE este JSON exatamente neste formato:
    {
      "summary": {
        "totalCalories": number,
        "protein": number,
        "carbohydrates": number,
        "fats": number
      },
      "days": [
        {
          "dayOfWeek": "monday",
          "dayLabel": "Segunda-feira",
          "isRestDay": false,
          "meals": [
            {
              "type": "breakfast",
              "label": "Café da Manhã",
              "icon": "☀️",
              "items": [
                { "name": "...", "quantity": "..." }
              ]
            }
          ]
        }
      ]
    }
  `;
  
  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash',
    contents: prompt,
    config: {
      responseMimeType: 'application/json'
    }
  });
  
  return JSON.parse(response.text);
};
```

### 4. Usage Example

```typescript
// Generate structured diet from AI
const result = await generateStructuredDietPlan(formData);

// Save to backend V2
const savedDiet = await apiService.createDietV2(
  user.id,
  user.role,
  {
    userId: String(targetUserId),
    goal: formData.goal,
    totalCalories: result.summary.totalCalories,
    protein: result.summary.protein,
    carbohydrates: result.summary.carbohydrates,
    fats: result.summary.fats,
    daysData: JSON.stringify({ days: result.days }),
    observations: formData.observations,
    // Optional: generate HTML for legacy mobile
    legacyHtml: generateLegacyHtml(result)
  }
);

// Later: fetch and display
const diets = await apiService.getDietsV2(userId, requesterId, role);
const parsedDays = JSON.parse(diets[0].daysData).days as DietDay[];
```

---

## Migration Strategy

1. **Phase 1**: Frontend implements V2 service functions alongside existing V1
2. **Phase 2**: New diet/training generation uses V2 endpoints
3. **Phase 3**: Display components read from both V1 (legacyHtml) and V2 (structured)
4. **Phase 4**: Mobile app updated to use native components with V2 data
5. **Phase 5**: V1 deprecated after full migration

---

## Error Responses

| Status | Message |
|--------|---------|
| 403 | `Acesso negado. Você não tem vínculo com este aluno.` |
| 403 | `Plano gratuito não permite geração de dietas estruturadas.` |
| 404 | `Dieta estruturada não encontrada.` |
| 500 | `Erro ao salvar dieta estruturada: {details}` |
