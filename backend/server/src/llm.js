const anthropicProvider = require('./providers/anthropic');
const ollamaProvider = require('./providers/ollama');

// AI_PROVIDER=ollama (default) -> gratis, corre local. Decidido con el usuario
// (2026-09-07): el modulo "Estudiar con la IA" (clasificar + preguntas
// preescritas) se queda en Ollama con qwen3-vl:2b por velocidad. Las
// resoluciones NO pasan por aca: tienen su apartado propio en la app y usan
// solo OCR (ver routes/hoja2.js).
// AI_PROVIDER=anthropic -> Claude Haiku/Sonnet, requiere ANTHROPIC_API_KEY con
// saldo cargado en console.anthropic.com.
const PROVIDER = (process.env.AI_PROVIDER || 'ollama').toLowerCase();

if (PROVIDER === 'ollama') {
  console.log(
    '[llm] Usando Ollama (gratis/local, qwen3-vl:2b) -- para pasar a Claude, poner ' +
      'AI_PROVIDER=anthropic y ANTHROPIC_API_KEY en server/.env',
  );
} else if (!process.env.ANTHROPIC_API_KEY) {
  console.warn('[llm] ANTHROPIC_API_KEY no esta configurada (ver server/.env.example)');
}

async function complete(args) {
  return PROVIDER === 'ollama' ? ollamaProvider.complete(args) : anthropicProvider.complete(args);
}

// Precarga el modelo (solo aplica a Ollama; con Anthropic no hay nada que
// calentar). No propaga el error -- si falla, el primer pedido real lo hara.
async function warmup() {
  if (PROVIDER !== 'ollama') return { skipped: true };
  return ollamaProvider.warmup();
}

// Estado del proveedor para /health (solo Ollama tiene chequeo util).
async function health() {
  if (PROVIDER !== 'ollama') return { provider: PROVIDER };
  return { provider: PROVIDER, ...(await ollamaProvider.health()) };
}

module.exports = { complete, warmup, health, PROVIDER };
