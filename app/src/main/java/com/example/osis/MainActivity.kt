package com.example.osis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.osis.databinding.ActivityMainBinding
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeReader
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var bind: ActivityMainBinding
    private val PICK_IMAGE_REQUEST = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.loginQr.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan QR Code")
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.captureActivity = PortraitCaptureActivity::class.java
            integrator.initiateScan()
        }

        bind.loginQrPhoto.setOnClickListener {
            openGallery()
        }
    }

    @SuppressLint("Recycle")
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        val load = SweetAlertDialog(this@MainActivity, SweetAlertDialog.PROGRESS_TYPE)
        if (result != null && result.contents != null)
        {
            load.progressHelper.barColor = Color.parseColor("#A5DC86")
            load.titleText = "Memproses..."
            load.setCancelable(false)
            load.show()

            val qrData = result.contents

            val url = "${Session.BASE_URL}/login"
            val reqBody = JSONObject()
            reqBody.put("qr_code", qrData)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                url,
                reqBody,
                { r ->

                    if (!r.getBoolean("status")) {
                        Toast.makeText(this@MainActivity, r.getString("message"), Toast.LENGTH_SHORT).show()
                    } else {
                        getSharedPreferences("auth", Context.MODE_PRIVATE).edit().apply {
                            putString("token", r.getString("token"))
                            putString("user", r.getJSONObject("peserta").toString())
                        }.apply()

                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        finish()

                        Toast.makeText(this@MainActivity, "Selamat Datang, ${r.getJSONObject("peserta").getString("nama_peserta")}", Toast.LENGTH_SHORT).show()
                    }

                },
                { e: VolleyError ->
                    load.dismiss()
                    Log.d("ApiError", "onActivityResult: ${e.message}")
                    val errAlert = SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                    errAlert.titleText = e.message
                    errAlert.setCancelable(true)
                    errAlert.show()
                }
            )

            Volley.newRequestQueue(this@MainActivity).add(jsonObjectRequest)

        }
        else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            try {
                // Ambil bitmap dari URI gambar
                val inputStream = contentResolver.openInputStream(uri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Pinda qr dari gambar
                scanQrFromPhoto(bitmap)
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun scanQrFromPhoto(photo: Bitmap) {
        val intArray = IntArray(photo.width * photo.height)
        photo.getPixels(intArray, 0, photo.width, 0, 0, photo.width, photo.height)

        val source = RGBLuminanceSource(photo.width, photo.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeReader()

        val load = SweetAlertDialog(this@MainActivity, SweetAlertDialog.PROGRESS_TYPE)

        try {
            val result = reader.decode(binaryBitmap)
            val url = "${Session.BASE_URL}/login"
            val reqBody = JSONObject()
            reqBody.put("qr_code", result.text)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                url,
                reqBody,
                { r ->

                    if (!r.getBoolean("status")) {
                        Toast.makeText(this@MainActivity, r.getString("message"), Toast.LENGTH_SHORT).show()
                    } else {
                        getSharedPreferences("auth", Context.MODE_PRIVATE).edit().apply {
                            putString("token", r.getString("token"))
                            putString("user", r.getJSONObject("peserta").toString())
                        }.apply()

                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        finish()

                        Toast.makeText(this@MainActivity, "Selamat Datang, ${r.getJSONObject("peserta").getString("nama_peserta")}", Toast.LENGTH_SHORT).show()
                    }

                },
                { e: VolleyError ->
                    load.dismiss()
                    Log.d("ApiError", "onActivityResult: ${e.message}")
                    val errAlert = SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                    errAlert.titleText = "Kesalahan Jaringan!"
                    errAlert.setCancelable(true)
                    errAlert.show()
                }
            )

            Volley.newRequestQueue(this@MainActivity).add(jsonObjectRequest)
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "QR Code Tidak Valid!", Toast.LENGTH_SHORT).show()
        }
    }
}