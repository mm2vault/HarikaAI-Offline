import unittest

import torch

from jarvis_brain.transformer import TinyGPT


class TinyGPTTests(unittest.TestCase):
    def test_forward_loss_backward(self):
        model = TinyGPT(19, context_size=8, width=16, heads=4, layers=2)
        tokens = torch.randint(0, 19, (2, 8))
        targets = torch.randint(0, 19, (2, 8))
        logits, loss = model(tokens, targets)
        self.assertEqual(tuple(logits.shape), (2, 8, 19))
        self.assertTrue(torch.isfinite(loss).item())
        loss.backward()
        self.assertIsNotNone(model.head.weight.grad)

    def test_generate_and_context_limit(self):
        model = TinyGPT(11, context_size=4, width=8, heads=2, layers=1)
        output = model.generate(torch.tensor([[1, 2]]), max_new_tokens=3, temperature=0)
        self.assertEqual(tuple(output.shape), (1, 5))
        with self.assertRaises(ValueError):
            model(torch.ones((1, 5), dtype=torch.long))

    def test_invalid_dimensions_and_inputs(self):
        with self.assertRaises(ValueError):
            TinyGPT(10, width=7, heads=2)
        model = TinyGPT(10, context_size=4, width=8, heads=2, layers=1)
        with self.assertRaises(ValueError):
            model(torch.tensor([[1, 2]]), torch.tensor([[1]]))
        with self.assertRaises(ValueError):
            model(torch.tensor([[10]]))
        with self.assertRaises(ValueError):
            model.generate(torch.empty((1, 0), dtype=torch.long))

    def test_generate_restores_training_mode(self):
        model = TinyGPT(10, context_size=4, width=8, heads=2, layers=1)
        model.train()
        model.generate(torch.tensor([[1, 2]]), max_new_tokens=1, temperature=0)
        self.assertTrue(model.training)


if __name__ == "__main__":
    unittest.main()
