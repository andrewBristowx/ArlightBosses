#!/usr/bin/env python3
"""Generate deterministic 64x64 medal/pedestal textures without external packages."""
from __future__ import annotations
import binascii, struct, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/arlightbosses/textures"


def png_write(path: Path, pixels, width=64, height=64):
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels[y][x])
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", binascii.crc32(kind + data) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    data += chunk(b"IEND", b"")
    path.write_bytes(data)


def canvas(color=(0,0,0,0)):
    return [[tuple(color) for _ in range(64)] for _ in range(64)]


def rect(p, x0,y0,x1,y1,c):
    for y in range(max(0,y0), min(64,y1)):
        for x in range(max(0,x0), min(64,x1)):
            p[y][x] = c


def line(p, x0,y0,x1,y1,c,w=1):
    dx=abs(x1-x0); sx=1 if x0<x1 else -1
    dy=-abs(y1-y0); sy=1 if y0<y1 else -1
    err=dx+dy
    while True:
        rect(p,x0-w//2,y0-w//2,x0+(w+1)//2,y0+(w+1)//2,c)
        if x0==x1 and y0==y1: break
        e2=2*err
        if e2>=dy: err+=dy; x0+=sx
        if e2<=dx: err+=dx; y0+=sy


def circle(p,cx,cy,r,c,fill=True):
    for y in range(cy-r, cy+r+1):
        for x in range(cx-r, cx+r+1):
            d=(x-cx)*(x-cx)+(y-cy)*(y-cy)
            if (fill and d<=r*r) or (not fill and (r-1)*(r-1)<=d<=r*r):
                if 0<=x<64 and 0<=y<64: p[y][x]=c


def shade(c, delta):
    return tuple(max(0,min(255,v+delta)) for v in c[:3])+(c[3],)


def medal_texture(path: Path, metal, dark, light, accent, emblem):
    p=canvas()
    rect(p,0,0,64,64,dark)
    for y in range(64):
        for x in range(64):
            n=((x*13+y*7+x*y*3)%17)-8
            base=metal if ((x//4+y//4)&1)==0 else shade(metal,-8)
            p[y][x]=shade(base,n//3)
    for ox,oy in [(2,2),(18,2),(34,2),(2,22),(22,22),(42,22)]:
        circle(p,ox+7,oy+7,7,dark,True)
        circle(p,ox+7,oy+7,6,light,True)
        circle(p,ox+7,oy+7,5,metal,True)
        circle(p,ox+7,oy+7,4,accent,True)
        if emblem=="leaf":
            line(p,ox+7,oy+3,ox+7,oy+11,dark,1)
            line(p,ox+7,oy+6,ox+4,oy+4,light,1)
            line(p,ox+7,oy+8,ox+10,oy+6,light,1)
        elif emblem=="scales":
            line(p,ox+7,oy+3,ox+7,oy+11,dark,1)
            line(p,ox+3,oy+6,ox+11,oy+6,dark,1)
            line(p,ox+3,oy+6,ox+2,oy+9,light,1)
            line(p,ox+11,oy+6,ox+12,oy+9,light,1)
        else:
            line(p,ox+4,oy+3,ox+10,oy+3,light,1)
            line(p,ox+4,oy+3,ox+4,oy+8,light,1)
            line(p,ox+10,oy+3,ox+10,oy+8,dark,1)
            line(p,ox+4,oy+8,ox+7,oy+11,light,1)
            line(p,ox+10,oy+8,ox+7,oy+11,dark,1)
    rect(p,0,48,16,64,accent)
    for i in range(0,16,3): line(p,i,48,i+8,63,light,1)
    png_write(path,p)


def pedestal_texture(path: Path, stone, stone_dark, trim, glow, emblem):
    p=canvas(stone)
    for y in range(64):
        for x in range(64):
            mortar = (x%8==0) or (y%8==0)
            if mortar:
                p[y][x]=stone_dark
            else:
                n=((x*11+y*5+x*y)%13)-6
                p[y][x]=shade(stone,n//2)
    rect(p,0,32,64,40,trim)
    for x in range(0,64,4): rect(p,x,32,x+2,40,shade(trim,18))
    rect(p,0,40,64,48,glow)
    for x in range(0,64,6): rect(p,x,40,x+2,48,shade(glow,28))
    rect(p,0,48,64,64,stone_dark)
    for ox in (1,17,33,49):
        circle(p,ox+7,55,7,stone_dark,True)
        circle(p,ox+7,55,6,shade(trim,25),True)
        circle(p,ox+7,55,5,trim,True)
        circle(p,ox+7,55,3,glow,True)
        if emblem=="leaf":
            line(p,ox+7,51,ox+7,59,stone_dark,1)
            line(p,ox+7,54,ox+4,52,shade(glow,30),1)
            line(p,ox+7,56,ox+10,54,shade(glow,30),1)
        elif emblem=="scales":
            line(p,ox+7,51,ox+7,59,stone_dark,1)
            line(p,ox+3,54,ox+11,54,stone_dark,1)
            line(p,ox+3,54,ox+2,58,shade(glow,30),1)
            line(p,ox+11,54,ox+12,58,shade(glow,30),1)
        else:
            line(p,ox+4,51,ox+10,51,shade(glow,30),1)
            line(p,ox+4,51,ox+4,56,shade(glow,30),1)
            line(p,ox+10,51,ox+10,56,stone_dark,1)
            line(p,ox+4,56,ox+7,59,shade(glow,30),1)
            line(p,ox+10,56,ox+7,59,stone_dark,1)
    png_write(path,p)


medal_texture(ASSETS/"item/mossbound_home_medal.png", (92,126,54,255),(31,48,25,255),(173,194,102,255),(75,168,87,255),"leaf")
medal_texture(ASSETS/"item/gilded_trade_medal.png", (190,126,28,255),(76,43,9,255),(255,218,104,255),(225,87,26,255),"scales")
medal_texture(ASSETS/"item/emerald_bastion_medal.png", (30,124,94,255),(8,43,38,255),(115,235,180,255),(39,224,128,255),"shield")
pedestal_texture(ASSETS/"block/home_medal_pedestal.png", (83,91,75,255),(38,43,35,255),(91,140,58,255),(142,226,100,255),"leaf")
pedestal_texture(ASSETS/"block/trade_medal_pedestal.png", (105,73,55,255),(47,31,24,255),(186,102,43,255),(255,196,74,255),"scales")
pedestal_texture(ASSETS/"block/bastion_medal_pedestal.png", (54,61,67,255),(20,24,29,255),(25,112,88,255),(59,235,154,255),"shield")
print("Generated ArlightBosses 1.22.2 medal and pedestal textures")
