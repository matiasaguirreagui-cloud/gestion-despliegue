# PROGRESO — GestionMantenimiento

Documento de continuidad actualizado para la rama `despliegue` del repositorio original.

## Estado actual

- Trabajo iniciado el 2026-11-21 siguiendo las decisiones aprobadas en el chat.
- Módulo 0 (entorno y configuración segura): implementación parcial.
- `.env` está presente localmente con permisos `600`, ignorado por Git; no se registran sus valores.
- `.env.example` contiene únicamente nombres de variables.
- Node.js 24.22.3 y npm 11.19.1 están disponibles; Node es compatible con Vite 8.
- Java 21 está disponible como runtime, pero `javac` no aparece en el entorno actual.
- Maven no está en `PATH`, ni se encontró el Maven de IntelliJ en las rutas conocidas. La carpeta `apache-maven-3.10.0-bin` se conserva.
- Todavía no se ha ejecutado Maven, npm, el backend ni pruebas contra Aiven/Brevo.

## Decisiones vigentes

- Mantener la arquitectura hexagonal y el límite de cinco casos activos por técnico.
- No implementar `DELETE`, no crear migración `V4` y conservar los nombres históricos de estados.
- No instalar Docker ni hacer `git push`.
- Omitir de `mvn test` las dos suites de integración basadas en Docker; la validación real contra Aiven debe usar únicamente datos con prefijo `PRUEBA-`.
- Nunca enviar correos reales durante pruebas a un destinatario distinto del correo de prueba indicado por el usuario. El marcador actual `[PONER_AQUI_CORREO_DE_PRUEBA]` aún no es un destinatario utilizable.
- No modificar ni borrar datos reales de Aiven. Las sentencias de limpieza se entregarán para ejecución manual.

## Módulos

| Módulo | Estado |
| --- | --- |
| 0. Entorno, `.env.example` y arranque Linux | 🟡 En progreso |
| 1. Backend: validación, excepciones, correo y CORS | ⬜ Pendiente |
| 2. Técnicos y equipos: CRUD permitido | ⬜ Pendiente |
| 3. Solicitudes, solicitantes y asignación | ⬜ Pendiente |
| 4. Revisiones preventivas y motor automático | ⬜ Pendiente |
| 5. Frontend conectado a consulta por ID y edición | ⬜ Pendiente |
| 6. Pruebas unitarias Mockito y `pruebas.http` | ⬜ Pendiente |
| 7. Verificación real Aiven/Brevo y manual final | ⬜ Pendiente |

## Bloqueos de validación conocidos

- Para compilar/probar en este entorno falta un JDK completo (`javac`) y Maven ejecutable. No se solicitará `sudo` ni se instalará Docker.
- Para los envíos de correo reales falta reemplazar `[PONER_AQUI_CORREO_DE_PRUEBA]` por la dirección controlada aprobada.

## Últimas acciones

- Se verificó que los nombres de variables de Aiven y Brevo coinciden con `application.properties`.
- Se creó `.env.example` sin valores reales y se confirmó que `.env` está excluido de Git.
- Se creó `iniciar-backend.sh`; todavía no se ejecutó porque el entorno carece de JDK compilador/Maven.
- No hay commits nuevos aún. Crear un commit local por cada módulo terminado y no hacer `push`.
