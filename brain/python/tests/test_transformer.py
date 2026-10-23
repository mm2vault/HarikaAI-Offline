import unittest
import torch
from jarvis_brain.transformer import TinyGPT
class TinyGPTTests(unittest.TestCase):
 def test_forward_loss_backward(self):
  model=TinyGPT(19,8,16,4,2); x=torch.randint(0,19,(2,8)); y=torch.randint(0,19,(2,8))
  logits,loss=model(x,y); self.assertEqual(tuple(logits.shape),(2,8,19)); self.assertTrue(torch.isfinite(loss).item())
  loss.backward(); self.assertIsNotNone(model.head.weight.grad)
 def test_generate_and_context_limit(self):
  model=TinyGPT(11,4,8,2,1); out=model.generate(torch.tensor([[1,2]]),3,0)
  self.assertEqual(tuple(out.shape),(1,5))
  with self.assertRaises(ValueError): model(torch.ones((1,5),dtype=torch.long))
if __name__=="__main__": unittest.main()
