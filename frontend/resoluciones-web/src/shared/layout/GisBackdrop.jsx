/**
 * Fondo fijo de toda la app: gradiente institucional cian + textura sutil.
 * Versión reducida del `GisBackdrop` del proyecto-erp (sin el PNG del skyline,
 * que es un asset propio de ese repo). El equipo del ERP usa su versión al
 * integrar.
 */
const NOISE_SVG = `
<svg xmlns='http://www.w3.org/2000/svg' width='180' height='180'>
  <filter id='n'>
    <feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch' />
  </filter>
  <rect width='100%' height='100%' filter='url(#n)' />
</svg>`
const NOISE_DATA_URI = `data:image/svg+xml;utf8,${encodeURIComponent(NOISE_SVG)}`

export function GisBackdrop() {
  return (
    <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-gradient-to-br from-accent-300 via-accent-400 to-accent-500">
      <div
        className="absolute inset-0 opacity-[0.05] mix-blend-soft-light"
        style={{ backgroundImage: `url("${NOISE_DATA_URI}")`, backgroundSize: '180px 180px' }}
      />
      <div
        className="absolute inset-0 opacity-[0.06]"
        style={{
          backgroundImage:
            'linear-gradient(to right, white 1px, transparent 1px), linear-gradient(to bottom, white 1px, transparent 1px)',
          backgroundSize: '64px 64px',
        }}
      />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_45%,rgba(10,13,18,0.16)_100%)]" />
    </div>
  )
}

export default GisBackdrop
