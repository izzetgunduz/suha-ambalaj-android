package com.izzet.suhaambalaj

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            LazyColumn(
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
    genisletildi: Boolean,
    kartaTiklandi: () -> Unit,
    altKategoriyeGit: (Int, String) -> Unit
) {
    var altKategoriler by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var altYukleniyor by remember { mutableStateOf(false) }

    // ÇÖZÜM: Kategori her açıldığında (genisletildi == true) sunucuya GÜNCEL veriyi sorar
    LaunchedEffect(genisletildi) {
        if (genisletildi) {
            altYukleniyor = true
            try {
                val cevap = ApiClient.retrofitService.altKategorileriGetir(kategori.id)
                if (cevap.basarili && cevap.altKategoriler != null) {
                    altKategoriler = cevap.altKategoriler
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
                Text(text = kategori.kategori_adi, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(imageVector = if (genisletildi) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "Genişlet", tint = MaterialTheme.colorScheme.primary)
            }

            AnimatedVisibility(visible = genisletildi) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                    if (altYukleniyor) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (altKategoriler.isEmpty()) {
                        Text("Bu kategoriye ait alt kategori bulunmamaktadır.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                    } else {
                        altKategoriler.forEach { altKategori ->
                            TextButton(
                                onClick = { altKategoriyeGit(altKategori.id, altKategori.kategori_adi) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Text(text = altKategori.kategori_adi, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
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

    // Eski Dialog değişkenleri silindi

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
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = geriyeDon) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
            }
            Text(text = kategoriAdi, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (markalar.size > 1 || olculer.size > 1) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(bottom = 12.dp)) {
                if (markalar.size > 1) {
                    LazyRow(
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
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
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

        if (yukleniyor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (gosterilecekUrunler.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bu kriterlere uygun ürün bulunamadı.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
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
                        sepeteEkle = {
                            // Butona basıldığında doğrudan yeni tam sayfa detay ekranına yönlendiriyor
                            sepeteEkle(urun)
                        }
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