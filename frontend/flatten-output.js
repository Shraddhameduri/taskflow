/**
 * Flattens the Angular application-builder output.
 *
 * The builder always emits into <outputPath>/browser/; Spring Boot serves
 * the SPA from the top of classpath:/static, so move everything up one
 * level and drop the now-empty browser/ directory.
 */
const fs = require('fs');
const path = require('path');

const base = path.join(__dirname, '..', 'src', 'main', 'resources', 'static');
const browserDir = path.join(base, 'browser');

if (!fs.existsSync(browserDir)) {
  process.exit(0);
}

for (const entry of fs.readdirSync(browserDir)) {
  fs.renameSync(path.join(browserDir, entry), path.join(base, entry));
}
fs.rmdirSync(browserDir);
console.log('Flattened Angular output into', base);
