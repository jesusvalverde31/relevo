<p align="center"><img src="docs/brand.svg" alt="Relevo — Menos buscar. Más hacer." width="900"></p>

# Relevo · Android

**Control de herramientas y equipos para pequeños negocios.** Registra dónde están, quién los tiene y cuándo deben volver. Proyecto personal de [JesusValverDEV](https://github.com/jesusvalverde31).

[![Android checks](https://github.com/jesusvalverde31/relevo/actions/workflows/android.yml/badge.svg)](https://github.com/jesusvalverde31/relevo/actions/workflows/android.yml)

## Estado

Primera versión de prueba **0.1.0**. No es todavía un producto comercial ni está publicada en Google Play. El alcance inicial es un inventario gestionado desde un solo dispositivo. No hay sincronización entre empleados, pagos ni cuentas.

## Funciones

- Registrar y editar equipos, ubicación, categoría, número de serie, notas y fecha de mantenimiento.
- Prestar equipos con responsable y fecha; registrar su devolución.
- Marcar y finalizar revisiones; impedir préstamos incompatibles con el estado del equipo.
- Consultar devoluciones vencidas y mantenimientos de los próximos siete días dentro de la app.
- Buscar y filtrar el inventario; consultar un historial persistente.
- Exportar y restaurar copias JSON mediante el selector de archivos de Android.
- Explorar ejemplos opcionales, claramente marcados como DEMO.
- Usar modo claro u oscuro siguiendo el sistema.

Los avisos son indicadores dentro de la app: todavía no hay notificaciones en segundo plano. Las fechas de mantenimiento se actualizan manualmente en la ficha.

## Sin servicios de pago

Funciona sin conexión, sin claves API, sin publicidad y sin servidor. No solicita permiso de Internet. Datos en almacenamiento privado de la app; copias exportadas sin cifrar. Desinstalar elimina los datos locales.

## Compilar

JDK 17 y Android SDK con `platforms;android-36` y `build-tools;36.0.0`. Configura `ANDROID_HOME` o `sdk.dir` en un `local.properties` no versionado.

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

En Windows: `gradlew.bat assembleDebug testDebugUnitTest lintDebug`.

APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`. Los workflows correctos también lo conservan como artefacto. Android mínimo: 8.0 (API 26). El APK debug no sustituye a una versión firmada para publicación.

## Arquitectura

Kotlin · Jetpack Compose · Material 3 · ViewModel/StateFlow · SQLite local.

```text
Compose UI → ViewModel → reglas de dominio + repositorio SQLite
                  └── copias JSON mediante Storage Access Framework
```

Reglas separadas de Android, escritura atómica de ficha + historial, almacenamiento en IO y validación de copias antes de reemplazar datos.

- [Decisiones técnicas](docs/ARCHITECTURE.md)
- [Estrategia comercial](docs/PRODUCT.md)
- [Hoja de ruta](docs/ROADMAP.md)
- [Privacidad](docs/PRIVACY.md)
- [Pruebas](docs/TESTING.md)
- [Borrador de LinkedIn](docs/LINKEDIN.md)

## Autoría

Diseño e implementación propios, desarrollados con asistencia de IA. Inspiración conceptual: [GitHub Trending](https://github.com/trending?since=monthly), [WeKnora](https://github.com/Tencent/WeKnora) y [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery). No se ha incorporado código ni servicios de estos dos proyectos.

El código se publica para mostrar su desarrollo. No se concede por ahora una licencia de software libre para el código propio. Las dependencias mantienen sus licencias: [avisos de terceros](THIRD_PARTY_NOTICES.md).
