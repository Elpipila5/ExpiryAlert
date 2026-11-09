from datetime import date
import unittest

from seleccion import seleccionar_ingredientes


class SeleccionTest(unittest.TestCase):
    def setUp(self):
        self.hoy = date(2026, 11, 9)

    def test_excluye_caducados_ubicaciones_inactivas_y_cero(self):
        docs = [
            {"nombre": "Leche vencida", "cantidad": 1, "fechaCaducidad": "08/11/2026"},
            {"nombre": "Arroz", "cantidad": 1, "activo": False, "fechaCaducidad": "30/11/2026"},
            {"nombre": "Queso", "cantidad": 0, "fechaCaducidad": "30/11/2026"},
            {"nombre": "Tomate", "cantidad": 2, "fechaCaducidad": "10/11/2026"},
        ]
        datos = seleccionar_ingredientes(docs, self.hoy)
        self.assertEqual([p["nombre"] for p in datos], ["Tomate"])

    def test_prioriza_proximos_y_marca_fecha_pendiente(self):
        docs = [
            {"nombre": "Aceite", "cantidad": 1, "fechaCaducidad": ""},
            {"nombre": "Avena", "cantidad": 1, "fechaCaducidad": "30/11/2026"},
            {"nombre": "Leche", "cantidad": 1, "fechaCaducidad": "09/11/2026"},
        ]
        datos = seleccionar_ingredientes(docs, self.hoy)
        self.assertEqual([p["nombre"] for p in datos], ["Leche", "Avena", "Aceite"])
        self.assertEqual(datos[2]["caducidad"], "sin fecha verificada")

    def test_fecha_invalida_se_excluye(self):
        docs = [{"nombre": "Yogur", "cantidad": 3, "fechaCaducidad": "31/02/2026"}]
        self.assertEqual([], seleccionar_ingredientes(docs, self.hoy))

    def test_limita_y_recorta_texto(self):
        docs = [{"nombre": "a" * 200, "cantidad": 1, "fechaCaducidad": "11/11/2026"}]
        datos = seleccionar_ingredientes(docs, self.hoy, max_productos=1)
        self.assertEqual(len(datos), 1)
        self.assertEqual(len(datos[0]["nombre"]), 100)


if __name__ == "__main__":
    unittest.main()
