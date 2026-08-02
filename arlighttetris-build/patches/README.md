# ArlightTetris 0.3.2

Mod NeoForge 1.21.1 para partidas de Tetris multijugador y pruebas individuales de administración.

## Cambios de 0.3.2

- Registro verificado en el selector de ArlightCore mediante el classloader real del plugin.
- Reintentos automáticos durante el primer minuto de arranque de Arclight.
- `/tetris core status` muestra si el proveedor quedó registrado.
- `/tetris core retry` fuerza un nuevo intento de registro.
- Modo individual para probar HUD, controles, red y arena sin una segunda cuenta.
- La prueba individual no entrega victoria ni XP.
- El HUD se limpia al salir o detener una prueba.

## Comandos normales

- `/tetris join` — entrar a la sala multijugador.
- `/tetris leave` — salir de la espera o detener tu prueba individual.
- `/tetris status` — estado de sala, jugadores, modo individual e integración con Core.

## Prueba individual — requiere OP

1. `/tetris test start`
2. Juega normalmente con las teclas configuradas.
3. `/tetris test stop` para terminar y restaurar la sesión.
4. `/tetris test restart` para reiniciar inmediatamente.

La prueba solo arranca si la sala está vacía y en estado `WAITING`.

## Arena — requiere OP

- `/tetris arena setcenter` — guarda tu posición como centro.
- `/tetris arena build` — construye la plataforma y los pods.

Mundo vacío recomendado con Multiverse y VoidGen:

```text
/mv create tetris_arena normal -g VoidGen
/mv tp tetris_arena
/tetris arena setcenter
/tetris arena build
```

## Diagnóstico de ArlightCore — requiere OP

```text
/tetris core status
/tetris core retry
```

Cuando funciona, la consola muestra:

```text
ArlightTetris registrado y verificado en el selector de ArlightCore.
```
