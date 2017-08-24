package com.iceapinan.stormy

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by IceApinan on 22/8/17.
 */

class CurrentWeather() {

    var icon: String? = null
    var time: Long = 0
    var timeZone: String? = null
    var temperature: Int = 0
    var humidity: Double = 0.toDouble()
    var precipChance: Double = 0.toDouble()
    var summary: String? = null

    fun getFormattedTime() : String {
        val formatter : SimpleDateFormat = SimpleDateFormat("h:mm a")
        formatter.timeZone = TimeZone.getTimeZone(timeZone)
            val dateTime = Date(time * 1000)
        return formatter.format(dateTime)
    }

    fun getPrecipChance() : Int {
        // Convert to Percentage
        val precipPercentage : Double = precipChance * 100
        return Math.round(precipPercentage).toInt()
    }

    fun getIconId() : Int {
        var iconId = R.drawable.clear_day

        when {
            icon.equals("clear-day") -> iconId = R.drawable.clear_day
            icon.equals("clear-night") -> iconId = R.drawable.clear_night
            icon.equals("rain") -> iconId = R.drawable.rain
            icon.equals("snow") -> iconId = R.drawable.snow
            icon.equals("sleet") -> iconId = R.drawable.sleet
            icon.equals("wind") -> iconId = R.drawable.wind
            icon.equals("fog") -> iconId = R.drawable.fog
            icon.equals("cloudy") -> iconId = R.drawable.cloudy
            icon.equals("partly-cloudy-day") -> iconId = R.drawable.partly_cloudy
            icon.equals("partly-cloudy-night") -> iconId = R.drawable.cloudy_night
        }

        return iconId

    }

}
