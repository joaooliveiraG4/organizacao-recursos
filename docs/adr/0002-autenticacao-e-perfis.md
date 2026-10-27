# ADR 0002 — Autenticação HTTP Basic e autorização por perfil

- Status: aceita
- Data: 2026-10-27
- Responsável: João
- Requisito: RF-01

## Contexto

A fundação usava o usuário padrão do Spring Security, com senha aleatória e sem perfis. Isso não permite provar que um Solicitante é impedido de aprovar ou de alterar o catálogo, e "autorização falha" é bloqueador da avaliação. Reservas (RF-07) e auditoria (RF-10) também precisam saber quem é o autor de cada operação.

## Decisão

- Perfis fixos: `SOLICITANTE`, `RESPONSAVEL`, `ADMINISTRADOR` (`Perfil`), gravados em `app_user.role`.
- Autenticação HTTP Basic, sem sessão (`STATELESS`), contra `app_user` com senha em BCrypt (`password_hash`, migration V3).
- Autorização **negada por padrão**: só `/api/v1/status` e `/actuator/health` são públicos.
- Catálogo (`/api/v1/salas`, `/professores`, `/materiais`): leitura para qualquer perfil autenticado; escrita somente para `ADMINISTRADOR` (decisão do João em 2026-10-27).
- `@EnableMethodSecurity` ativo para regras por operação, por exemplo `@PreAuthorize("hasRole('RESPONSAVEL')")` na aprovação de recurso restrito (RF-07).
- 401 quando a identidade não é confirmada; 403 quando o perfil não permite. Ambos em `application/problem+json` com mensagem fixa. Senha errada, e-mail inexistente e usuário inativo recebem a mesma resposta, para não revelar quais contas existem.
- O principal autenticado (`UsuarioAutenticado`) expõe o `id` do usuário para a auditoria registrar o autor real.
- Administrador inicial somente quando `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` estiverem definidas no ambiente; nenhuma credencial é versionada.

## Alternativas consideradas

- **JWT/OAuth2:** mais próximo de produção, mas adiciona emissão, expiração e revogação de token sem ganho para os riscos avaliados. Pode substituir o Basic sem alterar a matriz de perfis.
- **Usuários em memória:** mais simples, mas não prova o comportamento com o PostgreSQL nem a desativação de usuário.

## Consequências

- CSRF fica desabilitado porque a API não usa cookie de sessão; o navegador não envia credenciais Basic automaticamente para terceiros sem que o usuário as tenha digitado. Revisar se uma interface com sessão por cookie for criada.
- Testes de controller com escrita no catálogo precisam de `@WithMockUser(roles = "ADMINISTRADOR")`.
- HTTP Basic exige HTTPS fora do ambiente local.

## Questões em aberto e riscos residuais

- Permissões finas no catálogo (por exemplo, Responsável colocar o próprio recurso em manutenção) ainda não foram decididas.
- Não há limitação de tentativas de login (força bruta) nem rate limit.
- Não há cadastro, troca de senha ou desativação de usuário pela API.
