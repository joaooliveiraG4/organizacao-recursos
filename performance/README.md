# Testes de concorrência e desempenho — RF-06

## Estado desta entrega

Preparação de Kauã, em 05/11/2026. Há plano JMX parametrizado e avaliador
dos resultados HTTP com testes próprios. **Ainda não há execução do JMX
nem evidência de desempenho/concorrência da API de reservas.**

A main consultada foi 8cff3c8. Ela contém o catálogo de professores/materiais,
mas ainda não contém API de reservas, autenticação, salas e auditoria integradas.
Não usar o cadastro de materiais como substituto para uma disputa de reservas.

## Pré-condições

1. Integrar as dependências pelo fluxo do João, preservando a ordem das migrations.
2. Ter contrato aprovado de criação de reserva (rota, payload, 201 e conflito 409).
   Esses códigos são a expectativa do cenário; confirmar com o contrato implementado.
3. Ter PostgreSQL e dados sintéticos exclusivos para QA, com recurso/horário livre.
4. Ter Java 21 e Apache JMeter disponíveis. JMeter e Docker não foram encontrados nesta máquina.
5. Decidir os limites de carga nominal com João. Os números da utility tree são propostas.

## Plano de disputa

jmeter/disputa-reserva.jmx usa uma rodada com duas threads por padrão e um
Synchronizing Timer para liberá-las juntas. Há timeout para evitar espera
indefinida. É uma única JVM, sem execução distribuída.

Os usuários enviam o mesmo JSON de um arquivo externo. O plano exige baseUrl,
reservaPath e payloadFile; não inventa uma rota nem campos de reserva.
users aceita de 2 a 1000, mas começar com 2. Não aumentar a carga antes de
validar a fixture, o ambiente e a proteção no banco.

A configuração é validada antes dos envios; falha interrompe o teste.
A credencial vem da variável RECURSOS_QA_AUTHORIZATION da sessão, nunca
do repositório. Não imprimir seu valor. Usar credencial de QA aprovada para
criar reservas, sem privilégios adicionais.

Cada 409 é classificado como conflito esperado. O resultado só passa pelo
avaliador se houver exatamente um 201, N-1 conflitos e nenhuma falha.
Assim, duas respostas 201 ou todos os pedidos recusados reprovam.

O plano mede tentativas concorrentes pelo cliente. A identidade do recurso,
a causa do 409, a sobreposição no servidor e a persistência ainda precisam ser
conferidas contra o contrato e o PostgreSQL. Não concluir RF-06 só pela contagem HTTP.

## Executar depois de liberar as dependências

Na raiz, prepare em performance/resultados (ignorada pelo Git) o payload oficial
com IDs sintéticos válidos e um horário livre novo para cada rodada.
Configure a autenticação pelo canal de desenvolvimento seguro antes de executar.

No PowerShell, substitua os parâmetros pelo contrato aprovado:

```powershell
jmeter -n -q performance/jmeter/qa.properties -t performance/jmeter/disputa-reserva.jmx -JbaseUrl=http://localhost:8080 -JreservaPath=ROTA_APROVADA -JpayloadFile=CAMINHO_ABSOLUTO_DO_JSON -Jusers=2 -l performance/resultados/disputa-RODADA.jtl -j performance/resultados/disputa-RODADA.log
.\performance\avaliar-disputa.ps1 -Jtl performance/resultados/disputa-RODADA.jtl -Usuarios 2
```

Criar performance/resultados antes de executar e usar arquivos novos por rodada:
JMeter pode acrescentar amostras a JTL existente. O avaliador rejeita contagem
diferente da esperada. O retorno do JMeter sozinho não basta: amostras em falha
podem coexistir com saída normal do processo; executar o avaliador é obrigatório.

qa.properties desliga gravação de corpos e cabeçalhos. Conferir o log antes
de compartilhar e não usar Debug Sampler/registro detalhado com credenciais.

## Métricas e conclusão

O avaliador calcula p95/p99 por nearest-rank, throughput da janela observada
e diferença entre os horários de início. Usa apenas amostras ReservaConcurrente.
Recusa amostras sequenciais sem interseção temporal, threads repetidas,
contagem errada, códigos inesperados e qualquer amostra marcada em falha.

Uma rodada com duas amostras verifica disputa, não sustenta conclusão estatística
de desempenho. A carga nominal exige outro cenário, amostra suficiente, aquecimento
e RNF aprovado. Nenhum limite de latência é aprovado automaticamente aqui.

Após cada rodada, consultar o banco sem alterar dados:
- uma única reserva persistida para o recurso/período da fixture;
- vínculo e evento de auditoria corretos;
- nenhuma gravação de reserva para a tentativa recusada;
- erro 409 corresponde à disputa do recurso, não a outra validação.

Preencher docs/testes/relatorio-carga-modelo.md com commit, ambiente, comandos,
quantidade de amostras e resultados reais. JTLs brutos ficam locais.
O avaliador retorna persistencia_validada=false deliberadamente: essa prova
não está disponível no CSV e não pode ser inventada.

## Testar somente o avaliador

```powershell
.\performance\testar-avaliador.ps1
```

São fixtures sintéticas. Os testes verificam a ferramenta de análise e não
simulam aprovação da API. Resultado local: 12 casos aprovados em 05/11/2026.

Referências: [Synchronizing Timer](https://jmeter.apache.org/usermanual/component_reference.html#Synchronizing_Timer),
[execução em CLI](https://jmeter.apache.org/usermanual/best-practices.html#lean_mean).
