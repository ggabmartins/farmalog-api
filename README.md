\# Farmalog API



API REST para gestão de estoque, controle de validade e vendas em farmácias

independentes.



!\[Java](https://img.shields.io/badge/Java-21-orange)

!\[Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F)

!\[PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)

!\[Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

!\[Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)



> ⚠️ \*\*Projeto em desenvolvimento ativo.\*\* A fase 1 está concluída. As demais estão

> descritas no roadmap abaixo. A modelagem é uma simplificação inspirada na operação

> real de uma farmácia e não implementa conformidade regulatória.



\---



\## O problema



Farmácias de pequeno porte trabalham com um estoque que não é intercambiável: duas

caixas do mesmo medicamento podem ter validades diferentes, e essa diferença muda o

que pode ser vendido. Controlar isso em planilha leva a dois prejuízos recorrentes —

produto que vence no fundo da prateleira enquanto o lote novo é vendido primeiro, e

venda de item vencido.



O Farmalog resolve isso rastreando o estoque por lote, e não por produto.



\---



\## Decisões de arquitetura



As escolhas abaixo são o núcleo do projeto — mais do que a lista de tecnologias.



\*\*Estoque por lote, não por produto.\*\* A unidade de controle é o lote, com sua própria

validade e saldo. A quantidade disponível de um produto é a soma dos lotes não vencidos.



\*\*Estoque só muda por movimentação.\*\* Nenhum ponto do código altera o saldo de um lote

diretamente. Toda alteração gera um registro de movimentação com tipo, responsável e

data, formando um histórico auditável. O saldo é consequência do histórico, não um

número solto.



\*\*Baixa por FEFO.\*\* A venda consome primeiro o lote que vence antes, ignorando os

vencidos. É o comportamento correto do negócio e evita perda por validade.



\*\*Pacote por feature.\*\* O código é organizado por domínio (`produto`, `lote`, `venda`),

não por camada técnica. Cada feature carrega seu controller, service, repository,

entidade e DTOs. Isso mantém junto o que muda junto.



\*\*Entidade nunca sai do service.\*\* Controllers trabalham exclusivamente com DTOs —

`records` separados para entrada, saída e filtro. A entidade JPA não aparece em nenhuma

assinatura pública.



\*\*Schema versionado com Flyway.\*\* `ddl-auto` está em `validate`. O schema é definido em

migrations versionadas, nunca inferido pelo Hibernate — o banco é reproduzível em

qualquer máquina a partir do repositório.



\*\*Nenhuma credencial versionada.\*\* A configuração vem de variáveis de ambiente. O

repositório traz apenas um `.env.example` com valores fictícios.



\---



\## Stack



| Camada | Tecnologia |

|---|---|

| Linguagem | Java 21 |

| Framework | Spring Boot 3.4.5 |

| Persistência | Spring Data JPA / Hibernate |

| Banco | PostgreSQL 17 |

| Migrations | Flyway |

| Build | Maven |

| Testes | JUnit 5, Mockito, Spring Boot Test |

| Infra local | Docker Compose |



Planejado para as próximas fases: Spring Security com OAuth2 Resource Server (JWT),

springdoc-openapi, Testcontainers e GitHub Actions.



\---



\## Como executar



\### Pré-requisitos



\- JDK 21

\- Docker e Docker Compose



\### Passos



```bash

\# 1. Configure as variáveis de ambiente

cp .env.example .env



\# 2. Suba o PostgreSQL

docker compose up -d



\# 3. Rode a aplicação

./mvnw spring-boot:run

```



A API sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente

na inicialização.



\### Testes



```bash

./mvnw test

```



\### Perfis



| Perfil | Uso |

|---|---|

| `dev` | Desenvolvimento local (padrão) |

| `test` | Execução dos testes |

| `prod` | Produção — exige todas as variáveis, sem valores padrão |



\---



\## Endpoints disponíveis



Implementados até aqui:



| Método | Rota | Descrição |

|---|---|---|

| `POST` | `/api/v1/produtos` | Cadastra um produto |

| `GET` | `/api/v1/produtos` | Lista produtos — paginado, com filtros |

| `GET` | `/api/v1/produtos/{id}` | Busca um produto por ID |

| `PUT` | `/api/v1/produtos/{id}` | Atualiza um produto |

| `DELETE` | `/api/v1/produtos/{id}` | Desativa um produto (exclusão lógica) |



Erros são devolvidos no formato `ProblemDetail` (RFC 7807), com tratamento

centralizado.



\*\*Exemplo — cadastro de produto\*\*



```http

POST /api/v1/produtos

Content-Type: application/json



{

&#x20; "nome": "Dipirona Monoidratada 500mg",

&#x20; "principioAtivo": "Dipirona Sódica",

&#x20; "fabricante": "Medley",

&#x20; "codigoBarras": "7896422503457",

&#x20; "precoVenda": 12.90,

&#x20; "exigencia": "ISENTO",

&#x20; "estoqueMinimo": 20

}

```



O campo `exigencia` aceita `ISENTO`, `RECEITA\_SIMPLES`, `RECEITA\_RETIDA` e

`CONTROLADO`, e é o que determina se a venda exigirá registro de receita e

liberação por farmacêutico.



\---



\## Roadmap



\- \[x] \*\*Fase 1 — Fundação\*\*

&#x20;     PostgreSQL em container, Flyway, CRUD de produto com DTOs e validação,

&#x20;     filtros dinâmicos com Specification, tratamento global de erro.



\- \[ ] \*\*Fase 2 — Segurança\*\*

&#x20;     Usuários no banco, BCrypt, JWT assinado com par de chaves RSA, refresh token

&#x20;     e autorização por perfil (atendente, farmacêutico, gerente).



\- \[ ] \*\*Fase 3 — Estoque\*\*

&#x20;     Entrada de lote, movimentação como registro imutável, cálculo de

&#x20;     disponibilidade ignorando vencidos, alertas de validade e estoque mínimo.



\- \[ ] \*\*Fase 4 — Vendas\*\*

&#x20;     Fluxo de venda com baixa FEFO, atomicidade, regra de receita, cancelamento

&#x20;     com estorno nos lotes de origem e controle de concorrência.



\- \[ ] \*\*Fase 5 — Acabamento\*\*

&#x20;     Relatórios, documentação OpenAPI e testes de integração com Testcontainers.



\- \[ ] \*\*Fase 6 — Entrega\*\*

&#x20;     Dockerfile, pipeline no GitHub Actions e deploy.



A especificação completa do domínio, com as regras de negócio numeradas, está em

\[`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md).



\---



\## Autor



\*\*Gabriel Lourenço\*\* — Desenvolvedor Back-End



\[GitHub](https://github.com/ggabmartins) · \[LinkedIn](https://www.linkedin.com/in/ggabmartins/)

