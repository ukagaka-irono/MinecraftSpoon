Spoon
=====
[![Latest Release](https://img.shields.io/github/v/release/ukagaka-irono/MinecraftSpoon?include_prereleases&label=Latest&color=blue&logo=github)][Download]

[English](README.md) | 日本語

Spoon は Minecraft 1.7.10 用の Minecraft Forge 10.13.4.1614 から派生したフォークです。
Forge と FML のパッケージ（`net.minecraftforge`, `cpw.mods.fml`）、`Forge` と `FML` の Mod ID、Forge のバージョン番号を
そのまま残しています。そのため、既存の 1.7.10 用 Forge Mod を変更せずに読み込めます。

| Minecraft | 元にした Forge     | 状態       |
|-----------|--------------------|------------|
| 1.7.10    | Forge 10.13.4.1614 | ベータ版   |

* [ダウンロード][Download]
* [Web サイト][Website]
* [Issues]
* [サポート][Support]

# Spoon の導入

[リリースページ][Download]から最新のリリースのファイルをダウンロードしてください。
Spoon はベータ版です。試す前にワールドをバックアップしてください。

## Prism Launcher

Spoon は [Prism Launcher][Prism] で動作を確認しています。

1. `spoon-<バージョン>-prism.zip` をダウンロードします。
2. Prism で「インスタンスを追加」（Add Instance）>「インポート」（Import）を選び、zip を指定します。
   Minecraft 1.7.10 と必要なライブラリは Prism がダウンロードします。
3. Mod はインスタンスの `mods` フォルダーに入れます。ゲーム内の Mod 一覧にある「Mod フォルダーを開く」ボタンからも
   開けます。

Prism が Java 8 を選ばないときは、インスタンスの設定で Java 8 を指定してください。

## 公式ランチャー

1. 公式ランチャーで Minecraft 1.7.10 を一度起動しておきます。
2. `spoon-<バージョン>-installer.jar` を Java 8 で実行し、「Install client」を選びます。
3. ランチャーに `Spoon` という起動構成が追加されます。

インストーラーは Forge 1.7.10 のものを Spoon 用に変えたものです。
`spoon-<バージョン>-universal.jar` は Spoon 本体です。手作業で組み込むときに使います。

困ったときや質問があるときは、[サポートガイド][Support]を読んでください。

## 必要な Java のバージョン

Spoon には、Minecraft 1.7.10 と同じバージョンの Java が必要です。

| Minecraft | Java            |
|-----------|-----------------|
| 1.7.10    | [8][Adoptium-8] |

# Spoon の変更点

* **表示:** タイトル画面と F3 画面に `Spoon <バージョン> (Forge 10.13.4.1614)` と表示されます。
  クライアントとサーバーのブランド名は `fml,forge,spoon` です。
* **Minecraft Realms の削除:** タイトル画面には Realms ボタンの代わりに、横幅いっぱいの「Mod」ボタン（英語では「Mods」）が表示されます。
* **Windows での日本語・中国語・韓国語 IME への対応:**
  * 文字入力欄にフォーカスがないあいだは IME をゲーム画面から切り離します。IME をオンにしたままでも WASD などの
    操作キーが効きます。
  * IME で確定した文字が、チャット、看板、その他の文字入力欄に入ります。
  * 変換中の文字と変換候補のウィンドウが、入力位置（キャレット）のそばに開きます。
* **看板:** 矢印キー、Home/End、Delete で動かせるカーソルがあり、行の途中に文字を挿入できます。
  看板の幅に収まらない文字は入力できません。
* **全角英数字:** `ｋ`（U+FF4B）などの幅を、実際に描画される幅で計算します。全角英数字を含む中央揃えの文字列が
  左にずれなくなります。
* **[Mod Menu] 風の Mod 一覧:** 左側に検索、並べ替え、ロゴアイコン、バッジがあり、右側に詳細、「Web サイト」と
  「設定」のボタン、クリックできるリンク、スクロールする説明文があります。「Mod フォルダーを開く」ボタンもあります。
  この一覧は Forge の `GuiModList` の代わりに開く別の画面です。`GuiModList` 自体は変更していないので、EnderCore の
  ように `GuiModList` を拡張する Mod もそのまま動きます。
* **起動の高速化:** Mod を探すときにメソッドの中身を読み飛ばします。読み込みの各段階にかかった時間を
  `Spoon startup: ...` としてログに出力します。
* **クラッシュレポートの原因らしい Mod:** 新しい Forge と同じく、クラッシュレポートの `-- Head --` の節に、
  スタックトレースにコードが出てくる Mod（Mixin で組み込まれたコードも含む）を載せます。
  [サポートガイド][Support]を参照してください。
* **日本語化:** 設定画面、Spoon の設定項目、`/forge` コマンド、「Mod」と「Mod の設定...」のボタン、Mod が
  足りないときなどの FML のエラー画面を日本語にしています。エラー画面は言語ファイルを読み込む前に開くことがあり、
  そのときは英語で表示します。
* **バージョンチェック:** 新しいバージョンが出ると、タイトル画面に表示します。起動時に、Web サイトにある
  [promotions_slim.json][Promotions]（Forge と同じ形式）を読みます。正式版がまだないあいだは、タイトル画面に
  ベータ版の警告も表示します。

## 設定

| 設定                               | `config/forge.cfg` での名前          | 初期値  | 説明 |
|------------------------------------|--------------------------------------|---------|------|
| IME 制御                           | `imeControl`（`general`）            | `true`  | Windows でのみ働きます。`false` にすると、Spoon は IME を切り離さなくなります。 |
| Spoon のバージョンチェックを無効化 | `disableVersionCheck`（`general`）   | `false` | `true` にすると、起動時に新しいバージョンを確認しなくなります。 |

ゲーム内で変更するときは、タイトル画面の「Mod」で Spoon を選び、「設定」>「一般設定」を開きます。
タイトル画面で変更した「IME 制御」はすぐに反映されます。バージョンチェックの設定は、次に起動したときから反映されます。

# Mod の作成

Spoon は Forge 10.13.4.1614 用にビルドした Mod をそのまま動かします。Spoon 独自の MDK は配布していません。
Mod は Forge 1.7.10 用として、Forge のときと同じようにビルドしてください。

# Spoon の開発に参加する

Spoon のコードを調べたり、プルリクエストを送ったり、Spoon 自体に手を入れたりしたい場合は、このリポジトリから始めて
ください。

## ワークスペースの準備

必要なもの:

* JDK 8。Minecraft 1.7.10 と ForgeGradle 1.2 は、より新しい JDK ではビルドできません。
* 初回のセットアップ時のインターネット接続（Gradle、ForgeGradle、Minecraft と MCP のファイルを取得します）。

ビルドには GTNewHorizons がフォークした ForgeGradle 1.2（`com.github.GTNewHorizons:ForgeGradle:1.2.13`）を
Gradle 4.10.3 で使います。Gradle は Gradle Wrapper がダウンロードします。Gradle は更新しないでください。
このフォークの forgedev タスクは、Gradle 5 で削除された `Task.execute()` を呼んでいます。フォークを動かすための
回避策は `build.gradle`、`settings.gradle`（`eclipse/Clean` と `eclipse/Forge` をサブプロジェクトとして登録）、
`buildSrc`（フォークの `ZipFileTree` を修正したもの）にあります。

Wrapper を実行する前に、`JAVA_HOME` を JDK 8 に向けてください。例:

```
Windows (PowerShell): $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot"
MacOS/Linux:          export JAVA_HOME=/path/to/jdk8
```

| 作業 | Windows | MacOS/Linux |
|------|---------|-------------|
| ワークスペースをセットアップする（Minecraft を逆コンパイルし、FML と Forge のパッチを当てる） | `.\gradlew.bat setupForge` | `./gradlew setupForge` |
| universal jar とインストーラーをビルドする（出力先は `build/distributions`） | `.\gradlew.bat packageUniversal packageInstaller` | `./gradlew packageUniversal packageInstaller` |
| Prism Launcher のインスタンス（`build/distributions/spoon-...-prism.zip`）をビルドする | `.\gradlew.bat packagePrism` | `./gradlew packagePrism` |
| ワークスペースからクライアントを起動する（`-PmcUsername=<名前>` も指定できる） | `.\gradlew.bat runClient` | `./gradlew runClient` |

`eclipse/Forge` の Minecraft のクラスを変更したら、`gradlew genPatches` でパッチを作り直してください。
書き換わるのは `patches/minecraft` だけです。差分の基準には FML のパッチを当てたソースを使い、このソースは毎回
`fml/patches` から作り直されます。コミットする前に結果を確認してください。このフォークは、何も変更していない
ワークスペースでも、触る必要のなかったいくつかのパッチ（`Minecraft`、`GuiChat`、`WorldGenDungeons`、
`GenLayerBiome`）を書き換えます。MCP の名前（たとえば `WorldType.DEFAULT_1_1`）が残ったり、逆コンパイラーの
ラベルや行番号が変わったりします。変更するつもりだったパッチだけを残し、ほかは git で元に戻してください。

Eclipse を使う場合は、Eclipse のワークスペースをリポジトリ内の `eclipse` フォルダーにしてください。

### プルリクエスト

プルリクエストを送る前に、[コントリビューションガイド][Contributing]を読んでください。

# ライセンス

Spoon は Minecraft Forge と同じ条件で配布しています。[MinecraftForge-License.txt][License]（Forge Public License）と
[MinecraftForge-Credits.txt][Credits] を参照してください。FML は LGPL です。[fml/LICENSE-fml.txt][FML-License] を
参照してください。Paulscode のライセンスファイルは、同梱しているサウンドライブラリに適用されます。

[Contributing]: ./CONTRIBUTING.ja.md
[Support]: ./SUPPORT.ja.md

[Download]: https://github.com/ukagaka-irono/MinecraftSpoon/releases
[Website]: https://ukagaka-irono.github.io/minecraft-spoon/
[Issues]: https://github.com/ukagaka-irono/MinecraftSpoon/issues

[Prism]: https://prismlauncher.org/
[Mod Menu]: https://modrinth.com/mod/modmenu
[Adoptium-8]: https://adoptium.net/temurin/releases/?version=8
[Promotions]: https://ukagaka-irono.github.io/minecraft-spoon/promotions_slim.json

[License]: ../MinecraftForge-License.txt
[Credits]: ../MinecraftForge-Credits.txt
[FML-License]: ../fml/LICENSE-fml.txt
