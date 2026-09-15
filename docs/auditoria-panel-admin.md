# Auditoría del panel de Administrador — Sistema Académico Escuelita Maker

**Alcance de este documento:** auditoría y diseño en papel, sin cambios de código. Referencia contra flujos estándar de SIS/LMS (PowerSchool, Google Classroom Admin console, Moodle, Canvas, Additio, ClassDojo, Alegra Escolar, SchoolCues, Ecole24, Edmodo) contrastados contra el negocio real: Instituto Chihuahuense de Excelencia Académica, un solo administrador, dos líneas de negocio (cursos sueltos y secundaria/prepa abierta con esquema propio, no INEA/SEP), stack fijo (Spring Boot + MySQL, frontend plano sin build).

Todo lo descrito del sistema actual está verificado leyendo el código en este worktree (`src/main/java/...`, `landing/*.html`) a la fecha de este documento — no se infiere ni se asume comportamiento no confirmado en el código.

---

## 0. Inventario técnico verificado (base para todo lo demás)

### Modelo de datos actual

| Entidad | Campos relevantes | Notas verificadas en código |
|---|---|---|
| `Usuario` | `rol` (ALUMNO/MAESTRO/ADMINISTRADOR), `estado` (LISTA_ESPERA/ACTIVO/RECHAZADO, default LISTA_ESPERA), `password` (BCrypt, write-only) | Un solo `ADMINISTRADOR` esperado (`obtenerAdministrador()` toma el primero que encuentre) |
| `Curso` | `tipo` (CURSO_SUELTO/SECUNDARIA/PREPA_ABIERTA), `maestro` (ManyToOne, **nullable**), `activo` (boolean, default `true`) | El campo `activo` **existe en la entidad pero ningún endpoint lo expone ni lo modifica** — es un campo muerto hoy; no hay forma de archivar un curso, solo de borrarlo (`DELETE`) |
| `Inscripcion` | `estado` (ACTIVA/FINALIZADA/CANCELADA, default ACTIVA) | **No existe ningún endpoint que transicione una inscripción a FINALIZADA o CANCELADA.** El único `DELETE /api/inscripciones/{id}` hace `deleteById` — borrado físico, no cambio de estado. En la práctica el enum `FINALIZADA`/`CANCELADA` no se usa nunca hoy. |
| `Tema` | `curso`, `titulo`, `orden` | Sin fecha límite, sin peso/ponderación |
| `AvanceTema` | `alumno` + `tema` (única), `fechaCompletado` | La *existencia* del registro es el estado "completado"; no hay estados intermedios (en progreso, etc.). Se alterna con `PATCH /api/temas/{id}/completar?alumnoId=` (toggle) |
| `Actividad` | `tipo` (TAREA/EXAMEN), `curso` | Sin fecha de entrega/vencimiento, sin puntaje máximo |
| `Entrega` | `alumno` + `actividad` (única), `calificacion` (`Double`, **sin validación de rango**), `rutaArchivo` | Re-entregar sobrescribe la fila (mismo id) pero **no borra el archivo anterior en disco** — huérfanos acumulándose en `uploads/entregas/` sin límite |

### Endpoints existentes (todos `@CrossOrigin(origins = "*")`, sin autenticación de servidor)

- `Usuario`: `GET /api/usuarios`, `GET /{id}`, `GET /lista-espera`, `GET /administrador`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PATCH /{id}/activar`, `PATCH /{id}/rechazar`, `PATCH /{id}/telefono`
- `Curso`: `GET /api/cursos`, `GET /{id}`, `GET /maestro/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`
- `Inscripcion`: `GET /api/inscripciones`, `GET /alumno/{id}`, `GET /curso/{id}`, `POST`, `DELETE /{id}`
- `Tema`: `GET /curso/{id}`, `POST`, `DELETE /{id}`, `GET /avance?alumnoId&cursoId`, `PATCH /{id}/completar?alumnoId`
- `Actividad`: `POST`, `GET /curso/{id}`, `DELETE /{id}`, `GET /pendientes?alumnoId&cursoId`, `GET /pendientes/alumno/{id}`, `GET /estado?alumnoId&cursoId`, `GET /{id}/entregas`, `POST /{id}/entregar` (multipart)
- `Entrega`: `GET /{id}/archivo` (descarga), `PATCH /{id}/calificar?calificacion=`
- `Registro`: `POST /api/registro` (alta pública, siempre `ALUMNO` + `LISTA_ESPERA`, sin importar lo que mande el cliente)
- `Auth`: login (verificado que valida `estado` — bloquea login si `LISTA_ESPERA` o `RECHAZADO`)

### Seguridad real hoy

`SeguridadConfig` solo define un `BCryptPasswordEncoder`. **No hay filtro de seguridad, no hay sesión de servidor, no hay JWT, no hay `@PreAuthorize` en ningún controller.** El control de acceso es 100% de cliente: `dashboard-administrador.html` lee `localStorage.usuario.rol` y redirige si no es `ADMINISTRADOR`. Cualquiera que conozca la URL base de la API puede invocar `PATCH /api/usuarios/{id}/activar`, `DELETE /api/cursos/{id}`, etc. sin pasar por el frontend. Esto aplica a **todo** lo que se proponga en este documento: cualquier acción administrativa nueva hereda este mismo hueco hasta que se resuelva a nivel backend.

### El flujo de WhatsApp (contexto real)

En `catalogo.html`, cuando un alumno activo se auto-inscribe a un curso suelto (`inscribirme()`), la inscripción se crea **ACTIVA de inmediato** vía `POST /api/inscripciones`, y **después** se abre `wa.me/<telefono-admin>` con un mensaje prellenado. El mensaje de WhatsApp es solo una notificación de cortesía para que el admin sepa que debe cobrar/dar seguimiento — **no es una puerta de aprobación** y no queda registrado en el sistema (si el alumno cierra la pestaña de WhatsApp sin enviar, la inscripción ya existe igual). Esto confirma el matiz #1 del brief: hoy no hay aprobación de inscripción a curso suelto, solo aprobación de cuenta (lista de espera).

---

## 1. Mapa de flujos administrativos estándar vs. este sistema

Por función, comparando contra lo que ofrecen PowerSchool, Google Classroom (consola admin), Moodle ("Site administration"), Canvas, Additio, ClassDojo, Alegra Escolar, SchoolCues y Edmodo en instituciones chicas/medianas:

### 1.1 Aprobación y gestión de usuarios

| Función estándar en la industria | ¿Existe aquí? | Aplica a este negocio |
|---|---|---|
| Cola de aprobación de altas nuevas (Moodle: "pending approval"; SchoolCues: solicitudes de inscripción) | ✅ Sí — tab "Lista de espera" | Sí, ya cubierto |
| Alta manual de usuario por el admin sin pasar por registro público (PowerSchool: "add student/staff") | ⚠️ Endpoint existe (`POST /api/usuarios`) pero **no hay UI** en el panel admin para crearlo | Sí aplica — la dueña puede necesitar dar de alta a un alumno que se inscribió en persona/efectivo sin pasar por la landing |
| Edición de datos de un usuario (teléfono, nombre, email) | ⚠️ Endpoint `PUT` y `PATCH /telefono` existen, sin UI en el panel admin (salvo lo que ya hace el propio alumno/maestro en sus dashboards, no confirmado) | Sí aplica |
| Baja/suspensión de cuenta ya activa (no solo rechazo en lista de espera) | ❌ No existe ningún flujo para pasar de `ACTIVO` a `RECHAZADO`/inactivo una vez aprobado, solo `DELETE` físico | Sí aplica — un alumno que deja la academia a mitad de curso hoy solo puede borrarse (perdiendo historial) |
| Roles/permisos granulares entre administradores | Estándar en PowerSchool/Canvas (multi-admin con permisos por área) | **No aplica** — un solo administrador, dueña única; construir esto sería sobre-ingeniería |
| Restablecimiento de contraseña asistido por admin | Estándar en casi todas las plataformas | ❌ No existe ningún endpoint para que el admin resetee una contraseña olvidada | Sí aplica, es soporte operativo básico |

### 1.2 Gestión de catálogo de cursos

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Alta/edición/baja de curso | ✅ Alta y baja sí; edición (`PUT`) existe en backend pero **no hay botón de editar curso en el panel** (solo crear/eliminar) | Sí |
| Archivar curso sin eliminarlo (Moodle: "hide course"; Classroom: "archive") | ❌ Campo `activo` existe en el modelo pero está muerto (ningún endpoint lo toca) | Sí aplica — hoy la única forma de "cerrar" un curso es borrarlo, lo que además puede fallar por integridad referencial si tiene inscripciones/temas/actividades |
| Cupo máximo / lista de espera por curso (PowerSchool, SchoolCues) | ❌ No existe campo de cupo en `Curso` | **Pregunta abierta** — depende de si los cursos son 1:1 (un maestro, un grupo) o si hay capacidad física/de atención limitada. No inventar sin confirmar con la dueña |
| Prerrequisitos entre cursos (Canvas: "course requirements") | ❌ No existe | Probablemente **no aplica** a cursos sueltos, pero sí podría aplicar a Secundaria/Prepa (¿segundo grado requiere haber completado primero?) — **pregunta abierta**, no asumir |
| Duplicar curso como plantilla (Classroom: "copy") | ❌ No existe | Bajo valor con el volumen actual (pocos cursos); no priorizar |
| Cursos sin instructor asignado — visibilidad/publicación | El modelo permite `maestro = null`; el catálogo público (`catalogo.html`) no filtra por esto, así que un curso sin maestro **es inscribible igual** hoy | Sí aplica — es un caso real y actualmente silencioso: un alumno puede pagar/inscribirse a un curso que nadie va a dar clase |

### 1.3 Asignación de maestros

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Asignar/reasignar maestro a un curso | ✅ Vía formulario de creación de curso y `PUT`; el panel admin actual solo lo hace al crear, no al editar uno existente | Sí, falta UI de reasignación |
| Ver carga de trabajo de un maestro (cuántos cursos/alumnos totales) | ✅ Ya cubierto en el rediseño reciente (detalle expandible de maestro con sus cursos y cuenta de alumnos) | Cubierto |
| Sustituir maestro sin perder historial de calificaciones/avance (cambio de maestro a mitad de curso) | Parcialmente — reasignar el `maestro` de un `Curso` no toca `Tema`/`Actividad`/`Entrega` (no dependen del maestro directamente), así que el historial no se pierde técnicamente, pero no hay ningún flujo/aviso explícito para esto | Aplica como caso borde a documentar, no a construir necesariamente |

### 1.4 Seguimiento académico (avance, calificaciones)

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Vista de avance por alumno y por curso (gradebook) | ✅ Existe a nivel maestro/alumno (`GET /api/temas/avance`, `GET /api/actividades/estado`) — **el admin no tiene ninguna vista de esto hoy** | Sí aplica fuertemente — es el hueco más grande del panel admin actual |
| Vista consolidada de un alumno con avance simultáneo en varias inscripciones/líneas de negocio | ❌ No existe ni a nivel admin ni maestro; cada consulta de avance es por `alumnoId + cursoId` puntual | Sí aplica — un alumno de Secundaria que también toma un curso suelto de inglés no tiene una vista unificada en ningún rol |
| Escala de calificación configurable/documentada | `calificacion` es `Double` libre, sin mínimo/máximo validado en ningún lado | **Bloqueante para cualquier reporte/exportación** — no se puede calcular promedio, aprobado/reprobado, ni imprimir una boleta sin saber si la escala es 0–10, 0–100 o letras. **Pregunta abierta obligatoria** |
| Alertas de alumnos en riesgo (bajo avance, entregas atrasadas) | Estándar en Canvas/PowerSchool | ❌ No existe | Aplica como mejora de valor alto a mediano plazo, no urgente |

### 1.5 Reportes y analítica

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Exportar lista de alumnos por curso (CSV/Excel) | Estándar en todas las plataformas listadas | ❌ No existe ningún mecanismo de exportación en el sistema completo (no solo el panel admin) | Sí aplica, muy solicitado en la práctica para academias chicas que llevan control en Excel en paralelo |
| Constancias/reportes de avance imprimibles (PDF) | Estándar (SchoolCues, Alegra Escolar) | ❌ No existe | Depende de si "Secundaria/Prepa abierta" necesita constancia interna — dado que **no es INEA/SEP**, no hay obligación legal, pero la dueña puede necesitarlo para dar seguimiento a los padres. **Pregunta abierta** |
| Dashboard de KPIs (matrícula, ingresos, ocupación) | Estándar (PowerSchool, SchoolCues) | ⚠️ KPIs básicos de conteo ya se agregaron en el rediseño reciente (alumnos activos, maestros, cursos, lista de espera) | Cubierto en lo básico; **no hay ni puede haber KPI de ingresos/pagos** porque el modelo no tiene concepto de pago/adeudo — no inventarlo |

### 1.6 Comunicación

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Mensajería interna / anuncios (Classroom, Edmodo, ClassDojo) | ❌ No existe | Para el tamaño de esta academia, WhatsApp externo probablemente sigue siendo más práctico que construir mensajería propia — **no aplica construir un chat interno**, pero sí vale la pena centralizar el *registro* de que un contacto ocurrió (ver 1.7) |
| Bandeja de "solicitudes de contacto" para el admin | No existe hoy ni como registro ni como bandeja | El brief pregunta explícitamente si esto debe entrar al panel admin. Con el modelo actual, la inscripción ACTIVA ya es el registro implícito de "quiero este curso" — lo que falta no es una bandeja nueva sino **visibilidad de inscripciones recientes sin seguimiento de pago/contacto**, que es distinto a construir una bandeja de mensajes. Ver brecha priorizada más abajo. |
| Notificaciones push/email al admin (nuevo en lista de espera, entrega sin calificar, curso sin maestro) | Estándar en todas las plataformas de la lista | ❌ No existe ningún mecanismo de notificación saliente (ni email ni push); el admin solo se entera si abre el panel | Sí aplica, pero con el stack actual (sin backend de correo/push configurado) esto es esfuerzo alto — ver brechas |

### 1.7 Calendario académico

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Calendario de fechas de entrega, exámenes, periodos | Estándar (Canvas, Classroom, Moodle) | ❌ `Actividad` no tiene fecha de vencimiento en el modelo | Aplica a mediano plazo — pero es una brecha de **maestro**, no de admin, primero. El admin solo lo necesitaría como vista consolidada después de que exista a nivel maestro |
| Periodos académicos (semestres, ciclos) | Estándar en PowerSchool | No existe: los cursos no tienen fecha de inicio/fin | **Pregunta abierta** — depende de si la academia opera por ciclos fijos o inscripción continua todo el año; no asumir |

### 1.8 Auditoría / bitácora

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Log de quién aprobó/rechazó/calificó/borró y cuándo | Estándar en PowerSchool/Canvas (compliance); ausente en herramientas más ligeras como ClassDojo | ❌ No existe ninguna tabla ni columna de auditoría en todo el sistema | Con un solo administrador, el valor de "quién aprobó a quién" es bajo (siempre es la misma persona). El valor real de una bitácora aquí sería **de recuperación ante errores** (ej. "¿cuándo rechacé sin querer a este alumno?", "¿cuándo se borró este curso?"), no de control multi-usuario. Prioridad baja/media, no alta |

### 1.9 Configuración del sistema

| Función estándar | ¿Existe aquí? | Aplica |
|---|---|---|
| Configuración de datos institucionales (nombre, logo, teléfono de contacto) | Estándar (todas las plataformas tienen "ajustes de la institución") | ❌ El teléfono de WhatsApp y el nombre de la academia están **hardcodeados en el HTML del frontend** (no confirmado desde dónde exactamente lee `catalogo.html` el `administradorTelefono` — parece venir de `GET /api/usuarios/administrador` y su campo `telefono`, es decir, sí es dinámico vía el teléfono del usuario admin, pero el nombre/marca de la academia sí está fijo en el HTML) | El teléfono ya es semi-configurable (vía `PATCH /{id}/telefono` sobre el propio admin, que además el panel admin actual no expone en UI). El nombre/marca fija en HTML no aplica cambiarlo — es una academia real con marca fija, no un producto multi-tenant |

---

## 2. Lista priorizada de brechas y mejoras

Cada ítem: problema concreto → matiz de negocio al que responde → esfuerzo estimado dado el stack actual (Spring Boot + MySQL + HTML/JS plano, sin build).

### Prioridad alta

1. **El admin no tiene ninguna vista de avance académico ni de calificaciones.**
   Responde a: el rol de Administrador hoy solo gestiona altas/bajas y catálogo, pero no puede responder "¿cómo va fulanito en Secundaria?" sin pedirle al maestro. Es la brecha de mayor valor visible para la dueña.
   Esfuerzo: **medio**. Los endpoints (`/api/temas/avance`, `/api/actividades/estado`) ya existen y están probados desde maestro/alumno; falta solo una pestaña o vista de detalle en el panel admin que los consuma por alumno/curso. No requiere cambios de backend si se acepta una consulta por curso a la vez (ver sección 4 para el caso de vista *agregada* de todo un curso, que sí necesitaría un endpoint nuevo).

2. **Curso sin maestro asignado es inscribible sin ninguna advertencia.**
   Responde a: caso borde explícito del brief — un alumno puede pagar por un curso "fantasma".
   Esfuerzo: **bajo**. Es un cambio de UI puramente visual en el panel admin (badge "sin maestro" ya parcialmente reflejado en el rediseño reciente) más, si se decide, un bloqueo en `catalogo.html` (fuera del alcance de este worker, pertenece al flujo de alumno) o una alerta proactiva en el panel admin ("2 cursos sin maestro asignado con alumnos inscritos").

3. **Baja de inscripción es un `DELETE` físico, no un cambio de estado.**
   Responde a: matiz explícito del brief sobre bajas/cancelaciones — hoy se pierde el registro de que alguna vez existió esa inscripción, aunque el avance de temas y las entregas calificadas **no** se borran (no dependen de `Inscripcion`, dependen de `alumno` + `curso`/`tema`/`actividad` directamente), quedando huérfanas sin ninguna inscripción activa que las explique.
   Esfuerzo: **bajo-medio**. Requiere un endpoint nuevo (`PATCH /api/inscripciones/{id}/cancelar` o similar) en vez de reusar `DELETE`, ver sección 4.

4. **Sin escala de calificación definida ni validada.**
   Responde a: bloquea cualquier reporte, exportación o indicador de aprovechamiento.
   Esfuerzo: **bajo** una vez que la dueña confirme la escala (agregar validación `@DecimalMin/@DecimalMax` en `Entrega.calificacion` y ajustar el input del maestro). El esfuerzo real está en la **decisión de negocio**, no en el código.

5. **No hay forma de editar un curso existente (reasignar maestro, cambiar nombre/descripción) desde el panel.**
   Responde a: operación diaria básica — hoy la única forma de "corregir" un curso es borrarlo y recrearlo, lo cual además rompe inscripciones/temas/actividades ya ligadas a ese curso.
   Esfuerzo: **bajo**. El backend ya soporta `PUT /api/cursos/{id}`; falta solo UI (botón "Editar" + reusar el mismo formulario del panel admin actual en modo edición).

6. **Falta de exportación de datos (listas, avance) en formato usable sin el sistema.**
   Responde a: dependencia operativa — una academia chica necesita poder compartir/imprimir listas incluso si el sistema está caído o para trámites con padres de familia.
   Esfuerzo: **medio**. CSV es trivial (construible 100% en el cliente con los datos que ya trae el JSON, sin backend nuevo: generar un blob CSV desde JS). PDF requeriría o una librería de cliente (which se debe evaluar contra la restricción de "sin build/frameworks" — hay librerías JS standalone tipo `jsPDF` que no requieren build, pero sí es una dependencia nueva a evaluar) o un endpoint de backend que genere PDF (esfuerzo alto, nueva librería Java). **Recomendación: empezar por CSV client-side, que no requiere ninguna dependencia nueva ni cambio de backend.**

### Prioridad media

7. **Sin alta manual de usuario ni edición de datos de usuario desde el panel admin.**
   Responde a: caso real de inscripción en efectivo/presencial sin pasar por el registro público de la landing.
   Esfuerzo: **bajo**. Backend ya soporta `POST`/`PUT /api/usuarios`; falta solo formulario en el panel (similar al que ya existe para cursos).

8. **Sin forma de suspender/reactivar una cuenta ya `ACTIVO`** (solo lista de espera tiene activar/rechazar).
   Responde a: un alumno que deja de asistir o un maestro que ya no colabora, sin querer borrar su historial.
   Esfuerzo: **bajo-medio**. Requiere decidir si se reutiliza `EstadoCuenta.RECHAZADO` para esto (semánticamente confuso, "rechazado" suena a nunca aprobado) o se agrega un estado nuevo tipo `SUSPENDIDO`/`INACTIVO` (cambio de enum + migración de datos, bajo impacto dado el volumen actual).

9. **Sin notificaciones proactivas para el admin** (nuevo en lista de espera, curso sin maestro, entregas sin calificar).
   Responde a: hoy el admin debe entrar a cada pestaña para enterarse de todo.
   Esfuerzo: **alto** si se busca email/push real (requiere proveedor de correo, configuración SMTP, nueva infraestructura). **Esfuerzo bajo** si se acepta una versión "banner al entrar al panel" que simplemente resuma con los datos que ya se cargan hoy (`X en lista de espera`, `Y cursos sin maestro con inscritos`) — recomendado como primer paso, sin backend nuevo.

10. **Sin vista consolidada de un alumno con todas sus inscripciones y avance simultáneo.**
    Responde a: matiz explícito del brief — alumno con curso suelto + secundaria a la vez.
    Esfuerzo: **medio**. El detalle expandible de alumno ya existente en el panel muestra sus inscripciones; agregar el % de avance por curso ahí requeriría llamar `GET /api/temas/avance` por cada inscripción activa del alumno (N llamadas, aceptable al volumen actual, pero se vuelve costoso si la matrícula crece — ver sección 4 para una alternativa de endpoint agregado).

11. **Manejo de archivos de entregas desde la perspectiva admin (verlos/descargarlos/purgarlos).**
    Responde a: matiz explícito del brief; además hay un hallazgo técnico nuevo: **al re-entregar, el archivo anterior no se borra de disco** (`ActividadService.entregar` sobrescribe la fila de `Entrega` pero no elimina `rutaArchivo` anterior), por lo que `uploads/entregas/` crece indefinidamente con archivos huérfanos sin que nada en el sistema lo sepa.
    Esfuerzo: **medio**. Ver un listado de entregas ya es posible con el endpoint existente por actividad (`GET /api/actividades/{id}/entregas`); no existe hoy un listado global de entregas para el admin. Purgar archivos huérfanos requeriría un job/endpoint nuevo — no trivial de hacer bien sin arriesgar borrar un archivo referenciado.

### Prioridad baja

12. **Sin cupo máximo ni prerrequisitos de curso.**
    Responde a: pregunta abierta de negocio, no confirmado que aplique — no construir sin decisión de la dueña.
    Esfuerzo si se confirma que aplica: **bajo** para cupo (un campo entero + validación en el `POST /inscripciones`), **medio** para prerrequisitos (requiere modelar relación curso-curso y validar en el flujo de inscripción).

13. **Sin archivar cursos (usar el campo `activo` ya existente pero muerto).**
    Responde a: alternativa menos destructiva que `DELETE` para "cerrar" un curso.
    Esfuerzo: **bajo** — el campo ya existe en el modelo, solo falta exponerlo (`PATCH /api/cursos/{id}/archivar` + filtro en listados públicos).

14. **Sin bitácora/auditoría de acciones administrativas.**
    Responde a: valor bajo con un solo admin, salvo como recuperación ante error propio.
    Esfuerzo: **medio-alto** (tabla nueva, instrumentar cada servicio). No prioritario dado el contexto de un solo administrador.

15. **Sin periodos académicos / ciclos.**
    Responde a: pregunta abierta de negocio no confirmada.
    Esfuerzo: **alto** si se requiere (afecta el modelo de `Curso` y probablemente de `Inscripcion`). No construir sin confirmar que la academia opera por ciclos y no por inscripción continua.

---

## 3. Propuesta de flujo/UX por pestaña (en prosa, sin HTML)

Nota: el panel admin ya fue rediseñado visualmente en el ciclo anterior (marca del Instituto, KPIs, buscador en Alumnos/Maestros, tab de Inscripciones básico). Lo que sigue son **evoluciones de flujo**, no un rediseño visual desde cero.

### 3.1 Lista de espera (existente, sin cambios de flujo mayores)

Se mantiene igual: tarjeta por persona pendiente con Activar/Rechazar. Única mejora de bajo esfuerzo: mostrar junto a cada tarjeta si la persona ya tiene alguna inscripción en estado ACTIVA registrada (poco probable dado que el login se bloquea en LISTA_ESPERA, pero technically un registro público solo crea el `Usuario`, no una `Inscripcion` — confirmar que no hay forma de inscribirse sin estar `ACTIVO` antes de descartar este caso).

Estado vacío: ya cubierto ("No hay nadie en lista de espera ahorita").

### 3.2 Cursos

Flujo actual: crear curso (formulario) + lista con botón eliminar. Se propone:

- Agregar botón **"Editar"** en cada tarjeta de curso que abra el mismo formulario de "Nuevo curso" pre-llenado (reutilizando `PUT /api/cursos/{id}`), en vez de solo poder crear/eliminar.
- Badge visible **"Sin maestro asignado"** con color de alerta (dorado, semántica de "pendiente") cuando `curso.maestro == null`, ya insinuado en el texto actual (`"Sin maestro asignado"` en el subtítulo de la tarjeta) pero sin tratamiento visual de alerta.
- Si se confirma que el campo `activo` debe usarse: reemplazar el botón "Eliminar" por dos acciones — **"Archivar"** (soft, reversible, oculta el curso del catálogo público sin borrar historial) y, solo si el curso no tiene ninguna inscripción/tema/actividad ligada, **"Eliminar"** (hard delete, sin riesgo de romper integridad referencial).
- Estado borde: si el admin intenta eliminar un curso con inscripciones activas, la UI debe mostrar un mensaje explícito en vez de solo un error genérico de red (hoy el `catch` de `pintarCursos()` en el frontend solo dice "No se pudo eliminar el curso.", sin explicar por qué — probablemente porque MySQL rechaza el `DELETE` por restricción de llave foránea desde `temas`/`inscripciones`/`actividades`).

### 3.3 Inscripciones (nueva, ya con una primera versión básica en el rediseño anterior)

Evolución propuesta sobre la base ya construida (inscribir manualmente + dar de baja):

- Cambiar "dar de baja" de `DELETE` físico a una transición de estado a `CANCELADA` (requiere el endpoint nuevo de la sección 4), preservando el registro histórico.
- Agregar filtro por curso y por estado (ACTIVA/FINALIZADA/CANCELADA) una vez que esos estados empiecen a usarse de verdad.
- Mostrar, junto a cada inscripción activa, el **% de avance** del alumno en ese curso (reusando `GET /api/temas/avance`), para que esta pestaña sirva también como vista rápida de seguimiento sin tener que entrar al detalle del alumno.

### 3.4 Alumnos

Ya tiene búsqueda y detalle expandible con cursos inscritos. Evolución propuesta:

- Dentro del detalle expandible, agregar el **% de avance de temas** y **actividades pendientes/calificadas** por cada curso inscrito (no solo el nombre del curso y su estado de inscripción como hoy), para resolver el gap de "vista consolidada" (matiz #28 del brief) sin necesitar una pestaña nueva.
- Acción **"Editar datos"** (teléfono, nombre) reusando `PUT`/`PATCH /telefono` ya existentes en el backend.
- Estado borde a decidir con la dueña: si se agrega suspensión de cuenta activa, dónde vive el botón (¿en esta pestaña, junto al badge de estado?).

### 3.5 Maestros

Ya tiene búsqueda y detalle expandible con cursos que imparte. Evolución de bajo esfuerzo:

- Botón **"Editar datos"** igual que en Alumnos.
- Posible indicador de **entregas sin calificar** por curso del maestro, visible solo como información (el admin no califica, pero le sirve para dar seguimiento/recordar al maestro) — requiere agregar un conteo de entregas con `calificacion == null` por curso, calculable en el cliente si se expone un endpoint de listado de entregas por curso (no existe hoy, ver sección 4).

### 3.6 Pestaña nueva propuesta: "Resumen" o mejora del banner de KPIs ya existente

En vez de una pestaña nueva de notificaciones, se propone extender la fila de KPIs ya construida (o agregar una franja de alertas justo debajo) con contadores accionables, ej.: *"2 cursos sin maestro con alumnos inscritos"*, *"5 entregas sin calificar"* — cada uno como link directo al filtro correspondiente en la pestaña relevante. Esto cubre la necesidad de notificación proactiva (matiz explícito del brief) sin requerir infraestructura de correo/push, usando solo datos que ya se cargan en `cargarTodo()`.

### 3.7 Reportes/exportación (no necesariamente pestaña propia)

Se propone **no** crear una pestaña "Reportes" separada por ahora, sino agregar un botón de exportación CSV contextual dentro de cada pestaña existente (ej. "Exportar alumnos" en la pestaña Alumnos, "Exportar inscritos" dentro del detalle de un curso), evitando duplicar UI de filtros que ya existen ahí.

---

## 4. Cambios de modelo de datos y endpoints necesarios

Todos consistentes con las entidades ya existentes; ninguno rompe endpoints documentados (solo se agregan, no se modifican firmas existentes salvo donde se indica).

### 4.1 Para bajas reales de inscripción (brecha #3, alta prioridad)

- **Nuevo endpoint**: `PATCH /api/inscripciones/{id}/cancelar` → transiciona `estado` a `CANCELADA` sin borrar la fila.
- Opcional: `PATCH /api/inscripciones/{id}/finalizar` → transiciona a `FINALIZADA` (útil si se decide marcar cursos ya cursados/aprobados). Requiere decidir con la dueña si "finalizar" es una acción manual del admin/maestro o algo automático al llegar al 100% de avance — **pregunta abierta**, no asumir.
- El `DELETE /api/inscripciones/{id}` existente se mantiene para el caso de error de captura (inscripción creada por equivocación), pero deja de ser el botón principal de "dar de baja" en el panel.

### 4.2 Para editar curso desde el panel (brecha #5, alta prioridad)

Ningún cambio de backend — `PUT /api/cursos/{id}` ya existe y ya es usado por otros flujos. Solo trabajo de frontend (fuera del alcance de este documento de auditoría).

### 4.3 Para archivar cursos (brecha #13, baja prioridad, pero bajo esfuerzo)

- **Nuevo endpoint**: `PATCH /api/cursos/{id}/archivar` y `PATCH /api/cursos/{id}/reactivar`, que alternan el campo `activo` ya existente en la entidad.
- Ajuste necesario en el endpoint de catálogo público (`GET /api/cursos`, consumido por `catalogo.html`) para filtrar `activo == true` — **esto sí es un cambio de comportamiento de un endpoint existente** y debe evaluarse con cuidado porque el panel admin necesita ver cursos archivados (todos) mientras que el catálogo público solo debe ver los activos. Recomendación: no cambiar `GET /api/cursos`, sino agregar un `GET /api/cursos/activos` nuevo para el catálogo público, dejando el existente intacto para el admin (evita romper cualquier otro consumidor no documentado de `GET /api/cursos`).

### 4.4 Para vista consolidada de avance de alumno (brecha #10, esfuerzo medio)

Opción sin cambios de backend: el panel admin llama `GET /api/temas/avance?alumnoId&cursoId` una vez por cada inscripción activa del alumno (N llamadas). Aceptable con la matrícula actual (decenas de alumnos, pocos cursos cada uno).

Opción con cambio de backend (recomendada solo si la matrícula crece lo suficiente para que N llamadas sea notorio): **nuevo endpoint** `GET /api/temas/avance/alumno/{alumnoId}` que devuelva el avance de *todos* los cursos activos del alumno en una sola llamada (análogo a como `listarPendientesAlumno` ya agrega across cursos en `ActividadService`). No rompe nada existente, es puramente aditivo.

### 4.5 Para listado global de entregas por admin (brecha #11, esfuerzo medio)

**Nuevo endpoint**: `GET /api/entregas/curso/{cursoId}` (o `GET /api/actividades/entregas/curso/{cursoId}`) que liste todas las entregas de todas las actividades de un curso, útil para que el admin vea de un vistazo qué está pendiente de calificar sin tener que entrar actividad por actividad. Requiere un método nuevo en `EntregaRepository` (`findByActividadCursoId`), consistente con el patrón ya usado (`findByAlumnoIdAndActividadCursoId` ya existe en el mismo repositorio).

### 4.6 Para escala de calificación (brecha #4, bloqueante de reportes)

Sin cambio de endpoint. Cambio de validación en `Entrega.calificacion` (`@DecimalMin`/`@DecimalMax` con los valores que la dueña confirme) + ajuste del input correspondiente en el dashboard de maestro (fuera del alcance de este panel admin, pero documentado aquí porque bloquea cualquier reporte del lado admin).

### 4.7 Para suspensión de cuenta activa (brecha #8, esfuerzo bajo-medio)

Requiere decisión de negocio antes de tocar el enum: ¿se reutiliza `RECHAZADO` (rápido, pero semánticamente engañoso — un alumno suspendido no fue "rechazado") o se agrega `EstadoCuenta.SUSPENDIDO`? Se recomienda la segunda opción por claridad, aunque implica una migración de enum de bajo riesgo dado que `spring.jpa.hibernate.ddl-auto=update` ya gestiona el esquema automáticamente y el enum se persiste como `STRING` (no como índice numérico, por lo que agregar un valor nuevo no reordena ni rompe datos existentes).

### 4.8 Para alta/edición de usuario desde el panel (brecha #7, esfuerzo bajo)

Sin cambios de backend — `POST`/`PUT /api/usuarios` ya existen. Solo trabajo de frontend.

---

## 5. Riesgos y preguntas abiertas para confirmar con la dueña antes de construir

### Seguridad (no minimizar)

- **Todo el backend está abierto sin autenticación de servidor.** Cualquier acción administrativa nueva (cancelar inscripciones, editar cursos, suspender cuentas) es tan insegura como las que ya existen hoy — no es una regresión nueva, pero tampoco debe presentarse como "resuelto" solo porque el frontend oculte el botón. Antes de exponer más superficie administrativa (o de dar la URL de la API a más gente/dispositivos), se recomienda evaluar al menos un guard mínimo (ej. Spring Security con un solo usuario admin y sesión, o un token compartido) — **decisión y prioridad a confirmar con la dueña**, no es parte de esta auditoría de UX resolverlo, pero se documenta como riesgo real y creciente conforme el panel admin gane más poder.

### Preguntas abiertas de negocio (no asumir, no inventar)

1. **Escala de calificación**: ¿0–10, 0–100, letras, o "aprobado/no aprobado"? Bloquea reportes, exportaciones y cualquier indicador de aprovechamiento.
2. **Aprobación de inscripción a curso suelto**: ¿debe requerir validación del admin antes de quedar ACTIVA (como ya pasa con las cuentas nuevas), o el modelo actual de "inscripción instantánea + WhatsApp de cortesía" es intencional y debe mantenerse? ¿La respuesta es distinta para Secundaria/Prepa abierta vs. cursos sueltos?
3. **Cupo máximo por curso**: ¿existen límites de capacidad hoy (por atención del maestro, espacio físico, etc.) o la inscripción es siempre ilimitada?
4. **Prerrequisitos entre cursos**: ¿aplica a Secundaria/Prepa abierta (ej. no avanzar de grado sin terminar el anterior) aunque no sea un requisito legal externo?
5. **Periodos académicos**: ¿la academia opera por ciclos/semestres fijos o es inscripción continua todo el año? Afecta si "finalizar" una inscripción debe ser manual o basada en fechas.
6. **Constancias/reportes imprimibles**: ¿la dueña los necesita para dar seguimiento con padres de familia aunque no haya obligación legal (no es INEA/SEP)?
7. **Suspensión de cuenta activa**: ¿se necesita un estado nuevo (`SUSPENDIDO`) o basta con poder "desactivar" temporalmente sin perder el historial de otra forma?
8. **Bandeja de solicitudes de contacto**: ¿el flujo actual de WhatsApp (notificación de cortesía, sin registro en el sistema) es suficiente, o la dueña pierde solicitudes porque no siempre recibe/lee el WhatsApp? Si es lo segundo, el problema no se resuelve con una bandeja de mensajería nueva, sino con más visibilidad de las inscripciones recientes sin seguimiento — matiz a confirmar antes de proponer una solución concreta.
9. **Purga de archivos de entregas**: ¿hay una política de cuánto tiempo conservar entregas viejas, o se conservan indefinidamente? Relevante porque hoy ya se acumulan huérfanos sin que nada lo controle (hallazgo técnico, no solo de producto).

### Riesgos técnicos a tener presentes (no bloquean el diseño, pero deben acompañar cualquier construcción futura)

- Borrar un curso con inscripciones/temas/actividades ligadas probablemente falla hoy por restricción de llave foránea en MySQL (no se verificó en runtime en este ciclo por falta de credenciales de base de datos local, pero se infiere directamente de la ausencia de `cascade` en las relaciones `@ManyToOne` revisadas) — cualquier UI de "archivar en vez de eliminar" debe diseñarse pensando en este límite real, no como algo opcional.
- Los archivos de `uploads/entregas/` no están versionados ni respaldados (confirmado en el brief) y, además, **no se limpian al re-entregar** (hallazgo nuevo de este ciclo) — cualquier funcionalidad de "ver/purgar" que se construya debe tratar esto como deuda existente, no como algo introducido por el nuevo flujo.

---

## Resumen ejecutivo

El panel de administrador cubre bien las operaciones básicas de alta (lista de espera, cursos) pero **no tiene ninguna visibilidad de lo académico** (avance, calificaciones) ni de la salud operativa del catálogo (cursos sin maestro, inscripciones sin seguimiento). Las brechas de mayor valor con menor esfuerzo son: vista de avance por alumno/curso, edición de cursos existentes, y transición real de estado en bajas de inscripción (en vez de borrado físico) — ninguna requiere infraestructura nueva, solo endpoints aditivos y UI sobre datos que el backend ya expone. Las decisiones más importantes que bloquean construir con confianza son de negocio, no técnicas: escala de calificación, si debe haber aprobación de inscripción a curso, y si aplican cupos/prerrequisitos — ninguna se debe asumir.
