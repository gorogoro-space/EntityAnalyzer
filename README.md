# EntityAnalyzer
[![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-brightgreen.svg)](https://fill-ui.papermc.io/projects/paper/version/1.21.11)
[![GitHub release](https://img.shields.io/github/release/gorogoro-space/EntityAnalyzer.svg)](https://github.com/gorogoro-space/EntityAnalyzer/releases)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/gorogoro-space/EntityAnalyzer/issues)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://github.com/gorogoro-space/EntityAnalyzer/blob/main/LICENSE)

Plugin for Gorogoro servers.

読み込まれているチャンクのエンティティー・タイルエンティティーの数を数えて、多い場所や種類をランキングで表示するプラグインです。エンティティーが多いチャンクの近くにいるプレイヤーに注意を出すこともできます。

# Requirements
- Paper 1.21.11 以降(1.21.11 未満のサーバーでは読み込まれません)
- Java 21

# Installation method
Please place the .jar file in the Paper plugins folder.

.jar ファイルを Paper の plugins フォルダに置き、サーバーを再起動してください。

# Usage
```
/eastatus [<world>] [<type>]
/eawrank
/eatwrank
/earank <world> <limit>
/eatrank <world> <limit>
/eathrank <world> <limit>
/ealimits
/ealimitset <limit|chunk> <value>
/eanear
/eareload
```

- `/eastatus`(OP) — ワールドごとに、エンティティーとタイルエンティティーの数が多い種類を上位 10 件表示します。ワールドを指定するとそのワールドだけを表示します
  - `/eastatus <world> <type>` — 指定した種類(`ZOMBIE`、`HOPPER` など)が多いチャンクを上位 10 件表示します
- `/eawrank` / `/eatwrank` — ワールドごとのエンティティー / タイルエンティティーの数を、多い順に表示します
- `/earank` / `/eatrank` / `/eathrank` — 指定したワールドで、エンティティー / タイルエンティティー / ホッパーが多いチャンクを、多い順に `<limit>` 件表示します
- `/ealimits` — 注意を出す基準(`limit_entity`・`detect_chunk_range`)を表示します
- `/ealimitset`(OP) — 注意を出す基準を変更し、config.yml に保存します
  - `limit` — `limit_entity` を変更します(`lower_limit_entity` 未満にはできません)
  - `chunk` — `detect_chunk_range` を変更します(`upper_limit_detect_chunk_range` より大きくはできません)
- `/eanear`(OP・プレイヤーのみ) — 自分の周りに、エンティティーが多いチャンクがあるか調べます
- `/eareload`(OP) — config.yml を読み直します

チャンクの位置は、チャンクの北西の角のブロック座標(x,z)で表示します。

`/eastatus`・`/earank`・`/eatrank`・`/eathrank` は、サーバーが重くならないように数 tick に分けて数えるため、結果が出るまで少し時間がかかります。
- 同時に実行できるのはサーバー全体で 1 つまでです
- 実行した人は、結果が出てから 30 秒間(`scan_cooldown_seconds`)は再び実行できません(OP・コンソールも同じ)
- 数えている途中でモブなどが移動すると、エンティティーの数がわずかにずれることがあります

# Config
`plugins/EntityAnalyzer/config.yml`

| 項目 | 内容 |
|---|---|
| `notice` | `true` にすると、エンティティーが多いチャンクの近くにいるプレイヤーに注意を出します(同じチャンクについては 1 時間に 1 回) |
| `notice_interval_seconds` | 注意を出すために調べる間隔(秒)。既定は 1 |
| `limit_entity` | 注意を出す基準。チャンク内のエンティティーかタイルエンティティーがこの数以上 |
| `detect_chunk_range` | プレイヤーから何チャンク以内を調べるか |
| `lower_limit_entity` | `/ealimitset limit` で設定できる最小値 |
| `upper_limit_detect_chunk_range` | `/ealimitset chunk` で設定できる最大値 |
| `scan_time_per_tick_ms` | `/eastatus` などで、1 tick に数えるのに使う時間(ミリ秒)。小さいほどサーバーへの影響は小さく、結果が出るまでは長くなります |
| `scan_cooldown_seconds` | `/eastatus` などを実行した人が、結果が出てから再び実行できるまでの秒数 |

config.yml を書き換えたら、`/eareload` を実行するか、サーバーを再起動してください。

# Disclaimer
Do not assume any responsibility by use. Please use it at your own risk.

## ビルド

JDK 21 が必要です。Gradle はラッパー(`gradlew`)が自動でダウンロードするので、別途インストールする必要はありません。

```
gradlew.bat clean build
```

(macOS / Linux では `./gradlew clean build`)

## IntelliJ IDEA でのビルド手順

本プロジェクトはビルドツールに Gradle を使用しています。
IntelliJ IDEA 上で正しくプラグイン（JARファイル）を生成するには、以下の手順を実行してください。
「ビルド → アーティファクトのビルド」はクラスファイルが入らないことがあるので使わないでください。

### 🛠️ ビルド手順

1. IntelliJ IDEA の画面右端にある **「Gradle」タブ** をクリックして開きます。
2. プロジェクト名（EntityAnalyzer）を展開し、 **`Tasks`** ツリーを開きます。
3. リスト内にある **`clean`** をダブルクリックして実行します（古いビルドキャッシュを削除します）。
4. 続けてリスト内にある **`build`** をダブルクリックして実行します。

### 📦 生成されたファイルの場所
ビルドが成功すると、プロジェクトのルート直下に `build/libs` フォルダが作成（または更新）され、その中に中身の詰まった正しい JAR ファイルが生成されます。

* **生成先:** `build/libs/EntityAnalyzer-<version>.jar`(バージョンは `gradle.properties` の `version`)

この JAR ファイルを Minecraft サーバーの `plugins` フォルダに配置してください。

## 開発(IntelliJ IDEA で Claude Code を使う)

このリポジトリには、AI コーディングツール [Claude Code](https://docs.claude.com/ja/docs/claude-code/overview) 向けの作業ルールを書いた `CLAUDE.md` があります。IntelliJ IDEA で使う手順は次のとおりです。

1. Claude の有料プラン(Pro / Max)か、Anthropic API のアカウントを用意します。
2. Windows では、先に [Git for Windows](https://git-scm.com/downloads/win) をインストールします。
3. Claude Code 本体をインストールします。Windows では PowerShell で次を実行します。
   ```
   irm https://claude.ai/install.ps1 | iex
   ```
4. IntelliJ IDEA の「設定 → プラグイン → Marketplace」で「Claude Code」を検索してインストールし、IDE を再起動します。検索結果には Anthropic 以外が作った似た名前のプラグインも表示されます。プラグイン名の下に書かれた提供元が「Anthropic」になっているものを選んでください。
5. プロジェクトを開いて、ターミナルで `claude` を実行します(`Ctrl+Esc` でも起動できます)。初回はブラウザでログインします。

起動すると `CLAUDE.md` が自動で読み込まれ、このプロジェクトの設計方針に沿って作業します。

## ライセンス

[LICENSE](LICENSE) を参照してください。

## kubotan へのメモ

リポジトリを新規作成したら、**必ず Watch を設定すること**(忘れない!)。
GitHub の自動 Watch 機能は 2025 年 5 月に廃止されたため、設定しないと他の人が立てた issue や PR の通知が届かない。

1. リポジトリのページ右上の **「Watch」** を押す
2. **「Custom」** を選び、**Issues** と **Pull requests** にチェックを入れる
