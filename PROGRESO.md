# PROGRESO — GestionMantenimiento

Documento de continuidad actualizado para la rama `despliegue` del repositorio original.

## Estado actual

- Trabajo iniciado el 2026-11-21 siguiendo las decisiones aprobadas en el chat.
- Módulo 0 (entorno y configuración segura): completado en archivos; compilación/arranque pendientes por herramientas ausentes.
- `.env` está presente localmente con permisos `600`, ignorado por Git; no se registran sus valores.
- `.env.example` contiene únicamente nombres de variables.
- Node.js 24.22.3 y npm 11.19.1 están disponibles; Node es compatible con Vite 8.
- Java 21 y Maven no estaban instalados como herramientas de sistema. Para validar se descargaron JDK 21.0.13.1 y Maven 3.9.11 temporalmente en `/tmp` (sin `sudo`); la carpeta `apache-maven-3.10.0-bin` se conserva.
- `npm ci` y `npm run build` terminaron correctamente en `frontend-app`.
- `mvn test` compiló el backend y pasó los 30 tests unitarios; las dos integraciones Docker quedan excluidas.
- El frontend compiló con `npm run build`.
- El backend real y las pruebas contra Aiven/Brevo todavía no se han ejecutado.

## Decisiones vigentes

- Mantener la arquitectura hexagonal y el límite de cinco casos activos por técnico.
- No implementar `DELETE`, no crear migración `V4` y conservar los nombres históricos de estados.
- No instalar Docker ni hacer `git push`.
- Omitir de `mvn test` las dos suites de integración basadas en Docker; la validación real contra Aiven debe usar únicamente datos con prefijo `PRUEBA-`.
- No se ha definido `CORREO_PRUEBA`; las pruebas que mandan correo quedan documentadas pero no se ejecutan.
- No modificar ni borrar datos reales de Aiven. Las sentencias de limpieza se entregarán para ejecución manual.

## Módulos

| Módulo | Estado |
| --- | --- |
| 0. Entorno, `.env.example` y arranque Linux | ✅ Archivos listos; validación estática hecha |
| 1. Backend: validación, excepciones, correo y CORS | ✅ Código implementado; compilación pendiente |
| 2. Técnicos y equipos: CRUD permitido | ✅ Código + pruebas Mockito; compilación pendiente |
| 3. Solicitudes, solicitantes y asignación | ✅ Código + pruebas Mockito; compilación pendiente |
| 4. Revisiones preventivas y motor automático | ✅ Código + pruebas Mockito; compilación pendiente |
| 5. Frontend conectado a consulta por ID y edición | ✅ Código implementado y `npm run build` correcto |
| 6. Pruebas Mockito y `pruebas.http` | ✅ Compila; 30 pruebas ejecutadas, 30 pasaron; integraciones Docker omitidas |
| 7. Verificación real Aiven/Brevo y manual final | 🟡 Manual y limpieza SQL listos; prueba real pendiente por JDK/Maven y dirección `CORREO_PRUEBA` |

## Bloqueos de validación conocidos

- La ejecución del backend y las pruebas reales usan Aiven/Brevo y siguen pendientes.
- Para pruebas de correo real falta definir `CORREO_PRUEBA` con la dirección controlada del equipo.

## Últimas acciones

- Se verificó que los nombres de variables de Aiven y Brevo coinciden con `application.properties`.
- Se creó `.env.example` sin valores reales y se confirmó que `.env` está excluido de Git.
- Se creó `iniciar-backend.sh`; todavía no se ejecutó porque el entorno carece de JDK compilador/Maven.
- `MANUAL_PRUEBAS.md` documenta arranque, APIs, flujo de navegador y limpieza SQL manual.
- `pruebas.http` cubre consultas, creación, edición y respuestas de error; deja señaladas las peticiones que envían correo real.
- Se verificó `git diff --check`, `bash -n iniciar-backend.sh` y que `.env` tenga permisos `600` y esté ignorado.
- Para que Maven resolviera dependencias se usó temporalmente un espejo de `repo1.maven.org` y el certificado de red del entorno; esos ajustes quedaron en `/tmp`, fuera del repo.
- Hay ocho commits locales en `despliegue`; los cambios de corrección de pruebas también se guardarán localmente. No hacer `push`.
