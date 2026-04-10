package com.solguard.app.data

import android.content.Context
import org.json.JSONArray

data class SkinType(
    val type: Int,
    val name: String,
    val description: String,
    val medSed: Double,
    val vitdRate: Double,
    val burnDescription: String
)

data class UVSafetyInfo(
    val category: String,
    val uvMin: Double,
    val uvMax: Double,
    val colorHex: String,
    val shortAdvice: String,
    val detailedActions: List<String>,
    val hat: Boolean,
    val sunscreen: Boolean,
    val shade: Boolean,
    val sunglasses: Boolean
)

class UVDataRepository(private val context: Context) {

    private var skinTypes: List<SkinType>? = null
    private var safetyInfos: List<UVSafetyInfo>? = null

    fun getSkinTypes(): List<SkinType> {
        skinTypes?.let { return it }
        val json = context.assets.open("skin_types.json").bufferedReader().readText()
        val arr = JSONArray(json)
        val list = mutableListOf<SkinType>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(SkinType(
                type = obj.getInt("type"),
                name = obj.getString("name"),
                description = obj.getString("description"),
                medSed = obj.getDouble("med_sed"),
                vitdRate = obj.getDouble("vitd_iu_per_min_at_uv5"),
                burnDescription = obj.getString("burn_description")
            ))
        }
        skinTypes = list
        return list
    }

    fun getSafetyInfos(): List<UVSafetyInfo> {
        safetyInfos?.let { return it }
        val json = context.assets.open("uv_safety.json").bufferedReader().readText()
        val arr = JSONArray(json)
        val list = mutableListOf<UVSafetyInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val actions = mutableListOf<String>()
            val actionsArr = obj.getJSONArray("detailed_actions")
            for (j in 0 until actionsArr.length()) {
                actions.add(actionsArr.getString(j))
            }
            list.add(UVSafetyInfo(
                category = obj.getString("category"),
                uvMin = obj.getDouble("uv_min"),
                uvMax = obj.getDouble("uv_max"),
                colorHex = obj.getString("color_hex"),
                shortAdvice = obj.getString("short_advice"),
                detailedActions = actions,
                hat = obj.getBoolean("hat"),
                sunscreen = obj.getBoolean("sunscreen"),
                shade = obj.getBoolean("shade"),
                sunglasses = obj.getBoolean("sunglasses")
            ))
        }
        safetyInfos = list
        return list
    }

    fun getSafetyForUV(uvIndex: Double): UVSafetyInfo {
        val infos = getSafetyInfos()
        return infos.lastOrNull { uvIndex >= it.uvMin } ?: infos.first()
    }
}
