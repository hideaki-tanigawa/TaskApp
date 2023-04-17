package jp.techacademy.hideaki.tanigawa.taskapp

import java.io.Serializable
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class Task : RealmObject, Serializable {
    // idをプライマリーキーとして設定
    @PrimaryKey
    var id = 0

    var title = "" // タイトル
    var category = "" // カテゴリ
    var contents = "" // 内容
    var date = "" // 日時
}