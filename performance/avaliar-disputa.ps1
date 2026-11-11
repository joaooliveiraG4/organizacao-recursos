param(
 [Parameter(Mandatory=$true)][string]$Jtl,
 [Parameter(Mandatory=$true)][ValidateRange(2,1000)][int]$Usuarios
)
$ErrorActionPreference='Stop'
$rows=@(Import-Csv -LiteralPath $Jtl)
if ($rows.Count -eq 0) { throw 'JTL vazio.' }
foreach ($field in @('timeStamp','elapsed','label','responseCode','success','threadName')) {
 if ($rows[0].PSObject.Properties.Name -notcontains $field) { throw "Coluna ausente: $field" }
}
if (@($rows | Where-Object {$_.success -ne 'true'}).Count -gt 0) { throw 'Execucao com amostras em falha.' }
$samples=@($rows | Where-Object {$_.label -eq 'ReservaConcurrente'})
if ($samples.Count -ne $Usuarios) { throw 'Quantidade incorreta de requisicoes.' }
if (@($samples.threadName | Select-Object -Unique).Count -ne $Usuarios) { throw 'Usuarios virtuais precisam ser distintos.' }
$accepted=@($samples | Where-Object {$_.responseCode -eq '201'}).Count
$conflicts=@($samples | Where-Object {$_.responseCode -eq '409'}).Count
if ($accepted -ne 1 -or $conflicts -ne ($Usuarios-1)) { throw "Disputa invalida: $accepted aceitas, $conflicts conflitos." }
$durations=@(); $starts=@(); $ends=@()
foreach ($sample in $samples) {
 [long]$start=0; [long]$duration=0
 if (-not [long]::TryParse($sample.timeStamp,[ref]$start) -or -not [long]::TryParse($sample.elapsed,[ref]$duration) -or $start -lt 0 -or $duration -lt 0) { throw 'Tempo invalido.' }
 $durations += $duration; $starts += $start; $ends += ($start+$duration)
}
$latestStart=($starts | Measure-Object -Maximum).Maximum
$earliestEnd=($ends | Measure-Object -Minimum).Minimum
if ($latestStart -ge $earliestEnd) { throw 'Amostras sem sobreposicao temporal; disputa nao demonstrada.' }
$sorted=@($durations | Sort-Object)
$window=($ends | Measure-Object -Maximum).Maximum - ($starts | Measure-Object -Minimum).Minimum
if ($window -le 0) { throw 'Janela invalida.' }
[PSCustomObject]@{
 usuarios=$Usuarios; aceitas=$accepted; conflitos_esperados=$conflicts; erros_tecnicos=0
 p95_ms=$sorted[[Math]::Ceiling(0.95*$Usuarios)-1]; p99_ms=$sorted[[Math]::Ceiling(0.99*$Usuarios)-1]
 throughput_requisicoes_segundo=[Math]::Round($Usuarios*1000/$window,3)
 intervalo_inicio_ms=($starts | Measure-Object -Maximum).Maximum - ($starts | Measure-Object -Minimum).Minimum
 persistencia_validada=$false
 conclusao='Contagem HTTP correta; persistencia, simultaneidade no servidor e RNF exigem evidencia adicional.'
} | ConvertTo-Json
