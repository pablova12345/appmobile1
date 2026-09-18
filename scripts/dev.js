#!/usr/bin/env node
// Un solo comando ("npm run dev" en la raiz) para levantar las 3 partes del
// submodulo: backend Resoluciones (FastAPI :8080), backend Estudiar con IA
// (Node :4000) y frontend Resoluciones Web (Vite :5173).
//
// Cada una se abre en su propia ventana de terminal en vez de mezclarlas en
// una sola consola, para no mezclar logs y poder cerrar/Ctrl+C cada una por
// separado.

const { spawn } = require("child_process");
const path = require("path");

const root = path.resolve(__dirname, "..");
const isWin = process.platform === "win32";

const apiDir = path.join(root, "backend", "resoluciones");
const venvPython = isWin
  ? path.join(apiDir, ".venv", "Scripts", "python.exe")
  : path.join(apiDir, ".venv", "bin", "python");

const services = [
  {
    title: "Resoluciones API (FastAPI :8080)",
    cwd: apiDir,
    command: `"${venvPython}" -m app.main`,
  },
  {
    title: "Estudiar con IA (Node :4000)",
    cwd: path.join(root, "backend", "server"),
    command: "npm run dev",
  },
  {
    title: "Resoluciones Web (Vite :5173)",
    cwd: path.join(root, "frontend", "resoluciones-web"),
    command: "npm run dev",
  },
];

function openWindows(service) {
  const innerCommand = `cd /d "${service.cwd}" && ${service.command}`;
  spawn("cmd.exe", ["/c", "start", "", "cmd.exe", "/k", innerCommand], {
    cwd: root,
    detached: true,
    stdio: "ignore",
  }).unref();
}

function openPosix(service) {
  // Fallback simple para Mac/Linux: intenta abrir Terminal.app (macOS) o un
  // terminal generico; si no encuentra ninguno, corre el proceso inline.
  const inner = `cd "${service.cwd}" && ${service.command}`;
  if (process.platform === "darwin") {
    spawn("osascript", ["-e", `tell application "Terminal" to do script "${inner.replace(/"/g, '\\"')}"`], {
      detached: true,
      stdio: "ignore",
    }).unref();
    return;
  }
  console.log(`No se pudo abrir una ventana nueva para "${service.title}"; corriendo en esta consola.`);
  spawn(service.command, { cwd: service.cwd, shell: true, stdio: "inherit" });
}

console.log("Abriendo las 3 partes del submodulo, una ventana por servicio...\n");
for (const service of services) {
  console.log(`- ${service.title}`);
  if (isWin) {
    openWindows(service);
  } else {
    openPosix(service);
  }
}

console.log(`
Listo. Se abrieron ventanas nuevas para:
  - Resoluciones API   -> http://localhost:8080/docs
  - Estudiar con IA    -> http://localhost:4000
  - Resoluciones Web   -> http://localhost:5173

Cierra cada ventana (o Ctrl+C dentro de ella) para parar ese servicio.
Nota: Ollama (si "AI_PROVIDER=ollama") corre aparte, no lo levanta este comando.
`);
