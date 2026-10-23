import tempfile
import unittest
from pathlib import Path
import numpy as np
from jarvis_brain.tokenizer import CharTokenizer
from jarvis_brain.model import TinyLanguageModel
from train import make_examples

class TokenizerTests(unittest.TestCase):
    def test_round_trip_turkish(self):
        text = "Merhaba, JARVİS! şğıöçü"
        tok = CharTokenizer.fit([text])
        self.assertEqual(tok.decode(tok.encode(text)), text)

    def test_unknown_character(self):
        tok = CharTokenizer.fit(["abc"])
        self.assertEqual(tok.encode("z"), [0])

    def test_save_load(self):
        tok = CharTokenizer.fit(["selam"])
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "vocab.json"; tok.save(path)
            self.assertEqual(CharTokenizer.load(path).decode(tok.encode("selam")), "selam")

class ModelTests(unittest.TestCase):
    def test_train_generate_and_reload(self):
        tok = CharTokenizer.fit(["merhaba jarvis merhaba"])
        x, y = make_examples(tok.encode("merhaba jarvis merhaba"), 4)
        model = TinyLanguageModel(tok.vocab_size, 4, 12)
        for _ in range(10): model.train_step(x, y, 0.05)
        self.assertEqual(len(model.generate(tok.encode("mer"), 5)), 8)
        with tempfile.TemporaryDirectory() as tmp:
            path = str(Path(tmp) / "model.npz"); model.save(path)
            loaded = TinyLanguageModel.load(path)
            self.assertEqual(loaded.vocab_size, tok.vocab_size)
            self.assertTrue(np.isfinite(loaded._forward(x)[2]).all())

if __name__ == "__main__": unittest.main()
