# Check-ins Semanais e Streaks - Documentação para Frontend

## Visão Geral

A feature de **Check-ins Semanais** permite que usuários visualizem seu histórico recente de treinos em um formato de calendário semanal, acompanhem seu progresso em relação à meta semanal e visualizem sua sequência de dias consecutivos (streak).

---

## Tipos TypeScript (Interfaces)

Adicione estas interfaces ao arquivo de tipos do frontend (ex: `types.ts`).

### 1. Resposta de Check-ins Semanais

```typescript
export interface CheckInDetail {
  id: string;
  timestamp: number;
  comment?: string;
}

export interface DayCheckIn {
  dayOfWeek: 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday' | 'saturday' | 'sunday';
  dayLabel: string; // Ex: "Seg", "Ter"
  date: string;     // YYYY-MM-DD
  hasCheckIn: boolean;
  checkIn?: CheckInDetail | null;
}

export interface WeeklyCheckInsResponse {
  weekStart: string;    // YYYY-MM-DD
  weekEnd: string;      // YYYY-MM-DD
  weekLabel: string;    // Ex: "Semana 3 de Janeiro"
  weeklyGoal: number;   // Meta de treinos (default: 5)
  totalCheckIns: number;
  days: DayCheckIn[];   // Array sempre com 7 dias (Seg a Dom)
}
```

### 2. Resposta de Streak

```typescript
export interface UserStreakResponse {
  currentStreak: number;    // Dias consecutivos atuais
  longestStreak: number;    // Maior sequência histórica
  lastCheckInDate: string;  // YYYY-MM-DD
  isActiveToday: boolean;   // Se o usuário já treinou hoje
}
```

### 3. Request de Atualização de Meta

```typescript
export interface WeeklyGoalRequest {
  weeklyGoal: number; // Inteiro entre 1 e 7
}
```

---

## Endpoints

### 1. Obter Check-ins da Semana

Retorna os dados formatados para preencher o componente visual de calendário semanal.

**`GET /api/checkins/{userId}/week`**

**Query Params:**
- `weekStart` (opcional): Data de início da semana (YYYY-MM-DD). Se omitido, usa a segunda-feira da semana atual.
- `requesterId` (obrigatório): ID do usuário logado.
- `requesterRole` (obrigatório): Role do usuário (`user`, `personal`, `admin`).

**Exemplo de Chamada:**
```typescript
const response = await fetch(
  `/api/checkins/${userId}/week?requesterId=${userId}&requesterRole=user`
);
const data: WeeklyCheckInsResponse = await response.json();
```

**Exemplo de Resposta (200 OK):**
```json
{
  "weekStart": "2026-01-13",
  "weekEnd": "2026-01-19",
  "weekLabel": "Semana 3 de Janeiro",
  "weeklyGoal": 5,
  "totalCheckIns": 3,
  "days": [
    {
      "dayOfWeek": "monday",
      "dayLabel": "Seg",
      "date": "2026-01-13",
      "hasCheckIn": true,
      "checkIn": {
        "id": "123",
        "timestamp": 1736776800000,
        "comment": "Treino pago!"
      }
    },
    {
      "dayOfWeek": "tuesday",
      "dayLabel": "Ter",
      "date": "2026-01-14",
      "hasCheckIn": false,
      "checkIn": null
    },
    // ... até domingo
  ]
}
```

---

### 2. Obter Streak do Usuário

Retorna informações sobre a sequência de dias consecutivos de treino.

**`GET /api/checkins/{userId}/streak`**

**Query Params:**
- `requesterId` (obrigatório)
- `requesterRole` (obrigatório)

**Exemplo de Chamada:**
```typescript
const response = await fetch(
  `/api/checkins/${userId}/streak?requesterId=${userId}&requesterRole=user`
);
const data: UserStreakResponse = await response.json();
```

**Exemplo de Resposta (200 OK):**
```json
{
  "currentStreak": 3,
  "longestStreak": 15,
  "lastCheckInDate": "2026-01-19",
  "isActiveToday": true
}
```

**Regras de Exibição:**
- Se `currentStreak > 0`, mostre um ícone de fogo/chama 🔥.
- Se `isActiveToday` for `true`, destaque o dia atual visualmente.
- O streak reinicia (0) se o usuário ficar mais de 1 dia sem treinar (ontem ou hoje).

---

### 3. Atualizar Meta Semanal

Permite ao usuário definir quantos dias por semana pretende treinar.

**`PUT /api/usuarios/{userId}/weekly-goal`**

**Headers:**
- `Content-Type: application/json`

**Body:**
```json
{
  "weeklyGoal": 4
}
```

**Query Params:**
- `requesterId` (obrigatório)
- `requesterRole` (obrigatório)

> ⚠️ **Nota:** Apenas o próprio usuário pode alterar sua meta.

**Exemplo de Chamada:**
```typescript
await fetch(
  `/api/usuarios/${userId}/weekly-goal?requesterId=${userId}&requesterRole=user`, 
  {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ weeklyGoal: 4 })
  }
);
```

**Exemplo de Resposta (200 OK):**
```json
{
  "success": true,
  "weeklyGoal": 4
}
```

---

## Integração no Frontend (Sugestão)

### Componente de Progresso Semanal

Recomendamos criar um componente `WeeklyProgress` que:
1. Chama `/week` ao montar.
2. Exibe uma barra de progresso: `(totalCheckIns / weeklyGoal) * 100`.
3. Renderiza 7 "bolinhas" ou cards representando os dias da `response.days`.
   - Bolinha verde/preenchida se `hasCheckIn` for `true`.
   - Bolinha cinza/vazia se `false`.
4. Permite editar a meta clicando em um botão de "Configurar Meta" (chama `/weekly-goal`).

### Componente de Streak

Pode ser um widget pequeno no header ou ao lado da foto de perfil:
- Chama `/streak` ao montar ou a cada refresh.
- Se `currentStreak > 0`, exibe "🔥 3 dias".
- Se `currentStreak > longestStreak`, exibe animação de recorde batido.

---

## Erros Comuns

| Código | Mensagem | Causa |
|--------|----------|-------|
| 403 | Acesso negado | O `requesterId` não tem permissão para ver os dados do `userId`. |
| 400 | Meta inválida | O valor de `weeklyGoal` deve ser entre 1 e 7. |
