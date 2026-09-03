# Uso:
#   1. Guarda el JSON de tu resultado del /dataset-ajustado/solve como resultado.json
#      en la carpeta D:\javaProjects\timetabling (o donde sea que guardes este script).
#      IMPORTANTE: guardalo con codificacion UTF-8 (hay letras con tilde en los nombres).
#   2. Abre PowerShell en esa carpeta y corre:  .\armar_verificar.ps1
#      (si te bloquea con un error de "execution policy", corre en vez de eso:
#       powershell -ExecutionPolicy Bypass -File .\armar_verificar.ps1
#       o simplemente COPIA todo este contenido y PEGALO directo en la consola de PowerShell,
#       eso siempre funciona sin importar la politica de ejecucion)
#
# No requiere instalar nada: Invoke-RestMethod y ConvertTo/From-Json vienen con Windows.

$Base = "http://localhost:8080/api/timetable"

# 1. Traer el dataset base (profesores/cursos/salas/ramos/config), SIN sesionesActuales
$request = Invoke-RestMethod -Uri "$Base/dataset-ajustado" -Method Get

# 2. Cargar el resultado ya resuelto
$resultado = Get-Content -Raw -Encoding UTF8 -Path ".\resultado.json" | ConvertFrom-Json

# 3. Agrupar las sesiones del resultado por ramoId
$porRamo = @{}
foreach ($s in $resultado.sesiones) {
    if (-not $porRamo.ContainsKey($s.ramoId)) { $porRamo[$s.ramoId] = @() }
    $porRamo[$s.ramoId] += [PSCustomObject]@{
        indiceSesion = $s.indiceSesion
        dia          = $s.dia
        bloque       = $s.bloque
        salaId       = $s.salaId
    }
}

# 4. Inyectar sesionesActuales en cada ramo del request, ordenadas por indiceSesion
foreach ($ramo in $request.ramos) {
    $sesionesDeEsteRamo = $porRamo[$ramo.id]
    if ($sesionesDeEsteRamo) {
        $ordenadas = $sesionesDeEsteRamo | Sort-Object indiceSesion
        $valor = @($ordenadas | ForEach-Object {
            [PSCustomObject]@{ dia = $_.dia; bloque = $_.bloque; salaId = $_.salaId }
        })
        $ramo | Add-Member -NotePropertyName sesionesActuales -NotePropertyValue $valor -Force
    }
}

# 5. Llamar a /verificar
$body = $request | ConvertTo-Json -Depth 10
$data = Invoke-RestMethod -Uri "$Base/verificar" -Method Post -Body $body -ContentType "application/json; charset=utf-8"

Write-Host "Score: $($data.score) | Factible: $($data.factible)"
Write-Host ""
Write-Host "Detalle por restriccion:"
foreach ($d in $data.detalle) {
    Write-Host "  - $($d.restriccion): $($d.score)  ($($d.ocurrencias) ocurrencia(s))"
}
