package jp.techacademy.hideaki.tanigawa.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewParent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import jp.techacademy.hideaki.tanigawa.taskapp.databinding.ActivityInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputBinding

    private lateinit var realm: Realm
    private lateinit var realm2: Realm
    private lateinit var task: Task
    private var calendar = Calendar.getInstance()
    private var categoryId:Int = 0
    private var categoryNo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーの設定
        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // ボタンのイベントリスナーの設定
        binding.content.dateButton.setOnClickListener(dateClickListener)
        binding.content.timeButton.setOnClickListener(timeClickListener)
        binding.content.doneButton.setOnClickListener(doneClickListener)
        binding.content.categoryAddButton.setOnClickListener(addClickListener)

        // EXTRA_TASKからTaskのidを取得
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)

        // TaskのRealmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Task::class))
        realm = Realm.open(config)

        val config2 = RealmConfiguration.create(schema = setOf(Category::class))
        realm2 = Realm.open(config2)

        // タスクを取得または初期化
        initTask(taskId)
    }

    override fun onResume() {
        super.onResume()
        // スピナーの取得
        val spinner = findViewById<Spinner>(R.id.category_edit_text)

        setSpinner(spinner)

        // 選択されたアイテムの変更を検知する
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val id = parent?.selectedItemId
                categoryId = id!!.toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("SpinnerResult","何も選択されませんでした")
            }
        }
        spinner.setSelection(categoryNo)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Realmデータベースとの接続を閉じる
        realm.close()
    }

    /**
     * 日付選択ボタン
     */
    private val dateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                setDateTimeButtonText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }



    /**
     * 時刻選択ボタン
     */
    private val timeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                setDateTimeButtonText()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )
        timePickerDialog.show()
    }

    /**
     * 決定ボタン
     */
    private val doneClickListener = View.OnClickListener {
        CoroutineScope(Dispatchers.Default).launch {
            addTask()
            finish()
        }
    }

    /**
     * カテゴリ追加ボタン
     */
    private val addClickListener = View.OnClickListener {
        val intent = Intent(this, CategoryInput::class.java)
        startActivity(intent)
    }

    /**
     * タスクを取得または初期化
     */
    private fun initTask(taskId: Int) {
        // スピナーの取得
        val spinner = findViewById<Spinner>(R.id.category_edit_text)
        // 引数のtaskIdに合致するタスクを検索
        val findTask = realm.query<Task>("id==$taskId").first().find()

        if (findTask == null) {
            // 新規作成の場合
            task = Task()
            task.id = -1

            // 日付の初期値を1日後に設定
            calendar.add(Calendar.DAY_OF_MONTH, 1)

        } else {
            // 更新の場合
            task = findTask

            // taskの日時をcalendarに反映
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
            calendar.time = simpleDateFormat.parse(task.date) as Date

            // taskの値を画面項目に反映
            binding.content.titleEditText.setText(task.title)
            binding.content.contentEditText.setText(task.contents)
            categoryNo = task.category
        }

        // 日付と時刻のボタンの表示を設定
        setDateTimeButtonText()
    }

    /**
     * タスクの登録または更新を行う
     */
    private suspend fun addTask() {
        // 日付型オブジェクトを文字列に変換用
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)

        // 登録（更新）する値を取得
        val title = binding.content.titleEditText.text.toString()
        val content = binding.content.contentEditText.text.toString()
        val date = simpleDateFormat.format(calendar.time)

        if (task.id == -1) {
            // 登録

            // 最大のid+1をセット
            task.id = (realm.query<Task>().max("id", Int::class).find() ?: -1) + 1
            // 画面項目の値で更新
            task.title = title
            task.contents = content
            task.category = categoryId
            task.date = date

            // 登録処理
            realm.writeBlocking {
                copyToRealm(task)
            }
        } else {
            // 更新
            realm.write {
                findLatest(task)?.apply {
                    // 画面項目の値で更新
                    this.title = title
                    this.contents = content
                    this.category = categoryId
                    this.date = date
                }
            }
        }

        // タスクの日時にアラームを設定
        val intent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        intent.putExtra(EXTRA_TASK, task.id)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, null), pendingIntent)
    }

    /**
     * 日付と時刻のボタンの表示を設定する
     */
    private fun setDateTimeButtonText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
        binding.content.dateButton.text = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)
        binding.content.timeButton.text = timeFormat.format(calendar.time)

    }

    /**
     * Spinnerの設定
     */
    private fun setSpinner(spinner: Spinner){
        val categoryList = realm2.query<Category>().find()
        val list = mutableListOf<String>()
        for (i in categoryList.indices){
            list.add(categoryList[i].category_name)
        }

        // アダプタ作成
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            list,
        )

        // AdapterをSpinnerのAdapterとして設定
        spinner.adapter = adapter

        // 選択肢の各項目のレイアウト
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}