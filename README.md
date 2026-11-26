<div align="center">

<img src="docs/images/logo.png" width="128" alt="Raven Visuals">

# Raven Visuals

**Визуальный клиент для Minecraft 26.2 на Fabric**

Клик гуи в стиле iOS, свой HUD, ножи и агенты из CS2, кастом шрифты и куча мелочей для HolyWorld и FunTime.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?style=for-the-badge)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.5-DBD0B4?style=for-the-badge)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-E0245E?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-FF5C8A?style=for-the-badge)](LICENSE)

[![Download](https://img.shields.io/github/v/release/Sanch3zss/Raven?label=Скачать&style=for-the-badge&color=E0245E)](https://github.com/Sanch3zss/Raven/releases/latest)
[![Telegram](https://img.shields.io/badge/Telegram-ravenvisuals-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/ravenvisuals)

<img src="docs/images/visuals.png" alt="Меню Raven Visuals">

</div>

## Меню

Меню в стиле iOS со светлой и тёмной темой. Тема переключается на вкладке «Темы», там же любой цвет акцента, готовые пресеты и свои темы с экспортом кодом.

<table>
  <tr>
    <td><img src="docs/images/themes-dark.png" alt="Тёмная тема"></td>
    <td><img src="docs/images/themes-light.png" alt="Светлая тема"></td>
  </tr>
  <tr>
    <td align="center">Тёмная тема и выбор цвета</td>
    <td align="center">Светлая тема</td>
  </tr>
</table>

<!-- сюда ссылку на raven-menu.mp4 из GitHub (user-attachments) -->

Быстрый поиск по всем модулям - <kbd>Ctrl</kbd> + <kbd>F</kbd>.

<img src="docs/images/search.png" width="300" alt="Поиск модулей">

## HUD

Watermark, TargetHud, MediaPlayer с текстом песни, GIF, Hotkeys, координаты, ActivePotions, Equipment, Cooldowns и notifications.

<img src="docs/images/hud.png" alt="HUD">

## Ножи и агенты из CS2

Ножи, перчатки и скины из CS2 с анимациями осмотра и ударов. В третьем лице вместо скина - модель агента.

<table>
  <tr>
    <td><img src="docs/images/knife-inspect.png" alt="Осмотр ножа"></td>
    <td><img src="docs/images/knife-gloves.png" alt="Перчатки"></td>
  </tr>
</table>

<!-- сюда ссылку на raven-knife.mp4 из GitHub (user-attachments) -->

<img src="docs/images/agent.png" alt="Агент в третьем лице">

## Возможности

<details>
<summary><b>Visuals</b></summary>

ArmTweaks, AspectRatio, ChinaHat, ClientUsers, Crosshair, CustomEnchantColor, CustomFont, CustomHitBox, CustomTags, Ambience, HitColor, HitParticles, JumpCircle, KillEffects, LeaveTracker, MotionBlur, Party, Pets, PopTweaks, ProjectilePrediction, Removals, SeeInvisibles, ShulkerBoxTooltip, TargetESP, Trails, 2DItems, WorldParticles

</details>

<details>
<summary><b>Player</b></summary>

AutoJump, AutoLeave, AutoRespawn, AutoSensitivity, DeathCoordinates, FakePlayer, FreeLook, ItemScroller, Sprint

</details>

<details>
<summary><b>Misc</b></summary>

AntiRelog, AutoAuth, AutoSwap, BetterChat, ClanInvest, ClickFriend, DiscordRpc, ElytraSwap, ExpTweaks, FT Helper, HitSounds, HW Helper, LockSlot, MineHelper, NameProtect, NbtGetter, PvPSafe, Sounds, TapeMouse, TPAccept

</details>

<details>
<summary><b>Interface</b></summary>

ClickGui, Watermark, PotionList, TargetHud, MediaPlayer, Gif, Hotkeys, Coords, InventoryHud, EquipmentHud, Cooldowns, Notifications

</details>

Ещё в меню: друзья, Party с метками участников, вейпоинты, макросы, карта AlphaSmp и ивенты FunTime.

## CustomFont

Любой `.ttf` или `.otf` вместо шрифта Minecraft - в чате, табе, меню и над головами. Буквы рисуются под твой масштаб интерфейса, без зернистости.

<img src="docs/images/title-screen.png" alt="Свой шрифт в меню Minecraft">

## Установка

1. Поставь [Fabric Loader](https://fabricmc.net/use/) для Minecraft **26.2**.
2. Скачай [Fabric API](https://modrinth.com/mod/fabric-api) и [Raven Visuals](https://github.com/Sanch3zss/Raven/releases/latest).
3. Положи оба `.jar` в папку `mods`.
4. Нужна **Java 25**. Меню открывается на <kbd>Right Shift</kbd>.

## Сборка из исходников

```
git clone https://github.com/Sanch3zss/Raven.git
cd Raven
./gradlew build
```

Готовый мод появится в `build/libs/`.

## Лицензия

Код распространяется под лицензией [MIT](LICENSE). Сторонние ресурсы и их лицензии перечислены в [Credits](src/main/resources/META-INF/licenses/Credits.txt).

Модели ножей, перчаток и агентов, их текстуры и скины принадлежат Valve Corporation. Raven Visuals не связан с Valve, Mojang и Microsoft.
