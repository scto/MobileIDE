/**
 * Bun entrypoint for prettier cli
 */

import { realpathSync, lstatSync } from "fs";
import "process";
import * as cli from "./node_modules/prettier/internal/legacy-cli.mjs";

const resolvedArgv = process.argv.slice(2).map((arg) => {
  // Propagate flags without modifications
  if (arg.startsWith("-")) {
    return arg;
  }

  // If the arg is a file to lint, dereference symlinks if necessary
  const stats = lstatSync(arg);
  return stats.isSymbolicLink() ? realpathSync(arg) : arg;
});

const exitCode = await cli.run(resolvedArgv);
process.exit(exitCode);
