"""GLB (Sketchfab 'Stargate') -> NeoForge OBJ models for the formed gate.
Outputs ring.obj, chevron_1..7.obj (the event horizon is drawn by StargateRenderer) in block units, centred on the 5x5 frame
(control block at origin block, ring centre at x=0.5, y=2.5, z=0.5).
The inner symbol track is cut out of ring.obj: StargateRenderer draws it, turning, with the mod's own glyphs.
The script prints the track's radii and face depth for StargateRenderer.

FR: la piste des symboles est retirée de ring.obj : StargateRenderer la dessine, en rotation, avec les glyphes du
mod. Le script affiche les rayons et la profondeur de la piste pour StargateRenderer."""
import json, struct, sys, os, math
import numpy as np
src, outdir = sys.argv[1], sys.argv[2]
d=open(src,'rb').read()
clen=struct.unpack('<I',d[12:16])[0]; j=json.loads(d[20:20+clen]); B=d[20+clen+8:]
def acc(i):
    a=j['accessors'][i]; bv=j['bufferViews'][a['bufferView']]
    n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]; dt={5126:np.float32,5125:np.uint32,5123:np.uint16,5121:np.uint8}[a['componentType']]
    off=bv.get('byteOffset',0)+a.get('byteOffset',0); stride=bv.get('byteStride'); sz=n*np.dtype(dt).itemsize
    if stride and stride!=sz:
        raw=np.frombuffer(B,np.uint8,(a['count']-1)*stride+sz,off)
        return np.stack([raw[k*stride:k*stride+sz].view(dt) for k in range(a['count'])])
    return np.frombuffer(B,dt,a['count']*n,off).reshape(a['count'],n)
def mat(n):
    if 'matrix' in n: return np.array(n['matrix'],float).reshape(4,4).T
    M=np.eye(4)
    if 'scale' in n: M=np.diag(list(n['scale'])+[1])@M
    if 'rotation' in n:
        x,y,z,w=n['rotation']; R=np.eye(4); R[:3,:3]=[[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]]; M=R@M
    if 'translation' in n: T=np.eye(4); T[:3,3]=n['translation']; M=T@M
    return M
MATNAME={0:'event_horizon',1:'gate_grey',2:'glow',3:'gate'}
parts=[]  # (matname, group, positions, normals, uvs, tris)
def walk(i,P,group):
    n=j['nodes'][i]; W=P@mat(n)
    if 'mesh' in n:
        for p in j['meshes'][n['mesh']]['primitives']:
            a=p['attributes']; pos=acc(a['POSITION']).astype(float)
            nor=acc(a['NORMAL']).astype(float) if 'NORMAL' in a else np.zeros_like(pos)
            uv=acc(a['TEXCOORD_0']).astype(float) if 'TEXCOORD_0' in a else np.full((len(pos),2),0.5)
            idx=acc(p['indices']).reshape(-1).astype(int) if 'indices' in p else np.arange(len(pos))
            wp=(W@np.c_[pos,np.ones(len(pos))].T).T[:,:3]
            N=np.linalg.inv(W[:3,:3]).T; wn=(N@nor.T).T; ln=np.linalg.norm(wn,axis=1,keepdims=True); ln[ln==0]=1; wn/=ln
            parts.append((MATNAME[p['material']], group, wp, wn, uv, idx.reshape(-1,3)))
    for ch in n.get('children',[]):
        walk(ch,W, group if group is not None else (j['nodes'][ch].get('name') if n.get('name')=='Root' else None))
for r in j['scenes'][0]['nodes']: walk(r,np.eye(4),None)

allp=np.vstack([p[2] for p in parts]); radius=np.max(np.linalg.norm(allp[:,:2],axis=1))
S=2.5/radius; OFF=np.array([0.5,2.5,0.5])
# Symbol track, in the ring mesh's own units (outer edge 0.998): recessed faces at |z| <= 0.046, r 0.840..0.899.
# Piste des symboles, dans les unités du maillage de l'anneau : faces en retrait |z| <= 0.046, r 0.840..0.899.
TRACK_R0, TRACK_R1, TRACK_Z = 0.8395, 0.9005, 0.0465
def cut_track(wp, tris):
    unit = np.max(np.linalg.norm(wp[:,:2],axis=1)) / 0.998
    r = np.linalg.norm(wp[:,:2],axis=1) / unit; z = np.abs(wp[:,2]) / unit
    on = (r >= TRACK_R0) & (r <= TRACK_R1) & (z <= TRACK_Z)
    keep = ~on[tris].all(axis=1)
    print('track: cut', int((~keep).sum()), 'triangles; block units r', round(0.840*unit*S,4), '..', round(0.899*unit*S,4),
          'face |z|', round(0.045*unit*S,4))
    return tris[keep]
# model chevron slot (40 deg steps clockwise from top) -> our chevron number; slots 4,5 (bottom) unused
SLOT={0:7,1:1,2:2,3:3,6:4,7:5,8:6}
files={}
for mname,group,wp,wn,uv,tris in parts:
    if mname=='gate' or mname=='gate_grey':
        key='ring'
        if mname=='gate': tris=cut_track(wp,tris)
    elif mname=='event_horizon': continue  # drawn animated by StargateRenderer / dessiné animé par StargateRenderer
    else:
        c=wp.mean(0); slot=round((math.degrees(math.atan2(c[0],c[1]))%360)/40)%9
        num=SLOT.get(slot); key=f'chevron_{num}' if num else 'ring'
        if not num: mname='glow'  # unused chevrons: dark glow material, baked into the ring
    files.setdefault(key,[]).append((mname,wp*S+OFF,wn,uv,tris))
os.makedirs(outdir,exist_ok=True)
for key,meshes in files.items():
    with open(os.path.join(outdir,key+'.obj'),'w',newline='\n') as f:
        f.write(f'# Generated from "Stargate" by Chaitanya Krishnan (CC-BY-4.0)\nmtllib gate.mtl\no {key}\n')
        base=0; tri=0
        for mname,p,nrm,uv,tris in meshes:
            for v in p: f.write(f'v {v[0]:.5f} {v[1]:.5f} {v[2]:.5f}\n')
            for t in uv: f.write(f'vt {t[0]:.5f} {t[1]:.5f}\n')
            for v in nrm: f.write(f'vn {v[0]:.4f} {v[1]:.4f} {v[2]:.4f}\n')
            f.write(f'usemtl {mname}\n')
            for a,b,c in tris+base+1:
                f.write(f'f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}\n')
            base+=len(p); tri+=len(tris)
    allv=np.vstack([m[1] for m in meshes])
    print(key,'triangles',tri,'bbox',np.round(allv.min(0),3),np.round(allv.max(0),3))
print('scale',round(S,4))
