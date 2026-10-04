param(
    [string]$PackageName = "com.budimas.wms",
    [string]$Activity = ".MainActivity",
    [int]$DebounceMs = 1200
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

if (-not $env:JAVA_HOME) {
    $studioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $studioJbr) {
        $env:JAVA_HOME = $studioJbr
    }
}

if (-not $env:ANDROID_HOME) {
    $localSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $localSdk) {
        $env:ANDROID_HOME = $localSdk
    }
}

function Resolve-Adb {
    $candidates = @()

    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) {
        $candidates += $cmd.Source
    }

    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }

    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "adb.exe tidak ditemukan. Pastikan Android SDK Platform Tools sudah terinstall."
}

function Get-SourceFingerprint {
    $files = @()
    $sourceDir = Join-Path $Root "app\src\main"

    if (Test-Path $sourceDir) {
        $files += Get-ChildItem -Path $sourceDir -Recurse -File -Include *.java,*.xml -ErrorAction SilentlyContinue
    }

    foreach ($file in @("app\build.gradle", "build.gradle", "settings.gradle", "gradle.properties")) {
        $path = Join-Path $Root $file
        if (Test-Path $path) {
            $files += Get-Item $path
        }
    }

    return ($files |
        Sort-Object FullName |
        ForEach-Object { "$($_.FullName)|$($_.Length)|$($_.LastWriteTimeUtc.Ticks)" }) -join "`n"
}

function Assert-DeviceConnected {
    param([string]$Adb)

    $devices = & $Adb devices
    $online = $devices | Where-Object { $_ -match "`tdevice$" }

    if (-not $online) {
        Write-Host ""
        Write-Host "Device/emulator belum terdeteksi." -ForegroundColor Yellow
        Write-Host "Aktifkan USB debugging atau jalankan emulator, lalu cek:"
        Write-Host "  `"$Adb`" devices"
        Write-Host ""
        return $false
    }

    return $true
}

function Install-And-Launch {
    param(
        [string]$Adb,
        [string]$PackageName,
        [string]$Activity
    )

    if (-not (Assert-DeviceConnected -Adb $Adb)) {
        return
    }

    Write-Host ""
    Write-Host "[live] Build + install debug..." -ForegroundColor Cyan
    & (Join-Path $Root "gradlew.bat") ":app:installDebug"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[live] Build/install gagal. Perbaiki error lalu simpan file lagi." -ForegroundColor Red
        return
    }

    Write-Host "[live] Membuka aplikasi..." -ForegroundColor Cyan
    & $Adb shell am start -n "$PackageName/$Activity" | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[live] APK terinstall, tapi aplikasi gagal dibuka otomatis." -ForegroundColor Yellow
    } else {
        Write-Host "[live] Selesai. Menunggu perubahan berikutnya..." -ForegroundColor Green
    }
}

$adb = Resolve-Adb
$lastFingerprint = Get-SourceFingerprint

Write-Host "Budimas WMS live reload watcher" -ForegroundColor Green
Write-Host "Root : $Root"
Write-Host "ADB  : $adb"
Write-Host "Stop : Ctrl+C"

Install-And-Launch -Adb $adb -PackageName $PackageName -Activity $Activity

while ($true) {
    Start-Sleep -Milliseconds 1000
    $currentFingerprint = Get-SourceFingerprint

    if ($currentFingerprint -ne $lastFingerprint) {
        Write-Host ""
        Write-Host "[live] Perubahan terdeteksi, debounce $DebounceMs ms..." -ForegroundColor DarkCyan
        Start-Sleep -Milliseconds $DebounceMs

        $lastFingerprint = Get-SourceFingerprint
        Install-And-Launch -Adb $adb -PackageName $PackageName -Activity $Activity
    }
}
