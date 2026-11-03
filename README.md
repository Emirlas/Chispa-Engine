<div align="center">

# 🔥 Chispa Engine

**Motor de videojuegos ligero y de código abierto para Android.**
Crea, edita y ejecuta tus proyectos directamente desde el dispositivo —sin PC, sin fricción.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](#)
[![AGP](https://img.shields.io/badge/AGP-8.5.2-4285F4?logo=gradle&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](#-licencia)
[![Estado](https://img.shields.io/badge/Estado-En%20desarrollo-orange)](#)

</div>

---

## 📖 ¿Qué es Chispa Engine?

**Chispa Engine** es un motor de videojuegos para Android inspirado en plataformas de
programación por bloques. Su objetivo es que puedas **construir, probar y distribuir**
tus juegos desde el propio teléfono, sin necesidad de un entorno de escritorio.

A diferencia de otros motores por bloques, Chispa está **enfocado en el desarrollo de
juegos**: escenas, objetos, scripts y exportación de APK conviven en una misma app,
sin capas innecesarias.

> La meta del proyecto es simple: **plasmar tus ideas en un juego jugable en el menor
> número de pasos posible**, escuchando a la comunidad que lo usa.

---

## ✨ Características principales

### 🎬 Sistema de escenas
Los proyectos se organizan en **escenas**, y cada escena contiene objetos que
responden a los **scripts de código** que les asignas. Así puedes separar menús,
niveles, pantallas de fin de juego, etc., sin mezclar lógica.

### 🧩 Programación por bloques
Construye el comportamiento de tus objetos mediante **bloques de código**, sin
necesidad de escribir sintaxis a mano. El motor interpreta esos bloques y los
ejecuta en el hilo principal del juego.

### 📦 Exportación directa a APK
Desde la propia aplicación puedes **generar un APK listo para instalar o publicar**.
Sin pasos intermedios, sin configuraciones extra, sin dependencias externas.

### 🪶 Ligero y de código abierto
- Núcleo pequeño y sin dependencias pesadas.
- Código abierto bajo **Apache 2.0**: puedes revisarlo, modificarlo y aprender de él.
- **Tu proyecto es tuyo**: puedes publicarlo o distribuirlo libremente.

### 🗣️ Desarrollado escuchando a la comunidad
Las decisiones del motor se toman pensando en los creadores que lo usan. Si algo
estorba, se ajusta; si algo falta, se añade.

---

## ⚙️ Requisitos técnicos

| Componente        | Versión / Requisito                                          |
| ----------------- | ------------------------------------------------------------ |
| Plataforma        | **Android** (ejecución del motor y de los juegos exportados) |
| Lenguaje          | **Kotlin 1.9.24**                                            |
| Build System      | **Gradle** con **Android Gradle Plugin 8.5.2**               |
| JDK               | **JDK 17** (compatible con AGP 8.5.x)                        |
| SDK               | Android SDK con la plataforma definida en el proyecto        |
| Entorno de trabajo | Android Studio (recomendado) o Gradle CLI                   |

### Dependencias internas relevantes

- `com.android.application` (AGP 8.5.2)
- `org.jetbrains.kotlin.android` (Kotlin 1.9.24)
- Sistema de recursos Android para escenas y assets (`assets/proyecto/`)

</div>
