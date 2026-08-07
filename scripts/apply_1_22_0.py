#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, value: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(value, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"No se encontró el ancla para {label}")
    return text.replace(old, new, 1)


# Corrige el codec para conservar la variante real del bloque registrado.
pedestal_path = "src/main/java/com/arlight/bosses/block/MedalPedestalBlock.java"
pedestal = read(pedestal_path)
pedestal = pedestal.replace(
    '    public static final MapCodec<MedalPedestalBlock> CODEC = simpleCodec(\n'
    '            properties -> new MedalPedestalBlock(MedalKind.HOME, properties));\n',
    ''
)
pedestal = pedestal.replace(
    '    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }',
    '    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return MapCodec.unit(this); }'
)
write(pedestal_path, pedestal)


items_path = "src/main/java/com/arlight/bosses/item/BossItems.java"
items = read(items_path)

medal_constants = '''    public static final DeferredHolder<Item, Item> MOSSBOUND_HOME_MEDAL =
            ITEMS.register("mossbound_home_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.mossbound_home_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> GILDED_TRADE_MEDAL =
            ITEMS.register("gilded_trade_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.gilded_trade_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> EMERALD_BASTION_MEDAL =
            ITEMS.register("emerald_bastion_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.emerald_bastion_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

'''
anchor = '    public static final DeferredHolder<Item, BlockItem> CORRUPTED_PEARL_ALTAR =\n'
if 'MOSSBOUND_HOME_MEDAL' not in items:
    if anchor not in items:
        raise RuntimeError("No se encontró el ancla de objetos de misión")
    items = items.replace(anchor, medal_constants + anchor, 1)

pedestal_items = '''    public static final DeferredHolder<Item, BlockItem> HOME_MEDAL_PEDESTAL =
            ITEMS.register("home_medal_pedestal", () -> new BlockItem(
                    BossBlocks.HOME_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> TRADE_MEDAL_PEDESTAL =
            ITEMS.register("trade_medal_pedestal", () -> new BlockItem(
                    BossBlocks.TRADE_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> BASTION_MEDAL_PEDESTAL =
            ITEMS.register("bastion_medal_pedestal", () -> new BlockItem(
                    BossBlocks.BASTION_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

'''
anchor = '    public static final DeferredHolder<Item, BlockItem> COPPER_TREASURE_CHEST = treasureBlockItem(\n'
if 'DeferredHolder<Item, BlockItem> HOME_MEDAL_PEDESTAL' not in items:
    if anchor not in items:
        raise RuntimeError("No se encontró el ancla de BlockItems")
    items = items.replace(anchor, pedestal_items + anchor, 1)

items = replace_once(
    items,
    '            event.accept(EMERALDIZED_DRAGON_KEY.get());\n',
    '            event.accept(EMERALDIZED_DRAGON_KEY.get());\n'
    '            event.accept(MOSSBOUND_HOME_MEDAL.get());\n'
    '            event.accept(GILDED_TRADE_MEDAL.get());\n'
    '            event.accept(EMERALD_BASTION_MEDAL.get());\n',
    "medallas en pestaña creativa"
)
items = replace_once(
    items,
    '            event.accept(NETHER_DUNGEON_LOCK.get());\n',
    '            event.accept(NETHER_DUNGEON_LOCK.get());\n'
    '            event.accept(HOME_MEDAL_PEDESTAL.get());\n'
    '            event.accept(TRADE_MEDAL_PEDESTAL.get());\n'
    '            event.accept(BASTION_MEDAL_PEDESTAL.get());\n',
    "pedestales en pestaña creativa"
)
write(items_path, items)


translations_es = {
    "item.arlightbosses.mossbound_home_medal": "Medalla del Hogar Musgoso",
    "item.arlightbosses.mossbound_home_medal.lore": "Reconoce a quien protegió los hogares del bosque contaminado.",
    "item.arlightbosses.gilded_trade_medal": "Medalla del Comercio Dorado",
    "item.arlightbosses.gilded_trade_medal.lore": "Prueba que las rutas y los distritos comerciales fueron liberados.",
    "item.arlightbosses.emerald_bastion_medal": "Medalla del Bastión Esmeralda",
    "item.arlightbosses.emerald_bastion_medal.lore": "Sello obtenido al quebrar la defensa del bastión corrompido.",
    "block.arlightbosses.home_medal_pedestal": "Pedestal del Hogar Musgoso",
    "item.arlightbosses.home_medal_pedestal": "Pedestal del Hogar Musgoso",
    "block.arlightbosses.trade_medal_pedestal": "Pedestal del Comercio Dorado",
    "item.arlightbosses.trade_medal_pedestal": "Pedestal del Comercio Dorado",
    "block.arlightbosses.bastion_medal_pedestal": "Pedestal del Bastión Esmeralda",
    "item.arlightbosses.bastion_medal_pedestal": "Pedestal del Bastión Esmeralda",
}
translations_en = {
    "item.arlightbosses.mossbound_home_medal": "Mossbound Home Medal",
    "item.arlightbosses.mossbound_home_medal.lore": "Honors the hero who protected the homes within the corrupted forest.",
    "item.arlightbosses.gilded_trade_medal": "Gilded Trade Medal",
    "item.arlightbosses.gilded_trade_medal.lore": "Proof that the trade routes and merchant districts were liberated.",
    "item.arlightbosses.emerald_bastion_medal": "Emerald Bastion Medal",
    "item.arlightbosses.emerald_bastion_medal.lore": "A seal earned by breaking the corrupted bastion's defenses.",
    "block.arlightbosses.home_medal_pedestal": "Mossbound Home Pedestal",
    "item.arlightbosses.home_medal_pedestal": "Mossbound Home Pedestal",
    "block.arlightbosses.trade_medal_pedestal": "Gilded Trade Pedestal",
    "item.arlightbosses.trade_medal_pedestal": "Gilded Trade Pedestal",
    "block.arlightbosses.bastion_medal_pedestal": "Emerald Bastion Pedestal",
    "item.arlightbosses.bastion_medal_pedestal": "Emerald Bastion Pedestal",
}

for locale, additions in (("es_es", translations_es), ("en_us", translations_en)):
    path = ROOT / f"src/main/resources/assets/arlightbosses/lang/{locale}.json"
    data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    data.update(additions)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.medal_pedestal",
            "texture_width": 16,
            "texture_height": 16,
            "visible_bounds_width": 2.4,
            "visible_bounds_height": 3.0,
            "visible_bounds_offset": [0, 1.15, 0]
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "base", "parent": "root", "pivot": [0, 0, 0], "cubes": [
                {"origin": [-8, 0, -8], "size": [16, 3, 16], "uv": [0, 0]},
                {"origin": [-6, 3, -6], "size": [12, 2, 12], "uv": [0, 0]},
                {"origin": [-4, 5, -4], "size": [8, 10, 8], "uv": [0, 0]},
                {"origin": [-6, 15, -6], "size": [12, 2, 12], "uv": [0, 0]}
            ]},
            {"name": "halo", "parent": "root", "pivot": [0, 19, 0], "cubes": [
                {"origin": [-7, 18.5, -7], "size": [14, 1, 2], "uv": [0, 0]},
                {"origin": [-7, 18.5, 5], "size": [14, 1, 2], "uv": [0, 0]},
                {"origin": [-7, 18.5, -5], "size": [2, 1, 10], "uv": [0, 0]},
                {"origin": [5, 18.5, -5], "size": [2, 1, 10], "uv": [0, 0]}
            ]},
            {"name": "medal", "parent": "root", "pivot": [0, 23, 0], "cubes": [
                {"origin": [-3.5, 19.5, -1], "size": [7, 7, 2], "uv": [0, 0]},
                {"origin": [-1, 26, -0.5], "size": [2, 3, 1], "uv": [0, 0]}
            ]}
        ]
    }]
}
write("src/main/resources/assets/arlightbosses/geo/medal_pedestal.geo.json",
      json.dumps(geo, indent=2) + "\n")

animations = {
    "format_version": "1.8.0",
    "animations": {
        "animation.medal_pedestal.empty": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "medal": {"scale": [0, 0, 0]},
                "halo": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, 90, 0]}}
            }
        },
        "animation.medal_pedestal.insert": {
            "animation_length": 1.25,
            "bones": {
                "medal": {
                    "position": {"0.0": [0, -12, 0], "0.7": [0, 1.5, 0], "1.25": [0, 0, 0]},
                    "scale": {"0.0": [0.1, 0.1, 0.1], "0.7": [1.15, 1.15, 1.15], "1.25": [1, 1, 1]},
                    "rotation": {"0.0": [0, -180, 0], "1.25": [0, 0, 0]}
                },
                "halo": {"rotation": {"0.0": [0, 0, 0], "1.25": [0, 360, 0]},
                         "scale": {"0.0": [0.85, 0.85, 0.85], "0.7": [1.2, 1.2, 1.2], "1.25": [1, 1, 1]}}
            }
        },
        "animation.medal_pedestal.filled": {
            "loop": True,
            "animation_length": 3.0,
            "bones": {
                "medal": {"position": {"0.0": [0, 0, 0], "1.5": [0, 1.25, 0], "3.0": [0, 0, 0]},
                          "rotation": {"0.0": [0, 0, 0], "3.0": [0, 360, 0]}},
                "halo": {"rotation": {"0.0": [0, 0, 0], "3.0": [0, -180, 0]}}
            }
        },
        "animation.medal_pedestal.unlock": {
            "animation_length": 2.4,
            "bones": {
                "medal": {"position": {"0.0": [0, 0, 0], "1.2": [0, 5, 0], "2.4": [0, 1, 0]},
                          "rotation": {"0.0": [0, 0, 0], "2.4": [0, 720, 0]},
                          "scale": {"0.0": [1, 1, 1], "1.2": [1.35, 1.35, 1.35], "2.4": [1, 1, 1]}},
                "halo": {"rotation": {"0.0": [0, 0, 0], "2.4": [0, 1080, 0]},
                         "scale": {"0.0": [1, 1, 1], "1.2": [1.35, 1.35, 1.35], "2.4": [1, 1, 1]}},
                "base": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.35, 0], "0.5": [0, 0, 0]}}
            }
        },
        "animation.medal_pedestal.unlocked": {
            "loop": True,
            "animation_length": 2.5,
            "bones": {
                "medal": {"position": {"0.0": [0, 1, 0], "1.25": [0, 2, 0], "2.5": [0, 1, 0]},
                          "rotation": {"0.0": [0, 0, 0], "2.5": [0, 360, 0]}},
                "halo": {"rotation": {"0.0": [0, 0, 0], "2.5": [0, 360, 0]}}
            }
        }
    }
}
write("src/main/resources/assets/arlightbosses/animations/medal_pedestal.animation.json",
      json.dumps(animations, indent=2) + "\n")

block_textures = {
    "home_medal_pedestal": "minecraft:block/mossy_stone_bricks",
    "trade_medal_pedestal": "minecraft:block/exposed_cut_copper",
    "bastion_medal_pedestal": "minecraft:block/chiseled_deepslate",
}
for block_id, texture in block_textures.items():
    write(f"src/main/resources/assets/arlightbosses/blockstates/{block_id}.json",
          json.dumps({"multipart": [{"apply": {"model": f"arlightbosses:block/{block_id}"}}]}, indent=2) + "\n")
    write(f"src/main/resources/assets/arlightbosses/models/block/{block_id}.json",
          json.dumps({"parent": "minecraft:block/block", "textures": {"particle": texture}}, indent=2) + "\n")
    write(f"src/main/resources/assets/arlightbosses/models/item/{block_id}.json",
          json.dumps({"parent": f"arlightbosses:block/{block_id}"}, indent=2) + "\n")

medal_textures = {
    "mossbound_home_medal": "minecraft:item/echo_shard",
    "gilded_trade_medal": "minecraft:item/gold_ingot",
    "emerald_bastion_medal": "minecraft:item/emerald",
}
for item_id, texture in medal_textures.items():
    write(f"src/main/resources/assets/arlightbosses/models/item/{item_id}.json",
          json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": texture}}, indent=2) + "\n")

write("SOURCE_VERSION.txt", "ArlightBosses 1.22.0 OVERWORLD-MEDALS-PEDESTALS\n")
write("CAMBIOS_1.22.0.txt", """ArlightBosses 1.22.0

- Tres medallas de progresión del Overworld.
- Tres pedestales temáticos animados mediante GeckoLib.
- Estados persistentes: vacío, inserción, lleno, desbloqueo y desbloqueado.
- Recursos sin recetas; ArlightBingo controla entrega, consumo y apertura de puertas.
""")
print("ArlightBosses 1.22.0 preparado")
