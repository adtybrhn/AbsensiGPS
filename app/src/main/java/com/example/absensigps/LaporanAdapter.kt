package com.example.absensigps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LaporanAdapter(private val listAbsensi: List<LaporanAbsensi>) :
    RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder>() {

    class LaporanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvShift: TextView = view.findViewById(R.id.tvShift)
        val tvJam: TextView = view.findViewById(R.id.tvJam)
        val tvKeterangan: TextView = view.findViewById(R.id.tvKeterangan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_laporan, parent, false)
        return LaporanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        val absen = listAbsensi[position]

        val formatterTanggal = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        holder.tvTanggal.text = "- ${formatterTanggal.format(Date(absen.waktuMasuk))}"
        holder.tvShift.text = "Shift : ${absen.shiftAktual}"

        val formatterJam = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val jamMasuk = formatterJam.format(Date(absen.waktuMasuk))
        val jamKeluar = if (absen.waktuKeluar > 0L) formatterJam.format(Date(absen.waktuKeluar)) else "--:--:--"

        val teksDurasi = if (absen.waktuKeluar > 0L) {
            val totalMenit = (absen.waktuKeluar - absen.waktuMasuk) / (1000 * 60)
            "(${totalMenit / 60} Jam, ${totalMenit % 60} Menit)"
        } else "(Sedang Bekerja)"

        holder.tvJam.text = "Jam: $jamMasuk - $jamKeluar $teksDurasi"
        holder.tvKeterangan.text = "Keterangan : ${if (absen.keterangan.isNotEmpty()) absen.keterangan else "absen pulang"}"
    }

    override fun getItemCount(): Int = listAbsensi.size
}