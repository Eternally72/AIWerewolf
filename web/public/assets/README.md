# Frontend Assets

Generated image assets should be placed here and can be referenced by the app with `/assets/...` paths.

Recommended structure:

```text
web/public/assets/
├── backgrounds/
│   ├── landing-bg.png
│   ├── game-table-bg.png
│   └── victory-bg.png
├── roles/
│   ├── role-werewolf.png
│   ├── role-wolf-king.png
│   ├── role-white-wolf-king.png
│   ├── role-hidden-wolf.png
│   ├── role-villager.png
│   ├── role-seer.png
│   ├── role-witch.png
│   ├── role-hunter.png
│   ├── role-guard.png
│   ├── role-idiot.png
│   ├── role-knight.png
│   ├── role-grave-keeper.png
│   ├── role-magician.png
│   ├── role-cupid.png
│   └── role-elder.png
└── avatars/
    ├── avatar-ai-01.png
    ├── avatar-ai-02.png
    └── avatar-ai-18.png
```

Use PNG as the source artwork and generate WebP/AVIF derivatives for production when possible. Keep the documented filenames stable so the frontend can resolve them without extra configuration.

Recommended optimization targets:

- Backgrounds: keep source PNG, generate WebP around 1920px wide, quality 78-84.
- Role avatars: keep source PNG, generate WebP around 256px square, quality 78-84.
- AI avatars: keep source PNG, generate WebP around 192px square, quality 78-84.

Suggested optional structure:

```text
web/public/assets/optimized/
├── backgrounds/
├── roles/
└── avatars/
```

The current app preloads critical PNG assets and lazy-loads avatars. If WebP/AVIF derivatives are added later, update `web/src/assets.ts` to point role/avatar/background helpers at the optimized paths.
