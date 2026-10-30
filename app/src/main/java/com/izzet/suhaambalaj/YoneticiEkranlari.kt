package com.izzet.suhaambalaj

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.collections.forEach

@Composable
fun UrunVeKatalogYonetimiEkrani(geriyeDon: () -> Unit) {
    // YENİ: Artık küçük pencere değil, tam ekran geçişi için bu değişkeni kullanıyoruz
    var yeniUrunSayfasiAcik by remember { mutableStateOf(false) }
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var fiyatlarGizli by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }

    // YENİ: Ürün eklendiğinde listeyi otomatik tazelemek için tetikleyici
    var listeyiYenile by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Sayfa açıldığında veya ürün eklendiğinde listeyi günceller
    LaunchedEffect(listeyiYenile) {
        yukleniyor = true
        try {
            val ayarCevap = ApiClient.retrofitService.fiyatDurumuGetir()
            if (ayarCevap.basarili) { fiyatlarGizli = ayarCevap.fiyatlar_gizli_mi ?: false }

            val urunCevap = ApiClient.retrofitService.urunleriGetir()
            if (urunCevap.basarili && urunCevap.urunler != null) { urunListesi = urunCevap.urunler }
        } catch (e: Exception) { } finally { yukleniyor = false }
    }

    // EĞER BUTONA BASILDIYSA YENİ ÜRÜN EKLEME SAYFASINI TAM EKRAN GÖSTER
    if (yeniUrunSayfasiAcik) {
        YeniUrunEkleEkrani(
            geriyeDon = { yeniUrunSayfasiAcik = false },
            urunEklendi = {
                yeniUrunSayfasiAcik = false
                listeyiYenile++ // Başarıyla eklenirse listeyi hemen tazele
            }
        )
    }
    // EĞER BASILMADIYSA NORMAL KATALOG YÖNETİMİ (VİTRİN) EKRANINI GÖSTER
    else {
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

                // GÜNCELLENDİ: Artık bu butona tıklayınca tam sayfa geçişini tetikliyoruz
                Button(onClick = { yeniUrunSayfasiAcik = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                    Text("+ Yeni Ürün Ekle")
                }
            }

            // --- ÜRÜN STOK ŞALTERLERİ LİSTESİ ---
            if (yukleniyor) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(urunListesi.size) { index ->
                        val urun = urunListesi[index]
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YeniUrunEkleEkrani(geriyeDon: () -> Unit, urunEklendi: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // Temel Ürün Bilgileri
    var urunAdi by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }
    var birimDetayi by remember { mutableStateOf("") }

    // Kategori Seçimi
    var kategoriler by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var seciliKategoriId by remember { mutableStateOf<Int?>(null) }
    var kategoriDropdownAcik by remember { mutableStateOf(false) }

    // Birim Seçimi
    val varsayilanBirimler = listOf("Koli", "Paket", "Metre", "Kg", "Adet")
    var seciliBirim by remember { mutableStateOf(varsayilanBirimler[0]) }
    var birimDropdownAcik by remember { mutableStateOf(false) }

    // Dinamik Varyant Listesi
    var varyantlar = remember { mutableStateListOf<VaryantIstegi>() }

    // Varyant Ekleme Kutu State'leri
    var yeniRenk by remember { mutableStateOf("") }
    var yeniOlcu by remember { mutableStateOf("") }
    var yeniEkFiyat by remember { mutableStateOf("") }
    var varyantEklemeAcik by remember { mutableStateOf(false) }

    var islemYapiliyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf("") }

    // Sayfa açıldığında Kategorileri Sunucudan Çek
    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.anaKategorileriGetir()
            if (cevap.basarili && cevap.kategoriler != null) {
                kategoriler = cevap.kategoriler
                seciliKategoriId = kategoriler.firstOrNull()?.id // İlk kategoriyi varsayılan yap
            }
        } catch (e: Exception) { }
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = geriyeDon) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "İptal", tint = MaterialTheme.colorScheme.primary) }
                Text("Yeni Ürün (B2B)", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            }
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Button(
                    onClick = {
                        val tabanFiyatD = fiyat.toDoubleOrNull()
                        if (urunAdi.isNotBlank() && tabanFiyatD != null && seciliKategoriId != null) {
                            islemYapiliyor = true
                            coroutineScope.launch {
                                try {
                                    val istek = UrunEkleIstegi(
                                        kategori_id = seciliKategoriId!!,
                                        urun_adi = urunAdi,
                                        marka = marka,
                                        satis_birimi = seciliBirim,
                                        birim_detayi = birimDetayi,
                                        taban_fiyati = fiyat,
                                        fotograflar = emptyList(),
                                        stokta_var_mi = true,
                                        varyantlar = varyantlar.toList()
                                    )
                                    val cevap = ApiClient.retrofitService.urunEkle(istek)
                                    if (cevap.basarili) {
                                        urunEklendi()
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
                            hataMesaji = "Ürün adı, geçerli bir fiyat ve kategori zorunludur."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(55.dp),
                    enabled = !islemYapiliyor,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text(if (islemYapiliyor) "Kaydediliyor..." else "Kataloğa Kaydet", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)) {

            if (hataMesaji.isNotEmpty()) { Text(hataMesaji, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp)) }

            // 1. KATEGORİ SEÇİMİ
            Text("Ana Kategori", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedTextField(
                    value = kategoriler.find { it.id == seciliKategoriId }?.kategori_adi ?: "Kategori Seçin",
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text("▼", modifier = Modifier.padding(end = 16.dp)) }
                )
                Box(modifier = Modifier.matchParentSize().clickable { kategoriDropdownAcik = true })
                DropdownMenu(expanded = kategoriDropdownAcik, onDismissRequest = { kategoriDropdownAcik = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    kategoriler.forEach { kat ->
                        DropdownMenuItem(text = { Text(kat.kategori_adi) }, onClick = { seciliKategoriId = kat.id; kategoriDropdownAcik = false })
                    }
                }
            }

            // 2. TEMEL BİLGİLER
            OutlinedTextField(value = urunAdi, onValueChange = { urunAdi = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = marka, onValueChange = { marka = it }, label = { Text("Marka") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = fiyat, onValueChange = { fiyat = it }, label = { Text("Taban Fiyat (₺)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
            }

            // 3. SATIŞ BİRİMİ VE HİYERARŞİ
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = seciliBirim, onValueChange = {}, label = { Text("Birim") }, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { Text("▼", modifier = Modifier.padding(end = 12.dp)) })
                    Box(modifier = Modifier.matchParentSize().clickable { birimDropdownAcik = true })
                    DropdownMenu(expanded = birimDropdownAcik, onDismissRequest = { birimDropdownAcik = false }) {
                        varsayilanBirimler.forEach { b -> DropdownMenuItem(text = { Text(b) }, onClick = { seciliBirim = b; birimDropdownAcik = false }) }
                    }
                }
                OutlinedTextField(value = birimDetayi, onValueChange = { birimDetayi = it }, label = { Text("Detay (Örn: 1 Koli = 50 Adet)") }, modifier = Modifier.weight(2f), singleLine = true)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 4. VARYANT (SEÇENEK) EKLEME ALANI
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ürün Varyantları (Renk/Ebat)", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { varyantEklemeAcik = !varyantEklemeAcik }) { Text("+ Ekle", fontWeight = FontWeight.Bold) }
            }

            if (varyantEklemeAcik) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = yeniRenk, onValueChange = { yeniRenk = it }, label = { Text("Renk (Opsiyonel)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = yeniOlcu, onValueChange = { yeniOlcu = it }, label = { Text("Ölçü (Opsiyonel)") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = yeniEkFiyat, onValueChange = { yeniEkFiyat = it }, label = { Text("Ek Fiyat (₺)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                            Button(
                                onClick = {
                                    if (yeniRenk.isNotBlank() || yeniOlcu.isNotBlank()) {
                                        varyantlar.add(VaryantIstegi(yeniRenk, yeniOlcu, yeniEkFiyat.toDoubleOrNull() ?: 0.0))
                                        yeniRenk = ""; yeniOlcu = ""; yeniEkFiyat = ""; varyantEklemeAcik = false
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) { Text("Listeye Ekle") }
                        }
                    }
                }
            }

            // Eklenen Varyantları Listeleme
            if (varyantlar.isEmpty()) {
                Text("Ürüne ait bir seçenek eklenmedi. Ürün sadece standart haliyle satılacak.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
                varyantlar.forEachIndexed { index, v ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${v.renk} ${v.olcu}".trim(), fontWeight = FontWeight.Bold)
                            if (v.ek_fiyat > 0) Text("Fiyat Farkı: +${v.ek_fiyat} ₺", color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                        IconButton(onClick = { varyantlar.removeAt(index) }) { Icon(Icons.Filled.Clear, "Sil", tint = Color.Red) }
                    }
                }
            }
        }
    }
}

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