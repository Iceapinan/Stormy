package com.iceapinan.stormy

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.IOException
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private var mCurrentWeather = CurrentWeather()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiKey = "66c21604b611711eccf266371dec11cf"
        var latitude = "37.8267"
        var longitude = "-122.4233"
        val url = "https://api.darksky.net/forecast/$apiKey/$latitude,$longitude"
        if (isNetworkAvailable()) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response?) {
                    try {
                        if (response?.isSuccessful == true) {
                            val jsonData = response?.body()?.string()
                            Log.v(TAG, jsonData)
                            mCurrentWeather = getCurrentDetails(jsonData)
                        } else {
                            alertUserAboutError()
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Exception caught: ", e)
                    } catch (e: JSONException) {
                        Log.e(TAG, "Exception caught: ", e)
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {

                }

            })

        } else {
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG)
        }
    }
    @Throws (JSONException:: class)
    private fun getCurrentDetails(jsonData: String?): CurrentWeather  {

        val forecast = JSONObject(jsonData)
        val timezone = forecast.getString("timezone")
        Log.i(TAG,"From JSON: $timezone")
        val currently = forecast.getJSONObject("currently")
        val currentWeather = CurrentWeather()
        currentWeather.humidity = currently.getDouble("humidity")
        currentWeather.time = currently.getLong("time")
        currentWeather.icon = currently.getString("icon")
        currentWeather.precipChance = currently.getDouble("precipProbability")
        currentWeather.summary = currently.getString("summary")
        currentWeather.temperature = currently.getDouble("temperature")
        return currentWeather

    }

    private fun isNetworkAvailable(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo : NetworkInfo? = manager.activeNetworkInfo
        var isAvailable = false
        if (networkInfo != null && networkInfo.isConnected) {
            isAvailable = true
        }

        return isAvailable

    }

    private fun alertUserAboutError() {
          val alert =  AlertDialogFragment()
          alert.show(fragmentManager,"error_dialog")
    }
}
