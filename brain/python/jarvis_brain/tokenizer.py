"""Deterministic character tokenizer; no external tokenizer/API/model required."""
from __future__ import annotations
import json
from pathlib import Path
from typing import Iterable

class CharTokenizer:
    def __init__(self, alphabet: Iterable[str] | None = None):
        chars = sorted(set(alphabet or []))
        self.itos = ["<unk>"] + [c for c in chars if c != "<unk>"]
        self.stoi = {c: i for i, c in enumerate(self.itos)}

    @classmethod
    def fit(cls, texts: Iterable[str]) -> "CharTokenizer":
        return cls({ch for text in texts for ch in text})

    @property
    def vocab_size(self) -> int:
        return len(self.itos)

    def encode(self, text: str) -> list[int]:
        unk = self.stoi["<unk>"]
        return [self.stoi.get(ch, unk) for ch in text]

    def decode(self, ids: Iterable[int]) -> str:
        out = []
        for idx in ids:
            if 0 <= int(idx) < len(self.itos) and int(idx) != 0:
                out.append(self.itos[int(idx)])
        return "".join(out)

    def save(self, path: str | Path) -> None:
        Path(path).write_text(json.dumps({"itos": self.itos}, ensure_ascii=False, indent=2), encoding="utf-8")

    @classmethod
    def load(cls, path: str | Path) -> "CharTokenizer":
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        obj = cls()
        obj.itos = list(data["itos"])
        if not obj.itos or obj.itos[0] != "<unk>" or len(set(obj.itos)) != len(obj.itos):
            raise ValueError("Invalid tokenizer vocabulary")
        obj.stoi = {c: i for i, c in enumerate(obj.itos)}
        return obj
