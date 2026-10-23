# 💜 Aura AI

Ücretsiz, Türkçe/Azerice odaklı Android kişisel asistanı. Öncelik cihaz içinde çalışan yapay zekâdır; ücretli API anahtarı varsayılan olarak kullanılmaz.

<p align="center">
  <a href="https://github.com/mm2vault/HarikaAI-Offline/releases/latest">
    <img src="https://img.shields.io/badge/⬇%20AURA%20AI-EN%20SON%20APK-8B5CF6?style=for-the-badge&logo=android&logoColor=white" alt="Aura AI en son APK'yı indir">
  </a>
</p>

<p align="center">
  <a href="https://github.com/mm2vault/HarikaAI-Offline/releases">Tüm sürümler</a> ·
  <a href="https://github.com/mm2vault/HarikaAI-Offline/actions">Derleme durumu</a>
</p>

## 📥 Nasıl indirilir?

1. Yukarıdaki **AURA AI — EN SON APK** düğmesine dokun.
2. Açılan GitHub sürüm sayfasındaki **Assets** bölümünü aç.
3. En son sürümdeki APK dosyasını indir. Kaynak kodu ZIP dosyasını indirme.

**Güncelleme mantığı:** Düğme sabit bir eski APK adresine değil, GitHub'ın sürüm sayfasına gider. Yeni bir APK yayımlandığında kullanıcılar aynı düğmeden en son yayımlanan sürüme ulaşabilir. Yeni derlemenin bu sayfada görünmesi için APK'nın GitHub Release'e yüklenmesi gerekir; yalnızca Actions test artifact'i oluşması resmî sürüm yayımlamaz.

> **Mevcut durum:** Son arayüz/ikon değişiklikleri `brain/aura-rebuild-clean` dalında derleme testini geçti. Bu tek başına telefon üzerinde test edildiği veya en yeni APK'nın resmî Release olarak yayımlandığı anlamına gelmez. Eski uygulamanın üzerine kurmadan önce imza uyumluluğu doğrulanmalıdır.

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
3. Gradle senkronizasyonu ve `assembleDebug` çalıştır.
4. APK'yı gerçek cihazda test et; modeli indirip çıkarım yapabildiğini doğrula.

## Yeniden yapılandırma

- Görünür markayı Aura AI yapmak ve uygulama kimliğini şimdilik korumak.
- UI, model motoru, indirme, sohbet depolama, ses ve güncelleme sorumluluklarını ayırmak.
- İndirmeyi geçici dosya, doğrulama ve tekrar deneme ile güvenli yapmak.
- İmza anahtarı yoksa test APK'sını yayın sürümü diye yayımlamamak.
- Sohbet geçmişini koruma ve dışa/içe aktarma desteği eklemek.
