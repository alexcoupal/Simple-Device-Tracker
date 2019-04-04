package com.v2d.trackme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*


/**
 * Created by acoupal on 3/8/2019.
 */
@Entity
data class MyHistory (
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val createdDate: Date?
)
