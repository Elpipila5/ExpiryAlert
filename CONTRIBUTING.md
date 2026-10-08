# Cómo subir avances a ExpiryAlert

Cada avance se prepara en una rama corta y se revisa mediante un pull request hacia `main`. La guía usa comandos de Git compatibles con PowerShell y una alternativa desde la web de GitHub.

## 1. Acceso y copia del repositorio

El propietario añade a los compañeros desde **Settings > Collaborators > Add people** usando sus usuarios de GitHub verificados. Cada compañero acepta la invitación. Estas invitaciones todavía deben gestionarse por el propietario; no se han enviado desde esta preparación.

Con permiso de escritura, clona una sola vez:

```powershell
git clone https://github.com/Elpipila5/ExpiryAlert.git
cd ExpiryAlert
```

Si todavía no tienes permiso, crea un fork en GitHub, clona tu fork y envía el pull request al repositorio original. Cuando uses un fork, actualiza tu copia desde el botón **Sync fork** antes de crear una nueva rama.

Configura Git con tu propio nombre y el correo verificado en tu cuenta o tu correo privado de GitHub. Cada integrante conserva su identidad en los commits.

## 2. Escoge una tarea

Busca un issue del backlog, acuerda quién lo realizará y escribe un comentario breve indicando que lo tomaste. Añade tareas concretas al issue si la historia es grande. Evita que dos personas modifiquen simultáneamente el mismo archivo sin coordinarse.

Los estados de seguimiento son **Pendiente**, **En proceso**, **En revisión/pruebas** y **Terminado**. Por ahora se registran en cada issue y en el sprint correspondiente; no se ha creado un tablero de GitHub Projects.

## 3. Crea tu rama

Desde el repositorio local, antes de empezar una tarea nueva:

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/pb02-registro-manual
```

Usa nombres como `feature/pb03-ocr`, `fix/validacion-cantidad` o `docs/sprint-02`. El ejemplo `pb02` identifica una historia, no el número del issue.

## 4. Incorpora o modifica los archivos

- El proyecto móvil completo va dentro de `app/`; conserva las carpetas, manifiestos y archivos de dependencias que necesita para ejecutarse.
- Incluye el archivo de bloqueo de dependencias cuando el proyecto lo utilice.
- Las capturas y resultados de pruebas van en `docs/evidencias/`.
- Añade instrucciones reales de instalación en `docs/instalacion.md`.
- Usa datos ficticios en las evidencias. No subas contraseñas, tokens privados, archivos de firma ni compilaciones generadas al árbol de código.

Para incorporar el avance existente, una persona debe subir primero la base compilable y documentar cómo ejecutarla. Los demás crean sus ramas a partir de esa base integrada, así evitan subir copias distintas del mismo proyecto.

## 5. Guarda y sube el avance

Revisa qué archivos cambiaron:

```powershell
git status
git diff
```

Agrega las rutas que realmente modificaste; por ejemplo:

```powershell
git add app/
git add docs/instalacion.md
git diff --cached
git commit -m "feat: incorporar base existente de ExpiryAlert"
git push -u origin feature/pb02-registro-manual
```

Sustituye el nombre de la rama por la tuya. Usa mensajes concretos: `feat: registrar consumo`, `fix: impedir saldo negativo`, `docs: registrar pruebas del sprint 2`.

## 6. Abre un pull request

En GitHub pulsa **Compare & pull request**, verifica que la base sea `main` y completa la plantilla. Explica qué cambió, relaciona el issue y añade pruebas o capturas. Usa `Closes #NUMERO` únicamente si el cambio termina todos los criterios del issue; para avances parciales usa `Refs #NUMERO`.

Otro integrante revisa el cambio. Tras corregir observaciones, integrar y comprobar la Definition of Done, se cierra la tarea. El responsable de integración necesita permiso de escritura.

## Alternativa desde la web

En GitHub selecciona `main`, crea una rama desde el selector de ramas, entra a la carpeta apropiada y usa **Add file > Upload files**. Sube archivos y carpetas, guarda el commit en tu rama y abre un pull request. Para muchos archivos de una aplicación, utiliza Git local para conservar la estructura.

## Si aparecen conflictos

Con tus cambios guardados en un commit, actualiza tu rama desde `main`:

```powershell
git fetch origin
git merge origin/main
```

Resuelve los archivos que Git indique, revisa el resultado con el compañero que los modificó, ejecuta las comprobaciones correspondientes, agrega los archivos resueltos, crea el commit de integración y vuelve a subir tu rama. Evita forzar la subida a `main`.
