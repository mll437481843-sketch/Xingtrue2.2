#!/system/bin/sh
set -u
ROOT=$(pwd)
P="$ROOT/.probe/tx"
AJ="$ROOT/.probe/lib/x/plat/android-34/android.jar"
R8="$ROOT/.probe/lib/r8.jar"
AS="$ROOT/.probe/lib/apksig.jar"
APP="$ROOT/app"
OUT="$ROOT/build-p2p"
PKG=com.starlab.pic2pdf
PKGPATH=com/starlab/pic2pdf

export LD_LIBRARY_PATH="$P/lib"
export PATH="$P/bin:$PATH"
export JAVA_HOME="$P"
export TMPDIR="$ROOT/.probe/tmp"
mkdir -p "$TMPDIR"

die() { echo "FAILED: $1"; exit 1; }

JAVA="$P/lib/jvm/java-21-openjdk/bin/java"
JAVAC="$P/lib/jvm/java-21-openjdk/bin/javac"
KEYTOOL="$P/lib/jvm/java-21-openjdk/bin/keytool"
AAPT2="$P/bin/aapt2"
KOTLINC_JAR="$P/opt/kotlin/lib/kotlin-compiler.jar"
STDLIB="$P/opt/kotlin/lib/kotlin-stdlib.jar"
ZIP=$(command -v zip 2>/dev/null)

CP="$AJ:$STDLIB"
rm -rf "$OUT"; mkdir -p "$OUT"

echo "== 1) kotlinc =="
mkdir -p "$OUT/classes"
"$JAVA" -Xmx1500m -cp "$KOTLINC_JAR" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -jvm-target 11 -nowarn \
  -classpath "$CP" \
  -d "$OUT/classes" \
  $(ls "$APP"/app/src/main/java/$PKGPATH/*.kt) 2>&1 | tail -40
[ -f "$OUT/classes/$PKGPATH/MainActivity.class" ] || die "kotlinc failed"

echo "== 2) dex =="
mkdir -p "$OUT/dex"
CLASSES=$(find "$OUT/classes" -name "*.class" | tr ' ' '\n' | tr '\n' ' ')
"$JAVA" -Xmx1500m -cp "$R8" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
  --output "$OUT/dex" $CLASSES "$STDLIB" 2>&1 | tail -20
[ -f "$OUT/dex/classes.dex" ] || die "dex failed"

echo "== 3) aapt2 =="
"$AAPT2" compile --dir "$APP/app/src/main/res" -o "$OUT/res.zip" 2>&1 | tail -8
[ -f "$OUT/res.zip" ] || die "aapt2 compile failed"
"$AAPT2" link -o "$OUT/base.apk" -I "$AJ" \
  --manifest "$APP/app/src/main/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0 "$OUT/res.zip" 2>&1 | tail -12
[ -f "$OUT/base.apk" ] || die "aapt2 link failed"

echo "== 4) zip dex =="
cd "$OUT/dex" || die cd
for D in classes*.dex; do zip -q -X "$OUT/base.apk" "$D" || die "zip $D"; done
cd "$ROOT" || die cdback
unzip -l "$OUT/base.apk" | grep dex

echo "== 5) sign =="
KS="$ROOT/.probe/p2p.keystore"
if [ ! -f "$KS" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias p2p -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Pic2PDF,OU=Local,O=Local,L=Local,ST=Local,C=CN" >/dev/null 2>&1 || die "keytool failed"
fi
if [ ! -f "$OUT/Sign.class" ]; then
  "$JAVAC" -nowarn -encoding UTF-8 -classpath "$AS" -d "$OUT" "$ROOT/.probe/sign/Sign.java" 2>&1 | head -10
fi
rm -f "$OUT/pic2pdf-signed.apk"
"$JAVA" -cp "$AS:$OUT" Sign "$KS" android p2p "$OUT/base.apk" "$OUT/pic2pdf-signed.apk" 2>&1 | tail -6
[ -s "$OUT/pic2pdf-signed.apk" ] || die "sign failed"
ls -l "$OUT/pic2pdf-signed.apk"
"$AAPT2" dump badging "$OUT/pic2pdf-signed.apk" 2>&1 | head -4
echo "APK_DONE"

