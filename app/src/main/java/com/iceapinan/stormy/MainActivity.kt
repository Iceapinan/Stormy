package com.iceapinan.stormy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import io.nlopez.smartlocation.*
import java.io.IOException
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider
import io.nlopez.smartlocation.SmartLocation
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnLocationUpdatedListener {
    override fun onLocationUpdated(p0: Location?) {
        showLocation(p0)
    }

    private val TAG = this.javaClass.simpleName
    private var mCurrentWeather = CurrentWeather()
    val LOCATION_PERMISSION_ID = 6;
    lateinit var provider : LocationGooglePlayServicesProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()

        refreshImageView.setOnClickListener {
            showLast()
        }


    }
    private fun checkPermission() {
        // If user's device version is greater than Android Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

                    val alert = AlertDialog.Builder(this).setTitle("Location Permission")
                            .setMessage("Hi there! We can't display weather info without the location permission, " +
                                    "could you please grant it?")
                            .setPositiveButton("Yep") { _, _ -> requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_ID) }
                            .setNegativeButton("No thanks") { _, _ ->
                                val context : Context? = MainActivity()
                                Toast.makeText(context!!,":(",Toast.LENGTH_SHORT)
                            }
                            .show()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_ID)
                }
            } else {
                startLocation()
                showLast()
            }
        } else {
            startLocation()
            showLast()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_ID && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation()
            showLast()
        }
    }
    private fun showLast() {
        val lastLocation = SmartLocation.with(this).location().lastLocation
        if (lastLocation != null) {
            val latitude = lastLocation.latitude
            val longitude = lastLocation.longitude
            geoCoder(lastLocation)
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
        SmartLocation.with(this).geocoding().reverse(location) { _, p1 ->
            if (p1 != null && p1.size > 0) {
                val result: Address = p1[0];
                runOnUiThread {
                    val text = result.adminArea + ", " + result.countryName
                    if (text.length > 30) {
                        locationLabel.text = result.adminArea
                    } else {
                        locationLabel.text = text
                    }

                }
            }
        }
    }

    private fun startLocation() {
        provider = LocationGooglePlayServicesProvider()
        provider.setCheckLocationSettings(true)
        val smartLocation = SmartLocation.Builder(this).logging(true).build()
        smartLocation.location(provider).continuous().start(this)
    }


    private fun getForecast(latitude: String, longitude: String) {
        val apiKey = getString(R.string.apiKey)
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
