# Aura AI

Ücretsiz, Türkçe/Azerice odaklı Android kişisel asistanı. Öncelik cihaz içinde çalışan yapay zekâdır; ücretli API anahtarı varsayılan olarak kullanılmaz.

## Temel özellikler
- Koyu mor arayüz ve Aura AI markası
- LiteRT-LM üzerinden cihazda Qwen3-0.6B çalıştırma denemesi
- Model dosyasını uygulama içinden indirme (yaklaşık 586 MB)
- Cihaz destekliyorsa konuşmayı yazıya çevirme ve Türkçe sesli yanıt
- Sohbet geçmişini cihazda saklama
- GitHub Releases üzerinden güncelleme kontrolü

## Önemli sınırlar
- Model indirilmeden önceki hafif mod, genel amaçlı dil modeli değildir.
- APK derlemesinin başarılı olması modelin her telefonda çalıştığını kanıtlamaz; gerçek cihaz testi gerekir.
- Mevcut kurulumun üzerine güncelleme için aynı applicationId ve aynı imza sertifikası gerekir. Bu dal ana dala veya mevcut yayınlara otomatik aktarılmaz.
- Sohbet geçmişini silmeden önce dışa aktarma/yedekleme yolu hazırlanmalıdır.

## Geliştirme
1. Android Studio ile projeyi aç.
2. JDK 17 ve Android SDK platform 35 kullan.
3. Gradle senkronizasyonu ve assembleDebug çalıştır.
4. APK'yı gerçek cihazda test et; modeli indirip çıkarım yapabildiğini doğrula.

## Yeniden yapılandırma
- Görünür markayı Aura AI yapmak ve uygulama kimliğini şimdilik korumak.
- UI, model motoru, indirme, sohbet depolama, ses ve güncelleme sorumluluklarını ayırmak.
- İndirmeyi geçici dosya, doğrulama ve tekrar deneme ile güvenli yapmak.
- İmza anahtarı yoksa test APK'sını yayın sürümü diye yayımlamamak.
- Sohbet geçmişini koruma ve dışa/içe aktarma desteği eklemek.
