package com.izzet.suhaambalaj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
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
                3 -> ProfilVeSiparisEkrani() // YENİ: Boş sayfa yerine artık Sipariş Ekranı var
            }
        }
    }
}

// --- YENİ EKLENEN PROFİL VE SİPARİŞ GEÇMİŞİ EKRANI ---
@Composable
fun ProfilVeSiparisEkrani() {
    // Profil sekmesi içindeki ekran geçişlerini yöneten durum yöneticisi
    var aktifSayfa by remember { mutableStateOf("karsilama") }
    var girisYapildiMi by remember { mutableStateOf(false) }

    if (girisYapildiMi) {
        GecmisSiparislerEkrani()
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

// "Kayıt Olmadan İncele" butonunun kaldırıldığı, daha sade Giriş Ekranı
@Composable
fun KarsilamaEkrani(girisYapTikladi: () -> Unit, kayitOlTikladi: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SUHA AMBALAJ B2B", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 32.dp))
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Giriş Yap", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 24.dp))

        // YENİ: Görsel +90 eklendi ve baştaki 0 engellendi
        OutlinedTextField(
            value = telefon,
            onValueChange = {
                // Kullanıcı numaraya 0 ile başlarsa o sıfırı otomatik siler
                telefon = it.removePrefix("0")
            },
            label = { Text("Telefon Numarası") },
            leadingIcon = { Text("+90", modifier = Modifier.padding(start = 16.dp, end = 4.dp), fontWeight = FontWeight.Bold, color = Color.DarkGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = sifre, onValueChange = { sifre = it },
            label = { Text("Şifre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        TextButton(onClick = { /* Şifremi unuttum eklenecek */ }, modifier = Modifier.align(Alignment.End)) {
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
                        // Artık ekranda +90 olduğu için sadece birleştirip gönderiyoruz
                        val istek = GirisIstegi("+90$telefon", sifre)
                        val cevap = ApiClient.retrofitService.girisYap(istek)

                        if (cevap.basarili) {
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
fun GecmisSiparislerEkrani() {
    var siparisListesi by remember { mutableStateOf<List<SiparisOzet>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val cevap = ApiClient.retrofitService.siparisGecmisiniGetir()
            if (cevap.basarili && cevap.siparisler != null) {
                siparisListesi = cevap.siparisler
            } else if (cevap.basarili) {
                siparisListesi = emptyList()
            } else {
                hataMesaji = "Sipariş geçmişi alınamadı."
            }
        } catch (e: Exception) {
            hataMesaji = "Hata Detayı: ${e.message}"
        } finally {
            yukleniyor = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profilim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("İşletme: Suha Ambalaj B2B", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Yetkili: İzzet Gündüz", color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                Text("Müşteri No: #1", color = Color.DarkGray)
            }
        }

        Text("Geçmiş Siparişlerim", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (hataMesaji.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(hataMesaji, color = Color.Red, fontWeight = FontWeight.Bold) }
        } else if (siparisListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Henüz verilmiş bir siparişiniz bulunmamaktadır.", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(siparisListesi) { siparis ->
                    SiparisKarti(siparis)
                }
            }
        }
    }
}

@Composable
fun SiparisKarti(siparis: SiparisOzet) {
    // Sipariş durumuna göre renk belirliyoruz
    val durumRengi = when(siparis.siparis_durumu.lowercase()) {
        "bekliyor" -> Color(0xFFF59E0B) // Turuncu
        "onaylandı", "hazırlanıyor" -> Color(0xFF10B981) // Yeşil
        "iptal edildi" -> Color.Red // Kırmızı
        else -> Color.Gray
    }

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
                Text(text = siparis.siparis_tarihi.take(10), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${siparis.toplam_tutar} ₺", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = siparis.siparis_durumu.uppercase(),
                    fontSize = 12.sp,
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
                gridItems(filtrelenmisUrunler) { urun ->
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
fun SepetEkrani(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    var siparisGonderiliyor by remember { mutableStateOf(false) }
    var siparisSonucu by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sepetim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        if (sepetListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sepetiniz boş.", color = Color.Gray) }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                sepetListesi.forEach { eleman ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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

            if (siparisSonucu.isNotEmpty()) {
                Text(text = siparisSonucu, color = if (siparisSonucu.contains("Başarı")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    siparisGonderiliyor = true
                    siparisSonucu = ""
                    coroutineScope.launch {
                        try {
                            var toplamTutar = 0.0
                            val detayListesi = sepetListesi.map { eleman ->
                                val fiyat = eleman.urun.taban_fiyati.toDoubleOrNull() ?: 0.0
                                val araToplam = fiyat * eleman.miktar
                                toplamTutar += araToplam
                                SiparisDetay(eleman.urun.id, eleman.miktar, fiyat, araToplam)
                            }

                            val cevap = ApiClient.retrofitService.siparisGonder(SiparisIstegi(1, toplamTutar, detayListesi))

                            if (cevap.basarili) {
                                siparisSonucu = "Sipariş Başarıyla İletildi! ✓"
                                delay(2000)
                                sepetListesi.clear()
                                siparisSonucu = ""
                            } else {
                                siparisSonucu = "Hata: ${cevap.mesaj}"
                            }
                        } catch (e: Exception) { siparisSonucu = "Sunucuya bağlanılamadı! Node.js açık mı?" } finally { siparisGonderiliyor = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !siparisGonderiliyor
            ) { Text(if (siparisGonderiliyor) "Sipariş Mühürleniyor..." else "Siparişi İlet", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}