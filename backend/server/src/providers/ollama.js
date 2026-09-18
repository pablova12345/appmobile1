const { stripDataUriPrefix } = require('../util');

// El modelo local (Ollama) tiene que generar la respuesta completa antes de
// devolver nada (stream: false). Con qwen3-vl:2b + think:false, clasificar
// tarda ~12-35s medido en una RTX 3050 4GB (ver server/README.md) -- pero en
// FRIO (modelo recien cargado a VRAM) el primer pedido puede pasar del minuto.
// Por eso: (a) timeout amplio, (b) keep_alive:-1 para que el modelo NO se
// descargue entre pedidos, (c) warmup() al arrancar el server (ver index.js).
// Sintomas cuando esto falla: el celular corta con "network connection lost" /
// "fetch canceled" porque su propio timeout salta antes que la respuesta.
const TIMEOUT_MS = 180000;

// -1 = mantener el modelo en memoria indefinidamente (hasta que Ollama pare).
// Sin esto Ollama lo descarga a los ~5 min de inactividad y el pedido
// siguiente vuelve a pagar el arranque en frio.
const KEEP_ALIVE = process.env.OLLAMA_KEEP_ALIVE || -1;

function ollamaUrl() {
  return (process.env.CHATBOT_OLLAMA_URL || process.env.OLLAMA_URL || 'http://127.0.0.1:11434').replace(/\/+$/, '');
}

function ollamaModel() {
  return process.env.OLLAMA_MODEL || 'qwen3-vl:2b';
}

// Proveedor gratis/local para probar el flujo completo (clasificar,
// responder, extraer) sin gastar creditos de Anthropic. Ollama no distingue
// entre "tier" (Haiku vs Sonnet), usa el unico modelo que tengas cargado.
//
// Usa /api/chat (no /api/generate): qwen3-vl "piensa" en voz alta antes de
// responder (chain-of-thought token por token), y /api/generate ignora
// think:false para este modelo -- es un bug conocido de Ollama. /api/chat SI
// lo respeta, y sin eso una extraccion de tabla nunca terminaba (>10 minutos
// y seguia "pensando" sin haber escrito una sola letra de la respuesta
// real). Ver server/README.md sobre por que se paso de qwen3-vl:4b a
// qwen3-vl:2b (4b no entraba completo en 4GB de VRAM y se repartia con CPU;
// 2b si entra, ~4-6x mas rapido en las pruebas).
async function complete({ system, images, userText }) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    let res;
    try {
      res = await fetch(`${ollamaUrl()}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: ollamaModel(),
          think: false,
          stream: false,
          keep_alive: KEEP_ALIVE,
          messages: [
            { role: 'system', content: system },
            {
              role: 'user',
              content: userText,
              images: (images || []).map(stripDataUriPrefix),
            },
          ],
        }),
        signal: controller.signal,
      });
    } catch (err) {
      if (err.name === 'AbortError') {
        throw new Error(
          `Ollama no respondio en ${TIMEOUT_MS / 1000}s (modelo "${ollamaModel()}" en ${ollamaUrl()}). ` +
            'Suele ser el arranque en frio: probá de nuevo, o dejá el server con warmup activo.',
        );
      }
      throw new Error(`No se pudo contactar a Ollama en ${ollamaUrl()}: ${err.message}`);
    }
    if (res.status === 404) {
      throw new Error(
        `Ollama no tiene el modelo "${ollamaModel()}". Corré: ollama pull ${ollamaModel()} ` +
          '(o ollama create ... si es el modelo propio del Modelfile).',
      );
    }
    if (!res.ok) throw new Error(`Ollama respondio con estado ${res.status}`);
    const data = await res.json();
    return data.message?.content ?? '';
  } finally {
    clearTimeout(timeoutId);
  }
}

// Carga el modelo a memoria con un mensaje minimo (sin inferencia util). Se
// llama al arrancar el server para que el PRIMER pedido del celular no pague
// el arranque en frio. No revienta si Ollama todavia no esta levantado.
async function warmup() {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(`${ollamaUrl()}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: ollamaModel(),
        think: false,
        stream: false,
        keep_alive: KEEP_ALIVE,
        messages: [{ role: 'user', content: 'ok' }],
      }),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`estado ${res.status}`);
    return true;
  } finally {
    clearTimeout(timeoutId);
  }
}

// Estado de Ollama para /health: ¿responde? ¿tiene el modelo configurado?
async function health() {
  try {
    const res = await fetch(`${ollamaUrl()}/api/tags`, { signal: AbortSignal.timeout(5000) });
    if (!res.ok) return { ok: false, url: ollamaUrl(), detalle: `estado ${res.status}` };
    const data = await res.json();
    const modelos = (data.models || []).map((m) => m.name);
    const esperado = ollamaModel();
    const tiene = modelos.some((n) => n === esperado || n === `${esperado}:latest`);
    return { ok: true, url: ollamaUrl(), modelo: esperado, modeloDisponible: tiene, modelos };
  } catch (err) {
    return { ok: false, url: ollamaUrl(), detalle: err.message };
  }
}

module.exports = { complete, warmup, health };
