"""
Autenticacion para correr este modulo SOLO (standalone).

En el proyecto-erp la autenticacion la provee el dominio `seguridad`
(`/api/login`, `/api/refresh`, y la dependencia `get_current_user`). Este
paquete replica lo minimo para poder desarrollar/probar el modulo sin el resto
del ERP. Al integrar: borrar este paquete, quitar su router de `registry.py` y
repuntar los imports de `app.auth.deps` -> `app.domains.seguridad.presentation.deps`.
"""
