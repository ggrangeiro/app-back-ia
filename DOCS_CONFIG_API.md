# Documentação: API de Gerenciamento de Configurações

## Visão Geral

Esta API permite gerenciar configurações da aplicação armazenadas no banco de dados, como a **API Key do Gemini**. Isso elimina a necessidade de rebuild do frontend para atualizar chaves.

---

## Base URL

```
https://app-back-ia-732767853162.southamerica-east1.run.app
```

---

## Autenticação

Todas as rotas requerem autenticação via **query parameters**:

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `requesterId` | Long | ✓ | ID do usuário logado |
| `requesterRole` | String | ✓ | Role do usuário: `USER`, `PERSONAL`, ou `ADMIN` |

---

## Endpoints

### 1. Buscar Configuração (GET)

Retorna o valor de uma configuração pela chave.

```
GET /api/config/{key}?requesterId={id}&requesterRole={role}
```

**Exemplo - Buscar API Key do Gemini:**
```bash
GET /api/config/GEMINI_API_KEY?requesterId=123&requesterRole=USER
```

**Resposta (200 OK):**
```json
{
  "key": "GEMINI_API_KEY",
  "value": "AIzaSyAGfq7owXya1J7AxkdWk_N_uPE259cJZUM"
}
```

**Resposta (404 Not Found):**
```json
{
  "error": "CONFIG_NOT_FOUND",
  "message": "Configuração não encontrada"
}
```

**Resposta (401 Unauthorized):**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Usuário não encontrado"
}
```

---

### 2. Atualizar Configuração (PUT) - Apenas ADMIN

Atualiza o valor de uma configuração existente.

```
PUT /api/config/{key}?requesterId={id}&requesterRole=ADMIN
Content-Type: application/json
```

**Body:**
```json
{
  "value": "NOVA_API_KEY_AQUI"
}
```

**Exemplo:**
```bash
PUT /api/config/GEMINI_API_KEY?requesterId=1&requesterRole=ADMIN
Content-Type: application/json

{"value": "AIzaSy_NOVA_CHAVE_12345"}
```

**Resposta (200 OK):**
```json
{
  "key": "GEMINI_API_KEY",
  "value": "AIzaSy_NOVA_CHAVE_12345",
  "updatedAt": "2026-01-14T14:30:00"
}
```

**Resposta (403 Forbidden):**
```json
{
  "error": "FORBIDDEN",
  "message": "Apenas administradores podem atualizar configurações"
}
```

---

### 3. Criar Configuração (POST) - Apenas ADMIN

Cria uma nova configuração no sistema.

```
POST /api/config/?requesterId={id}&requesterRole=ADMIN
Content-Type: application/json
```

**Body:**
```json
{
  "key": "NOVA_CONFIG",
  "value": "valor_aqui",
  "description": "Descrição opcional",
  "isSensitive": true
}
```

**Resposta (201 Created):**
```json
{
  "key": "NOVA_CONFIG",
  "value": "valor_aqui",
  "message": "Configuração criada com sucesso"
}
```

**Resposta (409 Conflict):**
```json
{
  "error": "ALREADY_EXISTS",
  "message": "Configuração já existe"
}
```

---

## Configurações Disponíveis

| Chave | Descrição | Sensível |
|-------|-----------|----------|
| `GEMINI_API_KEY` | Chave da API do Google Gemini para análises de IA | Sim |

---

## Exemplo de Integração (TypeScript/React)

### Serviço de Configuração

```typescript
const API_URL = 'https://app-back-ia-732767853162.southamerica-east1.run.app';

interface ConfigResponse {
  key: string;
  value: string;
}

class ConfigService {
  private static geminiApiKey: string | null = null;

  /**
   * Busca a API Key do Gemini do backend.
   * Faz cache em memória para evitar chamadas repetidas.
   */
  static async getGeminiApiKey(userId: number, userRole: string): Promise<string> {
    // Retorna do cache se já tiver
    if (this.geminiApiKey) {
      return this.geminiApiKey;
    }

    const url = `${API_URL}/api/config/GEMINI_API_KEY?requesterId=${userId}&requesterRole=${userRole}`;
    
    const response = await fetch(url);
    
    if (!response.ok) {
      throw new Error('Falha ao buscar API Key do Gemini');
    }
    
    const data: ConfigResponse = await response.json();
    this.geminiApiKey = data.value;
    
    return this.geminiApiKey;
  }

  /**
   * Limpa o cache forçando nova busca na próxima chamada.
   * Útil quando a chave é atualizada ou em caso de erro.
   */
  static clearCache(): void {
    this.geminiApiKey = null;
  }
}

export default ConfigService;
```

### Uso no Componente

```typescript
import ConfigService from './services/ConfigService';

async function analyzeVideo(userId: number, userRole: string) {
  try {
    // Buscar a API Key do backend
    const apiKey = await ConfigService.getGeminiApiKey(userId, userRole);
    
    // Usar a chave com o Gemini
    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
    
    // ... continuar com a análise
    
  } catch (error) {
    console.error('Erro ao buscar API Key:', error);
    // Limpar cache e tentar novamente ou mostrar erro
    ConfigService.clearCache();
  }
}
```

### Tratamento de Erros

```typescript
async function getApiKeyWithRetry(userId: number, userRole: string, maxRetries = 3): Promise<string> {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await ConfigService.getGeminiApiKey(userId, userRole);
    } catch (error) {
      if (attempt === maxRetries) {
        throw error;
      }
      // Limpar cache e aguardar antes de tentar novamente
      ConfigService.clearCache();
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
  throw new Error('Falha ao buscar API Key após múltiplas tentativas');
}
```

---

## Fluxo de Integração

```
┌─────────────────┐     ┌────────────────────────────┐     ┌─────────────────┐
│  1. App Inicia  │────▶│ 2. GET /api/config/        │────▶│ 3. Armazena em  │
│  ou Usuário     │     │    GEMINI_API_KEY          │     │    memória      │
│  Faz Login      │     └────────────────────────────┘     └────────┬────────┘
└─────────────────┘                                                  │
                                                                     ▼
                    ┌─────────────────┐     ┌─────────────────┐
                    │ 5. Usa a chave  │◀────│ 4. Quando       │
                    │    com Gemini   │     │    precisar     │
                    └─────────────────┘     │    analisar     │
                                            └─────────────────┘
```

---

## Notas Importantes

1. **Cache**: Recomendado armazenar a chave em memória (não localStorage) por segurança
2. **Refresh**: Se receber erro 401/403 do Gemini, limpar cache e buscar novamente
3. **ADMIN**: Apenas usuários com `requesterRole=ADMIN` podem atualizar a chave
4. **Segurança**: A chave fica exposta no navegador, mas isso é aceitável para este caso de uso

---

## Contato

**Backend**: Implementado e disponível para uso.  
**Última atualização**: 14/01/2026
