"""Train the from-scratch baseline locally or in a free notebook runtime."""
from __future__ import annotations
import argparse
from pathlib import Path
import numpy as np
from jarvis_brain.tokenizer import CharTokenizer
from jarvis_brain.model import TinyLanguageModel

SEED_TEXT = """
Kullanıcı: Merhaba JARVİS
JARVİS: Merhaba! Sana nasıl yardımcı olabilirim?
Kullanıcı: Sen kimsin?
JARVİS: Ben JARVİS, deneysel bir yapay zekâ projesiyim.
Kullanıcı: Teşekkür ederim
JARVİS: Rica ederim, yardımcı olabildiysem ne mutlu.
Kullanıcı: Yapay zekâ nedir?
JARVİS: Yapay zekâ, verilerden örüntüler öğrenen sistemler alanıdır.
Kullanıcı: Günaydın
JARVİS: Günaydın! Bugün ne yapmak istersin?
Kullanıcı: Bana Python kodu yaz
JARVİS: Önce ne yapmak istediğini netleştirelim, sonra kodu adım adım oluşturalım.
Kullanıcı: Bilmiyorsan söyle
JARVİS: Emin olmadığım konularda bunu açıkça söylerim.
""".strip()

def make_examples(ids: list[int], context_size: int):
    contexts, targets = [], []
    padded = [0] * context_size + ids
    for i in range(context_size, len(padded)):
        contexts.append(padded[i-context_size:i]); targets.append(padded[i])
    return np.asarray(contexts, dtype=np.int64), np.asarray(targets, dtype=np.int64)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--steps", type=int, default=1000)
    parser.add_argument("--context", type=int, default=16)
    parser.add_argument("--hidden", type=int, default=64)
    parser.add_argument("--out", default="artifacts/jarvis-tiny")
    args = parser.parse_args()
    if args.steps < 1: parser.error("--steps must be >= 1")
    out = Path(args.out); out.mkdir(parents=True, exist_ok=True)
    tokenizer = CharTokenizer.fit([SEED_TEXT]); ids = tokenizer.encode(SEED_TEXT)
    x, y = make_examples(ids, args.context)
    model = TinyLanguageModel(tokenizer.vocab_size, args.context, args.hidden)
    rng = np.random.default_rng(123)
    for step in range(1, args.steps + 1):
        indices = rng.integers(0, len(y), size=min(128, len(y)))
        loss = model.train_step(x[indices], y[indices])
        if step == 1 or step % max(1, args.steps // 10) == 0:
            print(f"step={step}/{args.steps} loss={loss:.4f}")
    tokenizer.save(out / "tokenizer.json"); model.save(str(out / "model.npz"))
    sample = tokenizer.decode(model.generate(tokenizer.encode("JARVİS:"), 100, temperature=0.7))
    print("Sample (quality is expected to be limited):", sample)
    print("Saved local model to", out.resolve())

if __name__ == "__main__": main()
