"""
Normaliza TODOS los archivos de texto del proyecto a UTF-8 sin BOM.

Detecta y corrige:
  1. Archivos guardados en Latin-1/Windows-1252  -> reconvertidos a UTF-8.
  2. Texto con doble codificación ("Ã¡")          -> reparado a "á".
  3. BOM al inicio                                -> eliminado.

Uso:
    python backend/tools/normalizar_utf8.py            (solo informa)
    python backend/tools/normalizar_utf8.py --aplicar   (escribe cambios)
"""
import pathlib
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[2]
EXTENSIONES = {".java", ".js", ".html", ".css", ".sql", ".md", ".yml", ".yaml",
               ".xml", ".txt", ".properties", ".json", ".bat", ".py"}
EXCLUIR = {"target", "node_modules", ".git", "data", ".mvn", "tools"}

SENALES_DOBLE = ("Ã¡", "Ã©", "Ã­", "Ã³", "Ãº", "Ã±", "Ã‘", "Â¿", "Â¡", "â€")


def archivos():
    for ruta in RAIZ.rglob("*"):
        if not ruta.is_file() or ruta.suffix.lower() not in EXTENSIONES:
            continue
        if any(parte in EXCLUIR for parte in ruta.parts):
            continue
        yield ruta


def decodificar(bytes_archivo: bytes) -> tuple[str, str]:
    """Devuelve (texto, codificacion_detectada)."""
    try:
        return bytes_archivo.decode("utf-8"), "utf-8"
    except UnicodeDecodeError:
        pass
    for codificacion in ("cp1252", "latin-1"):
        try:
            return bytes_archivo.decode(codificacion), codificacion
        except UnicodeDecodeError:
            continue
    return bytes_archivo.decode("utf-8", errors="replace"), "reemplazo"


def reparar_doble(texto: str) -> tuple[str, bool]:
    if not any(s in texto for s in SENALES_DOBLE):
        return texto, False
    try:
        candidato = texto.encode("latin-1").decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return texto, False
    if sum(candidato.count(s) for s in SENALES_DOBLE) < sum(texto.count(s) for s in SENALES_DOBLE):
        return candidato, True
    return texto, False


def procesar(ruta: pathlib.Path, aplicar: bool) -> list[str]:
    original = ruta.read_bytes()
    cambios: list[str] = []

    if original.startswith(b"\xef\xbb\xbf"):
        cambios.append("BOM eliminado")

    texto, codificacion = decodificar(original)
    if codificacion != "utf-8":
        cambios.append(f"convertido de {codificacion} a UTF-8")

    texto, doble = reparar_doble(texto)
    if doble:
        cambios.append("doble codificación reparada")

    if cambios and aplicar:
        ruta.write_text(texto, encoding="utf-8", newline="")

    return cambios


def main() -> None:
    aplicar = "--aplicar" in sys.argv
    print(("CORRIGIENDO" if aplicar else "REVISANDO (--aplicar para corregir)") + f" · {RAIZ}\n")

    afectados = 0
    for ruta in archivos():
        try:
            cambios = procesar(ruta, aplicar)
        except Exception as ex:  # noqa: BLE001
            print(f"  ERROR {ruta.relative_to(RAIZ)}: {ex}")
            continue
        if cambios:
            afectados += 1
            print(f"  {ruta.relative_to(RAIZ)}: {', '.join(cambios)}")

    print(f"\nArchivos afectados: {afectados}")


if __name__ == "__main__":
    main()
