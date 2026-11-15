# Ehliyet Deneme — Android

26 test, 1.300 soru, 45 dakikalık sınavlar. Tüm sorular ve cevaplar uygulamanın içinde; uygulama internetsiz çalışır. Ana sayfa siyah-kırmızı, soru sayfaları beyaz. Uygulamadaki tek kredi **KAYRA HAN AYDOĞAN**.

## GitHub Actions'da `gradlew` hatası alırsanız

Bu proje **`gradlew` dosyasına ihtiyaç duymaz**: GitHub Actions, Gradle 8.9'u `gradle/actions/setup-gradle` ile kurar ve APK'yı `gradle :app:assembleDebug` ile üretir. Eğer Actions kayıtlarında **`set up JDK 11`** ya da **`chmod +x gradlew`** görürseniz, GitHub'ın otomatik oluşturduğu **farklı/eski Android CI iş akışını** çalıştırıyorsunuz demektir.

Repository'de `.github/workflows/` dizinini açın. **Bu projeye ait `android.yml` dosyasını bırakın; JDK 11 / `./gradlew` içeren eski iş akışı dosyasını silin.** `android.yml` GitHub'a yüklenmemişse bu arşivdeki `.github/workflows/android.yml` dosyasını aynı konuma yükleyin. Doğru workflow adı **Build Android APK**'dir. `GITHUB_FIX.md` ayrıntılı talimatları içerir.

## GitHub'da APK oluşturma

1. ZIP dosyasını bilgisayarında **klasöre çıkar**.
2. GitHub'da `main` dalıyla boş bir repository oluştur. İlk açarken README veya başka dosya ekleme.
3. **Add file → Upload files** bölümünden ZIP'in **içindeki** dosya ve klasörleri yükle. Özellikle gizli `.github` klasörü, `app`, `tools`, `settings.gradle`, `build.gradle` ve `gradle.properties` GitHub'da repository kökünde olmalı. ZIP'i tek dosya olarak yükleme ve projenin tamamını ek bir alt klasör içine koyma.
4. Değişiklikleri `main` dalına commit et.
5. GitHub'da **Actions → Build Android APK** sayfasını aç. Commit sonrası işlem otomatik başlayacak. İstersen **Run workflow** ile de başlatabilirsin.
6. Başarılı işleme girip **Artifacts → Ehliyet-Deneme-APK** bağlantısını indir. İnen arşivi açınca `app-debug.apk` dosyasını göreceksin.
7. APK dosyasını Android telefonuna gönderip kur. Telefon, dosya yöneticisinden uygulama kurulmasına izin vermeni isteyebilir.

## Özellikler

- Test 1–Test 26: Her testte 50 soru, 45 dakika; başarı şartı en az 35 doğru.
- İlk işaretlenen şık kilitlenir, değiştirilemez. Doğru şık yeşil, yanlış şık kırmızı gösterilir.
- Süre dolduğunda test otomatik tamamlanır.
- Ana sayfaya dönüldüğünde sınav kaydedilir; geri dönünce süre kaldığı yerden değil, geçen gerçek zaman hesaba katılarak devam eder.
- Doğru, yanlış, boş, 100 üzerinden puan, sınav sonucu, tüm cevapları inceleme ekranı.
- Önceki sonuç ve en iyi puan cihazda saklanır.
- Reklam, hesap, sunucu, API, web sayfası veya internet izni yoktur.

## Teknik bilgiler

- Java 17 • Gradle 8.9 • Android Gradle Plugin 8.7.3
- Android compileSdk 35 • targetSdk 34 • minSdk 23 (Android 6.0+)
- Harici Android kütüphanesi kullanılmıyor. Uygulama yalnızca Android sistem API'leriyle yazıldı.
- `app/src/main/assets/tests/`: 26 adet JSON soru paketi.
- `app/src/main/assets/figures/`: Uygulamanın içinde bulunan görseller.
- Doğru yanıtın karşılığı: 0 = A, 1 = B, 2 = C, 3 = D.

## PDF'lerden alınan içerik

Gönderilen 26 PDF'nin tamamındaki 1.300 soru ve 1.300 cevap anahtarı alınmıştır. Kaynak PDF'lerde bazı soruların metninde şekilden söz edilmesine rağmen şeklin kendisi yer almıyordu. Bu sorular için soru ve doğru cevaba uygun beş tamamlayıcı görsel oluşturuldu. PDF'lerde bulunan trafik işareti görseli de alındı. PDF sayfaları, kaynak sayfa başlıkları ve logoları uygulamada bulunmaz.

## Kaynak veriyi kontrol etme

```bash
python3 tools/validate_exams.py
```

Bu kontrol 26 testin 1.300 sorusunu, her sorunun dört şıkkını, geçerli doğru yanıtları, konu dağılımını, görselleri ve uygulamanın internetsiz yapılandırmasını denetler. GitHub Actions, APK derlemesinden önce aynı denetimi otomatik çalıştırır.

**Not:** APK telefona kurulduktan sonra internet istemez. Yalnızca GitHub'ın APK'yı derlerken Java, Android SDK ve Gradle araçlarını indirmesi için internet gerekir.
