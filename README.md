# JARVİS AI

Ücretsiz, Türkçe odaklı Android asistanı. Kotlin ile geliştirilir; yerel yapay zekâ modeli APK içine gömülmez, uygulama içinde indirilir.

## Özellikler
- Siyah/mor neon arayüz
- LiteRT-LM ile cihaz içi Qwen3-0.6B
- Türkçe konuşma tanıma ve sesli yanıt (Android cihaz desteğine bağlı)
- Sohbet geçmişini cihazda saklama
- Tarayıcıda internet araması
- GitHub Actions ile debug APK derleme

## Model
Yaklaşık 586 MB boyutlu model: https://huggingface.co/litert-community/Qwen3-0.6B

## APK derleme
GitHub'da Actions sekmesini açıp Build JARVIS AI APK iş akışını çalıştır. Başarılı olursa jarvis-ai-debug-apk artifact'ini indir.

> Bu ilk iskelettir. Gerçek cihazda derleme ve model çıkarımı doğrulanmadan üretime hazır olduğu iddia edilmez.
