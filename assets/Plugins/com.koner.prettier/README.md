<img src="./icon.png" alt="Prettier Logo" width="128" height="128" /><br>

# Xed-Editor Prettier Extension

This extension adds automatic code formatting powered
by [Prettier](https://github.com/prettier/prettier).

## Features

- Format code with Prettier through commands or automatically on save
- Format whole file or selected region
- Customize code style and formatting options through settings

## Build instructions

### Prerequisites

- [Bun 1.x](https://bun.com/docs/installation) (install e.g. via `npm install -g bun`)

### Build binaries

First setup `prettier-standalone` by installing dependencies:

```bash
cd prettier-standalone && bun install
```

To build the standalone Prettier binaries, execute:

```bash
bun run build
```

### Build extension

Now you can build the extension by executing:

```bash
cd .. && ./compileDebug
```

## Installation

Install the extension through the Xed-Editor's extension marketplace, and you're ready to go!
Alternatively, you can download the latest release ZIP file and install it via *
*`Settings > Extensions > Install from storage`**.
