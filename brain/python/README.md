# JARVİS Brain — Etap 1: sıfırdan temel

Bu klasör API veya hazır model kullanmadan yazılmış deneysel bir başlangıçtır.

- jarvis_brain/tokenizer.py: Türkçe karakterleri koruyan karakter tokenizer'ı.
- jarvis_brain/model.py: NumPy ile sıfırdan yazılmış küçük, tek gizli katmanlı next-character sinir ağı; ileri yayılım, softmax, çapraz entropi ve geri yayılım içerir.
- train.py: küçük örnek veri üzerinde eğitim ve tokenizer.json + model.npz çıktısı.
- tests/: tokenizer, eğitim, üretim ve kayıt/yükleme kontrolleri.

## Çalıştırma

Python 3.10+ ve NumPy gerekir:

    python -m pip install -r requirements.txt
    PYTHONPATH=. python -m unittest discover -s tests -v
    PYTHONPATH=. python train.py --steps 1000 --out artifacts/jarvis-tiny

Ücretsiz Colab/Kaggle notebook ortamında da aynı komutlar çalıştırılabilir; bu proje ücretli kaynak, API anahtarı veya ağ servisi çağırmaz. Eğitim küçük bir demo korpusu kullanır ve sonuç genel amaçlı akıllı sohbet kalitesinde değildir; kısa, tekrarlı karakter çıktıları üretmesi normaldir. Gerçek yetenek için sonraki etaplarda izinli ve yeterli Türkçe veri, daha iyi bir tokenizer, Transformer mimarisi, değerlendirme ve quantization gerekir. Bu aşamadaki model Android'e entegre edilmeye hazır değildir; Kotlin çalışma zamanı ayrı bir etapta geliştirilecektir.
