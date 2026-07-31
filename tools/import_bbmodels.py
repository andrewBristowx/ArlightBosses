from __future__ import annotations
import base64, json, math, os, re, shutil, zipfile
from pathlib import Path
from typing import Any
from PIL import Image

BASE_ZIP = Path('/mnt/data/ArlightBosses-1.8.2-TEXTURE-UV-NETHER-STABILITY-SOURCE.zip')
MODELS_ZIP = Path('/mnt/data/Downloads.zip')
OUT_ROOT = Path('/mnt/data/ArlightBosses-1.9.0-NEW-MODELS-ANIMATIONS-SOURCE')
WORK = Path('/mnt/data/_build_bosses_190')

MAPPINGS = [
    # source, slug, geometry identifier, animation prefix, archetype
    ('emerald_zombie_minion.bbmodel', 'emerald_zombie_minion', 'emerald_zombie_minion', 'animation.emerald_zombie', 'biped'),
    ('emerald_creeper_minion.bbmodel', 'emerald_creeper_minion', 'emerald_creeper_minion', 'animation.emerald_creeper', 'creeper'),
    ('emerald_golem_sentinel_minion.bbmodel', 'emerald_golem_sentinel_minion', 'emerald_golem_sentinel_minion', 'animation.emerald_golem_sentinel', 'golem'),
    ('emerald_skeleton_archer.bbmodel', 'emerald_skeleton_archer', 'emerald_skeleton_archer', 'animation.emerald_skeleton_archer', 'archer'),
    ('Emerald Ravager Cub Minion.bbmodel', 'emerald_ravager_cub', 'emerald_ravager_cub', 'animation.emerald_ravager_cub', 'ravager'),
    ('Mossbound Spider Minion.bbmodel', 'mossbound_spider_minion', 'mossbound_spider_minion', 'animation.mossbound_spider', 'spider'),
    ('Gilded Piglin Minion.bbmodel', 'gilded_piglin_minion', 'gilded_piglin_minion', 'animation.gilded_piglin', 'piglin'),
    ('gilded_blaze_wraith_minion.bbmodel', 'gilded_blaze_wraith_minion', 'gilded_blaze_wraith_minion', 'animation.gilded_blaze_wraith', 'blaze'),
    ('gilded_hoglin_rider_minion.bbmodel', 'gilded_hoglin_rider_minion', 'gilded_hoglin_rider_minion', 'animation.gilded_hoglin_rider', 'hoglin'),
    ('gilded_wither_skeleton_vanguard.bbmodel', 'gilded_wither_skeleton_vanguard', 'gilded_wither_skeleton_vanguard', 'animation.gilded_wither_vanguard', 'vanguard'),
    ('molten_strider_minion.bbmodel', 'molten_strider_minion', 'molten_strider_minion', 'animation.molten_strider', 'strider'),
    ('void_enderman_minion.bbmodel', 'void_enderman_minion', 'void_enderman_minion', 'animation.void_enderman', 'enderman'),
    ('void_enderman_sentinel_minion.bbmodel', 'void_enderman_sentinel_minion', 'void_enderman_sentinel_minion', 'animation.void_enderman_sentinel', 'enderman_sentinel'),
    ('amethyst_eye_minion.bbmodel', 'amethyst_eye_minion', 'amethyst_eye_minion', 'animation.amethyst_eye', 'eye'),
    ('amethyst_guardian_shard_minion.bbmodel', 'amethyst_guardian_shard_minion', 'amethyst_guardian_shard_minion', 'animation.amethyst_guardian_shard', 'golem'),
    ('amethyst_phantom_minion.bbmodel', 'amethyst_phantom_minion', 'amethyst_phantom_minion', 'animation.amethyst_phantom', 'phantom'),
    ('amethyst_shulker_minion.bbmodel', 'amethyst_shulker_minion', 'amethyst_shulker_minion', 'animation.amethyst_shulker', 'shulker'),
    ('Corrupted Ender Mite Minion.bbmodel', 'corrupted_ender_mite_minion', 'corrupted_ender_mite_minion', 'animation.corrupted_ender_mite', 'mite'),
    ('surface_guardian.bbmodel', 'surface_guardian', 'surface_guardian', 'animation.surface_guardian', 'surface_guardian'),
    ('nether_guardian.bbmodel', 'nether_guardian', 'nether_guardian', 'animation.nether_guardian', 'nether_guardian'),
    ('void_guardian.bbmodel', 'void_guardian', 'void_guardian', 'animation.void_guardian', 'void_guardian'),
    ('somita_vampire_person.bbmodel', 'somita_vampire_guardian', 'somita_vampire_guardian', 'animation.somita_vampire', 'vampire'),
    ('emerald_corruption_arrow.bbmodel', 'emerald_corruption_arrow_model', 'emerald_corruption_arrow_model', 'animation.emerald_corruption_arrow', 'arrow'),
]

# Hard-coded entity dimensions matched to the authored hidden hitboxes, but slightly tightened for gameplay.
ENTITY_SIZES = {
    'surface_guardian': (3.35, 3.40),
    'nether_guardian': (1.80, 3.18),
    'void_guardian': (3.30, 4.66),
    'dragon_guardian': (1.60, 2.32),
    'emerald_zombie_minion': (1.00, 2.44),
    'emerald_creeper_minion': (0.92, 2.02),
    'emerald_golem_sentinel_minion': (1.60, 2.55),
    'corrupted_ender_mite_minion': (1.45, 1.10),
    'emerald_skeleton_archer_minion': (0.98, 2.48),
    'emerald_ravager_cub_minion': (2.15, 1.94),
    'mossbound_spider_minion': (1.55, 1.18),
    'molten_strider_minion': (1.55, 1.68),
    'gilded_piglin_minion': (1.05, 2.50),
    'gilded_wither_skeleton_vanguard_minion': (1.46, 2.50),
    'gilded_hoglin_rider_minion': (1.72, 2.28),
    'gilded_blaze_wraith_minion': (0.92, 2.38),
    'void_enderman_minion': (0.82, 2.44),
    'void_enderman_sentinel_minion': (1.08, 3.18),
    'amethyst_eye_minion': (1.55, 1.92),
    'amethyst_guardian_shard_minion': (1.38, 2.18),
    'amethyst_shulker_minion': (1.28, 1.30),
    'amethyst_phantom_minion': (2.35, 0.95),
}


def clean():
    shutil.rmtree(WORK, ignore_errors=True)
    shutil.rmtree(OUT_ROOT, ignore_errors=True)
    WORK.mkdir(parents=True)
    with zipfile.ZipFile(BASE_ZIP) as z: z.extractall(WORK / 'base')
    with zipfile.ZipFile(MODELS_ZIP) as z: z.extractall(WORK / 'models')
    roots = [p for p in (WORK/'base').iterdir() if p.is_dir()]
    if len(roots) != 1: raise RuntimeError(f'Unexpected base roots: {roots}')
    shutil.copytree(roots[0], OUT_ROOT)


def group_iter(nodes):
    for n in nodes:
        if isinstance(n, dict):
            yield n
            yield from group_iter(n.get('children', []))


def model_center(data: dict[str, Any]):
    emap = {e['uuid']: e for e in data.get('elements', [])}
    for g in group_iter(data.get('outliner', [])):
        if g.get('name','').lower() == 'hitbox':
            cubes = [emap[x] for x in g.get('children', []) if isinstance(x, str) and x in emap]
            if cubes:
                mins = [min(e['from'][i] for e in cubes) for i in range(3)]
                maxs = [max(e['to'][i] for e in cubes) for i in range(3)]
                return ((mins[0]+maxs[0])/2, mins[1], (mins[2]+maxs[2])/2), (mins, maxs)
    elems = data.get('elements', [])
    mins = [min(e['from'][i] for e in elems) for i in range(3)]
    maxs = [max(e['to'][i] for e in elems) for i in range(3)]
    return ((mins[0]+maxs[0])/2, mins[1], (mins[2]+maxs[2])/2), (mins, maxs)


def tr_point(p, center):
    cx, base_y, cz = center
    return [round(p[0]-cx, 4), round(p[1]-base_y, 4), round(-(p[2]-cz), 4)]


def tr_rotation(r):
    return [round(-r[0], 4), round(-r[1], 4), round(r[2], 4)]


def face_uv(face):
    uv = face.get('uv')
    if not uv or face.get('texture') is None:
        return None
    out = {
        'uv': [round(uv[0], 4), round(uv[1], 4)],
        'uv_size': [round(uv[2]-uv[0], 4), round(uv[3]-uv[1], 4)],
    }
    return out


def convert_model(src: Path, slug: str, identifier: str):
    data = json.loads(src.read_text(encoding='utf-8'))
    center, hit_bounds = model_center(data)
    emap = {e['uuid']: e for e in data.get('elements', [])}
    bones = []
    used_names = set()

    def unique(name):
        base = re.sub(r'[^A-Za-z0-9_\-]', '_', name.strip()) or 'bone'
        n = base; i = 2
        while n in used_names:
            n = f'{base}_{i}'; i += 1
        used_names.add(n)
        return n

    # Preserve source names and hierarchy. Root gets a centered world pivot so global animations behave predictably.
    def walk(nodes, parent=None, hidden=False):
        for node in nodes:
            if isinstance(node, str):
                continue
            name_raw = node.get('name', 'bone')
            this_hidden = hidden or not node.get('visibility', True) or name_raw.lower() == 'hitbox' or not node.get('export', True)
            if this_hidden:
                continue
            name = unique(name_raw)
            bone = {'name': name}
            if parent: bone['parent'] = parent
            if name_raw.lower() == 'root' and parent is None:
                bone['pivot'] = [0, 0, 0]
            else:
                bone['pivot'] = tr_point(node.get('origin', [0,0,0]), center)
            rot = node.get('rotation', [0,0,0])
            if any(abs(v) > 1e-6 for v in rot): bone['rotation'] = tr_rotation(rot)
            cubes = []
            child_groups = []
            for child in node.get('children', []):
                if isinstance(child, str):
                    e = emap.get(child)
                    if not e or not e.get('visibility', True) or not e.get('export', True): continue
                    fr, to = e['from'], e['to']
                    cube = {
                        'origin': [round(fr[0]-center[0],4), round(fr[1]-center[1],4), round(-(to[2]-center[2]),4)],
                        'size': [round(to[i]-fr[i],4) for i in range(3)],
                    }
                    uv_obj = {}
                    # Direct face names are retained; Blockbench's Bedrock/Gecko coordinate conversion also mirrors Z.
                    for direction in ('north','east','south','west','up','down'):
                        fv = face_uv(e.get('faces',{}).get(direction,{}))
                        if fv: uv_obj[direction] = fv
                    if uv_obj: cube['uv'] = uv_obj
                    erot = e.get('rotation', [0,0,0])
                    if any(abs(v) > 1e-6 for v in erot):
                        cube['pivot'] = tr_point(e.get('origin', [(fr[0]+to[0])/2,(fr[1]+to[1])/2,(fr[2]+to[2])/2]), center)
                        cube['rotation'] = tr_rotation(erot)
                    cubes.append(cube)
                elif isinstance(child, dict):
                    child_groups.append(child)
            if cubes: bone['cubes'] = cubes
            bones.append(bone)
            walk(child_groups, name, False)

    walk(data.get('outliner', []))
    # Some free models (notably the projectile) have multiple top-level bones and no root.
    if bones and not any(b['name'] == 'root' for b in bones):
        bones.insert(0, {'name':'root','pivot':[0,0,0]})
        for b in bones[1:]:
            if 'parent' not in b: b['parent']='root'

    elems = [e for e in data.get('elements',[]) if e.get('visibility', True)]
    mins = [min(e['from'][i] for e in elems) for i in range(3)]
    maxs = [max(e['to'][i] for e in elems) for i in range(3)]
    width = max(maxs[0]-mins[0], maxs[2]-mins[2]) / 16.0 + 0.75
    height = (maxs[1]-center[1]) / 16.0 + 0.75
    res = data.get('resolution', {'width':64,'height':64})
    geo = {
      'format_version': '1.12.0',
      'minecraft:geometry': [{
        'description': {
          'identifier': f'geometry.{identifier}',
          'texture_width': int(res.get('width',64)),
          'texture_height': int(res.get('height',64)),
          'visible_bounds_width': round(max(2.0,width),3),
          'visible_bounds_height': round(max(2.0,height),3),
          'visible_bounds_offset': [0, round(max(0.5,height/2-0.2),3), 0],
        },
        'bones': bones,
      }]
    }
    return data, geo, [b['name'] for b in bones]


def extract_texture(data, dest: Path):
    textures = data.get('textures', [])
    if not textures: raise ValueError('bbmodel without embedded texture')
    source = textures[0].get('source','')
    if ',' not in source: raise ValueError('texture source is not embedded')
    raw = base64.b64decode(source.split(',',1)[1])
    dest.write_bytes(raw)
    with Image.open(dest) as im:
        im.load()
        expected = (int(data['resolution']['width']), int(data['resolution']['height']))
        if im.size != expected:
            # The bbmodel resolution is authoritative for UV coordinates; nearest-neighbour keeps pixel art exact.
            im.resize(expected, Image.Resampling.NEAREST).save(dest)


def glow_mask(src: Path, dest: Path, theme: str):
    im = Image.open(src).convert('RGBA')
    pix = im.load(); out = Image.new('RGBA', im.size, (0,0,0,0)); op=out.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a = pix[x,y]
            if a < 10: continue
            mx=max(r,g,b); mn=min(r,g,b); sat=(mx-mn)/max(1,mx); val=mx/255
            # Select luminous corruption/crystal/metal accents, not the complete body.
            if theme in ('emerald','surface'):
                selected = g > r*1.10 and g > b*1.05 and val > .30 and sat > .22
            elif theme in ('nether','gilded'):
                selected = (r > 125 and g > 55 and r > b*1.25 and val > .40 and sat > .25) or (r>180 and g>145 and b<120)
            elif theme in ('void','amethyst'):
                selected = (b > r*.85 and b > g*1.05 and val>.34 and sat>.20) or (r>120 and b>130 and g<150)
            elif theme == 'vampire':
                selected = (r>140 and (b>100 or g<90) and val>.45 and sat>.25)
            else:
                selected = val > .72 and sat > .22
            if selected:
                op[x,y]=(r,g,b,a)
    out.save(dest)


def points(*pairs):
    return {str(t): list(v) for t,v in pairs}


def anim_obj(loop, length):
    return {'loop': loop, 'animation_length': length, 'bones': {}}


def add(a, boneset, bone, channel, data):
    if bone not in boneset: return
    a['bones'].setdefault(bone,{})[channel]=data


def add_root_common(anims, prefix, boneset, death_len=1.6):
    spawn=anim_obj(False,0.7)
    add(spawn,boneset,'root','scale',points((0,[0.25,0.25,0.25]),(0.35,[1.12,1.12,1.12]),(0.7,[1,1,1])))
    add(spawn,boneset,'root','position',points((0,[0,2,0]),(0.7,[0,0,0])))
    hurt=anim_obj(False,0.28)
    add(hurt,boneset,'root','rotation',points((0,[0,0,0]),(0.10,[0,0,-8]),(0.20,[0,0,5]),(0.28,[0,0,0])))
    death=anim_obj(False,death_len)
    add(death,boneset,'root','rotation',points((0,[0,0,0]),(death_len*.45,[0,0,18]),(death_len,[0,0,88])))
    add(death,boneset,'root','position',points((0,[0,0,0]),(death_len,[0,-1.4,0])))
    anims[prefix+'.spawn']=spawn; anims[prefix+'.hurt']=hurt; anims[prefix+'.death']=death


def generate_animation(prefix: str, archetype: str, bones: list[str]):
    bs=set(bones); anims={}
    add_root_common(anims,prefix,bs,2.4 if 'guardian' in archetype or archetype=='vampire' else 1.6)

    idle=anim_obj(True,2.4); walk=anim_obj(True,1.0); attack=anim_obj(False,0.75)
    # universal breathing and head movement when those bones exist
    body = next((b for b in ('torso','body','body_core','hoglin_body','lower_body','vortex') if b in bs),None)
    if body:
        add(idle,bs,body,'rotation',points((0,[0,0,-1.5]),(1.2,[1.5,0,1.5]),(2.4,[0,0,-1.5])))
    add(idle,bs,'head','rotation',points((0,[0,-4,0]),(1.2,[1.5,4,0]),(2.4,[0,-4,0])))
    for crystal in [b for b in bs if any(k in b for k in ('crystal','shard','pylon','spike','crest','biolume','void_magic','pupil','glint'))]:
        add(idle,bs,crystal,'scale',points((0,[1,1,1]),(1.2,[1.08,1.08,1.08]),(2.4,[1,1,1])))

    if archetype in ('biped','piglin','archer','vanguard','golem','enderman','enderman_sentinel'):
        ll=next((b for b in ('left_leg','leg_left') if b in bs),None); rl=next((b for b in ('right_leg','leg_right') if b in bs),None)
        la=next((b for b in ('left_arm','arm_left') if b in bs),None); ra=next((b for b in ('right_arm','arm_right') if b in bs),None)
        for bone,phase in ((ll,1),(rl,-1)):
            if bone: add(walk,bs,bone,'rotation',points((0,[30*phase,0,0]),(.25,[0,0,0]),(.5,[-30*phase,0,0]),(.75,[0,0,0]),(1,[30*phase,0,0])))
        for bone,phase in ((la,-1),(ra,1)):
            if bone: add(walk,bs,bone,'rotation',points((0,[25*phase,0,0]),(.5,[-25*phase,0,0]),(1,[25*phase,0,0])))
        if body: add(walk,bs,body,'rotation',points((0,[0,-3,0]),(.5,[0,3,0]),(1,[0,-3,0])))
        if archetype=='archer':
            add(attack,bs,'arm_left','rotation',points((0,[0,0,0]),(.2,[-80,0,-18]),(.55,[-88,0,-12]),(.75,[0,0,0])))
            add(attack,bs,'arm_right','rotation',points((0,[0,0,0]),(.2,[-55,20,35]),(.55,[-100,-10,10]),(.75,[0,0,0])))
            add(attack,bs,'weapon','rotation',points((0,[0,0,0]),(.2,[0,-12,0]),(.55,[0,10,0]),(.75,[0,0,0])))
        elif archetype=='golem':
            for arm,sgn in (('arm_left',1),('arm_right',-1),('left_arm',1),('right_arm',-1)):
                add(attack,bs,arm,'rotation',points((0,[0,0,0]),(.25,[-105,0,18*sgn]),(.48,[35,0,-8*sgn]),(.75,[0,0,0])))
            if body: add(attack,bs,body,'rotation',points((0,[0,0,0]),(.25,[-12,0,0]),(.48,[12,0,0]),(.75,[0,0,0])))
        elif archetype.startswith('enderman'):
            for arm,sgn in ((la,1),(ra,-1)):
                if arm: add(attack,bs,arm,'rotation',points((0,[0,0,0]),(.2,[-115,0,15*sgn]),(.48,[40,0,-12*sgn]),(.75,[0,0,0])))
            add(attack,bs,'void_magic','scale',points((0,[.7,.7,.7]),(.3,[1.5,1.5,1.5]),(.75,[1,1,1])))
            add(attack,bs,'sentinel_pylons','rotation',points((0,[0,0,0]),(.35,[0,60,0]),(.75,[0,0,0])))
        else:
            for arm,sgn in ((la,1),(ra,-1)):
                if arm: add(attack,bs,arm,'rotation',points((0,[0,0,0]),(.18,[-75,0,15*sgn]),(.42,[35,0,-8*sgn]),(.75,[0,0,0])))
            weapon = 'weapon_right' if 'weapon_right' in bs else 'weapon'
            add(attack,bs,weapon,'rotation',points((0,[0,0,0]),(.18,[-35,0,0]),(.42,[55,0,0]),(.75,[0,0,0])))
    elif archetype=='creeper':
        add(idle,bs,'body','scale',points((0,[1,1,1]),(1.2,[1.03,.98,1.03]),(2.4,[1,1,1])))
        add(walk,bs,'legs','rotation',points((0,[7,0,-5]),(.5,[-7,0,5]),(1,[7,0,-5])))
        add(walk,bs,'body','position',points((0,[0,0,0]),(.25,[0,.35,0]),(.5,[0,0,0]),(.75,[0,.35,0]),(1,[0,0,0])))
        add(attack,bs,'body','scale',points((0,[1,1,1]),(.25,[1.18,.88,1.18]),(.5,[.92,1.18,.92]),(.75,[1,1,1])))
        add(attack,bs,'head_horns','scale',points((0,[1,1,1]),(.5,[1.25,1.25,1.25]),(.75,[1,1,1])))
    elif archetype=='ravager':
        legs=('leg_front_left','leg_front_right','leg_back_left','leg_back_right')
        phases=(1,-1,-1,1)
        for b,p in zip(legs,phases): add(walk,bs,b,'rotation',points((0,[24*p,0,0]),(.5,[-24*p,0,0]),(1,[24*p,0,0])))
        add(walk,bs,'torso','position',points((0,[0,0,0]),(.25,[0,.35,0]),(.5,[0,0,0]),(.75,[0,.35,0]),(1,[0,0,0])))
        add(attack,bs,'head','rotation',points((0,[0,0,0]),(.22,[-35,0,0]),(.48,[32,0,0]),(.75,[0,0,0])))
        add(attack,bs,'torso','position',points((0,[0,0,0]),(.32,[0,0,-2.2]),(.75,[0,0,0])))
        add(idle,bs,'tail_stone_base','rotation',points((0,[0,-10,0]),(1.2,[0,12,0]),(2.4,[0,-10,0])))
    elif archetype=='spider':
        add(walk,bs,'legs_left','rotation',points((0,[0,0,-14]),(.5,[0,0,14]),(1,[0,0,-14])))
        add(walk,bs,'legs_right','rotation',points((0,[0,0,14]),(.5,[0,0,-14]),(1,[0,0,14])))
        add(walk,bs,'body','position',points((0,[0,0,0]),(.25,[0,.45,0]),(.5,[0,0,0]),(.75,[0,.45,0]),(1,[0,0,0])))
        add(attack,bs,'body','rotation',points((0,[0,0,0]),(.2,[-22,0,0]),(.4,[20,0,0]),(.75,[0,0,0])))
        add(attack,bs,'legs_left','rotation',points((0,[0,0,0]),(.35,[0,0,-25]),(.75,[0,0,0])))
        add(attack,bs,'legs_right','rotation',points((0,[0,0,0]),(.35,[0,0,25]),(.75,[0,0,0])))
    elif archetype=='mite':
        for b,p in (('legs_front',1),('legs_mid',-1),('legs_rear',1)):
            add(walk,bs,b,'rotation',points((0,[0,0,16*p]),(.5,[0,0,-16*p]),(1,[0,0,16*p])))
        add(idle,bs,'tail','rotation',points((0,[0,-10,0]),(1.2,[0,12,0]),(2.4,[0,-10,0])))
        add(attack,bs,'head','rotation',points((0,[0,0,0]),(.25,[-28,0,0]),(.48,[28,0,0]),(.75,[0,0,0])))
    elif archetype=='hoglin':
        add(walk,bs,'hoglin_legs','rotation',points((0,[10,0,-8]),(.5,[-10,0,8]),(1,[10,0,-8])))
        add(walk,bs,'hoglin_body','position',points((0,[0,0,0]),(.25,[0,.4,0]),(.5,[0,0,0]),(.75,[0,.4,0]),(1,[0,0,0])))
        add(idle,bs,'rider','rotation',points((0,[0,0,-2]),(1.2,[0,0,2]),(2.4,[0,0,-2])))
        add(attack,bs,'hoglin_head','rotation',points((0,[0,0,0]),(.2,[-30,0,0]),(.48,[28,0,0]),(.75,[0,0,0])))
        add(attack,bs,'spear','rotation',points((0,[0,0,0]),(.2,[-55,0,0]),(.48,[35,0,0]),(.75,[0,0,0])))
    elif archetype=='strider':
        for b,p in (('leg_left',1),('leg_right',-1)):
            add(walk,bs,b,'rotation',points((0,[28*p,0,0]),(.5,[-28*p,0,0]),(1,[28*p,0,0])))
        add(idle,bs,'body','position',points((0,[0,0,0]),(1.2,[0,.5,0]),(2.4,[0,0,0])))
        add(attack,bs,'body','rotation',points((0,[0,0,0]),(.25,[-20,0,0]),(.5,[22,0,0]),(.75,[0,0,0])))
        for b in [x for x in bs if x.startswith('spike_') or x.startswith('head_crest')]:
            add(attack,bs,b,'scale',points((0,[1,1,1]),(.4,[1.25,1.25,1.25]),(.75,[1,1,1])))
    elif archetype=='blaze':
        add(idle,bs,'root','position',points((0,[0,0,0]),(1.2,[0,.65,0]),(2.4,[0,0,0])))
        add(idle,bs,'vortex','rotation',points((0,[0,0,0]),(2.4,[0,180,0])))
        for i,b in enumerate(('blaze_rod_fl','blaze_rod_fr','blaze_rod_bl','blaze_rod_br')):
            add(idle,bs,b,'rotation',points((0,[0,i*8,0]),(1.2,[0,90+i*8,0]),(2.4,[0,180+i*8,0])))
        walk=idle.copy(); walk={'loop':True,'animation_length':1.6,'bones':json.loads(json.dumps(idle['bones']))}
        add(attack,bs,'torso','scale',points((0,[1,1,1]),(.35,[1.18,1.18,1.18]),(.75,[1,1,1])))
        for b in ('blaze_rod_fl','blaze_rod_fr','blaze_rod_bl','blaze_rod_br'):
            add(attack,bs,b,'position',points((0,[0,0,0]),(.35,[0,2,0]),(.75,[0,0,0])))
    elif archetype=='phantom':
        for a in (idle,walk):
            add(a,bs,'root','position',points((0,[0,0,0]),(a['animation_length']/2,[0,.5,0]),(a['animation_length'],[0,0,0])))
        add(idle,bs,'wing_left','rotation',points((0,[0,0,18]),(.6,[0,0,-18]),(1.2,[0,0,18]),(1.8,[0,0,-18]),(2.4,[0,0,18])))
        add(idle,bs,'wing_right','rotation',points((0,[0,0,-18]),(.6,[0,0,18]),(1.2,[0,0,-18]),(1.8,[0,0,18]),(2.4,[0,0,-18])))
        add(walk,bs,'wing_left','rotation',points((0,[0,0,38]),(.25,[0,0,-38]),(.5,[0,0,38]),(.75,[0,0,-38]),(1,[0,0,38])))
        add(walk,bs,'wing_right','rotation',points((0,[0,0,-38]),(.25,[0,0,38]),(.5,[0,0,-38]),(.75,[0,0,38]),(1,[0,0,-38])))
        add(attack,bs,'body','rotation',points((0,[0,0,0]),(.25,[-32,0,0]),(.55,[18,0,0]),(.75,[0,0,0])))
        add(attack,bs,'wing_left','rotation',points((0,[0,0,0]),(.35,[0,0,-55]),(.75,[0,0,0])))
        add(attack,bs,'wing_right','rotation',points((0,[0,0,0]),(.35,[0,0,55]),(.75,[0,0,0])))
    elif archetype=='eye':
        add(idle,bs,'root','position',points((0,[0,0,0]),(1.2,[0,.7,0]),(2.4,[0,0,0])))
        add(idle,bs,'eye_iris','scale',points((0,[1,1,1]),(1.2,[1.08,1.08,1.08]),(2.4,[1,1,1])))
        for b,p in (('leg_fl_thigh',1),('leg_fr_thigh',-1),('leg_bl_thigh',-1),('leg_br_thigh',1)):
            add(walk,bs,b,'rotation',points((0,[18*p,0,0]),(.5,[-18*p,0,0]),(1,[18*p,0,0])))
        add(walk,bs,'root','position',points((0,[0,0,0]),(.25,[0,.5,0]),(.5,[0,0,0]),(.75,[0,.5,0]),(1,[0,0,0])))
        add(attack,bs,'eye_pupil_core','scale',points((0,[1,1,1]),(.25,[.45,1.25,.45]),(.5,[1.35,.75,1.35]),(.75,[1,1,1])))
        for b,s in (('shard_l_core',-1),('shard_r_core',1)):
            add(attack,bs,b,'rotation',points((0,[0,0,0]),(.4,[0,0,35*s]),(.75,[0,0,0])))
    elif archetype=='shulker':
        add(idle,bs,'top_shell','position',points((0,[0,0,0]),(1.2,[0,.6,0]),(2.4,[0,0,0])))
        walk=json.loads(json.dumps(idle)); walk['animation_length']=1.6
        add(attack,bs,'top_shell','position',points((0,[0,0,0]),(.25,[0,4.5,0]),(.55,[0,4.5,0]),(.75,[0,0,0])))
        add(attack,bs,'arm_left','rotation',points((0,[0,0,0]),(.4,[-70,0,-25]),(.75,[0,0,0])))
        add(attack,bs,'arm_right','rotation',points((0,[0,0,0]),(.4,[-70,0,25]),(.75,[0,0,0])))
    elif archetype in ('surface_guardian','nether_guardian','void_guardian'):
        # Guardian-specific heavy gait and phase roar.
        ll='leg_left' if 'leg_left' in bs else 'left_leg'; rl='leg_right' if 'leg_right' in bs else 'right_leg'
        la='arm_left' if 'arm_left' in bs else 'left_arm'; ra='arm_right' if 'arm_right' in bs else 'right_arm'
        for b,p in ((ll,1),(rl,-1)): add(walk,bs,b,'rotation',points((0,[25*p,0,0]),(.5,[-25*p,0,0]),(1,[25*p,0,0])))
        for b,p in ((la,-1),(ra,1)): add(walk,bs,b,'rotation',points((0,[18*p,0,0]),(.5,[-18*p,0,0]),(1,[18*p,0,0])))
        add(walk,bs,'torso','position',points((0,[0,0,0]),(.25,[0,.35,0]),(.5,[0,0,0]),(.75,[0,.35,0]),(1,[0,0,0])))
        for b,s in ((la,1),(ra,-1)): add(attack,bs,b,'rotation',points((0,[0,0,0]),(.25,[-120,0,18*s]),(.5,[45,0,-10*s]),(.75,[0,0,0])))
        add(attack,bs,'torso','rotation',points((0,[0,0,0]),(.25,[-15,0,0]),(.5,[18,0,0]),(.75,[0,0,0])))
        roar=anim_obj(False,1.35)
        add(roar,bs,'torso','rotation',points((0,[0,0,0]),(.35,[-18,0,0]),(.8,[-8,0,0]),(1.35,[0,0,0])))
        add(roar,bs,'head','rotation',points((0,[0,0,0]),(.35,[-35,0,0]),(.8,[-20,0,0]),(1.35,[0,0,0])))
        for b,s in ((la,1),(ra,-1)): add(roar,bs,b,'rotation',points((0,[0,0,0]),(.45,[-55,0,55*s]),(1.0,[-30,0,35*s]),(1.35,[0,0,0])))
        anims[prefix+'.roar']=roar
    elif archetype=='vampire':
        add(idle,bs,'wings','rotation',points((0,[0,0,-3]),(1.2,[0,0,3]),(2.4,[0,0,-3])))
        add(idle,bs,'tail','rotation',points((0,[0,-8,0]),(1.2,[0,10,0]),(2.4,[0,-8,0])))
        add(walk,bs,'legs','rotation',points((0,[12,0,0]),(.5,[-12,0,0]),(1,[12,0,0])))
        add(walk,bs,'arms','rotation',points((0,[-8,0,0]),(.5,[8,0,0]),(1,[-8,0,0])))
        add(attack,bs,'arms','rotation',points((0,[0,0,0]),(.22,[-95,0,0]),(.48,[35,0,0]),(.75,[0,0,0])))
        add(attack,bs,'wings','rotation',points((0,[0,0,0]),(.30,[0,0,32]),(.75,[0,0,0])))
        roar=anim_obj(False,1.35)
        add(roar,bs,'wings','rotation',points((0,[0,0,0]),(.45,[0,0,45]),(.9,[0,0,-10]),(1.35,[0,0,0])))
        add(roar,bs,'head','rotation',points((0,[0,0,0]),(.45,[-25,0,0]),(1.35,[0,0,0])))
        add(roar,bs,'tail_mid','rotation',points((0,[0,0,0]),(.45,[0,35,0]),(1.35,[0,0,0])))
        anims[prefix+'.roar']=roar
    elif archetype=='arrow':
        idle['animation_length']=.6
        add(idle,bs,'head','rotation',points((0,[0,-4,0]),(.3,[1.5,4,0]),(.6,[0,-4,0])))
        add(idle,bs,'root','rotation',points((0,[0,0,0]),(.6,[0,0,360])))
        walk=json.loads(json.dumps(idle))
        add(attack,bs,'head','scale',points((0,[1,1,1]),(.35,[1.2,1.2,1.2]),(.75,[1,1,1])))

    anims[prefix+'.idle']=idle; anims[prefix+'.walk']=walk; anims[prefix+'.attack']=attack
    if archetype in ('enderman','enderman_sentinel'):
        growl=anim_obj(False,1.1)
        add(growl,bs,'head','rotation',points((0,[0,0,0]),(.25,[-25,0,0]),(.65,[-15,12,0]),(1.1,[0,0,0])))
        for arm,s in (('arm_left',1),('arm_right',-1)):
            add(growl,bs,arm,'rotation',points((0,[0,0,0]),(.35,[-45,0,30*s]),(.8,[-25,0,18*s]),(1.1,[0,0,0])))
        add(growl,bs,'void_magic','scale',points((0,[1,1,1]),(.5,[1.45,1.45,1.45]),(1.1,[1,1,1])))
        anims[prefix+'.growl']=growl
    return {'format_version':'1.8.0','animations':anims}


def theme_for(slug):
    if 'ender_mite' in slug: return 'void'
    if slug.startswith(('emerald','mossbound','surface')): return 'emerald'
    if slug.startswith(('gilded','molten','nether')): return 'nether'
    if slug.startswith(('void','amethyst')): return 'void'
    if 'vampire' in slug: return 'vampire'
    return 'generic'


def install_assets():
    assets = OUT_ROOT/'src/main/resources/assets/arlightbosses'
    geo_dir=assets/'geo'; anim_dir=assets/'animations'; tex_dir=assets/'textures/entity'
    source_dir=OUT_ROOT/'blockbench_sources'
    for p in (geo_dir,anim_dir,tex_dir,source_dir): p.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for source_name,slug,identifier,prefix,archetype in MAPPINGS:
        src=WORK/'models'/source_name
        data,geo,bones=convert_model(src,slug,identifier)
        (geo_dir/f'{slug}.geo.json').write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        anim=generate_animation(prefix,archetype,bones)
        (anim_dir/f'{slug}.animation.json').write_text(json.dumps(anim,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        tex_name = slug + '.png'
        tex=tex_dir/tex_name
        extract_texture(data,tex)
        glow_mask(tex,tex.with_name(tex.stem+'_glowmask.png'),theme_for(slug))
        shutil.copy2(src,source_dir/source_name)
        center,bounds=model_center(data)
        manifest.append({'source':source_name,'slug':slug,'identifier':identifier,'animation_prefix':prefix,
                         'archetype':archetype,'bones':bones,'texture':tex_name,'authored_hitbox':bounds})
    (OUT_ROOT/'MODEL_IMPORT_MANIFEST.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def write_minion_model_java():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/client/CorruptedMinionModel.java'
    code='''package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Maps every registered minion to the matching model, texture and animation set imported from Downloads.zip. */
public final class CorruptedMinionModel<T extends CorruptedMinionEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T minion) {
        return id("geo/" + asset(minion) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T minion) {
        return id("textures/entity/" + asset(minion) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T minion) {
        return id("animations/" + asset(minion) + ".animation.json");
    }

    private static String asset(CorruptedMinionEntity minion) {
        if (minion instanceof EmeraldCreeperMinion) return "emerald_creeper_minion";
        if (minion instanceof EmeraldGolemSentinelMinion) return "emerald_golem_sentinel_minion";
        if (minion instanceof EmeraldSkeletonArcherMinion) return "emerald_skeleton_archer";
        if (minion instanceof EmeraldRavagerCubMinion) return "emerald_ravager_cub";
        if (minion instanceof MossboundSpiderMinion) return "mossbound_spider_minion";
        if (minion instanceof GildedPiglinMinion) return "gilded_piglin_minion";
        if (minion instanceof GildedBlazeWraithMinion) return "gilded_blaze_wraith_minion";
        if (minion instanceof GildedHoglinRiderMinion) return "gilded_hoglin_rider_minion";
        if (minion instanceof GildedWitherSkeletonVanguardMinion) return "gilded_wither_skeleton_vanguard";
        if (minion instanceof MoltenStriderMinion) return "molten_strider_minion";
        if (minion instanceof VoidEndermanSentinelMinion) return "void_enderman_sentinel_minion";
        if (minion instanceof VoidEndermanMinion) return "void_enderman_minion";
        if (minion instanceof AmethystEyeMinion) return "amethyst_eye_minion";
        if (minion instanceof AmethystGuardianShardMinion) return "amethyst_guardian_shard_minion";
        if (minion instanceof AmethystPhantomMinion) return "amethyst_phantom_minion";
        if (minion instanceof AmethystShulkerMinion) return "amethyst_shulker_minion";
        if (minion instanceof CorruptedEnderMiteMinion) return "corrupted_ender_mite_minion";
        return "emerald_zombie_minion";
    }

    private static ResourceLocation id(String path) { return BossClientEvents.id(path); }
}
'''
    p.write_text(code,encoding='utf-8')


def write_guardian_model_java():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/client/GuardianModel.java'
    code='''package com.arlight.bosses.client;

import com.arlight.bosses.entity.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Dedicated guardian models: surface, Nether, void and Somita vampire (draconic slot). */
public final class GuardianModel<T extends GuardianEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T guardian) {
        return id("geo/" + asset(guardian) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T guardian) {
        return id("textures/entity/" + asset(guardian) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T guardian) {
        return id("animations/" + asset(guardian) + ".animation.json");
    }

    private static String asset(GuardianEntity guardian) {
        if (guardian instanceof NetherGuardian) return "nether_guardian";
        if (guardian instanceof VoidGuardian) return "void_guardian";
        if (guardian instanceof DragonGuardian) return "somita_vampire_guardian";
        return "surface_guardian";
    }

    private static ResourceLocation id(String path) { return BossClientEvents.id(path); }
}
'''
    p.write_text(code,encoding='utf-8')


def patch_renderers():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/client/CorruptedMinionRenderer.java'
    p.write_text('''package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.CorruptedMinionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer for the new per-creature models, including their generated emissive accent masks. */
public final class CorruptedMinionRenderer<T extends CorruptedMinionEntity> extends GeoEntityRenderer<T> {
    public CorruptedMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedMinionModel<>());
        this.shadowRadius = 0.72F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override protected float getDeathMaxRotation(T minion) { return 0.0F; }
}
''',encoding='utf-8')
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/client/BossClientEvents.java'
    text=p.read_text(encoding='utf-8').replace('context -> new GuardianRenderer<>(context, false)','context -> new GuardianRenderer<>(context, true)')
    p.write_text(text,encoding='utf-8')


def patch_animation_prefixes():
    repl={
      'EmeraldZombieMinion.java':('animation.corrupted_minion','animation.emerald_zombie'),
      'GildedPiglinMinion.java':('animation.corrupted_minion','animation.gilded_piglin'),
      'VoidEndermanMinion.java':('animation.enderman_void','animation.void_enderman'),
      'VoidEndermanSentinelMinion.java':('animation.enderman_void','animation.void_enderman_sentinel'),
    }
    d=OUT_ROOT/'src/main/java/com/arlight/bosses/entity/minion'
    for file,(old,new) in repl.items():
        p=d/file; t=p.read_text(encoding='utf-8');
        if old not in t: raise RuntimeError(f'{old} missing in {file}')
        p.write_text(t.replace(old,new),encoding='utf-8')


def patch_base_animation_features():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/entity/minion/CorruptedMinionEntity.java'
    t=p.read_text(encoding='utf-8')
    t=t.replace('private static final String DEATH_TRIGGER = "death";', 'private static final String DEATH_TRIGGER = "death";\n    private static final String HURT_TRIGGER = "hurt";\n    private static final String SPAWN_TRIGGER = "spawn";')
    t=t.replace('RawAnimation death = RawAnimation.begin().thenPlay(animationPrefix() + ".death");', 'RawAnimation death = RawAnimation.begin().thenPlay(animationPrefix() + ".death");\n        RawAnimation hurt = RawAnimation.begin().thenPlay(animationPrefix() + ".hurt");\n        RawAnimation spawn = RawAnimation.begin().thenPlay(animationPrefix() + ".spawn");')
    t=t.replace('.triggerableAnim(DEATH_TRIGGER, death));', '.triggerableAnim(DEATH_TRIGGER, death)\n                .triggerableAnim(HURT_TRIGGER, hurt)\n                .triggerableAnim(SPAWN_TRIGGER, spawn));')
    anchor='''    @Override
    public void die(DamageSource source) {'''
    insert='''    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !isDeadOrDying() && level() instanceof ServerLevel) {
            triggerAnim(CONTROLLER, HURT_TRIGGER);
        }
        return damaged;
    }

'''
    if anchor not in t: raise RuntimeError('die anchor missing')
    t=t.replace(anchor,insert+anchor)
    ai='''    @Override
    public void aiStep() {
        super.aiStep();'''
    ai_new='''    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount == 1 && level() instanceof ServerLevel) {
            triggerAnim(CONTROLLER, SPAWN_TRIGGER);
        }'''
    if ai not in t: raise RuntimeError('aiStep anchor missing')
    t=t.replace(ai,ai_new)
    p.write_text(t,encoding='utf-8')


def patch_guardian_entity():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/entity/GuardianEntity.java'
    t=p.read_text(encoding='utf-8')
    # replace constants with dynamic prefixes and extra triggers
    start=t.index('    private static final String CONTROLLER')
    end=t.index('\n\n    private final AnimatableInstanceCache',start)
    constants='''    private static final String CONTROLLER = "main";
    private static final String ATTACK_TRIGGER = "attack";
    private static final String ROAR_TRIGGER = "roar";
    private static final String DEATH_TRIGGER = "death";
    private static final String HURT_TRIGGER = "hurt";
    private static final String SPAWN_TRIGGER = "spawn";
    private static final int DEATH_ANIMATION_TICKS = 52;
    private static final int PLAYER_SEARCH_INTERVAL = 20;'''
    t=t[:start]+constants+t[end:]
    old='''    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, this::locomotionAnimation)
                .triggerableAnim(ATTACK_TRIGGER, ATTACK)
                .triggerableAnim(ROAR_TRIGGER, ROAR)
                .triggerableAnim(DEATH_TRIGGER, DEATH));
    }

    private PlayState locomotionAnimation(AnimationState<GuardianEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }
'''
    new='''    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenPlay(prefix + ".idle");
        RawAnimation walk = RawAnimation.begin().thenPlay(prefix + ".walk");
        RawAnimation attack = RawAnimation.begin().thenPlay(prefix + ".attack");
        RawAnimation roar = RawAnimation.begin().thenPlay(prefix + ".roar");
        RawAnimation death = RawAnimation.begin().thenPlay(prefix + ".death");
        RawAnimation hurt = RawAnimation.begin().thenPlay(prefix + ".hurt");
        RawAnimation spawn = RawAnimation.begin().thenPlay(prefix + ".spawn");
        controllers.add(new AnimationController<>(this, CONTROLLER, 4,
                state -> state.setAndContinue(state.isMoving() ? walk : idle))
                .triggerableAnim(ATTACK_TRIGGER, attack)
                .triggerableAnim(ROAR_TRIGGER, roar)
                .triggerableAnim(DEATH_TRIGGER, death)
                .triggerableAnim(HURT_TRIGGER, hurt)
                .triggerableAnim(SPAWN_TRIGGER, spawn));
    }

    private String animationPrefix() {
        if (this instanceof NetherGuardian) return "animation.nether_guardian";
        if (this instanceof VoidGuardian) return "animation.void_guardian";
        if (this instanceof DragonGuardian) return "animation.somita_vampire";
        return "animation.surface_guardian";
    }
'''
    if old not in t: raise RuntimeError('guardian controller block not found')
    t=t.replace(old,new)
    # spawn trigger in aiStep after super
    t=t.replace('''    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;''','''    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (tickCount == 1 && level() instanceof ServerLevel) triggerAnim(CONTROLLER, SPAWN_TRIGGER);''')
    # hurt trigger before die
    anchor='''    @Override
    public void die(DamageSource source) {'''
    insert='''    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !isDeadOrDying() && level() instanceof ServerLevel) {
            triggerAnim(CONTROLLER, HURT_TRIGGER);
        }
        return damaged;
    }

'''
    if anchor not in t: raise RuntimeError('guardian die anchor')
    t=t.replace(anchor,insert+anchor)
    p.write_text(t,encoding='utf-8')


def patch_sizes():
    p=OUT_ROOT/'src/main/java/com/arlight/bosses/entity/BossEntities.java'
    t=p.read_text(encoding='utf-8')
    # Match each TYPES.register id to its first .sized(...) within the registration block.
    for eid,(w,h) in ENTITY_SIZES.items():
        pattern=rf'(TYPES\.register\("{re.escape(eid)}".*?\.sized\()([0-9.]+F,\s*[0-9.]+F)(\))'
        nt,n=re.subn(pattern,rf'\g<1>{w:.2f}F, {h:.2f}F\g<3>',t,count=1,flags=re.S)
        if n!=1: raise RuntimeError(f'Could not patch size {eid}')
        t=nt
    p.write_text(t,encoding='utf-8')


def patch_version_and_notes():
    p=OUT_ROOT/'build.gradle'; t=p.read_text(encoding='utf-8'); t=re.sub(r"version = '[^']+'","version = '1.9.0'",t); p.write_text(t,encoding='utf-8')
    notes='''ARLIGHTBOSSES 1.9.0 - NUEVOS MODELOS Y ANIMACIONES
====================================================

Base: 1.8.2 TEXTURE-UV-NETHER-STABILITY.

Cambios:
- Reemplazados los modelos visuales de todos los esbirros por los 19 modelos de Downloads.zip.
- Surface Guardian, Nether Guardian y Void Guardian ahora tienen modelos propios.
- El espacio de Dragon Guardian usa el modelo Somita Cute Vampire entregado por el usuario.
- Texturas embebidas extraídas directamente de cada .bbmodel con su resolución UV original.
- Modelos recentrados usando el hitbox oculto diseñado en Blockbench para evitar que aparezcan de lado.
- Hitboxes de EntityType actualizados para coincidir mejor con los nuevos cuerpos.
- Animaciones nuevas: aparición, idle, caminar/volar, ataque, recibir daño y muerte.
- Guardianes: animación adicional de rugido/cambio de fase.
- Enderman del vacío: animación adicional growl.
- Animaciones específicas para alas, colas, armas, caparazones, cristales, patas y pylons.
- Máscaras emisivas generadas para cristales/corrupción y capa AutoGlowingGeoLayer en todos los esbirros.
- Se mantienen todas las correcciones de compilación y estabilidad de 1.8.2.
- Se incluyen los .bbmodel originales en blockbench_sources/ y el conversor tools/import_bbmodels.py como referencia.

NOTA SOBRE LA FLECHA
--------------------
El modelo de la flecha de corrupción también fue convertido e incluido como recurso y fuente Blockbench.
La entidad sigue usando el ArrowRenderer vainilla por estabilidad con NeoForge/Arclight; su lógica y textura continúan funcionando.

COMPILAR
--------
gradlew.bat clean build

Salida esperada:
build/libs/ArlightBosses-1.9.0.jar
'''
    (OUT_ROOT/'CAMBIOS_1.9.0.txt').write_text(notes,encoding='utf-8')



def patch_language_and_readme():
    for lang, value in (("es_es.json", "Somita Vampira"), ("en_us.json", "Somita Vampire")):
        p = OUT_ROOT / 'src/main/resources/assets/arlightbosses/lang' / lang
        data = json.loads(p.read_text(encoding='utf-8'))
        data['entity.arlightbosses.dragon_guardian'] = value
        p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    readme = """ARLIGHT BOSSES 1.9.0 - NUEVOS MODELOS Y ANIMACIONES
====================================================

Requisitos:
- Java 21
- IntelliJ IDEA
- NeoForge 1.21.1
- GeckoLib 4.9.2 (Gradle la descarga como dependencia)

Compilar en Windows:
1. Abre esta carpeta como proyecto Gradle.
2. Espera a que Gradle termine de sincronizar.
3. En Gradle ejecuta Tasks > build > clean.
4. Después ejecuta Tasks > build > build.

Desde una terminal:

    gradlew.bat clean build

El JAR se genera en:

    build\\libs\\ArlightBosses-1.9.0.jar

IMPORTANTE
----------
Instala el mismo JAR 1.9.0 en el servidor y en todos los clientes.
Elimina el JAR 1.8.2 anterior para no cargar dos versiones del mismo mod.
Los archivos .bbmodel originales están en blockbench_sources/.
Los recursos ya exportados están en src/main/resources/assets/arlightbosses/.
"""
    (OUT_ROOT / 'README-COMPILAR.txt').write_text(readme, encoding='utf-8')

def install_tool_copy():
    tools=OUT_ROOT/'tools'; tools.mkdir(exist_ok=True)
    # Copy this script as an auditable reference, replacing absolute paths with explanatory comments isn't required for build.
    shutil.copy2(Path(__file__),tools/'import_bbmodels.py')


def remove_legacy_assets():
    assets=OUT_ROOT/'src/main/resources/assets/arlightbosses'
    keep_geo={m[1]+'.geo.json' for m in MAPPINGS}
    keep_anim={m[1]+'.animation.json' for m in MAPPINGS}
    # Keep legacy stone assets only if still referenced (they no longer are); remove confusing obsolete models/animations.
    for p in (assets/'geo').glob('*.geo.json'):
        if p.name not in keep_geo: p.unlink()
    for p in (assets/'animations').glob('*.animation.json'):
        if p.name not in keep_anim: p.unlink()
    # Texture output has new files plus the still-used gilded shard projectile.
    keep_tex={'gilded_shard_projectile.png', 'emerald_corruption_arrow.png'}
    for m in MAPPINGS:
        name=m[1]+'.png'; keep_tex.add(name); keep_tex.add(name[:-4]+'_glowmask.png')
    for p in (assets/'textures/entity').glob('*.png'):
        if p.name not in keep_tex: p.unlink()


def validate():
    # All JSON and PNG files load.
    for p in OUT_ROOT.rglob('*.json'):
        json.loads(p.read_text(encoding='utf-8'))
    for p in OUT_ROOT.rglob('*.png'):
        with Image.open(p) as im: im.verify()
    assets=OUT_ROOT/'src/main/resources/assets/arlightbosses'
    # Every animation bone must be present in its corresponding geometry.
    for _,slug,_,_,_ in MAPPINGS:
        geo=json.loads((assets/'geo'/f'{slug}.geo.json').read_text())
        bones={b['name'] for b in geo['minecraft:geometry'][0]['bones']}
        anim=json.loads((assets/'animations'/f'{slug}.animation.json').read_text())
        used={b for a in anim['animations'].values() for b in a.get('bones',{})}
        missing=used-bones
        if missing: raise RuntimeError(f'{slug}: animation missing bones {missing}')
    # Check Java braces (lightweight structural validation).
    for p in OUT_ROOT.rglob('*.java'):
        s=re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"', '', p.read_text(encoding='utf-8'), flags=re.S)
        if s.count('{')!=s.count('}'):
            raise RuntimeError(f'Unbalanced Java braces: {p}')
    # Resource mappings referenced by models.
    for required in ('emerald_zombie_minion','void_enderman_sentinel_minion','surface_guardian','nether_guardian','void_guardian','somita_vampire_guardian'):
        for sub,ext in (('geo','.geo.json'),('animations','.animation.json')):
            if not (assets/sub/(required+ext)).exists(): raise RuntimeError(f'missing {required}{ext}')
        if not (assets/'textures/entity'/(required+'.png')).exists(): raise RuntimeError(f'missing texture {required}')


def zip_output():
    outzip=Path('/mnt/data/ArlightBosses-1.9.0-NEW-MODELS-ANIMATIONS-SOURCE.zip')
    if outzip.exists(): outzip.unlink()
    with zipfile.ZipFile(outzip,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in sorted(OUT_ROOT.rglob('*')):
            if p.is_file(): z.write(p,p.relative_to(OUT_ROOT.parent))
    return outzip


def main():
    clean(); install_assets(); write_minion_model_java(); write_guardian_model_java(); patch_renderers(); patch_animation_prefixes(); patch_base_animation_features(); patch_guardian_entity(); patch_sizes(); patch_version_and_notes(); patch_language_and_readme(); install_tool_copy(); remove_legacy_assets(); validate(); out=zip_output(); print(out); print('size',out.stat().st_size)

if __name__=='__main__': main()
