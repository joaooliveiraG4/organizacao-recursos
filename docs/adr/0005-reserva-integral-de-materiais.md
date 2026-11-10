# ADR 0005 — Reserva integral de materiais

Cada alocação de material bloqueia todo o cadastro durante o período.
A quantidade do catálogo descreve o total cadastrado. A reserva ainda
não controla saldo nem permite reservar parte desse total.

O cadastro inteiro entra na mesma regra de exclusão que salas e professores.
Quando houver controle por unidade ou por quantidade, será preciso revisar a
regra de disponibilidade e os testes de concorrência.
