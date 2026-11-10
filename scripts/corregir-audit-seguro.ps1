# Correcciones de seguridad compatibles: nunca utiliza --force.
# Ejecutar desde cualquier carpeta con PowerShell 5.1 o superior.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

foreach ($nombre in @('frontend-web', 'frontend-movil')) {
    Write-Host "`n=== $nombre ===" -ForegroundColor Cyan
    $carpeta = Join-Path $raiz $nombre
    Push-Location $carpeta
    try {
        $respaldo = Join-Path $carpeta 'package-lock.json.antes-de-audit.bak'
        Copy-Item 'package-lock.json' $respaldo -Force
        Write-Host 'Respaldo package-lock.json creado.'

        # npm audit fix respeta los rangos de versiones actuales: NO usa --force.
        npm audit fix --no-fund
        if ($LASTEXITCODE -ne 0) {
            Write-Warning 'npm audit fix reportó vulnerabilidades pendientes; NO se forzarán actualizaciones mayores.'
        }

        npm ci --no-fund
        if ($LASTEXITCODE -ne 0) { throw "npm ci falló en $nombre" }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Compilación fallida en $nombre" }

        Write-Host 'Auditoría de dependencias de producción:'
        npm audit --omit=dev
        $estadoProduccion = $LASTEXITCODE
        Write-Host 'Auditoría total (incluye herramientas de compilación):'
        npm audit
        $estadoCompleto = $LASTEXITCODE
        if ($estadoProduccion -ne 0 -or $estadoCompleto -ne 0) {
            Write-Warning "Aún hay hallazgos por revisar en $nombre. Conservar el reporte y no ejecutar --force."
        } else {
            Write-Host "Sin hallazgos en auditorías npm de $nombre." -ForegroundColor Green
        }
        Write-Host "Respaldo original: $respaldo"
    } finally {
        Pop-Location
    }
}
Write-Host "`nRevisar cualquier hallazgo restante antes de desplegar a producción."
