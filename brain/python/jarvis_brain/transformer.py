"""Small decoder-only Transformer trained from random initialization; no pretrained weights."""
import torch
from torch import nn
from torch.nn import functional as F

class CausalSelfAttention(nn.Module):
 def __init__(self,width,heads,context_size,dropout=0.0):
  super().__init__()
  if width % heads: raise ValueError("width must be divisible by heads")
  self.heads=heads; self.head_width=width//heads
  self.qkv=nn.Linear(width,3*width); self.proj=nn.Linear(width,width); self.dropout=nn.Dropout(dropout)
  self.register_buffer("mask",torch.tril(torch.ones(context_size,context_size,dtype=torch.bool)))
 def forward(self,x):
  b,t,w=x.shape; q,k,v=self.qkv(x).chunk(3,dim=-1)
  def split(z): return z.view(b,t,self.heads,self.head_width).transpose(1,2)
  q,k,v=split(q),split(k),split(v)
  scores=(q@k.transpose(-2,-1))*(self.head_width**-0.5)
  scores=scores.masked_fill(~self.mask[:t,:t],torch.finfo(scores.dtype).min)
  a=self.dropout(F.softmax(scores,dim=-1))
  y=(a@v).transpose(1,2).contiguous().view(b,t,w)
  return self.dropout(self.proj(y))

class Block(nn.Module):
 def __init__(self,width,heads,context_size,dropout):
  super().__init__(); self.n1=nn.LayerNorm(width); self.attn=CausalSelfAttention(width,heads,context_size,dropout)
  self.n2=nn.LayerNorm(width); self.mlp=nn.Sequential(nn.Linear(width,4*width),nn.GELU(),nn.Linear(4*width,width),nn.Dropout(dropout))
 def forward(self,x):
  x=x+self.attn(self.n1(x)); return x+self.mlp(self.n2(x))

class TinyGPT(nn.Module):
 def __init__(self,vocab_size,context_size=128,width=128,heads=4,layers=4,dropout=0.0):
  super().__init__()
  if vocab_size<2 or context_size<1 or width<1 or layers<1: raise ValueError("invalid model dimensions")
  if width%heads: raise ValueError("width must be divisible by heads")
  self.vocab_size=vocab_size; self.context_size=context_size
  self.token=nn.Embedding(vocab_size,width); self.position=nn.Embedding(context_size,width)
  self.blocks=nn.ModuleList([Block(width,heads,context_size,dropout) for _ in range(layers)])
  self.norm=nn.LayerNorm(width); self.head=nn.Linear(width,vocab_size,bias=False); self.apply(self._init)
 @staticmethod
 def _init(m):
  if isinstance(m,nn.Linear):
   nn.init.normal_(m.weight,mean=0.0,std=0.02)
   if m.bias is not None: nn.init.zeros_(m.bias)
  elif isinstance(m,nn.Embedding): nn.init.normal_(m.weight,mean=0.0,std=0.02)
 def forward(self,tokens,targets=None):
  if tokens.ndim!=2: raise ValueError("tokens must have shape [batch, sequence]")
  if tokens.shape[1]>self.context_size: raise ValueError("sequence exceeds context_size")
  pos=torch.arange(tokens.shape[1],device=tokens.device)
  x=self.token(tokens)+self.position(pos)[None,:,:]
  for block in self.blocks: x=block(x)
  logits=self.head(self.norm(x))
  loss=F.cross_entropy(logits.reshape(-1,self.vocab_size),targets.reshape(-1)) if targets is not None else None
  return logits,loss
 @torch.no_grad()
 def generate(self,tokens,max_new_tokens=100,temperature=0.8):
  if max_new_tokens<0 or temperature<0: raise ValueError("invalid generation arguments")
  self.eval()
  for _ in range(max_new_tokens):
   logits,_=self(tokens[:,-self.context_size:]); last=logits[:,-1,:]
   nxt=last.argmax(-1,keepdim=True) if temperature==0 else torch.multinomial(torch.softmax(last/temperature,dim=-1),1)
   tokens=torch.cat((tokens,nxt),dim=1)
  return tokens
