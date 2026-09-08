import {defineConfig} from 'vitest/config';
import {fileURLToPath,URL} from 'node:url';
import ts from 'typescript';
// In-process TypeScript compilation also works where Windows blocks esbuild child processes.
export default defineConfig({
 esbuild:false,
 plugins:[{name:'typescript-in-process',enforce:'pre',transform(code,id){
  if(!id.includes('node_modules')&&/\.[cm]?tsx?$/.test(id.split('?')[0]))return {code:ts.transpileModule(code,{compilerOptions:{jsx:ts.JsxEmit.ReactJSX,target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText,map:null};
 }}],
 resolve:{preserveSymlinks:true,alias:{'@':fileURLToPath(new URL('.',import.meta.url))}},
 test:{environment:'jsdom',setupFiles:['./tests/setup.ts'],pool:'threads',maxWorkers:1,minWorkers:1}
});

