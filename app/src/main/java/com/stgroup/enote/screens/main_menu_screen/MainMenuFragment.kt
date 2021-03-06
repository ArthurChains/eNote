package com.stgroup.enote.screens.main_menu_screen

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.stgroup.enote.R
import com.stgroup.enote.database.*
import com.stgroup.enote.models.CategoryModel
import com.stgroup.enote.models.NoteModel
import com.stgroup.enote.objects.SearchEngine
import com.stgroup.enote.screens.main_menu_screen.search.SearchAdapter
import com.stgroup.enote.utilities.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_main_menu.*
import kotlinx.android.synthetic.main.toolbar_search.view.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: MainAdapter

    private lateinit var toolbarEditText: EditText
    private lateinit var toolbarClearButton: ImageView

    companion object {
        var categoryList: MutableList<CategoryModel> = mutableListOf()
        var noteList: MutableList<NoteModel> = mutableListOf()
    }

    private var searchList = listOf<NoteModel>()
    private lateinit var searchTextObservable: Observable<String>

    private lateinit var textWatcher: TextWatcher

    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchAdapter


    @SuppressLint("CheckResult")
    override fun onStart() {
        super.onStart()
        hideKeyboard()
        APP_ACTIVITY.mToolbar.search_toolbar.visibility = View.VISIBLE
        if (categoryList.isEmpty())
            initCategories()
        if (noteList.isEmpty())
            initNotes()

        initViews()

        initRecyclerView()
        initFunctions()
        if (CURRENT_UID != "null") {
            synchronizeCategories {
                compareLists(it)

                synchronizeNotes { downloadList ->
                    compareNotes(downloadList)
                }
            }
        }

        SEARCH_ENGINE = SearchEngine(noteList)
        searchTextObservable = createTextChangeObservable()
        searchTextObservable.observeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .map { SEARCH_ENGINE.search(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                searchList = it
                searchRecyclerView.visibility = View.VISIBLE
                mRecyclerView.visibility = View.INVISIBLE
                searchAdapter.updateData(searchList)
            }
    }

    override fun onStop() {
        super.onStop()
        toolbarEditText.removeTextChangedListener(textWatcher)
    }

    private fun initViews() {
        searchRecyclerView = search_notes_recycler_view
        searchAdapter = SearchAdapter(searchList)
        searchRecyclerView.layoutManager = LinearLayoutManager(context)
        searchRecyclerView.adapter = searchAdapter
        toolbarClearButton = APP_ACTIVITY.mToolbar.search_toolbar.clear_icon
        toolbarEditText = APP_ACTIVITY.mToolbar.search_toolbar.search_name_edit_text
    }


    private fun compareNotes(list: MutableList<NoteModel>) {
        if (!list.isNullOrEmpty()) {
            if (noteList.isNotEmpty()) {
                var isEquals = true
                val title =
                    "Список заметок в облаке отличается от вашего"
                list.forEach { noteModel ->
                    if (!noteList.contains(noteModel)) {
                        isEquals = false
                    }
                }
                noteList.forEach { noteModel ->
                    if (!list.contains(noteModel)) {
                        isEquals = false
                    }
                }
                if (!isEquals) {
                    AlertDialog.Builder(APP_ACTIVITY)
                        .setTitle(title)
                        .setPositiveButton("Обновить") { _, _ ->
                            noteList = list
                            mAdapter.updateData(categoryList)
                        }
                        .setNegativeButton("Отмена") { _, _ ->
                            deleteNotesFromDatabase(list)
                        }
                        .show()
                }
            } else {
                noteList = list
                mAdapter.updateData(categoryList)
            }
        }
    }

    private fun initNotes() {
        for (key: String in NOTES_STORAGE.all.keys) {
            val json = NOTES_STORAGE.getString(key, "")
            val note = Gson().fromJson(json, NoteModel::class.java)
            if (!note.inTrash)
                noteList.add(note)
        }
    }

    private fun compareLists(it: MutableList<CategoryModel>) {
        if (!it.isNullOrEmpty()) {
            it.sortBy { it.priority }
            if (categoryList.isNotEmpty()) {
                var isEquals = true
                val title =
                    "Список категорий в облаке отличается от вашего"
                it.forEach { categoryModel ->
                    if (!categoryList.contains(categoryModel)) {
                        isEquals = false
                    }
                }
                categoryList.forEach { categoryModel ->
                    if (!it.contains(categoryModel)) {
                        isEquals = false
                    }
                }
                if (!isEquals) {
                    AlertDialog.Builder(APP_ACTIVITY)
                        .setTitle(title)
                        .setPositiveButton("Обновить") { _, _ ->
                            categoryList = it
                            mAdapter.updateData(categoryList)
                        }
                        .setNegativeButton("Отмена") { _, _ ->
                            deleteCategoriesInDatabase(it)
                        }
                        .show()
                }

            }
        }
    }

    private fun createTextChangeObservable(): Observable<String> {
        val textChangeObservable = Observable.create<String> { emitter ->

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    if (s.isNullOrEmpty()) {
                        toolbarClearButton.visibility = View.INVISIBLE
                        searchRecyclerView.visibility = View.INVISIBLE
                        mRecyclerView.visibility = View.VISIBLE
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null) {
                        if (s.isNotEmpty()) {
                            emitter.onNext(s.toString())
                            toolbarClearButton.visibility = View.VISIBLE
                        } else {
                            toolbarClearButton.visibility = View.INVISIBLE
                            searchRecyclerView.visibility = View.INVISIBLE
                            mRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrEmpty()) {
                        toolbarClearButton.visibility = View.INVISIBLE
                        searchRecyclerView.visibility = View.INVISIBLE
                        mRecyclerView.visibility = View.VISIBLE
                    }
                }

            }

            toolbarEditText.addTextChangedListener(textWatcher)
        }
        return textChangeObservable.debounce(1000, TimeUnit.MILLISECONDS)
    }

    private fun initFunctions() {
        main_menu_btn_add.setOnClickListener {
            addCategory()
        }

        toolbarClearButton.setOnClickListener {
            toolbarEditText.editableText.delete(0, toolbarEditText.text.length)
            toolbarClearButton.visibility = View.INVISIBLE
        }
    }

    private fun addCategory() {

        var categoryName = ""
        var priority = 3
        val dialogView =
            LayoutInflater.from(APP_ACTIVITY).inflate(R.layout.dialog_create_category, null)
        dialogView.findViewById<EditText>(R.id.input_name).addTextChangedListener(object :
            TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                categoryName = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        with(dialogView.findViewById<NumberPicker>(R.id.input_priority)) {
            maxValue = 20
            minValue = 0
            value = priority
            setOnValueChangedListener { _, _, newVal ->
                priority = newVal
            }
        }

        AlertDialog.Builder(APP_ACTIVITY)
            .setTitle(R.string.create_category_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                categoryList.add(
                    CategoryModel(
                        UUID.randomUUID().toString(),
                        categoryName,
                        priority
                    )
                )
                categoryList.sortBy { it.priority }
                mAdapter.updateData(categoryList)
            }
            .show()
    }

    private fun initCategories() {
        for (key: String in CATEGORIES_STORAGE.all.keys) {
            val json = CATEGORIES_STORAGE.getString(key, "")
            val categoryModel = Gson().fromJson(json, CategoryModel::class.java)
            categoryList.add(categoryModel)
        }
        if (categoryList.isEmpty()) {
            categoryList.add(CategoryModel("0", "Today", 1))
            categoryList.add(CategoryModel("1", "Tomorrow", 2))
            categoryList.add(CategoryModel("2", "Unsorted", 3))
        }
        categoryList.sortBy {
            it.priority
        }
    }

    private fun initRecyclerView() {
        mRecyclerView = main_menu_recycler_view
        mAdapter = MainAdapter(categoryList)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = GridLayoutManager(APP_ACTIVITY, 2)
    }

    override fun onResume() {
        super.onResume()
        saveNotesToDatabase(noteList)
        APP_ACTIVITY.title = "eNote"
        APP_ACTIVITY.mDrawer.enableDrawer()
    }

    override fun onPause() {
        super.onPause()
        APP_ACTIVITY.mToolbar.search_toolbar.visibility = View.GONE
        saveCategories()
        if (CURRENT_UID != "null") {
            saveCategoriesToDatabase(categoryList)
        }
    }

    private fun saveCategories() {
        categoryList.forEach { categoryModel ->
            val jsonObject = Gson().toJson(categoryModel)
            CATEGORIES_STORAGE.edit()
                .putString("$STORAGE_CATEGORIES_ID:${categoryModel.id}", jsonObject).apply()
        }
    }
}