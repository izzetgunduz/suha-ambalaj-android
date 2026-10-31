package com.izzet.suhaambalaj

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrunDetayEkrani(
    urun: Urun,
    fiyatlarGizliMi: Boolean,
    geriyeDon: () -> Unit,
    // YENİ: Callback'e 5. parametre olarak "Ozet Metni" eklendi (String)
    sepeteOnayla: (Urun, Double, String, Varyant?, String) -> Unit
) {
    val renkler = urun.varyantlar?.mapNotNull { it.renk }?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
    val olculer = urun.varyantlar?.mapNotNull { it.olcu }?.filter { it.isNotBlank() }?.distinct() ?: emptyList()

    var seciliRenk by remember { mutableStateOf(renkler.firstOrNull() ?: "") }
    var seciliOlcu by remember { mutableStateOf(olculer.firstOrNull() ?: "") }
    val aktifVaryant = urun.varyantlar?.find { it.renk == seciliRenk && it.olcu == seciliOlcu }

    var secilenBirim by remember { mutableStateOf(if (urun.urun_tipi == "Koli") "Koli" else urun.satis_birimi) }
    var miktar by remember { mutableDoubleStateOf(1.0) }

    // YENİ: Koli / Paket Fiyat Hesaplama Mantığı
    val tabanFiyat = urun.taban_fiyati.toDoubleOrNull() ?: 0.0
    val ekFiyat = aktifVaryant?.ek_fiyat ?: 0.0

    // Eğer koli satılan bir ürün "Paket" olarak seçildiyse, fiyatı koli içi paket sayısına böler
    val koliIciPaketSayisi = urun.koli_ici_paket ?: 1
    val bolen = if (secilenBirim == "Paket" && urun.urun_tipi == "Koli" && koliIciPaketSayisi > 0) koliIciPaketSayisi else 1

    val guncelBirimFiyat = (tabanFiyat + ekFiyat) / bolen
    val toplamTutar = guncelBirimFiyat * miktar

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = geriyeDon) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = MaterialTheme.colorScheme.primary) }
                Text(text = urun.urun_adi, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!fiyatlarGizliMi) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Toplam Tutar:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                            Text("${String.format("%.2f", toplamTutar)} ₺", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (miktar > 1) miktar -= 1 }) { Text("-", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary) }
                            Text(text = if (miktar % 1.0 == 0.0) miktar.toInt().toString() else miktar.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
                            IconButton(onClick = { miktar += 1 }) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                // YENİ: Profesyonel Sipariş Özeti Cümlesi Oluşturucu
                                val miktarStr = if (miktar % 1.0 == 0.0) miktar.toInt().toString() else miktar.toString()
                                val renkStr = if (seciliRenk.isNotBlank()) "$seciliRenk " else ""
                                val olcuStr = if (seciliOlcu.isNotBlank()) "$seciliOlcu " else ""
                                // Çıktı Örneği: "5 Paket - Siyah Küçük Şeffaf Poşet"
                                // "1 koli" kısmını sildik, doğrudan okla ve renkle başlıyor
                                val siparisOzeti = "> $renkStr$olcuStr${urun.urun_adi}".trim()
                                sepeteOnayla(urun, miktar, secilenBirim, aktifVaryant, siparisOzeti)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Sepete Ekle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                Text("Ürün Görseli", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = urun.urun_adi, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp))
                if (!urun.birim_detayi.isNullOrBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(text = "📦 Hiyerarşi: ${urun.birim_detayi}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp))
                    }
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                if (renkler.isNotEmpty()) {
                    Text("Renk Seçimi", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        items(renkler.size) { index ->
                            val renk = renkler[index]
                            ModernFiltreButonu(metin = renk, secili = seciliRenk == renk) { seciliRenk = renk }
                        }
                    }
                }

                if (olculer.isNotEmpty()) {
                    Text("Ebat / Ölçü Seçimi", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        items(olculer.size) { index ->
                            val olcu = olculer[index]
                            val uyumluVaryant = urun.varyantlar?.find { it.renk == seciliRenk && it.olcu == olcu }
                            val fiyatFarkiMetni = if (uyumluVaryant != null && uyumluVaryant.ek_fiyat > 0 && !fiyatlarGizliMi) " (+${uyumluVaryant.ek_fiyat} ₺)" else ""
                            ModernFiltreButonu(metin = "$olcu$fiyatFarkiMetni", secili = seciliOlcu == olcu) { seciliOlcu = olcu }
                        }
                    }
                }

                Text("Alım Şekli (Birim)", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    val gecerliBirimler = mutableListOf(urun.satis_birimi)
                    // YENİ: Ürün koli ise, "Paket" seçeneğini de dinamik olarak ekrana çıkarıyoruz
                    val koliPaketSayisi = urun.koli_ici_paket ?: 0
                    if (urun.urun_tipi == "Koli" && koliPaketSayisi > 0) gecerliBirimler.add("Paket")

                    gecerliBirimler.distinct().forEach { birim ->
                        ModernFiltreButonu(metin = birim, secili = secilenBirim == birim) { secilenBirim = birim }
                    }
                }
            }
        }
    }
}