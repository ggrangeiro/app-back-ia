# Documentação de Implementação Frontend: Assinaturas e Planos

Este documento detalha como implementar o fluxo de contratação de planos e assinaturas no frontend, integrando com o backend Java e o Mercado Pago.

## Visão Geral do Fluxo

1.  **Listagem de Planos**: O frontend busca os planos disponíveis na API.
2.  **Seleção e Checkout**: O usuário escolhe um plano e o frontend solicita a criação de uma "Preferência de Pagamento" ao backend.
3.  **Redirecionamento Mercado Pago**: O backend retorna um link (`initPoint`) para o checkout do Mercado Pago. O frontend abre este link.
4.  **Conclusão**: O usuário paga no Mercado Pago e é redirecionado de volta para o app (Sucesso/Falha/Pendente).
5.  **Atualização**: O backend processa o pagamento via Webhok e atualiza o status do usuário. O frontend deve atualizar os dados do usuário (`/api/me`) para refletir o novo plano.

---

## 1. Listagem de Planos

Busque os planos disponíveis para exibir na tela de "Seja Pro".

### Endpoint
`GET /api/plans`

### Exemplo de Resposta
```json
{
  "plans": [
    {
      "id": "FREE",
      "name": "Gratuito",
      "price": 0.0,
      "credits": 0,
      "features": []
    },
    {
      "id": "STARTER",
      "name": "Starter",
      "price": 59.90,
      "credits": 30,
      "features": ["30 análises de vídeo/mês", "10 gerações de treino/dieta"]
    },
    {
      "id": "PRO",
      "name": "Pro",
      "price": 99.90,
      "credits": 80,
      "features": ["80 análises de vídeo/mês", "Gerações ilimitadas", "Relatórios PDF"]
    },
    {
      "id": "STUDIO",
      "name": "Studio",
      "price": 199.90,
      "credits": 200,
      "features": ["200 análises de vídeo/mês", "Gerações ilimitadas", "White Label"]
    }
  ]
}
```

---

## 2. Iniciar Assinatura (Checkout)

Quando o usuário clicar em "Assinar" em um dos planos.

### Endpoint
`POST /api/checkout/create-preference?userId={ID_DO_USUARIO}`

### Payload (Body)
```json
{
  "planId": "PRO"
}
```
*(Use o `id` retornado na listagem de planos: "STARTER", "PRO", ou "STUDIO")*

### Resposta (Sucesso)
```json
{
  "preferenceId": "123456789-abcd-efgh...",
  "initPoint": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.br/checkout/v1/redirect?...",
  "plan": {
      "id": "PRO",
      "name": "Pro",
      "price": 99.90
  }
}
```

### Ação do Frontend
1.  Receba o `initPoint` (produção) ou `sandboxInitPoint` (desenvolvimento).
2.  Redirecione o usuário para essa URL (ou abra em um WebView/Aba do navegador).

---

## 3. Retorno do Pagamento (Callback)

O Mercado Pago redirecionará o usuário de volta para o app nas URLs configuradas no backend (`appConfig` ou `application.yml`).

-   **Sucesso**: `/checkout/success`
-   **Falha**: `/checkout/failure`
-   **Pendente**: `/checkout/pending`

**Recomendação**: Crie uma rota no Frontend (ex: `/payment/callback`) que identifique o status e mostre uma mensagem apropriada ("Pagamento confirmado!", "Houve um erro", etc.), e então redirecione para a Home ou Profile.

---

## 4. Atualização de Status

Após o pagamento bem-sucedido, o status do usuário não atualiza *instantaneamente* no frontend até que o webhook seja processado.

### Lógica Sugerida:
1.  Ao retornar do Mercado Pago com sucesso, exiba um loader ("Confirmando assinatura...").
2.  Faça polling (chamadas repetidas a cada 2s) no endpoint `/api/me?userId={ID}` por alguns segundos.
3.  Verifique se `user.plan.type` mudou para o plano novo e `user.plan.status` é `ACTIVE`.
4.  Se atualizar, mostre "Assinatura Ativada!" e libere o acesso.

### Dados do Usuário (`/api/me`)
```json
{
  "id": 1,
  "name": "Usuário Teste",
  "plan": {
    "type": "PRO",
    "status": "ACTIVE",       // ACTIVE, INACTIVE, CANCELED
    "renewsAt": "2026-02-15T10:00:00"
  },
  "usage": {
    "credits": 80,            // Créditos totais
    "subscriptionCredits": 80 // Parte da assinatura
  }
}
```

---

## 5. Tipos TypeScript (Sugestão)

Use estas interfaces para tipar seu código:

```typescript
export interface Plan {
  id: string; // 'FREE' | 'STARTER' | 'PRO' | 'STUDIO'
  name: string;
  price: number;
  credits: number;
  features: string[];
}

export interface CheckoutResponse {
  preferenceId: string;
  initPoint: string;      // Link oficial
  sandboxInitPoint: string; // Link de teste
}

export interface UserSubscription {
  type: string;
  status: 'ACTIVE' | 'INACTIVE' | 'CANCELED';
  renewsAt: string; // ISO Date
}
```

## Resumo da Implementação

1.  Crie um componente `PlanCard` para exibir as opções vindas de `/api/plans`.
2.  No clique do botão "Assinar", chame `createSubscriptionPreference(planId)`.
3.  Use `window.location.href = response.initPoint` para levar ao Mercado Pago.
4.  Crie uma página de feedback (Sucesso/Erro) para onde o usuário volta.
5.  Atualize o contexto global de usuário (`userContext`) após a confirmação.
