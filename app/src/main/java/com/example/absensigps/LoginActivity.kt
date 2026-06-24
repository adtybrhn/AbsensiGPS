package com.example.absensigps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore // <-- PASTIKAN INI ADA

class LoginActivity : AppCompatActivity() {

    // Deklarasi variabel database
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Cek sesi login di penyimpanan lokal
        val sharedPref = getSharedPreferences("AbsensiData", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("is_logged_in", false)) {
            goToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        // Inisialisasi Firebase
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvErrorLogin = findViewById<TextView>(R.id.tvErrorLogin)

        btnLogin.setOnClickListener {
            val usernameInput = etUsername.text.toString().trim()
            val passwordInput = etPassword.text.toString().trim()

            if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                tvErrorLogin.text = "Harap isi username dan password!"
                tvErrorLogin.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Ubah teks tombol saat sedang memproses (loading)
            btnLogin.text = "MEMERIKSA..."
            btnLogin.isEnabled = false
            tvErrorLogin.visibility = View.GONE

            // BERTANYA KE FIREBASE CLOUD FIRESTORE
            db.collection("Users").document(usernameInput).get()
                .addOnSuccessListener { document ->
                    // Mengembalikan tombol ke semula
                    btnLogin.text = "MASUK"
                    btnLogin.isEnabled = true

                    if (document.exists()) {
                        // Menerjemahkan data JSON Firebase ke cetak biru DataModels.kt
                        val user = document.toObject(User::class.java)

                        if (user != null && user.password == passwordInput) {
                            // PASSWORD COCOK -> Simpan data sesi ke SharedPreferences
                            sharedPref.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("username", user.nama)
                                .putString("user_id", user.idKaryawan)
                                .putBoolean("is_shift_worker", user.isShiftWorker)
                                .putString("shift_kerja", user.shiftDefault)
                                .apply()

                            goToMainActivity()
                        } else {
                            tvErrorLogin.text = "Password salah!"
                            tvErrorLogin.visibility = View.VISIBLE
                        }
                    } else {
                        tvErrorLogin.text = "User tidak ditemukan di sistem!"
                        tvErrorLogin.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    // Terjadi error koneksi atau internet mati
                    btnLogin.text = "MASUK"
                    btnLogin.isEnabled = true
                    tvErrorLogin.text = "Gagal terhubung: ${e.message}"
                    tvErrorLogin.visibility = View.VISIBLE
                }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}