# Verificación

Comando: `./gradlew assembleDebug testDebugUnitTest lintDebug`.

Pruebas de dominio: préstamo/devolución, identidad, préstamos duplicados, revisión, fechas imposibles, fecha límite de devolución y ventana de mantenimiento. CI ejecuta compilación, pruebas y Android Lint.

## Resultados de la primera entrega · 13/10/2026

- 12 pruebas locales de reglas y formato de copias: correctas.
- Compilación del APK debug y Android Lint: correctos. Quedan avisos informativos de versiones más nuevas de dependencias; no errores de Lint.
- 3 pruebas instrumentadas en emulador Android 15: correctas. Cubren ejemplos opcionales, alta/préstamo/devolución/persistencia al recrear la actividad y rollback de SQLite ante una restauración fallida.
- Capturas reales: `docs/screenshots/`. Evidencia y resultados disponibles en el workflow Android device tests.
- El APK no solicita permiso INTERNET, cámara, contactos ni ubicación.

El emulador local no arrancó correctamente sin aceleración; las pruebas de dispositivo se ejecutaron en el runner Linux estándar de GitHub. Sigue pendiente la validación en un teléfono físico y en la versión mínima Android 8.0. Las comprobaciones manuales de abajo no se dan por realizadas solo por pasar CI.

## Recorrido manual antes del piloto

1. Instalación limpia: inventario vacío, sin ejemplos automáticos.
2. Explorar ejemplos: cuatro DEMO, devolución vencida y mantenimiento próximo.
3. Crear, cerrar/abrir, editar y comprobar persistencia e historial.
4. Prestar: rechazar responsable vacío/fecha pasada; aceptar fecha válida.
5. Devolver; comprobar disponibilidad e historial.
6. Iniciar/finalizar revisión; impedir préstamo durante revisión.
7. Buscar por nombre, serie, ubicación y responsable; combinar filtros.
8. Exportar, añadir otro equipo, restaurar y comprobar reemplazo confirmado.
9. Restaurar JSON malformado/duplicado: conservar datos previos.
10. Eliminar DEMO conservando equipos propios.
11. Modo oscuro, TalkBack, fuente grande, teclado y rotación.

Compilar no equivale a validar en hardware real. Documentar limitaciones antes de declarar lista para producción.

Ejecuciones verificadas para el código de la versión (`202a8f9`): [compilación, pruebas unitarias y Lint](https://github.com/jesusvalverde31/relevo/actions/runs/38408864603) y [pruebas en Android 15](https://github.com/jesusvalverde31/relevo/actions/runs/38408864591). Los artefactos temporales de CI se conservan un día; las capturas seleccionadas quedan versionadas en el repositorio.
