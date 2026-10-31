package com.izzet.suhaambalaj

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnaSayfaVitrini(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean, sepeteEkle: (Urun) -> Unit) {
    var aramaMetni by remember { mutableStateOf("") }
    val kullaniciRol = SessionManager(LocalContext.current).onayDurumuGetir()

    // Eski Dialog değişkenleri (seciliUrun vb.) tamamen silindi
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

    val filtrelenmisUrunler = urunListesi.filter { urun ->
        val urunAdiUyar = urun.urun_adi.contains(aramaMetni, ignoreCase = true)
        val kategoriAdiUyar = urun.kategori_adi?.contains(aramaMetni, ignoreCase = true) ?: false
        val markaUyar = urun.marka?.contains(aramaMetni, ignoreCase = true) ?: false

        urunAdiUyar || kategoriAdiUyar || markaUyar
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtrelenmisUrunler) { urun ->
                    UrunKarti(
                        urun = urun,
                        fiyatlarGizliMi = fiyatlarGizliMi,
                        kullaniciRol = kullaniciRol,
                        sepeteEkle = {
                            sepeteEkle(urun)
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun UrunKarti(urun: Urun, fiyatlarGizliMi: Boolean, kullaniciRol: String, sepeteEkle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(320.dp).padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                Text("Görsel", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.weight(1f))

            if (!fiyatlarGizliMi) {
                Text(text = "${urun.taban_fiyati} ₺ / ${urun.satis_birimi}", color = Color(0xFF0369A1), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            } else {
                Text(text = "Fiyat Gizli", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (kullaniciRol != "admin") {
                Button(
                    onClick = { sepeteEkle() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    // YENİ: Artık direkt sepete eklemediği, detay ekranına attığı için metni güncelledik
                    Text("İncele", color = Color.White)
                }
            }
        }
    }
}