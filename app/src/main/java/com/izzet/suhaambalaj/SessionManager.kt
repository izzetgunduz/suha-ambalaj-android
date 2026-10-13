package com.izzet.suhaambalaj

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    // Telefonun hafızasında sadece bizim uygulamamızın ulaşabileceği gizli bir kasa açıyoruz
    private val prefs: SharedPreferences = context.getSharedPreferences("SuhaB2BOturum", Context.MODE_PRIVATE)

    // Müşteri doğru şifreyle girdiğinde bilgilerini kasaya kilitliyoruz
    fun oturumuKaydet(musteriId: Int, yetkiliAdi: String, onayDurumu: String) {
        prefs.edit().apply {
            putInt("MUSTERI_ID", musteriId)
            putString("YETKILI_ADI", yetkiliAdi)
            putString("ONAY_DURUMU", onayDurumu)
            putBoolean("GIRIS_YAPILDI", true)
            apply() // Mühürle
        }
    }

    // Uygulama açıldığında kasaya bakıp "İçeride biri var mı?" diye soruyoruz
    fun girisYapildiMi(): Boolean {
        return prefs.getBoolean("GIRIS_YAPILDI", false)
    }

    // Sepetten sipariş verirken "Bu siparişi kim veriyor?" sorusunun cevabı
    fun musteriIdGetir(): Int {
        return prefs.getInt("MUSTERI_ID", -1) // -1 demek kimse yok demek
    }

    fun onayDurumuGetir(): String {
        return prefs.getString("ONAY_DURUMU", "bekliyor") ?: "bekliyor"
    }

    // Müşteri "Çıkış Yap" dediğinde kasayı tamamen boşaltıyoruz
    fun cikisYap() {
        prefs.edit().clear().apply()
    }

    fun yetkiliAdiGetir(): String {
        return prefs.getString("YETKILI_ADI", "Müşteri") ?: "Müşteri"
    }
}