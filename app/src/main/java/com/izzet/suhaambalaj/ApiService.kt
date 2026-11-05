package com.izzet.suhaambalaj

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// YENİ: Node.js'e gönderilecek varyant kalıbı
data class VaryantIstegi(
    val renk: String,
    val olcu: String,
    val ek_fiyat: Double,
    val stokta_var_mi: Boolean = true
)

// KALDIRILDI: UrunEkleIstegi (JSON + Base64 gövdesi). Ürün ekleme artık
// multipart/form-data ile yapılıyor; alanlar @PartMap, fotoğraflar @Part ile gidiyor.
data class UrunEkleCevabi(
    val basarili: Boolean,
    val mesaj: String,
    val urun: Urun?
)

// YENİ EKLENEN: Sipariş Detayları Kalıpları
data class SiparisDetayElemani(
    val miktar: Double,
    val birim_fiyati: Double,
    val ara_toplam: Double,
    val birim: String?,
    val urun_adi: String,
    val satis_birimi: String?,
    val varyant_bilgisi: String? = null
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

data class MusteriDurumCevap(
    val basarili: Boolean,
    val durum: String?,
    val sebep: String?
)

data class MusteriListesiCevabi(
    val basarili: Boolean,
    val musteriler: List<Musteri>?
)

data class MusteriOnayIstegi(
    val musteri_id: Int,
    val yeni_durum: String,
    val sebep: String? = null
)

// YENİ EKLENEN: Sadece Müşteri ID'sini Göndermek İçin Kullanılan İstek Kalıbı
data class MusteriIdIstegi(
    val musteri_id: Int
)

// 1. Müşteri Kayıt İstek/Cevap Kalıpları
data class KayitIstegi(
    val dukkanAdi: String,
    val yetkiliAdi: String,
    val telefon: String,
    val sifre: String,
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
    val onay_durumu: String?,
    val rol: String? = null // YENİ: "admin" veya "musteri"
)

data class ProfilGuncelleIstegi(
    val musteri_id: Int,
    val dukkan_adi: String,
    val yetkili_adi: String,
    val telefon: String
)

data class GenelCevap(
    val basarili: Boolean,
    val mesaj: String
)

data class Kategori(
    val id: Int,
    val kategori_adi: String,
    val ust_kategori_id: Int?
)

data class KategoriCevap(
    val basarili: Boolean,
    val mesaj: String?,
    val kategoriler: List<Kategori>?
)

// YENİ: Kategori ağacı (modern kategori ekranı + ürün eklerken kategori önerileri)
data class AltKategoriOzet(
    val id: Int,
    val kategori_adi: String,
    val urun_sayisi: Int?
)

data class KategoriAgaci(
    val id: Int,
    val kategori_adi: String,
    val urun_sayisi: Int?,
    val kapak_fotograf: String?,
    val alt_kategoriler: List<AltKategoriOzet>?
)

data class KategoriAgaciCevap(
    val basarili: Boolean,
    val mesaj: String?,
    val kategoriler: List<KategoriAgaci>?
)

data class AltKategoriCevap(
    val basarili: Boolean,
    val mesaj: String?,
    val altKategoriler: List<Kategori>?
)

// 2. Ürün Çekme İstek/Cevap Kalıpları
data class UrunCevap(
    val basarili: Boolean,
    val mesaj: String?,
    val urunler: List<Urun>?
)

// YENİ EKLENEN: Varyant Veri Sınıfı
data class Varyant(
    val id: Int,
    val renk: String?,
    val olcu: String?,
    val ek_fiyat: Double,
    val stokta_var_mi: Boolean
)

data class Urun(
    val id: Int,
    val kategori_id: Int,
    val kategori_adi: String?,
    val urun_adi: String,
    val marka: String?, // KESİNLİKLE BURADA OLMALI
    val satis_birimi: String,
    val birim_detayi: String?,
    val taban_fiyati: String,
    val fotograflar: List<String>?,
    val urun_tipi: String?,
    val koli_ici_paket: Int?,
    val paket_ici_adet: Int?,
    val olcu: String?,
    val stokta_var_mi: Boolean?,
    val varyantlar: List<Varyant>?
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
    val birim: String,
    val varyant_bilgisi: String? = null
)

data class SiparisIstegi(
    val musteri_id: Int,
    val toplam_tutar: Double,
    val sepet_urunleri: List<SiparisDetay>
)

// 4. YENİ: Sipariş Geçmişi Çekme Kalıpları
data class SiparisOzet(
    val id: Int,
    val toplam_tutar: String?,
    val siparis_durumu: String?,
    val siparis_tarihi: String?
)

data class SiparisGecmisiCevabi(
    val basarili: Boolean,
    val siparisler: List<SiparisOzet>?
)

// 5. API İsteklerimizi Tanımladığımız Arayüz
interface SuhaAmbalajApi {
    @POST("/api/kayit")
    suspend fun musteriKaydet(@Body istek: KayitIstegi): SunucuCevabi

    @POST("/api/giris")
    suspend fun girisYap(@Body istek: GirisIstegi): GirisCevabi

    @GET("/api/urunler")
    suspend fun urunleriGetir(): UrunCevap

    @POST("/api/siparis")
    suspend fun siparisGonder(@Body istek: SiparisIstegi): SunucuCevabi

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

    // GÜNCELLENDİ: Base64 JSON yerine multipart/form-data
    // alanlar   -> kategori_id, urun_adi, marka, satis_birimi, birim_detayi, taban_fiyati,
    //              stokta_var_mi, urun_tipi, koli_ici_paket, paket_ici_adet, varyantlar (JSON metni)
    // fotograflar -> "fotograflar" adlı dosya parçaları (en fazla 5)
    @Multipart
    @POST("/api/urun-ekle")
    suspend fun urunEkle(
        @PartMap alanlar: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part fotograflar: List<MultipartBody.Part>
    ): UrunEkleCevabi

    @GET("api/musteri-durum/{id}")
    suspend fun musteriDurumuGetir(@Path("id") musteriId: Int): MusteriDurumCevap

    @POST("api/musteri-guncelle")
    suspend fun profilGuncelle(@Body istek: ProfilGuncelleIstegi): GenelCevap

    @GET("api/kategoriler/ana")
    suspend fun anaKategorileriGetir(): KategoriCevap

    @GET("api/kategoriler/alt/{ust_id}")
    suspend fun altKategorileriGetir(@Path("ust_id") ustId: Int): AltKategoriCevap

    // YENİ: Ana kategoriler + alt kategoriler + ürün sayıları + kapak fotoğrafı
    @GET("api/kategoriler/agac")
    suspend fun kategoriAgaciniGetir(): KategoriAgaciCevap

    @GET("api/urunler/kategori/{kategori_id}")
    suspend fun kategoriUrunleriniGetir(@Path("kategori_id") kategoriId: Int): UrunCevap

    // YENİ EKLENEN: Konum bariyerini geçebilenleri onay kuyruğuna atan köprü
    @POST("/api/onay-kuyruguna-gir")
    suspend fun onayKuyrugunaGir(@Body istek: MusteriIdIstegi): GenelCevap
}

object ApiClient {
    private const val BASE_URL = "http://64.226.125.60:3000/"

    // YENİ: Fotoğraf yüklemesi yavaş mobil ağda 10 sn'lik varsayılanı aşabilir
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // YENİ: Veritabanındaki "/uploads/urun_xxx.jpg" yolunu tam adrese çevirir (Coil AsyncImage için).
    // Eski Base64 kayıtları ("data:image/...") Coil 2 tarafından açılamadığı için null döner (yer tutucu gösterilir).
    fun gorselUrl(yol: String?): String? {
        if (yol.isNullOrBlank() || yol.startsWith("data:")) return null
        if (yol.startsWith("http")) return yol
        return BASE_URL.trimEnd('/') + "/" + yol.trimStart('/')
    }

    val retrofitService: SuhaAmbalajApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SuhaAmbalajApi::class.java)
    }
}