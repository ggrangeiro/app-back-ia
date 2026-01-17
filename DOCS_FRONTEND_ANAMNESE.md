# Documentação de Integração: Anamnese Completa (V2)

Esta documentação descreve como o frontend deve interagir com o novo módulo de **Anamnese/Avaliação Física**.

## Visão Geral
A anamnese é armazenada como um objeto JSON estruturado dentro do registro do usuário. Ela é **opcional** e pode ser preenchida parcial ou totalmente.

## Tipagens (TypeScript)

Recomendamos criar o arquivo `src/types/Anamnesis.ts` com as seguintes interfaces:

```typescript
export interface AnamnesisDTO {
  updatedAt?: string;
  personal?: PersonalInfo;
  marketing?: MarketingInfo;
  physical?: PhysicalInfo;
  goals?: GoalsInfo;
  health?: HealthInfo;
  nutrition?: NutritionInfo;
  fitness?: FitnessInfo;
  preferences?: PreferencesInfo;
  closing?: ClosingInfo;
}

export interface PersonalInfo {
  fullName?: string;
  whatsapp?: string;
  birthDate?: string; // YYYY-MM-DD
  age?: number;
  location?: {
    city?: string;
    state?: string;
    country?: string;
  };
  maritalStatus?: 'Solteiro(a)' | 'Casado(a)' | 'Separado(a)' | 'Viúvo(a)';
  profession?: string;
}

export interface MarketingInfo {
  referralSource?: 'Indicação' | 'Instagram' | 'Outro';
  instagramFollowTime?: string;
}

export interface PhysicalInfo {
  weight?: number; // kg
  height?: number; // cm ou m
  targetWeight?: number;
  currentBodyShape?: number; // 1-9
  desiredBodyShape?: number; // 1-9
  bodyDissatisfaction?: string;
}

export interface GoalsInfo {
  threeMonthGoal?: string;
  mainObstacle?: string;
}

export interface HealthInfo {
  conditions?: string[];
  injuries?: string;
  lastCheckup?: string;
  chestPain?: boolean;
  dailyActivity?: 'Sentado(a)' | 'Em pé';
  sleepQuality?: 'Ruim' | 'Boa' | 'Ótima';
}

export interface NutritionInfo {
  nutritionalMonitoring?: boolean;
  eatingHabits?: 'Como de tudo' | 'Equilibrada' | 'Restritiva / Low Carb';
}

export interface FitnessInfo {
  currentlyExercising?: boolean;
  currentActivity?: string;
  timePracticing?: string;
  timeStopped?: string;
  trainingLocation?: 'Academia' | 'Em casa';
  gymDetails?: {
    type?: 'Rede' | 'Bairro' | 'Condomínio';
    name?: string;
    condoPhotosSent?: boolean;
  };
  homeEquipment?: string[];
  weeklyFrequency?: number; // 1-7
  trainingTimeAvailable?: string;
}

export interface PreferencesInfo {
  dislikedExercises?: string;
  likedExercises?: string;
  cardioPreference?: 'Sim' | 'Não' | 'Mais ou menos';
  bodyPartFocus?: string;
}

export interface ClosingInfo {
  programConfidence?: string;
  programAttraction?: string;
  salesVideoPoint?: string;
}
```

## Endpoints

### 1. Consultar Anamnese
Recupera os dados atuais da ficha do aluno.

*   **Método**: `GET`
*   **URL**: `/api/usuarios/{userId}/anamnese`
*   **Autenticação**: Bearer Token Obrigatório (Aluno, Personal ou Admin).
*   **Query Params Obrigatórios**: `requesterId`, `requesterRole` (para controle de acesso).

**Exemplo de Chamada (Axios):**
```typescript
const response = await api.get<AnamnesisDTO>(`/usuarios/${userId}/anamnese`, {
  params: { requesterId: currentUser.id, requesterRole: currentUser.role }
});
// Se nunca foi preenchida, retorna um objeto vazio {} ou com campos nulos.
```

### 2. Salvar/Atualizar Anamnese
Salva a ficha completa ou atualiza campos. O backend substituirá o objeto JSON atual pelo enviado no body. Para atualizações parciais, recomenda-se: **Ler (GET) -> Modificar no Front -> Salvar (PUT)**.

*   **Método**: `PUT`
*   **URL**: `/api/usuarios/{userId}/anamnese`
*   **Headers**: `Content-Type: application/json`
*   **Body**: Objeto `AnamnesisDTO` completo.

**Exemplo de Chamada (Axios):**
```typescript
const payload: AnamnesisDTO = {
  ...anamneseAtual, // Dados lidos anteriormente
  physical: {
    ...anamneseAtual.physical,
    weight: 75.5 // Atualizando apenas o peso
  },
  updatedAt: new Date().toISOString() // O backend coloca se omitido, mas bom enviar.
};

await api.put(`/usuarios/${userId}/anamnese`, payload, {
  params: { requesterId: currentUser.id, requesterRole: currentUser.role }
});
```

## Notas de Implementação
1.  **Validação**: O backend não rejeita campos nulos. A validação de obrigatoriedade deve ser visual no frontend (warnings), permitindo que o usuário salve rascunhos.
2.  **Delay de Persistência**: Como é uma coluna JSON única, a resposta é imediata.
3.  **Permissões**:
    *   **Admin**: Pode ler/editar qualquer um.
    *   **Personal**: Pode ler/editar seus alunos.
    *   **User**: Pode ler/editar a própria ficha.
