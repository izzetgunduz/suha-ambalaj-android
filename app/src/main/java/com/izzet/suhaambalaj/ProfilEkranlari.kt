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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfilVeSiparisEkrani(detaylaraGit: (Int) -> Unit) { // YENİ: Parametre eklendi
    val context = LocalContext.current
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilDuzenlemeEkrani(geriyeDon: () -> Unit) {
    val context = LocalContext.current
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
                            delay(2000)
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