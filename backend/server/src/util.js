// Extrae el primer bloque JSON valido de un texto. Los modelos a veces
// envuelven el JSON en texto o en fences ```json aunque se les pida no
// hacerlo -- esto lo tolera en vez de reventar con JSON.parse directo.
function extractJson(text) {
  const fenced = text.match(/```json\s*([\s\S]*?)```/i) || text.match(/```\s*([\s\S]*?)```/);
  const candidate = fenced ? fenced[1] : text;
  const start = candidate.indexOf('{');
  const startArr = candidate.indexOf('[');
  const useArray = startArr !== -1 && (start === -1 || startArr < start);
  const openChar = useArray ? '[' : '{';
  const closeChar = useArray ? ']' : '}';
  const from = useArray ? startArr : start;
  if (from === -1) throw new Error('La respuesta del modelo no contiene JSON');
  let depth = 0;
  for (let i = from; i < candidate.length; i++) {
    if (candidate[i] === openChar) depth++;
    else if (candidate[i] === closeChar) {
      depth--;
      if (depth === 0) return JSON.parse(candidate.slice(from, i + 1));
    }
  }
  throw new Error('JSON incompleto en la respuesta del modelo');
}

// Quita el prefijo "data:image/...;base64," si viene incluido.
function stripDataUriPrefix(img) {
  const match = /^data:image\/[a-zA-Z+]+;base64,(.*)$/s.exec(img);
  return match ? match[1] : img;
}

module.exports = { extractJson, stripDataUriPrefix };
