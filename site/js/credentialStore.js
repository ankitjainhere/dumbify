const DB_NAME  = 'dumbify';
const STORE    = 'Authentication';
const KEY_NAME = `dumbify@${location.hostname}`;

function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () =>
      req.result.createObjectStore(STORE, { autoIncrement: true });
    req.onsuccess = () => resolve(req.result);
    req.onerror   = () => reject(req.error);
  });
}

async function saveKey(pkcs8) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx  = db.transaction(STORE, 'readwrite');
    const req = tx.objectStore(STORE).add(pkcs8);
    req.onsuccess = () => resolve();
    req.onerror   = () => reject(req.error);
    tx.oncomplete = () => db.close();
  });
}

async function loadKeys() {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx  = db.transaction(STORE, 'readonly');
    const req = tx.objectStore(STORE).getAll();
    req.onsuccess = () => resolve(req.result);
    req.onerror   = () => reject(req.error);
    tx.oncomplete = () => db.close();
  });
}

export const credentialStore = {
  async generateKey() {
    const { privateKey } = await crypto.subtle.generateKey(
      {
        name: 'RSASSA-PKCS1-v1_5',
        modulusLength: 2048,
        publicExponent: new Uint8Array([1, 0, 1]),
        hash: 'SHA-1',
      },
      true,
      ['sign', 'verify'],
    );
    const buffer = new Uint8Array(
      await crypto.subtle.exportKey('pkcs8', privateKey),
    );
    await saveKey(buffer);
    return { buffer, name: KEY_NAME };
  },

  async *iterateKeys() {
    for (const key of await loadKeys()) {
      yield { buffer: key, name: KEY_NAME };
    }
  },
};
