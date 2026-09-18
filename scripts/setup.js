#!/usr/bin/env node
// Deja el repo listo para "npm run dev": crea el .venv de backend/resoluciones
// e instala sus deps, npm install en los proyectos Node que no tengan
// node_modules, y copia los .env que falten desde su .env.example.
// Se corre solo (es idempotente) cada vez que se hace `npm run dev`.

const { existsSync, copyFileSync } = require("fs");
const { spawnSync } = require("child_process");
const path = require("path");

const root = path.resolve(__dirname, "..");
const isWin = process.platform === "win32";

function run(command, args, cwd) {
  console.log(`\n> [${path.relative(root, cwd) || "."}] ${command} ${args.join(" ")}`);
  // npm en Windows es un .cmd (npm.cmd), no un .exe: sin shell, spawnSync no
  // lo encuentra aunque este en el PATH (ENOENT). Con shell activo se pasa un
  // solo string ya armado (en vez de command + args) para no arrastrar el
  // aviso de seguridad de Node sobre argumentos sin escapar.
  const needsShell = isWin && command === "npm";
  const result = needsShell
    ? spawnSync([command, ...args].join(" "), { cwd, stdio: "inherit", shell: true })
    : spawnSync(command, args, { cwd, stdio: "inherit" });
  if (result.error) {
    console.error(`No se pudo ejecutar "${command}": ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) {
    console.error(`Fallo: ${command} ${args.join(" ")} (cwd: ${cwd})`);
    process.exit(result.status ?? 1);
  }
}

function ensureEnv(dir, label) {
  const envPath = path.join(dir, ".env");
  const examplePath = path.join(dir, ".env.example");
  if (!existsSync(envPath) && existsSync(examplePath)) {
    copyFileSync(examplePath, envPath);
    console.log(`Creado ${label}/.env desde .env.example -- revisa sus valores.`);
  }
}

// 1) .env compartido en la raiz del repo (backend/resoluciones + frontend/resoluciones-web)
ensureEnv(root, ".");

// 2) backend/resoluciones: venv + requirements
const apiDir = path.join(root, "backend", "resoluciones");
const venvDir = path.join(apiDir, ".venv");
const venvPython = isWin
  ? path.join(venvDir, "Scripts", "python.exe")
  : path.join(venvDir, "bin", "python");

if (!existsSync(venvPython)) {
  run("python", ["-m", "venv", ".venv"], apiDir);
}
run(venvPython, ["-m", "pip", "install", "-q", "-r", "requirements.txt"], apiDir);

// 3) proyectos Node: npm install si falta node_modules
for (const rel of ["backend/server", "frontend/resoluciones-web"]) {
  const dir = path.join(root, rel);
  if (!existsSync(path.join(dir, "node_modules"))) {
    run("npm", ["install"], dir);
  }
}

// 4) .env propio de backend/server
ensureEnv(path.join(root, "backend", "server"), "backend/server");

console.log("\nSetup listo.\n");
