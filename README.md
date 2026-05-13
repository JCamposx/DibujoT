# DibujoT

App Android para enviar archivos G-code a un plotter CNC casero vía USB OTG.

Desarrollada como proyecto académico para el curso de Robótica. La app muestra una galería de 8 dibujos y envía el G-code correspondiente línea por línea al Arduino, esperando el ACK del firmware entre cada instrucción.

---

## Requisitos

| Requisito | Detalle |
|-----------|---------|
| Android | 7.0+ (API 24) |
| Cable | USB OTG (USB-A hembra a USB-C o micro-USB) |
| Hardware | Arduino con firmware que responde `ok` / `error` por serial |
| Build | Android Studio Hedgehog o superior |

---

## Cómo correr el proyecto

```bash
# 1. Clonar
git clone <url-del-repo>

# 2. Abrir en Android Studio y sincronizar Gradle

# 3. Correr los tests
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew test

# 4. Instalar en dispositivo/emulador
./gradlew installDebug
```

---

## Cómo usar la app

1. Conectar el Arduino al celular con el cable OTG
2. Abrir la app — aparece la galería con los 8 dibujos disponibles
3. Tocar el dibujo que se quiere trazar
4. En la pantalla de envío, presionar **Enviar al plotter**
5. La app pide permiso USB, conecta y empieza a enviar automáticamente
6. La barra de progreso muestra el avance línea por línea

---

## Arquitectura

```
app/
├── data/           # DrawingRepository, DrawingItem, GcodeLoader
├── gcode/          # GcodeParser, GcodeSender
├── serial/         # SerialPort (interfaz), UsbSerialPort, FakeSerialPort
├── ui/
│   ├── gallery/    # GalleryActivity, GalleryAdapter
│   └── send/       # SendActivity, SendViewModel, UiState
└── util/           # UsbPermissionHelper
```

**Decisiones clave:**

| Área | Decisión |
|------|----------|
| Testabilidad USB | `SerialPort` es una interfaz — `UsbSerialPort` para producción, `FakeSerialPort` para tests |
| Estado de la UI | `UiState` sellado: `Idle / Connecting / Ready / Sending / Done / Error` |
| Datos | `DrawingRepository` hardcodeado — sin base de datos (alcance académico) |
| Envío | `GcodeSender` usa corrutinas + `withTimeout` para esperar el ACK por cada línea |

---

## Tests

57 tests unitarios, sin necesidad de dispositivo físico.

```bash
./gradlew test
```

| Capa | Tests |
|------|-------|
| Data | `DrawingRepositoryTest`, `DrawingItemTest`, `AssetGcodeLoaderTest` |
| G-code | `GcodeParserTest`, `GcodeSenderTest` |
| Serial | `FakeSerialPortTest` |
| UI | `GalleryActivityTest`, `GalleryAdapterTest`, `SendActivityTest`, `SendViewModelTest` |
| Util | `UsbPermissionHelperTest` |

---

## Dibujos disponibles

| # | Nombre |
|---|--------|
| 1 | Espiral |
| 2 | Estrella |
| 3 | Círculo |
| 4 | Cuadrado |
| 5 | Triángulo |
| 6 | Hexágono |
| 7 | Rombo |
| 8 | Flor |

---

## Protocolo Arduino

El firmware debe responder por serial (9600 baud) con:

- `ok` — línea recibida y ejecutada correctamente
- `error` — fallo al procesar la línea

La app envía la siguiente línea solo después de recibir el ACK. Si no hay respuesta en el tiempo límite, emite `UiState.Error`.
