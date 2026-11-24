import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, deleteField, collection, query, where, getDocs, Timestamp } from 'firebase/firestore';

const projectId = 'demo-expiryalert';
let entorno;
let propietario;
let otraCuenta;
let anonimo;
const ruta = 'productos/lote-leche';
const producto = () => ({ usuarioId: 'cuenta-a', nombre: 'Leche entera', categoria: 'Lácteos',
  marca: 'Marca de prueba', cantidad: 1000, cantidadInicial: 1000, unidad: 'ml',
  fechaCaducidad: '25/11/2026', caducidadPendiente: false, activo: true,
  fechaCreacion: Timestamp.fromMillis(1000000), ultimaConfirmacion: Timestamp.fromMillis(1000000) });
// El servidor valida esquema y tamaño; la decodificación real se comprueba en Android.
const foto = () => ({ contenido: '/9j/YWJjZA==', fuente: 'galeria', pagina: '' });

before(async () => {
  assert.match(process.env.FIRESTORE_EMULATOR_HOST ?? '', /^(127\.0\.0\.1|localhost):\d+$/, 'Estas pruebas solo pueden ejecutarse contra el emulador local.');
  entorno = await initializeTestEnvironment({ projectId, firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') } });
  propietario = entorno.authenticatedContext('cuenta-a').firestore();
  otraCuenta = entorno.authenticatedContext('cuenta-b').firestore();
  anonimo = entorno.unauthenticatedContext().firestore();
});
beforeEach(async () => {
  await entorno.clearFirestore();
  await entorno.withSecurityRulesDisabled(async contexto => setDoc(doc(contexto.firestore(), ruta), producto()));
});
after(async () => { await entorno?.cleanup(); });

test('el propietario crea y consulta su producto sin foto', async () => {
  await assertSucceeds(setDoc(doc(propietario, 'productos/nuevo'), producto()));
  assert.equal((await assertSucceeds(getDoc(doc(propietario, ruta)))).data().nombre, 'Leche entera');
});
test('consulta filtrada por usuario permitida y consulta global denegada', async () => {
  await assertSucceeds(getDocs(query(collection(propietario, 'productos'), where('usuarioId', '==', 'cuenta-a'))));
  await assertFails(getDocs(collection(propietario, 'productos')));
});
test('sin sesión no se puede leer ni crear', async () => {
  await assertFails(getDoc(doc(anonimo, ruta)));
  await assertFails(setDoc(doc(anonimo, 'productos/nuevo'), producto()));
});
test('otra cuenta no puede leer, cambiar ni borrar el documento', async () => {
  await assertFails(getDoc(doc(otraCuenta, ruta)));
  await assertFails(updateDoc(doc(otraCuenta, ruta), { cantidad: 500 }));
  await assertFails(deleteDoc(doc(otraCuenta, ruta)));
});
test('no se puede crear en nombre de otra cuenta ni transferir propietario', async () => {
  await assertFails(setDoc(doc(otraCuenta, 'productos/suplantado'), producto()));
  await assertFails(updateDoc(doc(propietario, ruta), { usuarioId: 'cuenta-b' }));
});
test('fecha de creación inmutable y campos desconocidos rechazados', async () => {
  await assertFails(updateDoc(doc(propietario, ruta), { fechaCreacion: Timestamp.now() }));
  await assertFails(updateDoc(doc(propietario, ruta), { administrador: true }));
});
test('consumo, confirmación y archivo válidos', async () => {
  await assertSucceeds(updateDoc(doc(propietario, ruta), { cantidad: 750, ultimaConfirmacion: Timestamp.now() }));
  await assertSucceeds(updateDoc(doc(propietario, ruta), { cantidad: 0, activo: false, ultimaConfirmacion: Timestamp.now() }));
});
test('rechaza cantidades negativas, excesivas y creación agotada', async () => {
  for (const cantidad of [-1, 1000001, 'diez']) await assertFails(updateDoc(doc(propietario, ruta), { cantidad }));
  await assertFails(setDoc(doc(propietario, 'productos/agotar'), { ...producto(), cantidad: 0 }));
});
test('permite caducidad pendiente y rechaza formato inválido', async () => {
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fechaCaducidad: '', caducidadPendiente: true }));
  await assertFails(updateDoc(doc(propietario, ruta), { fechaCaducidad: 'mañana' }));
});
test('guarda, recupera, cambia y quita la foto sin cambiar el producto', async () => {
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fotoProducto: foto() }));
  assert.deepEqual((await getDoc(doc(propietario, ruta))).data().fotoProducto, foto());
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fotoProducto: { ...foto(), fuente: 'openfoodfacts', pagina: 'https://world.openfoodfacts.org/product/12345678' } }));
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fotoProducto: deleteField() }));
  assert.equal((await getDoc(doc(propietario, ruta))).data().fotoProducto, undefined);
});
test('la foto tiene el mismo aislamiento que el producto', async () => {
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fotoProducto: foto() }));
  await assertFails(getDoc(doc(otraCuenta, ruta)));
  await assertFails(updateDoc(doc(otraCuenta, ruta), { fotoProducto: foto() }));
});
test('rechaza fotos excesivas, malformadas o con campos inesperados', async () => {
  for (const invalida of [
    { ...foto(), contenido: '/9j/' + 'a'.repeat(120001) },
    { ...foto(), contenido: 'archivo cualquiera' },
    { ...foto(), fuente: 'desconocida' },
    { ...foto(), pagina: 'http://example.com/foto.jpg' },
    { ...foto(), extra: true },
    { contenido: '/9j/YWJjZA==' }, null, 'foto.jpg'
  ]) await assertFails(updateDoc(doc(propietario, ruta), { fotoProducto: invalida }));
});
test('conserva productos antiguos con campos opcionales ausentes', async () => {
  await assertSucceeds(setDoc(doc(propietario, 'productos/antiguo'), {
    usuarioId: 'cuenta-a', nombre: 'Arroz', categoria: 'Alimentos', cantidad: 1, fechaCaducidad: ''
  }));
});
test('borra el producto y su foto con la autorización del propietario', async () => {
  await assertSucceeds(updateDoc(doc(propietario, ruta), { fotoProducto: foto() }));
  await assertSucceeds(deleteDoc(doc(propietario, ruta)));
});
