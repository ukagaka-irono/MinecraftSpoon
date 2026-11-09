# Spoon 開発の引き継ぎ

Claude Code はこのファイルを毎回読み込みます。Spoon の開発を続けるための前提、手順、注意点をまとめています。
最終更新: 2026-11-09（Spoon 1.0.0-beta.5 をリリースしたあと）

## ユーザーとの進め方

- 返事は日本語で書く。
- コミットと push は、ユーザーに言われたときだけする。いつもの流れは次のとおり。
  1. 実装して、ビルドとテストをする（下の「テスト」）。
  2. テスト用の jar を Prism の `1.7.10-spoon` インスタンスに入れ、ゲーム内での確かめ方を伝える。
  3. ユーザーがゲームで確かめて「問題なし、push して」と言ったら、コミットして `spoon` に push する。
- リリース、Web サイトの更新、リポジトリの設定など、外に出る操作はユーザーの指示があってからする。
- 「ほかに付いたほうが良い機能ある？」と聞かれたら、候補をいくつか挙げ、おすすめを 1 つ示す（下の「次の機能の候補」）。

## プロジェクト

- Spoon は Minecraft 1.7.10 用の Minecraft Forge 10.13.4.1614 のフォーク。既存の 1.7.10 用 Mod をそのまま動かすため、
  次のものは変えない。
  - Forge と FML のパッケージ（`net.minecraftforge`、`cpw.mods.fml`）
  - Mod ID（`Forge`、`FML`）
  - Forge のバージョン番号
  - 既存の API（Mod が呼んだり継承したりしているクラス）
- GitHub は `ukagaka-irono/MinecraftSpoon`。作業もリリースも `spoon` ブランチ（デフォルトブランチ）でする。
- ユーザー向けの説明は `docs/` にある。
  - `README.md` と `README.ja.md`（特に「What Spoon Changes」と「Settings」の節）、`SUPPORT`、`CONTRIBUTING`。
  - MinecraftForge 26.3 の docs を手本にしている。
  - 目に見える機能を足したら、英語と日本語の両方を直す。
- Web サイトは別のリポジトリ `C:\Users\yukin\IdeaProjects\ukagaka-irono.github.io`（`main` ブランチ）。
  - `minecraft-spoon/index.html`: Spoon のページ
  - `minecraft-spoon/promotions_slim.json`: バージョンチェックが読む
  - `index.html`: トップの一覧

## ビルド

JDK 8 が要る。より新しい JDK ではビルドできない。Git Bash の例を示す。

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-8.0.502.7-hotspot"
./gradlew packageUniversal                                              # universal jar だけ（30 秒〜1 分）
./gradlew packageUniversal packageInstaller packagePrism createChangelog # リリースの 4 ファイル
./gradlew genPatches                                                    # Minecraft のクラスを変えたとき（2〜8 分）
```

- **出力:** `build/distributions/spoon-1.7.10-10.13.4.1614-spoon<版>-{universal.jar,installer.jar,prism.zip,changelog.txt}`。
  ビルドするたびに、今の版の名前で上書きされる。
- **Gradle:** 4.10.3 から上げない。GTNH がフォークした ForgeGradle 1.2.13 が、Gradle 5 で消えた API を使っている。
- **Java のバージョン:** クラスファイルは Java 6（major version 50）で、ソースも Java 6 の文法で書く。
  - 使えないもの: ラムダ、ダイヤモンド演算子、try-with-resources、String の switch。
  - 使えるもの: インターフェースのメソッドへの `@Override`。
- **Java ソースの文字:** ASCII 以外の文字を書かない。ビルドの途中で文字化けする。
  - `\u2022` のようなエスケープも化けたので、記号は `(char)0x2022` のように文字コードで書く。
  - コメントでも、`\u` のあとに 16 進数でない文字が続くとコンパイルエラーになる。
  - 日本語などの文は言語ファイルに置く。
- **コマンド:** Bash ツールは Git Bash で動く。`zip` コマンドはないので、zip の操作は Python の `zipfile` でする。
  `gh` は認証済み。

## ソースの置き場所

| 場所 | 中身 | ビルドでの扱い |
|------|------|----------------|
| `src/main/java`、`src/main/resources` | Forge（`net.minecraftforge`）と Forge の言語ファイル | そのまま使われる |
| `fml/src/main/java`、`fml/src/main/resources` | FML（`cpw.mods.fml`）と FML の言語ファイル | `eclipse/Forge/src/main/...` にコピーしないとビルドに入らない |
| `eclipse/Forge/src/main/java/net/minecraft` | パッチを当てた Minecraft のソース（MCP 名） | ビルドに使われる。git の管理外（`.gitignore`） |
| `patches/minecraft` | Forge のパッチ（SRG 名: `field_…`、`func_…`） | `genPatches` で `eclipse/Forge` から作る |
| `fml/patches` | FML のパッチ | ふだんは触らない |

### FML のファイルを変えたとき

`eclipse/Forge` にもコピーする。コピー先の改行の形を保つこと。既存の Java ファイルは CRLF で最後に改行がなく、言語ファイルは LF で最後に改行がある。
新しいファイルは CRLF で、最後に改行を付けない。

```bash
python -I - <<'EOF'
import os
rels = ['java/cpw/mods/fml/client/GuiSpoonModList.java']   # fml/src/main/ からの相対パス（resources/... も可）
for rel in rels:
    src = 'fml/src/main/' + rel
    dst = 'eclipse/Forge/src/main/' + rel
    text = open(src, 'rb').read().decode('utf-8').replace('\r\n', '\n')
    if os.path.exists(dst):
        old = open(dst, 'rb').read().decode('utf-8')
        crlf, trailing = '\r\n' in old, old.endswith('\n')
    else:
        crlf, trailing = True, False
    t = text.rstrip('\n') + ('\n' if trailing else '')
    if crlf:
        t = t.replace('\n', '\r\n')
    open(dst, 'wb').write(t.encode('utf-8'))
    print('synced', rel)
EOF
```

### Minecraft のクラスを変えたとき

1. `eclipse/Forge/src/main/java/net/minecraft/...` を MCP 名のまま直す。
   - バニラのクラスに Spoon のコードを書き込まず、Spoon のクラスを完全修飾名で呼ぶ 1〜2 行だけにする
     （例: `net.minecraftforge.client.CrashWindow.show(p_71377_1_);`）。
   - こうするとパッチが小さく保てる。
2. `./gradlew genPatches` を実行する。
3. `genPatches` は、関係のないパッチも毎回書き換える。次のパッチは `git checkout --` で元に戻す。
   - `Minecraft.java.patch`（行番号がずれる）
   - `GuiChat.java.patch`
   - `CrashReport.java.patch`
   - `WorldGenDungeons.java.patch`
   - `GenLayerBiome.java.patch`

   新しくできたパッチファイルは、そのまま使う。
4. 既存のパッチ（特に `Minecraft.java.patch`）に変更を足すときは、次の順にする。
   1. 生成されたファイルを scratchpad に控える。
   2. git から元に戻す。
   3. 新しいハンクだけを差し込む。
   - コミット済みの `Minecraft.java.patch` は、1300 行目あたりより後のハンクの行番号が、`genPatches` の出力より 1 大きい。
   - 新しいハンクが行を増やすなら、後ろのハンクの `+` 側の開始行もずらす。
   - 行番号は当てる位置の目安にすぎないが、きれいに保つ。

## 決まりごと

- **言語キー:** Spoon の新しい文は `spoon.*` のキーにし、`src/main/resources/assets/forge/lang/{en_US,ja_JP}.lang` に書く。
  - Forge の `forge.*` のキーを使い回さない。言語ファイルはすべて 1 つのキーの表を共有している。そのため、ほかの Mod 用の
    リソースパックが `forge.*` を上書きすることがある（Inventory Pets の日本語化パックで実際に起きた）。
  - FML の画面の文は `fml/src/main/resources/assets/fml/lang` に書く（`eclipse` へのコピーも忘れない）。
- **言語ファイルを読み込む前に開く画面:** `FMLScreenText.format(キー, 英語の文, 引数...)` で英語に戻す。
- **設定:**
  - `ForgeModContainer.syncConfig` に書く。
  - 言語キーは `forge.configgui.<名前>` と `.tooltip`。
  - README の設定の表に、英語と日本語の両方で書く。
  - 今ある Spoon の設定は `imeControl`、`disableVersionCheck`、`crashWindow` の 3 つ。
- **ファイルやフォルダーを開くとき:** `cpw.mods.fml.client.FileOpener.open(File)` を使う。
- **ログ:** 先頭に `Spoon …:` を付ける（`Spoon startup:`、`Spoon version check:`、`Spoon crash window:`、`Spoon world backup:` など）。
- **日本語の言い回し:**
  - ゲーム内の文と README では「Mod」「フォルダー」と書く。
  - Web サイトでは、くだけた書き方で「mod」「フォルダ」と書く。
- **コミットメッセージ:**
  - 英語で、題は命令形（例: `Add a Backup button to the world selection screen`）。本文には理由を書く。
  - 最後に `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` を付ける。
  - Web サイトのリポジトリでは、コミットメッセージを日本語で書く。

## テスト

単体テストの仕組みはない。

**ゲームの外でのテスト**
Minecraft のクラスを使わない処理は別のクラスに分けておき、scratchpad に小さな Java のプログラムを書いて、ビルドした jar で試す。
例: `WorldBackup`、`CrashWindow.createFrame`、`ScreenshotSaver.write`。
- **クラスパス:** ビルドした universal jar に、インスタンスの `minecraft/logs/fml-client-latest.log` にある
  「Java classpath at launch is …」の行を足す。その行からは `NewLaunch.jar` と spoon の jar を除く。
- **起動:** `-Xverify:none` を付けて動かす。
- **FMLLog:** 使う前に、`cpw.mods.fml.relauncher.FMLRelaunchLog.side` に `Side.CLIENT` をリフレクションで入れる。
- **Minecraft のクラスの名前:** universal jar は、Minecraft のクラスを難読化された名前で呼ぶ。
  例えば `I18n` は `brp`、`I18n.format` は `brp.a` になる。代わりのクラスを使うときは、その名前で作ってクラスパスの先頭に置く。
- **署名:** バニラの jar（`C:/Users/yukin/AppData/Roaming/PrismLauncher/libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar`）
  は署名されていて、`SecurityException`（signer information does not match）が出ることがある。そのときは、
  `META-INF/*.SF`、`*.RSA`、`*.DSA` を除いたコピーを使う。
- **初期化できないクラス:** `FMLCommonHandler` と `Loader` は、`LaunchClassLoader` の外では初期化できない（ClassCastException）。
- **Swing の画面:** 画面に出さずに `pack()` してから、`printAll` で `BufferedImage` に描いて PNG にする。その画像を見て確かめる。

**ゲーム内のテスト**（Prism Launcher のインスタンス）
- **`C:\Users\yukin\AppData\Roaming\PrismLauncher\instances\1.7.10-spoon`:** テスト用。Mod がたくさん入っている。
- **`C:\Users\yukin\AppData\Roaming\PrismLauncher\instances\All the Mods 0 - ATM0-spoon`:** 大きな Modpack。ふだんは正式版の jar のままにする。
- **テスト用 jar の入れ方:** ビルドした jar を、インスタンスが今使っている版のファイル名で `libraries/` に入れる。
  - 版は `mmc-pack.json` と `patches/io.github.ukagaka-irono.spoon.json` に書いてある。
  - ビルドの出力は今の版の名前なので、そのままコピーすればよい。
- **テスト用のクラッシュ:** ワールドの中で F3 と C を同時に 6 秒以上押し、C を離すとクラッシュする。

## リリースの手順

N を新しい版の番号とする。

1. `git log --oneline spoon-1.0.0-beta.<前の版>..HEAD` で変更を確かめる。
2. `src/main/java/net/minecraftforge/common/ForgeVersion.java` の `spoonVersion` を `1.0.0-beta.N` にし、
   `Set the Spoon version to 1.0.0-beta.N` としてコミットする。
3. 4 つのファイルをビルドする。
   - changelog の先頭を見る。
   - jar の中の版を確かめる（`unzip -p … net/minecraftforge/common/ForgeVersion.class | tr -c '[:print:]' '\n' | grep '^1\.0\.0'`）。
4. 2 つのインスタンスに入れる。
   - universal jar を `libraries/` にコピーする。
   - `build/prism/patches/io.github.ukagaka-irono.spoon.json` を `patches/` にコピーする。
   - `mmc-pack.json` の版を `sed` で書き換える。
5. 4 つのファイルの `sha256sum` を取る。
6. scratchpad にリリースノートを下書きする。
   - 手本は前回のノート（`gh release view spoon-1.0.0-beta.<前> -R ukagaka-irono/MinecraftSpoon --json body --jq .body`）。
   - 構成は次の順: What's new → 古いベータに表示が出ることの一文 → Downloads の表 → Changes since → 日本語 → SHA-256。
7. Web サイトの変更を用意する（まだコミットしない）。
   - `minecraft-spoon/index.html`: 版が 3 か所（上のメタ行、ベータ版の注意、ファイル名）と、機能の一覧の `<li>`。
   - `index.html`: 版が 1 か所。
   - `minecraft-spoon/promotions_slim.json`: `"1.7.10-latest"`。
8. ユーザーにゲームで確かめてもらい、「問題なし、リリースして」を待つ。
9. タグを作って push する。
   ```bash
   git tag -a spoon-1.0.0-beta.N -m "Spoon 1.0.0-beta.N" <版を上げたコミット>
   git push origin spoon refs/tags/spoon-1.0.0-beta.N
   ```
10. `gh release create spoon-1.0.0-beta.N -R ukagaka-irono/MinecraftSpoon --verify-tag --prerelease --title "Spoon 1.0.0-beta.N" --notes-file <ノート> <4 ファイル>`
    を実行する。そのあと `gh release view … --json assets` で、ハッシュが手元のものと一致することを確かめる。
11. Web サイトをコミットして `main` に push する。
    - メッセージは `Spoon を 1.0.0-beta.N にする` と箇条書き。
    - `promotions_slim.json` を公開するのは、必ず GitHub のリリースができたあとにする。古い版のお知らせがリリースページを指すため。
12. GitHub Pages に反映されるまで、`curl` で JSON とページを確かめる（数分かかる）。

## 機能と置き場所

| 機能 | おもな置き場所 |
|------|----------------|
| ブランド表示 | `src/main/resources/fmlbranding.properties`、`FMLCommonHandler` |
| Realms ボタンの代わりの「Mod」ボタン | `GuiMainMenu.java.patch` |
| Windows の IME 対応 | `net.minecraftforge.client.ime.IMEHandler`（`ForgeModContainer` で登録する。看板のキャレットの位置は `TileEntitySignRenderer.java.patch` から渡す） |
| 看板のカーソル | `GuiEditSign.java.patch` |
| 全角英数字の幅 | `FontRenderer.java.patch` |
| Mod Menu 風の Mod 一覧（検索、並べ替え、設定ファイルのボタン、更新ありのバッジ、起動時間） | `cpw.mods.fml.client.GuiSpoonModList`、`GuiSlotSpoonModList`、`cpw.mods.fml.common.ModConfigFiles`、`net.minecraftforge.client.gui.ModListHandler` |
| Mod ごとの起動時間 | `cpw.mods.fml.common.ModStartupTimes`（`LoadController` と `Loader` から呼ぶ） |
| バージョンチェック | `net.minecraftforge.common.ForgeVersion`（`spoon.update.*` のキー） |
| クラッシュレポートの原因らしい Mod | `net.minecraftforge.logging.CrashReportAnalyser`、`CrashReport.java.patch` |
| クラッシュ時の画面 | `net.minecraftforge.client.CrashWindow`（`Minecraft.displayCrashReport` から呼ぶ。設定は `crashWindow`） |
| ワールドのバックアップ | `net.minecraftforge.common.util.WorldBackup`、`net.minecraftforge.client.gui.GuiBackupWorld`、`GuiSelectWorld.java.patch` |
| FML の自動バックアップ | `cpw.mods.fml.common.ZipperUtil`（`WorldBackup` を使う）、`GuiOldSaveLoadConfirm` |
| 止まらないスクリーンショット | `net.minecraftforge.client.ScreenshotSaver`（F2 のときだけ。`ScreenShotHelper` はそのまま） |
| 日本語化 | 言語ファイル、`FMLScreenText` |

## 今の状態（2026-11-09）

- 最新のリリースは 1.0.0-beta.5。タグは `c14fbaf4d` で、`spoon` の HEAD と同じなので、未リリースの変更はない。
- Web サイトは `d516bff`。`promotions_slim.json` の `1.7.10-latest` は `1.0.0-beta.5`。
- 2 つの Prism インスタンスには、どちらも正式な beta.5 が入っている。
- `.claude/worktrees/practical-herschel-b0371d` は、ほかのセッションが残した古い worktree（beta.1 のころ、detached）。
  検索するとここの古いコピーも見つかるので、除いて読む。

## 次の機能の候補

ユーザーが選んだものだけを作る。

1. **起動完了の通知:** 起動に数分かかる Modpack（ATM0 など）向け。起動が終わったときにゲームのウィンドウが前面にないなら、
   タスクバーのアイコンを点滅させる。
   - Windows の `FlashWindowEx` を JNA で呼ぶ。`IMEHandler` も JNA を使っている。
   - Windows 以外では何もしない。
2. **メモリー不足の警告:** Java に割り当てたメモリーが Mod の数に対して少ないとき、タイトル画面とログで知らせる。
   - Prism は初期値が多めなので、Prism の利用者にはあまり効かない。
