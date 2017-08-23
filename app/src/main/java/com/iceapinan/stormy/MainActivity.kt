package com.iceapinan.stormy

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import io.nlopez.smartlocation.*
import java.io.IOException
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider
import io.nlopez.smartlocation.OnReverseGeocodingListener
import io.nlopez.smartlocation.SmartLocation





class MainActivity : AppCompatActivity(), OnLocationUpdatedListener {
    private val TAG = this.javaClass.simpleName
    private var mCurrentWeather = CurrentWeather()
    val LOCATION_PERMISSION_ID = 1001;
    lateinit var timeLabel : TextView
    lateinit var temperatureLabel: TextView
    lateinit var humidityValue: TextView
    lateinit var precipValue: TextView
    lateinit var summaryLabel: TextView
    lateinit var iconImageView : ImageView
    lateinit var refreshImageView : ImageView
    lateinit var progressBar : ProgressBar
    lateinit private var provider: LocationGooglePlayServicesProvider
    lateinit var addressLabel : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        timeLabel = findViewById(R.id.timeLabel)
        temperatureLabel = findViewById(R.id.temperatureLabel)
        humidityValue = findViewById(R.id.humidityValue)
        precipValue = findViewById(R.id.precipValue)
        summaryLabel = findViewById(R.id.summaryLabel)
        iconImageView = findViewById(R.id.iconImageView)
        refreshImageView = findViewById(R.id.refreshImageView)
        progressBar = findViewById(R.id.progressBar)
        addressLabel = findViewById(R.id.locationLabel)
        startLocation()
        progressBar.visibility = View.INVISIBLE
        showLast()
        refreshImageView.setOnClickListener {
            showLast()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
       super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.count() > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            alertUserAboutError()
        }

    }
    private fun showLast() {
        val lastLocation = SmartLocation.with(this).location().lastLocation
        if (lastLocation != null) {
            val latitude = lastLocation.latitude
            val longitude = lastLocation.longitude
            getForecast(latitude.toString(),longitude.toString())
        }

    }
    private fun showLocation(location: Location?) {

        if (location != null) {
            val latitude = location.latitude
            val longitude = location.longitude
            geoCoder(location)
            getForecast(latitude.toString(),longitude.toString())

        }

    }
    private fun geoCoder(location: Location) {
        SmartLocation.with(this).geocoding().reverse(location, object : OnReverseGeocodingListener {
            override fun onAddressResolved(p0: Location?, p1: MutableList<Address>?) {
                if (p1 != null && p1.size > 0) {
                    val result: Address = p1[0];
                    runOnUiThread {
                        addressLabel.text = result.adminArea + ", " + result.countryName
                    }
                }
            }
        })
    }

    private fun startLocation() {
        provider = LocationGooglePlayServicesProvider()
        provider.setCheckLocationSettings(true)
        val smartLocation = SmartLocation.Builder(this).logging(true).build()
        smartLocation.location(provider).start(this)
    }
    override fun onLocationUpdated(location: Location?) {
        showLocation(location)
    }

    private fun getForecast(latitude: String, longitude: String) {
        val apiKey = "66c21604b611711eccf266371dec11cf"
        val unit = "units=si"
        val url = "https://api.darksky.net/forecast/$apiKey/$latitude,$longitude/?$unit"
        if (isNetworkAvailable()) {
            toggleRefresh()
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response?) {
                    toggleRefresh()
                    try {
                        if (response?.isSuccessful == true) {
                            val jsonData = response?.body()?.string()
                            Log.v(TAG, jsonData)
                            mCurrentWeather = getCurrentDetails(jsonData)
                            runOnUiThread {
                                updateDisplay();
                            }
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
                    toggleRefresh()
                    alertUserAboutError()
                }

            })

        } else {
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG)
        }
    }

    private fun toggleRefresh() {
        runOnUiThread {
            if (progressBar.visibility == View.INVISIBLE) {
            progressBar.visibility = View.VISIBLE
            refreshImageView.visibility = View.INVISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
            refreshImageView.visibility = View.VISIBLE
        } }

    }

    private fun updateDisplay() {
        temperatureLabel.text = mCurrentWeather.temperature.toString()
        timeLabel.text = mCurrentWeather.getFormattedTime() + " it will be"
        humidityValue.text = mCurrentWeather.humidity.toString()
        precipValue.text = mCurrentWeather.getPrecipChance().toString() + "%"
        summaryLabel.text = mCurrentWeather.summary.toString()
        iconImageView.setImageResource(mCurrentWeather.getIconId())

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
        currentWeather.temperature = currently.getInt("temperature")
        currentWeather.timeZone = timezone
        Log.d(TAG, currentWeather.getFormattedTime())
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
        runOnUiThread {
            val alert =  AlertDialogFragment()
            alert.show(fragmentManager,"error_dialog")
        }

    }
}
