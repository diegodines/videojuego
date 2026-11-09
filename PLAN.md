# Tarea 2 — Shooter de gallinas: assets y plan de trabajo

**Taller de Computación Gráfica y R.E. — Primavera 2026**
**Entrega:** viernes 13 de noviembre, 23:59 hrs (Moodle) · −1.5 pts por día de atraso

**Idea del juego:** shooter en **primera persona (FPS)**. El jugador está en una granja y tiene **2 minutos** para abatir la mayor cantidad de gallinas posible. Si al terminar el tiempo abatió menos de **X** gallinas, pierde. El jugador no se ve: en pantalla aparecen solo sus brazos con el arma.

---

## 1. Qué exige el enunciado

- Un único escenario 3D explorable.
- Personaje principal controlable **con gamepad**.
- Una mecánica principal, un objetivo de 2 a 5 minutos y condiciones de victoria y derrota claras.
- Una interfaz que muestre el progreso y el estado del juego.

| Componente | Requisito mínimo |
|---|---|
| Terreno | Heightmap **distinto al de clases**, con desplazamiento sobre su superficie |
| Skybox | Ambientación propia con **imágenes nuevas** |
| Entidades | **8 tipos** de objetos estáticos distribuidos en el escenario |
| Modelos 3D | Al menos **3 modelos animados** MD3 o GLB |
| Texturizado | Texturas apropiadas y **normal mapping** en un objeto |
| Partículas | **2 efectos** distintos a los de clases, cada uno asociado a un evento |
| GUI y textos | **2 indicadores** funcionales y mensajes de victoria y derrota |
| Colisiones | **2 situaciones** de colisión con respuesta observable |

**Entrega:**
- Proyecto Java completo en .zip o .rar (puede ir en Drive).
- Informe PDF de 3 a 5 páginas: historia, nombres de los personajes, contexto, poderes, movimientos y controles del gamepad.
- Video mp4 de 2 a 4 minutos.

---

## 2. Cómo funciona el motor (importa al elegir assets)

| Pieza | Cómo está hecha hoy | Qué implica |
|---|---|---|
| Rutas | Todo se carga desde `src/main/java/org.lwjglb/assets/` y el loader **agrega `.png` / `.obj`** al nombre | Nombres en minúscula y sin espacios. Texturas **solo en PNG**: hay que convertir las JPG |
| Modelos estáticos | `objConverter/OBJFileLoader.java` lee primero todos los `v/vt/vn` y luego **un solo bloque continuo de caras `f`** | El OBJ debe estar **triangulado, con UVs y normales, en una sola malla y con un solo material**. Si hay líneas `usemtl`, `s` o `g` entre las caras, la carga falla. **Re-exportar todo desde Blender** |
| Modelos animados | `glb/GlbModel.java` usa Assimp, admite huesos (máximo 200) y texturas embebidas | Buscar **GLB con skinning y animaciones**. Un FBX de Mixamo se convierte a GLB en Blender |
| Terreno | `terrains/Terrain.java`: mide 150×150 y la altura máxima es 40. El heightmap es un PNG en grises | Heightmap nuevo, blendmap RGB y 4 texturas de suelo |
| Skybox | `skybox/SkyboxRenderer.java` carga 6 PNG: `right`, `left`, `top`, `bottom`, `back`, `front` | 6 caras nuevas de un cubemap |
| Gamepad | **No existe.** `entities/Player.java` solo lee las flechas del teclado | Hay que programarlo con `glfwGetGamepadState` de GLFW. **Es obligatorio** |
| Cámara | `entities/Camera.java` orbita detrás del jugador (tercera persona) | Hay que cambiarla a primera persona: a la altura de los ojos y con giro horizontal y vertical |
| Normal mapping | Ya funciona con barril, caja y roca | Un objeto nuevo con textura difusa y normal map |

### Lo que NO se puede reutilizar (ya está en la carpeta)
pine, fern, grass, flower, barrel, crate, boulder, rocks, person, homer (MD3), kratos, honguito, robot y las partículas star, atlas, cosmic, smoke y efecto6.

---

## 3. Lista de assets a buscar

Marquen cada casilla cuando el asset esté descargado **y cargando en el motor**.

### 3.1 Modelos animados (GLB, mínimo 3)
- [ ] **A1 — Gallina**, el objetivo con muchas copias en escena. Animaciones ideales: caminar, correr y morir (o al menos caminar). *Sketchfab (filtros Animated + Downloadable), Poly Pizza, Quaternius.*
- [ ] **A2 — Brazos con arma (vista FPS)**, lo único que se ve del jugador. Animaciones ideales: idle, disparo (retroceso) y recarga. *Sketchfab: buscar "fps arms rifle animated" o "fps arms shotgun animated". Una escopeta le queda bien a la granja.* Si no encuentran brazos animados, el arma puede ser un modelo estático y el tercer modelo animado pasa a ser A4.
- [ ] **A3 — Gallina dorada o gallo jefe**, un bonus que vale más puntos. Animaciones: caminar y correr. *Mismas fuentes que A1, o A1 recoloreado.*
- [ ] *(Opcional)* **A4 — Perro o zorro** que espanta a las gallinas.

> Antes de descargar, revisen en el visor de Sketchfab que el modelo **tenga animación** y que su licencia sea **CC-BY o CC0**. Anoten autor y link para el informe.

### 3.2 Objetos estáticos (OBJ + PNG, mínimo 8 tipos nuevos)
- [ ] 1. Cerca o valla de madera (también sirve para delimitar el mapa)
- [ ] 2. Granero
- [ ] 3. Fardo de paja
- [ ] 4. Gallinero
- [ ] 5. Molino de viento o silo
- [ ] 6. Pozo de agua
- [ ] 7. Carretilla o tractor
- [ ] 8. Árbol nuevo (manzano o roble, que no sea pino)
- [ ] 9. *(Extra)* Espantapájaros, tronco o bebedero

*Fuentes: Poly Pizza, Kenney ("Nature Kit" y "Survival Kit", CC0), Sketchfab, CGTrader (filtro gratis).*

> ⚠️ Muchos modelos low-poly de Kenney y Quaternius **no traen textura**: se colorean por material en el `.mtl` y en este motor se verían de un solo color. Busquen modelos **que incluyan un PNG/JPG y tengan UVs**, o horneen (*bake*) los colores a una textura en Blender.

### 3.3 Objeto con normal mapping (1)
- [ ] Tronco de corte, yunque o piedra de molino, con **textura difusa + normal map**. *Poly Haven, Sketchfab.*

### 3.4 Terreno
- [ ] **Heightmap:** PNG en grises de 256×256 o 512×512, con colinas suaves (una granja en un valle). *GIMP (Render → Noise), Tangram Heightmapper o L3DT.*
- [ ] **Blendmap:** PNG RGB del mismo tamaño. R = tierra, G = pasto seco, B = camino. Se pinta a mano en GIMP sobre el heightmap.
- [ ] **4 texturas de suelo** *seamless*: pasto de granja (fondo), tierra o barro, paja o pasto seco, camino de grava. *ambientCG, Poly Haven (CC0).*

### 3.5 Skybox
- [ ] 6 caras PNG de un cielo de campo soleado o un atardecer. *HDRI de Poly Haven convertido a cubemap con la herramienta web "HDRI to CubeMap" (matheowis), o skyboxes ya cortados en OpenGameArt.*

### 3.6 Partículas (2 efectos nuevos, cada uno ligado a un evento)
- [ ] **Explosión de plumas** cuando un disparo impacta una gallina. Textura: atlas de plumas de 2×2 o 4×4.
- [ ] **Fogonazo y humo** cuando el jugador dispara. Textura: atlas de *muzzle flash*.
- [ ] *(Extra)* Destello dorado cuando cae la gallina dorada.

*Fuentes: Kenney Particle Pack (CC0), OpenGameArt. Las plumas también se pueden dibujar en GIMP.*

### 3.7 GUI / HUD (PNG con transparencia)
- [ ] Mira (crosshair)
- [ ] Ícono de gallina (contador de bajas)
- [ ] Ícono de reloj (tiempo restante)
- [ ] Paneles de fondo para "¡VICTORIA!" y "DERROTA"
- [ ] *(Opcional)* Ícono de munición

*Fuentes: Kenney UI Pack, game-icons.net (CC-BY). Para el texto se puede reutilizar la fuente `candara` o `arial`. Si quieren una nueva, se genera con Hiero (libGDX) en formato `.fnt`.*

### 3.8 Proyectil
- [ ] Bala, perdigón o **huevo**: basta una esfera low-poly con textura.

---

## 4. Diseño del juego

- **Objetivo:** abatir **X = 25 gallinas en 120 s** (ajustar después de probar). Al terminar el tiempo, si hay al menos X bajas es **victoria**; si no, **derrota**.
- **Gallinas:** aparecen dentro de la granja, caminan al azar y **huyen** cuando el jugador se acerca. Al morir reaparecen en otro punto. La gallina dorada vale 3.
- **Controles del gamepad** (se mantiene el teclado como respaldo para depurar):

| Control | Acción |
|---|---|
| Stick izquierdo | Avanzar, retroceder y moverse de lado (*strafe*) |
| Stick derecho | Mirar y apuntar (horizontal y vertical) |
| RT | Disparar |
| A | Saltar |
| X | Recargar *(opcional)* |
| Start | Pausa o reiniciar |

- **Vista FPS:** la cámara va a la altura de los ojos del jugador y el giro vertical se limita a unos ±80°. Los brazos con el arma se dibujan al final, pegados a la cámara (abajo a la derecha), limpiando antes el buffer de profundidad para que no atraviesen paredes ni gallinas. La mira queda fija en el centro de la pantalla.
- **Colisiones** (se piden 2, con respuesta visible):
  1. **Disparo contra gallina:** el disparo sale desde el centro de la cámara, en la dirección de la mira, y se prueba contra la esfera de cada gallina. La gallina muere, salen plumas y sube el contador.
  2. **Jugador contra obstáculos** (cerca, granero, fardos): círculo contra caja (AABB). El jugador queda bloqueado o es empujado hacia atrás.
  3. *(Extra)* Jugador contra borde del mapa, y gallina contra cerca, que la hace rebotar.
- **HUD:** temporizador `01:43`, contador `Gallinas: 12 / 25`, mira, y mensaje final de victoria o derrota con "Start para reiniciar".
- **Idea de historia para el informe:** *Don Ramiro, granjero de Ñuble, tiene 2 minutos para recuperar su granja antes de que la plaga de gallinas rebeldes se coma toda la cosecha.*

---

## 5. Plan de programación

Clases nuevas propuestas, en un paquete `game/`:
- `Gamepad`: lectura de input
- `FpsCamera`: cámara en primera persona (reemplaza el uso de `Camera`)
- `WeaponView`: dibuja los brazos con el arma pegados a la cámara y elige su animación
- `GameState`: estados `PLAYING`, `WIN`, `LOSE` y el temporizador
- `Chicken`: posición, estado, IA y radio de colisión
- `Projectile`
- `CollisionSystem`
- `Hud`

| Fase | Tarea | Requisito que cubre | Quién |
|---|---|---|---|
| 1 | Bajar y convertir assets (Blender: triangular, una malla, exportar OBJ/GLB) y probar que cada uno carga | Todos | Ambos |
| 2 | Escenario: heightmap, blendmap, texturas de suelo, skybox nuevo y distribución de los 8 tipos de estáticos + el objeto con normal map | Terreno, Skybox, Entidades, Texturizado | Persona A |
| 3 | Gamepad y cámara FPS: `Gamepad.java` con GLFW, `FpsCamera` a la altura de los ojos, mirar con el stick derecho y moverse con *strafe* con el izquierdo. Dejar de dibujar `person.obj` | Gamepad | Persona B |
| 4 | Arma en pantalla: `WeaponView` dibuja los brazos con el arma pegados a la cámara y cambia la animación (idle, disparo, recarga). Mira fija en el centro | Modelos 3D | Persona B |
| 5 | Gallinas: N instancias del GLB, IA de deambular y huir, y reaparición | Modelos 3D, mecánica | Persona A |
| 6 | Disparo y colisiones: proyectil, impacto con gallina y bloqueo contra obstáculos | Colisiones | Persona B |
| 7 | Partículas de plumas (impacto) y de fogonazo (disparo) | Partículas | Persona A |
| 8 | HUD, temporizador, estados de victoria/derrota y reinicio | GUI y textos, condiciones | Persona B |
| 9 | Balancear X, velocidad y cantidad de gallinas; pulir | — | Ambos |
| 10 | Grabar el video de 2–4 min y escribir el informe de 3–5 págs (historia, personajes, controles, fuentes de assets) | Entrega | Ambos |

### Calendario

| Día | Qué |
|---|---|
| Dom 8 / Lun 9 | Fase 1: todos los assets cargando |
| Mar 10 | Fases 2 y 3 en paralelo |
| Mié 11 | Fases 4, 5 y 6 |
| Jue 12 | Fases 7 y 8 |
| Vie 13 | Fases 9 y 10, y entrega temprano |

### Riesgos
- **Rendimiento con muchas gallinas GLB:** actualizar la animación una sola vez por modelo y dibujarla con distintas matrices. Empezar con unas 15 en pantalla.
- **Escala y orientación de cada GLB:** cada modelo viene a otra escala. Normalizar en Blender o usar `getRecommendedScale()`.
- **Arma atravesando objetos:** si no se limpia el buffer de profundidad antes de dibujar los brazos, el arma se mete dentro de paredes y gallinas cercanas.
- **Personaje que no se ve:** el enunciado pide un personaje principal. En el informe y el video hay que presentarlo igual (nombre, historia y controles), aunque en pantalla solo aparezcan sus brazos.
- **No hay audio** en el motor (falta la dependencia OpenAL). Queda como mejora futura.

---

## 6. Créditos de assets (completar al descargar)

| Asset | Autor | Fuente / link | Licencia |
|---|---|---|---|
| | | | |
