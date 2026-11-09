package org.lwjglb.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjglb.entities.Camera;
import org.lwjglb.entities.Entity;
import org.lwjglb.entities.Light;
import org.lwjglb.entities.Player;
import org.lwjglb.fontMeshCreator.FontType;
import org.lwjglb.fontRendering.TextMaster;
import org.lwjglb.glb.GlbModel;
import org.lwjglb.models.TexturedModel;
import org.lwjglb.normalMappingObjConverter.NormalMappedObjLoader;
import org.lwjglb.particles.ParticleMaster;
import org.lwjglb.particles.ParticleSystem;
import org.lwjglb.particles.ParticleTexture;
import org.lwjglb.renderEngine.Loader;
import org.lwjglb.renderEngine.MasterRenderer;
import org.lwjglb.terrains.Terrain;
import org.lwjglb.textures.ModelTexture;
import org.lwjglb.textures.TerrainTexture;
import org.lwjglb.textures.TerrainTexturePack;
import org.lwjglb.util.Config;
import org.lwjglb.util.Maths;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * "La granja de Don Ramiro": shooter en primera persona. El jugador tiene 2 minutos para abatir
 * al menos GOAL gallinas. Esta clase arma el escenario y corre la lógica de cada frame.
 */
public class Game {

    // Reglas
    public static final int GOAL = 25;
    public static final float ROUND_TIME = 120f;
    private static final int REGULAR_CHICKENS = 15;
    private static final int MAX_SHELLS = 6;

    // Límites del mapa (terreno de 150 x 150 en x=[0,150], z=[-150,0])
    private static final float PLAYER_MIN = 4, PLAYER_MAX = 146;
    private static final float CHICKEN_MIN = 9, CHICKEN_MAX = 141;
    private static final Vector3f PLAYER_START = new Vector3f(75, 0, -40);

    private final Loader loader;
    private final MasterRenderer renderer;
    private final Random rng = new Random();

    private final Terrain terrain;
    private final List<Terrain> terrains = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> normalMapEntities = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Prop> props = new ArrayList<>();

    private final Player player;
    private final Camera camera;
    private final Input input = new Input();

    private final GlbModel chickenModel, goldenModel;
    private final Matrix4f chickenNormalize, goldenNormalize;
    private final int chickenWalk, chickenRun, chickenIdle, chickenFall;
    private final List<Chicken> chickens = new ArrayList<>();

    private final TexturedModel bulletModel;
    private final List<Bullet> bullets = new ArrayList<>();
    private final WeaponView weapon;

    private final ParticleSystem feathers, muzzleFlash, goldSparks;
    private final Hud hud;

    private GameState state = GameState.START;
    private float timeLeft = ROUND_TIME;
    private int score = 0;
    private int shells = MAX_SHELLS;
    private float reloadTimer = 0;
    private float fireCooldown = 0;

    /** Objeto estático cargado desde GLB, con su matriz de modelo ya calculada. */
    private static class Prop {
        final GlbModel model;
        final Matrix4f matrix;
        Prop(GlbModel model, Matrix4f matrix) { this.model = model; this.matrix = matrix; }
    }

    public Game(Loader loader, MasterRenderer renderer) throws IOException {
        this.loader = loader;
        this.renderer = renderer;

        lights.add(new Light(new Vector3f(10000, 15000, 6000), new Vector3f(1.1f, 1.1f, 1.05f)));

        // Terreno: heightmap y blendmap propios (barro, paja y camino de grava sobre pasto)
        TerrainTexturePack pack = new TerrainTexturePack(
                new TerrainTexture(loader.loadTexture("granjaPasto")),
                new TerrainTexture(loader.loadTexture("granjaBarro")),
                new TerrainTexture(loader.loadTexture("granjaPaja")),
                new TerrainTexture(loader.loadTexture("granjaGrava")));
        terrain = new Terrain(0, -1, loader, pack,
                new TerrainTexture(loader.loadTexture("blendMapGranja")), "heightMapGranja");
        terrains.add(terrain);

        player = new Player(new Vector3f(PLAYER_START), 0);
        camera = new Camera(player);

        buildScenery();

        // Modelos animados
        chickenModel = loadGlb("gallina.glb");
        goldenModel = loadGlb("gallina_dorada.glb");
        chickenNormalize = chickenModel.groundedMatrix(Chicken.HEIGHT);
        goldenNormalize = goldenModel.groundedMatrix(Chicken.HEIGHT * 1.15f);
        chickenWalk = Math.max(0, chickenModel.findAnimation("walk"));
        chickenRun = indexOr(chickenModel.findAnimation("run"), chickenWalk);
        chickenIdle = indexOr(chickenModel.findAnimation("idle"), chickenWalk);
        chickenFall = indexOr(chickenModel.findAnimation("fall"), chickenIdle);
        weapon = new WeaponView(loadGlb("brazos/fusilpompe.fbx"));

        bulletModel = loader.createTexturedModel("bala", "bala", 10, 0.6f);

        // Partículas: plumas al impactar, fogonazo al disparar y destellos al cazar la dorada
        ParticleMaster.init(loader, renderer.getProjectionMatrix());
        feathers = new ParticleSystem(new ParticleTexture(loader.loadTexture("particulaPluma"), 2),
                0, 6f, 0.08f, 1.4f, 0.9f);
        feathers.setDirection(new Vector3f(0, 1, 0), 0.6f);
        feathers.setSpeedError(0.6f);
        feathers.setLifeError(0.4f);
        feathers.setScaleError(0.4f);
        feathers.randomizeRotation();
        muzzleFlash = new ParticleSystem(new ParticleTexture(loader.loadTexture("particulaFogonazo"), 2),
                0, 1.2f, 0f, 0.15f, 0.22f);
        muzzleFlash.setScaleError(0.3f);
        muzzleFlash.randomizeRotation();
        goldSparks = new ParticleSystem(new ParticleTexture(loader.loadTexture("particulaDorada"), 2),
                0, 5f, -0.02f, 1.6f, 0.8f);
        goldSparks.setScaleError(0.5f);
        goldSparks.setSpeedError(0.5f);
        goldSparks.randomizeRotation();

        FontType font = new FontType(loader.loadTexture("candara"),
                new File(new Config().getPath() + "assets" + File.separator + "candara.fnt"));
        hud = new Hud(loader, font);
        hud.showStart(GOAL, (int) ROUND_TIME);

        resetRound();
    }

    private static int indexOr(int index, int fallback) {
        return index >= 0 ? index : fallback;
    }

    private GlbModel loadGlb(String name) throws IOException {
        GlbModel m = GlbModel.load(new Config().getPath() + "models" + File.separator + name);
        m.crearShader();
        return m;
    }

    // ------------------------------------------------------------------ escenario
    private void buildScenery() throws IOException {
        // Granero y tractor (OBJ con textura)
        TexturedModel barn = loader.createTexturedModel("granero", "granero", 10, 0.1f);
        addEntity(barn, 118, -118, 225, 0.12f, 12.5f, 14f);
        TexturedModel tractor = loader.createTexturedModel("tractor", "tractor", 8, 0.4f);
        addEntity(tractor, 88, -100, 30, 0.45f, 3.8f, 4f);

        // Troncos con normal mapping (textura + mapa de normales)
        TexturedModel log = new TexturedModel(NormalMappedObjLoader.loadOBJ("troncoNM", loader),
                new ModelTexture(loader.loadTexture("tronco")));
        log.getTexture().setNormalMap(loader.loadTexture("troncoNormal"));
        log.getTexture().setShineDamper(12);
        log.getTexture().setReflectivity(0.25f);
        float[][] logs = {{60, -60, 20}, {100, -70, 80}, {35, -80, 150}, {70, -125, 10}, {125, -85, 60}};
        for (float[] l : logs) {
            float y = terrain.getHeightOfTerrain(l[0], l[1]) + 0.2f;
            normalMapEntities.add(new Entity(log, new Vector3f(l[0], y, l[1]), 0, l[2], 0, 1.6f));
            obstacles.add(new Obstacle(l[0], l[1], 1.6f, 1f));
        }

        // Objetos GLB
        GlbModel tree = loadGlb("arbol.glb");
        GlbModel scarecrow = loadGlb("espantapajaros.glb");
        GlbModel hay = loadGlb("fardo.glb");
        GlbModel well = loadGlb("pozo.glb");
        GlbModel fence = loadGlb("valla.glb");

        Random layout = new Random(20261113);   // distribución fija para que el mapa sea siempre igual
        // arbol.glb trae una hilera de 5 árboles: se ubican hileras cerca de los bordes,
        // mirando hacia el centro, y se agrega un obstáculo por cada tronco
        float[][] groves = {{20, -20, 45}, {75, -10, 0}, {135, -15, -40}, {12, -75, 90},
                            {140, -60, 90}, {25, -135, 135}, {75, -142, 180}};
        for (float[] g : groves) {
            float h = 10 + layout.nextFloat() * 3;
            addProp(tree, g[0], g[1], g[2], h, 0);
            float width = (tree.getPoseMax().x - tree.getPoseMin().x) * h / (tree.getPoseMax().y - tree.getPoseMin().y);
            double rot = Math.toRadians(g[2]);
            for (int k = -2; k <= 2; k++) {
                float off = k * width / 5f;
                obstacles.add(new Obstacle(g[0] + (float) Math.cos(rot) * off, g[1] - (float) Math.sin(rot) * off, 0.7f, h));
            }
        }
        addProp(scarecrow, 30, -110, 30, 4.2f, 0.25f);
        addProp(scarecrow, 120, -40, 200, 4.2f, 0.25f);
        addProp(scarecrow, 55, -20, 120, 4.2f, 0.25f);
        addProp(well, 45, -45, 0, 4f, 0.8f);
        float[][] hayBales = {{92, -110}, {96, -114}, {90, -120}, {100, -122}, {104, -108}, {85, -106},
                              {20, -60}, {130, -20}, {65, -95}};
        for (float[] h : hayBales) addProp(hay, h[0], h[1], layout.nextFloat() * 90, 1.6f, 0.9f);

        // Cerca alrededor del mapa (solo decorativa: el límite real lo dan PLAYER_MIN/MAX)
        float len = 9f;
        Matrix4f fenceNorm = fence.groundedMatrix(1.5f);
        float fenceScale = len / ((fence.getPoseMax().x - fence.getPoseMin().x) * (1.5f /
                (fence.getPoseMax().y - fence.getPoseMin().y)));
        for (float p = 2 + len / 2; p < 148; p += len) {
            addFence(fence, fenceNorm, fenceScale, p, -2, 0);
            addFence(fence, fenceNorm, fenceScale, p, -148, 0);
            addFence(fence, fenceNorm, fenceScale, 2, -p, 90);
            addFence(fence, fenceNorm, fenceScale, 148, -p, 90);
        }
    }

    private void addEntity(TexturedModel model, float x, float z, float rotY, float scale,
                           float radius, float height) {
        float y = terrain.getHeightOfTerrain(x, z);
        entities.add(new Entity(model, new Vector3f(x, y, z), 0, rotY, 0, scale));
        obstacles.add(new Obstacle(x, z, radius, height));
    }

    /** Agrega un objeto GLB apoyado en el terreno. radiusFactor ajusta el radio de colisión (0 = sin colisión). */
    private void addProp(GlbModel model, float x, float z, float rotY, float height, float radiusFactor) {
        float y = terrain.getHeightOfTerrain(x, z);
        Matrix4f m = new Matrix4f().translate(x, y, z)
                .rotateY((float) Math.toRadians(rotY))
                .mul(model.groundedMatrix(height));
        props.add(new Prop(model, m));
        if (radiusFactor > 0) {
            obstacles.add(new Obstacle(x, z, model.groundRadius(height) * radiusFactor, height));
        }
    }

    private void addFence(GlbModel fence, Matrix4f norm, float stretch, float x, float z, float rotY) {
        float y = terrain.getHeightOfTerrain(x, z);
        Matrix4f m = new Matrix4f().translate(x, y, z)
                .rotateY((float) Math.toRadians(rotY))
                .scale(stretch, 1, 1)
                .mul(norm);
        props.add(new Prop(fence, m));
    }

    // ------------------------------------------------------------------ partida
    private void resetRound() {
        score = 0;
        timeLeft = ROUND_TIME;
        shells = MAX_SHELLS;
        reloadTimer = 0;
        fireCooldown = 0;
        bullets.clear();
        chickens.clear();
        player.reset(new Vector3f(PLAYER_START.x, terrain.getHeightOfTerrain(PLAYER_START.x, PLAYER_START.z),
                PLAYER_START.z), 0);
        for (int i = 0; i < REGULAR_CHICKENS; i++) chickens.add(spawnChicken(false));
        chickens.add(spawnChicken(true));
    }

    private Chicken spawnChicken(boolean golden) {
        Vector3f p = new Vector3f();
        for (int tries = 0; tries < 50; tries++) {
            p.x = CHICKEN_MIN + rng.nextFloat() * (CHICKEN_MAX - CHICKEN_MIN);
            p.z = -CHICKEN_MAX + rng.nextFloat() * (CHICKEN_MAX - CHICKEN_MIN);
            boolean blocked = p.distance(player.getPosition().x, p.y, player.getPosition().z) < 25;
            for (Obstacle o : obstacles) {
                if (o.pushOut(new Vector3f(p), 0.6f)) { blocked = true; break; }
            }
            if (!blocked) break;
        }
        p.y = terrain.getHeightOfTerrain(p.x, p.z);
        return new Chicken(p, golden, rng);
    }

    public void update(float dt) {
        dt = Math.min(dt, 0.05f);   // evita saltos grandes si un frame se demora
        input.setMouseCapture(state == GameState.PLAYING);
        input.update();

        switch (state) {
            case START:
            case WON:
            case LOST:
                if (input.startPressed()) {
                    resetRound();
                    state = GameState.PLAYING;
                }
                break;
            case PLAYING:
                updatePlaying(dt);
                break;
        }

        camera.move();
        ParticleMaster.update(camera);
    }

    private void updatePlaying(float dt) {
        player.update(dt, input, terrain, obstacles, PLAYER_MIN, PLAYER_MAX, -PLAYER_MAX, -PLAYER_MIN);
        weapon.update(dt);

        // Escopeta: disparo, cadencia y recarga
        fireCooldown -= dt;
        if (reloadTimer > 0) {
            reloadTimer -= dt;
            if (reloadTimer <= 0) shells = MAX_SHELLS;
        } else if (input.reloadPressed() && shells < MAX_SHELLS) {
            startReload();
        } else if (input.isFireHeld() && fireCooldown <= 0 && shells > 0) {
            shoot();
            if (shells == 0) startReload();
        }

        // Gallinas
        Iterator<Chicken> it = chickens.iterator();
        List<Chicken> respawn = new ArrayList<>();
        while (it.hasNext()) {
            Chicken c = it.next();
            c.update(dt, terrain, player.getPosition(), obstacles,
                    CHICKEN_MIN, CHICKEN_MAX, -CHICKEN_MAX, -CHICKEN_MIN, rng);
            if (c.isDead()) {
                it.remove();
                respawn.add(spawnChicken(c.isGolden()));
            }
        }
        chickens.addAll(respawn);

        // Balas. Colisión 1: bala contra gallina (segmento recorrido contra esfera)
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            b.update(dt);
            for (Chicken c : chickens) {
                if (c.isHitBy(b.getPrevious(), b.getPosition())) {
                    c.kill();
                    score += c.isGolden() ? 3 : 1;
                    feathers.emitBurst(c.getCenter(), c.isGolden() ? 30 : 22);
                    if (c.isGolden()) goldSparks.emitBurst(c.getCenter(), 40);
                    b.expire();
                    break;
                }
            }
            Vector3f p = b.getPosition();
            float ground = terrain.getHeightOfTerrain(p.x, p.z);
            if (p.y < ground) b.expire();
            for (Obstacle o : obstacles) {
                if (o.contains(p, ground)) { b.expire(); break; }
            }
            if (b.isExpired()) bi.remove();
        }

        // Reloj
        timeLeft -= dt;
        if (timeLeft <= 0) {
            timeLeft = 0;
            state = score >= GOAL ? GameState.WON : GameState.LOST;
            hud.showResult(state == GameState.WON, score, GOAL);
        }
    }

    private void startReload() {
        reloadTimer = weapon.getReloadDuration();
        weapon.playReload();
    }

    private void shoot() {
        shells--;
        fireCooldown = Math.max(0.45f, weapon.getFireDuration() * 0.8f);
        weapon.playFire();
        Vector3f dir = player.getLookDirection();
        Vector3f eye = player.getEyePosition();
        bullets.add(new Bullet(bulletModel, new Vector3f(eye).fma(0.6f, dir), dir));
        // Fogonazo en la punta del cañón (un poco a la derecha y abajo de la vista)
        double yaw = Math.toRadians(player.getYaw());
        Vector3f right = new Vector3f((float) Math.cos(yaw), 0, (float) Math.sin(yaw));
        Vector3f muzzle = new Vector3f(eye).fma(1.6f, dir).fma(0.22f, right).add(0, -0.18f, 0);
        muzzleFlash.emitBurst(muzzle, 5);
    }

    // ------------------------------------------------------------------ dibujo
    public void render() {
        List<Entity> frameEntities = new ArrayList<>(entities);
        for (Bullet b : bullets) frameEntities.add(b.getEntity());
        renderer.renderScene(frameEntities, normalMapEntities, terrains, lights, camera,
                new Vector4f(0, -1, 0, 100000));

        Matrix4f vp = new Matrix4f(renderer.getProjectionMatrix()).mul(Maths.createViewMatrix(camera));
        MasterRenderer.disableCulling();
        GlbModel current = null;
        for (Prop p : props) {
            if (p.model != current) {
                if (current != null) current.end();
                current = p.model;
                current.begin();
            }
            current.render(vp, p.matrix);
        }
        if (current != null) current.end();
        renderChickens(vp, false, chickenModel, chickenNormalize);
        renderChickens(vp, true, goldenModel, goldenNormalize);
        MasterRenderer.enableCulling();

        ParticleMaster.renderParticles(camera);

        if (state == GameState.PLAYING) {
            MasterRenderer.disableCulling();
            weapon.render(renderer.getProjectionMatrix(), camera, player.isMoving());
            MasterRenderer.enableCulling();
            hud.showPlaying(score, GOAL, timeLeft, shells, MAX_SHELLS, reloadTimer > 0);
        }
        hud.render(state);
        TextMaster.render();
    }

    private void renderChickens(Matrix4f vp, boolean golden, GlbModel model, Matrix4f normalize) {
        model.begin();
        for (Chicken c : chickens) {
            if (c.isGolden() != golden) continue;
            int anim;
            switch (c.getState()) {
                case FLEE: anim = chickenRun; break;
                case IDLE: anim = chickenIdle; break;
                case DYING: anim = chickenFall; break;
                default: anim = chickenWalk;
            }
            model.setAnimationState(anim, c.getAnimTime());
            model.render(vp, c.modelMatrix(normalize));
        }
        model.end();
    }

    // Acceso para pruebas automáticas
    public Input getInput() { return input; }
    public GameState getState() { return state; }
    public void forceStart() { resetRound(); state = GameState.PLAYING; }
    public Player getPlayer() { return player; }

    /** Solo para la prueba automática: apunta a la gallina viva más cercana. */
    public void debugAimAtNearestChicken() {
        Chicken best = null;
        float bestD = Float.MAX_VALUE;
        Vector3f eye = player.getEyePosition();
        for (Chicken c : chickens) {
            float d = c.getCenter().distance(eye);
            if (c.isAlive() && d < bestD) { bestD = d; best = c; }
        }
        if (best == null) return;
        Vector3f to = best.getCenter().sub(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(to.x, -to.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        player.setLook(yaw, pitch);
    }

    public int getScore() { return score; }

    /** Solo para la prueba automática: deja 0,2 segundos de juego para ver la pantalla final. */
    public void debugEndRound() {
        timeLeft = 0.2f;
    }

    public void cleanUp() {
        ParticleMaster.cleanUp();
        hud.cleanUp();
    }
}
