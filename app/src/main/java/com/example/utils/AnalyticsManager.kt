package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRange { WEEK, MONTH, ALL_TIME }

data class StatsDashboardData(
    val created: Int,
    val exported: Int,
    val editingTimeMs: Long,
    val features: Map<String, Int>,
    val streak: Int,
    val dailyActivity: List<Float>, 
    val badges: List<String>
)

object AnalyticsManager {
    private lateinit var prefs: SharedPreferences
    private lateinit var privacyPrefs: SharedPreferences
    private var _fullJson = JSONObject()

    private val _dashboardData = MutableStateFlow<StatsDashboardData?>(null)
    val dashboardData: StateFlow<StatsDashboardData?> = _dashboardData.asStateFlow()

    private var currentRange = TimeRange.WEEK
    var sessionStartMs = 0L

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("clipp_stats", Context.MODE_PRIVATE)
        privacyPrefs = context.getSharedPreferences("clipp_privacy", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("data", "{}") ?: "{}"
        _fullJson = try { JSONObject(jsonStr) } catch(e:Exception) { JSONObject() }
        
        checkStreak()
        updateDashboard()
    }

    private fun save() {
        prefs.edit().putString("data", _fullJson.toString()).apply()
        updateDashboard()
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getDailyObject(date: String): JSONObject {
        if (!_fullJson.has("daily")) _fullJson.put("daily", JSONObject())
        val daily = _fullJson.getJSONObject("daily")
        if (!daily.has(date)) {
            val newDay = JSONObject().apply {
                put("created", 0)
                put("exported", 0)
                put("timeMs", 0L)
                put("features", JSONObject())
            }
            daily.put(date, newDay)
        }
        return daily.getJSONObject(date)
    }

    fun trackVideoCreated() {
        if (::privacyPrefs.isInitialized && !privacyPrefs.getBoolean("analytics_enabled", true)) return
        val today = getTodayDate()
        val dayObj = getDailyObject(today)
        dayObj.put("created", dayObj.optInt("created", 0) + 1)
        
        checkBadges()
        save()
    }

    fun trackVideoExported(durationMs: Long) {
        if (::privacyPrefs.isInitialized && !privacyPrefs.getBoolean("analytics_enabled", true)) return
        val today = getTodayDate()
        val dayObj = getDailyObject(today)
        dayObj.put("exported", dayObj.optInt("exported", 0) + 1)
        
        if (sessionStartMs > 0 && (System.currentTimeMillis() - sessionStartMs) < 5 * 60 * 1000L) {
            awardBadge("Speed Editor")
        }
        
        checkBadges()
        save()
    }
    
    fun startEditingSession() {
        sessionStartMs = System.currentTimeMillis()
    }
    
    fun endEditingSession() {
        if (sessionStartMs == 0L) return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        
        if (::privacyPrefs.isInitialized && privacyPrefs.getBoolean("analytics_enabled", true)) {
            val today = getTodayDate()
            val dayObj = getDailyObject(today)
            dayObj.put("timeMs", dayObj.optLong("timeMs", 0L) + elapsed)
        }
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 0..4) {
            awardBadge("Night Owl Editor")
        }
        
        sessionStartMs = 0L
        save()
    }

    fun trackFeature(featureName: String) {
        if (::privacyPrefs.isInitialized && !privacyPrefs.getBoolean("analytics_enabled", true)) return
        val today = getTodayDate()
        val dayObj = getDailyObject(today)
        val features = dayObj.getJSONObject("features")
        features.put(featureName, features.optInt(featureName, 0) + 1)
        
        checkBadges()
        save()
    }
    
    private fun awardBadge(badgeId: String) {
        if (!_fullJson.has("badges")) _fullJson.put("badges", org.json.JSONArray())
        val badges = _fullJson.getJSONArray("badges")
        var hasBadge = false
        for (i in 0 until badges.length()) {
            if (badges.getString(i) == badgeId) hasBadge = true
        }
        if (!hasBadge) {
            badges.put(badgeId)
        }
    }
    
    private fun checkBadges() {
        var totalExported = 0
        var totalCreated = 0
        val allFeatures = mutableSetOf<String>()
        val daily = _fullJson.optJSONObject("daily") ?: JSONObject()
        val keys = daily.keys()
        while(keys.hasNext()) {
            val key = keys.next()
            val day = daily.getJSONObject(key)
            totalCreated += day.optInt("created", 0)
            totalExported += day.optInt("exported", 0)
            val features = day.optJSONObject("features") ?: JSONObject()
            val fKeys = features.keys()
            while(fKeys.hasNext()) {
                allFeatures.add(fKeys.next())
            }
        }
        
        if (totalExported >= 1 || totalCreated >= 1) awardBadge("First Video")
        if (totalExported >= 10) awardBadge("10 Videos")
        if (totalExported >= 100) awardBadge("100 Videos")
        
        val aiFeatures = listOf("Auto Caption", "AI Enhance", "AI Object Remove", "AI Music", "AI Voice", "Smart Reframe")
        var hasAllAI = true
        for (f in aiFeatures) {
            if (!allFeatures.contains(f)) hasAllAI = false
        }
        if (hasAllAI) awardBadge("AI Master")
    }

    private fun checkStreak() {
        val daily = _fullJson.optJSONObject("daily") ?: JSONObject()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var streak = 0
        
        var currentDayStr = dateFormat.format(cal.time)
        if (daily.has(currentDayStr)) streak++
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        currentDayStr = dateFormat.format(cal.time)
        
        while (daily.has(currentDayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = dateFormat.format(cal.time)
        }
        
        _fullJson.put("streak", streak)
    }

    fun setTimeRange(range: TimeRange) {
        currentRange = range
        updateDashboard()
    }

    private fun updateDashboard() {
        var totalCreated = 0
        var totalExported = 0
        var totalTimeMs = 0L
        val featureCount = mutableMapOf<String, Int>()
        
        val dailyActivityList = FloatArray(30) { 0f }
        
        val daily = _fullJson.optJSONObject("daily") ?: JSONObject()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val cal = Calendar.getInstance()
        val todayMs = cal.timeInMillis
        
        for (i in 0 until 30) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -(29 - i))
            val dStr = dateFormat.format(c.time)
            if (daily.has(dStr)) {
                val dObj = daily.getJSONObject(dStr)
                val min = dObj.optLong("timeMs", 0L) / 60000f
                val acts = dObj.optInt("created", 0) + dObj.optInt("exported", 0)
                dailyActivityList[i] = min + (acts * 5f)
            }
        }
        
        val daysToInclude = when(currentRange) {
            TimeRange.WEEK -> 7
            TimeRange.MONTH -> 30
            TimeRange.ALL_TIME -> 99999
        }
        
        val keys = daily.keys()
        while(keys.hasNext()) {
            val dStr = keys.next()
            val dDate = try { dateFormat.parse(dStr) } catch(e:Exception) { null } ?: continue
            val diffDays = (todayMs - dDate.time) / (1000 * 60 * 60 * 24)
            if (diffDays <= daysToInclude) {
                val dObj = daily.getJSONObject(dStr)
                totalCreated += dObj.optInt("created", 0)
                totalExported += dObj.optInt("exported", 0)
                totalTimeMs += dObj.optLong("timeMs", 0L)
                
                val userFeats = dObj.optJSONObject("features") ?: JSONObject()
                val featKeys = userFeats.keys()
                while(featKeys.hasNext()) {
                    val k = featKeys.next()
                    featureCount[k] = featureCount.getOrDefault(k, 0) + userFeats.getInt(k)
                }
            }
        }
        
        val badgesArr = _fullJson.optJSONArray("badges") ?: org.json.JSONArray()
        val badgesList = mutableListOf<String>()
        for (i in 0 until badgesArr.length()) badgesList.add(badgesArr.getString(i))
        
        val streak = _fullJson.optInt("streak", 0)
        
        _dashboardData.value = StatsDashboardData(
            created = totalCreated,
            exported = totalExported,
            editingTimeMs = totalTimeMs,
            features = featureCount,
            streak = streak,
            dailyActivity = dailyActivityList.toList(),
            badges = badgesList
        )
    }
}
