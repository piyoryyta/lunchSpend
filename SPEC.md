# 食費・ランチ管理アプリ 仕様書 (SPEC.md)

## 1. 概要 (Overview)

### 1.1 目的

自分専用（個人利用）の食費・ランチ支出管理 Android アプリ。
コンビニ・スーパー等で箱買い・まとめ買いした食品を「商品」として登録し、日々の消費（精算）を記録することで、実際に消費した分の原価を正確に把握する。

臨時の値引き購入と通常価格の購入が混在しても、精算時の原価計算は「商品の現在の基準価格」ではなく「実際に消費した在庫ロットの購入時単価」に基づいて正確に行う。これが本仕様の技術的な核心（在庫ロット単位でのFIFO会計）。

### 1.2 スコープ

- 単一ユーザー、ローカル動作のみ（サーバー・アカウント機能なし）
- オフラインファースト。クラウド同期は本バージョンでは対象外（7章の将来拡張として触れるのみ）
- プラットフォーム: Android（Kotlin, Jetpack Compose 前提）
- ローカル永続化: Room (SQLite)
- 対象外: 複数端末同期、共有、レシートOCR、通知/リマインダー（将来検討）

### 1.3 用語集

| 用語 | 英語/技術名 | 説明 |
|---|---|---|
| 商品 | Product | 継続的に買う食品のマスタ登録（基準価格・入り数） |
| 購入（仕入れ） | Purchase / StockLot | 商品を実際に買った記録。在庫ロットを1件生成する |
| 在庫ロット | StockLot | 1回の購入で生成される在庫の単位。残数と購入時単価を持つ |
| 精算 | Settlement / DailyRecord | 1日分の「何をいくつ消費したか」の記録 |
| 精算明細 | SettlementLineItem | 精算の中の1商品分の消費行。商品ベース or 都度（ad-hoc） |
| 都度アイテム | Ad-hoc Item | 商品未登録のその場買い（コンビニ惣菜等）を精算時に直接入力するもの |
| FIFO消費 | FIFO Consumption | 在庫ロットを購入順（古い順）に消費するロジック |

---

## 2. 機能要件 (Functional Requirements)

### 2.1 商品機能 (Product Management)

- 商品を登録・編集・削除（論理削除を推奨、履歴保持のため）できる
- 登録項目: 商品名、基準価格（デフォルト値段）、1パッケージあたりの入り数（unitsPerPackage）、任意でカテゴリ・メモ
- 基準価格はあくまで「次に購入する時のデフォルト値」であり、実際の原価計算は各購入（在庫ロット）ごとの実売価格を使う（詳細は 4.5）
- 商品一覧はアクティブ/アーカイブでフィルタ可能

### 2.2 購入機能 (Purchase / Restock) — 商品機能のサブ機能

- 商品を選んで「購入」を記録する
- 購入時、値段を臨時で上書き可能（セール価格・値上がり等に対応）。指定しない場合は商品の基準価格を使用
- 購入数量はパッケージ数で入力する（内部的には「入り数」を使って個数に変換して管理する。4.1の設計判断を参照）
- 購入記録は「在庫ロット (StockLot)」を1件生成し、在庫個数を増加させる
- 各ロットは購入時点の実効単価を保持する（後から基準価格を変えても過去ロットのコストは変わらない）

### 2.3 精算機能 (Settlement)

- 日付ごとに「商品 × 消費数量」のリストを登録する（**1日1件の DailyRecord**、複数の明細を持つ。朝・昼・夜のような分割記録は行わない）
- 商品ベースの明細:
  - 在庫があれば FIFO で在庫ロットから消費し、消費原価を自動計算
  - 在庫が不足する場合は不足分を商品の基準価格で自動補完する（4.4参照。登録操作自体はブロックしない）
- 都度アイテム（ad-hoc）明細:
  - 商品マスタに存在しない、その場買いの品（コンビニ惣菜等）を、名前と「その場で払った金額」をそのまま入力して記録する（単価×数量の概念は持たない、1行=1金額のシンプルな記録）
  - 任意で「商品として登録する」ボタンにより Product 化できる（4.6参照）
- 精算は後から編集・削除できる（4.7参照）

### 2.4 閲覧機能 (Browse / View)

- 日次ビュー: 特定日の精算内容（商品別内訳、都度アイテム、合計金額）
- 月次サマリー: 月ごとの合計支出、商品カテゴリ別内訳、日別グラフ
- 在庫一覧: 商品ごとの現在庫数、残存ロットと単価
- 購入履歴: 過去の購入（ロット）一覧、フィルタ（商品別、期間別）。不要になった購入記録はユーザーが任意のタイミングで削除できる（4.8参照）
- 商品一覧・編集: 商品マスタのCRUD画面
- 精算履歴カレンダー: 月間カレンダー表示で記録有無・金額を一覧

---

## 3. データモデル (Data Model)

Room (SQLite) を前提とした Entity 設計。

### 3.1 Product（商品マスタ）

| フィールド | 型 | 説明 |
|---|---|---|
| id | Long (PK, autoGenerate) | |
| name | String | 商品名 |
| defaultUnitPrice | Int（円、最小通貨単位で整数管理） | デフォルトのパッケージ単価 |
| unitsPerPackage | Int | 1パッケージあたりの個数（例: 6個入り） |
| category | String? | 任意カテゴリ |
| memo | String? | メモ |
| isArchived | Boolean (default false) | 論理削除フラグ |
| createdAt | Long (epoch millis) | |
| updatedAt | Long (epoch millis) | |

- 派生値（保存はしないが表示に使用）: `defaultUnitCostPerPiece = defaultUnitPrice / unitsPerPackage`

### 3.2 StockLot（在庫ロット＝購入記録を兼ねる）

| フィールド | 型 | 説明 |
|---|---|---|
| id | Long (PK) | |
| productId | Long (FK → Product.id) | |
| purchasedAt | Long (epoch millis) | 購入日時。FIFO順序の基準 |
| packageCount | Int | 購入したパッケージ数 |
| unitPriceAtPurchase | Int | このロットの実効パッケージ単価（臨時変更があればその値、なければ商品の基準価格をコピー） |
| unitsPerPackageAtPurchase | Int | 購入時点の入り数のスナップショット（Productの入り数が将来変更されても過去ロットの計算に影響しないようにする） |
| totalPieces | Int | packageCount × unitsPerPackageAtPurchase |
| remainingPieces | Int | 未消費の個数。消費のたびに減算 |
| memo | String? | 例:「セール」「まとめ買い」等 |

- `unitCostPerPiece = unitPriceAtPurchase / unitsPerPackageAtPurchase`（ロットごとに固定。これがFIFO原価計算のキモ）
- FIFO順序は `purchasedAt ASC`、同時刻なら `id ASC`（挿入順）で一意に順序付けする

**設計判断**: 数量の内部単位は「個 (piece)」で統一する。購入はパッケージ単位で入力されるが、`unitsPerPackageAtPurchase` を使って個数に変換して `totalPieces` / `remainingPieces` を管理する。精算の消費もすべて「個」単位で扱う。バラ売り商品は `unitsPerPackage=1` として扱えば同じロジックで統一できる。

### 3.3 DailyRecord（精算＝日次記録）

| フィールド | 型 | 説明 |
|---|---|---|
| id | Long (PK) | |
| date | String ("yyyy-MM-dd") または epoch day (Int) | 精算対象日。**一意制約**（1日1レコード） |
| memo | String? | |
| createdAt / updatedAt | Long | |

### 3.4 SettlementLineItem（精算明細）

| フィールド | 型 | 説明 |
|---|---|---|
| id | Long (PK) | |
| dailyRecordId | Long (FK → DailyRecord.id) | |
| type | Enum: PRODUCT / AD_HOC | 明細種別 |
| productId | Long? (FK → Product.id、PRODUCT時のみ) | |
| quantity | Int? (PRODUCT時のみ) | 消費数量（個数） |
| adHocName | String? (AD_HOC時のみ) | その場買い品の名前 |
| adHocPaidAmount | Int? (AD_HOC時のみ) | その場で払った金額そのまま（単価×数量ではない） |
| totalCost | Int | この明細の確定原価（PRODUCT: FIFO計算結果の合計。AD_HOC: adHocPaidAmountと同値） |
| createdAt | Long | |

在庫不足時の警告は精算登録操作を行っているその場でダイアログ表示するだけの一過性のUIであり、明細データとして永続化しない（フィールドを持たない。4.4参照）。

### 3.5 StockConsumption（FIFO消費の内訳、監査・トレーサビリティ用）

在庫がどのロットからどれだけ消費されたかを明細化する中間テーブル。これがないと精算の編集・削除時に在庫を正しく戻せない。

| フィールド | 型 | 説明 |
|---|---|---|
| id | Long (PK) | |
| settlementLineItemId | Long (FK → SettlementLineItem.id) | |
| stockLotId | Long (FK → StockLot.id) | どのロットから消費したか |
| consumedPieces | Int | このロットから消費した個数 |
| unitCostPerPieceSnapshot | Int | 消費時点のそのロットの単価（= ロットのunitCostPerPiece。冗長だが履歴保護のため保存） |

- `SettlementLineItem.totalCost`（PRODUCT時）= Σ(StockConsumption.consumedPieces × unitCostPerPieceSnapshot) + 在庫不足分の基準価格計上額

### 3.6 エンティティ関連図（テキスト表現）

```
Product 1---N StockLot
Product 1---N SettlementLineItem (type=PRODUCT)
DailyRecord 1---N SettlementLineItem
SettlementLineItem 1---N StockConsumption
StockConsumption N---1 StockLot
```

---

## 4. コアロジック仕様 (Core Logic)

### 4.1 FIFO消費アルゴリズム

商品ベースの精算明細を1件登録する際:

1. 対象 productId の StockLot を `remainingPieces > 0` の条件で `purchasedAt ASC, id ASC` の順に取得する
2. 必要数量 `quantity` を先頭ロットから順に減算していく
   - 各ロットで `consume = min(remainingPieces, 残り必要数)`
   - StockConsumption レコードを作成し、StockLot.remainingPieces を `consume` 分減算する
   - 累積 `totalCost += consume × unitCostPerPiece`
3. 全ロットの remainingPieces 合計 < quantity の場合は 4.4 の在庫不足ルールを適用する
4. トランザクション内で実行し、途中失敗時は全ロールバックする（Room の `@Transaction` を使用）

### 4.2 臨時売価とFIFOコストの関係

- 各購入は独立した StockLot として保存され、`unitPriceAtPurchase` を個別に持つため、同じ商品でも購入タイミングによって単価が異なるロットが並存する
- FIFO消費時は「どの単価だったか」をロット単位で正確に追跡するため、Product.defaultUnitPrice を後から変更しても過去ロットの原価計算には一切影響しない
- 計算例: 6個入り500円の商品を通常購入（500円ロット、単価83.3円/個）、次にセールで400円購入（400円ロット、単価66.7円/個）。精算で8個消費すると、先に500円ロットの6個分（500円）、次に400円ロットの2個分（約133円）が消し込まれ、合計約633円が原価となる

### 4.3 端数処理

- 円は整数（Int, 最小通貨単位）で保存するが、`unitPriceAtPurchase / unitsPerPackageAtPurchase` は割り切れないことが多い（例: 500円/6個=83.33...円）
- 方針: `unitCostPerPiece` は内部的に小数（例: 1/1000円単位の整数）で保持し、消費時の積算後、**明細（SettlementLineItem）単位で最後に1回だけ**四捨五入して整数円に丸める。ロットごとに個別に丸めると誤差が蓄積するため避ける

### 4.4 在庫不足時の扱い（確定仕様）

在庫があるだけFIFOで消費し、不足する分は商品の `defaultUnitPrice` ベースの原価で自動補完する。登録操作自体はブロックしない。

警告表示は、精算登録操作を行っているその場（同一画面）で「在庫が不足しています」といった内容をダイアログ表示するのみに留め、明細データに警告フラグ等を持たせて永続化することはしない。精算を保存した時点でその情報は不要になる、という位置づけ。

### 4.5 商品価格の履歴性

- Product.defaultUnitPrice はあくまで「次に購入する時のデフォルト初期値」。過去の精算原価はすべてStockLot / StockConsumptionのスナップショット値に基づくため、Product編集は過去記録に影響しない
- Product編集時は `updatedAt` を更新するのみで、価格履歴テーブルは持たない（必要になれば `ProductPriceHistory` を将来追加検討）

### 4.6 都度アイテム(Ad-hoc)から商品への昇格

精算画面でad-hocアイテムを入力後、「商品として登録」を選択すると:

1. Productレコードを新規作成する（name=adHocName、defaultUnitPrice=adHocPaidAmount、unitsPerPackageは初期値1でユーザーが編集可能なダイアログを出す）
2. **在庫ロットは作らない**（すでに消費済みのため、新規在庫は0からスタートする）
3. 既存のSettlementLineItem（type=AD_HOC）はそのまま残す。過去の記録は書き換えない

これにより、次回からは「購入 → 精算」の通常フローに移行できる。

### 4.7 精算の編集・削除

- 精算明細を削除・編集する場合、関連するStockConsumptionを取り消し、対応するStockLot.remainingPiecesを復元してから再計算する
- DailyRecordごとトランザクションで扱い、整合性を保証する

### 4.8 削除の制約

**StockLot（購入履歴）はユーザーが不要と判断した時点でいつでも削除できる**（消費済みかどうかに関わらず、購入履歴一覧から削除操作を提供する）。

- 削除時、`remainingPieces > 0` の分は在庫数からそのまま減算される
- 既に消費済みの部分については、その消費を裏付ける `StockConsumption` 監査行も一緒に削除される
- ただし `SettlementLineItem.totalCost` は消費確定時点の金額をスナップショットとして保持しているため、過去の精算記録の表示金額自体には影響しない。失われるのは「どのロット由来か」というトレーサビリティのみ
- 削除前には、この影響範囲（在庫が減ること、消費済みロットの場合はトレーサビリティが失われること）をダイアログで警告する

Productは、関連するStockLot / SettlementLineItemが存在する場合は物理削除不可とし、`isArchived=true` の論理削除のみ許可する。

### 4.9 具体例: 商品登録 → 購入 → 消費のフロー

1. **商品登録**: 「お茶 6本入り」を登録する。`defaultUnitPrice=600円`, `unitsPerPackage=6`（1本あたり100円相当）
2. **購入①（通常価格）**: 1パック購入。臨時単価は指定せず基準価格600円のまま → `StockLot A`（`unitPriceAtPurchase=600`, `totalPieces=6`, `remainingPieces=6`）が生成され、在庫は6本になる
3. **購入②（セールで臨時単価変更）**: 数日後、同じ商品をセール480円で1パック購入 → `StockLot B`（`unitPriceAtPurchase=480`, `unitsPerPackageAtPurchase=6`, `totalPieces=6`, `remainingPieces=6`）が生成され、在庫は合計12本になる。`Product.defaultUnitPrice` は600円のまま変更されない
4. **精算（消費・FIFOロット跨ぎ）**: ある日、この商品を8本消費として精算登録する → FIFOで購入日が古い`StockLot A`から優先消費: Aから6本（100円/本×6=600円）、残り2本をBから消費（80円/本×2=160円）。`StockConsumption`が2行作成され（A由来6本、B由来2本）、`SettlementLineItem.totalCost = 760円`が保存される。消費後 `StockLot A.remainingPieces=0`、`StockLot B.remainingPieces=4`
5. **精算（在庫不足）**: 別の日、同じ商品を10本消費として登録したが、残り在庫はB由来の4本のみ → 4本はBからFIFO消費（80円/本×4=320円）、不足する6本は`Product.defaultUnitPrice`基準の100円/本で計上（600円）→ `totalCost=920円`。登録操作時に「在庫が不足しています」とダイアログ警告を出すのみで、明細には警告データを残さない
6. **都度アイテム**: 別の日、コンビニでおにぎりを買った場合は商品登録せず、精算画面で直接「おにぎり／180円」を都度アイテムとして1行追加する。在庫やロットは一切関与しない
7. **都度アイテムの商品昇格**: 頻繁に買うと分かったので、上記「おにぎり」を商品として登録し直す（`name=おにぎり, defaultUnitPrice=180, unitsPerPackage=1`）。過去の都度アイテム明細はそのまま残り、以後は通常の「購入 → 精算」フローに乗せられる

---

## 5. 画面仕様 (Screens / UI Flow)

### 5.1 画面一覧

1. **ホーム / 今日の精算 (Today Settlement)**: 当日のDailyRecordを開き、商品検索して数量入力、または「都度入力」ボタンで登録
2. **商品一覧 (Product List)**: 検索・カテゴリフィルタ、タップで商品詳細へ
3. **商品詳細 (Product Detail)**: 基準価格・入り数編集、現在庫数、購入履歴、精算での消費履歴
4. **購入登録 (Purchase / Restock)**: 商品選択 → 数量・（任意）臨時単価入力 → 保存
5. **精算入力 (Settlement Entry)**: 日付選択、商品行の追加（商品ベース/都度）、合計金額のリアルタイム表示、保存
6. **日次詳細 (Daily Detail)**: 過去のある日の精算内容閲覧・編集
7. **月次サマリー (Monthly Summary)**: カレンダー + 月合計、カテゴリ別の円グラフ/棒グラフ
8. **在庫一覧 (Inventory List)**: 商品別の現在庫、ロット内訳（各ロットの残数・単価・購入日）
9. **購入履歴 (Purchase History)**: 全購入ロットの時系列一覧、商品/期間フィルタ。各ロットに削除操作を提供し、不要になった購入記録をユーザーが任意のタイミングで削除できる（4.8参照）

### 5.2 主要ユーザーフロー

**フロー1: 商品登録**
商品一覧 → [+追加] → 名前・基準価格・入り数入力 → 保存

**フロー2: 購入（仕入れ）**
商品詳細 or 購入登録画面 → 商品選択 → パッケージ数入力 → （任意）臨時単価入力 → 保存 → StockLot生成・在庫加算

**フロー3: 日次精算**
ホーム（今日の精算） → 商品検索して行追加（数量指定）→ FIFOで自動原価計算・表示 → 都度アイテムがあれば名前・金額を直接入力 → （任意）都度アイテムを商品に昇格 → 保存 → 在庫が消費される

**フロー4: 履歴閲覧**
月次サマリー → 日をタップ → 日次詳細（その日の内訳）→ 必要なら編集

---

## 6. 非機能要件・技術方針 (Non-functional / Tech Notes)

- 言語/UI: Kotlin, Jetpack Compose
- 永続化: Room (SQLite)。マイグレーション戦略（バージョン管理、破壊的変更を避ける）
- アーキテクチャ: MVVM + Repositoryパターンを推奨（ViewModel, Repository, Room DAOの分離）
- テスト: FIFOロジック（4.1〜4.4）はビジネスロジックの核心のため、in-memory Room DBを使ったユニットテストで厳密に検証する
- バックアップ: Room DBファイルのエクスポート/インポート（Android Auto Backup、または手動JSONエクスポート）を将来拡張として想定
- 通貨: 日本円のみ、整数運用（小数点処理は4.3に準拠）

---

## 7. 将来拡張候補（本仕様のスコープ外）

- 複数端末同期（クラウド）
- レシート写真添付・OCR
- 予算設定・アラート通知
- カテゴリ別月次比較グラフの高度化
- 商品価格の変動履歴グラフ（ProductPriceHistory）
