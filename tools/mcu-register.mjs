// Registers the extension-resolving hook for the duration of the process.
import { register } from 'node:module';
import { pathToFileURL } from 'node:url';
register('./mcu-loader.mjs', pathToFileURL('./tools/'));
