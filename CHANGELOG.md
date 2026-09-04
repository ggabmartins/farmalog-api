# Changelog

Registro das entregas por fase. Cada fase corresponde a uma tag e a um merge em
`main`.

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

## [fase-1] — 01-09-2026

- Projeto Spring Boot com PostgreSQL em container (`docker compose`) e Flyway
  (`ddl-auto: validate`).
- CRUD de produto (`/api/v1/produtos`) com DTOs `record`, Bean Validation em
  português e desativação lógica.
- Listagem paginada com filtros por nome, princípio ativo, exigência e situação.
- Tratamento global de erro e configuração por perfis.
- Cobertura de testes: unitários, de slice (`@WebMvcTest`) e de integração
  (`@SpringBootTest`).
