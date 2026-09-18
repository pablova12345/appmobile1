require('dotenv').config();
const express = require('express');
const cors = require('cors');

const llm = require('./llm');
const catalogRoute = require('./routes/catalog');
const classifyRoute = require('./routes/classify');
const answerRoute = require('./routes/answer');
const hoja2Route = require('./routes/hoja2');

const app = express();

app.use(cors({ origin: process.env.CORS_ORIGIN === '*' ? true : process.env.CORS_ORIGIN?.split(',') }));
// Las imagenes van en base64 dentro del JSON -- una pagina escaneada puede
// pesar varios MB, asi que se sube el limite por defecto de express.
app.use(express.json({ limit: '40mb' }));

app.get('/health', async (req, res) => {
  // Incluye el estado del proveedor de IA (Ollama): si responde y si tiene el
  // modelo configurado. Sirve para diagnosticar los "no se conecto" del celular
  // sin tener que mirar la terminal.
  res.json({ ok: true, ia: await llm.health() });
});

app.use('/api/catalog', catalogRoute);
app.use('/api/classify', classifyRoute);
app.use('/api/answer', answerRoute);
app.use('/api/hoja2', hoja2Route);

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`[server] AppTramitesSisCat AI backend escuchando en :${PORT}`);

  // Precarga el modelo de Ollama a VRAM para que el primer pedido del celular
  // no pague el arranque en frio (~1 min) y no lo corte por timeout. No
  // bloquea el arranque del server ni revienta si Ollama todavia no esta.
  llm
    .warmup()
    .then((r) => {
      if (r?.skipped) return;
      console.log('[server] Ollama precargado y listo (keep_alive activo).');
    })
    .catch((err) => {
      console.warn(
        `[server] No se pudo precargar Ollama (${err.message}). ` +
          'El primer pedido va a tardar mas; revisar que Ollama este corriendo.',
      );
    });
});
