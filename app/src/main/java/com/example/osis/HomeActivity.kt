package com.example.osis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.osis.databinding.ActivityHomeBinding
import com.example.osis.databinding.CandidateCardBinding
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {
    private lateinit var bind: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.logoutBtn.setOnClickListener {
            logoutProcess(getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null))
        }

        val dataUser = getSharedPreferences("auth", Context.MODE_PRIVATE).getString("user", null)
        val jsonUser = dataUser?.let { JSONObject(it) }

        bind.usernameTv.text = jsonUser?.getString("nama_peserta")

        loadKandidat(getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null))
    }

    private fun loadKandidat(token: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = "${Session.BASE_URL}/kandidat"
        val jsonKandidat = JSONObject()

        val stringRequest = object : JsonObjectRequest(Request.Method.GET, url, jsonKandidat ,
            { response ->
                val adapter = object : RecyclerView.Adapter<Holder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
                        val inflate = CandidateCardBinding.inflate(layoutInflater, parent, false)
                        return Holder(inflate)
                    }

                    override fun getItemCount(): Int {
                        return response.getJSONArray("kandidats").length()
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onBindViewHolder(holder: Holder, position: Int) {
                        holder.binding.ketuaKandidatTv.text = response.getJSONArray("kandidats")
                            .getJSONObject(position)
                            .getString("nama_ketua")

                        holder.binding.wakilKandidatTv.text = response.getJSONArray("kandidats")
                            .getJSONObject(position)
                            .getString("nama_wakil")

                        holder.binding.nomorKandidatTv.text = "Kandidat ${response.getJSONArray("kandidats")
                            .getJSONObject(position)
                            .getString("id_kandidat")}"

                        val dataFoto = response.getJSONArray("kandidats").getJSONObject(position).getString("foto")
                        val urlFoto = "${Session.WEB_URL}/uploads/${dataFoto}"

                        Glide.with(this@HomeActivity)
                            .load(urlFoto)
                            .into(holder.binding.imge)

                        holder.itemView.setOnClickListener {
                            startActivity(Intent(this@HomeActivity, DetailActivity::class.java).apply {
                                putExtra("idKandidat", response.getJSONArray("kandidats")
                                    .getJSONObject(position)
                                    .getString("id_kandidat"))
                            })
                        }
                    }
                }

                bind.kandidatRv.adapter = adapter
                bind.kandidatRv.layoutManager = LinearLayoutManager(this@HomeActivity)
            },
            {
                val confirmForm = SweetAlertDialog(this@HomeActivity, SweetAlertDialog.WARNING_TYPE)
                confirmForm.setTitleText("Koneksi Internet Tidak Stabil")
                confirmForm.setContentText("Gagal memuat data karena koneksi internet tidak stabil")
                confirmForm.setConfirmText("Coba Lagi")
                confirmForm.setConfirmClickListener { e ->
                    loadKandidat(
                        getSharedPreferences(
                            "auth",
                            Context.MODE_PRIVATE
                        ).getString("token", null)
                    )
                    e.dismiss()
                }
                confirmForm.setCancelable(false)

                confirmForm.show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        queue.add(stringRequest)
    }

    private fun logoutProcess(token: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = "${Session.BASE_URL}/logout"

        val stringRequest = @SuppressLint("CommitPrefEdits")
        object : StringRequest(Request.Method.GET, url,
            { _ ->
                Toast.makeText(this@HomeActivity, "Berhasil Logout!", Toast.LENGTH_SHORT).show()

                getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply()

                startActivity(Intent(this@HomeActivity, MainActivity::class.java))

                finish()
            },
            {
                val confirmForm = SweetAlertDialog(this@HomeActivity, SweetAlertDialog.ERROR_TYPE)
                confirmForm.setTitleText("Koneksi Bermasalah!")
                confirmForm.setContentText("Gagal logout, silahkan coba lagi!")
                confirmForm.setConfirmText("Coba Lagi")
                confirmForm.setConfirmClickListener { e -> e.dismissWithAnimation() }
                confirmForm.setCancelable(false)

                confirmForm.show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    class Holder(var binding: CandidateCardBinding) : RecyclerView.ViewHolder(binding.root)
}