package com.v2d.trackme.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

/**
 * Created by acoupal on 3/8/2019.
 */
@Dao
interface MyHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(myHistory: MyHistory)

    @Update
    fun update(myHistory: MyHistory)

    @Delete
    fun delete(myHistory: MyHistory)

    @Query("SELECT * FROM MyHistory ORDER BY createdDate DESC")
    fun getAll(): LiveData<List<MyHistory>>

    @Query("SELECT * FROM MyHistory WHERE name = :name LIMIT 1")
    fun getByName(name: String): MyHistory
}