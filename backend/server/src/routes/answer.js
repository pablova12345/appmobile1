const express = require('express');
const llm = require('../llm');
const { getByCode } = require('../catalog');

const router = express.Router();

const SYSTEM_PROMPT = [
  'Sos un asistente que lee documentos escaneados de tramites catastrales/de',
  'arquitectura de la Alcaldia de Cochabamba (GAMC). Se te muestran una o mas',
  'fotos del MISMO documento, ya identificado por tipo, y una pregunta puntual',
  'sobre un dato que deberia estar visible en la imagen. Respondé en español,',
  'en 1 a 3 frases, citando el dato exacto tal como aparece escrito (numeros,',
  'nombres, fechas, codigos). Si el dato no es legible o no esta en la imagen,',
  'decilo explicitamente en vez de inventarlo.',
].join('\n');

// POST /api/answer { images: string[], tipo: string, pregunta: string }
router.post('/', async (req, res) => {
  const { images, tipo, pregunta } = req.body || {};
  if (!Array.isArray(images) || images.length === 0) {
    return res.status(400).json({ error: 'Se espera "images": string[] en base64.' });
  }
  if (!pregunta || typeof pregunta !== 'string') {
    return res.status(400).json({ error: 'Se espera "pregunta": string.' });
  }

  const tipoInfo = getByCode(tipo);
  const contexto = tipoInfo
    ? `El documento fue clasificado como "${tipoInfo.label}" (${tipoInfo.code}).`
    : 'El tipo de documento no fue clasificado.';

  try {
    const respuesta = await llm.complete({
      tier: 'fast',
      system: SYSTEM_PROMPT,
      images,
      userText: `${contexto}\n\nPregunta: ${pregunta}`,
    });

    res.json({ respuesta });
  } catch (err) {
    console.error('[answer] error', err);
    res.status(502).json({ error: 'No se pudo responder la pregunta.', detalle: err.message });
  }
});

module.exports = router;
