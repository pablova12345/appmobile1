// Catalogo unico de tipos de documento + preguntas preescritas.
// Fuente de verdad para: (1) el prompt de clasificacion (via el campo
// "pistas" -- senas visuales/de texto reales de cada tipo), (2) las
// preguntas que la app muestra una vez clasificado el documento. La app las
// obtiene via GET /api/catalog en vez de mantener una copia propia, para que
// este archivo sea el unico lugar donde se edita el catalogo.
//
// Basado en los ejemplos reales de "entrenamiento" revisados en la carpeta
// que paso el ingeniero (2026-09-03). "RESOLUCION_ADM" se consolido dentro de
// "RESOLUCION_ADMINISTRATIVA" (eran la misma categoria duplicada), y
// "NO SE BIEN QUE ES" se renombro a "SENTENCIA" (el ejemplo era una sentencia
// judicial de nulidad de transferencia). Pendiente de confirmar ambos cambios
// con el ingeniero.
//
// IMPORTANTE sobre "pistas": es el texto que usa buildSystemPrompt() en
// routes/classify.js para describir cada tipo al modelo. Esto es lo que de
// verdad afecta la precision de clasificacion en produccion (con Ollama o con
// Claude), no el contenido de server/ollama/Modelfile -- el "system" que
// manda el backend en cada pedido pisa al SYSTEM del Modelfile. Ver
// server/ollama/README.md.

const CATALOG = [
  {
    code: 'CEDULA',
    label: 'Cedula de identidad',
    group: 'Identidad',
    pistas:
      'Cedula de identidad boliviana. Foto del titular, huella dactilar, codigo QR, ' +
      'texto "ESTADO PLURINACIONAL DE BOLIVIA" / "CEDULA DE IDENTIDAD", numero de serie ' +
      'y seccion. A veces viene con un certificado de "SERVICIO GENERAL DE ' +
      'IDENTIFICACION PERSONAL... CERTIFICA".',
    preguntas: [
      '¿Quien es el titular del documento?',
      '¿Cual es su numero de cedula de identidad?',
      '¿Cual es su fecha y lugar de nacimiento?',
    ],
  },
  {
    code: 'CERTIFICACION_CATASTRAL',
    label: 'Certificacion catastral',
    group: 'Catastro y registro',
    pistas:
      'Titulo grande "CERTIFICACION CATASTRAL", subtitulo "EXISTENCIA DEL REGISTRO ' +
      'CATASTRAL CON CODIGO", un codigo catastral en formato NN-NNN-NNN-N-NN-NNN-NNN, ' +
      'un croquis circular de manzana con flecha de norte, sello "GOBIERNO AUTONOMO ' +
      'MUNICIPAL DE COCHABAMBA".',
    preguntas: [
      '¿Cual es el codigo catastral?',
      '¿Quien es el propietario actual?',
      '¿Cuales son distrito, subdistrito, zona y manzana?',
      '¿Cual es la superficie del predio?',
    ],
  },
  {
    code: 'REGISTRO_CATASTRAL',
    label: 'Registro catastral',
    group: 'Catastro y registro',
    pistas:
      'Titulo "REGISTRO CATASTRAL", campos "Nombre Edificio:", "Bloque:", "Planta:", ' +
      '"Unidad:", tabla de "PROPIETARIOS ACTUALES", firmas de "Jefe Servicios ' +
      'Catastrales" / "Director de Administracion Geografica y Catastro".',
    preguntas: [
      '¿Cual es el codigo catastral?',
      '¿Cual es el edificio y la unidad (parqueo, depto.)?',
      '¿Quienes son los propietarios actuales?',
      '¿Cual es la superficie privada, comun e ideal?',
    ],
  },
  {
    code: 'FOLIO_REAL',
    label: 'Folio real',
    group: 'Catastro y registro',
    pistas:
      'Encabezado "ORGANO JUDICIAL DEL ESTADO PLURINACIONAL DE BOLIVIA" + "REGISTRO ' +
      'DE LA PROPIEDAD INMUEBLE", "MATRICULA N°", codigo de barras, tabla con columnas ' +
      '"TITULARIDAD SOBRE EL DOMINIO / GRAVAMENES Y RESTRICCIONES / CANCELACIONES".',
    preguntas: [
      '¿Cual es el numero de matricula?',
      '¿Cual es la ubicacion y superficie?',
      '¿Quien es el titular segun el ultimo asiento?',
      '¿Cuales son los linderos (N/S/E/O)?',
    ],
  },
  {
    code: 'RUAT',
    label: 'Proforma RUAT',
    group: 'Catastro y registro',
    pistas:
      'Titulo "RUAT - INMUEBLES" o "PROFORMA RESUMIDA DE INMUEBLES", seccion "DETALLE ' +
      'DE DEUDAS", menciones a "IMPBI" (Impuesto Municipal a la Propiedad).',
    preguntas: [
      '¿Cual es el numero de inmueble y el codigo catastral?',
      '¿Quien es el contribuyente?',
      '¿Cual es la deuda total a pagar?',
    ],
  },
  {
    code: 'COMPROBANTE_DE_PAGO',
    label: 'Comprobante de pago',
    group: 'Tramite y pago',
    pistas:
      'Titulo "FUR - COMPROBANTE DE PAGO" o "COMPROBANTE DE PAGO - FORMULARIO UNICO DE ' +
      'RECAUDACION", numero de folio, entidad recaudadora (banco), monto pagado, sello ' +
      'de cajero, codigo QR de RUAT.',
    preguntas: [
      '¿Cual es el numero de comprobante / FUR?',
      '¿Quien es el contribuyente?',
      '¿Cual es el monto pagado?',
      '¿Cual es el codigo catastral o N de inmueble asociado?',
    ],
  },
  {
    code: 'FUR',
    label: 'Formulario unico de recaudacion',
    group: 'Tramite y pago',
    pistas:
      'Formulario "FORMULARIO UNICO DE RECAUDACION" con "N° FUR", "CODIGO ADMISION", ' +
      'detalle de cuenta/monto, sin ser necesariamente un comprobante ya cobrado.',
    preguntas: [
      '¿A que tramite corresponde este FUR?',
      '¿A nombre de quien esta?',
      '¿Cual es el monto a pagar?',
    ],
  },
  {
    code: 'HOJA_DE_RUTA',
    label: 'Hoja de ruta',
    group: 'Tramite y pago',
    pistas:
      'Titulo "HOJA DE RUTA", tabla con columnas "UNIDAD ORIGEN / UNIDAD DESTINO / ' +
      'FECHA EMISION / FECHA RECEPCION / INSTRUCCION (MOTIVO)", texto "PRIORIDAD: ' +
      'NORMAL/URGENTE".',
    preguntas: [
      '¿Que tramite es (referencia / motivo)?',
      '¿En que unidad se encuentra actualmente?',
      '¿Cual fue la ultima instruccion registrada?',
    ],
  },
  {
    code: 'MEMORIAL_ALCALDE',
    label: 'Memorial al alcalde',
    group: 'Tramite y pago',
    pistas:
      'Encabezado "SEÑOR ALCALDE DEL GOBIERNO AUTONOMO MUNICIPAL DE COCHABAMBA", texto ' +
      'en primera persona "expongo y solicito", firma de abogado con numero de ' +
      'matricula.',
    preguntas: [
      '¿Quien presenta la solicitud y cual es su CI?',
      '¿Que esta solicitando?',
      '¿Que datos del inmueble menciona?',
    ],
  },
  {
    code: 'INFORME',
    label: 'Informe tecnico o legal',
    group: 'Tramite y pago',
    pistas:
      'Encabezado "INFORME", campos "A:", "De:", "Ref.:", "Fecha:", cuerpo con ' +
      'conclusion, firma y sello institucional.',
    preguntas: [
      '¿De quien a quien va dirigido?',
      '¿Cual es la referencia / asunto?',
      '¿Cual es la conclusion del informe?',
    ],
  },
  {
    code: 'DOCUMENTO_PRIVADO',
    label: 'Documento privado',
    group: 'Legal y notarial',
    pistas:
      'Contrato privado (no notariado), clausulas numeradas "PRIMERA:", "SEGUNDA:", ' +
      'etc., menciona compra/venta de un lote, nombres de las partes, sin sellos ' +
      'institucionales.',
    preguntas: [
      '¿Quienes son las partes (vendedor y comprador)?',
      '¿Cual es la ubicacion y superficie del lote?',
      '¿Cual es el monto de la venta?',
    ],
  },
  {
    code: 'TESTIMONIO_NOTARIAL',
    label: 'Testimonio notarial',
    group: 'Legal y notarial',
    pistas:
      '"CARATULA NOTARIAL" o "FORMULARIO NOTARIAL", "TESTIMONIO N°", sello de ' +
      '"NOTARIA DE FE PUBLICA", montos en Bolivianos.',
    preguntas: [
      '¿Cual es el numero de testimonio y quien es el notario?',
      '¿Que acto notarial se protocoliza?',
      '¿Quienes son las partes involucradas?',
    ],
  },
  {
    code: 'SENTENCIA',
    label: 'Sentencia judicial',
    group: 'Legal y notarial',
    pistas:
      'Encabezado "ORGANO JUDICIAL DE BOLIVIA", "SENTENCIA N°", "TRIBUNAL ' +
      'DEPARTAMENTAL DE JUSTICIA", "JUZGADO...", campos "PROCESO:", "DEMANDANTES:", ' +
      '"DEMANDADOS:".',
    preguntas: [
      '¿Cual es el numero de sentencia y el juzgado?',
      '¿Quienes son demandante(s) y demandado(s)?',
      '¿Que resuelve la sentencia?',
    ],
    nota: 'Antes "NO SE BIEN QUE ES" en la carpeta de entrenamiento. Confirmar el renombre con el ingeniero.',
  },
  {
    code: 'PLANO_LOTE_GEOREFERENCIADO',
    label: 'Plano de lote georeferenciado',
    group: 'Planos',
    pistas:
      'Titulo "PLANO DE LOTE - GEOREFERENCIADO", tabla "COORDENADAS UTM-WGS-84" con ' +
      'puntos P1..P4, croquis de ubicacion, tabla "BLOQUE / SUP. m2 / TIPOLOGIA / AÑO ' +
      'CONSTRUCCION", sello del Colegio de Arquitectos.',
    preguntas: [
      '¿Cuales son las coordenadas UTM de cada punto?',
      '¿Quien es el propietario o poseedor?',
      '¿Cual es la superficie total util?',
      '¿Cuales son zona, distrito, subdistrito, manzano y calle?',
    ],
  },
  {
    code: 'PLANO_LOTE_SN_ADM',
    label: 'Plano de lote sin aprobacion previa',
    group: 'Planos',
    pistas:
      'Titulo "REGULARIZACION PLANO DE LOTE SIN APROBACION PREVIA", similar al ' +
      'georeferenciado pero antes de tener aprobacion municipal formal.',
    preguntas: [
      '¿Cuales son las coordenadas del perimetro?',
      '¿Quien es el poseedor?',
      '¿Superficie segun documento privado vs. segun mensura?',
    ],
  },
  {
    code: 'REGULARIZACION_DE_LOTE',
    label: 'Regularizacion de lote',
    group: 'Planos',
    pistas:
      'Titulo "PLANO DE: REGULARIZACION DE LOTE", tabla "COORDENADAS PERIMETRO", sello ' +
      '"APROBADO", datos de "POSEEDOR(ES)".',
    preguntas: [
      '¿Cuales son las coordenadas del perimetro?',
      '¿Quien es el propietario?',
      '¿Cual es la superficie total util?',
      '¿Hay superficie afectada no regularizada?',
    ],
  },
  {
    code: 'PLANO_DE_LOTE_PH',
    label: 'Plano de propiedad horizontal',
    group: 'Planos',
    pistas:
      'Plano arquitectonico de una planta de edificio (ambientes/oficinas/ ' +
      'departamentos rotulados, cotas en metros), sello de aprobacion del Servicio de ' +
      'Desarrollo Urbano, indica el nombre de la planta (ej. "PLANTA PRIMER PISO").',
    preguntas: [
      '¿Cual es el nombre del edificio o proyecto?',
      '¿Que planta representa este plano?',
      '¿Que ambientes se identifican?',
    ],
  },
  {
    code: 'RESOLUCION_TECNICA',
    label: 'Resolucion tecnico administrativa',
    group: 'Resoluciones',
    pistas: 'Titulo "RESOLUCION TECNICO ADMINISTRATIVA N°", estructura "VISTOS / CONSIDERANDO / SE RESUELVE".',
    preguntas: [
      '¿Cual es el numero de resolucion y la fecha?',
      '¿A nombre de quien se aprueba?',
      '¿Que se resuelve?',
    ],
    // Marca que este tipo puede traer, en paginas siguientes, la tabla
    // "RELACION DE SUPERFICIE" por planta. La app YA NO extrae esa tabla desde
    // la pantalla "Estudiar con la IA": solo muestra un aviso que remite al
    // apartado "Resoluciones" (ResolucionesScreen), que corre el OCR y arma la
    // Hoja2 sin pasar por el clasificador (ver routes/hoja2.js).
    puedeTraerTablaSuperficie: true,
  },
  {
    code: 'RESOLUCION_ADMINISTRATIVA',
    label: 'Resolucion administrativa municipal',
    group: 'Resoluciones',
    pistas: 'Titulo "RESOLUCION ADMINISTRATIVA MUNICIPAL N°", misma estructura "VISTOS / CONSIDERANDO / (POR TANTO) SE RESUELVE".',
    preguntas: [
      '¿Cual es el numero de resolucion y la fecha?',
      '¿A nombre de quien se aprueba?',
      '¿Que se resuelve?',
    ],
    // Ver nota en RESOLUCION_TECNICA: la extraccion de la tabla vive ahora en
    // el apartado "Resoluciones" de la app, no en "Estudiar con la IA".
    puedeTraerTablaSuperficie: true,
    nota: 'Se consolido con "RESOLUCION_ADM" (misma categoria duplicada en la carpeta de entrenamiento). Confirmar con el ingeniero.',
  },
];

function getByCode(code) {
  return CATALOG.find((t) => t.code === code) || null;
}

function codes() {
  return CATALOG.map((t) => t.code);
}

module.exports = { CATALOG, getByCode, codes };
