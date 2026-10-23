"""Train TinyGPT from random initialization on a UTF-8 text corpus."""
import argparse,json
from pathlib import Path
import torch
from jarvis_brain.tokenizer import CharTokenizer
from jarvis_brain.transformer import TinyGPT

def main():
 p=argparse.ArgumentParser(); p.add_argument("--data",required=True); p.add_argument("--out",default="artifacts/jarvis-gpt")
 p.add_argument("--steps",type=int,default=500); p.add_argument("--context",type=int,default=128); p.add_argument("--width",type=int,default=128)
 p.add_argument("--heads",type=int,default=4); p.add_argument("--layers",type=int,default=4); p.add_argument("--batch",type=int,default=16); p.add_argument("--lr",type=float,default=3e-4)
 a=p.parse_args()
 if min(a.steps,a.context,a.width,a.heads,a.layers,a.batch)<1: p.error("dimensions and steps must be positive")
 if a.width%a.heads: p.error("--width must be divisible by --heads")
 corpus=Path(a.data).read_text(encoding="utf-8")
 if len(corpus)<a.context+2: p.error("corpus is too short for selected context")
 tok=CharTokenizer.fit([corpus]); ids=torch.tensor(tok.encode(corpus),dtype=torch.long)
 device=torch.device("cuda" if torch.cuda.is_available() else "cpu")
 model=TinyGPT(tok.vocab_size,a.context,a.width,a.heads,a.layers).to(device); opt=torch.optim.AdamW(model.parameters(),lr=a.lr)
 gen=torch.Generator().manual_seed(123)
 for step in range(1,a.steps+1):
  starts=torch.randint(0,len(ids)-a.context-1,(a.batch,),generator=gen)
  x=torch.stack([ids[i:i+a.context] for i in starts]).to(device)
  y=torch.stack([ids[i+1:i+a.context+1] for i in starts]).to(device)
  model.train(); _,loss=model(x,y); opt.zero_grad(set_to_none=True); loss.backward(); torch.nn.utils.clip_grad_norm_(model.parameters(),1.0); opt.step()
  if step==1 or step%max(1,a.steps//10)==0: print(f"step={step}/{a.steps} loss={loss.item():.4f} device={device}")
 out=Path(a.out); out.mkdir(parents=True,exist_ok=True); tok.save(out/"tokenizer.json")
 config={"vocab_size":tok.vocab_size,"context_size":a.context,"width":a.width,"heads":a.heads,"layers":a.layers}
 torch.save({"config":config,"state_dict":model.cpu().state_dict()},out/"model.pt")
 (out/"config.json").write_text(json.dumps(config,indent=2),encoding="utf-8")
 print("Saved TinyGPT. The demo corpus only smoke-tests training; it is not enough for a capable assistant.")
if __name__=="__main__": main()
