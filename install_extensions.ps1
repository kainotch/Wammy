param(
    [switch]$AppOnly,
    [switch]$ExtensionsOnly,
    [switch]$All,
    [switch]$Force,
    [string]$DeviceId,
    [switch]$AllDevices
)

if (-not $AppOnly -and -not $ExtensionsOnly) {
    $All = $true
}

$ErrorActionPreference = "Continue"

$installStateDir = Join-Path $PSScriptRoot ".install_state"
$installStateFile = Join-Path $installStateDir "extensions_install_state.json"

function Load-InstallState {
    if (-not (Test-Path $installStateFile)) {
        return @{
            version = 1
            devices = @{}
        }
    }

    try {
        $stateObject = Get-Content -Path $installStateFile -Raw | ConvertFrom-Json
        $state = ConvertTo-Hashtable -InputObject $stateObject
        if (-not $state.ContainsKey("devices") -or $null -eq $state.devices) {
            $state.devices = @{}
        }
        return $state
    }
    catch {
        Write-Host "Failed to parse install state file; recreating: $installStateFile"
        return @{
            version = 1
            devices = @{}
        }
    }
}

function ConvertTo-Hashtable {
    param($InputObject)

    if ($null -eq $InputObject) {
        return $null
    }

    if ($InputObject -is [System.Collections.IDictionary]) {
        $result = @{}
        foreach ($key in $InputObject.Keys) {
            $result[$key] = ConvertTo-Hashtable -InputObject $InputObject[$key]
        }
        return $result
    }

    if ($InputObject -is [System.Collections.IEnumerable] -and -not ($InputObject -is [string])) {
        $list = @()
        foreach ($item in $InputObject) {
            $list += ConvertTo-Hashtable -InputObject $item
        }
        return $list
    }

    if ($InputObject -is [psobject] -and $InputObject.PSObject.Properties.Count -gt 0) {
        $result = @{}
        foreach ($property in $InputObject.PSObject.Properties) {
            $result[$property.Name] = ConvertTo-Hashtable -InputObject $property.Value
        }
        return $result
    }

    return $InputObject
}

function Save-InstallState {
    param([hashtable]$State)

    if (-not (Test-Path $installStateDir)) {
        New-Item -ItemType Directory -Path $installStateDir | Out-Null
    }

    ($State | ConvertTo-Json -Depth 8) | Set-Content -Path $installStateFile -Encoding UTF8
}

function Get-ApkFingerprint {
    param([System.IO.FileInfo]$Apk)

    return "$($Apk.Length)-$($Apk.LastWriteTimeUtc.Ticks)"
}

function Get-ConnectedDevices {
    $devices = @()
    adb devices | Select-Object -Skip 1 | ForEach-Object {
        if ($_ -match '^(\S+)\s+device') {
            $devices += $matches[1]
        }
    }
    return $devices
}

function Install-ToDevice {
    param([string]$Device, [string]$ApkPath, [string]$Description = "APK")
    Write-Host "[$Device] Installing $Description..."
    adb -s $Device install -r $ApkPath
    return $LASTEXITCODE -eq 0
}

$devices = Get-ConnectedDevices
if ($devices.Count -eq 0) {
    if ($AppOnly) {
        Write-Host "No ADB devices connected; building app only..."
        $devices = @()
    } else {
        Write-Host "No ADB devices connected!"
        exit 1
    }
}

if ($devices.Count -gt 0) {
    if (-not [string]::IsNullOrWhiteSpace($DeviceId)) {
        if ($devices -notcontains $DeviceId) {
            Write-Host "Requested device not found: $DeviceId"
            Write-Host "Connected device(s): $($devices -join ', ')"
            exit 1
        }
        $devices = @($DeviceId)
    }
    elseif (-not $AllDevices -and $devices.Count -gt 1) {
        $selected = $devices[0]
        Write-Host "Found $($devices.Count) device(s): $($devices -join ', ')"
        Write-Host "Multiple devices detected; defaulting to first: $selected"
        Write-Host "Tip: pass -DeviceId <id> or -AllDevices"
        $devices = @($selected)
    }
    else {
        Write-Host "Found $($devices.Count) device(s): $($devices -join ', ')"
    }
}

if ($All -or $AppOnly) {
    Write-Host "Building Tsundoku app..."
    Push-Location $PSScriptRoot
    .\gradlew :app:assembleDebug
    $appApk = Get-ChildItem -Path "app\build\outputs\apk\debug\*.apk" | Select-Object -First 1
    if ($appApk) {
        if ($devices.Count -gt 0) {
            Write-Host "Installing app to all devices: $($appApk.Name)"
            foreach ($device in $devices) {
                Install-ToDevice -Device $device -ApkPath $appApk.FullName -Description "app"
            }
        } else {
            Write-Host "App APK built: $($appApk.Name)"
            Write-Host "No devices connected for installation"
        }
    }
    else {
        Write-Host "App APK not found!"
    }
    Pop-Location
}

if ($All -or $ExtensionsOnly) {
    Write-Host "Building extensions..."
    $extensionsDir = Join-Path $PSScriptRoot "extensions-source"
    if (Test-Path $extensionsDir) {
        Push-Location $extensionsDir
        .\gradlew assembleDebug
        $extensionApks = Get-ChildItem -Path $extensionsDir -Recurse -Filter "*.apk" | Where-Object { $_.FullName -like "*\build\outputs\apk\*" }
        if ($extensionApks.Count -gt 0) {
            Write-Host "Found $($extensionApks.Count) extension APK(s)"

            $installState = Load-InstallState
            $devicesState = $installState.devices

            foreach ($device in $devices) {
                Write-Host "Installing extensions to: $device"

                if (-not $devicesState.ContainsKey($device) -or $null -eq $devicesState[$device]) {
                    $devicesState[$device] = @{}
                }

                $deviceState = $devicesState[$device]
                $apksToInstall = @()

                if ($Force) {
                    $apksToInstall = $extensionApks
                }
                else {
                    foreach ($apk in $extensionApks) {
                        $fingerprint = Get-ApkFingerprint -Apk $apk
                        $previousFingerprint = $deviceState[$apk.FullName]
                        if ($previousFingerprint -ne $fingerprint) {
                            $apksToInstall += $apk
                        }
                    }
                }

                if ($apksToInstall.Count -eq 0) {
                    Write-Host "[$device] No changed extension APKs detected; skipping install"
                    continue
                }

                Write-Host "[$device] Installing $($apksToInstall.Count)/$($extensionApks.Count) changed extension APK(s)"
                $deviceInstalled = 0
                foreach ($apk in $apksToInstall) {
                    if (Install-ToDevice -Device $device -ApkPath $apk.FullName -Description $apk.Name) {
                        $deviceInstalled++
                        $deviceState[$apk.FullName] = Get-ApkFingerprint -Apk $apk
                    }
                }

                # Remove stale entries for APKs no longer present in current build outputs
                $currentApkPaths = @{}
                foreach ($apk in $extensionApks) {
                    $currentApkPaths[$apk.FullName] = $true
                }
                foreach ($trackedPath in @($deviceState.Keys)) {
                    if (-not $currentApkPaths.ContainsKey($trackedPath)) {
                        $null = $deviceState.Remove($trackedPath)
                    }
                }

                Write-Host "[$device] Installed: $deviceInstalled/$($apksToInstall.Count) changed extensions"
            }

            $installState.devices = $devicesState
            Save-InstallState -State $installState
        }
        else {
            Write-Host "No extension APKs found!"
        }
        Pop-Location
    }
    else {
        Write-Host "Extensions directory not found: $extensionsDir"
    }
}

Write-Host "Installation complete for $($devices.Count) device(s)!"
