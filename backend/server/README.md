# AppTramitesSisCat - AI backend

Backend chico (Node/Express) que hace de intermediario entre la app movil y
el modelo de IA (Claude, o un modelo local via Ollama para probar gratis). La
app nunca habla directo con el modelo.

## Por que existe

El modulo "Estudiar con la IA" de la app clasifica el tipo de documento que se
escaneo y muestra preguntas preescritas segun el tipo (con Ollama/Claude, ver
"Dos proveedores de IA"). Ese modulo **ya no interviene en las resoluciones**:
tienen su propio apartado en la app ("Resoluciones" -> `ResolucionesScreen`),
donde el usuario carga las paginas y se corre directamente el OCR para armar la
Hoja2 del Excel, sin pasar por ningun modelo de lenguaje (ver "Extraccion de la
tabla de superficies: OCR, no IA" abajo). Con Claude, clasificar/responder
implica mandar la API key de Anthropic, y esa llamada no puede salir directo
del celular porque la expondria -- por eso este backend intermedia siempre,
sea cual sea el proveedor configurado.

## Dos proveedores de IA (`AI_PROVIDER` en `.env`)

- **`ollama`** (default) -- gratis, corre local (`OLLAMA_URL`/`OLLAMA_MODEL`,
  hoy `qwen3-vl:2b` -- ver server/ollama/README.md, incluye por que se paso del
  4b al 2b). **Decision con el usuario (2026-09-07): el modulo "Estudiar con la
  IA" se queda en Ollama 2b por velocidad**, aceptando que lee peor tablas
  fusionadas, sellos y letra chica que Claude (no importa: los datos "duros"
  salen por OCR en el apartado de Resoluciones, no por el modelo).
- **`anthropic`** -- Claude Haiku/Sonnet, requiere `ANTHROPIC_API_KEY` con
  saldo cargado en console.anthropic.com (la API no tiene nivel gratuito).
  Queda como opcion si mas adelante se quiere mejor calidad en clasificar /
  responder preguntas.

`src/llm.js` elige el proveedor segun esa variable; `src/routes/*.js` no saben
ni les importa cual es -- para cambiar de uno a otro alcanza con editar
`.env`, no hay que tocar codigo.

## Setup

```bash
cd backend/server
npm install
cp .env.example .env   # AI_PROVIDER=ollama por defecto; ajustar OLLAMA_URL a la
                       # IP de la maquina con Ollama y OCR_API_URL si hace falta
npm run dev
```

Por defecto escucha en `http://localhost:4000`. La app apunta a esta URL via
`EXPO_PUBLIC_AI_BACKEND_URL` (ver `src/config/env.js` en la raiz del repo) --
en un celular fisico tiene que ser la IP de la maquina en la red local, no
`localhost`.

## Endpoints

- `GET /api/catalog` -- catalogo de tipos de documento + preguntas
  preescritas (fuente de verdad en `src/catalog.js`).
- `POST /api/classify { images: string[] }` -- clasifica el tipo de
  documento (tier "fast" -> el modelo de Ollama, o Haiku si `AI_PROVIDER=anthropic`).
  `images` son JPEG/PNG en base64. Las resoluciones se reconocen pero su tabla
  se extrae aparte (apartado "Resoluciones" de la app -> `/api/hoja2/*`).
- `POST /api/answer { images, tipo, pregunta }` -- responde una pregunta
  preescrita sobre el documento (tier "fast").
- `POST /api/hoja2/extract { images }` -- corre OCR (`OCR_API_URL`) sobre
  cada pagina y reconstruye una tabla por posicion (fila/columna). Devuelve
  `{ paginas: [{ pagina, columnCount, rows }] }` para que la app la muestre
  editable -- no interpreta que significa cada columna ni escribe nada en el
  Excel todavia. No depende de `AI_PROVIDER`.
- `POST /api/hoja2/fill { filas }` -- toma esas filas (ya revisadas/con
  columnas asignadas por el usuario en la app) y devuelve la plantilla
  `.xlsm` llenada en base64, preservando las macros VBA y las formulas
  existentes. No usa IA, no depende de `AI_PROVIDER`.

## Modelos usados

Decision con el usuario (2026-09-07): el modulo "Estudiar con la IA"
(clasificar + preguntas preescritas) corre en `AI_PROVIDER=ollama` con
`qwen3-vl:2b` -- se prioriza que sea rapido para todos los documentos que NO
son resoluciones. Con `AI_PROVIDER=anthropic` se usaria Haiku para lo mismo
(tier "fast"). Ninguno de los dos interviene en la extraccion de la tabla de
superficies de las resoluciones -- ver el punto siguiente.

## Extraccion de la tabla de superficies: OCR, no IA

Decision tomada con el usuario (2026-09-04, reforzada 2026-09-07), a pedido de
los ingenieros de GAMC: para la tabla "RELACION DE SUPERFICIE" no se usa un
modelo de lenguaje (ni Claude ni Ollama) -- se probo primero con el modelo de
4B y nunca termino de generar una respuesta en un tiempo razonable (paso de 10
minutos "pensando" antes de escribir una sola letra, ver el historial de esa
conversacion). Ademas, desde 2026-09-07 las resoluciones no se clasifican con
IA en absoluto: el usuario entra al apartado "Resoluciones" de la app, carga
las paginas y dispara el OCR directo. Se usa el servicio OCR real de GAMC
(`ocr.catastrocbba.com`, PaddleOCR con cola de trabajos) + un algoritmo propio
que agrupa los bloques de texto en filas/columnas SOLO por posicion (nunca
por contenido) -- adaptado del modulo "geoextraccion" del repo de referencia
que paso el ingeniero (`github.com/MateoBazo/proyecto-erp`).

Contrato del servicio OCR (confirmado contra el servicio real, ver
`src/ocr.js`):

```
POST {OCR_API_URL}/ocr/           (multipart, campo "file") -> { job_id }
GET  {OCR_API_URL}/ocr/result/{job_id}/json
  -> { status: "queued"|"done"|"failed", result: { result: [...] } }
  -- cada item: { points: [[x,y] x4], text, confidence (0-1) }
```

`src/tableFromOcr.js` agrupa esos bloques en filas (por salto de Y respecto
al bloque anterior, no por distancia a un ancla fija -- ver el comentario en
el archivo) y columnas (por posicion X). **A proposito no intenta adivinar
que significa cada columna**: en fotos reales el texto del encabezado que
devuelve el OCR sale bastante distorsionado (`"SUPEFICPANADA(m')"`,
`"Coratruids"` en vez de `"Construida"` -- probado con un documento real de
"Documentos para llenar a excel"), asi que no es confiable para inferirlo
solo. En la app, el usuario le asigna un rol a cada columna una sola vez
(`src/components/TablaSuperficies.jsx`) y corrige a mano las celdas que
salgan con confianza baja (resaltadas en rojo, umbral 0.85).

La columna "Planta" tampoco se intenta detectar automaticamente: en el
documento real el nivel/planta es una celda fusionada que abarca varias
filas, y el OCR la ubica en una posicion inconsistente segun donde haya caido
el texto -- por eso es un campo de texto libre por fila en la app, no una
columna mas.

Los umbrales de agrupamiento (`rowGapY`, `mergeGapX`, `columnGapX` en
`tableFromOcr.js`) se ajustaron contra un documento real
(`Documentos para llenar a excel/img1.webp`) y pueden necesitar retoque con
otros tamaños/resoluciones de imagen.

## Limitacion conocida: mapeo de columnas de Hoja2

El mapeo de `POST /api/hoja2/fill` (`src/routes/hoja2.js`) se dedujo leyendo
las FORMULAS reales de `templates/plantilla-ph.xlsm` (no solo los
encabezados), fila por fila, y probando el llenado contra la plantilla real.
Con eso quedo bastante mas claro que el primer intento:

- Fila de datos: empieza en la fila 10 (`HOJA2_FIRST_ROW`).
- **Se escriben:** `A` (Planta), `C` (Ambiente), `D`/`E` (Sup. Privada
  Construida/Libre), `G` (Sup. Ideal), `H`/`I` (Sup. Comun Construida/Libre).
- **NO se tocan `F` y `J`** aunque el JSON extraido traiga esos valores --
  son formulas (`F=D+E`, `J=D+H`) que se calculan solas; escribirles un
  numero fijo las rompe.
- El resto del bloque `N:BA` son formulas que se derivan de `A`, `B` y `C`
  (`N=A`, `O=B`, `Q=C`, etc.) o valores fijos del primer tramite (`AD10`,
  `AI10`) que se copian hacia abajo -- no hace falta ni conviene tocarlas.

**Sigue pendiente / sin confirmar:**

- **Columna `B`**: hay una segunda columna de "Planta" (`O10 = B10`, header
  "PLANTA" vs. el de `A` que es "Planta") que tambien la leen RESUMEN /
  Hoja 3 / MODEL SISCAT. No quedo claro si `B` debe llevar el mismo texto que
  `A`, un codigo corto (ej. "PB" vs. "Planta Baja"), u otra cosa -- se deja
  sin llenar (igual que en la plantilla original) para no meter un dato
  incorrecto en una columna que otras hojas consumen.
- **Codigo Catastral (columna `P`) y Nro R.E. (columna `S`)** no se llenan:
  esos datos no estan en la tabla "RELACION DE SUPERFICIE" de la resolucion
  (son de un tramite aparte, `REGISTRO_CATASTRAL` / `FOLIO_REAL`, que se
  genera despues). Quedan vacios a proposito.

Regla acordada con el usuario (2026-09-07): **se carga solo lo que se ve en las
imagenes**. Si un campo (columna `B`, o cualquier otro) no aparece en la tabla
escaneada o no tiene datos, se descarta y queda vacio -- no se infiere ni se
completa a mano por ahora. Esta version solo llena la **Hoja2**; el resto de
las hojas de la plantilla (RESUMEN / Hoja 3 / MODEL SISCAT) no se tocan y no
importa si sus formulas quedan sin datos aguas abajo.

Sigue pendiente para una version posterior: pedirle al ingeniero un Hoja2 ya
llenado a mano para comparar linea por linea contra lo que genera este
endpoint, y confirmar que es la columna `B`.

## Pendiente / fuera de este MVP

- Deploy: este server corre local por ahora. Falta decidir donde se aloja en
  produccion (mismo servidor que bkdgd.catastrocbba.com, u otro).
- Autenticacion entre la app y este backend (hoy no valida quien llama).
- Confirmar con el ingeniero los dos cambios de catalogo marcados en
  `src/catalog.js` (nota en `SENTENCIA` y en `RESOLUCION_ADMINISTRATIVA`).
- Probar `tableFromOcr.js` contra mas paginas reales (solo se probo contra
  una) y retocar `rowGapY`/`mergeGapX`/`columnGapX` si hace falta.
- Si varias paginas de la misma resolucion traen columnas en un orden
  distinto, hoy hay que reasignar los roles pagina por pagina en la app (no
  hay forma de "copiar" la asignacion de una pagina a otra).
