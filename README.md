# videojuego — La granja de Don Ramiro

Shooter en primera persona hecho sobre el motor LWJGL de clases (Tarea 2, Computación Gráfica 2026).
El jugador tiene **2 minutos** para abatir al menos **25 gallinas** en la granja. La gallina dorada vale 3.

## Cómo ejecutar
Abrir el proyecto en IntelliJ (Maven) y ejecutar `org.lwjglb.engineTester.MainGameLoop`.
El directorio de trabajo debe ser la raíz del proyecto (las rutas de assets parten en `src\main\java\org.lwjglb\`).

## Controles
| Acción | Gamepad (tipo Xbox) | Mouse y teclado |
|---|---|---|
| Moverse | Stick izquierdo | W A S D |
| Mirar / apuntar | Stick derecho | Mouse |
| Disparar | RT (o RB) | Clic izquierdo o Espacio |
| Recargar | X | R |
| Saltar | A | Shift izquierdo |
| Comenzar / reintentar | START | Enter |

Gamepad y mouse funcionan a la vez. Mientras se juega el cursor queda oculto y capturado por la ventana;
Esc lo suelta y un clic en la ventana lo vuelve a capturar.

## Requisitos de la tarea y dónde están
| Requisito | Implementación |
|---|---|
| Terreno propio | `heightMapGranja.png` + `blendMapGranja.png` (pasto, barro, paja, grava) |
| Skybox nuevo | 6 caras de Poly Haven en `assets/right…front.png` |
| 8 tipos de objetos estáticos | granero, tractor (OBJ); árboles, espantapájaros, fardo, pozo, cerca (GLB); troncos (OBJ con normal map) |
| 3 modelos animados | gallina, gallina dorada (GLB) y escopeta (FBX vía Assimp; se dibuja solo la escopeta, sin los brazos) |
| Normal mapping | troncos (`troncoNM.obj` + `troncoNormal.png`) |
| 2 efectos de partículas por evento | plumas al impactar, fogonazo al disparar (y destellos al abatir la dorada) |
| GUI | contador de gallinas, reloj, cartuchos, mira; pantallas de inicio, victoria y derrota |
| 2 colisiones | bala contra gallina (segmento contra esfera) y jugador contra obstáculos (círculo contra círculo); además las gallinas rebotan en obstáculos y la cerca |

## Código del juego
Todo lo nuevo está en `src/main/java/org.lwjglb/game/`:
- `Game` — arma el escenario y corre la lógica de cada frame (estados, disparo, colisiones, reloj).
- `Input` — gamepad (API de gamepads de GLFW) con teclado de respaldo.
- `Chicken` — IA simple (deambular, quedarse quieta, huir del jugador) y muerte animada.
- `Bullet`, `Obstacle`, `WeaponView` (escopeta pegada a la cámara, con retroceso al disparar), `Hud`, `GameState`.
- `ScreenCapture` — solo para pruebas: con la variable de entorno `GRANJA_CAPTURA=ruta` el juego se juega solo y guarda capturas.

Se modificaron del motor: `Player` y `Camera` (ahora en primera persona), `GlbModel` (animación por instancia,
búsqueda de texturas de FBX, apoyo en el suelo), `ParticleSystem` (ráfagas) y el contorno del texto.

Ver `PLAN.md` para el plan y los créditos de los assets.
