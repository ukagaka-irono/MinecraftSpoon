*** SPOON ***
Spoon は Minecraft 1.7.10 用の Minecraft Forge 10.13.4.1614 から派生したフォークです。
リポジトリ: https://github.com/ukagaka-irono/MinecraftSpoon
English: README.txt

Spoon は Forge と FML のパッケージ（net.minecraftforge, cpw.mods.fml）、"Forge" と "FML" の Mod ID、
Forge のバージョン番号をそのまま残しています。そのため、既存の 1.7.10 用 Forge Mod を変更せずに読み込めます。

現在はベータ版です。試す前にワールドをバックアップしてください。
不具合は https://github.com/ukagaka-irono/MinecraftSpoon/issues に報告してください。


*** 導入 ***
配布ファイルはリリースページからダウンロードできます:
  https://github.com/ukagaka-irono/MinecraftSpoon/releases

Forge 1.7.10 と同じく、Java 8 で起動してください。

Prism Launcher で使う場合（Spoon はこの構成で動作を確認しています）:
  1. spoon-<バージョン>-prism.zip をダウンロードします。
  2. Prism で「インスタンスを追加」（Add Instance）>「インポート」（Import）を選び、zip を指定します。
     Minecraft 1.7.10 と必要なライブラリは Prism がダウンロードします。
  3. Mod はインスタンスの mods フォルダーに入れます。ゲーム内の Mod 一覧にある「Mod フォルダーを開く」
     ボタンからも開けます。
  Prism が Java 8 を選ばないときは、インスタンスの設定で Java 8 を指定してください。

公式ランチャーで使う場合（インストーラー）:
  1. 公式ランチャーで Minecraft 1.7.10 を一度起動しておきます。
  2. spoon-<バージョン>-installer.jar を Java 8 で実行し、「Install client」を選びます。
  3. ランチャーに "Spoon" という起動構成が追加されます。
  インストーラーは Forge 1.7.10 のものを Spoon 用に変えたものです。

spoon-<バージョン>-universal.jar は Spoon 本体です。手作業で組み込むときに使います。


*** Spoon の変更点 ***
  - 表示: タイトル画面と F3 画面に「Spoon <バージョン> (Forge 10.13.4.1614)」と表示されます。
    クライアントとサーバーのブランド名は "fml,forge,spoon" です。
  - Minecraft Realms を削除しました。タイトル画面には Realms ボタンの代わりに、横幅いっぱいの
    「Mods」ボタンが表示されます。
  - Windows での日本語・中国語・韓国語 IME への対応:
      - 文字入力欄にフォーカスがないあいだは IME をゲーム画面から切り離します。IME をオンにしたままでも
        WASD などの操作キーが効きます。
      - IME で確定した文字が、チャット、看板、その他の文字入力欄に入ります。
      - 変換中の文字と変換候補のウィンドウが、入力位置（キャレット）のそばに開きます。
    この機能は下の「設定」にある「IME 制御」で止められます。
  - 看板: 矢印キー、Home/End、Delete で動かせるカーソルがあり、行の途中に文字を挿入できます。
    看板の幅に収まらない文字は入力できません。
  - 全角英数字（ｋ など）の幅を、実際に描画される幅で計算します。全角英数字を含む中央揃えの文字列が
    左にずれなくなります。
  - Mod Menu 風の Mod 一覧: 左側に検索、並べ替え、ロゴアイコン、バッジがあり、右側に詳細、「Web サイト」と
    「設定」のボタン、クリックできるリンク、スクロールする説明文があります。「Mod フォルダーを開く」ボタンも
    あります。この一覧は Forge の GuiModList の代わりに開く別の画面です。GuiModList 自体は変更していないので、
    EnderCore のように GuiModList を拡張する Mod もそのまま動きます。
  - 起動の高速化: Mod を探すときにメソッドの中身を読み飛ばします。読み込みの各段階にかかった時間を
    "Spoon startup: ..." としてログに出力します。
  - Forge のバージョンチェックは無効です。Spoon にはまだ更新サーバーがありません。新しいバージョンは
    リリースページで確認してください。


*** 設定 ***
IME 制御（config/forge.cfg の general にある imeControl、初期値 true）
  Windows でのみ働きます。false にすると、Spoon は IME を切り離さなくなります。
  タイトル画面の「Mods」で Spoon を選び、「設定」>「一般設定」>「IME 制御」から変更できます。
  タイトル画面で変更した場合はすぐに反映されます。


*** ビルド ***
必要なもの:
  - JDK 8。Minecraft 1.7.10 と ForgeGradle 1.2 は、より新しい JDK ではビルドできません。
  - 初回のセットアップ時のインターネット接続（Gradle、ForgeGradle、Minecraft と MCP のファイルを取得します）。

ビルドには GTNewHorizons がフォークした ForgeGradle 1.2（com.github.GTNewHorizons:ForgeGradle:1.2.13）を
Gradle 4.10.3 で使います。Gradle は Gradle Wrapper がダウンロードします。Gradle は更新しないでください。
このフォークの forgedev タスクは、Gradle 5 で削除された Task.execute() を呼んでいます。フォークを動かすための
回避策は build.gradle、settings.gradle（eclipse/Clean と eclipse/Forge をサブプロジェクトとして登録）、
buildSrc（フォークの ZipFileTree を修正したもの）にあります。

Wrapper を実行する前に、JAVA_HOME を JDK 8 に向けてください。例:
  Windows (PowerShell): $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot"
  MacOS/Linux:          export JAVA_HOME=/path/to/jdk8

ワークスペースをセットアップします（Minecraft を逆コンパイルし、FML と Forge のパッチを当てます）:
  Windows:     .\gradlew.bat setupForge
  MacOS/Linux: ./gradlew setupForge

universal jar とインストーラーをビルドします（出力先は build/distributions）:
  Windows:     .\gradlew.bat packageUniversal packageInstaller
  MacOS/Linux: ./gradlew packageUniversal packageInstaller

Prism Launcher のインスタンス（build/distributions/spoon-...-prism.zip）をビルドします:
  Windows:     .\gradlew.bat packagePrism
  MacOS/Linux: ./gradlew packagePrism

ワークスペースからクライアントを起動します（-PmcUsername=<名前> も指定できます）:
  Windows:     .\gradlew.bat runClient
  MacOS/Linux: ./gradlew runClient

eclipse/Forge の Minecraft のクラスを変更したら、"gradlew genPatches" でパッチを作り直してください。
コミットする前に結果を確認してください。このフォークは、触る必要のなかったパッチに MCP の名前
（たとえば WorldType.DEFAULT_1_1）を残すことがあります。変更するつもりだったパッチだけを残し、
ほかは git で元に戻してください。
Eclipse を使う場合は、Eclipse のワークスペースをリポジトリ内の eclipse フォルダーにしてください。


*** ライセンス ***
Spoon は Minecraft Forge と同じ条件で配布しています。MinecraftForge-License.txt（Forge Public License）と
MinecraftForge-Credits.txt を参照してください。FML は LGPL です。fml/LICENSE-fml.txt を参照してください。
Paulscode のライセンスファイルは、同梱しているサウンドライブラリに適用されます。
