package com.example.absensigps

// Pastikan class ini ada dan ditulis dengan benar
data class User(
    val idKaryawan: String = "",
    val nama: String = "",
    val password: String = "",
    val isShiftWorker: Boolean = false,
    val shiftDefault: String = ""
)

data class LaporanAbsensi(
    val idKaryawan: String = "",
    val tanggal: String = "",
    val waktuMasuk: Long = 0L,
    val waktuKeluar: Long = 0L,
    val shiftAktual: String = "",
    val keterangan: String = ""
)