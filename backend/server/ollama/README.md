# Modelo propio de Ollama para clasificar documentos

Esto es lo que Ollama llama "crear un modelo": empaquetar un modelo base +
parametros + un system prompt propio bajo un nombre nuevo. **No es
fine-tuning** (no se tocan los pesos del modelo) -- con 1 a 17 ejemplos por
categoria que tenemos hoy, un fine-tune real se sobreajustaria o arruinaria
lo que el modelo ya sabe hacer, y ademas Ollama no entrena, solo *sirve*
modelos ya entrenados.

## Que hace este Modelfile

- `FROM qwen3-vl:2b` -- ver "Por que 2b y no 4b" abajo.
- `PARAMETER temperature 0.2` -- clasificacion mas consistente (menos
  "creativa"), el mismo documento deberia dar el mismo tipo cada vez.
- `PARAMETER num_ctx 8192` -- el default de Ollama (4096) se queda corto: el
  system prompt con las 19 categorias + sus "pistas" ya son ~4300 tokens el
  solo, antes de sumar la imagen.
- `SYSTEM` -- las 20 categorias del catalogo con las senas reales de cada una
  (titulos exactos, sellos, tablas) sacadas de revisar los documentos de
  ejemplo.
- `MESSAGE` -- 3 ejemplos de pregunta/respuesta para reforzar que conteste
  SOLO con el JSON pedido, sin texto alrededor (el fallo mas comun de
  modelos chicos no es que se equivoquen de categoria, es que no respeten el
  formato).

## Por que 2b y no 4b

Se empezo con `qwen3-vl:4b`, pero en la GPU real donde corre esto (RTX 3050
Laptop, 4GB VRAM) el modelo de 4B (4.5GB) no entraba completo y Ollama
repartia el trabajo 60%/40% entre GPU y CPU -- clasificar tardaba ~110-190s.
Probado el mismo catalogo con `qwen3-vl:2b` (2.3GB, entra casi entero en la
GPU: 82%/18%) contra 4 documentos reales: clasifico los 4 bien, en 12-34s
cada uno (measured 2026-09-04). Mismo prompt, mismo JSON, 4-6x mas rapido --
por eso se cambio la base del Modelfile.

## Importante: esto NO reemplaza el prompt que manda el backend

Ollama pisa el `SYSTEM` del Modelfile con el `system` que se manda en cada
pedido -- y `backend/server/src/routes/classify.js` siempre manda el suyo (armado
desde `src/catalog.js`, campo `pistas`). O sea: **lo que de verdad gobierna
la precision en la app es `src/catalog.js`**, no este Modelfile. Ya deje ahi
las mismas senas que estan aca. (El proveedor usa `/api/chat`, no
`/api/generate` -- ver el comentario en `src/providers/ollama.js` sobre por
que.)

Este Modelfile igual vale la pena tenerlo:

- Es el artefacto real y nombrado ("nuestro clasificador") que le podes
  mostrar a GAMC -- una version propia, no el modelo generico de Ollama.
- Sirve para probar a mano con `ollama run` sin tener que levantar el
  backend (ver abajo).
- Los `PARAMETER` (temperatura, etc.) SI se aplican siempre, sin importar
  quien mande el `system`.

Si mas adelante cambia el catalogo, actualizar `src/catalog.js` (que es lo
que usa la app) Y este `Modelfile` (para que `ollama run` a mano quede
consistente).

## Como crearlo

Correr esto en la maquina donde vive Ollama (hoy: la misma PC donde corre
`backend/server/`, ver Ollama instalado via winget el 2026-09-03):

```bash
cd backend/server/ollama
ollama create apptramites-clasificador -f Modelfile
```

Probarlo a mano (sin el backend, para ver que responda bien):

```bash
ollama run apptramites-clasificador "Clasifica este documento segun las instrucciones." /ruta/a/una/pagina.jpg
```

Despues, en `backend/server/.env`:

```
AI_PROVIDER=ollama
OLLAMA_URL=http://<ip-de-la-maquina-con-ollama>:11434
OLLAMA_MODEL=apptramites-clasificador
```

## Si el catalogo cambia

Editar `src/catalog.js` (campo `pistas` de cada tipo) y volver a correr
`ollama create apptramites-clasificador -f Modelfile` con el `SYSTEM` de este
archivo actualizado, para que ambos queden alineados.
