package jp.techacademy.hideaki.tanigawa.taskapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import jp.techacademy.hideaki.tanigawa.taskapp.databinding.ActivityCategoryInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryInput : AppCompatActivity() {
    
    private lateinit var binding: ActivityCategoryInputBinding
    private lateinit var realm: Realm
    private lateinit var category : Category
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar2)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // ボタンのイベントリスナーの設定
        binding.category.categoryRegiterButton.setOnClickListener(registerClickListener)

        // Realmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Category::class))
        realm = Realm.open(config)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Realmデータベースとの接続を閉じる
        realm.close()
    }

    /**
     * カテゴリ登録ボタンが押されたときに発火
     */
    private val registerClickListener = View.OnClickListener {
        // カテゴリ登録ボタンが押された時の処理
        CoroutineScope(Dispatchers.Default).launch {
            addCategory()
            finish()
        }
    }

    private suspend fun addCategory(){
        // カテゴリ登録ボタンが押された時の処理
        category = Category()
        val cateText = binding.category.categoryAddText.text.toString()
        category.id = (realm.query<Category>().max("id", Int::class).find() ?: -1) + 1
        category.category_name = cateText

        // 登録処理
        realm.writeBlocking {
            copyToRealm(category)
        }
    }
}