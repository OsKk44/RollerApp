# Auditoría no destructiva: no ejecuta npm audit fix --force ni modifica dependencias.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
foreach ($carpeta in @('frontend-web', 'frontend-movil')) {
    Write-Host "`n=== $carpeta ===" -ForegroundColor Cyan
    Push-Location (Join-Path $raiz $carpeta)
    try {
        node --version
        npm --version
        npm ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci falló en $carpeta" }
        npm audit --json | Out-File -FilePath (Join-Path $raiz "audit-$carpeta.json") -Encoding utf8
        Write-Host "Reporte de auditoría guardado. Las alertas de npm audit requieren revisión." -ForegroundColor Yellow
        npm audit --omit=dev
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Falló la compilación en $carpeta" }
    } finally { Pop-Location }
}
Write-Host "`n=== backend-spring ===" -ForegroundColor Cyan
Push-Location (Join-Path $raiz 'backend-spring')
try {
    mvn test
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas Maven' }
} finally { Pop-Location }
