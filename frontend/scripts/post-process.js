const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../src/app/api/generated/models.ts');

fs.readFile(filePath, 'utf8', (err, data) => {
  if (err) {
    console.error('Error reading file:', err);
    return;
  }

  const updatedContent = data.replace(/export {/g, 'export type {');

  fs.writeFile(filePath, updatedContent, 'utf8', (err) => {
    if (err) {
      console.error('Error writing file:', err);
      return;
    }
    console.log('Successfully updated exports to include type keyword');
  });
});
