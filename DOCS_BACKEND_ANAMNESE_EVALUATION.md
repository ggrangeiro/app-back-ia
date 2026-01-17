# Relatório de Avaliação Técnica: Anamnese Completa (V2)

## 1. Resumo Executivo
A especificação "Anamnese Completa do Aluno V2" é **tecnicamente viável** e alinhada com a arquitetura atual (Micronaut + Micronaut Data JDBC + MySQL). A recomendação é seguir com a **Abordagem 1 (Coluna JSON na tabela `usuario`)**, pois oferece melhor performance de leitura e flexibilidade para o schema dinâmico proposto.

## 2. Análise da Arquitetura Atual
*   **Framework**: Micronaut 4.10.x
*   **Persistência**: Micronaut Data JDBC
*   **Banco de Dados**: MySQL 8.x (Google Cloud SQL)
*   **Entidade Alvo**: [`Usuario.java`](src/main/java/gcfv2/Usuario.java)

## 3. Avaliação das Opções de Modelagem

### Opção A: Coluna JSON na tabela `usuario` (Recomendada)
Esta opção adiciona uma coluna `dossier` (ou `anamnesis`) do tipo `JSON` na tabela existente.

*   **Prós**:
    *   **Performance**: Leitura dos dados do usuário e sua anamnese em uma única query SQL (sem JOINs).
    *   **Flexibilidade**: O MySQL 8 suporta nativamente o tipo JSON, permitindo schemas parciais e evolução sem *migrations* complexas (excelente para os muitos campos opcionais da especificação).
    *   **Simplicidade**: Mantém o modelo de objeto do `Usuario` coeso.
*   **Contras**:
    *   Requer implementação de um `AttributeConverter` para serializar/deserializar o DTO automaticamente no Micronaut Data JDBC, já que não estamos usando Hibernate/JPA completo.

### Opção B: Tabela Separada (`anamnesis`)
*   **Prós**:
    *   Normalização estrita.
    *   Permite carregar o usuário sem carregar a anamnese (Lazy Loading), embora em JDBC puro isso precise ser manual.
*   **Contras**:
    *   Complexidade de gestão de chave estrangeira.
    *   Requires JOIN ou queries adicionais para popular o contexto da IA.

## 4. Pontos de Atenção & Requisitos Técnicos

### 4.1. Suporte a JSON no Micronaut Data JDBC
Como o projeto utiliza JDBC direto (e não JPA), a conversão objeto <-> JSON não é mágica. Será necessário:
1.  Criar a classe `AnamnesisDTO` refletindo a estrutura JSON proposta.
2.  Implementar `io.micronaut.data.model.runtime.convert.AttributeConverter<AnamnesisDTO, String>` que utiliza o `ObjectMapper` para converter o objeto em String JSON para o banco.
3.  Anotar o campo na entidade `Usuario` com `@TypeDef(type = DataType.JSON, converter = AnamnesisConverter.class)`.

### 4.2. Migration de Banco de Dados
Será necessário criar um script V10 (ex: `V10__add_anamnesis_json_to_usuario.sql`) para:
```sql
ALTER TABLE usuario ADD COLUMN anamnesis JSON NULL;
```

### 4.3. Interface com IA
Os campos mapeados para a IA (Lesões, Local, Frequência, Cardio) estarão aninhados no JSON. O `PromptBuilder` (ou serviço equivalente) precisará ser atualizado para ler:
*   `usuario.getAnamnesis().getHealth().getInjuries()`
*   `usuario.getAnamnesis().getFitness().getTrainingLocation()`

Como a anamnese é opcional, o código da IA deve ser robusto a `NullPointerException` se o usuário ainda não tiver preenchido a ficha.

## 5. Estimativa de Esforço
1.  **Backend (DTOs + Converter + Entity Update)**: Baixo (2-3 horas).
2.  **Database Migration**: Muito Baixo (30 min).
3.  **Controller (Endpoint PUT/GET)**: Baixo (1-2 horas).
4.  **Testes**: Médio (Garantir que updates parciais funcionem corretamente).

## 6. Conclusão
A proposta é sólida. Recomendo prosseguir com a implementação da **Coluna JSON**, criando os DTOs necessários e o conversor para integração com o Micronaut Data. Não há bloqueios técnicos identificados.
