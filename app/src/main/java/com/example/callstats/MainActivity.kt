package com.example.callstats

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.provider.CallLog
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var output: LinearLayout
    private val req = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showUi()
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS), req)
        } else loadStats()
    }

    private fun showUi() {
        val scroll = ScrollView(this)
        output = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,24,24,24)
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        output.addView(TextView(this).apply {
            text = "סטטיסטיקת שיחות"; textSize = 28f; gravity = Gravity.CENTER
            setPadding(0,0,0,24)
        })
        output.addView(TextView(this).apply {
            text = "סיכום זמן שיחה לפי איש קשר. הנתונים נלקחים מיומן השיחות."
            textSize = 16f; setPadding(0,0,0,20)
        })
        output.addView(Button(this).apply {
            text = "רענן נתונים"; setOnClickListener { loadStats() }
        })
        scroll.addView(output)
        setContentView(scroll)
    }

    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, results:IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == req) {
            if (results.all { it == PackageManager.PERMISSION_GRANTED }) loadStats()
            else Toast.makeText(this, "צריך לאשר גישה ליומן השיחות ולאנשי הקשר", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadStats() {
        while (output.childCount > 3) output.removeViewAt(3)
        val now = System.currentTimeMillis()
        val day = 24L*60*60*1000
        val weekStart = now - 7*day
        val monthStart = now - 30*day
        val totals = linkedMapOf<String, LongArray>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME
        )
        contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC")?.use { c ->
            val ni=c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val di=c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val du=c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val ci=c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            while(c.moveToNext()) {
                val date=c.getLong(di); val dur=c.getLong(du)
                if (dur <= 0) continue
                val name=c.getString(ci)?.takeIf { it.isNotBlank() } ?: c.getString(ni) ?: "לא ידוע"
                val a=totals.getOrPut(name){LongArray(4)}
                a[3]+=dur
                if(date>=monthStart) a[2]+=dur
                if(date>=weekStart) a[1]+=dur
                if(date>=now-day) a[0]+=dur
            }
        }
        if(totals.isEmpty()) {
            output.addView(TextView(this).apply{text="לא נמצאו שיחות ביומן."; textSize=18f; setPadding(0,30,0,0)})
            return
        }
        totals.entries.take(100).forEach { (name,a) ->
            output.addView(TextView(this).apply {
                text="${name}\nהיום: ${fmt(a[0])} | 7 ימים: ${fmt(a[1])}\n30 ימים: ${fmt(a[2])} | הכול: ${fmt(a[3])}"
                textSize=17f; setPadding(16,18,16,18)
            })
        }
    }

    private fun fmt(sec:Long):String {
        val h=sec/3600; val m=(sec%3600)/60; val s=sec%60
        return if(h>0) "${h}ש ${m}ד" else if(m>0) "${m}ד ${s}ש" else "${s}ש"
    }
}
