param(
    [Parameter(Mandatory)] [string] $JavaHome,
    [Parameter(Mandatory)] [string] $AndroidSdk,
    [string] $GradleCache = "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1"
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$java = Join-Path $JavaHome 'bin'
$sdkTools = Join-Path $AndroidSdk 'build-tools/36.0.0'
$platform = Join-Path $AndroidSdk 'platforms/android-34/android.jar'
$stage = $null

Push-Location (Join-Path $PSScriptRoot '..')
try {
    $output = New-Item -ItemType Directory -Force 'build/android-debug'
    $stage = (New-Item -ItemType Directory (Join-Path $output.FullName ([guid]::NewGuid()))).FullName
    $assets = New-Item -ItemType Directory "$stage/assets/circuit"
    New-Item -ItemType Directory "$stage/res/drawable", "$stage/classes", "$stage/dex" | Out-Null

    $groups = 'org.gwtproject', 'com.google.jsinterop', 'org.ow2.asm', 'colt',
        'commons-collections', 'commons-io', 'com.ibm.icu', 'com.google.code.gson',
        'javax.validation', 'org.eclipse.jdt', 'tapestry', 'com.google.code.findbugs',
        'jakarta.validation'
    $jars = foreach ($group in $groups) {
        Get-ChildItem "$GradleCache/$group" -Filter '*.jar' -Recurse -ErrorAction SilentlyContinue
    }
    $jars = $jars | Where-Object {
        $_.FullName -notmatch '[\\/]org\.gwtproject[\\/]' -or $_.Name -in 'gwt-dev-2.13.1.jar', 'gwt-user-2.13.1.jar'
    }
    $classpath = 'src;' + ($jars.FullName -join ';')
    Copy-Item 'war/*' $assets.FullName -Recurse -Exclude 'WEB-INF', 'service-worker.js', '*.php'
    & "$java/java.exe" -Xmx1400m '-Dgwt.persistentunitcache=false' -cp $classpath com.google.gwt.dev.Compiler -war $assets.FullName -workDir "$stage/gwt-work" -strict -sourceLevel 8 -localWorkers 1 com.lushprojects.circuitjs1.circuitjs1

    Copy-Item 'war/icon512.png' "$stage/res/drawable/circuit_icon.png"
    & "$sdkTools/aapt2.exe" compile --dir "$stage/res" -o "$stage/resources.zip"
    & "$sdkTools/aapt2.exe" link -I $platform --manifest 'android/AndroidManifest.xml' -o "$stage/resources.apk" "$stage/resources.zip"
    & "$java/javac.exe" --release 8 -classpath $platform -d "$stage/classes" 'android/MainActivity.java'
    & "$java/jar.exe" cf "$stage/classes.jar" -C "$stage/classes" .
    $env:JAVA_HOME = $JavaHome
    & "$sdkTools/d8.bat" --lib $platform --min-api 23 --debug --output "$stage/dex" "$stage/classes.jar"

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::Open("$stage/resources.apk", 'Update')
    try {
        $assetRoot = Join-Path $stage 'assets'
        foreach ($file in Get-ChildItem $assets.FullName -File -Recurse) {
            $relative = [IO.Path]::GetRelativePath($assetRoot, $file.FullName).Replace('\', '/')
            if (!$relative.StartsWith('circuit/WEB-INF/')) {
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, "assets/$relative") | Out-Null
            }
        }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, "$stage/dex/classes.dex", 'classes.dex') | Out-Null
    } finally {
        $archive.Dispose()
    }

    $key = Join-Path $output.FullName 'debug.keystore'
    if (!(Test-Path $key)) {
        & "$java/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
    }
    $apk = Join-Path $output.FullName 'CircuitJS-Mobile-debug.apk'
    & "$sdkTools/zipalign.exe" -f 4 "$stage/resources.apk" "$stage/aligned.apk"
    $signedApk = Join-Path $stage 'signed.apk'
    & "$sdkTools/apksigner.bat" sign --ks $key --ks-pass pass:android --v4-signing-enabled false --out $signedApk "$stage/aligned.apk"
    & "$sdkTools/apksigner.bat" verify --verbose $signedApk
    [IO.File]::Move($signedApk, $apk, $true)
    $oldSignature = "$apk.idsig"
    if (Test-Path -LiteralPath $oldSignature -PathType Leaf) {
        Remove-Item -LiteralPath $oldSignature
    }
    Get-Item $apk
} finally {
    try {
        if ($stage -and (Test-Path -LiteralPath $stage -PathType Container)) {
            $resolvedStage = Get-Item -LiteralPath $stage -Force
            if ($resolvedStage.Parent.FullName -ne $output.FullName -or
                ($resolvedStage.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                throw "Unexpected build staging path: $stage"
            }
            Remove-Item -LiteralPath $resolvedStage.FullName -Recurse -Force
        }
    } finally {
        Pop-Location
    }
}
