from app.core.errors.exceptions import NotFoundException, ValidationException


class ResolucionNoEncontrada(NotFoundException):
    def __init__(self, id_resolucion: str):
        super().__init__(f"No existe una resolucion con id '{id_resolucion}' para este usuario.")


class ResolucionInvalida(ValidationException):
    pass
