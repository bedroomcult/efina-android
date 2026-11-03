// material-color-utilities ships extensionless ESM imports, which Node's ESM
// resolver rejects ("Cannot find module .../dynamic_color"). This loader appends
// .js for relative specifiers that only resolve with an extension.
export async function resolve(specifier, context, next) {
  try {
    return await next(specifier, context);
  } catch (error) {
    if (specifier.startsWith('.') && !specifier.endsWith('.js')) {
      return next(specifier + '.js', context);
    }
    throw error;
  }
}
