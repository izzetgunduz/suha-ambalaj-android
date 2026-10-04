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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izzet.suhaambalaj.ui.theme.SuhaAmbalajTheme
import kotlinx.coroutines.delay

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

data class Urun(val id: Int, val ad: String, val fiyat: Double, val birim: String, val kategori: String)
data class SepetElemani(val urun: Urun, var miktar: Int)

@Composable
fun AnaUygulamaEkrani() {
    var seciliSekme by remember { mutableIntStateOf(0) }
    // İleride Yönetici panelinden kontrol edilecek olan şalter
    val fiyatlarGizliMi by remember { mutableStateOf(true) }
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
    val ornekUrunler = listOf(
        Urun(1, "Azracup Karton Bardak 7oz", 450.00, "Koli", "Bardak"),
        Urun(2, "Metrosan Karton Bardak 7oz", 420.00, "Koli", "Bardak"),
        Urun(3, "Baloncuklu Naylon 100m", 150.00, "Rulo", "Sargı"),
        Urun(4, "Büyük Boy Koli", 22.00, "Adet", "Kutu"),
        Urun(5, "Şeffaf Koli Bandı 6'lı", 110.00, "Paket", "Bant"),
        Urun(6, "Plastik Sızdırmaz Kap", 3.25, "Adet", "Plastik")
    )

    val filtrelenmisUrunler = ornekUrunler.filter {
        it.ad.contains(aramaMetni, ignoreCase = true) || it.kategori.contains(aramaMetni, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("SUHA AMBALAJ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        OutlinedTextField(
            value = aramaMetni,
            onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün, marka veya kategori ara...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
        )

        Text(if (aramaMetni.isEmpty()) "Popüler Ürünler" else "Arama Sonuçları", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
            items(filtrelenmisUrunler) { urun ->
                UrunKarti(urun = urun, fiyatlarGizliMi = fiyatlarGizliMi) { secilenUrun ->
                    val sepettekiVarMi = sepetListesi.find { it.urun.id == secilenUrun.id }
                    if (sepettekiVarMi != null) sepettekiVarMi.miktar++ else sepetListesi.add(SepetElemani(secilenUrun, 1))
                }
            }
        }
    }
}

@Composable
fun UrunKarti(urun: Urun, fiyatlarGizliMi: Boolean, sepeteEkle: (Urun) -> Unit) {
    var eklendiMi by remember { mutableStateOf(false) }

    LaunchedEffect(eklendiMi) {
        if (eklendiMi) {
            delay(1000)
            eklendiMi = false
        }
    }

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
            Text(text = urun.ad, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)

            if (!fiyatlarGizliMi) {
                Text("${urun.fiyat} ₺ / ${urun.birim}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                Text("Birim: ${urun.birim}", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Button(
                onClick = { sepeteEkle(urun); eklendiMi = true },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (eklendiMi) Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (eklendiMi) "Eklendi ✓" else "Sepete Ekle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SepetEkrani(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean) {
    var kayitEkraniGosterilsinMi by remember { mutableStateOf(false) }

    if (kayitEkraniGosterilsinMi) {
        KayitEkrani(
            geriyeDon = { kayitEkraniGosterilsinMi = false },
            kaydiTamamla = { kayitEkraniGosterilsinMi = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Sepetim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

            if (sepetListesi.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sepetiniz şu an boş.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    sepetListesi.forEach { eleman ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = eleman.urun.ad, fontWeight = FontWeight.Bold)
                                if (!fiyatlarGizliMi) {
                                    Text(text = "Fiyat: ${eleman.urun.fiyat} ₺", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                }
                            }
                            Text(text = "${eleman.miktar} ${eleman.urun.birim}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Button(
                    onClick = { kayitEkraniGosterilsinMi = true },
                    modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Siparişi İlet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KayitEkrani(geriyeDon: () -> Unit, kaydiTamamla: () -> Unit) {
    var dukkanAdi by remember { mutableStateOf("") }
    var yetkiliAdi by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var konumAlindi by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Müşteri Kaydı", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        Text("Sipariş verebilmek için yönetici onayı gereklidir.", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = dukkanAdi,
            onValueChange = { dukkanAdi = it },
            label = { Text("Dükkan / İşletme Adı (Opsiyonel)") }, // Opsiyonel olduğu belirtildi
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = yetkiliAdi,
            onValueChange = { yetkiliAdi = it },
            label = { Text("Yetkili Adı Soyadı") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = telefon,
            onValueChange = { telefon = it },
            label = { Text("Telefon Numarası") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            singleLine = true
        )

        Button(
            onClick = { konumAlindi = true },
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (konumAlindi) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (konumAlindi) "📍 Konum Kaydedildi ✓" else "📍 Sipariş Teslim Konumumu Al", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { kaydiTamamla() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            // Dükkan adı kuralı kaldırıldı, artık yetkili adı, telefon ve konum yeterli
            enabled = yetkiliAdi.isNotEmpty() && telefon.isNotEmpty() && konumAlindi
        ) {
            Text("Kayıt Ol ve Onaya Gönder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("İptal Et ve Sepete Dön", color = MaterialTheme.colorScheme.primary)
        }
    }
}