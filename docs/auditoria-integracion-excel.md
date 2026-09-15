# Auditoría de integración — flujo real de la academia (Excel) vs. sistema actual

**Alcance de este documento:** análisis/diseño en papel, sin cambios de código. No se tocó `landing/` ni `src/main/java`.

**Método de verificación:** los 2 archivos `.xlsx` se copiaron (sin moverlos de su ubicación original) a `.zip`, se extrajeron con `Expand-Archive` de PowerShell, y se parsearon `xl/worksheets/sheetN.xml` + `xl/sharedStrings.xml` con un script Node.js ad-hoc (regex sobre el XML de OOXML, resolviendo `t="s"` contra `sharedStrings.xml`, incluyendo fórmulas cacheadas `<f>...</f><v>...</v>`). Se verificó cada hoja, cada fila con datos reales y las fórmulas que generan las columnas calculadas — no se confía únicamente en el resumen que se me dio en el brief. Todo lo que sigue está anclado a celdas y filas concretas que se citan cuando aporta contexto.

El modelo de datos y endpoints "actuales" citados aquí están verificados leyendo `src/main/java/com/escuelita/sistemaacademico/{model,controller,service}` en este worktree a la fecha de este documento, y coinciden con lo ya documentado en `docs/auditoria-panel-admin.md` (auditoría previa, PR #4) — no se reinventan ahí donde ese documento ya es correcto.

---

## 0. Los dos archivos, tal como existen (verificación directa)

### 0.1 `Anexo_X_Seguimiento_académico_PREPA.xlsx` — 2 hojas

**Hoja "PREPA"** — título en B1: *"ANEXO X. TABLA DE SEGUIMIENTO ACADÉMICO – PREPARATORIA ABIERTA ICEA (5 MESES)"*. Encabezados (fila 2): `Mes / Semana | No. Mód | Bloque | Asesora | Nombre del Módulo | Semana de estudio | Dom. Aplic. | Fecha agenda 1.ª aplicación | ¿Acreditó? | [K: fórmula] | Observaciones / Reaplicación`.

Confirmado exactamente como se resumió, con matices verificados directamente:

- **22 filas de módulo** (filas 4–25), organizadas en `Mes 1`…`Mes 5`, cada una con un `Bloque` I–IV.
- **`Dom. Aplic.` (Domingo de aplicación) se repite** entre módulos de la misma semana/bloque de semanas (`Dom 1` aparece en 2 módulos, `Dom 4` en 1, `Dom 9` en 3, etc.) — confirma que es una fecha de examen **compartida por cohorte/semana, no individual por alumno**.
- **`Fecha agenda 1.ª aplicación` (columna I) está vacía en las 22 filas** de esta plantilla — es un campo pensado para llenarse por instancia/ciclo real, distinto del `Dom. Aplic.` fijo del catálogo.
- **`¿Acreditó?` (columna J) solo tiene datos en 2 de las 22 filas** (fila 4 = "Sí", fila 5 = "No"), el resto vacío. Esto es una plantilla/catálogo con un par de filas de ejemplo para mostrar cómo funciona la fórmula, **no un registro real de avance de muchos alumnos** — la hoja no tiene columna de nombre de alumno, así que estructuralmente es un catálogo + calendario de cohorte, no un gradebook por alumno (ese rol lo cumple la hoja "Control Académico" del segundo archivo).
- **Columna K es una fórmula de array** confirmada: `=IF(J:J="Sí","OK",IF(J="No","Reagendar"," "))` — confirma exactamente la columna calculada OK/Reagendar descrita en el brief.
- **Columna L ("Observaciones / Reaplicación") no es solo texto libre de reagenda**: en 8 de las 22 filas contiene una etiqueta de área temática — `"Matemáticas – solo"` (×2), `"Matemáticas"` (×2), `"Idiomas I"`, `"Idiomas II"`, `"Ciencias"` (×3). **Esto no estaba en el resumen que se me dio.** Parece marcar módulos que pertenecen a un área/eje temático específico (posiblemente relevante para agrupar exámenes o para los "diversificados"), pero no hay evidencia suficiente en el archivo para saber su función operativa exacta — **pregunta abierta** (ver sección 6).
- **Numeración de módulo no es 1–22 consecutivo**: los 22 números usados son `1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,26` — es decir, 21 números consecutivos **más el 26**, saltándose 22–25. Esto es evidencia circunstancial a favor de que existe un catálogo numerado más grande y compartido del que Prepa solo usa un subconjunto (los números 22–25 estarían "reservados" para algo más) — coincide con la premisa del brief de un catálogo global. Pero ver el matiz de la siguiente hoja, que la complica.

**Hoja "SECU Y PRIMA"** — contiene primero un resumen de diseño curricular (filas 4–18) y luego dos tablas de seguimiento independientes, cada una con su propia numeración local:

- **Resumen de diseño (filas 4–18)**, confirmado tal cual el brief:
  - *Primaria (5 módulos)*: "1. PRIMARIA ICEA (Modelo Intensivo)", "5 módulos base + 1 diversificado obligatorio", "META: 1 módulo cada 2–3 semanas".
  - *Secundaria (7 módulos)*: "2. SECUNDARIA ICEA (Modelo Intensivo)", "7 módulos base + 2 diversificados", "META: 1 módulo cada 2 semanas".
- **Tabla "PRIMARIA ABIERTA ICEA (2 MESES)"** (filas 20–27): mismo layout de columnas que PREPA pero sin `Bloque`. Usa **numeración local 1–5 más `D1`** (el diversificado, ej. fila 27 = `C:"D1", E:"Vivamos mejor"`) — **no `1–22`, ni comparte números con Prepa** (el módulo 1 de Primaria es "Lengua y comunicación 1", que nada tiene que ver con el módulo 1 de Prepa, "De la información al conocimiento").
- **Tabla "SECUNDARIA ABIERTA ICEA (3 MESES)"** (filas 30–40+): igual patrón, numeración local **1–7 más `D1`, `D2`** (ej. fila 39 `C:"D1", E:"Ser joven"`; fila 40 `C:"D2", E:"Un hogar sin violencia"`). El módulo 1 de Secundaria es "Lengua y comunicación 3" — **numeración local independiente de Prepa, no el "26" que menciona el brief.**

**Corrección directa al brief:** no encontré evidencia, en estos 2 archivos, de que "el módulo 26 sea de Secundaria". El número 26 ("Administración") aparece dentro de la propia tabla PREPA (fila 24, Bloque II). Lo que sí confirmé es que el encabezado de "Control Académico" en el segundo Excel (ver 0.2) usa exactamente el conjunto de 22 números de la tabla PREPA — es decir, esa hoja de gradebook, tal como está capturada en este archivo, corresponde a **Prepa Abierta**, no a Secundaria. No hay una hoja "Control Académico" separada para Secundaria/Primaria en estos 2 archivos para contrastar. Esto deja una tensión real en la evidencia que no se debe resolver por adivinanza:
- A favor de un catálogo global compartido: el salto de numeración 21→26 en Prepa.
- En contra: Secundaria y Primaria usan numeración local 1–7 y 1–5 en sus propias tablas de seguimiento, sin señal de compartir rango con Prepa.

Ver pregunta abierta #2 en la sección 6 — **no se debe modelar el catálogo asumiendo una respuesta a esto sin confirmar con la dueña.**

### 0.2 `ICEA_Control_Prospectos_y_Comisiones.xlsx` — 5 hojas

El archivo es un export de Google Sheets a `.xlsx` (confirmado por namespaces `xmlns:mx="...mac/excel/2008..."` y fórmulas envueltas en `__xludf.DUMMYFUNCTION(...)`, el marcador que Excel usa para funciones de Google Sheets sin equivalente nativo como `QUERY` y `FILTER`). Esto explica por qué varias hojas son 100% fórmulas encadenadas entre sí — es un sistema vivo de hojas enlazadas, no una plantilla estática.

1. **"Form Responses 1"** (23 filas de datos reales, no 1000+): encabezados confirmados `Timestamp | Fecha | Asesor | Empresa o lugar de origen | Nombre del prospecto | Celular del prospecto | Interesado en: | ¿Cuándo desea comenzar | CONTACTADO | RESPUESTA`. **Los 23 leads reales tienen `Interesado en:` = "Preparatoria abierta" en el 100% de los casos** (una fila dice literalmente "PREPA PARA SU HIJO"). No hay ni un solo lead de Secundaria o Primaria en esta muestra — coincide con que Prepa es, en la práctica, la línea de negocio activa hoy para este canal de ventas.
   - **Hallazgo no mencionado en el brief:** la columna "CONTACTADO" a veces contiene **"Nancy" o "SUSANA"** — los mismos nombres de las asesoras *académicas* del primer Excel, no nombres de asesores de ventas (que en la columna "Asesor" son "Monserrat", "Iván", "Valeria", "Jazmin Selene Glz"). Es decir: **en al menos algunos casos, el seguimiento de un prospecto lo hace directamente una maestra/asesora académica, no el vendedor.** Esto es relevante para la pregunta de diseño de roles (ver sección 6, pregunta #8).
   - La columna "¿Cuándo desea comenzar" casi nunca tiene una fecha real: los valores observados son `"NP"` y `"Solo pregunta para evaluar"` — es decir, funciona más como un indicador de intención/urgencia que como una fecha capturable.

2. **"Prospectos y Comisiones"** (23 filas reales, alimentadas por fórmula `IF('Form Responses 1'!A2:A1000="","",...)` desde la hoja anterior — no captura manual duplicada). Encabezados: `Fecha | Asesor | Empresa / Origen | Nombre del prospecto | Celular | Inscrito | Comisión generada ($) | Comisión pagada (Sí/No) | Fecha de pago | Monto comisión pagada`.
   - **Hallazgo importante no mencionado en el brief — la fórmula real de comisión**, verificada en la celda G2: `=IF(F2="Sí",300,IF(F2="Referido",100,IF(F2="Seguimiento",200,0)))`. Esto significa que **`Inscrito` no es un booleano Sí/No** como sugiere el nombre de la columna — es un **estado con al menos 4 valores posibles**: `"Sí"` (inscrito, comisión $300), `"Referido"` (comisión $100), `"Seguimiento"` (comisión $200) y vacío/`"No"` (comisión $0). Este es un hallazgo que cambia directamente cómo modelar el estado del prospecto (no es un booleano `inscrito`, es un enum de resultado).
   - **En las 23 filas reales, la columna `Inscrito` está completamente vacía** — ningún prospecto capturado en este archivo tiene resultado de venta registrado todavía. Esto importa para calibrar urgencia: la parte de comisiones/checklist de documentos existe estructuralmente (fórmulas listas) pero **no hay evidencia en este archivo de que se esté usando activamente para cerrar el ciclo de venta** — puede que la dueña lo lleve en otro medio, o que aún no se haya cerrado ninguna venta en el rango capturado. Confirmar con ella (pregunta abierta #6).

3. **"Pagos de Comisiones"**: confirmado que es un rollup — la fórmula usa `QUERY('Prospectos y Comisiones'!B:J, "select Col1, sum(Col6), sum(Col9), sum(Col6)-sum(Col9) ... group by Col1", 1)`, envuelta en `__xludf.DUMMYFUNCTION` (función de Google Sheets no soportada al exportar a xlsx, por lo que en este archivo **no hay valores cacheados**, solo la fórmula). Confirma exactamente lo que decía el brief: total generado / pagado / pendiente por asesor de ventas, agregado desde la hoja anterior.

4. **"Control Administrativo"**: encabezados confirmados `Fecha | Asesor | Empresa / Origen | Nombre del prospecto | Celular | Acta de nacimiento | CURP | Comprobante de domicilio | Fotos tamaño infantil | Identificación oficial | Papelería COMPLETA | Inscripción pagada | Mensualidad 1 | Mensualidad 2 | Mensualidad 3 | Mensualidad 4 | Mensualidad 5 | Pagos completos`.
   - **Corrección al brief: no son 5 columnas de pago, son 6** — "Inscripción pagada" **más** las 5 mensualidades. La fórmula de `Pagos completos` lo confirma: `=IF(COUNTA(L2:Q2)=0,"",IF(COUNTIF(L2:Q2,"Sí")=6,"Sí","No"))` — cuenta 6 columnas (L a Q), no 5.
   - `Papelería COMPLETA` también es calculada: `=IF(COUNTA(F2:J2)=0,"",IF(COUNTIF(F2:J2,"Sí")=5,"Sí","No"))` — confirma los 5 documentos exactos del brief.
   - **La fila A2 se autopuebla** con `=IFERROR(FILTER('Prospectos y Comisiones'!A2:E1000,'Prospectos y Comisiones'!F2:F1000="Sí"),"")` — es decir, **este checklist administrativo solo recibe filas automáticamente cuando `Inscrito = "Sí"`** en la hoja de prospectos. Esto confirma un flujo real encadenado: `Prospecto → (Inscrito="Sí") → aparece en Control Administrativo → se llenan documentos y pagos`. No es una hoja aislada, es la segunda etapa de un pipeline de 3 pasos.
   - **No hay ninguna fila con datos reales** (todas vacías salvo la fórmula del encabezado) — consistente con que ningún prospecto real tiene `Inscrito="Sí"` todavía en este snapshot.

5. **"Control Académico"**: confirmado que es el gradebook real, con la estructura exacta descrita: `No. | Alumno | Papelería COMPLETA | Nombre(s) | Apellido(s)` y luego, por módulo, 4 subcolumnas repetidas `Mod. N | Fecha agenda | ¿Acreditó? | Reagenda | ¿Acreditó?`. Confirmado: **22 bloques de módulo en el encabezado**, con números `1, 6, 7, 2, 4, 13, 3, 10, 14, 16, 8, 21, 11, 5, 15, 9, 17, 18, 12, 26, 19, 20` — **exactamente el mismo conjunto de 22 números que la tabla PREPA** (mismo orden en el que aparecen ahí, no orden numérico). Esto confirma que, en este archivo, esta hoja de "Control Académico" es específicamente para alumnos de **Prepa Abierta**.
   - `Papelería COMPLETA` aquí se trae por `VLOOKUP` contra "Control Administrativo" — reusa el mismo dato, no lo vuelve a capturar.
   - `Alumno` se trae por `FILTER('Control Administrativo'!D:D, D:D<>"")` — es decir, un alumno solo aparece aquí cuando ya tiene fila en Control Administrativo (que a su vez solo existe si `Inscrito="Sí"`). Confirma el pipeline de 3 etapas completo: **Prospecto → Inscrito=Sí → Control Administrativo (documentos/pagos) → Control Académico (módulos)**.
   - **No hay ni un solo alumno real cargado** — la hoja tiene 18,535 filas generadas por `=SEQUENCE(1000,1,1,1)` pero todas vacías de datos de alumno/módulo. No hay datos reales de acreditación para analizar patrones de uso; solo la estructura.

### 0.3 Resumen de lo verificado vs. lo no verificable

| Afirmación del brief | Verificado | Nota |
|---|---|---|
| Plan curricular fijo de 3 programas con conteo de módulos y ritmo | ✅ Confirmado | Primaria 5+1, Secundaria 7+2, Prepa 22 en 4 bloques |
| Domingo de aplicación compartido por semana, no individual | ✅ Confirmado | Se repite entre módulos de la misma semana |
| ¿Acreditó? binario Sí/No | ✅ Confirmado | Sin escala numérica en ningún lado de estos 2 archivos |
| Columna calculada OK/Reagendar por fórmula | ✅ Confirmado | Fórmula de array exacta verificada |
| Numeración de módulo compartida entre Secundaria y Prepa (ej. módulo 26) | ⚠️ **No verificable / evidencia mixta** | Ver corrección en 0.1 — es una pregunta abierta, no un hecho confirmado |
| Asesor de ventas ≠ Asesora académica (cuidado con el nombre) | ✅ Confirmado, y con matiz nuevo | A veces la misma persona (Nancy/Susana) hace ambas funciones en la práctica |
| 5 mensualidades en Control Administrativo | ⚠️ **Corregido: son 6 pagos** | Inscripción pagada + 5 mensualidades |
| Rollup de comisiones con SUMIF | ⚠️ **Corregido: es QUERY, no SUMIF** | Detalle menor, mismo efecto (agregación por asesor) |
| Comisión es un monto fijo por inscripción | ⚠️ **Corregido: 3 tasas distintas** | $300 Sí / $100 Referido / $200 Seguimiento — `Inscrito` es un enum, no un booleano |

---

## 1. Comparación contra el modelo de datos actual

Modelo verificado en `src/main/java/.../model/*.java`:

| Entidad actual | Campos | ¿Cubre algo del flujo Excel? |
|---|---|---|
| `Usuario` (`rol`: ALUMNO/MAESTRO/ADMINISTRADOR, `estado`) | — | No tiene rol de asesor de ventas ni de "prospecto" (alguien sin cuenta todavía) |
| `Curso` (`tipo`: CURSO_SUELTO/SECUNDARIA/PREPA_ABIERTA) | — | **`TipoCurso` no tiene `PRIMARIA_ABIERTA`** — hoy el sistema no puede ni representar ese programa como tipo de curso, aunque el Excel lo trata como línea de negocio igual de real que Secundaria y Prepa |
| `Inscripcion` (`estado`: ACTIVA/FINALIZADA/CANCELADA) | — | No tiene ningún campo de documentos, pagos, ni vínculo con un prospecto de origen |
| `Tema` (`curso`, `titulo`, `orden`) | — | Ver análisis detallado abajo |
| `AvanceTema` (`alumno`+`tema` única, `fechaCompletado`) | — | Ver análisis detallado abajo |
| `Actividad`/`Entrega` (`calificacion: Double` libre) | — | La calificación de Entrega es numérica libre; el Excel usa binario Sí/No. Son conceptos distintos, no el mismo dato con distinta escala |

### ¿"Tema" equivale a "Módulo"? — No, son conceptos distintos que hay que reconciliar, no fusionar sin más

Comparando campo a campo:

- `Tema.orden` es un entero libre por curso, sin numeración global ni significado fuera de ese curso — el "Módulo" del Excel tiene un número de catálogo con peso propio (referenciado desde otra hoja, el gradebook), no es solo un orden de despliegue.
- `AvanceTema` es un simple booleano implícito por existencia de fila (`fechaCompletado` presente = completado), **sin segundo intento, sin fecha de examen, sin resultado explícito Sí/No** — el Excel tiene 4 sub-datos por módulo por alumno (`Fecha agenda | ¿Acreditó? | Reagenda | ¿Acreditó?`), es decir, **hasta 2 intentos con su propia fecha y resultado cada uno**. `AvanceTema` no tiene espacio para modelar un reintento sin perder el primero.
- `Tema` no tiene fecha de examen ni concepto de "domingo de aplicación" compartido por cohorte — es contenido de estudio libre, no un examen calendarizado.
- El catálogo de módulos de Prepa/Secundaria/Primaria es **fijo y reutilizable entre ciclos/alumnos** (el mismo módulo "Administración" #26 aplica a cualquier alumno de Prepa que llegue a esa semana), mientras que `Tema` se crea **por curso individual** (`POST /api/temas` recibe `curso` + `titulo`, sin ninguna noción de reutilización entre cursos).

**Conclusión:** `Tema`/`AvanceTema` sirve bien para "Curso suelto" (contenido libre, progreso simple, sin examen formal ni reintentos) y **no debe forzarse** a representar el catálogo de módulos de los 3 programas estructurados. Son dos necesidades de negocio distintas coexistiendo bajo el mismo `TipoCurso`: cursos sueltos con temario libre vs. programas con currícula fija + examen binario + reintento. Esto requiere una entidad nueva (`Modulo`, ver sección 3), no una extensión de `Tema`.

---

## 2. Tres áreas funcionales ausentes hoy — propuesta de modelado (sin código)

### a) Catálogo de módulos por programa + calendario de domingos de aplicación + Acreditado Sí/No + reagenda

Propuesta de entidades nuevas, en prosa/pseudocódigo JPA:

```
Modulo
  id
  programa: TipoCurso (SECUNDARIA / PREPA_ABIERTA / PRIMARIA_ABIERTA — requiere agregar este valor al enum)
  numeroCatalogo: int          // "No. Mód" — ver pregunta abierta sobre si es único global o único por programa
  nombre: String
  bloque: String (nullable)    // "I".."IV", solo aplica a Prepa
  areaTematica: String (nullable) // lo observado en "Observaciones" (Matemáticas, Idiomas, Ciencias) — pendiente de confirmar su función real
  ordenSecuencia: int          // posición dentro del ritmo del programa (Mes/Semana)
  esDiversificado: boolean     // para los D1/D2 de Secundaria y el D1 de Primaria

DomingoAplicacion
  id
  modulo: Modulo (o cohorte, ver más abajo)
  fecha: LocalDate
  // representa el "Dom. Aplic." fijo, compartido por todos los alumnos de esa cohorte en esa semana

AvanceModulo   // equivalente a AvanceTema/Entrega pero para catálogo fijo con reintento
  id
  alumno: Usuario
  modulo: Modulo
  fechaAgendaPrimeraAplicacion: LocalDate (nullable)
  acreditoPrimeraAplicacion: Boolean (nullable)   // null = pendiente, no usar Double
  fechaReagenda: LocalDate (nullable)
  acreditoReagenda: Boolean (nullable)
  // estado derivado (no persistido): OK si acreditoPrimeraAplicacion=true o acreditoReagenda=true; Reagendar si acreditoPrimeraAplicacion=false y aún no hay reagenda
```

El "Domingo de aplicación" fijo por semana, compartido por todos los alumnos, implica que probablemente se necesita una noción de **cohorte/generación** (grupo de alumnos que arrancó junto y avanza junto) para que las fechas de `DomingoAplicacion` tengan sentido — si dos alumnos de Prepa empezaron en meses distintos, sus "Semana 1" no caen en la misma fecha real. Esto conecta directamente con la pregunta abierta #5 de la auditoría anterior sobre periodos académicos (ver sección 4). **No se debe asumir sin confirmar** si la academia maneja cohortes formales o si en la práctica solo hay "el grupo activo" en cada momento (dado que solo hay 2 asesoras, Nancy y Susana, es plausible que hoy solo exista un grupo o muy pocos en paralelo).

### b) Embudo de ventas: prospectos, asesor de ventas, comisión

```
AsesorVentas          // ver pregunta abierta sobre si necesita login (Rol nuevo) o es solo un catálogo
  id
  nombre: String
  telefono: String (nullable)
  activo: boolean

Prospecto
  id
  fecha: LocalDate
  nombreProspecto: String
  celular: String
  empresaOrigen: String (nullable)
  programaInteres: TipoCurso (nullable, texto libre en el Excel — "Preparatoria abierta", "PREPA PARA SU HIJO")
  cuandoQuiereComenzar: String   // texto libre observado ("NP", "Solo pregunta para evaluar"), no una fecha real — no forzar a LocalDate
  asesorVentas: AsesorVentas
  contactadoPor: String (nullable)     // observado: a veces es una asesora académica, no el asesor de ventas — ver pregunta abierta #8
  notasRespuesta: String (nullable)
  estadoInscripcion: EstadoProspecto (enum: SIN_RESULTADO / SEGUIMIENTO / REFERIDO / INSCRITO)  // reemplaza el booleano "Inscrito", confirmado que son 4 estados reales por la fórmula de comisión
  montoComisionGenerada: BigDecimal   // calculable en backend con la misma regla verificada: 300/100/200/0 según estadoInscripcion
  comisionPagada: boolean
  fechaPagoComision: LocalDate (nullable)
  montoComisionPagada: BigDecimal (nullable)
  inscripcion: Inscripcion (nullable, FK) // se llena cuando estadoInscripcion pasa a INSCRITO y se crea la Inscripcion real
```

No se propone una entidad `Comision` separada: la relación prospecto→comisión es 1:1 en los datos reales (un prospecto genera cuando mucho una comisión), así que separarla sería una tabla extra sin beneficio real hoy. El rollup "Pagos de Comisiones" no necesita tabla propia — es un `GROUP BY asesorVentas` calculable en una consulta/endpoint de reporte.

### c) Checklist de documentos + plan de mensualidades

```
ExpedienteInscripcion   // 1:1 con Inscripcion
  id
  inscripcion: Inscripcion
  actaNacimiento: boolean
  curp: boolean
  comprobanteDomicilio: boolean
  fotos: boolean
  identificacionOficial: boolean
  // papeleriaCompleta: no persistir, calcular (los 5 anteriores = true)
  inscripcionPagada: boolean

PagoMensualidad         // 1:N en vez de 5 columnas fijas Mensualidad1..5
  id
  expediente: ExpedienteInscripcion
  numeroMensualidad: int   // 1..5 hoy, pero modelarlo como lista evita hardcodear "5" si el plan de pagos cambia
  pagada: boolean
  fechaPago: LocalDate (nullable)
  // monto: el Excel no registra montos, solo Sí/No — no inventar un campo de monto sin confirmar que se necesita
```

Se propone `PagoMensualidad` como lista (`@OneToMany`) en vez de 5 columnas booleanas fijas replicando el Excel literalmente, porque el costo adicional en JPA es mínimo y evita que un cambio futuro de "5 mensualidades" a otro número requiera una migración de esquema. Esto es la única desviación deliberada del layout exacto del Excel en esta propuesta, y se señala explícitamente como decisión de diseño, no como hallazgo de negocio.

---

## 3. Reconciliación con `docs/auditoria-panel-admin.md`

| Pregunta abierta anterior | Efecto de esta nueva información |
|---|---|
| **#1 Escala de calificación** (0–10, 0–100, letras, aprobado/no aprobado) | **Respondida parcialmente, con alcance limitado.** Para Secundaria/Primaria/Prepa: es binaria, Acreditado Sí/No, sin escala numérica — confirmado en ambos Excel, sin excepción. **Pero esto no aplica a "Curso suelto"**, que sigue usando `Entrega.calificacion: Double` y no aparece en ningún Excel — esa pregunta sigue abierta para esa línea de negocio específicamente. No contradice la auditoría anterior, la acota. |
| **#2 Aprobación de inscripción a curso suelto vs. Secundaria/Prepa** | **Respondida para Secundaria/Primaria/Prepa, no para curso suelto.** El pipeline verificado (Prospecto → Inscrito=Sí → Control Administrativo → Control Académico) muestra un proceso de venta y documentación formal antes de que un alumno "cuente" académicamente, muy distinto del autoservicio instantáneo de `catalogo.html` para cursos sueltos. La pregunta original seguía siendo válida y ahora tiene evidencia real detrás para uno de los dos casos. |
| **#3 Cupo máximo por curso** | Sin cambios — ningún Excel menciona límites de capacidad. Sigue abierta. |
| **#4 Prerrequisitos entre cursos** | **Parcialmente respondida.** Dentro de cada programa (Primaria/Secundaria/Prepa) hay una secuencia fija de módulos por semana — no es un catálogo libre. No queda claro si es estrictamente bloqueante (no puedes hacer el módulo 7 sin haber acreditado el 6) o solo una guía de ritmo recomendado — **pregunta abierta nueva, más específica que la anterior** (ver sección 6). No aplica a "curso suelto", que sigue sin prerrequisitos documentados. |
| **#5 Periodos académicos / ciclos** | **Respondida: sí existen.** El ritmo fijo por semana y el "Domingo de aplicación" compartido confirman que la academia opera con calendario de cohorte para estos 3 programas, no inscripción 100% continua. Esto reabre la necesidad de un concepto de cohorte/generación no contemplado en la auditoría anterior (ver sección 2a). |
| **#6 Constancias/reportes imprimibles** | Sin cambios — no hay evidencia en estos Excel. Sigue abierta. |
| **#7 Suspensión de cuenta activa** | No relacionado con esta información nueva. Sigue abierta tal cual. |
| **#8 Bandeja de solicitudes de contacto** | **Reencuadrada, no solo respondida.** La auditoría anterior especulaba sobre si hacía falta una "bandeja de mensajes" para no perder solicitudes de WhatsApp. Lo que muestran estos Excel es que la dueña **ya tiene un embudo de ventas completo y funcional fuera del sistema** (Google Form + hoja de prospectos + comisiones), mucho más elaborado que una simple bandeja de contacto. La necesidad real no es "no perder mensajes de WhatsApp de cursos sueltos" sino **integrar o reemplazar un proceso de ventas ya maduro** — cambia sustancialmente el tamaño y la prioridad de este ítem frente a como se planteó originalmente. |
| **#9 Purga de archivos de entregas** | No relacionado. Sigue abierta tal cual. |

---

## 4. Riesgos y preguntas abiertas para confirmar con la dueña antes de construir

No se inventa ninguna respuesta de negocio a lo siguiente — son bloqueantes de diseño reales:

1. **Numeración del catálogo de módulos:** ¿es verdaderamente un pool global compartido entre Primaria/Secundaria/Prepa (sugerido por el salto 21→26 en Prepa), o cada programa tiene su propio rango independiente (sugerido por que Secundaria y Primaria usan 1–7 y 1–5 en sus propias tablas)? Esto determina si `Modulo.numeroCatalogo` debe ser único globalmente o único por programa — es la decisión de modelado más importante de las tres áreas nuevas y no se puede inferir de forma concluyente con la evidencia disponible en estos 2 archivos.
2. **Rol de asesor de ventas:** ¿necesita cuenta/login en el sistema, o basta con que el administrador capture sus datos manualmente (como hoy en el Excel)? Se observó que a veces una asesora académica (Nancy/Susana) hace de contacto de ventas — ¿el diseño debe permitir que una misma persona tenga ambos roles, o son siempre personas distintas en la realidad y esto fue una excepción puntual?
3. **¿"Acreditó módulo" reemplaza el avance de `Tema`/`AvanceTema` para estos 3 programas, o coexisten?** Por ejemplo: ¿el alumno sigue viendo una lista de "temas de estudio" semanales dentro de un módulo (contenido) además del resultado binario del examen del módulo (evaluación), o el módulo ES la única unidad de seguimiento, sin subdividir en temas?
4. **Significado de la columna "Observaciones" con etiquetas de área** (`Matemáticas – solo`, `Idiomas I/II`, `Ciencias`): ¿agrupa módulos que se examinan juntos un mismo domingo, identifica un eje temático para reportes, o es simplemente una nota operativa sin impacto en el modelo de datos?
5. **¿Es bloqueante el orden de los módulos?** ¿Un alumno puede intentar el módulo de la semana 10 sin haber acreditado el de la semana 3, o el sistema debe impedirlo?
6. **Vigencia real del embudo de ventas en Excel:** en el snapshot analizado, ninguno de los 23 prospectos reales tiene resultado de venta (`Inscrito`) registrado y ninguna fila de Control Administrativo/Académico tiene datos. ¿Es que la dueña ya no usa esta parte del Excel activamente (lleva el cierre de venta en otro medio), o es que aún no se ha cerrado ninguna venta en el rango de fechas capturado? Esto cambia qué tan urgente es migrar la parte de comisiones frente al checklist de documentos.
7. **Significado exacto de los 3 estados positivos de `Inscrito`** (`Sí` $300, `Referido` $100, `Seguimiento` $200): ¿qué acción de negocio dispara cada uno? ¿"Referido" significa que el prospecto se derivó a otro asesor/programa y aun así genera comisión parcial?
8. **Cohortes/generaciones:** dado que el "Domingo de aplicación" es una fecha fija compartida por semana, ¿los alumnos de un mismo programa siempre avanzan juntos como un solo grupo (así parece operar hoy, con solo 2 asesoras), o pueden existir varias cohortes en paralelo con calendarios distintos?
9. **Montos de mensualidad:** el Excel solo registra si cada mensualidad está pagada (Sí/No), sin monto. ¿Se necesita capturar el monto de cada mensualidad en el sistema, o basta con el estado pagada/pendiente como hoy?
10. **`TipoCurso` no tiene valor para Primaria Abierta hoy** — si se confirma que Primaria es una línea de negocio activa (el Excel la documenta con el mismo detalle que Secundaria), hace falta agregar `PRIMARIA_ABIERTA` al enum antes de poder representarla en absoluto.

### Riesgo técnico a tener presente

Todo lo propuesto aquí hereda el mismo hueco de seguridad ya documentado en la auditoría anterior (`SeguridadConfig` sin `@PreAuthorize`, sin sesión de servidor) — cualquier endpoint nuevo para prospectos/comisiones/documentos de pago sería tan abierto como los existentes hoy. Dado que esta área maneja datos más sensibles que un catálogo de cursos (teléfonos de prospectos, montos de comisión, estatus de pago de colegiatura), este riesgo se vuelve más relevante a medida que se construya esta parte, no menos.

---

## 5. Priorización propuesta (esfuerzo/valor)

1. **(c) Checklist de documentos + mensualidades** — esfuerzo bajo-medio, no depende de resolver la ambigüedad de numeración de módulos ni la decisión de rol de asesor de ventas. Es la pieza más aislada y de valor operativo inmediato (la dueña ya necesita saber quién debe papelería/pago sin abrir un Excel aparte).
2. **(b) Embudo de ventas y comisiones** — esfuerzo bajo si se modela como se propone en la sección 2b (sin tabla `Comision` separada), pero bloqueado por la pregunta #2 (rol de asesor de ventas) antes de tocar código. Independiente de (a).
3. **(a) Catálogo de módulos + calendario + acreditación** — mayor valor a largo plazo pero también el mayor esfuerzo y el que depende de más decisiones de negocio sin resolver (numeración global vs. local, relación con `Tema`, cohortes). Se recomienda dejarlo para el final, después de que la dueña resuelva las preguntas #1, #3, #5 y #8 de la sección 4 — construir sobre una decisión equivocada aquí sería el rediseño más caro de deshacer de los tres.

---

## Resumen ejecutivo

Los dos Excel muestran un negocio bastante más elaborado del lado de ventas/administración de lo que el sistema actual contempla: un embudo de captación de prospectos con comisiones reales por resultado (no un simple booleano inscrito/no inscrito), un checklist de documentos y un plan de pago de 6 conceptos (no 5), y un catálogo curricular fijo con calendario de cohorte y reintento de examen — ninguna de las tres cosas tiene equivalente hoy en el modelo de datos. La pieza de menor riesgo para empezar es el checklist de documentos/mensualidades; la de mayor riesgo y mayor valor a futuro es el catálogo de módulos, precisamente porque su decisión de diseño central (¿numeración global o por programa?) no se puede resolver solo con estos 2 archivos y debe confirmarse con la dueña antes de modelar nada. En paralelo, esta información resuelve o acota varias preguntas abiertas de la auditoría del panel de administrador (escala de calificación, periodos académicos), pero dentro del alcance específico de Secundaria/Primaria/Prepa — no debe asumirse que también aplican a "curso suelto", que sigue sin evidencia propia en ningún Excel.
