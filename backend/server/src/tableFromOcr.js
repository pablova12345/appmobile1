// Reconstruye una tabla (filas/columnas) a partir de los bloques crudos que
// devuelve el servicio OCR, usando solo la POSICION de cada bloque (nunca el
// contenido de texto) -- adaptado de processAndFilterOCRData() en el repo de
// referencia geo-extract/proyecto-erp (dominio "geoextraccion"), generalizado
// de 2 columnas (Este/Norte) a N columnas.
//
// A proposito NO intenta adivinar que significa cada columna (esa parte del
// original dependia de un truco especifico de coordenadas UTM que no aplica
// aca). En su lugar, esto solo agrupa por fila/columna segun posicion y deja
// que el usuario le asigne un rol a cada columna en la app -- ver nota en
// server/README.md sobre por que (el texto de encabezado que devuelve el OCR
// en fotos reales sale bastante distorsionado, no es confiable para inferir
// columnas automaticamente).

function yCenter(points) {
  // points: [[x,y] x4] en orden top-left, top-right, bottom-right, bottom-left
  return (points[0][1] + points[2][1]) / 2;
}

function xStart(points) {
  return Math.min(points[0][0], points[3][0]);
}

function xEnd(points) {
  return Math.max(points[1][0], points[2][0]);
}

// rowGapY: dentro de una misma fila real, dos celdas casi siempre difieren
// unos pocos px en Y (distinta linea base entre numeros y texto); entre una
// fila y la siguiente el salto es notoriamente mayor. Por eso agrupa por
// SALTO respecto al bloque anterior (ordenado por Y), no por distancia a un
// ancla fija -- una sola celda corrida 4-5px ya no rompe la fila. Medido
// contra una tabla real: salto dentro de fila ~0-2.5px, entre filas ~4-7px.
// mergeGapX: bloques de la misma fila separados por menos que esto en X se
// pegan en una sola celda (para palabras/numeros que el OCR partio en dos).
function buildTable(blocks, { rowGapY = 3.5, mergeGapX = 18, columnGapX = 45 } = {}) {
  const withPos = blocks
    .map((block) => ({
      text: (block.text || '').trim(),
      confidence: block.confidence ?? 1,
      y: yCenter(block.points),
      xStart: xStart(block.points),
      xEnd: xEnd(block.points),
    }))
    .filter((b) => b.text)
    .sort((a, b) => a.y - b.y);

  const rows = [];
  withPos.forEach((item) => {
    const last = rows[rows.length - 1];
    if (!last || item.y - last.lastY > rowGapY) {
      rows.push({ y: item.y, lastY: item.y, items: [item] });
    } else {
      last.items.push(item);
      last.lastY = item.y;
      last.y = (last.y * (last.items.length - 1) + item.y) / last.items.length;
    }
  });

  // Pegar bloques muy cercanos en la misma fila (palabras/numeros partidos).
  rows.forEach((row) => {
    row.items.sort((a, b) => a.xStart - b.xStart);
    const merged = [];
    row.items.forEach((item) => {
      const prev = merged[merged.length - 1];
      if (prev && item.xStart - prev.xEnd < mergeGapX) {
        prev.text = `${prev.text} ${item.text}`.trim();
        prev.xEnd = Math.max(prev.xEnd, item.xEnd);
        prev.confidence = Math.min(prev.confidence, item.confidence);
      } else {
        merged.push({ ...item });
      }
    });
    row.items = merged;
  });

  // Anclas de columna: agrupar todas las posiciones X de inicio de celda.
  const allXStarts = [];
  rows.forEach((row) => row.items.forEach((item) => allXStarts.push(item.xStart)));
  allXStarts.sort((a, b) => a - b);

  const columnAnchors = [];
  allXStarts.forEach((x) => {
    if (columnAnchors.length === 0 || x - columnAnchors[columnAnchors.length - 1] > columnGapX) {
      columnAnchors.push(x);
    }
  });

  // Alinear cada fila a las columnas detectadas (celda vacia si no hay nada).
  rows.forEach((row) => {
    const aligned = columnAnchors.map(() => ({ text: '', confidence: 1 }));
    row.items.forEach((item) => {
      let closest = 0;
      let minDiff = Math.abs(item.xStart - columnAnchors[0]);
      for (let i = 1; i < columnAnchors.length; i++) {
        const diff = Math.abs(item.xStart - columnAnchors[i]);
        if (diff < minDiff) {
          minDiff = diff;
          closest = i;
        }
      }
      // Si dos bloques caen en la misma columna (fila mal agrupada), se
      // concatenan en vez de pisarse -- mejor visible-y-corregible que
      // perder el dato en silencio.
      if (aligned[closest].text) {
        aligned[closest].text += ` ${item.text}`;
        aligned[closest].confidence = Math.min(aligned[closest].confidence, item.confidence);
      } else {
        aligned[closest] = { text: item.text, confidence: item.confidence };
      }
    });
    row.cells = aligned;
  });

  rows.sort((a, b) => a.y - b.y);

  return {
    columnCount: columnAnchors.length,
    rows: rows.map((row, i) => ({ id: `f-${i}`, cells: row.cells })),
  };
}

module.exports = { buildTable };
