package com.example.absensigps

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class LaporanActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvLaporan: RecyclerView
    private lateinit var spinnerFilter: Spinner
    private lateinit var adapter: LaporanAdapter
    private val listAbsensi = mutableListOf<LaporanAbsensi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan)

        db = FirebaseFirestore.getInstance()
        rvLaporan = findViewById(R.id.rvLaporan)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        rvLaporan.layoutManager = LinearLayoutManager(this)
        adapter = LaporanAdapter(listAbsensi)
        rvLaporan.adapter = adapter

        val pilihanFilter = arrayOf("Harian", "Mingguan", "Bulanan")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pilihanFilter)
        spinnerFilter.adapter = spinnerAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadRiwayatAbsensi(pilihanFilter[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadRiwayatAbsensi(tipe: String) {
        val calendar = Calendar.getInstance()
        val waktuAkhir = System.currentTimeMillis()

        when (tipe) {
            "Harian" -> { calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0) }
            "Mingguan" -> { calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0) }
            "Bulanan" -> { calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0) }
        }

        db.collection("Absensi")
            .whereEqualTo("idKaryawan", "001") // Ganti dengan ID user Anda
            .whereGreaterThanOrEqualTo("waktuMasuk", calendar.timeInMillis)
            .whereLessThanOrEqualTo("waktuMasuk", waktuAkhir)
            .orderBy("waktuMasuk", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                listAbsensi.clear()
                for (doc in documents) {
                    val absen = doc.toObject(LaporanAbsensi::class.java)
                    if (absen != null) listAbsensi.add(absen)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}