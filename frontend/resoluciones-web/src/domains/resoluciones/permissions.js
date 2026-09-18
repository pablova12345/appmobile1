/**
 * Permisos internos que el dominio Resoluciones necesitaría en el modelo RBAC
 * del proyecto-erp (`seguridad.permisos`, formato `dominio.recurso.accion`).
 * Hoy el backend standalone NO chequea permisos finos (solo autenticación +
 * dueño por `JWT.sub`). Al integrar, agregar estas filas y proteger los
 * endpoints con `Depends(require_permission(...))`.
 */
export const PERMISOS = {
  VER: 'resoluciones.resoluciones.ver',
  EDITAR_TABLA: 'resoluciones.tabla.editar',
  ELIMINAR: 'resoluciones.resoluciones.eliminar',
}
