<#
.SYNOPSIS
    Dong goi ung dung UDP Multi-room Chat thanh ban Portable cho Windows x64.
    Khong yeu cau may dich cai dat Java, Maven hay bat ky cong cu nao.

.DESCRIPTION
    Script su dung Maven de bien dich va thu thap dependencies,
    sau do su dung jpackage (co san trong JDK 21) de tao hai ban runtime doc lap:
    1. UDPChatServer-Windows-x64 (May chu giam sat va dieu phoi UDP)
    2. UDPChatClient-Windows-x64 (Ung dung chat nguoi dung)
    Moi ban bao gom:
    - Tep thuc thi .exe (UDPChatServer.exe / UDPChatClient.exe)
    - Java Runtime Environment (JRE 21) tu chua trong thu muc runtime/
    - Cac thu vien jar phu thuoc (FlatLaf) trong app/
    - Tep HUONG_DAN.txt va tep khoi chay du phong .bat

.EXAMPLE
    .\scripts\build-portable.ps1
#>

[CmdletBinding()]
param(
    [string]$Maven = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
)

$ErrorActionPreference = 'Stop'
$gocDuAn = Split-Path -Parent $PSScriptRoot
$thuMucDist = Join-Path $gocDuAn 'dist'
$thuMucTarget = Join-Path $gocDuAn 'target'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   BAT DAU DONG GOI PORTABLE UDP MULTI-ROOM CHAT (WINDOWS)  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Kiem tra cong cu
Write-Host "[1/6] Kiem tra JDK 21, jpackage va Maven..." -ForegroundColor Yellow
if (-not (Test-Path -LiteralPath $Maven)) {
    $mvnCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mvnCmd) { $Maven = $mvnCmd.Source }
    else { throw "Khong tim thay Maven tai: $Maven" }
}

$jpCmd = Get-Command jpackage.exe -ErrorAction SilentlyContinue
if (-not $jpCmd) {
    throw "Khong tim thay jpackage trong PATH. Yeu cau JDK 21 tro len."
}
Write-Host "  + Maven: $Maven" -ForegroundColor Green
Write-Host "  + jpackage: $($jpCmd.Source)" -ForegroundColor Green

# 2. Don dep thu muc cu
Write-Host "[2/6] Chuan bi thu muc dau ra (dist/)..." -ForegroundColor Yellow
if (Test-Path -LiteralPath $thuMucDist) {
    Remove-Item -LiteralPath $thuMucDist -Recurse -Force | Out-Null
}
New-Item -ItemType Directory -Path $thuMucDist -Force | Out-Null

$staging = Join-Path $thuMucTarget 'staging-portable'
if (Test-Path -LiteralPath $staging) {
    Remove-Item -LiteralPath $staging -Recurse -Force | Out-Null
}
New-Item -ItemType Directory -Path $staging -Force | Out-Null

# 3. Bien dich va dong goi jar
Write-Host "[3/6] Bien dich va copy dependencies..." -ForegroundColor Yellow
Push-Location -LiteralPath $gocDuAn
try {
    & $Maven -q package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Loi khi chay mvn package" }
    & $Maven -q dependency:copy-dependencies
    if ($LASTEXITCODE -ne 0) { throw "Loi khi copy dependencies" }
} finally {
    Pop-Location
}

$jarApp = Join-Path $thuMucTarget 'UDPmultiChat-1.0-SNAPSHOT.jar'
$jarFlatlaf = Join-Path $thuMucTarget 'dependency\flatlaf-3.7.2.jar'

if (-not (Test-Path -LiteralPath $jarApp)) { throw "Khong tim thay file JAR ung dung: $jarApp" }
if (-not (Test-Path -LiteralPath $jarFlatlaf)) { throw "Khong tim thay FlatLaf: $jarFlatlaf" }

Copy-Item $jarApp $staging
Copy-Item $jarFlatlaf $staging

# 4. jpackage cho Server
Write-Host "[4/6] Tao goi UDPChatServer voi jpackage (kem JRE 21)..." -ForegroundColor Yellow
& jpackage --type app-image `
    --dest $thuMucDist `
    --name "UDPChatServer" `
    --input $staging `
    --main-jar "UDPmultiChat-1.0-SNAPSHOT.jar" `
    --main-class "com.mycompany.udpmultichat.server.ServerMain" `
    --java-options "-Dfile.encoding=UTF-8" `
    --java-options "-Dsun.stdout.encoding=UTF-8" `
    --java-options "-Dsun.stderr.encoding=UTF-8"

if ($LASTEXITCODE -ne 0) { throw "Loi khi jpackage UDPChatServer" }

# Tao script chay va huong dan Server
$huongDanServer = @"
======================================================================
           UDP MULTI-ROOM CHAT - MAY CHU DIEU PHOI (PORTABLE)
======================================================================

1. GIOI THIEU:
   - Day la ban chay doc lap danh cho May Chu (Server).
   - KHONG can cai dat Java hoac bat ky cong cu nao khac tren may tinh.
   - Chay truc tiep bang cach bam dup vao: UDPChatServer.exe

2. HUONG DAN SU DUNG:
   - Buoc 1: Chay tep 'UDPChatServer.exe'.
   - Buoc 2: Cua so giam sat may chu xuat hien, nhan nut [Bat dau].
   - Buoc 3: Nhan nut [Sao chep IP] de lay dia chi IP LAN cua may chu 
             (vi du: 192.168.1.15).
   - Buoc 4: Gui dia chi IP nay cho cac may Client de ket noi.

3. CAU HINH MANG & FIREWALL (TUONG LUA):
   - May chu lang nghe giao thuc UDP tren cong 5000 (dieu khien).
   - Cac phong chat su dung UDP Multicast (dai cong 21000 - 35000).
   - Neu Windows Defender Firewall hoi khi mo ung dung lan dau, 
     vui long chon [Allow access] (Cho phep truy cap ca mang Private).

4. QUAN TRI VA BAO CAO:
   - Nhan nut [Xuat bao cao] de xem truoc va in/luu PDF hoac mo HTML.
   - Du lieu duoc luu tu dong trong thu muc 'data/'.
"@
Set-Content -Path (Join-Path $thuMucDist "UDPChatServer\HUONG_DAN.txt") -Value $huongDanServer -Encoding UTF8

$batServer = @"
@echo off
chcp 65001 > nul
start "" "%~dp0UDPChatServer.exe"
"@
Set-Content -Path (Join-Path $thuMucDist "UDPChatServer\Chay-Server.bat") -Value $batServer -Encoding ASCII

# 5. jpackage cho Client
Write-Host "[5/6] Tao goi UDPChatClient voi jpackage (kem JRE 21)..." -ForegroundColor Yellow
& jpackage --type app-image `
    --dest $thuMucDist `
    --name "UDPChatClient" `
    --input $staging `
    --main-jar "UDPmultiChat-1.0-SNAPSHOT.jar" `
    --main-class "com.mycompany.udpmultichat.ClientMain" `
    --java-options "-Dfile.encoding=UTF-8" `
    --java-options "-Dsun.stdout.encoding=UTF-8" `
    --java-options "-Dsun.stderr.encoding=UTF-8"

if ($LASTEXITCODE -ne 0) { throw "Loi khi jpackage UDPChatClient" }

# Tao script chay va huong dan Client
$huongDanClient = @"
======================================================================
           UDP MULTI-ROOM CHAT - MAY KHACH (CLIENT PORTABLE)
======================================================================

1. GIOI THIEU:
   - Day la ban chay doc lap danh cho Nguoi dung / May Khach (Client).
   - KHONG can cai dat Java hoac bat ky phan mem nao khac.
   - Chay truc tiep bang cach bam dup vao: UDPChatClient.exe

2. HUONG DAN KET NOI:
   - Buoc 1: Chay 'UDPChatClient.exe'.
   - Buoc 2: O man hinh Dang nhap / Dang ky:
     + Mo muc "Cai dat may chu" (neu can).
     + Nhap dia chi IP LAN cua May chu (vi du: 192.168.1.15).
     + Nhan [Kiem tra ket noi] de xac nhan ket noi UDP voi May chu.
   - Buoc 3: Dang ky tai khoan moi hoac Dang nhap.
   - Buoc 4: Tham gia phong Sanh chung, tao phong moi, gui tin nhan,
             sticker, tep dinh kem, trinh bay bao cao...

3. MO NHIEU CLIENT TREN CUNG MOT MAY:
   - De mo them Client 2 hoac Client 3 tren cung may tinh de thu nghiem,
     ban co the chay tep 'Chay-Client-Phien-2.bat' hoac 'Chay-Client-Phien-3.bat'
     de moi phien co file cau hinh rieng biet ma khong bi ghi de.
"@
Set-Content -Path (Join-Path $thuMucDist "UDPChatClient\HUONG_DAN.txt") -Value $huongDanClient -Encoding UTF8

$batClient = @"
@echo off
chcp 65001 > nul
start "" "%~dp0UDPChatClient.exe"
"@
Set-Content -Path (Join-Path $thuMucDist "UDPChatClient\Chay-Client.bat") -Value $batClient -Encoding ASCII

$batClient2 = @"
@echo off
chcp 65001 > nul
start "" "%~dp0UDPChatClient.exe" -Dudpmultichat.clientConfig=client-phien-2.properties
"@
Set-Content -Path (Join-Path $thuMucDist "UDPChatClient\Chay-Client-Phien-2.bat") -Value $batClient2 -Encoding ASCII

# 6. Tao tep ZIP de ban giao
Write-Host "[6/6] Dong goi cac tep ZIP phan phoi..." -ForegroundColor Yellow
$zipServer = Join-Path $thuMucDist "UDPChatServer-Windows-x64.zip"
$zipClient = Join-Path $thuMucDist "UDPChatClient-Windows-x64.zip"

if (Test-Path -LiteralPath $zipServer) { Remove-Item $zipServer -Force }
if (Test-Path -LiteralPath $zipClient) { Remove-Item $zipClient -Force }

Compress-Archive -Path (Join-Path $thuMucDist "UDPChatServer") -DestinationPath $zipServer -CompressionLevel Optimal
Compress-Archive -Path (Join-Path $thuMucDist "UDPChatClient") -DestinationPath $zipClient -CompressionLevel Optimal

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "   DONG GOI PORTABLE HOAN TAT THANH CONG!               " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Thu muc ket qua:" -ForegroundColor White
Write-Host "  -> $thuMucDist\UDPChatServer\" -ForegroundColor Cyan
Write-Host "  -> $thuMucDist\UDPChatClient\" -ForegroundColor Cyan
Write-Host "Tep ZIP ban giao:" -ForegroundColor White
Write-Host "  -> $zipServer" -ForegroundColor Green
Write-Host "  -> $zipClient" -ForegroundColor Green
