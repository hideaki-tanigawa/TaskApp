package jp.techacademy.hideaki.tanigawa.taskapp

import io.realm.kotlin.types.RealmObject

open class Category : RealmObject, java.io.Serializable {
    // idをプライマリキーとして設定
    var id = 0

    var category_name = "" // カテゴリ名
}