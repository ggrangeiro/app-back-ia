# Documentação Técnica: Integração de Assets e Branding (White Label)

Esta documentação detalha os novos endpoints e alterações nos payloads para a integração das funcionalidades de Foto de Perfil (Avatar) e Logomarca do Personal (White Label).

---

## 1. Upload de Imagens (Avatar e Logo)

Utilize este endpoint para realizar o upload de fotos de perfil ou logos de marca.

- **URL:** `POST /api/usuarios/{id}/upload-asset`
- **Content-Type:** `multipart/form-data`
- **Query Parameters:**
    - `requesterId`: ID do usuário que está realizando a ação.
    - `requesterRole`: Role do usuário (`USER`, `PERSONAL` ou `ADMIN`).

### Payload (Form Data)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `file` | `File` | Arquivo de imagem (JPG ou PNG, máx 2MB). |
| `type` | `String` | `'avatar'` para foto de perfil ou `'logo'` para marca. |

### Regras de Negócio:
1. **Avatar:** Qualquer usuário pode subir seu próprio avatar (ou ADMIN).
2. **Logo:** Apenas usuários com role `PERSONAL` ou `ADMIN` podem realizar o upload.
3. **Limites:** O arquivo deve ter no máximo 2MB e ser uma imagem válida.

### Resposta de Sucesso (200 OK):
```json
{
  "success": true,
  "imageUrl": "/api/assets/assets/avatar/hash_gerado.png"
}
```

---

## 2. Consulta de Perfil e Branding

O backend agora serve as imagens diretamente (Proxy) para evitar problemas de CORS.

### 2.1. Endpoint de Perfil Logado (`/api/me`)
Retorna os dados do usuário atual enriquecidos com a foto de perfil e a logo da marca aplicada.

- **URL:** `GET /api/me?userId={id}`

### 2.2. Detalhes do Usuário (`GET /api/usuarios/{id}`)
Retorna os detalhes de um usuário específico.

---

## 3. Lógica de Exibição no Frontend (White Label)

O backend agora automatiza a lógica de "Mestre x Aluno".

### Comportamento do Campo `brandLogo`:
1. **Para Personais:** Retorna a path da imagem que ele mesmo subiu.
2. **Para Alunos (`role: 'USER'`):** 
    - Se o personal vinculado tiver uma logo cadastrada, o backend retornará essa path no campo `brandLogo` do payload do aluno.
    - Se não houver personal ou logo cadastrada, retornará uma string vazia `""`.

### Estrutura do Payload Atualizada:
```json
{
  "id": 123,
  "name": "João Silva",
  "role": "USER",
  "avatar": "/api/assets/assets/avatar/...", // Proxy Path
  "brandLogo": "/api/assets/assets/logo/...", // Proxy Path
  "personalId": 456,
  "plan": { ... },
  "usage": { ... }
}
```

---

## 4. Recomendações para o Frontend

1. **Proxy Backend:**
   - As imagens agora são servidas pelo próprio backend na rota `/api/assets/...`.
   - Você pode usar o valor retornado diretamente no `src` da tag `<img>` (ex: `<img src="https://api.domain.com/api/assets/..." />`).
2. **Componentes de Avatar:**
   - Substitua ícones genéricos (ex: `UserCircle`) pela imagem do campo `avatar`. Se `avatar` estiver vazio, mantenha o fallback visual.
3. **Cache:**
   - Como as URLs são fixas após o upload e servidas via GCS, o cache do navegador lidará bem com a performance, mas considere invalidar o estado local do perfil após o sucesso de um novo upload.

---
> [!IMPORTANT]
> O ambiente produtivo já entrega URLs públicas via Google Cloud Storage (`storage.googleapis.com`). Certifique-se de que não há bloqueios de Content Security Policy (CSP) para este domínio.
