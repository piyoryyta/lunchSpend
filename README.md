# LunchSpend

食費・ランチ支出管理 Android アプリ。仕様は [SPEC.md](./SPEC.md) を参照。

## 技術スタック

- Kotlin 2.0 / Jetpack Compose (Material 3)
- Room (SQLite) によるローカル永続化
- MVVM + Repository パターン
- Gradle Version Catalog (`gradle/libs.versions.toml`) で依存バージョンを一元管理

## プロジェクト構成

```
app/src/main/java/com/piyoryyta/lunchspend/
  data/
    entity/       Room Entity (SPEC.md 3章のデータモデルにそのまま対応)
    dao/          Room DAO
    db/           RoomDatabase, TypeConverter
    repository/   Repository層 (ViewModelから利用する想定)
  ui/
    theme/        Compose のテーマ定義
    navigation/    画面遷移 (SPEC.md 5.1 の9画面ぶんのルートを定義)
    common/        共通Composable (現時点ではプレースホルダー画面)
  MainActivity.kt
  LunchSpendApplication.kt   DB・Repositoryの簡易DIコンテナ
```

## 現在のスコープ (このセットアップで完了していること)

- Gradle プロジェクトの土台 (ビルドスクリプト、Version Catalog、Gradle Wrapper)
- SPEC.md 3章のデータモデルに対応する Room Entity / DAO / Database
- SPEC.md 5.1 の9画面ぶんの Navigation ルートとプレースホルダー画面
- Room の in-memory DB を使ったユニットテストの土台 (Robolectric)

## 未実装 (今後のタスク)

コアロジック (SPEC.md 4章: FIFO在庫消費、在庫不足時の自動補完、端数処理、精算の編集・削除、
都度アイテムの商品昇格) は `StockRepository` / `SettlementRepository` に `TODO` コメントとして
記載してあり、次のフェーズで実装する。各画面の実UIも未実装 (プレースホルダーのみ)。

## ビルドについて

このリポジトリには Gradle Wrapper (`./gradlew`) を同梱しているが、ビルドには以下が必要:

- Android SDK (`local.properties` に `sdk.dir` を設定、または `ANDROID_HOME` を設定)
- `google()` (dl.google.com) への到達性 (AndroidX / AGP の取得に必要)

このセットアップを行った開発コンテナには Android SDK が無く、`dl.google.com` への
アウトバウンドアクセスもポリシーでブロックされているため、`./gradlew build` の実行検証は
行えていない。Android Studio、または Android SDK とフルインターネットアクセスのある CI で
ビルド確認すること。
