# 🎫 배지 카탈로그

TokenPhage가 제공하는 배지 전체 목록입니다. 처음이라면 [README의 시작하기](README.md#-시작하기--사용법)부터 보세요.

<br/>

---

## 목차

- [붙이는 법](#붙이는-법)
- [파라미터](#파라미터)
  - [테마별 지원 mode](#테마별-지원-mode)
- [🖥️ gpu — 기본 테마](#️-gpu--기본-테마)
- [👾 claude](#-claude)
- [🌱 grass-claude](#-grass-claude)
- [🏅 beta-tester — 자격 배지](#-beta-tester--자격-배지)
  - [🔒 locked](#-locked)
- [⏱️ 참고](#️-참고)

<br/>

---

## 붙이는 법

README(이미지를 붙일 수 있는 곳이면 어디든)에 아래 한 줄을 넣고, `<your-github-name>`만 본인 GitHub 아이디로 바꾸면 됩니다.

```markdown
[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>)](https://github.com/TOKENPHAGE)
```

외형은 URL 뒤에 `?theme=`·`&mode=`를 붙여 바꿉니다.

```markdown
[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=claude&mode=dark)](https://github.com/TOKENPHAGE)
```

## 파라미터

| 파라미터 | 값 | 기본값 | 설명 |
|---------|-----|-------|------|
| `theme` | `gpu` · `claude` · `grass-claude` · `beta-tester` | `gpu` | 배지 스킨 (마스코트·레이아웃·색 팔레트가 달라짐) |
| `mode` | 테마마다 다름 (아래 표) | 테마마다 다름 | 색상 |

### 테마별 지원 mode

| 테마 | 지원 `mode` | 기본값 |
|------|------------|-------|
| `gpu` | `light` · `dark` | `light` |
| `claude` | `light` · `dark` | `light` |
| `grass-claude` | `light` · `dark` | `light` |
| `beta-tester` | `cyan` · `green` · `purple` | `cyan` |

> 지원하지 않는 `mode`를 넣어도 오류가 나지 않고, 그 테마의 기본값으로 표시됩니다.

<br/>

---

## 🖥️ gpu — 기본 테마

**부여 조건** · 기본 제공 — CLI로 로그인하고 한 번 동기화하면 바로 쓸 수 있습니다.

<div align="center">

**light · 기본**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=gpu&amp;mode=light" alt="TokenPhage gpu light 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>)](https://github.com/TOKENPHAGE)`

**dark**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=gpu&amp;mode=dark" alt="TokenPhage gpu dark 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?mode=dark)](https://github.com/TOKENPHAGE)`

</div>

<div align="center">

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

</div>

<br/>

---

## 👾 claude

**부여 조건** · 기본 제공 — CLI로 로그인하고 한 번 동기화하면 바로 쓸 수 있습니다.

<div align="center">

**light · 기본**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=claude&amp;mode=light" alt="TokenPhage claude light 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=claude)](https://github.com/TOKENPHAGE)`

**dark**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=claude&amp;mode=dark" alt="TokenPhage claude dark 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=claude&mode=dark)](https://github.com/TOKENPHAGE)`

</div>

<div align="center">

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

</div>

<br/>

---

## 🌱 grass-claude

**부여 조건** · 기본 제공 — CLI로 로그인하고 한 번 동기화하면 바로 쓸 수 있습니다.

<div align="center">

**light · 기본**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=grass-claude" alt="TokenPhage grass-claude light 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=grass-claude)](https://github.com/TOKENPHAGE)`

**dark**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=grass-claude&amp;mode=dark" alt="TokenPhage grass-claude dark 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=grass-claude&mode=dark)](https://github.com/TOKENPHAGE)`

</div>

<br/>

---

## 🏅 beta-tester — 자격 배지

**부여 조건** · 운영자 부여 — 클로즈드 베타(2026-07~08) 기여자에게 운영자가 직접 부여합니다. 별도 신청 경로는 없습니다.

<div align="center">

**cyan · 기본**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=beta-tester" alt="TokenPhage beta-tester cyan 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=beta-tester)](https://github.com/TOKENPHAGE)`

**green**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=beta-tester&amp;mode=green" alt="TokenPhage beta-tester green 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=beta-tester&mode=green)](https://github.com/TOKENPHAGE)`

**purple**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/kobenlys?theme=beta-tester&amp;mode=purple" alt="TokenPhage beta-tester purple 배지"></a>

`[![Tokenphage](https://api.tokenphage.com/badge/<your-github-name>?theme=beta-tester&mode=purple)](https://github.com/TOKENPHAGE)`

</div>

### 🔒 locked

**부여 조건** · 자동 표시 — 자격이 없는 계정이 자격 배지를 요청하면 실제 배지 대신 이 배지가 나옵니다. 직접 고르는 테마가 아닙니다.

<div align="center">

**locked**

<a href="https://github.com/TOKENPHAGE"><img src="https://api.tokenphage.com/badge/octocat?theme=beta-tester" alt="TokenPhage locked 배지"></a>

</div>

<br/>

---

## ⏱️ 참고

배지는 **60분간 캐시됩니다.** 방금 동기화했다면 최신 수치가 배지에 반영되기까지 최대 1시간 걸릴 수 있어요.

새로운 테마는 계속 추가될 예정입니다.
