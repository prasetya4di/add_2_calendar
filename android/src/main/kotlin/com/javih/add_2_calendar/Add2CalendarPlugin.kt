package com.javih.add_2_calendar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


/** Add2CalendarPlugin */
class Add2CalendarPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var context: Context? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "add_2_calendar")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "add2Cal") {
            val success = insert(
                call.argument("title")!!,
                call.argument("desc") as String?,
                call.argument("location") as String?,
                call.argument("startDate")!!,
                call.argument("endDate")!!,
                call.argument("timeZone") as String?,
                call.argument("allDay")!!,
                call.argument("recurrence") as HashMap<String, Any>?,
                call.argument("invites") as String?
            )
            result.success(success)
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    private fun insert(
        title: String,
        desc: String?,
        location: String?,
        start: Long,
        end: Long,
        timeZone: String?,
        allDay: Boolean,
        recurrence: HashMap<String, Any>?,
        invites: String?
    ): Boolean {
        val mContext: Context = activity?.applicationContext ?: context!!
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
            putExtra(CalendarContract.Events.EVENT_END_TIMEZONE, timeZone)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
            putExtra(CalendarContract.Events.RRULE, buildRRule(recurrence))
            putExtra(Intent.EXTRA_EMAIL, invites)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(mContext.packageManager) != null) {
            mContext.startActivity(intent)
            true
        } else {
            false
        }
    }

    private fun buildRRule(recurrence: HashMap<String, Any>?): String? {
        if (recurrence == null) return null

        var rRule = recurrence["rRule"] as String?
        if (rRule == null) {
            rRule = buildString {
                val freqEnum = recurrence["frequency"] as Int?
                if (freqEnum != null) {
                    append("FREQ=")
                    append(
                        when (freqEnum) {
                            0 -> "DAILY"
                            1 -> "WEEKLY"
                            2 -> "MONTHLY"
                            3 -> "YEARLY"
                            else -> ""
                        }
                    )
                    append(";")
                }
                append("INTERVAL=")
                append(recurrence["interval"] as Int)
                append(";")
                (recurrence["ocurrences"] as Int?)?.let {
                    append("COUNT=$it;")
                }
                (recurrence["endDate"] as Long?)?.let { endDateMillis ->
                    val endDate = Date(endDateMillis)
                    val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
                    append("UNTIL=${formatter.format(endDate)};")
                }
            }
        }
        return rRule
    }
}
