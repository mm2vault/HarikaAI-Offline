"""Small decoder-only Transformer trained from random initialization.

This is an educational baseline, not a pretrained or production-ready assistant.
"""
from __future__ import annotations

import torch
from torch import nn
from torch.nn import functional as F


class CausalSelfAttention(nn.Module):
    def __init__(self, width: int, heads: int, context_size: int, dropout: float = 0.0):
        super().__init__()
        if width < 1 or heads < 1 or width % heads:
            raise ValueError("width must be positive and divisible by heads")
        if context_size < 1:
            raise ValueError("context_size must be positive")
        if not 0.0 <= dropout < 1.0:
            raise ValueError("dropout must be in [0, 1)")

        self.heads = heads
        self.head_width = width // heads
        self.qkv = nn.Linear(width, 3 * width)
        self.proj = nn.Linear(width, width)
        self.dropout = nn.Dropout(dropout)
        self.register_buffer(
            "mask", torch.tril(torch.ones(context_size, context_size, dtype=torch.bool))
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        batch, steps, width = x.shape
        if steps > self.mask.shape[0]:
            raise ValueError("sequence exceeds attention context_size")
        q, k, v = self.qkv(x).chunk(3, dim=-1)

        def split_heads(value: torch.Tensor) -> torch.Tensor:
            return value.view(batch, steps, self.heads, self.head_width).transpose(1, 2)

        q, k, v = split_heads(q), split_heads(k), split_heads(v)
        scores = (q @ k.transpose(-2, -1)) * (self.head_width ** -0.5)
        scores = scores.masked_fill(~self.mask[:steps, :steps], torch.finfo(scores.dtype).min)
        weights = self.dropout(F.softmax(scores, dim=-1))
        output = (weights @ v).transpose(1, 2).contiguous().view(batch, steps, width)
        return self.dropout(self.proj(output))


class Block(nn.Module):
    def __init__(self, width: int, heads: int, context_size: int, dropout: float):
        super().__init__()
        self.norm1 = nn.LayerNorm(width)
        self.attention = CausalSelfAttention(width, heads, context_size, dropout)
        self.norm2 = nn.LayerNorm(width)
        self.mlp = nn.Sequential(
            nn.Linear(width, 4 * width),
            nn.GELU(),
            nn.Linear(4 * width, width),
            nn.Dropout(dropout),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = x + self.attention(self.norm1(x))
        return x + self.mlp(self.norm2(x))


class TinyGPT(nn.Module):
    """Minimal GPT-like language model with randomly initialized weights."""

    def __init__(
        self,
        vocab_size: int,
        context_size: int = 128,
        width: int = 128,
        heads: int = 4,
        layers: int = 4,
        dropout: float = 0.0,
    ):
        super().__init__()
        if vocab_size < 2 or context_size < 1 or width < 1 or layers < 1 or heads < 1:
            raise ValueError("vocab_size must be >= 2; other dimensions must be positive")
        if width % heads:
            raise ValueError("width must be divisible by heads")
        if not 0.0 <= dropout < 1.0:
            raise ValueError("dropout must be in [0, 1)")

        self.vocab_size = vocab_size
        self.context_size = context_size
        self.token = nn.Embedding(vocab_size, width)
        self.position = nn.Embedding(context_size, width)
        self.blocks = nn.ModuleList(
            [Block(width, heads, context_size, dropout) for _ in range(layers)]
        )
        self.norm = nn.LayerNorm(width)
        self.head = nn.Linear(width, vocab_size, bias=False)
        self.apply(self._init_weights)

    @staticmethod
    def _init_weights(module: nn.Module) -> None:
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                nn.init.zeros_(module.bias)
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def forward(
        self, tokens: torch.Tensor, targets: torch.Tensor | None = None
    ) -> tuple[torch.Tensor, torch.Tensor | None]:
        if tokens.ndim != 2:
            raise ValueError("tokens must have shape [batch, sequence]")
        if tokens.shape[0] < 1 or tokens.shape[1] < 1:
            raise ValueError("tokens must have a non-empty batch and sequence")
        if tokens.shape[1] > self.context_size:
            raise ValueError("sequence exceeds context_size")
        if torch.any(tokens < 0) or torch.any(tokens >= self.vocab_size):
            raise ValueError("token id is outside the vocabulary")
        if targets is not None:
            if targets.shape != tokens.shape:
                raise ValueError("targets must have the same shape as tokens")
            if torch.any(targets < 0) or torch.any(targets >= self.vocab_size):
                raise ValueError("target id is outside the vocabulary")

        positions = torch.arange(tokens.shape[1], device=tokens.device)
        x = self.token(tokens) + self.position(positions)[None, :, :]
        for block in self.blocks:
            x = block(x)
        logits = self.head(self.norm(x))
        loss = None
        if targets is not None:
            loss = F.cross_entropy(logits.reshape(-1, self.vocab_size), targets.reshape(-1))
        return logits, loss

    @torch.no_grad()
    def generate(
        self, tokens: torch.Tensor, max_new_tokens: int = 100, temperature: float = 0.8
    ) -> torch.Tensor:
        if tokens.ndim != 2 or tokens.shape[0] < 1 or tokens.shape[1] < 1:
            raise ValueError("tokens must have shape [non-empty batch, non-empty sequence]")
        if torch.any(tokens < 0) or torch.any(tokens >= self.vocab_size):
            raise ValueError("token id is outside the vocabulary")
        if max_new_tokens < 0 or temperature < 0:
            raise ValueError("max_new_tokens and temperature must be non-negative")

        was_training = self.training
        self.eval()
        try:
            for _ in range(max_new_tokens):
                logits, _ = self(tokens[:, -self.context_size :])
                last_logits = logits[:, -1, :]
                next_token = (
                    last_logits.argmax(dim=-1, keepdim=True)
                    if temperature == 0
                    else torch.multinomial(torch.softmax(last_logits / temperature, dim=-1), 1)
                )
                tokens = torch.cat((tokens, next_token), dim=1)
            return tokens
        finally:
            self.train(was_training)
