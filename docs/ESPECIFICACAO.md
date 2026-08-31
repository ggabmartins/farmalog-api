# Sistema de Gestão para Farmácia Independente — Especificação

API REST para controle de estoque por lote, gestão de validade e registro de vendas
em farmácias de pequeno porte.

> Documento de referência do projeto. A modelagem e as regras aqui descritas são
> uma simplificação inspirada na operação real de uma farmácia — não constituem
> implementação de conformidade regulatória.

---

## 1. Stack

| Item | Escolha |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Segurança | Spring Security + OAuth2 Resource Server (JWT com par de chaves RSA) |
| Persistência | Spring Data JPA / Hibernate |
| Banco | PostgreSQL (via Docker Compose) |
| Migrations | Flyway |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, Testcontainers |
| Container | Docker |
| CI | GitHub Actions |

**Organização do código:** pacote por feature (`auth`, `produto`, `lote`, `venda`,
`estoque`, `relatorio`), não por camada. Dentro de cada feature: `Controller`,
`Service`, `Repository`, `entity`, `dto`.

---

## 2. Modelo de domínio

### Entidades

**Usuario**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| nome | String | |
| email | String | único, usado como login |
| senhaHash | String | BCrypt |
| perfil | Perfil | `ATENDENTE`, `FARMACEUTICO`, `GERENTE` |
| ativo | boolean | desativação lógica |
| criadoEm | Instant | |

**Produto**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| nome | String | |
| principioAtivo | String | |
| fabricante | String | |
| codigoBarras | String | único |
| precoVenda | BigDecimal | nunca `Double` para dinheiro |
| exigencia | ExigenciaReceita | `ISENTO`, `RECEITA_SIMPLES`, `RECEITA_RETIDA`, `CONTROLADO` |
| estoqueMinimo | Integer | dispara alerta de reposição |
| ativo | boolean | |

**Lote**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| produto | Produto | ManyToOne |
| codigo | String | único junto com `produto` |
| dataValidade | LocalDate | |
| quantidadeAtual | Integer | |
| precoCusto | BigDecimal | |
| dataEntrada | LocalDate | |
| version | Long | `@Version` — lock otimista |

**Venda**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| operador | Usuario | quem registrou |
| farmaceutico | Usuario | nulo, exceto em venda com exigência de receita |
| clienteCpf | String | opcional |
| dataHora | Instant | |
| valorTotal | BigDecimal | calculado no servidor, nunca recebido do cliente |
| status | StatusVenda | `CONCLUIDA`, `CANCELADA` |

**ItemVenda**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| venda | Venda | ManyToOne |
| produto | Produto | |
| quantidade | Integer | |
| precoUnitario | BigDecimal | congelado no momento da venda |
| subtotal | BigDecimal | |

**MovimentacaoEstoque**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| lote | Lote | |
| tipo | TipoMovimentacao | `ENTRADA`, `SAIDA_VENDA`, `ESTORNO`, `AJUSTE`, `DESCARTE` |
| quantidade | Integer | sempre positiva; o tipo define o sinal |
| itemVenda | ItemVenda | nulo quando não originada de venda |
| usuario | Usuario | responsável |
| dataHora | Instant | |
| observacao | String | obrigatória em `AJUSTE` e `DESCARTE` |

**Receita**
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | |
| venda | Venda | OneToOne |
| numero | String | |
| crmMedico | String | |
| dataEmissao | LocalDate | |
| farmaceutico | Usuario | quem conferiu |

### Relacionamentos

```
Usuario ──< Venda ──< ItemVenda ──< MovimentacaoEstoque >── Lote >── Produto
                │
                └──── Receita
```

Um `ItemVenda` pode gerar **várias** movimentações, porque a quantidade vendida
pode ser atendida por mais de um lote.

---

## 3. Regras de negócio

Estas regras são o núcleo do projeto. São elas que diferenciam a aplicação de um
CRUD — e são elas que serão perguntadas em entrevista.

**RN-01 — Baixa por FEFO.** A venda consome os lotes em ordem de validade, do que
vence primeiro para o que vence por último. Lotes vencidos são ignorados.

**RN-02 — Lote vencido não é vendável.** Um lote com `dataValidade` anterior à data
atual nunca entra no cálculo de disponibilidade, mesmo com saldo positivo.

**RN-03 — Venda é atômica.** Se qualquer item não puder ser atendido integralmente,
a venda inteira falha e nada é gravado. Uma venda parcial não existe.

**RN-04 — Estoque só muda via movimentação.** Nenhum ponto do código altera
`Lote.quantidadeAtual` diretamente. Toda alteração gera uma `MovimentacaoEstoque`
correspondente, formando um histórico auditável.

**RN-05 — Receita obrigatória.** Produtos com `exigencia` diferente de `ISENTO`
só podem ser vendidos se a venda tiver uma `Receita` associada e um farmacêutico
responsável registrado.

**RN-06 — Cancelamento estorna nos mesmos lotes.** Cancelar uma venda gera
movimentações do tipo `ESTORNO` devolvendo a quantidade exatamente aos lotes de onde
saiu — não a um lote qualquer.

**RN-07 — Preço é congelado.** O `precoUnitario` do item é copiado do produto no
momento da venda. Alterar o preço do produto depois não altera vendas passadas.

**RN-08 — Concorrência no estoque.** Dois operadores vendendo o último lote ao mesmo
tempo não podem deixar o estoque negativo. Resolver com lock otimista (`@Version`)
e tratar o conflito devolvendo `409 Conflict`.

**RN-09 — Alertas.** O sistema expõe produtos abaixo do estoque mínimo e lotes que
vencem dentro de um prazo configurável.

**RN-10 — Descarte de vencido.** Lote vencido pode ser baixado por descarte, com
observação obrigatória e registro de quem executou.

---

## 4. Autorização

| Ação | ATENDENTE | FARMACEUTICO | GERENTE |
|---|:---:|:---:|:---:|
| Consultar produtos e estoque | ✅ | ✅ | ✅ |
| Registrar venda de produto isento | ✅ | ✅ | ✅ |
| Registrar venda com receita | ❌ | ✅ | ✅ |
| Cancelar venda | ❌ | ✅ | ✅ |
| Registrar entrada de lote | ❌ | ✅ | ✅ |
| Descartar lote vencido | ❌ | ✅ | ✅ |
| Cadastrar/editar produto e preço | ❌ | ❌ | ✅ |
| Relatórios | ❌ | ❌ | ✅ |
| Gerenciar usuários | ❌ | ❌ | ✅ |

Além do perfil, aplicar verificação de contexto quando fizer sentido — por exemplo,
um atendente consulta as próprias vendas, não as de todos.

---

## 5. Endpoints

Prefixo: `/api/v1`

### Autenticação
| Método | Rota | Perfil |
|---|---|---|
| POST | `/auth/login` | público |
| POST | `/auth/refresh` | público (com refresh token) |
| GET | `/auth/me` | autenticado |

### Produtos
| Método | Rota | Perfil |
|---|---|---|
| GET | `/produtos` | autenticado — paginado, filtros por nome, princípio ativo, exigência, abaixo do mínimo |
| GET | `/produtos/{id}` | autenticado |
| POST | `/produtos` | GERENTE |
| PUT | `/produtos/{id}` | GERENTE |
| DELETE | `/produtos/{id}` | GERENTE — desativação lógica |

### Lotes
| Método | Rota | Perfil |
|---|---|---|
| GET | `/produtos/{id}/lotes` | autenticado |
| POST | `/produtos/{id}/lotes` | FARMACEUTICO, GERENTE — entrada de estoque |
| GET | `/lotes/vencendo?dias=30` | autenticado |
| POST | `/lotes/{id}/descarte` | FARMACEUTICO, GERENTE |

### Vendas
| Método | Rota | Perfil |
|---|---|---|
| POST | `/vendas` | autenticado — regra de receita aplicada por produto |
| GET | `/vendas` | autenticado — paginado, filtro por período |
| GET | `/vendas/{id}` | autenticado |
| POST | `/vendas/{id}/cancelamento` | FARMACEUTICO, GERENTE |

### Estoque
| Método | Rota | Perfil |
|---|---|---|
| GET | `/estoque/movimentacoes` | autenticado — paginado, auditoria |
| GET | `/estoque/alertas` | autenticado |

### Relatórios
| Método | Rota | Perfil |
|---|---|---|
| GET | `/relatorios/vendas?inicio=&fim=` | GERENTE |
| GET | `/relatorios/produtos-mais-vendidos` | GERENTE |

### Usuários
| Método | Rota | Perfil |
|---|---|---|
| GET | `/usuarios` | GERENTE |
| POST | `/usuarios` | GERENTE |
| PUT | `/usuarios/{id}` | GERENTE |
| DELETE | `/usuarios/{id}` | GERENTE — desativação lógica |

---

## 6. Padrões obrigatórios

- **DTOs sempre.** Entidade JPA nunca aparece na assinatura de um controller, nem na
  entrada nem na saída. Usar `record` para os DTOs.
- **Bean Validation** nos DTOs de entrada, com mensagens em português.
- **`@RestControllerAdvice`** centralizando o tratamento de erro, devolvendo
  `ProblemDetail` (RFC 7807).
- **`BigDecimal`** para todo valor monetário.
- **`Instant`** para data e hora; `LocalDate` para datas sem hora.
- **Paginação** com `Pageable` em toda listagem.
- **Flyway** para todo o schema. Nada de `ddl-auto: update`.
- **Configuração externalizada.** Nenhuma credencial no código.

---

## 7. Fases de implementação

**Fase 1 — Fundação**
Projeto Spring Boot, Docker Compose com PostgreSQL, Flyway configurado, entidades e
migrations, CRUD de produto com DTOs, validação e tratamento global de erro. Sem
segurança ainda.

**Fase 2 — Segurança**
Entidade `Usuario` no banco, `UserDetailsService` customizado, BCrypt, geração e
validação de JWT com chaves RSA, refresh token, `@PreAuthorize` por perfil.

**Fase 3 — Estoque**
Entrada de lote, `MovimentacaoEstoque` como registro imutável, cálculo de
disponibilidade ignorando vencidos, alertas, descarte.

**Fase 4 — Vendas**
Fluxo de venda com FEFO, atomicidade, regra de receita, cancelamento com estorno,
lock otimista.

**Fase 5 — Acabamento**
Relatórios, Swagger, testes unitários das regras e de integração com Testcontainers.

**Fase 6 — Entrega**
Dockerfile, GitHub Actions, deploy, README.

---

## 8. Critérios de pronto

- [ ] Sobe com um único `docker compose up`
- [ ] Swagger acessível e com todos os endpoints documentados
- [ ] Testes cobrindo as regras RN-01, RN-03, RN-05, RN-06 e RN-08
- [ ] Nenhuma credencial versionada
- [ ] README explicando decisões de arquitetura, não só comandos
- [ ] Pipeline verde no GitHub Actions
- [ ] Aplicação publicada com dados de demonstração
