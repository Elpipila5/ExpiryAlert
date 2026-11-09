"""Selección de ingredientes para recetas, sin conexión a Firebase ni a la IA.

El inventario tiene cantidad numérica, estado activo y fecha dd/MM/yyyy.
Nunca enviar a la IA artículos archivados, agotados o vencidos.
Una fecha ausente se identifica explícitamente como no verificada.
"""
from datetime import date, datetime
import math


def seleccionar_ingredientes(documentos, hoy=None, max_productos=40):
    hoy = hoy or date.today()
    ingredientes = []

    for original in documentos:
        p = original or {}
        if p.get("activo") is False:
            continue

        nombre = " ".join(str(p.get("nombre") or "").split())[:100]
        if not nombre:
            continue

        try:
            cantidad = float(p.get("cantidad", 0))
        except (TypeError, ValueError, OverflowError):
            continue
        if not math.isfinite(cantidad) or cantidad <= 0:
            continue

        fecha = str(p.get("fechaCaducidad") or "").strip()
        dias = None
        if fecha:
            try:
                vencimiento = datetime.strptime(fecha, "%d/%m/%Y").date()
            except ValueError:
                # Fecha registrada pero inválida: no asumir seguridad.
                continue
            dias = (vencimiento - hoy).days
            if dias < 0:
                continue

        unidad = " ".join(str(p.get("unidad") or "pzas").split())[:20]
        ingredientes.append({
            "nombre": nombre,
            "cantidad": round(cantidad, 3),
            "unidad": unidad,
            "caducidad": fecha or "sin fecha verificada",
            "diasRestantes": dias,
        })

    # Primero los que están próximos a caducar y después los sin fecha.
    ingredientes.sort(key=lambda x: (
        x["diasRestantes"] is None,
        x["diasRestantes"] if x["diasRestantes"] is not None else 99999,
        x["nombre"].casefold()
    ))
    return ingredientes[:max_productos]
