const fs = require('fs');
const content = fs.readFileSync('caja.component.html', 'utf-8');
const lines = content.split('\n');
let depth = 0;
lines.forEach((line, i) => {
  const divOpens = (line.match(/<div(\s|>)/g) || []).length;
  const divCloses = (line.match(/<\/div>/g) || []).length;
  depth += divOpens - divCloses;
  if (i >= 50 && i <= 395 && divOpens !== divCloses) {
     console.log("Line " + (i+1) + ": " + depth + " (+" + divOpens + " -" + divCloses + ")");
  }
});
console.log("Final depth: " + depth);
