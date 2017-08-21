package com.iceapinan.stormy

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.IOException
import okhttp3.*


class MainActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiKey = "66c21604b611711eccf266371dec11cf"
        var latitude = 99999 //"37.8267"
        var longitude = "-122.4233"
        val url = "https://api.darksky.net/forecast/$apiKey/$latitude,$longitude"
        val client = OkHttpClient()

        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                try {
                    if (response?.isSuccessful == true) {
                        Log.v(TAG, response?.body()?.string())
                    } else {
                        alertUserAboutError()
                    }
                } catch (e : IOException) {
                    Log.e(TAG, "Exception caught: ", e)
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {

            }

        })

    }

    private fun alertUserAboutError() {
          val alert =  AlertDialogFragment()
          alert.show(fragmentManager,"error_dialog")
    }
}
