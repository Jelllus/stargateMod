"""Tiny software rasteriser: flat-shaded preview PNG of an OBJ (no external libs)."""
import sys, zlib, struct, numpy as np
obj, out, yaw, pitch = sys.argv[1], sys.argv[2], float(sys.argv[3]), float(sys.argv[4])
V=[]; F=[]; mats=[]; cur=None
COL={'dhd_stone':(150,140,130),'dhd_grill':(110,105,100),'dhd_metal':(160,160,170),'dhd_ring':(80,80,90),'dhd_key':(220,120,40),'dhd_bubble':(210,30,20)}
for line in open(obj):
    p=line.split()
    if not p: continue
    if p[0]=='v': V.append([float(x) for x in p[1:4]])
    elif p[0]=='usemtl': cur=p[1]
    elif p[0]=='f': F.append([int(x.split('/')[0])-1 for x in p[1:4]]); mats.append(cur)
V=np.array(V); F=np.array(F)
c=V.mean(0); V=V-c
cy,sy=np.cos(np.radians(yaw)),np.sin(np.radians(yaw)); cp,sp=np.cos(np.radians(pitch)),np.sin(np.radians(pitch))
R=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]); P=np.array([[1,0,0],[0,cp,-sp],[0,sp,cp]])
W=V@R.T@P.T
Wd=H=600; s=380
X=(W[:,0]*s+Wd/2); Y=(H/2-W[:,1]*s); Z=W[:,2]
img=np.full((H,Wd,3),235,np.uint8); zb=np.full((H,Wd),-1e9)
light=np.array([0.3,0.6,0.75]); light/=np.linalg.norm(light)
for t,m in zip(F,mats):
    a,b,cc=W[t]; n=np.cross(b-a,cc-a); ln=np.linalg.norm(n)
    if ln==0: continue
    n/=ln; sh=0.35+0.65*abs(n@light)
    col=np.array(COL.get(m,(255,0,255)))*sh
    xs,ys,zs=X[t],Y[t],Z[t]
    x0,x1=int(max(0,xs.min())),int(min(Wd-1,xs.max())+1); y0,y1=int(max(0,ys.min())),int(min(H-1,ys.max())+1)
    if x1<=x0 or y1<=y0: continue
    gx,gy=np.meshgrid(np.arange(x0,x1)+.5,np.arange(y0,y1)+.5)
    d=(ys[1]-ys[2])*(xs[0]-xs[2])+(xs[2]-xs[1])*(ys[0]-ys[2])
    if abs(d)<1e-9: continue
    l1=((ys[1]-ys[2])*(gx-xs[2])+(xs[2]-xs[1])*(gy-ys[2]))/d; l2=((ys[2]-ys[0])*(gx-xs[2])+(xs[0]-xs[2])*(gy-ys[2]))/d; l3=1-l1-l2
    msk=(l1>=0)&(l2>=0)&(l3>=0)
    z=l1*zs[0]+l2*zs[1]+l3*zs[2]
    sub=zb[y0:y1,x0:x1]; upd=msk&(z>sub)
    sub[upd]=z[upd]; img[y0:y1,x0:x1][upd]=col.astype(np.uint8)
raw=b''.join(b'\x00'+img[r].tobytes() for r in range(H))
def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
open(out,'wb').write(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',Wd,H,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,6))+chunk(b'IEND',b''))
print(out)
