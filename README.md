# Antigravity for Android

A mobile coding workspace rebuilt from the ground up using proven ideas from the open-source Acode editor, with a new visual system inspired by Google Antigravity's desktop experience.

This repository is the new Android application foundation. The previous task-control-plane implementation has been removed.

## Architecture

```text
Android UI
   │
   ├── Project / File system
   ├── Code editor
   ├── Agent workspace
   └── Terminal / Run
          │
          ▼
   Local Android ↔ Termux bridge
          │
          ▼
   Antigravity CLI (agy)
```

The UI is intentionally mobile-first. Acode is the reference implementation for solving hard Android editor problems, not the visual identity of this app.

## Status

- [x] Legacy AI-AGENT control plane removed
- [x] New Android/Compose foundation
- [x] Antigravity-inspired dark visual system
- [x] Mobile project rail + editor + bottom workspace dock
- [ ] Real filesystem provider
- [ ] Embedded editor engine
- [ ] Agent streaming bridge to `agy`
- [ ] Terminal integration
- [ ] Build/run pipeline

## License / attribution

Acode is an open-source Android code editor. Its source is used as architectural reference and will be integrated only where compatible with its license and project requirements.

Official source: https://github.com/Acode-Foundation/Acode
