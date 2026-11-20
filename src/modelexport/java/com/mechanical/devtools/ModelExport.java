package com.mechanical.devtools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Dev-time tool (never in the mod jar): bakes every entity model layer the mod registers, plus the vanilla models the mod
 * reskins, poses it with the model's own setupAnim on a default render state, and writes the drawn quads to JSON for
 * scripts/gen_registry.py. Runs without a game, GL context or bootstrap (FML has no loader here, so a
 * setupAnim that touches an entity class, like ReindeerModel, falls back to the baked rest pose): {@code ./gradlew exportModels}.
 * <p>Layers are found by calling every static method taking a {@code RegisterLayerDefinitions} event in the mod's classes,
 * so new models are picked up automatically. Positions are in model pixels (y down), UVs in texture pixels.
 */
public final class ModelExport {
    record Layer(String id, Supplier<LayerDefinition> def, String modelClass) {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Map<String, Layer> layers = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        Map<String, Class<?>> modelOf = new LinkedHashMap<>(); // layer id -> class that declares its ModelLayerLocation
        Map<String, String> fields = new LinkedHashMap<>(); // "FooModel.LAYER" -> layer id, for matching renderer source code
        Path assets = Path.of(args[1], "assets");
        if (Files.isDirectory(assets)) try (Stream<Path> s = Files.walk(assets)) {
            s.map(p -> assets.relativize(p).toString().replace('\\', '/')).filter(p -> p.matches("[^/]+/textures/entity/.*\\.png"))
                    .sorted().forEach(p -> ENTITY_TEXTURES.add(p.substring(0, p.indexOf('/')) + ":" + p.substring(p.indexOf('/') + 1)));
        }
        for (int i = 2; i < args.length; i++) {
            Path dir = Path.of(args[i]);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files;
            try (Stream<Path> s = Files.walk(dir)) {
                files = s.filter(p -> p.toString().endsWith(".class")).toList();
            }
            for (Path f : files) {
                String bytes = new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1);
                boolean registers = bytes.contains("RegisterLayerDefinitions"), declares = bytes.contains("ModelLayerLocation");
                if (!registers && !declares) continue;
                String name = dir.relativize(f).toString().replace('\\', '/').replace('/', '.').replaceAll("\\.class$", "");
                try {
                    Class<?> c = Class.forName(name, false, ModelExport.class.getClassLoader());
                    if (declares) for (Field fl : c.getDeclaredFields()) {
                        if (Modifier.isStatic(fl.getModifiers()) && fl.getType() == ModelLayerLocation.class) {
                            fl.setAccessible(true);
                            modelOf.putIfAbsent(fl.get(null).toString(), c);
                            fields.put(c.getSimpleName() + "." + fl.getName(), fl.get(null).toString());
                        }
                    }
                    if (registers) for (Method m : c.getDeclaredMethods()) {
                        if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
                                && m.getParameterTypes()[0] == EntityRenderersEvent.RegisterLayerDefinitions.class) {
                            m.setAccessible(true);
                            m.invoke(null, new EntityRenderersEvent.RegisterLayerDefinitions() {
                                @Override
                                public void registerLayerDefinition(ModelLayerLocation loc, Supplier<LayerDefinition> supplier) {
                                    layers.put(loc.toString(), new Layer(loc.toString(), supplier, null));
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    errors.add(name + ": " + t);
                }
            }
        }

        StringBuilder json = new StringBuilder("{\"layers\":{");
        boolean first = true;
        for (Layer l : layers.values()) {
            Class<?> cls = modelOf.get(l.id());
            first = write(json, first, l.id(), cls, l.def(), errors);
        }
        // vanilla models the mod reskins (undead skins, tribespeople), built like LayerDefinitions does
        first = write(json, first, "minecraft:zombie#main", ZombieModel.class,
                () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64), errors);
        first = write(json, first, "minecraft:skeleton#main", SkeletonModel.class, SkeletonModel::createBodyLayer, errors);
        first = write(json, first, "minecraft:player#main", PlayerModel.class,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64), errors);
        write(json, first, "minecraft:player_slim#main", PlayerModel.class,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64), errors);
        json.append("},\"fields\":{");
        int k = 0;
        for (Map.Entry<String, String> e : fields.entrySet()) json.append(k++ > 0 ? "," : "").append(str(e.getKey())).append(":").append(str(e.getValue()));
        json.append("},\"errors\":[");
        for (int i = 0; i < errors.size(); i++) json.append(i > 0 ? "," : "").append(str(errors.get(i)));
        json.append("]}");
        Files.createDirectories(out.getParent());
        Files.writeString(out, json, StandardCharsets.UTF_8);
        System.out.println("Exported " + layers.size() + " mod layers + 4 vanilla to " + out + (errors.isEmpty() ? "" : ", " + errors.size() + " errors:"));
        errors.forEach(e -> System.out.println("  " + e));
    }

    private static boolean write(StringBuilder json, boolean first, String id, Class<?> cls, Supplier<LayerDefinition> def, List<String> errors) {
        try {
            LayerDefinition layer = def.get();
            float[] tex = texSize(layer);
            Map<String, String> poses = new LinkedHashMap<>();
            poses.put("default", quads(posed(layer.bakeRoot(), cls, id, null, null, errors), tex));
            // models that read the texture (reindeer bull/cow) or showsHorns get one pose per combination that differs from default
            Class<?> st = cls == null || !Model.class.isAssignableFrom(cls) ? null : stateClass(cls);
            List<String> texes = new ArrayList<>();
            if (hasField(st, "texture")) {
                String path = id.substring(id.indexOf(':') + 1, id.indexOf('#'));
                for (String t : ENTITY_TEXTURES) {
                    String stem = t.substring(t.lastIndexOf('/') + 1, t.length() - 4);
                    if (stem.equals(path) || stem.startsWith(path + "_")) texes.add(t);
                }
            }
            if (texes.isEmpty()) texes.add(null);
            List<Boolean> horns = hasField(st, "showsHorns") ? List.of(true, false) : java.util.Collections.singletonList(null);
            for (String t : texes)
                for (Boolean h : horns) {
                    if (t == null && h == null) continue;
                    String q = quads(posed(layer.bakeRoot(), cls, id, t, h, errors), tex);
                    if (!q.equals(poses.get("default"))) poses.put((t == null ? "" : t) + (h == null ? "" : h ? "|horns" : "|nohorns"), q);
                }
            json.append(first ? "" : ",").append(str(id)).append(":{\"cls\":").append(str(cls == null ? "" : cls.getSimpleName()))
                    .append(",\"tw\":").append(fmt(tex[0])).append(",\"th\":").append(fmt(tex[1])).append(",\"poses\":{");
            boolean f = true;
            for (Map.Entry<String, String> e : poses.entrySet()) {
                json.append(f ? "" : ",").append(str(e.getKey())).append(":").append(e.getValue());
                f = false;
            }
            json.append("}}");
            return false;
        } catch (Throwable t) {
            errors.add(id + ": " + t);
            return first;
        }
    }

    /** Builds the model class around the baked root and runs its setupAnim on a fresh render state, so rest poses and
     *  visibility rules (antlers only on stags ...) apply. Falls back to the raw baked pose when that fails. */
    private static ModelPart posed(ModelPart root, Class<?> cls, String id, String texture, Boolean horns, List<String> errors) {
        if (cls == null || !Model.class.isAssignableFrom(cls) || Modifier.isAbstract(cls.getModifiers())) return root;
        try {
            Model<Object> model = null;
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 0 || p[0] != ModelPart.class) continue;
                Object[] a = new Object[p.length];
                a[0] = root;
                boolean ok = true;
                for (int i = 1; i < p.length; i++) {
                    if (p[i] == float.class) a[i] = 1.0F;
                    else if (p[i] == boolean.class) a[i] = id.contains("slim");
                    else if (p[i] == int.class) a[i] = 0;
                    else ok = false;
                }
                if (!ok) continue;
                c.setAccessible(true);
                @SuppressWarnings("unchecked") Model<Object> m = (Model<Object>) c.newInstance(a);
                model = m;
                break;
            }
            if (model == null) return root;
            Class<?> type = stateClass(cls);
            for (int tries = 0; ; tries++) {
                Object state = newState(type);
                if (state == null) return root;
                setField(state, "showsHorns", horns == null || horns);
                if (texture != null) setField(state, "texture", net.minecraft.resources.Identifier.parse(texture));
                try {
                    model.setupAnim(state);
                    return root;
                } catch (ClassCastException e) {
                    // setupAnim(WildAnimalRenderState) that casts to the renderer's subclass (birds): retry with that class
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("cannot be cast to class (\\S+)").matcher(String.valueOf(e.getMessage()));
                    Class<?> sub = m.find() ? Class.forName(m.group(1)) : null;
                    if (tries > 3 || sub == null || !type.isAssignableFrom(sub)) throw e;
                    type = sub;
                    root.getAllParts().forEach(ModelPart::resetPose);
                }
            }
        } catch (Throwable t) {
            errors.add(id + " setupAnim (raw pose used): " + t);
            root.getAllParts().forEach(ModelPart::resetPose);
            return root;
        }
    }

    private static Object newState(Class<?> type) throws ReflectiveOperationException {
        if (type == null) return null;
        if (type == Float.class) return 0.0F;
        if (type == net.minecraft.util.Unit.class) return net.minecraft.util.Unit.INSTANCE;
        if (Modifier.isAbstract(type.getModifiers())) return null;
        Constructor<?> c = type.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    /** The render state to pose with: FooModel -> FooRenderState next to it when that fits setupAnim (birds), else the parameter type. */
    private static Class<?> stateClass(Class<?> cls) {
        if (cls == ZombieModel.class) return ZombieRenderState.class;
        if (cls == SkeletonModel.class) return SkeletonRenderState.class;
        if (cls == PlayerModel.class) return AvatarRenderState.class;
        Class<?> param = stateType(cls);
        try {
            Class<?> own = Class.forName(cls.getName().replaceAll("Model$", "RenderState"));
            if (param != null && param.isAssignableFrom(own)) return own;
        } catch (ClassNotFoundException ignored) {
        }
        return param;
    }

    private static boolean hasField(Class<?> t, String name) {
        for (; t != null; t = t.getSuperclass())
            for (Field f : t.getDeclaredFields()) if (f.getName().equals(name)) return true;
        return false;
    }

    static final List<String> ENTITY_TEXTURES = new ArrayList<>();

    private static Class<?> stateType(Class<?> cls) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass())
            for (Method m : c.getDeclaredMethods())
                if (m.getName().equals("setupAnim") && m.getParameterCount() == 1 && !m.isBridge()) return m.getParameterTypes()[0];
        return null;
    }

    private static void setField(Object o, String name, Object v) throws IllegalAccessException {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields())
                if (f.getName().equals(name)) {
                    f.setAccessible(true);
                    f.set(o, v);
                    return;
                }
    }

    /** LayerDefinition keeps the texture size in its private MaterialDefinition; baked UVs are normalized by it. */
    private static float[] texSize(LayerDefinition layer) throws ReflectiveOperationException {
        Field mf = LayerDefinition.class.getDeclaredField("material");
        mf.setAccessible(true);
        Object mat = mf.get(layer);
        Field x = mat.getClass().getDeclaredField("xTexSize"), y = mat.getClass().getDeclaredField("yTexSize");
        x.setAccessible(true);
        y.setAccessible(true);
        return new float[] {((Number) x.get(mat)).floatValue(), ((Number) y.get(mat)).floatValue()};
    }

    /** Every quad the model would draw (visible parts only, like ModelPart.render). */
    private static String quads(ModelPart root, float[] tex) {
        StringBuilder sb = new StringBuilder("[");
        float[] q = new float[20];
        int[] n = {0};
        boolean[] first = {true};
        VertexConsumer capture = new VertexConsumer() {
            @Override
            public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
                int k = n[0]++;
                q[k * 3] = x * 16;
                q[k * 3 + 1] = y * 16;
                q[k * 3 + 2] = z * 16;
                q[12 + k * 2] = u * tex[0];
                q[13 + k * 2] = v * tex[1];
                if (n[0] == 4) {
                    n[0] = 0;
                    sb.append(first[0] ? "[" : ",[");
                    first[0] = false;
                    for (int i = 0; i < 20; i++) sb.append(i > 0 ? "," : "").append(fmt(q[i]));
                    sb.append("]");
                }
            }

            @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer setColor(int color) { return this; }
            @Override public VertexConsumer setUv(float u, float v) { return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setUv3(float u, float v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }
        };
        root.render(new PoseStack(), capture, 0, 0);
        return sb.append("]").toString();
    }

    private static String fmt(float f) {
        float r = Math.round(f * 100) / 100.0F;
        if (r == (int) r) return Integer.toString((int) r);
        return String.format(Locale.ROOT, "%.2f", r).replaceAll("0+$", "");
    }

    private static String str(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }

    private ModelExport() {}
}
