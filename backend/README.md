# ExpiryAlert — recetas IA (Etapa 2)

Esta implementación adapta el ZIP y la guía del compañero. El backend verifica un token de Firebase y usa el UID del token; **nunca** confía en un UID enviado por Android.

**Estado:** código preparado, pero no hay servidor HTTPS desplegado ni clave de IA configurada. La IA puede generar costos.

## Prueba en Windows PowerShell

Requiere Python 3.10+, Firebase del proyecto del equipo y una clave privada del proveedor IA.

1. Abrir PowerShell en la carpeta `backend`.
2. `py -m venv .venv`
3. `.\.venv\Scripts\Activate.ps1`
4. `pip install -r requirements.txt`
5. Configurar variables en esa terminal, sin guardarlas en GitHub:
   - `$env:OPENAI_API_KEY="CLAVE_REAL_SOLO_EN_EL_BACKEND"`
   - `$env:OPENAI_MODEL="gpt-4o-mini"`
   - `$env:GOOGLE_APPLICATION_CREDENTIALS="C:\ruta\privada\firebase-service-account.json"`
6. `uvicorn main:app --host 127.0.0.1 --port 8000`
7. Abrir `http://127.0.0.1:8000/salud`. Debe devolver `{"estado":"ok"}`.
8. Ejecutar `python -m unittest test_seleccion.py`.

El endpoint `POST /recetas/ia` requiere `Authorization: Bearer <Firebase ID token>`, no acepta el UID del teléfono. Sin token devuelve 401, sin inventario apto 400, sin configuración IA 503 y solicitudes seguidas 429. La clave se mantiene en el backend.

## Configurar Android

`localhost` de la PC no permite conectar el teléfono a un servidor HTTPS remoto. Para usar IA se necesita desplegar el servicio bajo HTTPS real, con secretos seguros.

En el archivo **personal y privado** `~/.gradle/gradle.properties` agregar:

`RECETAS_API_BASE_URL=https://tu-servidor-https-real.example.com`

Este valor es **solo la URL pública**. No colocar la clave de IA en Android, Gradle, GitHub ni en el APK. Sin URL, el botón de IA aparece desactivado y siguen funcionando las recetas locales/YouTube.

## Seguridad y operación

- La identidad se obtiene verificando el token Firebase; la lectura de Firestore filtra por el UID del token.
- Se envían a IA solo nombre, cantidad/unidad y caducidad de hasta 40 artículos. Se excluyen vencidos, archivados y agotados. Los artículos sin fecha se señalan como no verificados.
- Cuenta de servicio: permisos mínimos para Firestore y Firebase Auth; usar variables secretas o identidad de servicio gestionada.
- La cuenta de servicio puede eludir reglas de seguridad de clientes: la comprobación del token es imprescindible.
- Se usa JSON estructurado y validación Pydantic. La IA no garantiza la seguridad alimentaria.
- El límite de 45 segundos es solo por instancia; antes de publicar para muchos usuarios agregar controles distribuidos, prevención de abuso, presupuesto/cuota de IA y monitoreo sin datos personales.
- `GET /salud` puede devolver OK sin que esté configurada la clave de IA.
- En producción se necesita HTTPS con certificado válido y no se deben deshabilitar verificaciones TLS.

## Procedencia e integración

Se conservan los endpoints y el contrato de respuesta del ZIP de Etapa 2: `GET /salud`, `POST /recetas/ia`, `nombre`, `tiempoMinutos`, `porciones`, `ingredientes`, `faltantes`, `pasos`, `advertencia`.

Se añadieron filtros por cantidad y estado, controles de costo/errores, un cliente Android configurable y una pantalla F10 de recetas locales con YouTube. No se reemplazaron pantallas del compañero: en la versión actual de `main` no había `RecetasScreen.kt`.
