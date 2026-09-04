# Farmalog API

API REST para gestão de estoque, controle de validade e vendas em farmácias
independentes.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

> ⚠️ **Projeto em desenvolvimento ativo.** A fase 1 está concluída. As demais estão
> descritas no roadmap abaixo. A modelagem é uma simplificação inspirada na operação
> real de uma farmácia e não implementa conformidade regulatória.

---

## O problema

Farmácias de pequeno porte trabalham com um estoque que não é intercambiável: duas
caixas do mesmo medicamento podem ter validades diferentes, e essa diferença muda o
que pode ser vendido. Controlar isso em planilha leva a dois prejuízos recorrentes —
produto que vence no fundo da prateleira enquanto o lote novo é vendido primeiro, e
venda de item vencido.

O Farmalog resolve isso rastreando o estoque por lote, e não por produto.

---

## Decisões de arquitetura

As escolhas abaixo são o núcleo do projeto — mais do que a lista de tecnologias.

**Estoque por lote, não por produto.** A unidade de controle é o lote, com sua própria
validade e saldo. A quantidade disponível de um produto é a soma dos lotes não vencidos.

**Estoque só muda por movimentação.** Nenhum ponto do código altera o saldo de um lote
diretamente. Toda alteração gera um registro de movimentação com tipo, responsável e
data, formando um histórico auditável. O saldo é consequência do histórico, não um
número solto.

**Baixa por FEFO.** A venda consome primeiro o lote que vence antes, ignorando os
vencidos. É o comportamento correto do negócio e evita perda por validade.

**Organização por camada.** O código é dividido em `controller`, `service`,
`repository`, `entity` e `dto` — cada tipo de responsabilidade no seu pacote, com o
tratamento de erro isolado em `validation`.

**Entidade não aparece no controller.** Os endpoints trabalham exclusivamente com
DTOs — `records` separados para entrada, saída e filtro. O mapeamento entre DTO e
entidade fica nos próprios DTOs (`toEntity` / `fromEntity`).

**Erro tratado num ponto só.** Um `@RestControllerAdvice` traduz falha de validação e
de negócio para respostas JSON previsíveis: `{ status, message }` para regra de
negócio, lista de `{ field, message }` para validação. Nenhum stack trace vaza.

**Schema versionado com Flyway.** `ddl-auto` está em `validate`. O schema é definido em
migrations versionadas, nunca inferido pelo Hibernate — o banco é reproduzível em
qualquer máquina a partir do repositório.

**Nenhuma credencial versionada.** A configuração vem de variáveis de ambiente; o
repositório traz apenas um `.env.example` com valores fictícios. O banco de testes
sobe em container, sem senha no código.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | PostgreSQL 17 |
| Migrations | Flyway |
| Segurança | Spring Security + OAuth2 Resource Server (JWT RS256) |
| Documentação | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Testes | JUnit 5, Mockito, Spring Boot Test |
| Infra local | Docker Compose |

Planejado para as próximas fases: pipeline no GitHub Actions e deploy.

---

## Como executar

### Pré-requisitos

- JDK 21
- Docker e Docker Compose

### Passos

```bash
# 1. Configure as variáveis de ambiente
cp .env.example .env

# 2. Rode a aplicação
./mvnw spring-boot:run
```

A aplicação sobe o container do PostgreSQL automaticamente (via
`spring-boot-docker-compose`), aplica as migrations do Flyway e fica disponível em
`http://localhost:8080`. A documentação interativa fica em
`http://localhost:8080/swagger-ui.html`.

As chaves RSA que assinam os JWT ficam fora do versionamento. Gere-as antes de
subir a aplicação:

```bash
bash scripts/gen-keys.sh
```

A migration cria três usuários de demonstração (senha `farmalog123`):
`gerente@farmalog.dev`, `farmaceutico@farmalog.dev` e `atendente@farmalog.dev`.
Autentique em `POST /auth/login` e envie o token no header `Authorization: Bearer`.

### Testes

```bash
./mvnw test
```

Os testes de integração sobem um PostgreSQL em container; o Docker precisa estar
em execução.

---

## Endpoints disponíveis

Implementados até aqui:

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | público | Autentica por email e senha, devolve um JWT |
| `GET` | `/auth/me` | autenticado | Email e perfil do usuário do token |
| `GET` | `/api/v1/produtos` | autenticado | Lista produtos — paginado, com filtros por nome, princípio ativo, exigência e ativo |
| `GET` | `/api/v1/produtos/{id}` | autenticado | Busca um produto por ID |
| `POST` | `/api/v1/produtos` | GERENTE | Cadastra um produto |
| `PUT` | `/api/v1/produtos/{id}` | GERENTE | Atualiza um produto |
| `DELETE` | `/api/v1/produtos/{id}` | GERENTE | Desativa um produto (exclusão lógica) |
| `GET` `POST` `PUT` `DELETE` | `/api/v1/usuarios` | GERENTE | CRUD de usuários |

**Exemplo — cadastro de produto**

```http
POST /api/v1/produtos
Content-Type: application/json

{
  "nome": "Dipirona Monoidratada 500mg",
  "principioAtivo": "Dipirona Sódica",
  "fabricante": "Medley",
  "codigoBarras": "7896422503457",
  "precoVenda": 12.90,
  "exigencia": "ISENTO",
  "estoqueMinimo": 20
}
```

O campo `exigencia` aceita `ISENTO`, `RECEITA_SIMPLES`, `RECEITA_RETIDA` e
`CONTROLADO`, e é o que determina se a venda exigirá registro de receita e
liberação por farmacêutico.

---

## Roadmap

- [x] **Fase 1 — Fundação**
      PostgreSQL em container, Flyway, CRUD de produto com DTOs e validação,
      listagem paginada com filtros, tratamento centralizado de erro e
      documentação OpenAPI.

- [x] **Fase 2 — Segurança**
      Usuários no banco, BCrypt, JWT assinado com par de chaves RSA, login e
      autorização por perfil (atendente, farmacêutico, gerente).

- [ ] **Fase 3 — Estoque**
      Entrada de lote, movimentação como registro imutável, cálculo de
      disponibilidade ignorando vencidos, alertas de validade e estoque mínimo.

- [ ] **Fase 4 — Vendas**
      Fluxo de venda com baixa FEFO, atomicidade, regra de receita, cancelamento
      com estorno nos lotes de origem e controle de concorrência.

- [ ] **Fase 5 — Acabamento**
      Relatórios e cobertura de testes de integração das regras de negócio.

- [ ] **Fase 6 — Entrega**
      Dockerfile, pipeline no GitHub Actions e deploy.

A especificação completa do domínio, com as regras de negócio numeradas, está em
[`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md).

---

## Autor

**Gabriel Lourenço** — Desenvolvedor Back-End

[GitHub](https://github.com/ggabmartins) · [LinkedIn](https://www.linkedin.com/in/ggabmartins/)
