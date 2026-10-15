# ADR 0001 — Java 21, Spring Boot 3 e PostgreSQL

- Status: aceita
- Data: 2026-10-15

## Contexto

O trabalho exige Java 21 e Spring Boot 3.x. Reservas concorrentes, constraints e auditoria precisam ser verificadas em ambiente próximo do real.

## Decisão

Usar Java 21, Spring Boot 3.5.x, Maven Wrapper, PostgreSQL e Flyway. Testes críticos de persistência usarão Testcontainers.

## Consequências

- o ambiente fica reproduzível por wrapper e containers;
- migrations passam a ser parte do código;
- Docker é necessário para testes de integração com Testcontainers;
- H2 não será usado como prova de comportamento crítico do PostgreSQL.

