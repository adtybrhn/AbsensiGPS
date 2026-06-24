package com.example.absensigps

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit



class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var layoutBeranda: LinearLayout
    private lateinit var layoutLaporan: LinearLayout
    private lateinit var layoutProfil: LinearLayout

    private lateinit var tvStatus: TextView
    private lateinit var tvWaktuMasuk: TextView
    private lateinit var tvTotalJam: TextView
    private lateinit var btnMasuk: Button
    private lateinit var btnKeluar: Button
    private lateinit var btnMenu: TextView
    private lateinit var tvAppTitle: TextView
    private lateinit var tvProfil: TextView
    private lateinit var spinnerShift: Spinner

    private lateinit var btnHarian: Button
    private lateinit var btnMingguan: Button
    private lateinit var btnBulanan: Button
    private lateinit var tvIsiLaporan: TextView

    private lateinit var ivProfileFoto: ImageView
    private lateinit var btnUbahFoto: Button
    private lateinit var tvProfileNama: TextView
    private lateinit var tvProfileId: TextView
    private lateinit var tvProfileGolongan: TextView

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: FirebaseFirestore

    private lateinit var currentUserId: String
    private var isShiftWorker = true
    private val PICK_IMAGE_REQUEST = 101

    private val TARGET_LAT = -6.1927
    private val TARGET_LON = 106.7975
    private val RADIUS_MAKSIMAL_METER = 2000f

    // Variabel state absensi hari ini
    private var tglSekarang = ""
    private var docIdHariIni = ""
    private var waktuMasukHariIni = 0L

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PREF_NAME = "AbsensiData"
    }

    private fun keyFotoProfil() = "profile_image_uri_$currentUserId"

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase Database
        db = FirebaseFirestore.getInstance()

        layoutBeranda = findViewById(R.id.layoutBeranda)
        layoutLaporan = findViewById(R.id.layoutLaporan)
        layoutProfil = findViewById(R.id.layoutProfil)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tvStatus = findViewById(R.id.tvStatus)
        tvWaktuMasuk = findViewById(R.id.tvWaktuMasuk)
        tvTotalJam = findViewById(R.id.tvTotalJam)
        btnMasuk = findViewById(R.id.btnMasuk)
        btnKeluar = findViewById(R.id.btnKeluar)
        btnMenu = findViewById(R.id.btnMenu)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        tvProfil = findViewById(R.id.tvProfil)
        spinnerShift = findViewById(R.id.spinnerShift)
        btnHarian = findViewById(R.id.btnHarian)
        btnMingguan = findViewById(R.id.btnMingguan)
        btnBulanan = findViewById(R.id.btnBulanan)
        tvIsiLaporan = findViewById(R.id.tvIsiLaporan)
        ivProfileFoto = findViewById(R.id.ivProfileFoto)
        btnUbahFoto = findViewById(R.id.btnUbahFoto)
        tvProfileNama = findViewById(R.id.tvProfileNama)
        tvProfileId = findViewById(R.id.tvProfileId)
        tvProfileGolongan = findViewById(R.id.tvProfileGolongan)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        currentUserId = sharedPref.getString("user_id", "001") ?: "001"
        isShiftWorker = sharedPref.getBoolean("is_shift_worker", true)

        tglSekarang = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        docIdHariIni = "${currentUserId}_${tglSekarang}"

        setupSpinnerShift()
        setupSidebarAndLocalProfile()

        // Membaca status absen hari ini langsung dari Server
        sinkronisasiStatusAbsenServer()

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_beranda -> {
                    tvAppTitle.text = "Beranda"
                    layoutBeranda.visibility = View.VISIBLE
                    layoutLaporan.visibility = View.GONE
                    layoutProfil.visibility = View.GONE
                    sinkronisasiStatusAbsenServer()
                }
                R.id.nav_laporan -> {
                    tvAppTitle.text = "Laporan"
                    layoutBeranda.visibility = View.GONE
                    layoutLaporan.visibility = View.VISIBLE
                    layoutProfil.visibility = View.GONE
                    setTabLaporanAktif(btnHarian)
                    loadRiwayatAbsensi()
                }
                R.id.nav_profil -> {
                    tvAppTitle.text = "Profil"
                    layoutBeranda.visibility = View.GONE
                    layoutLaporan.visibility = View.GONE
                    layoutProfil.visibility = View.VISIBLE
                }
                R.id.nav_logout -> {
                    sharedPref.edit().putBoolean("is_logged_in", false).apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnHarian.setOnClickListener {
            setTabLaporanAktif(btnHarian)
            loadRiwayatAbsensi()
        }
        btnMingguan.setOnClickListener {
            setTabLaporanAktif(btnMingguan)
            tvIsiLaporan.text = "Fitur laporan mingguan sedang dikembangkan."
        }
        btnBulanan.setOnClickListener {
            setTabLaporanAktif(btnBulanan)
            tvIsiLaporan.text = "Fitur laporan bulanan sedang dikembangkan."
        }

        btnMasuk.setOnClickListener {
            if (waktuMasukHariIni != 0L) {
                // Tombol bisa diklik, tapi muncul peringatan
                AlertDialog.Builder(this)
                    .setTitle("Info")
                    .setMessage("Anda sudah melakukan Tap-In hari ini.")
                    .setPositiveButton("OK", null).show()
            } else {
                cekLokasiDanAbsen(isMasuk = true, alasan = "")
            }
        }

        btnKeluar.setOnClickListener {
            // 1. Cek langsung ke Firebase apakah data hari ini sudah ada dan belum Tap-Out
            db.collection("Absensi").document(docIdHariIni).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val absen = doc.toObject(LaporanAbsensi::class.java)

                    // Cek apakah sudah Tap-In (waktuMasuk ada) dan belum Tap-Out (waktuKeluar masih 0)
                    if (absen != null && absen.waktuMasuk != 0L && absen.waktuKeluar == 0L) {
                        // Semua kondisi OK, tampilkan dialog alasan
                        tampilkanDialogAlasanTapOut()
                    } else if (absen != null && absen.waktuKeluar != 0L) {
                        // Sudah Tap-Out sebelumnya
                        AlertDialog.Builder(this)
                        .setTitle("Info")
                        .setMessage("Anda sudah melakukan Tap-Out hari ini.")
                        .setPositiveButton("OK", null).show()
                    } else {
                        // Data ada tapi mungkin rusak atau tidak sesuai kondisi
                        Toast.makeText(this, "Data absensi tidak valid.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Data hari ini belum ada di Firebase
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Peringatan")
                        .setMessage("Anda belum melakukan Tap-In hari ini!")
                        .setPositiveButton("Mengerti", null).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal terhubung ke server.", Toast.LENGTH_SHORT).show()
            }
        }

        val savedImageUri = sharedPref.getString(keyFotoProfil(), null)
        if (savedImageUri != null) ivProfileFoto.setImageURI(Uri.parse(savedImageUri))

        btnUbahFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    private fun setupSpinnerShift() {
        val shiftOptions = arrayOf("Shift 1 (08:00 - 17:00)", "Shift 2 (15:00 - 00:00)", "Shift 3 (00:00 - 08:00)")
        val adapter = ArrayAdapter(this, R.layout.custom_spinner_item, shiftOptions)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        spinnerShift.adapter = adapter

        if (!isShiftWorker) {
            spinnerShift.visibility = View.GONE
            findViewById<TextView>(R.id.tvLabelShift).visibility = View.GONE
        } else {
            val savedShift = sharedPref.getString("shift_kerja", "Shift 1 (08:00 - 17:00)")
            val spinnerPosition = adapter.getPosition(savedShift)
            if (spinnerPosition >= 0) spinnerShift.setSelection(spinnerPosition)
        }
    }

    private fun sinkronisasiStatusAbsenServer() {
        tvStatus.text = "Memeriksa data server..."
        db.collection("Absensi").document(docIdHariIni).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val absen = doc.toObject(LaporanAbsensi::class.java)
                    if (absen != null) {
                        waktuMasukHariIni = absen.waktuMasuk
                        val stringWaktuMasuk = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(waktuMasukHariIni))
                        tvWaktuMasuk.text = "🕒 Masuk : $stringWaktuMasuk"

                        if (absen.waktuKeluar == 0L) {
                            tvStatus.text = "🟢 Sedang Bekerja"
                            tvStatus.setTextColor(Color.parseColor("#388E3C"))
                        } else {
                            tvStatus.text = "✅ Selesai Bekerja Hari Ini"
                            tvStatus.setTextColor(Color.parseColor("#1976D2"))
                            // Hitung total jam
                            val selisih = absen.waktuKeluar - absen.waktuMasuk
                            val jam = TimeUnit.MILLISECONDS.toHours(selisih)
                            val mnt = TimeUnit.MILLISECONDS.toMinutes(selisih) % 60
                            tvTotalJam.text = "💼 Total : $jam Jam, $mnt Menit"
                        }
                    }
                } else {
                    waktuMasukHariIni = 0L
                    tvWaktuMasuk.text = "🕒 Masuk : Belum absen"
                    tvStatus.text = "⚪ Silakan lakukan absensi"
                    tvStatus.setTextColor(Color.parseColor("#212121"))
                }
            }
            .addOnFailureListener {
                tvStatus.text = "Koneksi terputus."
            }
    }



    private fun loadRiwayatAbsensi() {
        tvIsiLaporan.text = "Memuat data dari server..."
        db.collection("Absensi")
            .whereEqualTo("idKaryawan", currentUserId)
            .orderBy("waktuMasuk", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvIsiLaporan.text = "Belum ada riwayat absensi."
                    return@addOnSuccessListener
                }

                val rekapBuilder = StringBuilder()
                val formatJam = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                // Cukup gunakan SATU perulangan saja
                for (doc in documents) {
                    val absen = doc.toObject(LaporanAbsensi::class.java)
                    if (absen != null) {
                        val jamMasuk = formatJam.format(Date(absen.waktuMasuk))
                        val jamKeluar = if (absen.waktuKeluar == 0L) "Belum Tap-Out" else formatJam.format(Date(absen.waktuKeluar))

                        // Logika Perhitungan Durasi
                        var totalJamKerja = ""
                        if (absen.waktuKeluar != 0L) {
                            val selisih = absen.waktuKeluar - absen.waktuMasuk
                            val j = TimeUnit.MILLISECONDS.toHours(selisih)
                            val m = TimeUnit.MILLISECONDS.toMinutes(selisih) % 60
                            totalJamKerja = "\n  Total Kerja: $j Jam, $m Menit"
                        }

                        // Gabungkan ke builder
                        rekapBuilder.append("• Tanggal: ${absen.tanggal}\n")
                        rekapBuilder.append("  Shift: ${absen.shiftAktual}\n")
                        rekapBuilder.append("  Jam: $jamMasuk - $jamKeluar $totalJamKerja\n")
                        rekapBuilder.append("  Keterangan: ${absen.keterangan}\n\n")
                    }
                }
                // Tampilkan hasil akhir
                tvIsiLaporan.text = rekapBuilder.toString()
            }
            .addOnFailureListener { e ->
                tvIsiLaporan.text = "Gagal memuat riwayat: ${e.message}"
            }
    }

    private fun setTabLaporanAktif(tombolAktif: Button) {
        val inactiveBgColor = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
        val inactiveTextColor = Color.parseColor("#555555")

        btnHarian.backgroundTintList = inactiveBgColor
        btnHarian.setTextColor(inactiveTextColor)
        btnMingguan.backgroundTintList = inactiveBgColor
        btnMingguan.setTextColor(inactiveTextColor)
        btnBulanan.backgroundTintList = inactiveBgColor
        btnBulanan.setTextColor(inactiveTextColor)

        tombolAktif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1976D2"))
        tombolAktif.setTextColor(Color.parseColor("#FFFFFF"))
    }

    private fun setupSidebarAndLocalProfile() {
        val username = sharedPref.getString("username", "Pengguna")
        val golonganTeks = if (!isShiftWorker) "Non-Shift" else "Shift"

        tvProfil.text = "Selamat datang, $username!"

        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvHeaderNama).text = username?.uppercase(Locale.getDefault())
        headerView.findViewById<TextView>(R.id.tvHeaderId).text = "ID Karyawan: $currentUserId"
        headerView.findViewById<TextView>(R.id.tvHeaderShift).text = "Golongan: $golonganTeks"

        tvProfileNama.text = "Nama: $username"
        tvProfileId.text = "ID Karyawan: $currentUserId"
        tvProfileGolongan.text = "Golongan: $golonganTeks"
    }

    private fun tampilkanDialogAlasanTapOut() {
        val input = EditText(this)
        input.hint = "Ketik alasan (ex: absen pulang)"
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Tap-Out")
            .setView(input)
            .setPositiveButton("Simpan & Keluar") { _, _ ->
                val alasan = input.text.toString().trim()
                if (alasan.isNotEmpty()) cekLokasiDanAbsen(isMasuk = false, alasan = alasan)
                else Toast.makeText(this, "Alasan wajib diisi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .setCancelable(false).show()
    }

    private fun cekLokasiDanAbsen(isMasuk: Boolean, alasan: String) {
        // Mencegah Tap-In ganda jika server mengonfirmasi sudah absen
        if (isMasuk && waktuMasukHariIni != 0L) {
            AlertDialog.Builder(this)
                .setTitle("⛔ Ditolak")
                .setMessage("Anda sudah melakukan Tap-In hari ini.")
                .setPositiveButton("Tutup", null).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        tvStatus.text = "Memeriksa lokasi Anda..."
        fusedLocationClient.lastLocation.addOnSuccessListener { currentLocation: Location? ->
            if (currentLocation != null) prosesAbsensiSesuaiAturan(currentLocation, isMasuk, alasan)
            else tvStatus.text = "Gagal mendapatkan lokasi GPS."
        }
    }

    private fun prosesAbsensiSesuaiAturan(currentLocation: Location, isMasuk: Boolean, alasan: String) {
        val targetLocation = Location("Target").apply { latitude = TARGET_LAT; longitude = TARGET_LON }
        val distance = currentLocation.distanceTo(targetLocation)

        if (isMasuk) {
            if (distance <= RADIUS_MAKSIMAL_METER) {
                val selectedShift = if (!isShiftWorker) "Non-Shift" else spinnerShift.selectedItem.toString()

                // --- CREATE DATA KE FIREBASE ---
                val absensiBaru = LaporanAbsensi(
                    idKaryawan = currentUserId,
                    tanggal = tglSekarang,
                    waktuMasuk = System.currentTimeMillis(),
                    waktuKeluar = 0L,
                    shiftAktual = selectedShift,
                    keterangan = "-"
                )

                db.collection("Absensi").document(docIdHariIni).set(absensiBaru)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tap-In Berhasil Tersimpan di Server!", Toast.LENGTH_LONG).show()
                        sinkronisasiStatusAbsenServer()
                    }
                    .addOnFailureListener { e ->
                        tvStatus.text = "Gagal menyimpan ke server: ${e.message}"
                    }
            } else {
                tvStatus.text = "❌ Anda diluar jangkauan yang telah ditentukan"
                tvStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
        } else {
            // --- UPDATE DATA KE FIREBASE ---
            val pembaruanData = mapOf(
                "waktuKeluar" to System.currentTimeMillis(),
                "keterangan" to alasan
            )

            db.collection("Absensi").document(docIdHariIni).update(pembaruanData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Tap-Out Berhasil Tersimpan di Server!", Toast.LENGTH_LONG).show()
                    sinkronisasiStatusAbsenServer()
                }
                .addOnFailureListener { e ->
                    tvStatus.text = "Gagal update server: ${e.message}"
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sharedPref.edit().putString(keyFotoProfil(), uri.toString()).apply()
                ivProfileFoto.setImageURI(uri)
                Toast.makeText(this, "Foto Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}