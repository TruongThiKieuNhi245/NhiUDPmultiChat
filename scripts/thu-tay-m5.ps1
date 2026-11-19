# Vai tro: Mo Server va 3 Client that voi fixture moi trong .selftest.
# Ai goi: nguoi thu tay M5. Goi: Maven, ChuanBiThuTayM5, ServerMain, ClientMain.
[CmdletBinding()]
param(
    [switch]$ChiChuanBi,
    [string]$Maven = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd',
    [string]$Java = ''
)

$ErrorActionPreference = 'Stop'
$gocDuAn = Split-Path -Parent $PSScriptRoot
$thuTests = Join-Path $gocDuAn '.selftest'

# Khong ghi fixture qua junction/symlink tro ra ngoai workspace.
if (Test-Path -LiteralPath $thuTests) {
    $muc = Get-Item -LiteralPath $thuTests -Force
    if (($muc.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw '.selftest la lien ket. Dung de tranh ghi ngoai workspace.'
    }
}
New-Item -ItemType Directory -Path $thuTests -Force | Out-Null
$tenPhien = 'thu-tay-m5-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$thuPhien = Join-Path $thuTests $tenPhien
New-Item -ItemType Directory -Path $thuPhien | Out-Null

# Uu tien JDK21 da khai bao trong cau hinh du an; khong sua JAVA_HOME/PATH.
if ([string]::IsNullOrWhiteSpace($Java)) {
    $tepIde = Join-Path $gocDuAn '.vscode/settings.json'
    if (Test-Path -LiteralPath $tepIde) {
        $caiDat = Get-Content -LiteralPath $tepIde -Raw -Encoding UTF8 | ConvertFrom-Json
        $jdk = $caiDat.'java.configuration.runtimes' | Where-Object { $_.name -eq 'JavaSE-21' } | Select-Object -First 1
        if ($jdk) { $Java = Join-Path $jdk.path 'bin/java.exe' }
    }
    if (-not $Java -or -not (Test-Path -LiteralPath $Java)) { $Java = (Get-Command java.exe -ErrorAction Stop).Source }
}
if (-not (Test-Path -LiteralPath $Maven)) { throw "Khong tim thay Maven: $Maven. Truyen tham so -Maven neu can." }
$tepPhienBan = Join-Path $thuPhien 'java-version.log'
$kiemJava = Start-Process -FilePath $Java -ArgumentList '-version' -WindowStyle Hidden -RedirectStandardError $tepPhienBan -PassThru -Wait
if ($kiemJava.ExitCode -ne 0 -or (Get-Content -LiteralPath $tepPhienBan -Raw) -notmatch 'version\s+"21[.\-]') {
    throw "Can Java21. Xem $tepPhienBan hoac truyen -Java duong-dan-java.exe."
}

# Maven va fixture Java luon chay tu project, du goi script o CWD khac.
$tepClasspath = Join-Path $thuPhien 'classpath.txt'
Push-Location -LiteralPath $gocDuAn
try {
    & $Maven -q test-compile dependency:build-classpath "-Dmdep.outputFile=.selftest/$tenPhien/classpath.txt" *> (Join-Path $thuPhien 'compile.log')
    if ($LASTEXITCODE -ne 0) { throw "Compile loi. Xem $thuPhien\compile.log" }
    $classpath = (Join-Path $gocDuAn 'target/test-classes') + ';' + (Join-Path $gocDuAn 'target/classes') + ';' + (Get-Content -LiteralPath $tepClasspath -Raw).Trim()
    & $Java '-Dfile.encoding=UTF-8' '-Dsun.stdout.encoding=UTF-8' '-Dsun.stderr.encoding=UTF-8' -cp $classpath com.mycompany.udpmultichat.ChuanBiThuTayM5 (Join-Path $thuPhien 'data') *> (Join-Path $thuPhien 'fixture.log')
    if ($LASTEXITCODE -ne 0) { throw "Tao fixture loi. Xem $thuPhien\fixture.log" }
}
finally { Pop-Location }

# Moi JVM Client co file preferences rieng, chi chua username/server.
$utf8 = [Text.UTF8Encoding]::new($false)
$taiKhoan = @('admin', 'm5_a', 'm5_b')
for ($i = 0; $i -lt $taiKhoan.Count; $i++) {
    [IO.File]::WriteAllText((Join-Path $thuPhien "client-$($i+1).properties"), "username=$($taiKhoan[$i])`nserver=127.0.0.1`n", $utf8)
}
$huongDan = @(
    'BO THU TAY M5 - DU LIEU MAU RIENG'
    "Thu muc: $thuPhien"
    '1. Tren cua so Server, bam Bat dau (UDP5000).'
    '2. Client admin: admin / admin123. Doi mat khau khi duoc yeu cau.'
    '3. Hai Client con lai: m5_a / abcdef123; m5_b / abcdef123.'
    '4. Thu 5 tab Quan tri, tao/reset/khoa user, phong, mute/kick/ban/broadcast/report.'
    '5. Gui tep-mau.txt va tep-mau-5MiB.bin; doi chieu CRC voi tep-mau.properties.'
    'Chi la fixture thu tay; khong dung mat khau mau cho du lieu that.'
    'Dong cac cua so khi xong. Script khong xoa fixture/log va khong dong app khac.'
    'ChiChuanBi chi tao du lieu, khong mo cua so va khong phai test UI PASS.'
) -join [Environment]::NewLine
[IO.File]::WriteAllText((Join-Path $thuPhien 'HUONG_DAN.txt'), $huongDan, $utf8)
if ($ChiChuanBi) { Write-Output "CHI_CHUAN_BI=$thuPhien"; return }

# Khong dong/chiem Server dang chay voi du lieu that.
$congBan = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveUdpListeners() | Where-Object { $_.Port -eq 5000 }
if ($congBan) { throw "UDP5000 dang duoc dung. Tu dong Server cu truoc; fixture da tao tai $thuPhien" }
$javaw = Join-Path (Split-Path -Parent $Java) 'javaw.exe'
if (-not (Test-Path -LiteralPath $javaw)) { throw "Khong tim thay javaw.exe trong JDK21: $javaw" }
$tienTrinh = @()

# Cua so hien thi de nguoi dung thao tac; khong tu bam hay tu dang nhap.
function Mo-CuaSoThu {
    param([string]$VaiTro, [string]$Lop, [string]$TepClient)
    $thamSo = @('-Djava.net.preferIPv4Stack=true', '-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-Dsun.stderr.encoding=UTF-8')
    if ($TepClient) { $thamSo += ('-Dudpmultichat.clientConfig="' + $TepClient + '"') }
    $thamSo += @('-cp', ('"' + $classpath + '"'), $Lop)
    $p = Start-Process -FilePath $javaw -ArgumentList $thamSo -WorkingDirectory $thuPhien -WindowStyle Normal -RedirectStandardOutput (Join-Path $thuPhien "$VaiTro.log") -RedirectStandardError (Join-Path $thuPhien "$VaiTro.err") -PassThru
    return [pscustomobject]@{ VaiTro = $VaiTro; ProcessId = $p.Id; ThuMuc = $thuPhien }
}
$tienTrinh += Mo-CuaSoThu -VaiTro 'server' -Lop 'com.mycompany.udpmultichat.server.ServerMain' -TepClient ''
for ($i = 1; $i -le 3; $i++) {
    $tienTrinh += Mo-CuaSoThu -VaiTro "client-$i" -Lop 'com.mycompany.udpmultichat.ClientMain' -TepClient (Join-Path $thuPhien "client-$i.properties")
}
$tienTrinh | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $thuPhien 'tien-trinh.json') -Encoding UTF8
Write-Output "DA_KHOI_CHAY=$thuPhien"
Write-Output $huongDan
