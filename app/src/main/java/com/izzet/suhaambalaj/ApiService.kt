package com.izzet.suhaambalaj

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 1. Müşteri Kayıt İstek/Cevap Kalıpları
data class KayitIstegi(
    val dukkanAdi: String,
    val yetkiliAdi: String,
    val telefon: String,
    val enlem: Double,
    val boylam: Double
)

data class SunucuCevabi(
    val basarili: Boolean,
    val mesaj: String
)

// 2. Ürün Çekme İstek/Cevap Kalıpları
data class UrunCevabi(
    val basarili: Boolean,
    val urun_sayisi: Int,
    val urunler: List<Urun>
)

data class Urun(
    val id: Int,
    val urun_adi: String,
    val marka: String,
    val satis_birimi: String,
    val birim_detayi: String,
    val taban_fiyati: String,
    val urun_tipi: String,
    val koli_ici_paket: Int,
    val paket_ici_adet: Int,
    val kategori_adi: String
)

// 3. YENİ: Sipariş Gönderme Kalıpları (Sepetteki her ürün ve genel sipariş kapağı)
data class SiparisDetay(
    val urun_id: Int,
    val miktar: Double,
    val birim_fiyati: Double,
    val ara_toplam: Double
)

data class SiparisIstegi(
    val musteri_id: Int,
    val toplam_tutar: Double,
    val sepet_urunleri: List<SiparisDetay>
)

// 4. API İsteklerimizi Tanımladığımız Arayüz
interface SuhaAmbalajApi {
    @POST("/api/kayit")
    suspend fun musteriKaydet(@Body istek: KayitIstegi): SunucuCevabi

    @GET("/api/urunler")
    suspend fun urunleriGetir(): UrunCevabi

    // YENİ: Sepeti Node.js'e fırlatacak köprü
    @POST("/api/siparis")
    suspend fun siparisGonder(@Body istek: SiparisIstegi): SunucuCevabi
}

// 5. Retrofit İstemcisi (Motoru)
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val retrofitService: SuhaAmbalajApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SuhaAmbalajApi::class.java)
    }
}