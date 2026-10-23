# Aura AI — Brain / Runtime

## Hedefler
- Offline-first: Qwen LiteRT-LM modeli kullanıcının cihazında çalışır.
- Varsayılan akışta ücretli API anahtarı yoktur.
- Model yoksa veya bozuksa uygulama bunu açıkça söyler; basit yedek yanıtlar gerçek AI gibi sunulmaz.
- Arayüz, model motoru, sohbet hafızası, indirme, ses ve güncelleme kodu birbirinden ayrılır.
- İsteğe bağlı çevrimiçi araçlar çevrimdışı sohbeti engellemez.

## Mevcut durum
Eski iskelette model motoru ve birçok uygulama sorumluluğu tek MainActivity.kt içinde. Model LiteRT-LM Engine ve Conversation üzerinden açılıyor. Bu henüz modüllere ayrılmış ve test edilmiş mimari değildir.

## Hedef modüller
1. ui: ekran, mesaj balonları, durum ve etkileşim.
2. brain: model motoru, prompt ve üretim yaşam döngüsü.
3. data: sürümlenmiş sohbet geçmişi ve dışa/içe aktarma.
4. model: güvenli indirme, geçici dosya, doğrulama, tekrar deneme ve iptal.
5. voice: Android konuşma tanıma ve TTS.
6. updater: release metadata, sürüm karşılaştırma ve APK indirme.
7. tools: tarih/saat, hesap makinesi ve açıkça çağrılan web araması.

## Veri koruma
- applicationId uyumluluk doğrulanmadan değiştirilmemeli.
- Android güncellemesi aynı paket kimliği ve aynı imza sertifikasını gerektirir.
- Yeni debug anahtarı mevcut kuruluma uyacak varsayımı yapılmamalı.
- Sohbet dışa aktarma eklenmeden depolama şeması yıkıcı biçimde değiştirilmemeli.
- Tamamlanmamış model dosyası geçerli kabul edilmemeli.
- Sürüm etiketi tek başına sürüm karşılaştırması için yeterli değildir; sayısal versionCode tercih edilir.

## Doğrulama
- assembleDebug ve imza yapılandırıldıysa assembleRelease.
- Paket/manifest/ikon referansları, iş akışı YAML'si ve eski marka metinleri kontrolü.
- Cihazda temiz kurulum, güncelleme, geçmişin korunması, kesintili indirme, model açılışı, ses izinleri ve APK kurulum akışı testi.
- Bu kontroller yapılana kadar her cihazda çalıştığı iddia edilmez.
