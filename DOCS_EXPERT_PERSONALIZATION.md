# Documentação Técnica: Personalização da IA do Expert

Esta documentação descreve os novos campos adicionados ao perfil do usuário para permitir que Personal Trainers (Experts) definam sua metodologia e estilo de comunicação. Esses dados serão utilizados para personalizar a geração de treinos e dietas pela IA.

## Novos Campos

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `methodology` | `string` (TEXT) | Define a metodologia de trabalho do personal. Ex: "Foco em progressão de carga", "Bi-sets". |
| `communicationStyle` | `string` (TEXT) | Define o tom de voz e estilo de comunicação. Ex: "Motivador", "Sargento", "Uso de emojis". |

## Integração Frontend

### 1. Leitura (GET)

Ao buscar os dados do usuário, os novos campos estarão disponíveis no objeto JSON de resposta.

**Endpoint:** `GET /api/usuarios/{id}`

**Response Example:**
```json
{
  "id": 123,
  "name": "Carlos Personal",
  "email": "carlos@fitai.com",
  "role": "personal",
  "methodology": "Priorizo execução perfeita com cargas moderadas.",
  "communicationStyle": "Seja direto e use termos técnicos.",
  ...
}
```

### 2. Atualização (PUT)

Para salvar as preferências do personal, envie os campos no corpo da requisição PUT. A atualização é parcial (campos não enviados não são alterados, mas se enviados como null podem anular o valor).

**Endpoint:** `PUT /api/usuarios/{id}`

**Payload Example:**
```json
{
  "methodology": "Priorizo execução perfeita com cargas moderadas.",
  "communicationStyle": "Seja direto e use termos técnicos."
}
```

**Regras de Acesso:**
- O próprio usuário (Personal) pode editar seu perfil.
- Um usuário ADMIN pode editar qualquer perfil.

## Onde Implementar no Frontend?

- **Perfil do Usuário / Configurações do Expert**: Adicionar dois campos de texto (Textarea) nas configurações de perfil para que o personal possa preencher essas informações.
