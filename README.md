<div align="center">

<img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/hero.png" width="400" alt="TokenPhage" />

### 당신이 AI를 얼마나 굴렸는지, 프로필 한 장으로 증명하세요.

**토큰을 쌓을수록 마스코트가 진화하는 GitHub README 배지** 👾

![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4%20·%20Java%2021-6DB33F?style=flat-square)
![CLI](https://img.shields.io/badge/CLI-Node.js%20ESM-5EEAD4?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-555?style=flat-square)

</div>

---

## 👾 TokenPhage란?

GitHub 잔디가 당신의 커밋을 보여주듯, **TokenPhage**는 Claude Code·Codex·opencode로 태운 토큰을 **당신의 AI 활용 스탯**으로 보여줍니다.

누적 토큰·모델별 집계·30일 히트맵을 배지 한 장에 담고, 토큰을 쌓을수록 **마스코트가 Lv.1 → Lv.5로 진화**합니다. README에 **한 줄** 붙이면 끝 — 그다음은 매일 자동으로 갱신됩니다.

<div align="center">

<table>
  <tr>
    <td align="center"><strong>gpu · dark</strong></td>
    <td align="center"><strong>claude · light</strong></td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=gpu&amp;mode=dark" alt="TokenPhage gpu dark 배지"></a><br/>
      <code>https://api.tokenphage.com/badge/kobenlys?theme=gpu&amp;mode=dark</code>
    </td>
    <td align="center">
      <a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=claude&amp;mode=light" alt="TokenPhage claude light 배지"></a><br/>
      <code>https://api.tokenphage.com/badge/kobenlys?theme=claude&amp;mode=light</code>
    </td>
  </tr>
</table>

</div>

### 🎫 배지 한 장에 담기는 정보

- 🔥 **누적 토큰 사용량**
- 📅 **최근 30일 히트맵**
- 📊 **모델별 집계**
- 👾 **레벨 마스코트**
- ➕ **그 외 다양한 정보 (ETC)**

> ✨ 배지에 담기는 정보와 기능은 **테마마다 다르며**, 앞으로 새로운 **테마**가 계속 추가됩니다. ✨

---

## 🚀 시작하기 & 사용법

> 📦 아래는 **빠른 시작 요약**이에요. <br>
> 화면별 자세한 사용법·인증 절차·자동 동기화 설정은 👉 **[tokenphage-cli 레포](https://github.com/TOKENPHAGE/tokenphage-cli)** 에 정리돼 있습니다.



**1️⃣ 설치** — Node.js 20.17+

```bash
npm install -g tokenphage@latest
```

**2️⃣ 실행하고 로그인**

```bash
tokenphage
```

실행하면 대화형 화면(TUI)이 뜹니다. **`Authenticate with Gist`** 를 고르면 **공개 Gist 소유권**만으로 로그인되고(비밀번호·OAuth 불필요), 첫 동기화까지 자동으로 끝납니다.

**3️⃣ 배지 붙이기** — GitHub 프로필이나 레포 README에 한 줄:

```markdown
[![Tokenphage](https://api.tokenphage.com/badge/<your-github-username>)](https://github.com/TOKENPHAGE)
```

로그인 후에는 같은 화면에서 **지금 동기화 · 매일 04:00 자동 동기화 켜기/끄기 · 데이터 초기화(Reset)** 를 모두 다룰 수 있어요.

> 💡 로그인 직후, Claude Code가 오래된 기록을 자동 삭제(`cleanupPeriodDays`)해 과거 토큰이 누락되지 않도록 **기록 영구 보존**을 켤지 물어봅니다. `Yes`로 두면 모든 토큰이 빠짐없이 집계됩니다.

---

## 🎨 테마 & 레벨

배지는 쿼리 파라미터로 외형(테마·색상 모드)을 바꿀 수 있고, 어떤 테마든 **누적 토큰이 쌓일수록 마스코트가 Lv.1 → Lv.5로 진화**합니다.

```markdown
[![Tokenphage](https://api.tokenphage.com/badge/<username>?theme=claude&mode=dark)](https://github.com/TOKENPHAGE)
```

| 파라미터    | 값                                  | 기본값     | 설명                          |
|---------|------------------------------------|---------|-----------------------------|
| `theme` | `gpu` · `claude` · `grass-claude`  | `gpu`   | 배지 스킨 (마스코트·레이아웃·색 팔레트가 달라짐) |
| `mode`  | `light` · `dark`                   | `light` | 색상 모드                       |

> 🚀 현재 테마는 **3종(`gpu` · `claude` · `grass-claude`)** 입니다. 앞으로 더 다양한 테마를 계속 추가 릴리즈할 예정이에요 — 다음 업데이트를 기대해 주세요!

#### 🖥️ GPU 테마 (기본)

코어(팬) 가속 · 과열 글로우 · 색 변화(녹→빨) · Lv.4~5 전기 스파크로 진화합니다.

<table>
<tr>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/gpu-lv1.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/gpu-lv2.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/gpu-lv3.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/gpu-lv4.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/gpu-lv5.svg" width="120"/></td>
</tr>
<tr>
<td align="center"><b>Lv.1</b><br/><sub><code>&lt; 10M</code></sub></td>
<td align="center"><b>Lv.2</b><br/><sub><code>&lt; 100M</code></sub></td>
<td align="center"><b>Lv.3</b><br/><sub><code>&lt; 500M</code></sub></td>
<td align="center"><b>Lv.4</b><br/><sub><code>&lt; 1B</code></sub></td>
<td align="center"><b>Lv.5</b><br/><sub><code>&ge; 1B</code></sub></td>
</tr>
</table>

#### 👾 Claude 테마

글로우가 강해지고, 최고 레벨에선 몸 색이 빨갛게 변합니다.

<table>
<tr>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/claude-lv1.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/claude-lv2.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/claude-lv3.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/claude-lv4.svg" width="120"/></td>
<td align="center"><img src="https://cdn.jsdelivr.net/gh/TOKENPHAGE/tokenphage-assets@main/mascot/claude-lv5.svg" width="120"/></td>
</tr>
<tr>
<td align="center"><b>Lv.1</b><br/><sub><code>&lt; 10M</code></sub></td>
<td align="center"><b>Lv.2</b><br/><sub><code>&lt; 100M</code></sub></td>
<td align="center"><b>Lv.3</b><br/><sub><code>&lt; 500M</code></sub></td>
<td align="center"><b>Lv.4</b><br/><sub><code>&lt; 1B</code></sub></td>
<td align="center"><b>Lv.5</b><br/><sub><code>&ge; 1B</code></sub></td>
</tr>
</table>

#### 🌱 Grass 테마

클로드 마스코트가 **하늘 놀이터**를 걸어 다니는 잔디 스타일 배지. 카드형과 달리 잔디 그리드를 넓게 펼칩니다.

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=grass-claude" alt="TokenPhage grass-claude light 배지"></a>

`?theme=grass-claude`

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=grass-claude&amp;mode=dark" alt="TokenPhage grass-claude dark 배지"></a>

`?theme=grass-claude&mode=dark`

#### 🌗 색상 모드

<table>
<tr>
<td align="center"><img src="https://api.tokenphage.com/badge/kobenlys?mode=light" width="300"/><br/><sub><code>mode=light</code></sub></td>
<td align="center"><img src="https://api.tokenphage.com/badge/kobenlys?mode=dark" width="300"/><br/><sub><code>mode=dark</code></sub></td>
</tr>
</table>

>  ⏱️ **배지는 60분간 캐시됩니다.** 방금 동기화했다면, 최신 수치가 배지에 반영되기까지 최대 1시간 걸릴 수 있어요.


---

<div align="center"><sub>MIT License · 토큰을 먹일수록 마스코트는 자랍니다 👾</sub></div>
