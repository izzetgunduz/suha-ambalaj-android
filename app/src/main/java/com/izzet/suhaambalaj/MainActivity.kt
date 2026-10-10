package com.izzet.suhaambalaj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

// YENİ: Miktar artık Double (1.5 metre için) ve secilenBirim tutuluyor
data class SepetElemani(val urun: Urun, var miktar: Double, var secilenBirim: String)

@Composable
fun AnaUygulamaEkrani() {
    var seciliSekme by remember { mutableIntStateOf(0) }
    val fiyatlarGizliMi by remember { mutableStateOf(false) }
    val sepetListesi = remember { mutableStateListOf<SepetElemani>() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                NavigationBarItem(icon = { Icon(Icons.Filled.Home, "Ana Sayfa") }, label = { Text("Ana Sayfa") }, selected = seciliSekme == 0, onClick = { seciliSekme = 0 })
                NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Filled.List, "Kategoriler") }, label = { Text("Kategoriler") }, selected = seciliSekme == 1, onClick = { seciliSekme = 1 })
                NavigationBarItem(icon = { Icon(Icons.Filled.ShoppingCart, "Sepetim") }, label = { Text("Sepetim") }, selected = seciliSekme == 2, onClick = { seciliSekme = 2 })
                NavigationBarItem(icon = { Icon(Icons.Filled.Person, "Profil") }, label = { Text("Profil") }, selected = seciliSekme == 3, onClick = { seciliSekme = 3 })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (seciliSekme) {
                0 -> AnaSayfaVitrini(sepetListesi, fiyatlarGizliMi)
                1 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Kategoriler Sayfası") }
                2 -> SepetEkrani(sepetListesi, fiyatlarGizliMi)
                3 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profil & Onay Sayfası") }
            }
        }
    }
}

@Composable
fun AnaSayfaVitrini(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean) {
    var aramaMetni by remember { mutableStateOf("") }
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.urunleriGetir()
            if (cevap.basarili) { urunListesi = cevap.urunler } else { hataMesaji = "Sunucudan ürünler alınamadı." }
        } catch (e: Exception) {
            hataMesaji = "Node.js sunucusuna bağlanılamadı!"
        } finally {
            yukleniyor = false
        }
    }

    val filtrelenmisUrunler = urunListesi.filter {
        it.urun_adi.contains(aramaMetni, ignoreCase = true) || it.kategori_adi.contains(aramaMetni, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("SUHA AMBALAJ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        OutlinedTextField(
            value = aramaMetni, onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün, marka veya kategori ara...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        Text(if (aramaMetni.isEmpty()) "Canlı Ürün Vitrini" else "Arama Sonuçları", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(hataMesaji, color = Color.Red) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(filtrelenmisUrunler) { urun ->
                    UrunKarti(urun = urun, fiyatlarGizliMi = fiyatlarGizliMi, sepetListesi = sepetListesi)
                }
            }
        }
    }
}

@Composable
fun UrunKarti(urun: Urun, fiyatlarGizliMi: Boolean, sepetListesi: MutableList<SepetElemani>) {
    var dialogAcikMi by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                Text("Görsel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = urun.urun_adi, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)

            // YENİ: Müşterinin aklı karışmasın diye ürün detayını (Örn: 1 Koli = 20 Paket) doğrudan karta ekledik
            Text(text = urun.birim_detayi, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

            if (!fiyatlarGizliMi) {
                Text("${urun.taban_fiyati} ₺ / ${urun.satis_birimi}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Button(
                onClick = { dialogAcikMi = true },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Sepete Ekle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // YENİ: Akıllı Sepete Ekleme Penceresi
    if (dialogAcikMi) {
        MiktarSecimDialog(
            urun = urun,
            kapat = { dialogAcikMi = false },
            sepeteOnayla = { miktar, secilenBirim ->
                val mevcutEleman = sepetListesi.find { it.urun.id == urun.id && it.secilenBirim == secilenBirim }
                if (mevcutEleman != null) {
                    mevcutEleman.miktar += miktar
                } else {
                    sepetListesi.add(SepetElemani(urun, miktar, secilenBirim))
                }
                dialogAcikMi = false
            }
        )
    }
}

@Composable
fun MiktarSecimDialog(urun: Urun, kapat: () -> Unit, sepeteOnayla: (Double, String) -> Unit) {
    // Ürün tipine göre varsayılan birimi belirliyoruz
    var secilenBirim by remember { mutableStateOf(if (urun.urun_tipi == "Koli") "Koli" else urun.satis_birimi) }
    var miktarGirdisi by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = { kapat() },
        title = { Text(text = "Sepete Ekle: ${urun.urun_adi}", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(urun.birim_detayi, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

                // Eğer ürün KOLİ ise ve içinde paket varsa müşteriye seçenek sunuyoruz
                if (urun.urun_tipi == "Koli" && urun.koli_ici_paket > 0) {
                    Text("Alım Şekli:", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = secilenBirim == "Koli", onClick = { secilenBirim = "Koli" })
                        Text("Koli Olarak", modifier = Modifier.padding(end = 16.dp))

                        RadioButton(selected = secilenBirim == "Paket", onClick = { secilenBirim = "Paket" })
                        Text("Paket Olarak")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Miktar girişi (Metre ürünler için küsuratlı girişe izin verilir)
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
            }) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = { kapat() }) { Text("İptal") }
        }
    )
}

@Composable
fun SepetEkrani(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    var siparisGonderiliyor by remember { mutableStateOf(false) }
    var siparisSonucu by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sepetim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        if (sepetListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sepetiniz boş.", color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                sepetListesi.forEach { eleman ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = eleman.urun.urun_adi, fontWeight = FontWeight.Bold)
                            if (!fiyatlarGizliMi) {
                                Text(text = "Birim Fiyatı: ${eleman.urun.taban_fiyati} ₺", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            }
                        }
                        val formatliMiktar = if (eleman.miktar % 1.0 == 0.0) eleman.miktar.toInt().toString() else eleman.miktar.toString()
                        Text(text = "$formatliMiktar ${eleman.secilenBirim}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Sipariş Sonuç Mesajı
            if (siparisSonucu.isNotEmpty()) {
                Text(
                    text = siparisSonucu,
                    color = if (siparisSonucu.contains("Başarı")) Color(0xFF10B981) else Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    siparisGonderiliyor = true
                    siparisSonucu = ""

                    coroutineScope.launch {
                        try {
                            // 1. Sepetteki ürünlerin toplam tutarını hesapla
                            var toplamTutar = 0.0
                            val detayListesi = sepetListesi.map { eleman ->
                                val fiyat = eleman.urun.taban_fiyati.toDoubleOrNull() ?: 0.0
                                val araToplam = fiyat * eleman.miktar
                                toplamTutar += araToplam

                                SiparisDetay(
                                    urun_id = eleman.urun.id,
                                    miktar = eleman.miktar,
                                    birim_fiyati = fiyat,
                                    ara_toplam = araToplam
                                )
                            }

                            // 2. Node.js'in beklediği o kargo paketini (JSON) hazırla
                            val istek = SiparisIstegi(
                                musteri_id = 1, // Şimdilik test için 1 numaralı müşteri
                                toplam_tutar = toplamTutar,
                                sepet_urunleri = detayListesi
                            )

                            // 3. Kargoyu (Siparişi) Node.js'e fırlat!
                            val cevap = ApiClient.retrofitService.siparisGonder(istek)

                            if (cevap.basarili) {
                                siparisSonucu = "Sipariş Başarıyla İletildi! ✓"
                                delay(2000) // Başarı mesajını 2 saniye göster
                                sepetListesi.clear() // Sepeti boşalt
                                siparisSonucu = ""
                            } else {
                                siparisSonucu = "Hata: ${cevap.mesaj}"
                            }
                        } catch (e: Exception) {
                            siparisSonucu = "Sunucuya bağlanılamadı! Node.js açık mı?"
                        } finally {
                            siparisGonderiliyor = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !siparisGonderiliyor
            ) {
                Text(if (siparisGonderiliyor) "Sipariş Mühürleniyor..." else "Siparişi İlet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KayitEkrani(geriyeDon: () -> Unit, kaydiTamamla: () -> Unit) {
    // (Burası bir önceki kodla tamamen aynı, tasarruf için özet geçildi. Önceki dosyandaki KayitEkrani fonksiyonunun içini değiştirmene gerek yok, doğrudan çalışacaktır.)
    var dukkanAdi by remember { mutableStateOf("") }
    var yetkiliAdi by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var konumAlindi by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var yukleniyor by remember { mutableStateOf(false) }
    var sonucMesaji by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Müşteri Kaydı", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(value = dukkanAdi, onValueChange = { dukkanAdi = it }, label = { Text("Dükkan / İşletme Adı (Opsiyonel)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true)
        OutlinedTextField(value = yetkiliAdi, onValueChange = { yetkiliAdi = it }, label = { Text("Yetkili Adı Soyadı") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true)
        OutlinedTextField(value = telefon, onValueChange = { telefon = it }, label = { Text("Telefon Numarası") }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), singleLine = true)
        Button(onClick = { konumAlindi = true }, modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 16.dp)) { Text(if (konumAlindi) "📍 Konum Kaydedildi ✓" else "📍 Sipariş Teslim Konumumu Al") }
        Button(
            onClick = {
                yukleniyor = true
                coroutineScope.launch {
                    try {
                        val cevap = ApiClient.retrofitService.musteriKaydet(KayitIstegi(dukkanAdi, yetkiliAdi, telefon, 37.0, 35.32))
                        if (cevap.basarili) { sonucMesaji = "Kayıt Başarılı!"; delay(2000); kaydiTamamla() } else { sonucMesaji = "Hata: ${cevap.mesaj}" }
                    } catch (e: Exception) { sonucMesaji = "Sunucuya ulaşılamadı!" } finally { yukleniyor = false }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp), enabled = yetkiliAdi.isNotEmpty() && telefon.isNotEmpty() && konumAlindi && !yukleniyor
        ) { Text(if (yukleniyor) "Gönderiliyor..." else "Kayıt Ol ve Onaya Gönder") }
        if (sonucMesaji.isNotEmpty()) { Text(text = sonucMesaji, color = if (sonucMesaji.contains("Başarılı")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(top = 16.dp)) }
        TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(top = 16.dp)) { Text("İptal Et ve Sepete Dön") }
    }
}