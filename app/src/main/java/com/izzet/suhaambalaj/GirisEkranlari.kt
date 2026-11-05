package com.izzet.suhaambalaj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KarsilamaEkrani(girisYapTikladi: () -> Unit, kayitOlTikladi: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SUHA AMBALAJ ", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 32.dp))
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

    // YENİ: Hafıza yöneticisini ekrana çağırıyoruz
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Giriş Yap", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = telefon,
            onValueChange = { telefon = it.removePrefix("0") },
            label = { Text("Telefon Numarası") },
            leadingIcon = { Text("+90", modifier = Modifier.padding(start = 16.dp, end = 4.dp), fontWeight = FontWeight.Bold, color = Color.DarkGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        OutlinedTextField(
            value = sifre, onValueChange = { sifre = it },
            label = { Text("Şifre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true
        )

        TextButton(onClick = { /* Şifremi unuttum */ }, modifier = Modifier.align(Alignment.End)) {
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
                        val istek = GirisIstegi("+90$telefon", sifre)
                        val cevap = ApiClient.retrofitService.girisYap(istek)

                        if (cevap.basarili) {
                            // YENİ: Node.js'ten gelen bilgileri telefona kalıcı olarak kazıyoruz!
                            sessionManager.oturumuKaydet(
                                musteriId = cevap.musteri_id ?: -1,
                                yetkiliAdi = cevap.yetkili_adi ?: "",
                                onayDurumu = cevap.onay_durumu ?: "bekliyor",
                                rol = cevap.rol ?: "musteri"
                            )

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
                sonucMesaji = "Kayıt oluşturuluyor..."

                coroutineScope.launch {
                    try {
                        // Yeni mimaride konum sormadığımız için koordinatları şimdilik sıfır (0.0) gönderiyoruz.
                        // Zaten sipariş anında güncel konum alınıp kontrol edilecek.
                        val istek = KayitIstegi(isyeriAdi, yetkiliAdi, "+90$telefon", sifre, 0.0, 0.0)
                        val cevap = ApiClient.retrofitService.musteriKaydet(istek)

                        if (cevap.basarili) {
                            sonucMesaji = "Kayıt talebiniz alındı! Lütfen giriş yapınız."
                            kotlinx.coroutines.delay(2000)
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
            Text(if (yukleniyor) "İşleniyor..." else "Kayıt Talebi Gönder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { geriyeDon() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Geri Dön", color = Color.Gray)
        }
    }
}