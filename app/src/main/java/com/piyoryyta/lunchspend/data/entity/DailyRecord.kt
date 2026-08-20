package com.piyoryyta.lunchspend.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 精算＝日次記録 (SPEC.md 3.3)。1日1件、朝・昼・夜のような分割記録は行わない。
 */
@Entity(
    tableName = "daily_records",
    indices = [Index(value = ["date"], unique = true)],
)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** "yyyy-MM-dd" 形式。精算対象日。一意制約 (1日1レコード)。 */
    val date: String,
    val memo: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
