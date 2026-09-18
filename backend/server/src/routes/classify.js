const express = require('express');
const llm = require('../llm');
const { extractJson } = require('../util');
const { CATALOG, codes } = require('../catalog');

const router = express.Router();

function buildSystemPrompt() {
  // "pistas" trae las senas reales (titulos, sellos, tablas) que se ven en
  // los ejemplos de entrenamiento -- es lo que de verdad mejora la precision,
  // mas que solo mandar el nombre de la categoria. Ver nota en catalog.js.
  const lista = CATALOG.map((t) => `- ${t.code}: ${t.pistas || t.label}`).join('\n');
  return [
    'Sos un clasificador de documentos para tramites catastrales/de arquitectura de la',
    'Alcaldia de Cochabamba (GAMC). Se te muestran una o mas fotos/paginas escaneadas del',
    'MISMO documento. Tu unica tarea es identificar a cual de estos tipos pertenece, usando',
    'las senas visuales y de texto descritas para cada uno (titulos, sellos, tablas):',
    '',
    lista,
    '',
    'Si el documento no calza claramente con ninguno, respondé "DESCONOCIDO".',
    'Respondé UNICAMENTE con un objeto JSON, sin texto antes ni despues, con esta forma',
    'exacta: {"tipo": "<uno de los codigos de arriba o DESCONOCIDO>", "confianza": <numero',
    'de 0 a 1>, "razon": "<una frase corta explicando por que, en español>"}.',
  ].join('\n');
}

// POST /api/classify { images: string[] } -> { tipo, confianza, razon }
router.post('/', async (req, res) => {
  const { images } = req.body || {};
  if (!Array.isArray(images) || images.length === 0) {
    return res.status(400).json({ error: 'Se espera "images": string[] en base64.' });
  }

  try {
    const text = await llm.complete({
      tier: 'fast',
      system: buildSystemPrompt(),
      images,
      userText: 'Clasifica este documento segun las instrucciones.',
    });

    const parsed = extractJson(text);
    const tipo = codes().includes(parsed.tipo) ? parsed.tipo : 'DESCONOCIDO';
    res.json({ tipo, confianza: parsed.confianza ?? null, razon: parsed.razon ?? '' });
  } catch (err) {
    console.error('[classify] error', err);
    res.status(502).json({ error: 'No se pudo clasificar el documento.', detalle: err.message });
  }
});

module.exports = router;
