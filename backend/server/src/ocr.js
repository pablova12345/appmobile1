const { stripDataUriPrefix } = require('./util');

// Cliente del servicio OCR real de GAMC (ocr.catastrocbba.com, PaddleOCR con
// cola de trabajos). Contrato confirmado 2026-09-04 contra el servicio real Y
// contra el repo de referencia que paso el ingeniero
// (github.com/MateoBazo/proyecto-erp, dominio "geoextraccion"):
//   POST {OCR_API_URL}/ocr/  (multipart, campo "file")  -> { job_id }
//   GET  {OCR_API_URL}/ocr/result/{job_id}/json         -> { status: "queued"|"done"|"failed", result: { result: [...] } }
// Cada item de result.result es un bloque OCR: { points: [[x,y]x4], text, confidence }.

function ocrUrl() {
  return (process.env.OCR_API_URL || 'https://ocr.catastrocbba.com').replace(/\/+$/, '');
}

function guessExtensionAndMime(imageBase64) {
  const match = /^data:(image\/[a-zA-Z+]+);base64,/.exec(imageBase64);
  if (match) {
    const mediaType = match[1];
    return { mediaType, ext: mediaType.split('/')[1] };
  }
  return { mediaType: 'image/jpeg', ext: 'jpg' };
}

// Sube una imagen (base64) al servicio OCR y devuelve el job_id.
async function submitJob(imageBase64, filename = 'pagina.jpg') {
  const { mediaType } = guessExtensionAndMime(imageBase64);
  const buffer = Buffer.from(stripDataUriPrefix(imageBase64), 'base64');

  const formData = new FormData();
  formData.append('file', new Blob([buffer], { type: mediaType }), filename);

  const res = await fetch(`${ocrUrl()}/ocr/`, { method: 'POST', body: formData });
  if (!res.ok) throw new Error(`El servicio OCR respondio con estado ${res.status}`);
  const data = await res.json();
  if (!data.job_id) throw new Error('El servicio OCR no devolvio job_id.');
  return data.job_id;
}

// Espera (con polling) el resultado de un job. Devuelve el arreglo crudo de
// bloques OCR (points/text/confidence).
async function waitForResult(jobId, { maxAttempts = 30, intervalMs = 2000 } = {}) {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const res = await fetch(`${ocrUrl()}/ocr/result/${jobId}/json`);
    if (!res.ok) throw new Error(`El servicio OCR respondio con estado ${res.status}`);
    const data = await res.json();

    if (data.status === 'done') return data.result?.result || [];
    if (data.status === 'failed') throw new Error('El servicio OCR marco el trabajo como fallido.');

    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
  throw new Error('Tiempo agotado esperando el resultado del OCR.');
}

// Sube una imagen y espera su resultado. Conveniencia para el caso comun.
async function ocrImage(imageBase64, filename) {
  const jobId = await submitJob(imageBase64, filename);
  return waitForResult(jobId);
}

module.exports = { submitJob, waitForResult, ocrImage };
