$ErrorActionPreference = "Stop"

$TOOLS_DIR = "..\tools\windows"
$AAPT2 = "$TOOLS_DIR\aapt2.exe"
$ANDROID_JAR = "$TOOLS_DIR\android.jar"
$ECJ_JAR = "$TOOLS_DIR\ecj-3.45.0.jar"
$D8_JAR = "$TOOLS_DIR\d8.jar"
$ZIPALIGN = "$TOOLS_DIR\zipalign.exe"
$APKSIGNER_JAR = "$TOOLS_DIR\apksigner.jar"
$KEYSTORE = "$TOOLS_DIR\promaster.keystore"
$KEY_ALIAS = "promaster"
$KEY_PASS = "promaster"
$PROJ = ".\\app\\src\\main"
$BUILD = ".\\build"

Write-Host "Building Tetris APK..."

if (Test-Path $BUILD) {
    Remove-Item $BUILD -Recurse -Force
}

New-Item -ItemType Directory -Force -Path `
    "$BUILD\\res", `
    "$BUILD\\classes", `
    "$BUILD\\dex", `
    "$BUILD\\apk", `
    "$BUILD\\apk_final" | Out-Null

Write-Host "Link manifest/resources..."
$hasRes = (Get-ChildItem "$PROJ\\res" -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0
if ($hasRes) {
    & $AAPT2 compile --dir "$PROJ\\res" -o "$BUILD\\res\\compiled.zip"
    & $AAPT2 link `
        -o "$BUILD\\apk\\resources.apk" `
        -I $ANDROID_JAR `
        --manifest "$PROJ\\AndroidManifest.xml" `
        --auto-add-overlay `
        "$BUILD\\res\\compiled.zip"
} else {
    & $AAPT2 link `
        -o "$BUILD\\apk\\resources.apk" `
        -I $ANDROID_JAR `
        --manifest "$PROJ\\AndroidManifest.xml"
}

Write-Host "Compile Java..."
java -jar $ECJ_JAR -source 8 -target 8 -encoding UTF-8 `
    -bootclasspath $ANDROID_JAR `
    -classpath $ANDROID_JAR `
    -d "$BUILD\\classes" `
    "$PROJ\\java\\com\\tetris\\TetrisUltra.java"

Write-Host "Run R8..."
$classes = Get-ChildItem "$BUILD\\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
java -cp $D8_JAR com.android.tools.r8.R8 `
    --release `
    --dex `
    --min-api 26 `
    --lib $ANDROID_JAR `
    --pg-conf .\\proguard.pro `
    --pg-map-output "$BUILD\\mapping.txt" `
    --output "$BUILD\\dex" `
    $classes

Write-Host "Package APK..."
Copy-Item "$BUILD\\apk\\resources.apk" "$BUILD\\apk_final\\app-unsigned.apk"
jar uf "$BUILD\\apk_final\\app-unsigned.apk" -C "$BUILD\\dex" classes.dex

Write-Host "Zipalign..."
& $ZIPALIGN -f 4 "$BUILD\\apk_final\\app-unsigned.apk" "$BUILD\\apk_final\\app-aligned.apk"

if (-not (Test-Path $KEYSTORE)) {
    Write-Host "Create Promaster keystore..."
    keytool -genkeypair `
        -alias $KEY_ALIAS `
        -keyalg EC `
        -groupname secp256r1 `
        -sigalg SHA256withECDSA `
        -validity 10000 `
        -keystore $KEYSTORE `
        -storetype PKCS12 `
        -storepass $KEY_PASS `
        -keypass $KEY_PASS `
        -dname "CN=Promaster,O=Promaster Development,C=US"
}

Write-Host "Sign APK..."
java -jar $APKSIGNER_JAR sign `
    --ks $KEYSTORE `
    --ks-key-alias $KEY_ALIAS `
    --ks-pass pass:$KEY_PASS `
    --key-pass pass:$KEY_PASS `
    --v1-signing-enabled false `
    --v2-signing-enabled true `
    --v3-signing-enabled false `
    --v4-signing-enabled false `
    --out "$BUILD\\apk_final\\LowBlocks.apk" `
    "$BUILD\\apk_final\\app-aligned.apk"

$apk = Get-Item "$BUILD\\apk_final\\LowBlocks.apk"
Write-Host ""
Write-Host ("Done: {0} bytes" -f $apk.Length)
