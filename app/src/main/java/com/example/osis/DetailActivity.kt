package com.example.osis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.osis.HomeActivity.Holder
import com.example.osis.databinding.ActivityDetailBinding
import com.example.osis.databinding.CandidateCardBinding
import org.json.JSONObject

class DetailActivity : AppCompatActivity() {
    lateinit var bind : ActivityDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.detailTbr.setNavigationOnClickListener {
            finish()
        }

        loadKandidat(getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null),
            intent.getStringExtra("idKandidat"))

        bind.pilihBtn.setOnClickListener {
            val confirmVote = SweetAlertDialog(this@DetailActivity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Apakah Kamu Yakin Memilih Kandidat Ini?")
                .setContentText("Kamu hanya memiliki 1 kesempatan voting, Apakah kamu yakin?")
                .setConfirmText("Pilih")
                .setConfirmClickListener { e ->
                    voteKandidat(intent.getStringExtra("idKandidat"),
                        getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null))
                    e.dismissWithAnimation()
                }
                .setCancelButton("Batal") { e -> e.dismissWithAnimation() }
            confirmVote.show()
        }
    }

    private fun loadKandidat(token: String?, idKandidat: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = "${Session.BASE_URL}/kandidat/detail/${idKandidat}"
        val jsonKandidat = JSONObject()

        val stringRequest = object : JsonObjectRequest(Request.Method.GET, url, jsonKandidat ,
            { response ->
                val dataKandidat = response.getJSONObject("kandidat")

                bind.detailKetuaTv.text = dataKandidat.getString("nama_ketua")
                bind.detailWakilTv.text = dataKandidat.getString("nama_wakil")
                bind.detailVisiTv.text = dataKandidat.getString("visi")
                bind.detailMisiTv.text = dataKandidat.getJSONArray("misi")[0].toString()
                bind.detailTbr.title = "Kandidat ${idKandidat}"
                bind.detailSloganTv.text = dataKandidat.getString("slogan")


//                Log.d("Fotogagal", "loadKandidat: ${Session.WEB_URL}/uploads/${dataKandidat.getString("foto")}}")
                val urlFoto = "${Session.WEB_URL}/uploads/${dataKandidat.getString("foto")}"

                Glide.with(this@DetailActivity)
                    .load(urlFoto)
                    .into(bind.imageView2)

            },
            {
                // Create the object of AlertDialog Builder class
                val builder = AlertDialog.Builder(this)

                // Set the message show for the Alert time
                builder.setMessage("Gagal Terhubung Ke Server!")

                // Set Alert Title
                builder.setTitle("Tidak Ada Koneksi!")

                // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                builder.setCancelable(false)

                // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
                builder.setPositiveButton("Coba Lagi") {
                    // When the user click yes button then app will close
                        dialog, which -> loadKandidat(getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null),
                    intent.getStringExtra("idKandidat"))
                }

                // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                builder.setNegativeButton("Kembali") {
                    // If user click no then dialog box is canceled.
                        dialog, which -> finish()
                }

                // Create the Alert dialog
                val alertDialog = builder.create()
                // Show the Alert Dialog box
                alertDialog.show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        queue.add(stringRequest)
    }

    private fun voteKandidat(id_kandidat: String?, token: String?) {
            val url = "${Session.BASE_URL}/kandidat/vote"
            val reqBody = JSONObject()
            reqBody.put("id_kandidat", id_kandidat)

            val jsonObjectRequest = @SuppressLint("SuspiciousIndentation")
            object : JsonObjectRequest(
                Request.Method.POST,
                url,
                reqBody,
                { r ->
                    if (!r.getBoolean("status")) {
                        SweetAlertDialog(this@DetailActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Gagal Vote!")
                            .setContentText(r.getString("message"))
                            .show()
                    } else {
                        val sucAlrt = SweetAlertDialog(this@DetailActivity, SweetAlertDialog.SUCCESS_TYPE)
                            sucAlrt.setCancelable(false)
                            sucAlrt.setTitleText(r.getString("message"))
                            sucAlrt.setConfirmClickListener { e ->
                                finish()
                            }
                        sucAlrt.show()
                    }
                },
                { err: VolleyError ->
                    // Create the object of AlertDialog Builder class
                    val builder = AlertDialog.Builder(this)

                    // Set the message show for the Alert time
                    builder.setMessage("Gagal Melakukan Voting!")

                    // Set Alert Title
                    builder.setTitle("Tidak Ada Koneksi!")

                    // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                    builder.setCancelable(false)

                    // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
                    builder.setPositiveButton("Coba Lagi") {
                        // When the user click yes button then app will close
                            dialog, which -> dialog.cancel()
                    }

                    // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                    builder.setNegativeButton("Kembali") {
                        // If user click no then dialog box is canceled.
                            dialog, which -> finish()
                    }

                    // Create the Alert dialog
                    val alertDialog = builder.create()
                    // Show the Alert Dialog box
                    alertDialog.show()
                }
            ){
                override fun getHeaders(): MutableMap<String, String> {
                    val header = HashMap<String, String>()
                    header["Authorization"] = "Bearer $token"
                    return header
                }
            }

            Volley.newRequestQueue(this@DetailActivity).add(jsonObjectRequest)
        }
}