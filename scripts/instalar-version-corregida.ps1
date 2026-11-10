# RollerApp: actualiza los lockfiles de ambos frontends y valida la compilacion.
# Requiere acceso a Internet, Node.js y npm compatibles con package.json.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

function Ejecutar-Npm([string[]]$Argumentos) {
    & npm @Argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo npm $($Argumentos -join ' ') (codigo $LASTEXITCODE)."
    }
}

foreach ($nombre in @('frontend-web', 'frontend-movil')) {
    $carpeta = Join-Path $raiz $nombre
    Write-Host "`n=== Preparando $nombre ===" -ForegroundColor Cyan
    Push-Location $carpeta
    try {
        $respaldo = 'package-lock.json.antes-de-correccion.bak'
        if ((Test-Path 'package-lock.json') -and !(Test-Path $respaldo)) {
            Copy-Item 'package-lock.json' $respaldo
        }
        # Sin --force: respeta las dependencias del proyecto.
        Ejecutar-Npm @('install', '@angular/build@22.2.6', '@angular/cli@22.2.6', '--save-dev', '--save-exact', '--no-fund')
        Ejecutar-Npm @('audit', 'fix', '--no-fund')
        Ejecutar-Npm @('run', 'build')
        Ejecutar-Npm @('audit')
        Write-Host "OK: $nombre compilado y sin vulnerabilidades npm reportadas." -ForegroundColor Green
    } finally {
        Pop-Location
    }
}
Write-Host "`nAmbos frontend completaron las verificaciones." -ForegroundColor Green
Write-Host 'Recuerda: esto no verifica el backend, la base de datos ni las pruebas funcionales.'
