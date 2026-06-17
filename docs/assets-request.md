# Assets Request

请使用拥有授权的 AI 生成图片或自制素材。

## 放置目录

生成后的前端静态图片统一放在：

```text
web/public/assets/
├── backgrounds/
├── roles/
└── avatars/
```

Vite 会把 `web/public` 原样作为站点根目录发布，所以代码中使用 `/assets/...` 路径访问这些图片。推荐使用 PNG；如果你想减小体积，也可以额外提供 WebP，但当前代码默认读取下面这些 PNG 文件名。

## 背景图

| 文件名 | 尺寸 | 用途 | 风格 | 透明背景 | 提示词 |
| --- | --- | --- | --- | --- | --- |
| `web/public/assets/backgrounds/landing-bg.png` | 1920x1080 | 首页背景 | 暗夜森林、满月、薄雾、远处古堡 | 否 | dark night forest, full moon, fog, distant gothic castle, board game mystery atmosphere |
| `web/public/assets/backgrounds/game-table-bg.png` | 1920x1080 | 游戏桌背景 | 俯视圆桌、暗色木纹、烛光 | 否 | top down round wooden table, candle light, mysterious hidden role board game |
| `web/public/assets/backgrounds/victory-bg.png` | 1920x1080 | 胜利结算 | 阵营胜利海报风 | 否 | cinematic werewolf game victory poster, moonlight, dramatic but readable background |

## 角色头像

角色头像统一 512x512，建议透明背景，放在 `web/public/assets/roles/`：

- role-werewolf.png
- role-wolf-king.png
- role-white-wolf-king.png
- role-hidden-wolf.png
- role-villager.png
- role-seer.png
- role-witch.png
- role-hunter.png
- role-guard.png
- role-idiot.png
- role-knight.png
- role-grave-keeper.png
- role-magician.png
- role-cupid.png
- role-elder.png

## AI 玩家头像

AI 玩家头像放在 `web/public/assets/avatars/`：

- `avatar-ai-01.png` 到 `avatar-ai-18.png`
- 512x512
- 建议透明背景
- 风格：不同性格的神秘桌游角色头像

## 使用说明

- 首页会读取 `/assets/backgrounds/landing-bg.png`。
- 游戏桌会读取 `/assets/backgrounds/game-table-bg.png`。
- 结算页会读取 `/assets/backgrounds/victory-bg.png`。
- 玩家身份已知时优先读取 `/assets/roles/role-*.png`。
- 玩家身份未知时读取 `/assets/avatars/avatar-ai-XX.png`。
- 如果某张图片缺失，前端会回退到数字头像或 CSS 背景，不会影响构建。
