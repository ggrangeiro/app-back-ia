# Documentação: Sistema de Controle de Créditos por Plano

## Visão Geral

Este documento descreve como o frontend deve utilizar as rotas de débito de créditos considerando as regras de negócio por plano.

---

## Regras de Negócio

### Tabela de Consumo por Plano

| Plano | Dieta/Treino | Análises (vídeo) |
|-------|--------------|------------------|
| **FREE** | Cobra 1 crédito | Cobra 1 crédito |
| **STARTER** | 10 gratuitas/mês, depois cobra 1 crédito | Cobra 1 crédito |
| **PRO** | **GRÁTIS** (ilimitado) | Cobra 1 crédito |
| **STUDIO** | **GRÁTIS** (ilimitado) | Cobra 1 crédito |

### Regra Especial para Personal Trainers

Quando um **Personal Trainer** (role=PERSONAL) gera dieta ou treino para seu **aluno**:
- O sistema verifica o **plano do Personal**, não do aluno
- Exemplo: Personal PRO gerando para aluno FREE → **não cobra crédito**

---

## Rotas da API

### 1. Consumir Crédito (Débito)

```
POST /api/usuarios/consume-credit/{userId}
```

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `requesterId` | Long | ✓ | ID do usuário que está fazendo a requisição |
| `requesterRole` | String | ✓ | Role do requester: `USER`, `PERSONAL`, `ADMIN` |
| `reason` | String | ✓ | Motivo do débito: `DIETA`, `TREINO`, ou `ANALISE` |
| `analysisType` | String | Se reason=ANALISE | Tipo específico da análise |

**Tipos de Análise (`analysisType`):**
- `BODY_COMPOSITION` - Composição Corporal
- `POSTURAL_ASSESSMENT` - Avaliação Postural
- `SQUAT_ANALYSIS` - Análise de Agachamento
- `BENCH_PRESS` - Supino
- `DEADLIFT` - Levantamento Terra
- `FREE_ANALYSIS` - Análise Livre
- *(outros exercícios)*

**Exemplo de Requisição (Dieta):**
```bash
POST /api/usuarios/consume-credit/123?requesterId=456&requesterRole=PERSONAL&reason=DIETA
```

**Exemplo de Requisição (Análise):**
```bash
POST /api/usuarios/consume-credit/123?requesterId=123&requesterRole=USER&reason=ANALISE&analysisType=BODY_COMPOSITION
```

**Resposta de Sucesso (200 OK):**
```json
{
  "message": "Geração gratuita utilizada",
  "novoSaldo": 25,
  "creditsConsumed": 0,
  "wasFree": true,
  "reason": "DIETA",
  "freeGenerationsRemaining": 9,
  "subscriptionCredits": 20,
  "purchasedCredits": 5
}
```

**Campos da Resposta:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `message` | String | Mensagem de feedback |
| `novoSaldo` | Integer | Saldo total após operação |
| `creditsConsumed` | Integer | Créditos efetivamente debitados (0 ou 1) |
| `wasFree` | Boolean | Se usou geração gratuita |
| `reason` | String | Motivo passado na requisição |
| `freeGenerationsRemaining` | Integer | Gerações gratuitas restantes no mês (STARTER) |
| `subscriptionCredits` | Integer | Créditos da assinatura |
| `purchasedCredits` | Integer | Créditos avulsos comprados |

**Erros Possíveis:**

| Status | Mensagem | Descrição |
|--------|----------|-----------|
| `400 Bad Request` | Parâmetro 'reason' inválido | `reason` deve ser DIETA, TREINO ou ANALISE |
| `400 Bad Request` | Parâmetro 'analysisType' é obrigatório | Falta `analysisType` quando reason=ANALISE |
| `402 Payment Required` | Saldo insuficiente | Usuário sem créditos |
| `403 Forbidden` | Acesso negado | Sem permissão para débito |

---

### 2. Consultar Histórico de Consumo

```
GET /api/usuarios/credit-history/{userId}
```

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `requesterId` | Long | ✓ | ID do usuário que está fazendo a requisição |
| `requesterRole` | String | ✓ | Role do requester: `USER`, `PERSONAL`, `ADMIN` |

**Exemplo de Requisição:**
```bash
GET /api/usuarios/credit-history/123?requesterId=123&requesterRole=USER
```

**Resposta de Sucesso (200 OK):**
```json
{
  "history": [
    {
      "id": 1,
      "userId": 123,
      "reason": "DIETA",
      "analysisType": null,
      "creditsConsumed": 0,
      "wasFree": true,
      "creditSource": "FREE",
      "createdAt": "2026-01-12T15:30:00"
    },
    {
      "id": 2,
      "userId": 123,
      "reason": "ANALISE",
      "analysisType": "BODY_COMPOSITION",
      "creditsConsumed": 1,
      "wasFree": false,
      "creditSource": "SUBSCRIPTION",
      "createdAt": "2026-01-12T14:00:00"
    }
  ],
  "summary": {
    "totalConsumed": 5,
    "freeGenerationsUsed": 3,
    "freeGenerationsRemaining": 7,
    "consumedThisMonth": 2
  }
}
```

**Campos do Histórico:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | Long | ID do registro |
| `userId` | Long | ID do usuário |
| `reason` | String | DIETA, TREINO ou ANALISE |
| `analysisType` | String | Tipo de análise (se aplicável) |
| `creditsConsumed` | Integer | Créditos debitados (0 ou 1) |
| `wasFree` | Boolean | Se foi geração gratuita |
| `creditSource` | String | SUBSCRIPTION, PURCHASED ou FREE |
| `createdAt` | DateTime | Timestamp do consumo |

**Campos do Summary:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `totalConsumed` | Long | Total de créditos já consumidos |
| `freeGenerationsUsed` | Long | Gerações gratuitas usadas no mês |
| `freeGenerationsRemaining` | Integer | Restante (STARTER=10-usadas, PRO/STUDIO=-1) |
| `consumedThisMonth` | Long | Créditos consumidos no mês atual |

---

## Fluxo de Integração

### Fluxo para Geração de Dieta/Treino

```
┌─────────────────┐     ┌────────────────────────────┐     ┌─────────────────┐
│  1. Clica em    │────▶│ 2. POST consume-credit     │────▶│ 3. Verifica     │
│  "Gerar Dieta"  │     │    reason=DIETA            │     │    Resposta     │
└─────────────────┘     └────────────────────────────┘     └────────┬────────┘
                                                                     │
                              ┌──────────────────────────────────────┴───────────┐
                              ▼                                                  ▼
                    ┌─────────────────┐                             ┌─────────────────┐
                    │ wasFree=true OR │                             │ status=402      │
                    │ creditsConsumed │                             │ Saldo           │
                    │ =1 ? SUCESSO    │                             │ Insuficiente    │
                    └────────┬────────┘                             └─────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ 4. Chamar API   │
                    │    Gemini       │
                    │    (gerar IA)   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ 5. Salvar Dieta │
                    │    /api/dietas/ │
                    └─────────────────┘
```

### Fluxo para Análise de Vídeo

```
┌─────────────────┐     ┌────────────────────────────┐     ┌─────────────────┐
│  1. Upload do   │────▶│ 2. POST consume-credit     │────▶│ 3. Verifica     │
│  Vídeo          │     │    reason=ANALISE          │     │    Resposta     │
│                 │     │    analysisType=SQUAT      │     │                 │
└─────────────────┘     └────────────────────────────┘     └────────┬────────┘
                                                                     │
                              ┌──────────────────────────────────────┴───────────┐
                              ▼                                                  ▼
                    ┌─────────────────┐                             ┌─────────────────┐
                    │ creditsConsumed │                             │ status=402      │
                    │ =1 ? SUCESSO    │                             │ Saldo           │
                    │ (análise sempre │                             │ Insuficiente    │
                    │  consome)       │                             └─────────────────┘
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ 4. Processar    │
                    │    Vídeo com IA │
                    └─────────────────┘
```

---

## Exemplos de Código (TypeScript/React)

### Função de Consumo de Crédito

```typescript
interface ConsumeCreditsParams {
  userId: number;
  requesterId: number;
  requesterRole: 'USER' | 'PERSONAL' | 'ADMIN';
  reason: 'DIETA' | 'TREINO' | 'ANALISE';
  analysisType?: string;
}

interface ConsumeCreditsResponse {
  message: string;
  novoSaldo: number;
  creditsConsumed: number;
  wasFree: boolean;
  reason: string;
  freeGenerationsRemaining: number;
  subscriptionCredits: number;
  purchasedCredits: number;
}

async function consumeCredits(params: ConsumeCreditsParams): Promise<ConsumeCreditsResponse> {
  const queryParams = new URLSearchParams({
    requesterId: params.requesterId.toString(),
    requesterRole: params.requesterRole,
    reason: params.reason,
    ...(params.analysisType && { analysisType: params.analysisType }),
  });

  const response = await fetch(
    `${API_URL}/api/usuarios/consume-credit/${params.userId}?${queryParams}`,
    { method: 'POST' }
  );

  if (!response.ok) {
    if (response.status === 402) {
      throw new Error('Saldo insuficiente. Adquira mais créditos.');
    }
    const error = await response.json();
    throw new Error(error.message);
  }

  return response.json();
}
```

### Uso no Componente

```typescript
// Antes de gerar dieta
async function handleGenerateDiet() {
  try {
    const result = await consumeCredits({
      userId: targetUser.id,
      requesterId: currentUser.id,
      requesterRole: currentUser.role,
      reason: 'DIETA',
    });

    if (result.wasFree) {
      toast.success(`Geração gratuita! Restam ${result.freeGenerationsRemaining}`);
    } else {
      toast.success(`1 crédito debitado. Saldo: ${result.novoSaldo}`);
    }

    // Continuar com geração da dieta via IA
    await generateDietWithAI(targetUser);

. } catch (error) {
    toast.error(error.message);
  }
}

// Antes de análise de vídeo
async function handleAnalyzeVideo(exerciseType: string) {
  try {
    const result = await consumeCredits({
      userId: currentUser.id,
      requesterId: currentUser.id,
      requesterRole: currentUser.role,
      reason: 'ANALISE',
      analysisType: exerciseType, // ex: 'SQUAT_ANALYSIS'
    });

    toast.success(`Crédito debitado. Saldo: ${result.novoSaldo}`);
    
    // Continuar com análise do vídeo
    await analyzeVideoWithAI(videoFile, exerciseType);

  } catch (error) {
    toast.error(error.message);
  }
}
```

---

## Considerações Importantes

1. **Sempre chamar `consume-credit` ANTES** de processar a IA
2. Se retornar erro 402, **não prosseguir** com a geração
3. O campo `wasFree` indica se foi geração gratuita (PRO/STUDIO ou Starter com quota)
4. O campo `freeGenerationsRemaining` é útil para mostrar contador no UI
5. Para ANALISE, sempre passar `analysisType` para fins de rastreamento
6. O histórico pode ser usado para mostrar consumo ao usuário

---

## Tabela de Migração (Banco de Dados)

A nova tabela `credit_consumption_history` será criada automaticamente pela migration V6:

```sql
CREATE TABLE credit_consumption_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,          -- DIETA, TREINO, ANALISE
    analysis_type VARCHAR(100) NULL,      -- Tipo específico de análise
    credits_consumed INT NOT NULL,        -- 0 ou 1
    was_free BOOLEAN DEFAULT FALSE,       -- Se usou geração gratuita
    credit_source VARCHAR(20) NOT NULL,   -- SUBSCRIPTION, PURCHASED, FREE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES usuario(id)
);
```
