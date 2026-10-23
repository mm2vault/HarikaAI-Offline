# Etap 2 — sıfırdan TinyGPT

Bu aşamada hazır ağırlık kullanılmadan rastgele başlatılan decoder-only Transformer yazıldı: token ve konum gömmeleri, nedensel çok başlı öz-dikkat, residual bağlantılar, LayerNorm, MLP, çapraz entropi ve AdamW eğitim döngüsü.

Python 3.10+ ve PyTorch gerekir. Ücretsiz notebook ortamında GPU varsa otomatik kullanılır, yoksa CPU kullanılır:

    python -m pip install -r brain/python/requirements-transformer.txt
    PYTHONPATH=brain/python python -m unittest brain/python/tests/test_transformer.py -v
    PYTHONPATH=brain/python python brain/python/train_transformer.py --data brain/python/data/tiny_tr.txt --steps 500 --context 64 --width 64 --heads 4 --layers 2

Bu küçük örnek veri yalnızca eğitim hattını denemek içindir; anlamlı bir sohbet modeli eğitmeye yetmez. Sonraki işler: lisansı uygun daha büyük Türkçe veri, değerlendirme, checkpoint/resume, daha iyi tokenizer, model sıkıştırma ve Android çalışma zamanı. Model henüz Android'e bağlanmamıştır.
