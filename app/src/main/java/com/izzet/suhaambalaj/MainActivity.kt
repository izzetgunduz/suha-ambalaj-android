package com.izzet.suhaambalaj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izzet.suhaambalaj.ui.theme.SuhaAmbalajTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuhaAmbalajTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AnaUygulamaEkrani()
                }
            }
        }
    }
}

data class SepetElemani(val urun: Urun, var miktar: Double, var secilenBirim: String)

@Composable
fun AnaUygulamaEkrani() {
    var seciliSekme by remember { mutableIntStateOf(0) }
    var aktifAltKategoriId by remember { mutableStateOf<Int?>(null) }
    var aktifAltKategoriAdi by remember { mutableStateOf("") }
    var seciliSiparisId by remember { mutableIntStateOf(0) }
    var fiyatlarGizliMi by remember { mutableStateOf(false) }
    var sepetListesi = remember { mutableStateListOf<SepetElemani>() }
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }

    LaunchedEffect(seciliSekme) {
        try {
            val ayarCevap = ApiClient.retrofitService.fiyatDurumuGetir()
            if (ayarCevap.basarili) { fiyatlarGizliMi = ayarCevap.fiyatlar_gizli_mi ?: false }

            val urunCevap = ApiClient.retrofitService.urunleriGetir()
            if (urunCevap.basarili && urunCevap.urunler != null) {
                urunListesi = urunCevap.urunler.filter { it.stokta_var_mi == true }
            }
        } catch (e: Exception) {
        } finally {
            yukleniyor = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                NavigationBarItem(icon = { Icon(Icons.Filled.Home, "Ana Sayfa") }, label = { Text("Ana Sayfa") }, selected = seciliSekme == 0, onClick = { seciliSekme = 0 })
                NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Filled.List, "Kategoriler") }, label = { Text("Kategoriler") }, selected = seciliSekme == 1, onClick = { seciliSekme = 1 })
                NavigationBarItem(icon = { Icon(Icons.Filled.ShoppingCart, "Sepetim") }, label = { Text("Sepetim") }, selected = seciliSekme == 2, onClick = { seciliSekme = 2 })
                // Alt menüde 4. sekme yok, Profil yine 3 numara
                NavigationBarItem(icon = { Icon(Icons.Filled.Person, "Profil") }, label = { Text("Profil") }, selected = seciliSekme == 3 || seciliSekme == 4, onClick = { seciliSekme = 3 })
            }
        }
    ) { paddingValues ->
        // AnaUygulamaEkrani içindeki when(seciliSekme) bloğunu bul ve şu şekilde değiştir:
        Box(modifier = Modifier.padding(paddingValues)) {
            when (seciliSekme) {
                0 -> AnaSayfaVitrini(sepetListesi, fiyatlarGizliMi)

                1 -> {
                    if (aktifAltKategoriId != null) {
                        // Eğer bir alt kategoriye tıklandıysa, o kategorinin ürünlerini göster
                        KategoriUrunleriEkrani(
                            kategoriId = aktifAltKategoriId!!,
                            kategoriAdi = aktifAltKategoriAdi,
                            fiyatlarGizliMi = fiyatlarGizliMi, // YENİ EKLENDİ
                            kullaniciRol = "musteri", // YENİ EKLENDİ,       // YENİ EKLENDİ
                            geriyeDon = { aktifAltKategoriId = null },
                            sepeteEkle = { urun ->
                                // Ana sayfadaki sepete ekleme mantığının aynısı
                                // Ana sayfadaki sepete ekleme mantığının aynısı
                                val mevcut = sepetListesi.find { it.urun.id == urun.id }
                                if (mevcut != null) {
                                    mevcut.miktar += 1.0
                                } else {
                                    sepetListesi.add(
                                        SepetElemani(
                                            urun = urun,
                                            miktar = 1.0,
                                            secilenBirim = urun.satis_birimi ?: "Adet"
                                        )
                                    )
                                }
                            }
                        )
                    } else {
                        // Herhangi bir şeye tıklanmadıysa ana kategori akordeon listesini göster
                        KategorilerEkrani(
                            altKategoriyeGit = { id, ad ->
                                aktifAltKategoriId = id
                                aktifAltKategoriAdi = ad
                            }
                        )
                    }
                }

                // YENİ: Sepet ekranına profil düzenleme sayfasına gitme köprüsünü verdik
                2 -> SepetEkrani(
                    sepetListesi = sepetListesi,
                    fiyatlarGizliMi = fiyatlarGizliMi,
                    profilDuzenlemeyeGit = { seciliSekme = 5 }
                )

                3 -> ProfilVeSiparisEkrani(
                    detaylaraGit = { tiklananId ->
                        seciliSiparisId = tiklananId
                        seciliSekme = 4
                    }
                )

                4 -> SiparisDetayEkrani(
                    siparisId = seciliSiparisId,
                    geriyeDon = { seciliSekme = 3 }
                )

                // YENİ: 5. GİZLİ SEKME (Profil Düzenleme)
                5 -> ProfilDuzenlemeEkrani(
                    geriyeDon = { seciliSekme = 2 } // Geri tuşuyla tekrar sepete dön
                )
            }
        }
    }
}

// --- YENİ EKLENEN PROFİL VE SİPARİŞ GEÇMİŞİ EKRANI ---
@Composable
fun ProfilVeSiparisEkrani(detaylaraGit: (Int) -> Unit) { // YENİ: Parametre eklendi
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var girisYapildiMi by remember { mutableStateOf(sessionManager.girisYapildiMi()) }
    var aktifSayfa by remember { mutableStateOf("karsilama") }

    val onayDurumu = sessionManager.onayDurumuGetir()

    if (girisYapildiMi) {
        if (onayDurumu == "admin") {
            YoneticiPaneliEkrani(
                cikisYap = {
                    sessionManager.cikisYap()
                    girisYapildiMi = false
                    aktifSayfa = "karsilama"
                }
            )
        } else {
            GecmisSiparislerEkrani(
                cikisYap = {
                    sessionManager.cikisYap()
                    girisYapildiMi = false
                    aktifSayfa = "karsilama"
                },
                detaylaraGit = detaylaraGit // YENİ: Yetkiyi içeri aktardık
            )
        }
    } else {
        when (aktifSayfa) {
            "karsilama" -> KarsilamaEkrani(
                girisYapTikladi = { aktifSayfa = "giris" },
                kayitOlTikladi = { aktifSayfa = "kayit" }
            )
            "giris" -> GirisYapEkrani(
                geriyeDon = { aktifSayfa = "karsilama" },
                basariliGiris = { girisYapildiMi = true }
            )
            "kayit" -> KayitOlEkrani(
                geriyeDon = { aktifSayfa = "karsilama" },
                basariliKayit = { girisYapildiMi = true }
            )
        }
    }
}

// --- YENİ EKLENEN: SADECE YÖNETİCİLERİN GÖREBİLECEĞİ GİZLİ EKRAN ---
@Composable
fun YoneticiPaneliEkrani(cikisYap: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val yetkiliAdi = sessionManager.yetkiliAdiGetir()

    var aktifAltSayfa by remember { mutableStateOf("ana_panel") }
    var seciliSiparisId by remember { mutableStateOf(-1) } // YENİ: Hangi siparişe tıklandığını aklında tutacak

    when (aktifAltSayfa) {
        "musteri_yonetimi" -> MusteriYonetimEkrani(geriyeDon = { aktifAltSayfa = "ana_panel" })
        "tum_siparisler" -> TumSiparislerEkrani(
            geriyeDon = { aktifAltSayfa = "ana_panel" },
            detayaGit = { siparisId ->
                seciliSiparisId = siparisId
                aktifAltSayfa = "siparis_detay" // Tıklanınca detay sayfasına fırlat
            }
        )
        "siparis_detay" -> SiparisDetayEkrani(
            siparisId = seciliSiparisId,
            geriyeDon = { aktifAltSayfa = "tum_siparisler" } // Geri dönünce listeye atsın
        )
        // YENİ EKLENEN ROTA: Katalog Yönetimi
        "katalog_yonetimi" -> UrunVeKatalogYonetimiEkrani(geriyeDon = { aktifAltSayfa = "ana_panel" })

        else -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Patron Paneli", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { cikisYap() }) { Text("Çıkış Yap", color = Color.Red, fontWeight = FontWeight.Bold) }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hoş Geldin, $yetkiliAdi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Yetki Seviyesi: SÜPER YÖNETİCİ", color = Color(0xFF10B981), modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.Bold)
                    }
                }

                Text("Yönetim İşlemleri", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

                Button(onClick = { aktifAltSayfa = "musteri_yonetimi" }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp)) { Text("Müşteri Onay ve Yönetimi", fontSize = 16.sp) }
                Button(onClick = { aktifAltSayfa = "tum_siparisler" }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp)) { Text("Gelen Tüm Siparişler", fontSize = 16.sp) }

                // GÜNCELLENEN BUTON: Artık tıklanınca katalog yönetimine gidiyor
                Button(
                    onClick = { aktifAltSayfa = "katalog_yonetimi" },
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Ürün ve Katalog Yönetimi", fontSize = 16.sp)
                }
            }
        }
    }
}

// --- YENİ EKLENEN: TÜM SİPARİŞLERİ GÖSTEREN YÖNETİCİ EKRANI ---
@Composable
fun TumSiparislerEkrani(geriyeDon: () -> Unit, detayaGit: (Int) -> Unit) {
    var siparisListesi by remember { mutableStateOf<List<AdminSiparis>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.tumSiparisleriGetir()
            if (cevap.basarili && cevap.siparisler != null) { siparisListesi = cevap.siparisler }
            else { hataMesaji = "Sipariş bulunamadı." }
        } catch (e: Exception) { hataMesaji = "Sunucuya bağlanılamadı." }
        finally { yukleniyor = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(end = 8.dp)) { Text("← Geri", color = Color.Gray, fontWeight = FontWeight.Bold) }
            Text("Tüm Siparişler", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (yukleniyor) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (siparisListesi.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sistemde sipariş yok.", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(siparisListesi) { siparis ->
                    AdminSiparisKarti(siparis, detayaGit) // YENİ: Kartlara tıklandığında detaya gidecek yetkiyi veriyoruz
                }
            }
        }
    }
}

@Composable
fun AdminSiparisKarti(siparis: AdminSiparis, detayaGit: (Int) -> Unit) {
    val tarih = siparis.siparis_tarihi.take(10)
    val dukkanAdi = siparis.dukkan_adi ?: "Bilinmeyen İşletme"

    // YENİ: Modifier'a 'clickable' eklendi! Artık karta tıklanabiliyor.
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { detayaGit(siparis.siparis_id) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Sipariş #${siparis.siparis_id}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "${siparis.toplam_tutar} ₺", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp).background(Color.LightGray.copy(alpha = 0.5f)))
            Text(text = "İşletme: $dukkanAdi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "Tarih: $tarih", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

            Text(text = "İçeriği Görmek İçin Tıklayın →", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp).align(Alignment.End))
        }
    }
}

@Composable
fun SiparisDetayEkrani(siparisId: Int, geriyeDon: () -> Unit) {
    var detayListesi by remember { mutableStateOf<List<SiparisDetayElemani>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    LaunchedEffect(siparisId) {
        try {
            val cevap = ApiClient.retrofitService.siparisDetayiGetir(siparisId)
            if (cevap.basarili && cevap.detaylar != null) { detayListesi = cevap.detaylar }
            else { hataMesaji = "Sipariş detayları bulunamadı." }
        } catch (e: Exception) { hataMesaji = "Sunucu hatası: ${e.message}" }
        finally { yukleniyor = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(end = 8.dp)) { Text("← Geri", color = Color.Gray, fontWeight = FontWeight.Bold) }
            Text("Sipariş #$siparisId Detayı", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (yukleniyor) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty() && detayListesi.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(hataMesaji, color = Color.Red) }
        } else {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) { Text("Sipariş Detayı", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(detayListesi) { urun ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = urun.urun_adi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Birim Fiyatı: ${urun.birim_fiyati} ₺", color = Color.Gray, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            // YENİ: Küsurat kontrolü ve veritabanından gelen gerçek birim yazdırılıyor
                            val formatliMiktar = if (urun.miktar % 1.0 == 0.0) urun.miktar.toInt().toString() else urun.miktar.toString()
                            val birimYazisi = urun.birim ?: ""

                            Text(text = "$formatliMiktar $birimYazisi", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Toplam: ${urun.ara_toplam} ₺", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun UrunVeKatalogYonetimiEkrani(geriyeDon: () -> Unit) {
    var urunEkleDialogAcik by remember { mutableStateOf(false) }
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var fiyatlarGizli by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Sayfa açıldığında hem ürünleri hem de fiyat gizlilik ayarını çekiyoruz
    LaunchedEffect(Unit) {
        try {
            val ayarCevap = ApiClient.retrofitService.fiyatDurumuGetir()
            if (ayarCevap.basarili) { fiyatlarGizli = ayarCevap.fiyatlar_gizli_mi ?: false }

            val urunCevap = ApiClient.retrofitService.urunleriGetir()
            if (urunCevap.basarili && urunCevap.urunler != null) { urunListesi = urunCevap.urunler }
        } catch (e: Exception) { } finally { yukleniyor = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(end = 8.dp)) { Text("← Geri", color = Color.Gray, fontWeight = FontWeight.Bold) }
            Text("Katalog Yönetimi", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        // --- GLOBAL FİYAT ŞALTERİ ---
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = if (fiyatlarGizli) Color(0xFFFEE2E2) else Color(0xFFE0F2FE))) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Fiyatları Müşterilere Gizle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (fiyatlarGizli) "Şu an GİZLİ (Kapalı)" else "Şu an GÖRÜNÜR (Açık)", color = if (fiyatlarGizli) Color.Red else Color(0xFF0369A1), fontSize = 14.sp)
                }
                Switch(
                    checked = fiyatlarGizli,
                    onCheckedChange = { yeniDurum ->
                        fiyatlarGizli = yeniDurum
                        coroutineScope.launch {
                            try { ApiClient.retrofitService.fiyatGizlilikGuncelle(FiyatGuncelleIstegi(yeniDurum)) } catch (e: Exception) {}
                        }
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ürün Stok Şalterleri", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))

            // Bir sonraki adımda bu butona tıklayınca form açtıracağız
            Button(onClick = { urunEkleDialogAcik = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                Text("+ Yeni Ürün Ekle")
            }

            if (urunEkleDialogAcik) {
                YeniUrunEkleDialog(
                    kapat = { urunEkleDialogAcik = false },
                    urunEklendi = {
                        urunEkleDialogAcik = false
                        // Ürün eklendiğinde ekrandan çıkıp tekrar girerseniz yeni ürün listede görünecektir.
                    }
                )
            }
        }

        // --- ÜRÜN STOK ŞALTERLERİ LİSTESİ ---
        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(urunListesi) { urun ->
                    var stoktaVarMi by remember { mutableStateOf(urun.stokta_var_mi ?: true) }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = urun.urun_adi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "${urun.taban_fiyati} ₺ / ${urun.satis_birimi}", color = Color.Gray, fontSize = 14.sp)
                        }
                        Switch(
                            checked = stoktaVarMi,
                            onCheckedChange = { yeniStok ->
                                stoktaVarMi = yeniStok
                                coroutineScope.launch {
                                    try { ApiClient.retrofitService.urunStokGuncelle(StokGuncelleIstegi(urun.id, yeniStok)) } catch (e: Exception) {}
                                }
                            }
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// --- YENİ EKLENEN: MÜŞTERİ YÖNETİM SAYFASI VE LİSTESİ ---
@Composable
fun MusteriYonetimEkrani(geriyeDon: () -> Unit) {
    var musteriListesi by remember { mutableStateOf<List<Musteri>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var listeyiYenile by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var seciliSekme by remember { mutableIntStateOf(0) }
    val sekmeler = listOf("İstekler", "Onaylılar", "Engellenenler")

    // YENİ: Reddetme penceresi için hafıza değişkenleri
    var reddedilecekMusteri by remember { mutableStateOf<Musteri?>(null) }
    var secilenRedSebebi by remember { mutableStateOf("") }
    var ozelRedSebebi by remember { mutableStateOf("") }

    LaunchedEffect(listeyiYenile) {
        yukleniyor = true
        try {
            val cevap = ApiClient.retrofitService.musterileriGetir()
            if (cevap.basarili && cevap.musteriler != null) {
                musteriListesi = cevap.musteriler
            }
        } catch (e: Exception) {
        } finally {
            yukleniyor = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // 1. BAŞLIK VE GERİ BUTONU
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(end = 8.dp)) {
                Text("← Geri", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            Text("Müşteri Yönetimi", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        // 2. SEKMELER
        TabRow(selectedTabIndex = seciliSekme, containerColor = Color.Transparent, modifier = Modifier.padding(bottom = 16.dp)) {
            sekmeler.forEachIndexed { index, baslik ->
                Tab(
                    selected = seciliSekme == index,
                    onClick = { seciliSekme = index },
                    text = { Text(text = baslik, fontWeight = if (seciliSekme == index) FontWeight.ExtraBold else FontWeight.Medium) }
                )
            }
        }

        // 3. FİLTRELEME
        val gosterilecekMusteriler = musteriListesi.filter { musteri ->
            when (seciliSekme) {
                0 -> musteri.onay_durumu == "bekliyor"
                1 -> musteri.onay_durumu == "onayli"
                2 -> musteri.onay_durumu == "engellendi"
                else -> true
            }
        }

        // 4. LİSTELEME
        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (gosterilecekMusteriler.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bu kategoride müşteri bulunmuyor.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(gosterilecekMusteriler) { musteri ->
                    MusteriKarti(musteri) { yeniDurum ->
                        if (yeniDurum == "engellendi") {
                            // HATA BURADAYDI: Sadece pencereyi aç komutu veriyoruz, API isteği atmıyoruz!
                            reddedilecekMusteri = musteri
                        } else {
                            // Onayla veya Beklet tıklandıysa direkt sunucuya git
                            coroutineScope.launch {
                                try {
                                    val cevap = ApiClient.retrofitService.musteriOnayGuncelle(MusteriOnayIstegi(musteri.id, yeniDurum))
                                    if (cevap.basarili) { listeyiYenile++ }
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. YENİ: REDDETME SEBEBİ PENCERESİ (En altta, güvenli alanda)
    if (reddedilecekMusteri != null) {
        AlertDialog(
            onDismissRequest = {
                reddedilecekMusteri = null
                secilenRedSebebi = ""
                ozelRedSebebi = ""
            },
            title = { Text("Reddetme Sebebi", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("${reddedilecekMusteri?.dukkan_adi} isimli işletmeyi neden reddediyorsunuz?", modifier = Modifier.padding(bottom = 12.dp))

                    val sebepler = listOf("Geçersiz İsim", "Geçersiz Dükkan İsmi", "Hizmet Dışı Alan", "Diğer")

                    sebepler.forEach { sebep ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { secilenRedSebebi = sebep }) {
                            RadioButton(selected = (secilenRedSebebi == sebep), onClick = { secilenRedSebebi = sebep })
                            Text(text = sebep, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    if (secilenRedSebebi == "Diğer") {
                        OutlinedTextField(
                            value = ozelRedSebebi,
                            onValueChange = { ozelRedSebebi = it },
                            label = { Text("Lütfen sebebi yazın") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = secilenRedSebebi.isNotEmpty() && (secilenRedSebebi != "Diğer" || ozelRedSebebi.isNotBlank()),
                    onClick = {
                        val finalSebep = if (secilenRedSebebi == "Diğer") ozelRedSebebi else secilenRedSebebi
                        coroutineScope.launch {
                            try {
                                val istek = MusteriOnayIstegi(
                                    musteri_id = reddedilecekMusteri!!.id,
                                    yeni_durum = "engellendi",
                                    sebep = finalSebep
                                )
                                val cevap = ApiClient.retrofitService.musteriOnayGuncelle(istek)
                                if (cevap.basarili) {
                                    listeyiYenile++
                                    reddedilecekMusteri = null
                                    secilenRedSebebi = ""
                                    ozelRedSebebi = ""
                                }
                            } catch (e: Exception) {}
                        }
                    }
                ) { Text("Onayla ve Reddet") }
            },
            dismissButton = {
                TextButton(onClick = {
                    reddedilecekMusteri = null
                    secilenRedSebebi = ""
                    ozelRedSebebi = ""
                }) { Text("İptal") }
            }
        )
    }
}

@Composable
fun MusteriKarti(musteri: Musteri, durumGuncelle: (String) -> Unit) {
    // Veritabanından eski kayıtlardan null (boş) değer gelirse çökmeyi engeller
    val durum = musteri.onay_durumu ?: "bekliyor"
    val dukkanAdi = musteri.dukkan_adi ?: "Bilinmeyen İşletme"
    val yetkiliAdi = musteri.yetkili_adi ?: "İsimsiz Yetkili"
    val telefon = musteri.telefon ?: "Telefon Yok"

    val durumRengi = when (durum) {
        "onayli" -> Color(0xFF10B981) // Yeşil
        "bekliyor" -> Color(0xFFF59E0B) // Turuncu
        "engellendi" -> Color.Red // Kırmızı
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = dukkanAdi, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(text = yetkiliAdi, color = Color.DarkGray, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(text = telefon, color = Color.DarkGray, fontSize = 14.sp)
                }
                Card(colors = CardDefaults.cardColors(containerColor = durumRengi.copy(alpha = 0.1f))) {
                    Text(text = durum.uppercase(), color = durumRengi, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp).background(Color.LightGray.copy(alpha = 0.5f)))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { durumGuncelle("onayli") }, enabled = durum != "onayli") {
                    Text("Onayla", color = if (durum != "onayli") Color(0xFF10B981) else Color.LightGray)
                }
                TextButton(onClick = { durumGuncelle("bekliyor") }, enabled = durum != "bekliyor") {
                    Text("Beklet", color = if (durum != "bekliyor") Color(0xFFF59E0B) else Color.LightGray)
                }
                TextButton(onClick = { durumGuncelle("engellendi") }, enabled = durum != "engellendi") {
                    Text("Engelle", color = if (durum != "engellendi") Color.Red else Color.LightGray)
                }
            }
        }
    }
}

// "Kayıt Olmadan İncele" butonunun kaldırıldığı, daha sade Giriş Ekranı
@Composable
fun KarsilamaEkrani(girisYapTikladi: () -> Unit, kayitOlTikladi: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SUHA AMBALAJ ", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 32.dp))
        Text("Sipariş vermek ve geçmiş siparişlerinizi takip etmek için giriş yapmalısınız.", textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

        Button(onClick = { girisYapTikladi() }, modifier = Modifier.fillMaxWidth().height(55.dp)) {
            Text("Giriş Yap", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { kayitOlTikladi() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Yeni Kayıt Oluştur", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GirisYapEkrani(geriyeDon: () -> Unit, basariliGiris: () -> Unit) {
    var telefon by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var yukleniyor by remember { mutableStateOf(false) }
    var sonucMesaji by remember { mutableStateOf("") }

    // YENİ: Hafıza yöneticisini ekrana çağırıyoruz
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Giriş Yap", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = telefon,
            onValueChange = { telefon = it.removePrefix("0") },
            label = { Text("Telefon Numarası") },
            leadingIcon = { Text("+90", modifier = Modifier.padding(start = 16.dp, end = 4.dp), fontWeight = FontWeight.Bold, color = Color.DarkGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        OutlinedTextField(
            value = sifre, onValueChange = { sifre = it },
            label = { Text("Şifre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        TextButton(onClick = { /* Şifremi unuttum */ }, modifier = Modifier.align(Alignment.End)) {
            Text("Şifremi Unuttum", color = MaterialTheme.colorScheme.secondary)
        }

        if (sonucMesaji.isNotEmpty()) {
            Text(text = sonucMesaji, color = if (sonucMesaji.contains("Başarılı")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Button(
            onClick = {
                yukleniyor = true
                sonucMesaji = ""
                coroutineScope.launch {
                    try {
                        val istek = GirisIstegi("+90$telefon", sifre)
                        val cevap = ApiClient.retrofitService.girisYap(istek)

                        if (cevap.basarili) {
                            // YENİ: Node.js'ten gelen bilgileri telefona kalıcı olarak kazıyoruz!
                            sessionManager.oturumuKaydet(
                                musteriId = cevap.musteri_id ?: -1,
                                yetkiliAdi = cevap.yetkili_adi ?: "",
                                onayDurumu = cevap.onay_durumu ?: "bekliyor"
                            )

                            sonucMesaji = "Giriş Başarılı! Yönlendiriliyorsunuz..."
                            delay(1500)
                            basariliGiris()
                        } else {
                            sonucMesaji = cevap.mesaj
                        }
                    } catch (e: Exception) {
                        sonucMesaji = "Sunucuya bağlanılamadı!"
                    } finally {
                        yukleniyor = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp),
            enabled = !yukleniyor && telefon.isNotEmpty() && sifre.isNotEmpty()
        ) {
            Text(if (yukleniyor) "Giriş Yapılıyor..." else "Giriş Yap", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Geri Dön", color = Color.Gray)
        }
    }
}

@Composable
fun KayitOlEkrani(geriyeDon: () -> Unit, basariliKayit: () -> Unit) {
    var isyeriAdi by remember { mutableStateOf("") }
    var yetkiliAdi by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var yukleniyor by remember { mutableStateOf(false) }
    var sonucMesaji by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Yeni Kayıt Oluştur", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(value = isyeriAdi, onValueChange = { isyeriAdi = it }, label = { Text("İşyeri Adı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true)
        OutlinedTextField(value = yetkiliAdi, onValueChange = { yetkiliAdi = it }, label = { Text("Yetkili Adı Soyadı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true)

        // YENİ: Görsel +90 eklendi ve baştaki 0 engellendi
        OutlinedTextField(
            value = telefon,
            onValueChange = { telefon = it.removePrefix("0") },
            label = { Text("Telefon Numarası") },
            leadingIcon = { Text("+90", modifier = Modifier.padding(start = 16.dp, end = 4.dp), fontWeight = FontWeight.Bold, color = Color.DarkGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = sifre, onValueChange = { sifre = it },
            label = { Text("Şifre Belirleyin (En az 6 hane)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            isError = sifre.isNotEmpty() && sifre.length < 6
        )

        if (sifre.isNotEmpty() && sifre.length < 6) {
            Text("Şifreniz en az 6 karakter olmalıdır.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (sonucMesaji.isNotEmpty()) {
            Text(text = sonucMesaji, color = if (sonucMesaji.contains("alındı")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Button(
            onClick = {
                yukleniyor = true
                sonucMesaji = ""
                coroutineScope.launch {
                    try {
                        // Artık ekranda +90 olduğu için sadece birleştirip gönderiyoruz
                        val istek = KayitIstegi(isyeriAdi, yetkiliAdi, "+90$telefon", sifre, 0.0, 0.0)
                        val cevap = ApiClient.retrofitService.musteriKaydet(istek)

                        if (cevap.basarili) {
                            sonucMesaji = "Kayıt talebiniz alındı! Lütfen giriş yapınız."
                            delay(2000)
                            geriyeDon()
                        } else {
                            sonucMesaji = cevap.mesaj
                        }
                    } catch (e: Exception) {
                        sonucMesaji = "Sunucuya bağlanılamadı!"
                    } finally {
                        yukleniyor = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = !yukleniyor && isyeriAdi.isNotEmpty() && yetkiliAdi.isNotEmpty() && telefon.isNotEmpty() && sifre.length >= 6
        ) {
            Text(if (yukleniyor) "Gönderiliyor..." else "Kayıt Talebi Gönder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Geri Dön", color = Color.Gray)
        }
    }
}

// Eski ProfilVeSiparisEkrani içeriğimizi bu isme taşıdık
@Composable
fun GecmisSiparislerEkrani(cikisYap: () -> Unit, detaylaraGit: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Hafızadan müşterinin gerçek bilgilerini çekiyoruz
    val yetkiliAdi = sessionManager.yetkiliAdiGetir() ?: "Bilinmiyor"

    var siparisListesi by remember { mutableStateOf<List<SiparisOzet>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    // Ekran her açıldığında sayfayı zorla tazeleyen hiperaktif yapı
    LaunchedEffect(key1 = System.currentTimeMillis()) {
        yukleniyor = true
        try {
            val musteriId = sessionManager.musteriIdGetir()
            val cevap = ApiClient.retrofitService.siparisGecmisiniGetir(musteriId)

            if (cevap.basarili) {
                siparisListesi = cevap.siparisler ?: emptyList()
                hataMesaji = ""
            } else {
                hataMesaji = "Sipariş geçmişi yüklenemedi."
            }
        } catch (e: Exception) {
            hataMesaji = "Sunucu bağlantı hatası."
        } finally {
            yukleniyor = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // YENİ VE MODERN PROFİL KARTI (Müşteri No Silindi!)
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("İşletme Profiliniz", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { cikisYap() }) {
                        Text("Çıkış Yap", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))

                Text(text = "Suha Ambalaj ", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(text = "Yetkili: $yetkiliAdi", color = Color.DarkGray, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Text("Geçmiş Siparişlerim", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))

        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty() && siparisListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(hataMesaji, color = Color.Red, fontWeight = FontWeight.Bold) }
        } else if (siparisListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Henüz verilmiş bir siparişiniz bulunmamaktadır.", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(siparisListesi) { siparis ->
                    // YENİ: Siparişe tıklandığında ID'sini alıp detay sayfasına yönlendiriyoruz
                    SiparisKarti(siparis = siparis, detaylaraGit = { detaylaraGit(siparis.id) })
                }
            }
        }
    }
}

@Composable
fun SiparisKarti(siparis: SiparisOzet, detaylaraGit: () -> Unit) {
    val guvenliDurum = siparis.siparis_durumu ?: "bekliyor"
    val durumRengi = when (guvenliDurum.lowercase()) {
        "onaylandi" -> Color(0xFF10B981)
        "iptal edildi" -> Color.Red
        else -> Color(0xFFF59E0B)
    }

    // YENİ: Modifier.clickable ile karta tıklanabilme özelliği kazandırdık
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { detaylaraGit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Sipariş #${siparis.id}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(text = siparis.siparis_tarihi?.take(10) ?: "Tarih Yok", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${siparis.toplam_tutar} ₺", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                // YENİ: Sadece burada sağ altta bir kez durum yazıyor
                Text(
                    text = guvenliDurum.uppercase(),
                    color = durumRengi,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }


// Ekrana basarken de güvenliDurum'u büyük harfe çevirerek basıyoruz
    Text(
        text = guvenliDurum.uppercase(),
        color = durumRengi,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Sipariş #${siparis.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                // Tarihi "2026-10-10" gibi sadece gün kısmını alarak daha temiz gösteriyoruz
                Text(text = siparis.siparis_tarihi?.take(10) ?: "Tarih Yok", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${siparis.toplam_tutar} ₺", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = (siparis.siparis_durumu ?: "bekliyor").uppercase(),                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = durumRengi,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
// --------------------------------------------------------

@Composable
fun AnaSayfaVitrini(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean) {
    var aramaMetni by remember { mutableStateOf("") }
    val kullaniciRol =
        SessionManager(androidx.compose.ui.platform.LocalContext.current).onayDurumuGetir()
    var seciliUrun by remember { mutableStateOf<Urun?>(null) }
    var miktarSecimDialogAcik by remember { mutableStateOf(false) }
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.urunleriGetir()
            if (cevap.basarili) {
                urunListesi = cevap.urunler ?: emptyList()
            } else {
                hataMesaji = "Sunucudan ürünler alınamadı."
            }
        } catch (e: Exception) {
            hataMesaji = "Node.js sunucusuna bağlanılamadı!"
        } finally {
            yukleniyor = false
        }
    }

    val filtrelenmisUrunler = urunListesi.filter {
        it.urun_adi.contains(aramaMetni, ignoreCase = true) || it.kategori_adi  .contains(
            aramaMetni,
            ignoreCase = true
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "SUHA AMBALAJ",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = aramaMetni, onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün, marka veya kategori ara...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        Text(
            if (aramaMetni.isEmpty()) "Canlı Ürün Vitrini" else "Arama Sonuçları",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (yukleniyor) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    hataMesaji,
                    color = Color.Red
                )
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                gridItems(filtrelenmisUrunler) { urun ->
                    UrunKarti(
                        urun = urun,
                        fiyatlarGizliMi = fiyatlarGizliMi,
                        kullaniciRol = kullaniciRol, // YENİ: Rolü karta gönderdik
                        sepeteEkle = {
                            seciliUrun = urun
                            miktarSecimDialogAcik = true
                        }
                    )
                }
            }
        }
    }

    // YENİ: Etrafına koruyucu kilit eklendi! Sadece butona basıldığında ve ürün seçildiğinde çalışacak.
    if (miktarSecimDialogAcik && seciliUrun != null) {
        MiktarSecimDialog(
            urun = seciliUrun!!,
            kapat = { miktarSecimDialogAcik = false },
            sepeteOnayla = { miktar, secilenBirim ->
                // YENİ MANTIK: Sepette bu ürün zaten var mı diye kontrol ediyoruz
                val sepettekiUrun =
                    sepetListesi.find { it.urun.id == seciliUrun!!.id && it.secilenBirim == secilenBirim }

                if (sepettekiUrun != null) {
                    // Ürün zaten varsa, sadece miktarını arttır
                    val index = sepetListesi.indexOf(sepettekiUrun)
                    sepetListesi[index] = sepettekiUrun.copy(miktar = sepettekiUrun.miktar + miktar)
                } else {
                    // Ürün yoksa, sepete yeni bir satır olarak ekle
                    sepetListesi.add(SepetElemani(seciliUrun!!, miktar, secilenBirim))
                }

                miktarSecimDialogAcik = false
            }
        )
    }
}
@Composable
fun UrunKarti(urun: Urun, fiyatlarGizliMi: Boolean, kullaniciRol: String, sepeteEkle: () -> Unit) {
    Card(
        // 1. DEĞİŞİKLİK: Karta sabit bir yükseklik (Örn: 320.dp) veriyoruz ki hepsi aynı boyda olsun
        modifier = Modifier.fillMaxWidth().height(320.dp).padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // 2. DEĞİŞİKLİK: Column'a "fillMaxSize()" ekliyoruz ki içerideki boşluk (Spacer) çalışabilsin
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Görsel kısmı (Varsa)
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                Text("Görsel", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. DEĞİŞİKLİK: Uzun ürün isimlerinin kartı büyütmemesi için 2 satırda kesilmesini sağlıyoruz
            Text(
                text = urun.urun_adi,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = urun.birim_detayi ?: "",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 4. DEĞİŞİKLİK: Bu "weight(1f)" boşluğu, kendinden sonra gelen Fiyat ve Butonu kartın en dibine iter!
            Spacer(modifier = Modifier.weight(1f))

            // Fiyat Şalteri
            if (!fiyatlarGizliMi) {
                Text(text = "${urun.taban_fiyati} ₺ / ${urun.satis_birimi}", color = Color(0xFF0369A1), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            } else {
                Text(text = "Fiyat Gizli", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Yetki Kontrolü - Sadece admin olmayanlar bu butonu görecek
            if (kullaniciRol != "admin") {
                Button(
                    onClick = { sepeteEkle() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Sepete Ekle", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MiktarSecimDialog(urun: Urun, kapat: () -> Unit, sepeteOnayla: (Double, String) -> Unit) {
    var secilenBirim by remember { mutableStateOf(if (urun.urun_tipi == "Koli") "Koli" else urun.satis_birimi) }
    var miktarGirdisi by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = { kapat() },
        title = { Text(text = "Sepete Ekle: ${urun.urun_adi}", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(urun.birim_detayi, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

                if (urun.urun_tipi == "Koli" && urun.koli_ici_paket > 0) {
                    Text("Alım Şekli:", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = secilenBirim == "Koli", onClick = { secilenBirim = "Koli" })
                        Text("Koli", modifier = Modifier.padding(end = 16.dp))
                        RadioButton(selected = secilenBirim == "Paket", onClick = { secilenBirim = "Paket" })
                        Text("Paket")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = miktarGirdisi,
                    onValueChange = { miktarGirdisi = it },
                    label = { Text("Miktar ($secilenBirim)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val girilenMiktar = miktarGirdisi.toDoubleOrNull() ?: 1.0
                sepeteOnayla(girilenMiktar, secilenBirim)
            }) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = { kapat() }) { Text("İptal") } }
    )
}

@Composable
fun SepetEkrani(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean, profilDuzenlemeyeGit: () -> Unit = {}) {    val coroutineScope = rememberCoroutineScope()
    var siparisGonderiliyor by remember { mutableStateOf(false) }
    var siparisSonucu by remember { mutableStateOf("") }
    var uyariPenceresiAcik by remember { mutableStateOf(false) }
    var uyariBasligi by remember { mutableStateOf("") }
    var uyariMesaji by remember { mutableStateOf("") }
    var musteriDurumu by remember { mutableStateOf("onayli") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sepetim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        if (sepetListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sepetiniz boş.", color = Color.Gray) }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                sepetListesi.toList().forEach { eleman ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = eleman.urun.urun_adi, fontWeight = FontWeight.Bold)
                            if (!fiyatlarGizliMi) {
                                Text(text = "Birim Fiyatı: ${eleman.urun.taban_fiyati} ₺", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val guncelIndex = sepetListesi.indexOf(eleman)
                                if (guncelIndex != -1) {
                                    if (eleman.miktar > 1) {
                                        // HATA DÜZELTİLDİ: - işareti eklendi
                                        sepetListesi[guncelIndex] = eleman.copy(miktar = eleman.miktar - 1)
                                    } else {
                                        sepetListesi.removeAt(guncelIndex)
                                    }
                                }
                            }) {
                                Text(text = "-", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Red)
                            }

                            val formatliMiktar = if (eleman.miktar % 1.0 == 0.0) eleman.miktar.toInt().toString() else eleman.miktar.toString()
                            Text(
                                text = "$formatliMiktar ${eleman.secilenBirim}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(onClick = {
                                val guncelIndex = sepetListesi.indexOf(eleman)
                                if (guncelIndex != -1) {
                                    sepetListesi[guncelIndex] = eleman.copy(miktar = eleman.miktar + 1)
                                }
                            }) {
                                Text(text = "+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            if (siparisSonucu.isNotEmpty()) {
                Text(text = siparisSonucu, color = if (siparisSonucu.contains("Başarılı") || siparisSonucu.contains("İletildi")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    if (!sessionManager.girisYapildiMi()) {
                        siparisSonucu = "Sipariş vermek için Profil sekmesinden giriş yapmalısınız."
                        return@Button
                    }

                    siparisGonderiliyor = true
                    siparisSonucu = ""

                    coroutineScope.launch {
                        try {
                            val gercekMusteriId = sessionManager.musteriIdGetir()

                            // YENİ: KASADAN GEÇMEDEN ÖNCE YÖNETİCİNİN KARARINI SORGULA
                            val durumCevap = ApiClient.retrofitService.musteriDurumuGetir(gercekMusteriId)

                            if (durumCevap.basarili) {
                                musteriDurumu = durumCevap.durum ?: "bekliyor"

                                when (musteriDurumu) {
                                    "onayli" -> {
                                        var toplamTutar = 0.0
                                        val detayListesi = sepetListesi.map { eleman ->
                                            val fiyat = eleman.urun.taban_fiyati.toDoubleOrNull() ?: 0.0
                                            val araToplam = fiyat * eleman.miktar
                                            toplamTutar += araToplam
                                            SiparisDetay(eleman.urun.id, eleman.miktar, fiyat, araToplam, eleman.secilenBirim ?: "")
                                        }

                                        val cevap = ApiClient.retrofitService.siparisGonder(SiparisIstegi(gercekMusteriId, toplamTutar, detayListesi))

                                        if (cevap.basarili) {
                                            siparisSonucu = "Sipariş Başarıyla İletildi! ✓"
                                            delay(2000)
                                            sepetListesi.clear()
                                            siparisSonucu = ""
                                        } else {
                                            siparisSonucu = "Hata: ${cevap.mesaj}"
                                        }
                                    }
                                    "bekliyor" -> {
                                        uyariBasligi = "Hesabınız Onay Aşamasında"
                                        uyariMesaji = "Sipariş verebilmek için yönetici onayını beklemelisiniz. Uygulamayı incelemeye devam edebilirsiniz."
                                        uyariPenceresiAcik = true
                                    }
                                    "engellendi" -> {
                                        uyariBasligi = "Sipariş Verilemiyor"
                                        val sebep = durumCevap.sebep ?: "Bilinmeyen bir sebep"
                                        uyariMesaji = "Hesabınız şu sebeple askıya alınmıştır:\n$sebep\n\nLütfen profil bilgilerinizi güncelleyerek tekrar onaya gönderin."
                                        uyariPenceresiAcik = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            siparisSonucu = "Sunucuya bağlanılamadı!"
                        } finally {
                            siparisGonderiliyor = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !siparisGonderiliyor
            ) { Text(if (siparisGonderiliyor) "Sipariş Mühürleniyor..." else "Siparişi İlet", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }

    // YENİ: DURUM UYARI PENCERESİ
    if (uyariPenceresiAcik) {
        AlertDialog(
            onDismissRequest = { uyariPenceresiAcik = false },
            title = { Text(uyariBasligi, color = Color.Red, fontWeight = FontWeight.ExtraBold) },
            text = { Text(uyariMesaji, fontSize = 16.sp) },
            confirmButton = {
                if (musteriDurumu == "engellendi") {
                    Button(onClick = {
                        uyariPenceresiAcik = false
                        profilDuzenlemeyeGit() // Butona basılınca 5. sekmeye uçur!
                    }) { Text("Bilgilerimi Düzenle") }
                }
            },
            dismissButton = {
                TextButton(onClick = { uyariPenceresiAcik = false }) { Text("Anladım") }
            }
        )
    }
}


@Composable
fun YeniUrunEkleDialog(kapat: () -> Unit, urunEklendi: () -> Unit) {
    var urunAdi by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }

    // Birim Dropdown (Açılır Menü) State'leri
    var dropdownAcik by remember { mutableStateOf(false) }
    val varsayilanBirimler = listOf("Koli", "Paket", "Metre", "Kg", "Adet", "Yeni Ekle...")
    var seciliBirim by remember { mutableStateOf(varsayilanBirimler[0]) }
    var ozelBirim by remember { mutableStateOf("") } // Yeni Ekle seçilirse burası dolacak
    var birimDetayi by remember { mutableStateOf("") }

    // Fotoğraf Carousel (Kaydırmalı) State'i
    var secilenFotografSayisi by remember { mutableIntStateOf(0) }

    var islemYapiliyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { kapat() },
        title = { Text("Yeni Ürün Ekle", fontWeight = FontWeight.Bold) },
        text = {
            // Ekran küçükse veya form uzunsa aşağı kaydırılabilmesi için verticalScroll eklendi
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                // --- 1. FOTOĞRAF SEÇME ALANI (KAYDIRMALI GALERİ) ---
                Text("Ürün Fotoğrafları", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    // Yeni Fotoğraf Ekleme Butonu
                    Card(modifier = Modifier.size(80.dp).padding(end = 8.dp).clickable {
                        // Şimdilik sadece arayüzü canlandırmak için sayacı artırıyoruz. İleride galeriye bağlayacağız.
                        if(secilenFotografSayisi < 5) secilenFotografSayisi++
                    },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E7EB))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("+ Foto Ekle", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Seçilen Fotoğrafları Yatayda Gösterme (Carousel Yapısı)
                    for (i in 1..secilenFotografSayisi) {
                        Card(modifier = Modifier.size(80.dp).padding(end = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Fotoğraf $i", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text(if (secilenFotografSayisi == 0) "Maksimum 5 fotoğraf ekleyebilirsiniz." else "$secilenFotografSayisi fotoğraf seçildi.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))


                // --- 2. ÜRÜN TEMEL BİLGİLERİ ---
                OutlinedTextField(value = urunAdi, onValueChange = { urunAdi = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
                OutlinedTextField(value = marka, onValueChange = { marka = it }, label = { Text("Marka (Opsiyonel)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
                OutlinedTextField(value = fiyat, onValueChange = { fiyat = it }, label = { Text("Taban Fiyatı (₺)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true)


                // --- 3. SATIŞ BİRİMİ SEÇİMİ (AÇILIR MENÜ) ---
                Text("Satış Birimi", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = seciliBirim,
                        onValueChange = {},
                        readOnly = true, // Klavyeyi açmaz
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Text("▼", modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.ExtraBold) }
                    )
                    // Görünmez bir katman koyarak tüm alanın tıklanabilir olmasını sağlıyoruz
                    Box(modifier = Modifier.matchParentSize().clickable { dropdownAcik = true })

                    DropdownMenu(expanded = dropdownAcik, onDismissRequest = { dropdownAcik = false }, modifier = Modifier.fillMaxWidth(0.7f)) {
                        varsayilanBirimler.forEach { birim ->
                            DropdownMenuItem(
                                text = { Text(birim) },
                                onClick = {
                                    seciliBirim = birim
                                    dropdownAcik = false
                                    // Eğer Yeni Ekle dışında bir şey seçilirse detayı otomatik formatla
                                    if(birim != "Yeni Ekle...") birimDetayi = "1 $birim = ... Adet/Paket"
                                }
                            )
                        }
                    }
                }

                // Eğer "Yeni Ekle..." Seçildiyse Ekstra Kutu Çıkar
                if (seciliBirim == "Yeni Ekle...") {
                    OutlinedTextField(value = ozelBirim, onValueChange = { ozelBirim = it }, label = { Text("Yeni Birim Adı (Örn: Çuval, Rulo)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
                }


                // --- 4. BİRİM DETAYI ---
                val gecerliBirim = if (seciliBirim == "Yeni Ekle...") ozelBirim else seciliBirim
                OutlinedTextField(value = birimDetayi, onValueChange = { birimDetayi = it }, label = { Text("İçerik Detayı (Örn: 1 $gecerliBirim = 50 Adet)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                if (hataMesaji.isNotEmpty()) {
                    Text(hataMesaji, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sonBirim = if (seciliBirim == "Yeni Ekle...") ozelBirim else seciliBirim

                    if (urunAdi.isNotEmpty() && fiyat.isNotEmpty() && sonBirim.isNotEmpty()) {
                        islemYapiliyor = true
                        coroutineScope.launch {
                            try {
                                val istek = UrunEkleIstegi(kategori_id = 1, urun_adi = urunAdi, marka = marka, satis_birimi = sonBirim, birim_detayi = birimDetayi, taban_fiyati = fiyat, fotograflar = emptyList(), stokta_var_mi = true)
                                val cevap = ApiClient.retrofitService.urunEkle(istek)
                                if (cevap.basarili) {
                                    urunEklendi()
                                    kapat()
                                } else {
                                    hataMesaji = cevap.mesaj
                                }
                            } catch (e: Exception) {
                                hataMesaji = "Sunucuya bağlanılamadı."
                            } finally {
                                islemYapiliyor = false
                            }
                        }
                    } else {
                        hataMesaji = "Lütfen ürün adı, fiyat ve satış birimini eksiksiz giriniz."
                    }
                },
                enabled = !islemYapiliyor
            ) { Text(if (islemYapiliyor) "Ekleniyor..." else "Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = { kapat() }, enabled = !islemYapiliyor) { Text("İptal", color = Color.Gray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilDuzenlemeEkrani(geriyeDon: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var dukkanAdi by remember { mutableStateOf("") }
    var yetkiliAdi by remember { mutableStateOf(sessionManager.yetkiliAdiGetir() ?: "") }
    var telefon by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var islemMesaji by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // BAŞLIK VE GERİ BUTONU
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = geriyeDon) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
            }
            Text("Bilgilerimi Düzenle", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        }

        // BİLGİ KARTI
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = "Bilgi", tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Bilgilerinizi güncellediğinizde hesabınız tekrar onaya gönderilecektir. Yöneticimiz inceledikten sonra sipariş vermeye devam edebilirsiniz.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // MODERN GİRİŞ ALANLARI
        OutlinedTextField(
            value = dukkanAdi,
            onValueChange = { dukkanAdi = it },
            label = { Text("Dükkan / İşletme Adı") },
            leadingIcon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Dükkan") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = yetkiliAdi,
            onValueChange = { yetkiliAdi = it },
            label = { Text("Yetkili Adı Soyadı") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Kişi") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = telefon,
            onValueChange = { yeniDeger ->
                val sadeceRakam = yeniDeger.filter { it.isDigit() }
                if (sadeceRakam.startsWith("0")) {
                    telefon = sadeceRakam.drop(1).take(10)
                } else {
                    telefon = sadeceRakam.take(10)
                }
            },
            label = { Text("Telefon Numarası") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Telefon") },
            prefix = { Text("+90 ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (islemMesaji.isNotEmpty()) {
            Text(
                text = islemMesaji,
                color = if (islemMesaji.contains("Başarılı")) Color(0xFF10B981) else Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
            )
        }

        // KAYDET BUTONU (Node.js Sunucusuna Bağlandı)
        Button(
            onClick = {
                yukleniyor = true
                islemMesaji = "Güncelleme isteği gönderiliyor..."

                coroutineScope.launch {
                    try {
                        val musteriId = sessionManager.musteriIdGetir()
                        // Başına '0' ekleyerek sunucuya 05xx formatında yolluyoruz
                        val temizTelefon = "0$telefon"

                        val istek = ProfilGuncelleIstegi(musteriId, dukkanAdi, yetkiliAdi, temizTelefon)
                        val cevap = ApiClient.retrofitService.profilGuncelle(istek)

                        if (cevap.basarili) {
                            islemMesaji = "Başarılı: Hesabınız tekrar onaya gönderildi."
                            // 2 saniye mesajı gösterip sepete geri yolla
                            kotlinx.coroutines.delay(2000)
                            geriyeDon()
                        } else {
                            islemMesaji = "Hata: ${cevap.mesaj}"
                        }
                    } catch (e: Exception) {
                        islemMesaji = "Sunucuya bağlanılamadı!"
                    } finally {
                        yukleniyor = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(12.dp),
            // YENİ: Numara 10 hane olana kadar buton kilitli kalacak
            enabled = !yukleniyor && dukkanAdi.isNotBlank() && yetkiliAdi.isNotBlank() && telefon.length == 10
        ) {
            if (yukleniyor) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Bilgilerimi Güncelle ve Onaya Gönder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KategorilerEkrani(altKategoriyeGit: (Int, String) -> Unit) {
    var kategoriler by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    var aramaMetni by remember { mutableStateOf("") }

    // YENİ: Aynı anda sadece bir kartın açık kalması için, açık olan kartın ID'sini tutuyoruz
    var genisletilenKategoriId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.anaKategorileriGetir()
            if (cevap.basarili && cevap.kategoriler != null) {
                kategoriler = cevap.kategoriler
            } else {
                hataMesaji = cevap.mesaj ?: "Kategoriler bulunamadı."
            }
        } catch (e: Exception) {
            hataMesaji = "Sunucuya bağlanılamadı."
        } finally {
            yukleniyor = false
        }
    }

    val filtrelenmisKategoriler = kategoriler.filter {
        it.kategori_adi.contains(aramaMetni, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Text(
                text = "Kategoriler",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = aramaMetni,
                onValueChange = { aramaMetni = it },
                placeholder = { Text("Kategori ara...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
                trailingIcon = {
                    if (aramaMetni.isNotEmpty()) {
                        IconButton(onClick = { aramaMetni = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
        }

        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(hataMesaji, color = Color.Red) }
        } else if (filtrelenmisKategoriler.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sonuç bulunamadı.", color = Color.Gray) }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtrelenmisKategoriler.size) { index ->
                    val kategori = filtrelenmisKategoriler[index]
                    KategoriAkordeonKarti(
                        kategori = kategori,
                        // YENİ: Eğer bu kartın ID'si hafızadaki ID'ye eşitse bunu açık (true) yap, değilse kapalı (false) yap
                        genisletildi = genisletilenKategoriId == kategori.id,
                        kartaTiklandi = {
                            // YENİ: Zaten açıksa kapat, kapalıysa bunu aç (diğerleri otomatik kapanır)
                            genisletilenKategoriId = if (genisletilenKategoriId == kategori.id) null else kategori.id
                        },
                        altKategoriyeGit = altKategoriyeGit
                    )
                }
            }
        }
    }
}

@Composable
fun KategoriAkordeonKarti(
    kategori: Kategori,
    genisletildi: Boolean, // Artık durum içeriden değil, ana sayfadan geliyor
    kartaTiklandi: () -> Unit, // Tıklanma komutu ana sayfaya gönderiliyor
    altKategoriyeGit: (Int, String) -> Unit
) {
    var altKategoriler by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var altYukleniyor by remember { mutableStateOf(false) }
    var verilerCekildiMi by remember { mutableStateOf(false) }

    LaunchedEffect(genisletildi) {
        if (genisletildi && !verilerCekildiMi) {
            altYukleniyor = true
            try {
                val cevap = ApiClient.retrofitService.altKategorileriGetir(kategori.id)
                if (cevap.basarili && cevap.altKategoriler != null) {
                    altKategoriler = cevap.altKategoriler
                    verilerCekildiMi = true
                }
            } catch (e: Exception) {
                println("Alt kategori çekme hatası: ${e.message}")
            } finally {
                altYukleniyor = false
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { kartaTiklandi() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kategori.kategori_adi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (genisletildi) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Genişlet",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = genisletildi) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                    if (altYukleniyor) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (altKategoriler.isEmpty()) {
                        Text(
                            text = "Bu kategoriye ait alt kategori bulunmamaktadır.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        altKategoriler.forEach { altKategori ->
                            TextButton(
                                onClick = { altKategoriyeGit(altKategori.id, altKategori.kategori_adi) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Text(
                                        text = altKategori.kategori_adi,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategoriUrunleriEkrani(
    kategoriId: Int,
    kategoriAdi: String,
    fiyatlarGizliMi: Boolean,
    kullaniciRol: String,
    geriyeDon: () -> Unit,
    sepeteEkle: (Urun) -> Unit
) {
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }

    var seciliMarka by remember { mutableStateOf<String?>("Tümü") }
    var seciliOlcu by remember { mutableStateOf<String?>("Tümü") }

    LaunchedEffect(kategoriId) {
        try {
            val cevap = ApiClient.retrofitService.kategoriUrunleriniGetir(kategoriId)
            if (cevap.basarili && cevap.urunler != null) {
                urunler = cevap.urunler
            }
        } catch (e: Exception) {
            println("Hata: ${e.message}")
        } finally {
            yukleniyor = false
        }
    }

    val markalar = listOf("Tümü") + urunler.mapNotNull { it.marka }.filter { it.isNotBlank() }.distinct()
    val olculer = listOf("Tümü") + urunler.mapNotNull { it.olcu }.filter { it.isNotBlank() }.distinct()

    val gosterilecekUrunler = urunler.filter { urun ->
        (seciliMarka == "Tümü" || urun.marka == seciliMarka) &&
                (seciliOlcu == "Tümü" || urun.olcu == seciliOlcu)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ÜST BAR
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = geriyeDon) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
            }
            Text(text = kategoriAdi, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        // MODERN FİLTRELEME ALANI
        if (markalar.size > 1 || olculer.size > 1) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(bottom = 12.dp)) {
                if (markalar.size > 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(markalar.size) { index ->
                            val marka = markalar[index]
                            val secili = seciliMarka == marka
                            ModernFiltreButonu(metin = marka, secili = secili) { seciliMarka = marka }
                        }
                    }
                }
                if (olculer.size > 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        // DÜZELTİLEN KISIM 1: Buraya UrunKarti değil FiltreButonu gelmeliydi
                        items(olculer.size) { index ->
                            val olcu = olculer[index]
                            val secili = seciliOlcu == olcu
                            ModernFiltreButonu(metin = olcu, secili = secili) { seciliOlcu = olcu }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
        }

        // ÜRÜNLER LİSTESİ (Ana Sayfadaki Gibi İkili Izgara Tasarımı)
        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (gosterilecekUrunler.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bu kriterlere uygun ürün bulunamadı.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2), // 2'li yan yana dizilim
                modifier = Modifier.fillMaxSize(),
                // Alt menünün arkasında kalmaması için bottom padding eklendi
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(count = gosterilecekUrunler.size) { index ->
                    val urun = gosterilecekUrunler[index]
                    UrunKarti(
                        urun = urun,
                        fiyatlarGizliMi = fiyatlarGizliMi,
                        kullaniciRol = kullaniciRol,
                        sepeteEkle = { sepeteEkle(urun) }
                    )
                }
            }
        }
    }
}

// MODERN FİLTRE BUTONU TASARIMI (Yardımcı Fonksiyon)
@Composable
fun ModernFiltreButonu(metin: String, secili: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50), // Tam yuvarlak köşeler
        color = if (secili) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, if (secili) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Text(
            text = metin,
            color = if (secili) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (secili) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
