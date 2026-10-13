# Verificación

Comando: `./gradlew assembleDebug testDebugUnitTest lintDebug`.

Pruebas de dominio: préstamo/devolución, identidad, préstamos duplicados, revisión, fechas imposibles, fecha límite de devolución y ventana de mantenimiento. CI ejecuta compilación, pruebas y Android Lint.

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
