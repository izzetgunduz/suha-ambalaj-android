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

data class SepetElemani(val urun: Urun, var miktar: Double, var secilenBirim: String, val secilenVaryant: Varyant? = null)

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

    // YENİ: Hangi ürünün detay ekranının açık olduğunu tutan değişken
    var detayGosterilenUrun by remember { mutableStateOf<Urun?>(null) }

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
                NavigationBarItem(icon = { Icon(Icons.Filled.Person, "Profil") }, label = { Text("Profil") }, selected = seciliSekme == 3 || seciliSekme == 4, onClick = { seciliSekme = 3 })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // YENİ: Eğer bir ürün seçildiyse ana ekranları gizle ve sadece Detay Ekranını tam sayfa göster
            if (detayGosterilenUrun != null) {
                UrunDetayEkrani(
                    urun = detayGosterilenUrun!!,
                    fiyatlarGizliMi = fiyatlarGizliMi,
                    geriyeDon = { detayGosterilenUrun = null },
                    sepeteOnayla = { urun, miktar, secilenBirim, secilenVaryant ->
                        val sepettekiUrun = sepetListesi.find { it.urun.id == urun.id && it.secilenBirim == secilenBirim && it.secilenVaryant?.id == secilenVaryant?.id }
                        if (sepettekiUrun != null) {
                            val index = sepetListesi.indexOf(sepettekiUrun)
                            sepetListesi[index] = sepettekiUrun.copy(miktar = sepettekiUrun.miktar + miktar)
                        } else {
                            sepetListesi.add(SepetElemani(urun, miktar, secilenBirim, secilenVaryant))
                        }
                        // Sepete eklendikten sonra detay ekranını kapatıp vitrine dönüyoruz
                        detayGosterilenUrun = null
                    }
                )
            } else {
                when (seciliSekme) {
                    0 -> AnaSayfaVitrini(sepetListesi, fiyatlarGizliMi, sepeteEkle = { urun ->
                        // YENİ: Butona basıldığında artık miktar seçimi yerine tam sayfa detay ekranını tetikliyoruz
                        detayGosterilenUrun = urun
                    })

                    1 -> {
                        if (aktifAltKategoriId != null) {
                            KategoriUrunleriEkrani(
                                kategoriId = aktifAltKategoriId!!,
                                kategoriAdi = aktifAltKategoriAdi,
                                fiyatlarGizliMi = fiyatlarGizliMi,
                                kullaniciRol = "musteri",
                                geriyeDon = { aktifAltKategoriId = null },
                                sepeteEkle = { urun ->
                                    // YENİ: Kategorilerdeki karta tıklanınca da detay ekranına geçiş yap
                                    detayGosterilenUrun = urun
                                }
                            )
                        } else {
                            KategorilerEkrani(
                                altKategoriyeGit = { id, ad ->
                                    aktifAltKategoriId = id
                                    aktifAltKategoriAdi = ad
                                }
                            )
                        }
                    }

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

                    5 -> ProfilDuzenlemeEkrani(
                        geriyeDon = { seciliSekme = 2 }
                    )
                }
            }
        }
    }
}





























