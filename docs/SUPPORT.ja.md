Spoon のサポートを受けるには
============================

[English](SUPPORT.md) | 日本語

このリポジトリの [Issues][Issues] に報告する前に、この文書を読んでください。問題の原因が Spoon、Mod、Minecraft 本体の
どれにあるのかを見分ける方法、報告先の選び方、報告に書く内容を説明します。

### 目次

 1. [問題の原因を見つける](#問題の原因を見つける)
    1. [スタックトレースを読む](#スタックトレースを読む)
    2. [Forge と比べる](#forge-と比べる)
    3. [問題を絞り込む](#問題を絞り込む)
 2. [報告先を選ぶ](#報告先を選ぶ)
 3. [役に立つ不具合報告を書く](#役に立つ不具合報告を書く)
    1. [重複を避ける](#重複を避ける)
    2. [必要な情報](#必要な情報)
    3. [Markdown の報告テンプレート](#markdown-の報告テンプレート)

問題の原因を見つける
--------------------

まずログファイルを見てください。ログはゲームディレクトリの `logs` フォルダーにあります。公式ランチャーでは
`.minecraft/logs`、Prism Launcher ではインスタンスのフォルダー内の `minecraft/logs`（または `.minecraft/logs`）です。

* `fml-client-latest.log`（サーバーでは `fml-server-latest.log`）は、FML、Forge、Spoon、すべての Mod のログを
  すべてのレベルで記録した完全なログです。**見るべきログ、報告に添付するログはこれです。**
* `latest.log` には Minecraft 本体のメッセージしか入っていません。
* クラッシュしたときは、`crash-reports` フォルダーにクラッシュレポート（`crash-2026-01-01_12.00.00-client.txt` など）
  も書き出されます。

クラッシュレポートの最初には、説明とスタックトレースがあります。スタックトレースは次のような行の並びです。

```
at some.words.related.to.a.mod.MoreWords(MoreWords.java:123)
```

いちばん上の行がゲームが止まった場所、いちばん下の行が処理の始まった場所です。`-- System Details --` の節の最後に
ある `FML:` の行には、Spoon のバージョンと読み込まれたすべての Mod が載っています。

新しい Forge と同じく、`-- Head --` の節の最初には原因らしい Mod（Suspected Mods）が載ります。スタックトレースに
コードが出てくる Mod（Mixin でほかのクラスに組み込まれたコードも含みます）と、それが最初に出てくる行です。

```
-- Head --
Suspected Mod: 
	Example Mod (examplemod), Version: 1.0
		Website: https://example.com/
		at com.example.ExampleBlock.onBlockActivated(ExampleBlock.java:42)
Stacktrace:
```

まずはここに載った Mod から調べてください。ただし、載った Mod がエラーを受け渡しただけのこともあるので、
スタックトレースも読んでください。

### スタックトレースを読む

次の順に当てはめてください。

 1. スタックトレースに Mod の名前があれば、その Mod が原因である可能性が高いです。原因らしい Mod の欄に、
    その Mod が載っています。複数の Mod が出てくる場合は、すべてに報告するか、名前からどれが原因かを考えてください。
 2. `net.minecraft` のほかに `net.minecraftforge` や `cpw.mods.fml` しか見つからない場合は、Spoon か Forge 自体に
    問題がある可能性があります。Spoon は Forge のパッケージ名を残しているので、Spoon のコードも同じ名前で出てきます。
    どちらなのかは [Forge と比べて](#forge-と比べる)見分けてください。
 3. はっきりした原因が見つからない場合は、まずコア Mod を疑ってください。コア Mod はゲームで動くどのコードでも
    書き換えられます。コア Mod は `fml-client-latest.log` に `Instantiating coremod class ...` という行で出てきます。

### Forge と比べる

Spoon が変えているのは Forge 10.13.4.1614 の一部だけです（[Spoon の変更点][Changes]を参照）。問題がその変更から
来ているかどうかは、同じ Mod と同じ手順を Forge 10.13.4.1614 で試すと分かります。Prism Launcher では、インスタンスを
コピーして、Spoon の代わりに Forge 10.13.4.1614 を入れられます。

* Forge でも同じ問題が起きるなら、原因は Spoon ではありません。Mod に報告してください。
* Spoon でだけ起きるなら、Spoon に報告してください。

### 問題を絞り込む

手順がはっきりしていて、関係する Mod が少ない報告ほど、直してもらえる見込みが高くなります。

* **問題が起きる直前の操作を振り返ってください。** スタックトレースの名前から、アイテムを使った、画面を開いた
  などのゲームの動きが分かることがよくあります。
* **Mod の数を減らしてください。** 1 つずつ外すのではなく、半分ずつ外していきます。
   1. 明らかに関係している Mod は残し、残りの半分をインスタンスから外します。
   2. まだ問題が起きるなら、残っている Mod で手順 1 を繰り返します。
   3. 問題が起きなくなったら、原因は外した Mod の中にあります。外した Mod を戻し、代わりに残っていた Mod を外して、
      戻した Mod で手順 1 を繰り返します。
   4. どの Mod を外しても問題が消えてしまうところまで来たら終わりです。その Mod の組み合わせで報告してください。

報告先を選ぶ
------------

* **Spoon:** Spoon でだけ起きる問題と、Spoon 独自の機能（IME 対応、看板、Mod 一覧、起動）の問題は、[Issues][Issues] に
  報告してください。
* **Mod:** Forge でも起きる問題は Mod に報告してください。Mod の [CurseForge] や [Modrinth] のページにある Issue の
  リンクを探すか、[GitHub] でリポジトリを検索してください。
* **Forge と Mojang:** どちらも Minecraft 1.7.10 をもう修正していません。Spoon の問題を報告しないでください。

役に立つ不具合報告を書く
------------------------

### 重複を避ける

[Issues][Issues] を、閉じたものも含めて検索し、同じ問題がないか確認してください。見つかったら、新しく作らずに、
再現する別の手順など新しい情報をそこにコメントしてください。
[最新のリリース][Download]とその変更履歴も確認してください。すでに直っているかもしれません。

### 必要な情報

 - **Spoon のバージョン:** タイトル画面に表示されるもの。例: `Spoon 1.0.0-beta.1 (Forge 10.13.4.1614)`
 - **ランチャーと Java のバージョン:** 例: Prism Launcher 9.4、Java 8u462
 - **OS:** 文字入力の問題では、使っている IME も（例: Microsoft IME）。
 - **Mod の完全な一覧:** バージョンも書いてください。[問題を絞り込んだ](#問題を絞り込む)場合は、残った Mod だけで
   かまいません。
 - **再現手順:** できるだけ短くしてください。特別な準備が必要な問題なら、ワールドを添付することも考えてください。
 - **完全なログ:** `fml-client-latest.log` と、あればクラッシュレポート。報告に直接貼らず、[GitHub Gist][Gist] に
   アップロードしてリンクしてください。

報告は日本語でも英語でもかまいません。

### Markdown の報告テンプレート

Issue の入力欄には、最初からこれと同じ項目のテンプレート（英語）が入っています。`{...}` をそれぞれの情報に置き換えて
ください。

```markdown
**Spoon Version:** {例: 1.0.0-beta.1}

**Launcher and Java:** {例: Prism Launcher, Java 8u462}

**OS and IME:** {例: Windows 11, Microsoft IME}

**Logs:** {fml-client-latest.log やクラッシュレポートを上げた GitHub Gist のリンク}

**Mods in This Instance:**
| Name    | Version     |
| ------- | ----------- |
| {mod 1} | {version 1} |
| {mod 2} | {version 2} |

**Steps to Reproduce:**
 1. これをする
 2. 次にこれをする
 3. ...

**Description of issue:**
```

[Issues]: https://github.com/ukagaka-irono/MinecraftSpoon/issues
[Download]: https://github.com/ukagaka-irono/MinecraftSpoon/releases
[Changes]: ./README.ja.md#spoon-の変更点

[CurseForge]: https://www.curseforge.com/minecraft/mc-mods
[Modrinth]: https://modrinth.com/mods
[GitHub]: https://github.com/search
[Gist]: https://gist.github.com/
