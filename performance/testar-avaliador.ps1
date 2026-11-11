$ErrorActionPreference='Stop'
$evaluator=Join-Path $PSScriptRoot 'avaliar-disputa.ps1'
$tempRoot=Join-Path $PSScriptRoot 'resultados'
New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
$fixture=Join-Path $tempRoot ('fixture-'+[guid]::NewGuid().ToString('N')+'.csv')
$header='timeStamp,elapsed,label,responseCode,success,threadName'
$cases=@(
 @{Name='uma aceita e um conflito'; Valid=$true; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,409,true,t2')},
 @{Name='tres usuarios'; Valid=$true; Users=3; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,409,true,t2','1002,25,ReservaConcurrente,409,true,t3')},
 @{Name='requisicoes sequenciais'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1021,30,ReservaConcurrente,409,true,t2')},
 @{Name='dupla reserva'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,201,true,t2')},
 @{Name='todas recusadas'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,409,true,t1','1001,30,ReservaConcurrente,409,true,t2')},
 @{Name='erro tecnico'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,500,false,t2')},
 @{Name='sem autenticacao'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,401,true,t2')},
 @{Name='amostra ausente'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1')},
 @{Name='mesma thread'; Valid=$false; Users=2; Lines=@('1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,409,true,t1')},
 @{Name='tempo invalido'; Valid=$false; Users=2; Lines=@('1000,-1,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,409,true,t2')},
 @{Name='configuracao falhou'; Valid=$false; Users=2; Lines=@('900,1,Preparar,CONFIG_ERROR,false,setup','1000,20,ReservaConcurrente,201,true,t1','1001,30,ReservaConcurrente,409,true,t2')}
)
try {
 foreach ($case in $cases) {
  @($header)+$case.Lines | Set-Content -LiteralPath $fixture -Encoding UTF8
  $valid=$true
  try { $result=& $evaluator -Jtl $fixture -Usuarios $case.Users | ConvertFrom-Json } catch { $valid=$false }
  if ($valid -ne $case.Valid) { throw "Resultado incorreto: $($case.Name)" }
  if ($valid -and ($result.p95_ms -ne 30 -or $result.persistencia_validada -ne $false)) { throw "Metricas incorretas: $($case.Name)" }
  Write-Output "OK: $($case.Name)"
 }
 $header | Set-Content -LiteralPath $fixture -Encoding UTF8
 $rejected=$false
 try { & $evaluator -Jtl $fixture -Usuarios 2 | Out-Null } catch { $rejected=$true }
 if (-not $rejected) { throw 'JTL sem amostras deveria ser recusado.' }
 Write-Output 'OK: arquivo sem amostras'
 Write-Output '12 testes do avaliador aprovados. Fixtures sinteticas; nao comprovam a API nem desempenho.'
}
finally { if (Test-Path -LiteralPath $fixture) { Remove-Item -LiteralPath $fixture -Force } }
