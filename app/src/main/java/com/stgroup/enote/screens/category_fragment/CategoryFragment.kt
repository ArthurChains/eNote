package com.stgroup.enote.screens.category_fragment

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.stgroup.enote.R
import com.stgroup.enote.models.CategoryModel
import com.stgroup.enote.models.NoteModel
import com.stgroup.enote.utilities.APP_ACTIVITY
import com.stgroup.enote.utilities.NOTES_STORAGE
import com.stgroup.enote.utilities.STORAGE_NOTES_ID
import kotlinx.android.synthetic.main.fragment_category.*

class CategoryFragment(private var category: CategoryModel) : Fragment(R.layout.fragment_category) {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: CategoryAdapter

    private lateinit var mNoteList: MutableList<NoteModel>

    override fun onStart() {
        super.onStart()
        initNoteList()
        initRecyclerView()
    }

    override fun onPause() {
        super.onPause()
        saveNoteList()
    }

    private fun saveNoteList() {
        // Сохраняем все заметки из этой категории
        // выполняются лишние действия, потому что, может, что их сохранять и не нужно
        // Поэтому в будущем можно оптимизировать
        mNoteList.forEach {
            val json = Gson().toJson(it)
            NOTES_STORAGE.edit().putString("$STORAGE_NOTES_ID:${it.id}", json).apply()
        }
    }

    private fun initRecyclerView() {
        mRecyclerView = category_recycler_view
        mAdapter = CategoryAdapter(mNoteList)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(APP_ACTIVITY)
    }


    // Как вариант загрузить все заметки сразу при запуске приложения, а потом уже отбирать.
    // Нужно подумать над этим вариантом
    private fun initNoteList() {
        mNoteList = mutableListOf()
        // Загружаем заметки из хранилища
        for (key: String in NOTES_STORAGE.all.keys) {
            val json = NOTES_STORAGE.getString(key, "")
            val note = Gson().fromJson(json, NoteModel::class.java)
            // Если категория заметки совпадает с текущей, то добавляем её в наш список
            if (note.category == category.name)
                mNoteList.add(note)
        }
        mNoteList.sortBy { it.id }
    }

    override fun onResume() {
        super.onResume()
        APP_ACTIVITY.title = category.name
    }
}