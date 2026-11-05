package com.izzet.suhaambalaj

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Kategori kartlarında fotoğraf yoksa kullanılan vurgu renkleri (kategori ID'sine göre sabit)
private val KATEGORI_RENKLERI = listOf(
    Color(0xFF1E3A8A), Color(0xFF0F766E), Color(0xFF7C3AED),
    Color(0xFFB45309), Color(0xFFBE123C), Color(0xFF0369A1)
)

private fun kategoriRengi(id: Int) = KATEGORI_RENKLERI[Math.floorMod(id, KATEGORI_RENKLERI.size)]

// ===================================================================
// 1. KATEGORİLER ANA EKRANI (Görsel kart ızgarası + akıllı arama)
// ===================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorilerEkrani(altKategoriyeGit: (Int, String) -> Unit) {
    var kategoriler by remember { mutableStateOf<List<KategoriAgaci>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }
    var aramaMetni by remember { mutableStateOf("") }
    var yenile by remember { mutableIntStateOf(0) }

    // Ekran her açıldığında güncel veriyi çeker: yeni eklenen ürün/kategori anında görünür
    LaunchedEffect(yenile) {
        yukleniyor = true
        hataMesaji = ""
        try {
            val cevap = ApiClient.retrofitService.kategoriAgaciniGetir()
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

    val toplamUrun = kategoriler.sumOf { it.urun_sayisi ?: 0 }
    val arama = aramaMetni.trim()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // --- BAŞLIK VE ARAMA ---
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Text("Kategoriler", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            if (!yukleniyor && kategoriler.isNotEmpty()) {
                Text(
                    "${kategoriler.size} kategori · $toplamUrun ürün",
                    fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp)
                )
            }
            OutlinedTextField(
                value = aramaMetni,
                onValueChange = { aramaMetni = it },
                placeholder = { Text("Kategori veya alt kategori ara...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
                trailingIcon = {
                    if (aramaMetni.isNotEmpty()) {
                        IconButton(onClick = { aramaMetni = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Temizle") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                singleLine = true
            )
        }

        when {
            yukleniyor -> KategoriIskeleti()

            hataMesaji.isNotEmpty() -> BosDurum(
                baslik = "Bir sorun oluştu",
                aciklama = hataMesaji,
                butonMetni = "Tekrar Dene",
                butonTiklandi = { yenile++ }
            )

            kategoriler.isEmpty() -> BosDurum(
                baslik = "Henüz kategori yok",
                aciklama = "Yönetici yeni ürün eklediğinde kategoriler burada görünecek."
            )

            // --- ARAMA MODU: Ana + alt kategorilerde düz liste ---
            arama.isNotEmpty() -> {
                val sonuclar = buildList {
                    kategoriler.forEach { ana ->
                        if (ana.kategori_adi.contains(arama, ignoreCase = true)) {
                            add(Triple(ana.id, ana.kategori_adi, "${ana.urun_sayisi ?: 0} ürün · Ana kategori"))
                        }
                        ana.alt_kategoriler.orEmpty().forEach { alt ->
                            if (alt.kategori_adi.contains(arama, ignoreCase = true)) {
                                add(Triple(alt.id, alt.kategori_adi, "${ana.kategori_adi} › ${alt.urun_sayisi ?: 0} ürün"))
                            }
                        }
                    }
                }
                if (sonuclar.isEmpty()) {
                    BosDurum(baslik = "Sonuç bulunamadı", aciklama = "\"$arama\" ile eşleşen kategori yok.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sonuclar.size) { i ->
                            val (id, ad, altYazi) = sonuclar[i]
                            AramaSonucSatiri(id = id, ad = ad, altYazi = altYazi) { altKategoriyeGit(id, ad) }
                        }
                    }
                }
            }

            // --- NORMAL MOD: Görsel kategori kartları ---
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(kategoriler.size) { i ->
                        val kategori = kategoriler[i]
                        AnaKategoriKarti(kategori) { altKategoriyeGit(kategori.id, kategori.kategori_adi) }
                    }

                    // Alt kategorilere hızlı erişim (tek dokunuşla doğrudan ürünlere)
                    val altlarVarMi = kategoriler.any { !it.alt_kategoriler.isNullOrEmpty() }
                    if (altlarVarMi) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Hızlı Erişim",
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        kategoriler.filter { !it.alt_kategoriler.isNullOrEmpty() }.forEach { ana ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AltKategoriSeridi(ana) { id, ad -> altKategoriyeGit(id, ad) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnaKategoriKarti(kategori: KategoriAgaci, tiklandi: () -> Unit) {
    val renk = kategoriRengi(kategori.id)
    val kapak = ApiClient.gorselUrl(kategori.kapak_fotograf)
    val altSayisi = kategori.alt_kategoriler?.size ?: 0

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp).clickable { tiklandi() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (kapak != null) {
                AsyncImage(
                    model = kapak,
                    contentDescription = kategori.kategori_adi,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(renk, renk.copy(alpha = 0.7f)))))
                // Fotoğraf yoksa büyük baş harf dekoru
                Text(
                    text = kategori.kategori_adi.take(1).uppercase(),
                    fontSize = 96.sp, fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp)
                )
            }

            if (altSayisi > 0) {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                ) {
                    Text("$altSayisi alt kategori", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = renk, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(
                    kategori.kategori_adi,
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${kategori.urun_sayisi ?: 0} ürün",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AltKategoriSeridi(ana: KategoriAgaci, altKategoriyeGit: (Int, String) -> Unit) {
    val renk = kategoriRengi(ana.id)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(renk))
            Text(ana.kategori_adi, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val altlar = ana.alt_kategoriler.orEmpty()
            items(altlar.size) { i ->
                val alt = altlar[i]
                Surface(
                    modifier = Modifier.clickable { altKategoriyeGit(alt.id, alt.kategori_adi) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, renk.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(alt.kategori_adi, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${alt.urun_sayisi ?: 0} ürün", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun AramaSonucSatiri(id: Int, ad: String, altYazi: String, tiklandi: () -> Unit) {
    val renk = kategoriRengi(id)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { tiklandi() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(renk.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(ad.take(1).uppercase(), fontWeight = FontWeight.Black, color = renk, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(ad, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(altYazi, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

// Yüklenirken boş ekran yerine kart iskeletleri
@Composable
private fun KategoriIskeleti() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun BosDurum(baslik: String, aciklama: String, butonMetni: String? = null, butonTiklandi: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(baslik, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(aciklama, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            if (butonMetni != null) {
                Button(onClick = butonTiklandi, modifier = Modifier.padding(top = 16.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(butonMetni)
                }
            }
        }
    }
}

// ===================================================================
// 2. KATEGORİ ÜRÜNLERİ EKRANI
// Ana kategori açılınca alt kategorilerin ürünleri de gelir; üstteki çiplerle daraltılır.
// ===================================================================
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
    var altKategoriler by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf("") }
    var yenile by remember { mutableIntStateOf(0) }

    var seciliAltKategoriId by remember(kategoriId) { mutableStateOf<Int?>(null) }
    var seciliMarka by remember(kategoriId) { mutableStateOf("Tümü") }
    var aramaMetni by remember(kategoriId) { mutableStateOf("") }

    LaunchedEffect(kategoriId, yenile) {
        yukleniyor = true
        hataMesaji = ""
        try {
            val urunCevap = ApiClient.retrofitService.kategoriUrunleriniGetir(kategoriId)
            if (urunCevap.basarili) urunler = urunCevap.urunler ?: emptyList()
            else hataMesaji = urunCevap.mesaj ?: "Ürünler alınamadı."

            val altCevap = ApiClient.retrofitService.altKategorileriGetir(kategoriId)
            if (altCevap.basarili) altKategoriler = altCevap.altKategoriler ?: emptyList()
        } catch (e: Exception) {
            hataMesaji = "Sunucuya bağlanılamadı."
        } finally {
            yukleniyor = false
        }
    }

    val altKategoriFiltreli = urunler.filter { seciliAltKategoriId == null || it.kategori_id == seciliAltKategoriId }
    val markalar = listOf("Tümü") + altKategoriFiltreli.mapNotNull { it.marka?.trim() }.filter { it.isNotBlank() }.distinct()
    val gosterilecekUrunler = altKategoriFiltreli.filter { urun ->
        (seciliMarka == "Tümü" || urun.marka?.trim() == seciliMarka) &&
                (aramaMetni.isBlank() || urun.urun_adi.contains(aramaMetni.trim(), ignoreCase = true) || (urun.marka?.contains(aramaMetni.trim(), ignoreCase = true) ?: false))
    }
    val filtreAktif = seciliAltKategoriId != null || seciliMarka != "Tümü" || aramaMetni.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // --- BAŞLIK ---
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = geriyeDon) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(kategoriAdi, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!yukleniyor) Text("${gosterilecekUrunler.size} ürün", fontSize = 13.sp, color = Color.Gray)
                }
            }

            OutlinedTextField(
                value = aramaMetni,
                onValueChange = { aramaMetni = it },
                placeholder = { Text("Bu kategoride ara...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (aramaMetni.isNotEmpty()) IconButton(onClick = { aramaMetni = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Temizle") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                singleLine = true
            )

            // Alt kategori çipleri (sayılarıyla)
            if (altKategoriler.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    item {
                        ModernFiltreButonu(metin = "Tümü (${urunler.size})", secili = seciliAltKategoriId == null) {
                            seciliAltKategoriId = null; seciliMarka = "Tümü"
                        }
                    }
                    items(altKategoriler.size) { i ->
                        val alt = altKategoriler[i]
                        val adet = urunler.count { it.kategori_id == alt.id }
                        ModernFiltreButonu(metin = "${alt.kategori_adi} ($adet)", secili = seciliAltKategoriId == alt.id) {
                            seciliAltKategoriId = alt.id; seciliMarka = "Tümü"
                        }
                    }
                }
            }

            // Marka çipleri (birden fazla marka varsa)
            if (markalar.size > 2) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(markalar.size) { i ->
                        val marka = markalar[i]
                        ModernFiltreButonu(metin = marka, secili = seciliMarka == marka) { seciliMarka = marka }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

        when {
            yukleniyor -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            hataMesaji.isNotEmpty() -> BosDurum(baslik = "Bir sorun oluştu", aciklama = hataMesaji, butonMetni = "Tekrar Dene", butonTiklandi = { yenile++ })

            gosterilecekUrunler.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (filtreAktif) "Bu filtrelere uygun ürün yok" else "Bu kategoride henüz ürün yok", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        if (filtreAktif) {
                            TextButton(onClick = { seciliAltKategoriId = null; seciliMarka = "Tümü"; aramaMetni = "" }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Filtreleri Temizle", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 100.dp)
                ) {
                    items(gosterilecekUrunler.size) { index ->
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
}

// MODERN FİLTRE BUTONU TASARIMI (Yardımcı Fonksiyon) — ürün ekleme ve detay ekranları da kullanıyor
@Composable
fun ModernFiltreButonu(metin: String, secili: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50),
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