#!/usr/bin/env bash
# Chispa Engine - proyecto inicial (Hito 1: "hola mundo" con libGDX)
# Uso: dentro de la carpeta del repositorio, ejecutar:  bash setup-chispa.sh
set -e

mkdir -p .github/workflows
mkdir -p app/src/main/kotlin/com/chispa/engine

# ---------------------------------------------------------------
cat > settings.gradle <<'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "chispa-engine"
include ':app'
EOF

# ---------------------------------------------------------------
cat > build.gradle <<'EOF'
plugins {
    id 'com.android.application' version '8.5.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.24' apply false
}
EOF

# ---------------------------------------------------------------
cat > gradle.properties <<'EOF'
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
EOF

# ---------------------------------------------------------------
cat > .gitignore <<'EOF'
.gradle/
build/
local.properties
*.iml
.idea/
app/libs/
EOF

# ---------------------------------------------------------------
cat > app/build.gradle <<'EOF'
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

def gdxVersion = '1.12.1'

android {
    namespace 'com.chispa.engine'
    compileSdk 34

    defaultConfig {
        applicationId 'com.chispa.engine'
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName '0.0.1'
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
}

configurations {
    natives
}

dependencies {
    implementation "com.badlogicgames.gdx:gdx:${gdxVersion}"
    implementation "com.badlogicgames.gdx:gdx-backend-android:${gdxVersion}"
    natives "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-x86"
    natives "com.badlogicgames.gdx:gdx-platform:${gdxVersion}:natives-x86_64"
}

// Copia las librerias nativas de libGDX a app/libs antes de empaquetar
tasks.register('copyAndroidNatives') {
    doFirst {
        file('libs/armeabi-v7a/').mkdirs()
        file('libs/arm64-v8a/').mkdirs()
        file('libs/x86/').mkdirs()
        file('libs/x86_64/').mkdirs()

        configurations.natives.copy().files.each { jar ->
            def outputDir = null
            if (jar.name.endsWith('natives-armeabi-v7a.jar')) outputDir = file('libs/armeabi-v7a')
            if (jar.name.endsWith('natives-arm64-v8a.jar')) outputDir = file('libs/arm64-v8a')
            if (jar.name.endsWith('natives-x86.jar')) outputDir = file('libs/x86')
            if (jar.name.endsWith('natives-x86_64.jar')) outputDir = file('libs/x86_64')
            if (outputDir != null) {
                copy {
                    from zipTree(jar)
                    into outputDir
                    include '*.so'
                }
            }
        }
    }
}

tasks.matching { it.name.contains('merge') && it.name.contains('JniLibFolders') }.configureEach {
    dependsOn 'copyAndroidNatives'
}
EOF

# ---------------------------------------------------------------
cat > app/src/main/AndroidManifest.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature
        android:glEsVersion="0x00020000"
        android:required="true" />

    <application
        android:allowBackup="false"
        android:label="Chispa Engine"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">

        <activity
            android:name=".AndroidLauncher"
            android:exported="true"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|uiMode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
EOF

# ---------------------------------------------------------------
cat > app/src/main/kotlin/com/chispa/engine/AndroidLauncher.kt <<'EOF'
package com.chispa.engine

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration()
        config.useImmersiveMode = true
        config.useAccelerometer = false
        config.useCompass = false
        initialize(ChispaGame(), config)
    }
}
EOF

# ---------------------------------------------------------------
cat > app/src/main/kotlin/com/chispa/engine/ChispaGame.kt <<'EOF'
package com.chispa.engine

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Hito 1: dibuja un cuadrado naranja que sigue el dedo.
 * Usa las reglas de la especificacion: resolucion virtual 1280x720,
 * origen (0,0) en el centro de la pantalla y eje Y hacia arriba.
 */
class ChispaGame : ApplicationAdapter() {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var texture: Texture
    private lateinit var sprite: Sprite
    private val toque = Vector3()

    override fun create() {
        camera = OrthographicCamera()
        viewport = FitViewport(1280f, 720f, camera)
        camera.position.set(0f, 0f, 0f)

        batch = SpriteBatch()

        val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(1f, 0.55f, 0.1f, 1f))
        pixmap.fill()
        texture = Texture(pixmap)
        pixmap.dispose()

        sprite = Sprite(texture)
        sprite.setCenter(0f, 0f)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, false)
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime

        if (Gdx.input.isTouched) {
            toque.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(toque)
            val cx = sprite.x + sprite.width / 2f
            val cy = sprite.y + sprite.height / 2f
            val k = minOf(1f, 10f * dt)
            sprite.setCenter(cx + (toque.x - cx) * k, cy + (toque.y - cy) * k)
        }

        Gdx.gl.glClearColor(0.106f, 0.165f, 0.255f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        sprite.draw(batch)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        texture.dispose()
    }
}
EOF

# ---------------------------------------------------------------
cat > .github/workflows/build.yml <<'EOF'
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Compilar APK de prueba
        run: gradle assembleDebug --stacktrace

      - uses: actions/upload-artifact@v4
        with:
          name: chispa-debug-apk
          path: app/build/outputs/apk/debug/*.apk
EOF

echo ""
echo "Listo. Archivos creados. Siguiente paso:"
echo "  git add . && git commit -m 'Hito 1: hola mundo con libGDX' && git push"
