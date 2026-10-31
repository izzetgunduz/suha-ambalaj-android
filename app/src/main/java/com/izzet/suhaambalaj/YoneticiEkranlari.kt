package com.izzet.suhaambalaj

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. ÇOKLU FOTOĞRAF SİSTEMİ
    var secilenGorseller by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var base64Gorseller by remember { mutableStateOf<List<String>>(emptyList()) }

    val cokluGaleriSecici = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uriler ->
        if (uriler.isNotEmpty()) {
            secilenGorseller = uriler
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val base64List = mutableListOf<String>()
                uriler.forEach { uri ->
                    try {
                        val stream = context.contentResolver.openInputStream(uri)
                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        val outputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
                        val byteArray = outputStream.toByteArray()
                        base64List.add("data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP))
                    } catch (e: Exception) { }
                }
                base64Gorseller = base64List
            }
        }
    }

    // 2. ÖZGÜR KATEGORİ SİSTEMİ
    var anaKategori by remember { mutableStateOf("") }
    var altKategori by remember { mutableStateOf("") }

    // 3. TEMEL BİLGİLER
    var urunAdi by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }

    // 4. AKILLI SATIŞ BİRİMİ SİSTEMİ
    var satisBirimi by remember { mutableStateOf("") }
    var kovaKg by remember { mutableStateOf("") }
    var koliPaket by remember { mutableStateOf("") }
    var paketAdet by remember { mutableStateOf("") }

    val birimKucuk = satisBirimi.lowercase()
    val isKova = birimKucuk.contains("kova")
    val isKoli = birimKucuk.contains("koli")

    // 5. MATRİS VARYANT SİSTEMİ (Önce Renk, Sonra O Renge Özel Bedenler)
    var renkListesi = remember { mutableStateListOf<String>() }
    var varyantlar = remember { mutableStateListOf<VaryantIstegi>() }

    var yeniRenkGirdisi by remember { mutableStateOf("") }
    // Her renk kartı için geçici beden ve fiyat girdilerini hafızada tutan yapı
    var bedenGirisleri = remember { mutableStateMapOf<String, String>() }
    var fiyatGirisleri = remember { mutableStateMapOf<String, String>() }

    var islemYapiliyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = geriyeDon) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "İptal", tint = MaterialTheme.colorScheme.primary) }
                Text("Ürün Ekle", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            }
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Button(
                    onClick = {
                        val tabanFiyatD = fiyat.toDoubleOrNull()
                        if (urunAdi.isNotBlank() && tabanFiyatD != null && anaKategori.isNotBlank()) {
                            islemYapiliyor = true

                            // Akıllı Birim Detayı Oluşturma
                            val dinamikDetay = when {
                                isKova -> "1 Kova = $kovaKg Kg"
                                isKoli -> "1 Koli = $koliPaket Paket (${(koliPaket.toIntOrNull()?:0) * (paketAdet.toIntOrNull()?:0)} Adet)"
                                else -> "Birim: $satisBirimi"
                            }

                            // Kategori hiyerarşisini ürün adına veya API'ye yedirme
                            // YENİ HALİ (Sadece saf ürün adını kaydeder)
                            val finalUrunAdi = urunAdi.trim()
                            coroutineScope.launch {
                                try {
                                    val istek = UrunEkleIstegi(
                                        kategori_id = 1, // Not: Backend API'si Int bekliyorsa 1 gönderiyoruz, gerçekte Ana Kategori metnini kullanıyoruz
                                        urun_adi = finalUrunAdi,
                                        marka = marka,
                                        satis_birimi = satisBirimi,
                                        birim_detayi = dinamikDetay,
                                        taban_fiyati = fiyat,
                                        fotograflar = base64Gorseller,
                                        stokta_var_mi = true,
                                        varyantlar = varyantlar.toList()
                                    )
                                    val cevap = ApiClient.retrofitService.urunEkle(istek)
                                    if (cevap.basarili) urunEklendi() else hataMesaji = cevap.mesaj
                                } catch (e: Exception) {
                                    hataMesaji = "Sunucuya bağlanılamadı. Fotoğraf boyutu büyük olabilir."
                                } finally { islemYapiliyor = false }
                            }
                        } else { hataMesaji = "Ana Kategori, Ürün Adı ve Fiyat zorunludur." }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(55.dp),
                    enabled = !islemYapiliyor,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text(if (islemYapiliyor) "İşleniyor..." else "Kataloğa Kaydet", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)) {

            if (hataMesaji.isNotEmpty()) Text(hataMesaji, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            // --- 1. ÇOKLU FOTOĞRAF GALERİSİ ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ürün Görselleri", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = { cokluGaleriSecici.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (secilenGorseller.isEmpty()) "+ Fotoğraf Seç" else "Değiştir/Ekle")
                }
            }
            if (secilenGorseller.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    items(secilenGorseller.size) { index ->
                        coil.compose.AsyncImage(
                            model = secilenGorseller[index],
                            contentDescription = "Ürün Fotoğrafı",
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }  else {
        // YENİ: Koyu Mavi Çerçeveli, Açık Mavi Arka Planlı Şık Kutu
        Surface(
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp)
                .clickable { cokluGaleriSecici.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0F8FF), // Ferah çok açık mavi
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E3A8A)) // Marka rengi koyu mavi çerçeve
        ) {
            // DOĞRUSU: mainAxisAlignment kelimesini silip yerine verticalArrangement yazıyoruz
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {                Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color(0xFF1E3A8A), modifier = Modifier.size(32.dp))
                Text("Müşteriler için görsel ekleyin (Max 5)", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

            // --- 2. KATEGORİ VE TEMEL BİLGİLER ---
            Text("Sınıflandırma", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = anaKategori, onValueChange = { anaKategori = it }, label = { Text("Ana Kategori (Örn: Ambalajlar)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = altKategori, onValueChange = { altKategori = it }, label = { Text("Alt Kategori (Örn: Hışır Poşet)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            OutlinedTextField(value = urunAdi, onValueChange = { urunAdi = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = marka, onValueChange = { marka = it }, label = { Text("Marka (Opsiyonel)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = fiyat, onValueChange = { fiyat = it }, label = { Text("Taban Fiyat (₺)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
            }

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            // --- 3. AKILLI SATIŞ BİRİMİ ---
            Text("Satış Birimi", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = satisBirimi,
                onValueChange = { satisBirimi = it },
                label = { Text("Birim (Örn: Koli, Kova, Paket, Kg, Metre)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true
            )

            // Yapay Zeka Form Görünümü
            androidx.compose.animation.AnimatedVisibility(visible = isKova) {
                OutlinedTextField(value = kovaKg, onValueChange = { kovaKg = it }, label = { Text("Bu Kova Kaç Kg?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            }
            androidx.compose.animation.AnimatedVisibility(visible = isKoli) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = koliPaket, onValueChange = { koliPaket = it }, label = { Text("İçindeki Paket Sayısı") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paketAdet, onValueChange = { paketAdet = it }, label = { Text("1 Paketteki Adet") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            // --- 4. MATRİS VARYANT SİSTEMİ ---
            Text("Ürün Çeşitleri (Varyantlar)", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))

            // Adım 1: Ana Renk / Çeşit Ekleme
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = yeniRenkGirdisi, onValueChange = { yeniRenkGirdisi = it }, label = { Text("Yeni Renk Ekle (Örn: Siyah)") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(
                    onClick = {
                        if (yeniRenkGirdisi.isNotBlank() && !renkListesi.contains(yeniRenkGirdisi)) {
                            renkListesi.add(yeniRenkGirdisi)
                            bedenGirisleri[yeniRenkGirdisi] = ""
                            fiyatGirisleri[yeniRenkGirdisi] = ""
                            yeniRenkGirdisi = ""
                        }
                    },
                    modifier = Modifier.height(55.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Renk Ekle") }
            }

            // Adım 2: Eklenen Renklerin Kartları ve Beden Yönetimi
            if (renkListesi.isEmpty()) {
                Text("Ürün standart olarak satılacak (Renk/Beden seçeneği yok).", color = Color.Gray, fontSize = 13.sp)
            } else {
                renkListesi.forEach { renk ->
                    // YENİ: Koyu Mavi Başlıklı Modern E-Ticaret Kartı
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column {
                            // KOYU MAVİ BAŞLIK ŞERİDİ
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E3A8A)).padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(renk.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                                Icon(Icons.Default.Delete, "Rengi Sil", tint = Color(0xFFFCA5A5), modifier = Modifier.clickable {
                                    renkListesi.remove(renk)
                                    varyantlar.removeAll { it.renk == renk }
                                })
                            }

                            // BEDENLERİN LİSTELENDİĞİ ALT KISIM
                            Column(modifier = Modifier.padding(16.dp)) {
                                val buRenginVaryantlari = varyantlar.filter { it.renk == renk }
                                buRenginVaryantlari.forEach { v ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Boyut: ${v.olcu}", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (v.ek_fiyat > 0) Text("+${v.ek_fiyat} ₺", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                                            Icon(Icons.Default.Clear, "Sil", tint = Color.Red, modifier = Modifier.size(20.dp).clickable { varyantlar.remove(v) })
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // YENİ BEDEN EKLEME KUTULARI
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = bedenGirisleri[renk] ?: "", onValueChange = { bedenGirisleri[renk] = it }, label = { Text("Boyut (Örn: Küçük)") }, modifier = Modifier.weight(1.5f), singleLine = true)
                                    OutlinedTextField(value = fiyatGirisleri[renk] ?: "", onValueChange = { fiyatGirisleri[renk] = it }, label = { Text("Ek ₺") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                                    IconButton(
                                        onClick = {
                                            val beden = bedenGirisleri[renk] ?: ""
                                            val ekFiyat = fiyatGirisleri[renk]?.toDoubleOrNull() ?: 0.0
                                            if (beden.isNotBlank()) {
                                                varyantlar.add(VaryantIstegi(renk, beden, ekFiyat))
                                                bedenGirisleri[renk] = ""; fiyatGirisleri[renk] = ""
                                            }
                                        },
                                        modifier = Modifier.background(Color(0xFF1E3A8A), RoundedCornerShape(8.dp)).size(48.dp)
                                    ) { Icon(Icons.Default.Add, "Ekle", tint = Color.White) }
                                }
                            }
                        }
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

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { detayaGit(siparis.siparis_id) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // YÖNETİCİ EKRANI: Yöneticiler veritabanı ID'sini görmelidir (Sipariş #12 vb.)
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