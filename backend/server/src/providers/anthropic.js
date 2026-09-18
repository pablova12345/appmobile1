const Anthropic = require('@anthropic-ai/sdk');
const { stripDataUriPrefix } = require('../util');

const client = new Anthropic();

// Haiku clasifica el tipo de documento y responde las preguntas preescritas
// (tarea simple/barata). Sonnet extrae la tabla de superficies de una
// resolucion para llenar la Hoja2 del Excel (tarea que exige mas precision).
// Ver decision tomada con el usuario el 2026-09-03.
const MODEL_FAST = 'claude-haiku-4-5';
const MODEL_STRONG = 'claude-sonnet-5';

function imageBlocks(images = [], defaultMediaType = 'image/jpeg') {
  return images.map((img) => {
    const match = /^data:(image\/[a-zA-Z+]+);base64,(.*)$/s.exec(img);
    const mediaType = match ? match[1] : defaultMediaType;
    return {
      type: 'image',
      source: { type: 'base64', media_type: mediaType, data: stripDataUriPrefix(img) },
    };
  });
}

// tier: 'fast' (Haiku) | 'strong' (Sonnet). effort solo aplica a 'strong' --
// Haiku 4.5 no soporta output_config.effort (tira error si se lo mandamos).
async function complete({ tier = 'fast', system, images, userText, effort }) {
  const model = tier === 'strong' ? MODEL_STRONG : MODEL_FAST;
  const params = {
    model,
    max_tokens: tier === 'strong' ? 4096 : 512,
    system,
    messages: [
      {
        role: 'user',
        content: [...imageBlocks(images), { type: 'text', text: userText }],
      },
    ],
  };
  if (tier === 'strong' && effort) {
    params.output_config = { effort };
  }

  const message = await client.messages.create(params);
  const block = message.content.find((b) => b.type === 'text');
  return block ? block.text : '';
}

module.exports = { complete, MODEL_FAST, MODEL_STRONG };
