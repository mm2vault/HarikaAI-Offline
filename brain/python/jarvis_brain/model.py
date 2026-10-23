"""Tiny character-level next-token MLP implemented directly with NumPy.
Educational baseline, not a capable general-purpose assistant.
"""
from __future__ import annotations
import numpy as np

class TinyLanguageModel:
    def __init__(self, vocab_size: int, context_size: int = 16, hidden_size: int = 64, seed: int = 7):
        if vocab_size < 2 or context_size < 1 or hidden_size < 1:
            raise ValueError("vocab_size >= 2, context_size and hidden_size must be positive")
        self.vocab_size, self.context_size, self.hidden_size = vocab_size, context_size, hidden_size
        rng = np.random.default_rng(seed)
        scale = 0.08
        self.W1 = rng.normal(0, scale, (context_size * vocab_size, hidden_size)).astype(np.float32)
        self.b1 = np.zeros(hidden_size, dtype=np.float32)
        self.W2 = rng.normal(0, scale, (hidden_size, vocab_size)).astype(np.float32)
        self.b2 = np.zeros(vocab_size, dtype=np.float32)

    def _one_hot(self, contexts: np.ndarray) -> np.ndarray:
        n = len(contexts)
        x = np.zeros((n, self.context_size, self.vocab_size), dtype=np.float32)
        x[np.arange(n)[:, None], np.arange(self.context_size)[None, :], contexts] = 1.0
        return x.reshape(n, -1)

    def _forward(self, contexts: np.ndarray):
        x = self._one_hot(contexts)
        h = np.tanh(x @ self.W1 + self.b1)
        logits = h @ self.W2 + self.b2
        logits -= logits.max(axis=1, keepdims=True)
        exp = np.exp(logits)
        return x, h, exp / exp.sum(axis=1, keepdims=True)

    def train_step(self, contexts: np.ndarray, targets: np.ndarray, learning_rate: float = 0.08) -> float:
        if len(contexts) == 0 or len(contexts) != len(targets):
            raise ValueError("contexts and targets must be non-empty and have equal length")
        x, h, probs = self._forward(contexts)
        n = len(targets)
        loss = -np.log(np.clip(probs[np.arange(n), targets], 1e-9, 1.0)).mean()
        dlogits = probs.copy()
        dlogits[np.arange(n), targets] -= 1.0
        dlogits /= n
        dW2 = h.T @ dlogits; db2 = dlogits.sum(axis=0)
        dh = dlogits @ self.W2.T
        dpre = dh * (1.0 - h * h)
        dW1 = x.T @ dpre; db1 = dpre.sum(axis=0)
        for param, grad in ((self.W1,dW1),(self.b1,db1),(self.W2,dW2),(self.b2,db2)):
            param -= learning_rate * grad
        return float(loss)

    def next_token(self, context: list[int], temperature: float = 0.0, rng=None) -> int:
        padded = ([0] * self.context_size + context)[-self.context_size:]
        _, _, probs = self._forward(np.asarray([padded], dtype=np.int64))
        p = probs[0]
        if temperature <= 0: return int(np.argmax(p))
        logits = np.log(np.clip(p, 1e-9, 1.0)) / temperature
        logits -= logits.max(); p = np.exp(logits); p /= p.sum()
        return int((rng or np.random.default_rng()).choice(self.vocab_size, p=p))

    def generate(self, prompt_ids: list[int], max_new_tokens: int = 100, temperature: float = 0.8, seed: int = 1) -> list[int]:
        if max_new_tokens < 0: raise ValueError("max_new_tokens must not be negative")
        out = list(prompt_ids); rng = np.random.default_rng(seed)
        for _ in range(max_new_tokens): out.append(self.next_token(out, temperature, rng))
        return out

    def save(self, path: str) -> None:
        np.savez(path, vocab_size=self.vocab_size, context_size=self.context_size, hidden_size=self.hidden_size,
                 W1=self.W1, b1=self.b1, W2=self.W2, b2=self.b2)

    @classmethod
    def load(cls, path: str) -> "TinyLanguageModel":
        with np.load(path) as d:
            model = cls(int(d["vocab_size"]), int(d["context_size"]), int(d["hidden_size"]))
            for name in ("W1", "b1", "W2", "b2"):
                value = d[name]
                if value.shape != getattr(model, name).shape: raise ValueError(f"Invalid shape for {name}")
                setattr(model, name, value.astype(np.float32))
            return model
