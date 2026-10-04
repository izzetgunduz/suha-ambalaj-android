package com.izzet.suhaambalaj

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. Sunucuya Göndereceğimiz Müşteri Kayıt Paketi (JSON formatına dönüşecek)
data class KayitIstegi(
    val dukkanAdi: String,
    val yetkiliAdi: String,
    val telefon: String,
    val enlem: Double,
    val boylam: Double
)

// 2. Sunucudan (Node.js) Bize Gelecek Cevap Paketi
data class SunucuCevabi(
    val basarili: Boolean,
    val mesaj: String
)

// 3. API İsteklerimizi Tanımladığımız Arayüz
interface SuhaAmbalajApi {
    // İleride Node.js tarafında "/api/kayit" adında bir adres (endpoint) oluşturacağız
    @POST("/api/kayit")
    suspend fun musteriKaydet(@Body istek: KayitIstegi): SunucuCevabi
}

// 4. Retrofit İstemcisi (Motoru)
object ApiClient {
    /*
     ÇOK ÖNEMLİ: Android Emülatörü kendi içinde sanal bir cihaz olduğu için
     bilgisayarındaki Node.js'e (localhost) doğrudan ulaşamaz.
     Emülatörün senin bilgisayarına bağlanması için "10.0.2.2" IP'si kullanılır.
    */
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val retrofitService: SuhaAmbalajApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Verileri otomatik JSON yapar
            .build()
            .create(SuhaAmbalajApi::class.java)
    }
}