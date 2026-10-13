# Decisiones técnicas

Android nativo, API 26+, interfaz española. Piloto con un responsable de inventario en un único móvil, sin cuenta ni costes operativos de servidor.

- `domain`: modelos y reglas puras. Un equipo prestado no se puede volver a prestar ni enviar a revisión. Devolver elimina el responsable y fecha actuales; conserva historial.
- `data`: SQLiteOpenHelper versión 1, consultas parametrizadas y transacciones. SQLite directo por el tamaño de esta entrega; reevaluar Room al crecer consultas y migraciones.
- `ui`: Compose + ViewModel, flujos observables con lifecycle, disco en Dispatchers.IO y acciones de escritura serializadas desde la UI.
- `BackupCodec`: JSON versionado, validación previa a transacción y límite de importación de 10 MB.

Sin migración destructiva automática. Cualquier cambio de esquema requerirá migración probada. El registro no constituye un historial inalterable ni una prueba de entrega firmada.

Sin permiso INTERNET ni analítica. Copias automáticas del sistema desactivadas. El usuario elige el destino de su copia mediante el selector del sistema; puede ser un proveedor cloud instalado por el usuario. JSON exportado sin cifrar.

Fotos privadas, QR, notificaciones y exportación comercial precederán a sincronización, usuarios y pagos. La nube se decidirá tras validar demanda y presupuesto. No se promete IA local en todos los dispositivos.

No hay claves de firma de producción ni secretos en el repositorio. El applicationId es provisional hasta el primer lanzamiento público.
