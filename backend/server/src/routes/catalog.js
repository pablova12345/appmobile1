const express = require('express');
const { CATALOG } = require('../catalog');

const router = express.Router();

// GET /api/catalog -- la app lo consulta al abrir la pantalla de IA, asi el
// catalogo de tipos/preguntas vive en un solo lugar (este backend).
router.get('/', (req, res) => {
  res.json({ tipos: CATALOG });
});

module.exports = router;
