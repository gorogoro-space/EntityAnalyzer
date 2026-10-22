# EntityAnalyzer

読み込まれているチャンクのエンティティー・タイルエンティティーの数を数えてランキングで表示し、多いチャンクの近くのプレイヤーに注意を出す Paper 用プラグイン(Paper 1.21.11 以降・Java 21 必須)。
リポジトリ: https://github.com/gorogoro-space/EntityAnalyzer (LGPL-3.0)

## 作業の進め方(必ず守ること)

- **設計が確定するまで実装しない。** 機能追加や仕様変更は、まず設計案(何を・なぜ・どう変えるか、影響範囲)を提示し、承認を得てからコードを書く。
- 判断が必要な点は、選択肢を示して質問する。勝手に決めない。
- やり取りは日本語で行う。新しく書くコードのコメントは日本語でよい。既存の英語のコメントは、頼まれない限り書き換えない。
- 変更は必要最小限にする。頼まれていないリファクタリングや機能追加はしない。
- 作業後は、変更・追加・削除したファイルの一覧と変更内容を報告する。
- 仕様を変えたら README.md と CLAUDE.md も合わせて更新する。
- `git push` の前には必ず確認を取る。コミットは意味のある単位で分ける。
- リポジトリへの初回の `git push` の前には、「GitHub で Watch 設定(Custom → Issues と Pull requests)はしましたか?」と日本語で確認する。自動 Watch は廃止されており、設定しないと issue や PR の通知が届かない。
- コミットメッセージや PR に `Co-Authored-By: Claude` などの署名を付けない。`.claude/` は Git に入れない(`.git/info/exclude` で除外済み)。
- 実装中に設計の抜けや穴に気づいたら、黙って対処せず報告して相談する。
- プルリクエストをチェックするときは、次の観点で確認して報告する。
  - 変更概要(何を・なぜ変えているか)
  - 脆弱性(権限チェックの漏れ、入力値の検証、パスの扱い、コマンドに埋め込む文字列の検証など)
  - 安全面(データの破損・消失、再読み込み時の後始末、他プラグインの妨げなど)
  - 性能(TPS など)の低下(高頻度イベントでの重い処理、メインスレッドでの同期 I/O、定期タスクの追加など。「最重要の設計方針」に沿っているか)

## 環境

- Paper 1.21.11(`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`、`compileOnly`)/ `plugin.yml` の `api-version: '1.21.11'`(1.21.11 未満のサーバーでは読み込まれない)。勝手に変えない
- **Java 21 必須。** `build.gradle.kts` の toolchain で JDK 21 を使い、Java 21 の class ファイルを出力する。勝手に変えない
- ビルド: Gradle(Kotlin DSL、wrapper 9.7.1)。`gradlew.bat clean build`(Windows)。**JDK 21 で実行する**(JDK の場所は `CLAUDE.local.md`)。「ビルドして」と言われたら常にクリーンビルドする。成果物は `build/libs/EntityAnalyzer-<version>.jar`
  - IntelliJ の「アーティファクトのビルド」はクラスファイルが入らないことがある。必ず Gradle でビルドする
- バージョンは `gradle.properties` の `version` だけで管理する。`plugin.yml` の `version: '${version}'` は `processResources` でビルド時に置き換わる
- パッケージ: `space.gorogoro.entityanalyzer`(**すべて小文字**。大文字が混ざると plugin.yml の main と一致せず起動しない)
- メッセージ・タイトルは Adventure(`Component`・`NamedTextColor`・`Title`)で送る。非推奨の `ChatColor`・`sendTitle(String, ...)` は使わない
- 動作確認はサーバーを再起動して行う。PlugManX での読み込みは権限やコマンドの登録が不完全になることがある。jar を差し替えるときは先に古い jar を消す(同じ名前のプラグインが 2 つあると、どちらが読まれるかわからない)
- サーバーと開発 PC の環境情報は `CLAUDE.local.md`(Git に入れない)を参照する

## 最重要の設計方針

### TPS に影響させない
- 高頻度イベント(PlayerInteractEvent、PlayerMoveEvent など)は、安い判定を先に行い、対象外なら即座に抜ける
- BlockPhysicsEvent、VehicleMoveEvent など発生頻度が極端に高いイベントは使わない
- メインスレッドでファイルや DB の同期 I/O をしない(起動時・リロード時に一度だけ行う小さなファイルの読み込みは除く)
- 定期タスクは最小限にし、追加するときは頻度と理由を設計案に書く
- このプラグインは読み込まれている全チャンクを走査する。走査の回数・範囲を増やす変更は、設計案に影響を書く
  - チャンク・エンティティーはメインスレッドでしか読めないので、別スレッド・非同期では数えない。全チャンクの走査は `ChunkScanTask` で数 tick に分ける(2.0 で変更)
  - タイルエンティティーは `getTileEntities(false)`(複製を作らない)で取得する。取得したものは書き換えない(ワールドが直接変わる)
  - ワールドごとの合計数は `World.getEntityCount()` / `getTileEntityCount()` を使う

### データの保存
- プラグインフォルダ以外には何も書き込まない

### 他プラグインとの関係
- 既存の機能を妨げないこと。イベントをキャンセルする範囲は必要最小限にする

## 機能仕様

(現在の実装の動作。仕様を変えたらここを更新する)

- **起動時(`onEnable`)**: `config.yml` が無ければ jar 内のものを保存する。`notice_interval_seconds` 秒(既定 1。1 未満は 1)ごとの定期タスクを登録する(`startNoticeTask`。2.2 で config 化。それまでは 20 tick 固定)
- **定期タスク(注意)**: `notice: true` のときだけ動く。全ワールドの各プレイヤーについて、プレイヤーのいるチャンクから `detect_chunk_range` チャンク以内(チャンク座標の距離を整数に切り捨てて判定)の読み込まれているチャンクだけを調べ(`getNearbyCrowdedChunks`。読み込まれていないチャンクは読み込まない)、エンティティーかタイルエンティティーが `limit_entity` 以上のチャンクがあれば、タイトル「注意」とメッセージを出し、ログにも出す(1.5 までは全チャンクを調べてから距離で絞っていた)
  - 同じチャンク(キーは `ワールド名_x_z`)については 1 時間に 1 回だけ出す。記録はメモリ上(`checkedMap`)で、1 時間を過ぎたものは定期タスクの最初に消す(2.0 でワールドを区別し、古い記録を消すようにした)
- **チャンクの位置の表示**: チャンク座標 × 16(チャンクの北西の角のブロック座標)
- **コマンド**(権限は `plugin.yml` に登録していない。OP の判定は `sender.isOp()`)
  - ランキング系のコマンドに OP チェックを付けない(プレイヤーが自分のいるチャンクが重いか確認するため)。重さはチェックではなく、分割処理とクールダウンで抑える
  - `/eastatus [<world>]`(OP) — ワールドごとに、エンティティー・タイルエンティティーの種類別の数を上位 10 件表示し、ログにも出す
  - `/eastatus <world> <type>`(OP) — 指定した種類(`EntityType` / `Material` の名前)が多いチャンクを、エンティティー・タイルエンティティーそれぞれ上位 10 件と合計を表示し、ログにも出す
  - `/eawrank` / `/eatwrank` — ワールドごとのエンティティー / タイルエンティティーの数(`getEntityCount` / `getTileEntityCount`)を多い順に表示する
  - `/earank` / `/eatrank` / `/eathrank <world> <limit>` — エンティティー / タイルエンティティー / ホッパー(`HOPPER`・`HOPPER_MINECART`)の数が多いチャンクを `<limit>` 件表示する(共通処理は `chunkRank`)
  - `/ealimits` — `limit_entity` と `detect_chunk_range` を表示する
  - `/ealimitset limit|chunk <value>`(OP) — `limit_entity`(`lower_limit_entity` 以上)/ `detect_chunk_range`(`upper_limit_detect_chunk_range` 以下)を変更し、`config.yml` に保存して読み直す
  - `/eanear`(OP・プレイヤーのみ) — 定期タスクと同じ判定(`getNearbyCrowdedChunks`)を自分の周りで行い、注意を出す(1 時間に 1 回の制限なし)
  - `/eareload`(OP・コンソール) — `config.yml` を読み直し、定期タスクを新しい間隔で登録し直して、「config.yml を読み直しました。」を出す。実行中の集計は、開始時の `scan_time_per_tick_ms` のまま続く
- **全チャンクの集計(`/eastatus`・`/earank`・`/eatrank`・`/eathrank`)**(2.0 で追加)
  - 実行時に読み込み済みチャンクの一覧を作り、`ChunkScanTask` が 1 tick に `scan_time_per_tick_ms` ミリ秒(既定 2)まで数えて(最低 1 チャンク)、続きは次の tick に回す。途中でアンロードされたチャンクは飛ばす。数え終わったら結果を表示する
  - 開始時に「集計を始めました。結果が出るまでお待ちください。」を出す
  - 同時に実行できるのはサーバー全体で 1 つまで(`runningScan`)。実行中なら「ほかの集計が実行中です。終わってから実行してください。」
  - 終わったあと、実行した人(プレイヤーは UUID、コンソールは名前で区別。OP・コンソールも対象)は `scan_cooldown_seconds` 秒(既定 30)実行できない。「クールダウン中です。あと N 秒待ってから実行してください。」(`cooldownMap`。期限が過ぎたものは次の登録時に消す)
  - 数えている途中で失敗しても、実行中の状態を解除してクールダウンに入る
- 存在しないワールド名には「ワールドが見つかりません: X」、数字でない値には「数字で指定してください: X」を出す(2.0 で追加。以前はスタックトレースだけ)
- メッセージは `EntityAnalyzerUtility.sendMessage`(`[EntityAnalyzer]` を暗い赤、本文を赤)で送る。ログアウトしたプレイヤーには送らない
- 例外は `EntityAnalyzerUtility.logStackTrace` でスタックトレースを警告ログに出す

## 過去にハマった点(他プロジェクトでの経験)

- `config.getString(path, "")` のように既定値を渡すと、jar 内 config.yml の既定値が参照されない。既定値なしで取得して null を判定すること
- plugin.yml で `default: true` にした権限でも、登録されないと Bukkit は「OP のみ」として扱う。全員向けの機能を権限で縛らない

## ファイル構成(src/main/java/space/gorogoro/entityanalyzer/)

- `EntityAnalyzer.java` — メインクラス。起動・停止、注意の定期タスク、全コマンドの処理、集計の同時実行とクールダウンの管理
- `ChunkScanTask.java` — チャンクを数 tick に分けて数える定期タスク
- `EntityAnalyzerUtility.java` — ログ出力、メッセージ送信
- `src/main/resources/plugin.yml` — プラグイン定義
- `src/main/resources/config.yml` — 注意の基準、`/ealimitset` で設定できる範囲、集計の 1 tick の時間とクールダウン
- `build.gradle.kts` / `settings.gradle.kts` / `gradle.properties` — Gradle の設定(バージョンは `gradle.properties`)
