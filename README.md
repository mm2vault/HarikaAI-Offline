# Aura AI

Ücretsiz, Türkçe odaklı Android asistanı. Kotlin ile geliştirilir; yerel yapay zekâ modeli APK içine gömülmez, uygulama içinde indirilir.

## Özellikler
- Siyah/mor neon arayüz ve yerel çizilmiş Aura orb'u
- LiteRT-LM ile cihaz içi Qwen3-0.6B
- Türkçe konuşma tanıma ve sesli yanıt (Android cihaz desteğine bağlı)
- Sohbet geçmişini cihazda saklama
- Kullanıcı açıkça istediğinde web araması
- GitHub Releases üzerinden APK sürüm kontrolü
- GitHub Actions ile APK derleme

## Model
Yaklaşık 586 MB boyutlu model: https://huggingface.co/litert-community/Qwen3-0.6B

## APK derleme
GitHub'daki Actions bölümünde **Aura AI Android APK Build** iş akışını aç. Başarılı derlemenin `Aura-AI-APK` artifact'ini indir. Release indirme bağlantısı da ilgili `aura-build-N` etiketinde yayımlanır.

## Android güncelleme imzası — önemli
Android, mevcut uygulamanın üzerine yalnızca aynı uygulama kimliği **ve aynı imza sertifikası** ile imzalanmış APK'yi kurar. Bu nedenle her GitHub Actions çalışmasında yeniden oluşturulan debug anahtarı, önceki APK'nin üzerine güncelleme olarak kurulamayabilir.

Gelecek derlemelerde sabit imza kullanmak için GitHub deposunda **Settings → Secrets and variables → Actions** bölümüne şu Actions secrets değerlerini ekle:
- `AURA_KEYSTORE_BASE64`: sabit keystore dosyasının Base64 içeriği
- `AURA_STORE_PASSWORD`: keystore parolası
- `AURA_KEY_ALIAS`: anahtar takma adı
- `AURA_KEY_PASSWORD`: anahtar parolası

**Mevcut telefondaki uygulamayla aynı imza anahtarı kullanılmalıdır.** Eski anahtar elinde yoksa yeni imzalı APK eski kurulumun üzerine yüklenemez; uygulamayı kaldırmak yerel sohbet geçmişini silebilir. Önce sohbet geçmişini yedeklemeden uygulamayı kaldırma. Gizli anahtar dosyasını Git deposuna asla ekleme.

> CI derlemesinin başarılı olması, gerçek telefonda kurulumun veya model çıkarımının doğrulandığı anlamına gelmez. Gerçek cihaz testi ayrıca yapılmalıdır.
