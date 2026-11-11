
<div align="center">

# 🔥 Chispa Engine

**Motor de videojuegos 2D ligero y de código abierto para Android.**
Crea, edita y ejecuta tus proyectos directamente desde el dispositivo —sin PC, sin fricción.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](#)
[![LibGDX](https://img.shields.io/badge/Runtime-LibGDX-E44D26)](#)
[![AGP](https://img.shields.io/badge/AGP-8.5.2-4285F4?logo=gradle&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](#-licencia)
[![Estado](https://img.shields.io/badge/Estado-En%20desarrollo-orange)](#)

</div>

---

## 📖 ¿Qué es Chispa Engine?

**Chispa Engine** es un motor de videojuegos 2D para Android inspirado en plataformas de programación por bloques. Su objetivo es que puedas **construir, probar y distribuir** tus juegos desde el propio teléfono, sin necesidad de un entorno de escritorio.

A diferencia de otros motores por bloques, Chispa está **enfocado en el desarrollo de juegos**: escenas, objetos, scripts, assets y exportación de APK conviven en una misma app, sin capas innecesarias. El motor se apoya en **LibGDX** para el renderizado y el audio, y **FreeType** para tipografías personalizadas.

> La meta del proyecto es simple: **plasmar tus ideas en un juego jugable en el menor número de pasos posible**, escuchando a la comunidad que lo usa.

---

## ✨ Características principales

### 🎬 Sistema de escenas
Los proyectos se organizan en **escenas** (`escenas/*.json`), y cada escena contiene **objetos** que responden a los **scripts** que les asignas. Puedes tener menús, niveles, pantallas de victoria, etc., sin mezclar lógica.

### 🧩 Programación por bloques
Construye el comportamiento de tus objetos mediante **bloques**, sin escribir sintaxis a mano. El intérprete (`Interprete.kt` + `Hilo.kt`) ejecuta cada bloque a **60 pasos por segundo**, con soporte para:

- **Eventos**: `al_iniciar`, `al_tocar_pantalla`, `al_tocar_objeto`, `al_recibir_mensaje`, `al_colisionar`, `al_empezar_clon`.
- **Bloques de movimiento**: `mover`, `ir_a`, `rotar`, `mover_hacia`, `rebotar_borde`.
- **Control de flujo**: `esperar`, `repetir`, `por_siempre`, `si / sino`, `esperar_hasta`, `detener_script`.
- **Estado y variables**: `asignar`, `cambiar`, operadores `suma / resta / mult / div`, comparadores `mayor / menor / igual`, lógicos `y / o / no`, `azar`, `tocando_pantalla`, `toque_x`, `toque_y`, `temporizador`, `tocando_objeto`.
- **Presentación**: `mostrar_texto`, `borrar_texto`, `reproducir_sonido`, `siguiente_disfraz`, `cambiar_disfraz`, `mostrar`, `ocultar`.
- **Instancias**: `crear_clon`, `borrar_clon` (con límite de 400 objetos por escena), `enviar_mensaje`, `ir_a_escena`.

### 📦 Exportación directa a APK
Desde la propia aplicación puedes **generar un APK instalable** con tu juego, firmado y listo para instalar. El proceso:

1. Copia el APK base de Chispa Engine.
2. **Parchea el `AndroidManifest.xml` binario** (AXML) para cambiar el paquete y la etiqueta.
3. Sustituye el icono del launcher por el que hayas elegido.
4. Mete tus `assets/juego/` dentro del APK.
5. Firma el resultado con **`apksig`** y una clave del **AndroidKeyStore** (alias `chispa_exportador`), con firma **v1 + v2**.
6. Guarda el APK en **Descargas** (o en el almacenamiento externo de la app si el dispositivo es anterior a Android 10).

### 🎨 Editor visual integrado
- **EditorActivity**: gestiona escenas, crea/edita objetos y ajustes del proyecto (variables visibles, debug, orientación, color de fondo, sonidos, fuentes, icono).
- **VistaActivity** + **LienzoView**: editor visual *drag & drop* con selección, arrastre, escalado (círculo naranja), rotación (círculo azul), duplicado, borrado, cambio de capa y cuadrícula.
- **ScriptsActivity**: editor de scripts por bloques con jerarquía visual (cuerpo y rama *sino* indentados), reordenar bloques, agregar/quitar y edición de expresiones con un selector de tipo (`num`, `var`, `azar`, `suma`, `toque_x`, `tocando_objeto`, …).

### 🪶 Ligero y de código abierto
- Núcleo pequeño, sin dependencias pesadas más allá de LibGDX y FreeType.
- Código abierto bajo **Apache 2.0**: puedes revisarlo, modificarlo y aprender de él.
- **Tu proyecto es tuyo**: puedes publicarlo o distribuirlo libremente.

### 🗣️ Desarrollado escuchando a la comunidad
Las decisiones del motor se toman pensando en los creadores que lo usan. Si algo estorba, se ajusta; si algo falta, se añade.

---

## ⚙️ Requisitos técnicos

### Plataforma y toolchain

| Componente          | Versión / Requisito                                          |
| ------------------- | ------------------------------------------------------------ |
| Plataforma          | **Android** (minSdk según proyecto; el exportador usa `setMinSdkVersion(26)`) |
| Lenguaje            | **Kotlin 1.9.24**                                            |
| Runtime gráfico     | **LibGDX** (`ApplicationAdapter`, `SpriteBatch`, `FitViewport`) |
| Fuentes             | **LibGDX FreeType** para `.ttf` dinámicos                    |
| Firma de APK        | **`com.android.apksig`** + **AndroidKeyStore**              |
| Build System        | **Gradle** + **Android Gradle Plugin 8.5.2**                 |
| JDK                 | **JDK 17**                                                   |
| Entorno de trabajo  | Android Studio (recomendado) o Gradle CLI                    |
| CI                  | **GitHub Actions** con Gradle 8.9 (`.github/workflows/build.yml`) |

### Dependencias internas relevantes

- `com.badlogic.gdx` — render, input, audio, matemáticas, JSON.
- `com.badlogic.gdx.graphics.g2d.freetype` — fuentes `.ttf`.
- `com.android.apksig` — firma v1 + v2 del APK exportado.
- `org.json` (Android) — edición de proyectos en el editor.
- Recursos Android estándar (`R.mipmap.ic_launcher`, `R.drawable.ic_launcher_foreground`).

---

## 🧱 Arquitectura del proyecto

```
com.chispa.engine/
├── AndroidLauncher.kt      # Activity LibGDX: orientación + arranque del motor
├── Axml.kt                 # Parcheador de AndroidManifest.xml binario (AXML)
├── ChispaGame.kt           # ApplicationAdapter: bucle, cámara, sprites, debug
├── EditorActivity.kt       # Editor de proyectos, escenas y objetos
├── Exportador.kt           # Empaquetado + firma + guardado del APK
├── LienzoView.kt           # Canvas de edición visual (drag/scale/rotate)
├── MainActivity.kt         # Lista de proyectos / punto de entrada del APK exportado
├── Proyectos.kt            # CRUD de proyectos en filesDir/proyectos/
├── Recursos.kt             # Cache de Textures y Sounds (LibGDX)
├── ScriptsActivity.kt      # Editor de scripts por bloques
├── Textos.kt               # Render de TextoDef con FreeType y estilos
├── VistaActivity.kt        # Contenedor del LienzoView + toolbar
└── core/
    ├── CargadorProyecto.kt # JSON → Proyecto / Escena / Objeto
    ├── Clonar.kt           # Copia de Objeto para crear clones
    ├── Colision.kt         # AABB orientado por rotación
    ├── Contexto.kt         # Estado compartido: variables, textos, colas, etc.
    ├── Hilo.kt             # Ejecutor de bloques con pila de marcos
    ├── Interprete.kt       # Orquesta hilos, eventos de toque y colisiones
    ├── LectorBloques.kt    # JSON → List<Bloque> / Script / variables
    ├── Modelo.kt           # Proyecto, Escena, Objeto, Script, Bloque
    └── TextoDef.kt         # data class para texto renderizable
```

### Ciclo de ejecución (resumen)

1. `AndroidLauncher` lee la orientación del proyecto y crea `ChispaGame`.
2. `ChispaGame.create()` carga `proyecto.json` y la escena inicial, y crea el `Interprete`.
3. En cada `render(dt)`: el intérprete avanza **en pasos fijos de 1/60 s**, atiende toques (con *unproject* de la cámara) y procesa eventos. Luego se dibujan los objetos visibles ordenados por `capa` y, encima, los `TextoDef` activos.
4. Si un bloque `ir_a_escena` se ejecutó, `ChispaGame.cambiarEscena()` recarga la escena y crea un nuevo `Interprete` conservando las variables.

---

## 🚀 Compilación y ejecución

```bash
# Clonar el repositorio
git clone https://github.com/Emirlas/Chispa-Engine.git
cd Chispa-Engine

# Compilar en modo debug
./gradlew assembleDebug

# Instalar en un dispositivo o emulador conectado
./gradlew installDebug
```

O simplemente abre el proyecto en **Android Studio** y ejecuta la configuración `app`.

> 💡 El workflow de GitHub Actions (`.github/workflows/build.yml`) compila el APK en cada push a `main` y lo publica como artefacto `chispa-debug-apk`.

---

## 📝 Formato de proyecto

Cada proyecto vive en `filesDir/proyectos/<Nombre>/` con esta estructura:

```
<Nombre>/
├── proyecto.json          # Metadatos, pantalla, escena inicial, variables globales
├── escenas/
│   ├── menu.json
│   ├── escena_1.json
│   └── ...
└── assets/
    ├── imagenes/          # .png referenciados por "imagen" o "disfraces"
    ├── sonidos/           # .wav / .mp3 / .ogg
    └── fuentes/           # .ttf
```

Ejemplo mínimo de `proyecto.json`:

```json
{
  "formato": 1,
  "tipo": "2D",
  "nombre": "Toca el cuadrado",
  "idPaquete": "com.chispa.demo",
  "version": "1.0",
  "motor": "0.1.0",
  "pantalla": { "ancho": 1280, "alto": 720, "orientacion": "horizontal" },
  "escenaInicial": "menu",
  "variablesGlobales": [
    { "nombre": "puntos", "valor": 0 }
  ]
}
```

---

## 🗺️ Estado del proyecto

- [x] Sistema de escenas
- [x] Editor de scripts por bloques (con jerarquía y expresiones anidadas)
- [x] Editor visual *drag & drop* (mover, escalar, rotar, capas, duplicar)
- [x] Renderizado de texto configurable (color, tamaño, alineación, estilo, fuente)
- [x] Fuentes `.ttf` personalizadas (FreeType)
- [x] Sonidos (`.wav`, `.mp3`, `.ogg`)
- [x] Clonación de objetos (límite de 400 por escena)
- [x] Colisiones AABB orientadas
- [x] Mensajes entre objetos y colisiones por evento
- [x] Icono personalizado por juego
- [x] **Exportación directa a APK firmado** (v1 + v2, AndroidKeyStore)
- [x] CI con GitHub Actions
- [ ] Documentación de la API interna
- [ ] Proyectos de ejemplo adicionales
- [ ] Galería de proyectos de la comunidad
- [ ] Soporte de mapas / tiles

> ⚠️ Proyecto en desarrollo activo: la API y la estructura interna pueden cambiar entre versiones. El campo `formato` de `proyecto.json` permite versionar el formato de archivo en el futuro.

---

## 🤝 Contribuir

1. Haz un **fork** del repositorio.
2. Crea una rama descriptiva: `git checkout -b feature/mi-aporte`.
3. Realiza tus cambios con commits claros.
4. Abre un **Pull Request** explicando qué aporta.

Las ideas, reportes de bugs y sugerencias también son bienvenidos en la sección de **Issues**.

---

## 📄 Licencia

Este proyecto se distribuye bajo la **Apache License 2.0**. Consulta el archivo [`LICENSE`](LICENSE) para el texto legal completo.

```
Copyright 2026 Emirlas

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

> Los proyectos creados con Chispa Engine **son propiedad de sus autores**: puedes publicarlos o distribuirlos como prefieras.

---

<div align="center">

Hecho con 🔥 por [@Emirlas](https://github.com/Emirlas)

</div>