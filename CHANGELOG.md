# Changelog

Registro das entregas por fase. Cada fase corresponde a uma tag e a um merge em
`main`.

## [fase-1] — 01-09-2026

- Projeto Spring Boot com PostgreSQL em container (`docker compose`) e Flyway
  (`ddl-auto: validate`).
- CRUD de produto (`/api/v1/produtos`) com DTOs `record`, Bean Validation em
  português e desativação lógica.
- Listagem paginada com filtros por nome, princípio ativo, exigência e situação.
- Tratamento global de erro e configuração por perfis.
- Cobertura de testes: unitários, de slice (`@WebMvcTest`) e de integração
  (`@SpringBootTest`).

## [fase-2] — 03-09-2026

### Segurança

- Autenticação JWT stateless (OAuth2 Resource Server, RS256).
- `POST /auth/login` (token de 30 min) e `GET /auth/me`.
- Entidade `Usuario` (migration V2) com seed de um usuário por perfil.
- Autorização por perfil: consulta de produto liberada para qualquer perfil
  autenticado; cadastro/edição/desativação restritos ao `GERENTE`.
- CRUD de usuários em `/api/v1/usuarios`, restrito ao `GERENTE`.
- Chaves RSA fora do versionamento (`scripts/gen-keys.sh`).

### Arquitetura

- Migração para Spring Boot 4.1 e organização por camada
  (`controller` / `service` / `repository` / `entity` / `dto` / `validation`).
- Tratamento de erro com `@RestControllerAdvice` simples e `ResponseStatusException`
  (`{ status, message }` / lista de `{ field, message }`).
- Filtro de listagem por `@Query`; resposta paginada com `Page`.
- Mapeamento DTO ↔ entidade nos próprios DTOs (`toEntity` / `fromEntity`).
- Documentação OpenAPI (springdoc) em `/swagger-ui.html`.

## [fase-3] — 08-09-2026

### Estoque por lote

- Entidades `Lote` e `MovimentacaoEstoque` (livro-razão imutável, `@Immutable`)
  na migration V3.
- Entrada de lote em `POST /produtos/{id}/lotes`: o lote nasce com saldo 0 e uma
  movimentação `ENTRADA` traz o saldo; restrito a FARMACÊUTICO/GERENTE.
- Descarte em `POST /lotes/{id}/descarte`: apenas de lote vencido, parcial ou
  total, com observação obrigatória (RN-10).
- Saldo do lote derivado das movimentações; `EstoqueService` é o ponto único de
  alteração de saldo (RN-04).
- Consulta de lotes por produto e alertas: `GET /lotes/vencendo` e
  `GET /estoque/alertas` (abaixo do mínimo e vencendo em N dias).
- Auditoria em `GET /estoque/movimentacoes`, com filtros por tipo, lote e período.
- `ProdutoResponse` expõe `quantidadeEmEstoque` e a listagem aceita
  `?abaixoDoMinimo=true`; lotes vencidos não entram na disponibilidade (RN-02).
- Cobertura de testes das regras de estoque (RN-02, RN-04, RN-10).
