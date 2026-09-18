const express = require('express');
const path = require('path');
const XLSX = require('xlsx');
const ocr = require('../ocr');
const { buildTable } = require('../tableFromOcr');

const router = express.Router();

const TEMPLATE_PATH = path.join(__dirname, '..', '..', 'templates', 'plantilla-ph.xlsm');

// Primera fila de datos de la tabla "por ambiente" en Hoja2 (columnas C:J).
// Confirmado leyendo el XML crudo de la plantilla real (fila 3 = encabezados,
// fila 10 en adelante = filas de datos). Ver server/README.md para el detalle
// y las columnas que quedan SIN confirmar todavia.
const HOJA2_FIRST_ROW = 10;

// POST /api/hoja2/extract { images: string[] }
// Corre OCR (ocr.catastrocbba.com) sobre cada pagina y reconstruye una tabla
// por posicion (fila/columna), SIN interpretar que significa cada columna --
// eso lo asigna el usuario en la app (ver server/README.md, el texto de
// encabezado que devuelve el OCR en fotos reales sale demasiado distorsionado
// para inferirlo solo). Nada se escribe en el Excel en este paso todavia.
router.post('/extract', async (req, res) => {
  const { images } = req.body || {};
  if (!Array.isArray(images) || images.length === 0) {
    return res.status(400).json({ error: 'Se espera "images": string[] en base64.' });
  }

  try {
    const paginas = [];
    for (let i = 0; i < images.length; i++) {
      const blocks = await ocr.ocrImage(images[i], `pagina_${i + 1}.jpg`);
      paginas.push({ pagina: i + 1, ...buildTable(blocks) });
    }
    res.json({ paginas });
  } catch (err) {
    console.error('[hoja2/extract] error', err);
    res.status(502).json({ error: 'No se pudo extraer la tabla de superficies.', detalle: err.message });
  }
});

// POST /api/hoja2/fill { filas: [...] } (misma forma que devuelve /extract,
// ya revisada/corregida por el usuario) -> { nombre, archivoBase64 }
router.post('/fill', (req, res) => {
  const { filas } = req.body || {};
  if (!Array.isArray(filas) || filas.length === 0) {
    return res.status(400).json({ error: 'Se espera "filas": array no vacio (ver /api/hoja2/extract).' });
  }

  try {
    // bookVBA:true preserva el proyecto VBA de la plantilla sin tocarlo.
    const workbook = XLSX.readFile(TEMPLATE_PATH, { bookVBA: true, cellStyles: true });
    const sheet = workbook.Sheets['Hoja2'];
    if (!sheet) throw new Error('La plantilla no tiene una hoja "Hoja2".');

    filas.forEach((fila, i) => {
      const row = HOJA2_FIRST_ROW + i;
      // A y C son las unicas celdas realmente "de entrada" en este bloque
      // (confirmado leyendo las formulas de la plantilla, no solo los
      // encabezados -- ver nota en server/README.md). F (=D+E, privada
      // total) y J (=D+H, construida total) son FORMULAS que se calculan
      // solas: si les escribimos un valor fijo las rompemos, asi que no se
      // tocan aca aunque el JSON extraido traiga sup_privada_total /
      // sup_construida_total (se ignoran a proposito).
      setCell(sheet, `A${row}`, fila.planta);
      setCell(sheet, `C${row}`, fila.ambiente);
      setCell(sheet, `D${row}`, fila.sup_privada_construida);
      setCell(sheet, `E${row}`, fila.sup_privada_libre);
      setCell(sheet, `G${row}`, fila.sup_ideal);
      setCell(sheet, `H${row}`, fila.sup_comun_construida);
      setCell(sheet, `I${row}`, fila.sup_comun_libre);
    });

    const lastRow = HOJA2_FIRST_ROW + filas.length - 1;
    const currentRef = XLSX.utils.decode_range(sheet['!ref']);
    if (currentRef.e.r < lastRow - 1) {
      currentRef.e.r = lastRow - 1;
      sheet['!ref'] = XLSX.utils.encode_range(currentRef);
    }

    const archivoBase64 = XLSX.write(workbook, { bookType: 'xlsm', bookVBA: true, type: 'base64' });
    res.json({ nombre: `hoja2_llenado_${Date.now()}.xlsm`, archivoBase64 });
  } catch (err) {
    console.error('[hoja2/fill] error', err);
    res.status(500).json({ error: 'No se pudo generar el Excel.', detalle: err.message });
  }
});

function setCell(sheet, ref, value) {
  if (value === null || value === undefined || value === '') return;
  const isNumber = typeof value === 'number';
  sheet[ref] = { t: isNumber ? 'n' : 's', v: value };
}

module.exports = router;
