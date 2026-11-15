# GitHub Actions — `gradlew` hatasını giderme

## Sebep

Bu projede Gradle Wrapper (`gradlew`) yoktur; çünkü Gradle 8.9, GitHub Actions tarafından otomatik yüklenir. Hata ekranındaki **set up JDK 11** ve **Grant execute permission for gradlew** adımları, projenin `.github/workflows/android.yml` dosyasına ait değildir. GitHub'da farklı bir varsayılan iş akışı çalışmaktadır.

## Çözüm

1. GitHub repository → **Code → .github → workflows** klasörüne girin.
2. `set up JDK 11` ve `chmod +x gradlew` içeren eski iş akışı dosyasını (`gradle.yml`, `android.yml` veya başka adla kayıtlı olabilir) **silin ya da içeriğini tamamen değiştirin**.
3. Bu ZIP'in `.github/workflows/android.yml` dosyasını repository'deki **`.github/workflows/android.yml`** konumuna ekleyin. Eski yanlış workflow ile birlikte bırakmayın.
4. `app/`, `tools/`, `settings.gradle`, `build.gradle`, `gradle.properties` dosyalarının GitHub repository'nin kökünde olduğunu kontrol edin. Ek bir alt dizinin içine koymayın.
5. Değişikliği `main` branch'ine kaydedin.
6. **Actions → Build Android APK → Run workflow** yolunu izleyin. Başarılı olduğunda **Ehliyet-Deneme-APK** artifact'ini indirin ve içindeki `app-debug.apk` dosyasını kullanın.

## Doğru iş akışı

- Java 17
- Gradle 8.9 (GitHub Actions kurar)
- Android SDK 35
- `gradle --no-daemon --stacktrace :app:assembleDebug`
- `gradlew` veya `chmod +x gradlew` yok

APK telefonda tamamen çevrimdışı çalışır. GitHub'ın derleme sırasında SDK ve Gradle indirmesi uygulamanın harici API kullandığı anlamına gelmez.
