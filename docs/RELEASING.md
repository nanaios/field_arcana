# 公開の手順（メンテナー向け）

Field Arcana を GitHub Releases・Modrinth・CurseForge に公開する手順です。
Field Theory も同じ手順で公開します（[field_theory の docs/RELEASING.md](https://github.com/nanaios/field_theory/blob/main/docs/RELEASING.md)）。

## いつもの公開

**先に Field Theory を公開してください。** Field Arcana は、`gradle.properties` の `field_theory_version` の版の Field Theory（タグ `v<版>`）と組み合わせてビルドします。その版が未公開だと、最初の確認で止まります。

1. `gradle.properties` の `mod_version` を上げる（Field Theory を上げたなら `field_theory_version` と `field_theory_version_range` も）
2. `CHANGELOG.md` に `## <版>` の節を書く（その節が各サイトの変更履歴になる）
3. 1 と 2 を main に入れる
4. GitHub の **Actions → Release → Run workflow** を押す（ブランチは `main`、種類は既定で `release`）

ワークフロー（`.github/workflows/release.yml`）は次を順に行います。

- 設定の確認。main 以外から実行した場合、その版がすでに公開済み（タグ `v<版>` がある）場合、Field Theory の版が未公開の場合は止まる
- Field Theory のタグを取ってきて Maven Local に入れ、Field Arcana をビルドして GameTest
- GitHub Releases・Modrinth・CurseForge への公開。タグ `v<版>` もこのとき自動で作られる

公開の種類は、Run workflow の画面で `beta` や `alpha` にも変えられます。

## 最初に一度だけ

### 1. 各サイトにプロジェクトを作る

- Modrinth と CurseForge で「Field Arcana」のプロジェクトを作る。アイコンは `docs/icon.png`（512×512）
- Modrinth の URL（スラッグ）は `field-arcana` にする。別のスラッグにした場合は、`release.yml` の `MODRINTH_PROJECT` を書き換える
- CurseForge のプロジェクト ID（プロジェクトページに表示される数字）を、`release.yml` の `CURSEFORGE_PROJECT` に書く
- Field Theory のスラッグを `field-theory` 以外にした場合は、`release.yml` の `dependencies` の `field-theory` も書き換える

### 2. トークンを登録する

リポジトリの **Settings → Secrets and variables → Actions → New repository secret** で、次の 2 つを登録します。
field_theory に登録したものと同じトークンで構いません（シークレットはリポジトリごとなので、両方に登録が必要です）。

| 名前 | 中身 |
|---|---|
| `MODRINTH_TOKEN` | Modrinth の個人アクセストークン（PAT）。権限は「Create versions」「Read projects」「Write versions」 |
| `CURSEFORGE_TOKEN` | CurseForge の API トークン（CurseForge のアカウント設定の API Tokens で作る） |

GitHub 側の設定（Variables など）はほかに要りません。

### 段階的に始められます

トークンが登録されていないサイトは自動で飛ばされます（`mc-publish` の仕様）。

- 何も登録しなくても、GitHub Releases への公開は動く
- `MODRINTH_TOKEN` だけ登録すれば、GitHub と Modrinth に公開する
- `CURSEFORGE_TOKEN` を登録したのに `CURSEFORGE_PROJECT` が空だと、最初の確認で止まる（ID の書き忘れを防ぐため）

## 依存関係の登録

公開する版には、次の依存関係を付けます（`release.yml` の `dependencies`）。

- Field Theory: 必須
- JEI: 任意

依存先はスラッグで指定します。依存先のプロジェクトがまだ審査中などで見つからないときは、その依存関係だけ付けずに公開されます（`mc-publish` の仕様）。

## プロジェクト ID について

| サイト | 必要なもの | 理由 |
|---|---|---|
| Modrinth | スラッグ（`field-arcana`）で足りる | Modrinth の API はスラッグでもプロジェクトを引ける |
| CurseForge | 数値の ID | スラッグで引けるのは、審査を通って公開されたプロジェクトだけ。最初の公開は審査前なので、ID でないと失敗する |

どちらも秘密の情報ではないので、GitHub の設定ではなく `release.yml` に直接書いています。

## うまくいかないとき

- **「already released」で止まる**: `mod_version` を上げ忘れている
- **「Field Theory vX is not released yet」で止まる**: 先に Field Theory を公開する
- **Modrinth で project not found**: `MODRINTH_PROJECT` のスラッグが実際のものと違う、またはトークンに「Read projects」の権限がない
- **一部のサイトだけ失敗した**: 失敗したサイトのトークンや ID を直す。その版はタグが作られて「公開済み」扱いになるので、同じ版をもう一度流すには、GitHub の Releases からその版のリリースとタグを消してから実行する（Modrinth・CurseForge にすでに上がった分は、重複しないように各サイトで消す）
