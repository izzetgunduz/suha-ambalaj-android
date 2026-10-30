package com.izzet.suhaambalaj

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SepetEkrani(sepetListesi: MutableList<SepetElemani>, fiyatlarGizliMi: Boolean, profilDuzenlemeyeGit: () -> Unit = {}) {    val coroutineScope = rememberCoroutineScope()
    var siparisGonderiliyor by remember { mutableStateOf(false) }
    var siparisSonucu by remember { mutableStateOf("") }
    var uyariPenceresiAcik by remember { mutableStateOf(false) }
    var uyariBasligi by remember { mutableStateOf("") }
    var uyariMesaji by remember { mutableStateOf("") }
    var musteriDurumu by remember { mutableStateOf("onayli") }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sepetim", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        if (sepetListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sepetiniz boş.", color = Color.Gray) }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                sepetListesi.toList().forEach { eleman ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = eleman.urun.urun_adi, fontWeight = FontWeight.Bold)

                            // YENİ EKLENEN: Varyant Bilgisini Sepette Gösterme
                            if (eleman.secilenVaryant != null) {
                                val renk = eleman.secilenVaryant?.renk ?: ""
                                val olcu = eleman.secilenVaryant?.olcu ?: ""
                                val varyantMetni = "$renk $olcu".trim()

                                if (varyantMetni.isNotEmpty()) {
                                    Text(text = "Seçim: $varyantMetni", color = Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
                                }
                            }

                            if (!fiyatlarGizliMi) {
                                // Taban fiyata varsa varyantın ek fiyatını da ekleyip gösteriyoruz
                                val tabanFiyat = eleman.urun.taban_fiyati.toDoubleOrNull() ?: 0.0
                                val ekFiyat = eleman.secilenVaryant?.ek_fiyat ?: 0.0
                                val guncelFiyat = tabanFiyat + ekFiyat

                                Text(text = "Birim Fiyatı: $guncelFiyat ₺", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val guncelIndex = sepetListesi.indexOf(eleman)
                                if (guncelIndex != -1) {
                                    if (eleman.miktar > 1) {
                                        // HATA DÜZELTİLDİ: - işareti eklendi
                                        sepetListesi[guncelIndex] = eleman.copy(miktar = eleman.miktar - 1)
                                    } else {
                                        sepetListesi.removeAt(guncelIndex)
                                    }
                                }
                            }) {
                                Text(text = "-", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Red)
                            }

                            val formatliMiktar = if (eleman.miktar % 1.0 == 0.0) eleman.miktar.toInt().toString() else eleman.miktar.toString()
                            Text(
                                text = "$formatliMiktar ${eleman.secilenBirim}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(onClick = {
                                val guncelIndex = sepetListesi.indexOf(eleman)
                                if (guncelIndex != -1) {
                                    sepetListesi[guncelIndex] = eleman.copy(miktar = eleman.miktar + 1)
                                }
                            }) {
                                Text(text = "+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            if (siparisSonucu.isNotEmpty()) {
                Text(text = siparisSonucu, color = if (siparisSonucu.contains("Başarılı") || siparisSonucu.contains("İletildi")) Color(0xFF10B981) else Color.Red, modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    if (!sessionManager.girisYapildiMi()) {
                        siparisSonucu = "Sipariş vermek için Profil sekmesinden giriş yapmalısınız."
                        return@Button
                    }

                    siparisGonderiliyor = true
                    siparisSonucu = ""

                    coroutineScope.launch {
                        try {
                            val gercekMusteriId = sessionManager.musteriIdGetir()

                            // YENİ: KASADAN GEÇMEDEN ÖNCE YÖNETİCİNİN KARARINI SORGULA
                            val durumCevap = ApiClient.retrofitService.musteriDurumuGetir(gercekMusteriId)

                            if (durumCevap.basarili) {
                                musteriDurumu = durumCevap.durum ?: "bekliyor"

                                when (musteriDurumu) {
                                    "onayli" -> {
                                        var toplamTutar = 0.0
                                        val detayListesi = sepetListesi.map { eleman ->
                                            val ekFiyat = eleman.secilenVaryant?.ek_fiyat ?: 0.0
                                            val fiyat = (eleman.urun.taban_fiyati.toDoubleOrNull() ?: 0.0) + ekFiyat
                                            val araToplam = fiyat * eleman.miktar
                                            toplamTutar += araToplam

                                            val varyantMetni = if (eleman.secilenVaryant != null) "${eleman.secilenVaryant.renk ?: ""} ${eleman.secilenVaryant.olcu ?: ""}".trim() else null

                                            SiparisDetay(eleman.urun.id, eleman.miktar, fiyat, araToplam, eleman.secilenBirim ?: "", varyantMetni)
                                        }

                                        val cevap = ApiClient.retrofitService.siparisGonder(SiparisIstegi(gercekMusteriId, toplamTutar, detayListesi))

                                        if (cevap.basarili) {
                                            siparisSonucu = "Sipariş Başarıyla İletildi! ✓"
                                            delay(2000)
                                            sepetListesi.clear()
                                            siparisSonucu = ""
                                        } else {
                                            siparisSonucu = "Hata: ${cevap.mesaj}"
                                        }
                                    }
                                    "bekliyor" -> {
                                        uyariBasligi = "Hesabınız Onay Aşamasında"
                                        uyariMesaji = "Sipariş verebilmek için yönetici onayını beklemelisiniz. Uygulamayı incelemeye devam edebilirsiniz."
                                        uyariPenceresiAcik = true
                                    }
                                    "engellendi" -> {
                                        uyariBasligi = "Sipariş Verilemiyor"
                                        val sebep = durumCevap.sebep ?: "Bilinmeyen bir sebep"
                                        uyariMesaji = "Hesabınız şu sebeple askıya alınmıştır:\n$sebep\n\nLütfen profil bilgilerinizi güncelleyerek tekrar onaya gönderin."
                                        uyariPenceresiAcik = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            siparisSonucu = "Sunucuya bağlanılamadı!"
                        } finally {
                            siparisGonderiliyor = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !siparisGonderiliyor
            ) { Text(if (siparisGonderiliyor) "Sipariş Mühürleniyor..." else "Siparişi İlet", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }

    // YENİ: DURUM UYARI PENCERESİ
    if (uyariPenceresiAcik) {
        AlertDialog(
            onDismissRequest = { uyariPenceresiAcik = false },
            title = { Text(uyariBasligi, color = Color.Red, fontWeight = FontWeight.ExtraBold) },
            text = { Text(uyariMesaji, fontSize = 16.sp) },
            confirmButton = {
                if (musteriDurumu == "engellendi") {
                    Button(onClick = {
                        uyariPenceresiAcik = false
                        profilDuzenlemeyeGit() // Butona basılınca 5. sekmeye uçur!
                    }) { Text("Bilgilerimi Düzenle") }
                }
            },
            dismissButton = {
                TextButton(onClick = { uyariPenceresiAcik = false }) { Text("Anladım") }
            }
        )
    }
}