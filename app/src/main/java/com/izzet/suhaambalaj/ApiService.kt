package com.izzet.suhaambalaj

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


data class UrunEkleIstegi(
    val kategori_id: Int,
    val urun_adi: String,
    val marka: String,
    val satis_birimi: String,
    val birim_detayi: String,
    val taban_fiyati: String,
    val fotograflar: List<String>?,
    val stokta_var_mi: Boolean
)

data class UrunEkleCevabi(
    val basarili: Boolean,
    val mesaj: String,
    val urun: Urun?
)


// YENİ EKLENEN: Sipariş Detayları Kalıpları
data class SiparisDetayElemani(
    val miktar: Double,
    val birim_fiyati: String,
    val ara_toplam: String,
    val urun_adi: String
)

data class SiparisDetayCevabi(
    val basarili: Boolean,
    val detaylar: List<SiparisDetayElemani>?
)


// YENİ EKLENEN: Yönetici İçin Tüm Siparişler Kalıpları
data class AdminSiparis(
    val siparis_id: Int,
    val toplam_tutar: String,
    val siparis_tarihi: String,
    val dukkan_adi: String?,
    val yetkili_adi: String?,
    val telefon: String?
)

data class TumSiparislerCevabi(
    val basarili: Boolean,
    val siparisler: List<AdminSiparis>?
)


// YENİ EKLENEN: Müşteri ve Yönetim Kalıpları
data class Musteri(
    val id: Int,
    val dukkan_adi: String?,
    val yetkili_adi: String?,
    val telefon: String?,
    val onay_durumu: String?
)

data class MusteriListesiCevabi(
    val basarili: Boolean,
    val musteriler: List<Musteri>?
)

data class MusteriOnayIstegi(
    val musteri_id: Int,
    val yeni_durum: String
)

// 1. Müşteri Kayıt İstek/Cevap Kalıpları
data class KayitIstegi(
    val dukkanAdi: String,
    val yetkiliAdi: String,
    val telefon: String,
    val sifre: String, // YENİ EKLENDİ
    val enlem: Double,
    val boylam: Double
)

data class SunucuCevabi(
    val basarili: Boolean,
    val mesaj: String
)

// 2. YENİ EKLENEN: Giriş Yap İstek/Cevap Kalıpları
data class GirisIstegi(
    val telefon: String,
    val sifre: String
)

data class GirisCevabi(
    val basarili: Boolean,
    val mesaj: String,
    val musteri_id: Int?,
    val yetkili_adi: String?,
    val onay_durumu: String?
)

// ... (Buradan sonrası aynı: UrunCevabi, SiparisDetay vb.)

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
    val kategori_adi: String,
    val stokta_var_mi: Boolean? = true
)

// YENİ EKLENEN: Ayar ve Stok Kalıpları
data class AyarDurumuCevabi(val basarili: Boolean, val fiyatlar_gizli_mi: Boolean?)
data class FiyatGuncelleIstegi(val gizli_mi: Boolean)
data class StokGuncelleIstegi(val urun_id: Int, val stokta_var_mi: Boolean)


// 3. Sipariş Gönderme Kalıpları
data class SiparisDetay(
    val urun_id: Int,
    val miktar: Double,
    val birim_fiyati: Double,
    val ara_toplam: Double,
    val birim: String // YENİ EKLENDİ
)

data class SiparisIstegi(
    val musteri_id: Int,
    val toplam_tutar: Double,
    val sepet_urunleri: List<SiparisDetay>
)

// 4. YENİ: Sipariş Geçmişi Çekme Kalıpları
// YENİ: Sipariş Geçmişi Çekme Kalıpları
data class SiparisOzet(
    val id: Int,
    val toplam_tutar: String, // Numeric değer string olarak gelebilir
    val siparis_durumu: String,
    val siparis_tarihi: String
)

data class SiparisGecmisiCevabi(
    val basarili: Boolean,
    val siparisler: List<SiparisOzet>?
)

// 5. API İsteklerimizi Tanımladığımız Arayüz
interface SuhaAmbalajApi {
    @POST("/api/kayit")
    suspend fun musteriKaydet(@Body istek: KayitIstegi): SunucuCevabi

    // YENİ: Giriş yapma köprüsü
    @POST("/api/giris")
    suspend fun girisYap(@Body istek: GirisIstegi): GirisCevabi

    @GET("/api/urunler")
    suspend fun urunleriGetir(): UrunCevabi

    @POST("/api/siparis")
    suspend fun siparisGonder(@Body istek: SiparisIstegi): SunucuCevabi

    // Linkte süslü parantez varsa, fonksiyonda kesinlikle @Path olmak zorundadır!
    @GET("/api/siparislerim/{musteri_id}")
    suspend fun siparisGecmisiniGetir(@Path("musteri_id") id: Int): SiparisGecmisiCevabi
    @GET("/api/musteriler")
    suspend fun musterileriGetir(): MusteriListesiCevabi

    @POST("/api/musteri-onay")
    suspend fun musteriOnayGuncelle(@Body istek: MusteriOnayIstegi): SunucuCevabi

    @GET("/api/tum-siparisler")
    suspend fun tumSiparisleriGetir(): TumSiparislerCevabi

    @GET("/api/siparis-detay/{siparis_id}")
    suspend fun siparisDetayiGetir(@Path("siparis_id") siparisId: Int): SiparisDetayCevabi

    @GET("/api/ayarlar/fiyat-durumu")
    suspend fun fiyatDurumuGetir(): AyarDurumuCevabi

    @POST("/api/ayarlar/fiyat-guncelle")
    suspend fun fiyatGizlilikGuncelle(@Body istek: FiyatGuncelleIstegi): SunucuCevabi

    @POST("/api/urun/stok-guncelle")
    suspend fun urunStokGuncelle(@Body istek: StokGuncelleIstegi): SunucuCevabi


    @POST("/api/urun-ekle")
    suspend fun urunEkle(@Body istek: UrunEkleIstegi): UrunEkleCevabi


}

// 6. Retrofit İstemcisi (Motoru)
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