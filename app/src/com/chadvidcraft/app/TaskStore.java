package com.chadvidcraft.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TaskStore {
    private static final String PREFS = "chadvidcraft_store";
    private static final String KEY = "tasks";

    private final SharedPreferences prefs;
    private final List<Task> tasks = new ArrayList<>();

    public TaskStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
        if (tasks.isEmpty()) {
            seed();
            save();
        }
    }

    public List<Task> all() {
        return tasks;
    }

    public Task find(String id) {
        if (id == null) return null;
        for (Task t : tasks) if (t.id.equals(id)) return t;
        return null;
    }

    public Task latest() {
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    public Task firstRendering() {
        for (Task t : tasks) if (t.isActive()) return t;
        return null;
    }

    public List<Task> filtered(String filter) {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) {
            if ("All".equals(filter)) {
                out.add(t);
            } else if ("In progress".equals(filter) && t.isActive()) {
                out.add(t);
            } else if ("Completed".equals(filter) && t.isDone()) {
                out.add(t);
            } else if ("Failed".equals(filter) && (t.isFailed() || t.isCancelled())) {
                out.add(t);
            } else if ("Rendering".equals(filter) && t.isActive()) {
                out.add(t);
            } else if ("Done".equals(filter) && t.isDone()) {
                out.add(t);
            }
        }
        return out;
    }

    public void add(Task task) {
        tasks.add(0, task);
        save();
    }

    public void update(Task task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id.equals(task.id)) {
                tasks.set(i, task);
                break;
            }
        }
        save();
    }

    public void delete(String id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id.equals(id)) {
                tasks.remove(i);
                break;
            }
        }
        save();
    }

    public void reset() {
        tasks.clear();
        seed();
        save();
    }

    public int completedCount() {
        int n = 0;
        for (Task t : tasks) if (t.isDone()) n++;
        return n;
    }

    public int activeCount() {
        int n = 0;
        for (Task t : tasks) if (t.isActive()) n++;
        return n;
    }

    public int failedCount() {
        int n = 0;
        for (Task t : tasks) if (t.isFailed() || t.isCancelled()) n++;
        return n;
    }

    private void load() {
        try {
            String raw = prefs.getString(KEY, null);
            if (raw == null) return;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                Task t = Task.fromJson(arr.getJSONObject(i));
                if (t != null) tasks.add(t);
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : tasks) arr.put(t.toJson());
            prefs.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void seed() {
        long now = System.currentTimeMillis();
        Task t1 = addSeed("Neon Skyline", "Music Video",
                "A neon-lit rooftop performance with rain reflections, slow orbit shots and a glowing final chorus.",
                "16:9", "Studio", 45, "Done", 100, "Final cut", now - 38L * 60L * 1000L,
                "Neon Skyline", 122, "F# minor", "Midnight drive");
        t1.concept = "A city that rearranges itself around one performer.";
        t1.aesthetic = "Editorial neon with silver highlights";
        t1.world = "Rooftops, wet streets and a tunnel of moving light";
        t1.characters = "Lead performer · silver jacket";
        t1.characterA = "Lead · silver jacket · short dark hair";
        t1.variant = "Performance";
        t1.camera = "Slow orbit";

        Task t2 = addSeed("Midnight Drive", "Dance Video",
                "A fast-cut night drive choreography with headlights, wet asphalt and kinetic camera moves.",
                "9:16", "Studio", 30, "Done", 100, "Final cut", now - 3L * 60L * 60L * 1000L,
                "Midnight Drive", 138, "A minor", "Kinetic");
        t2.variant = "Group";
        t2.choreo = "Street";
        t2.camera = "Orbit";
        t2.characters = "Four dancers · monochrome wardrobe";
        t2.characterA = "Dancer A · monochrome wardrobe";

        Task t3 = addSeed("Lyric Bloom", "Lyric Video",
                "Hand-painted lyric typography blooming through a soft greenhouse at golden hour.",
                "1:1", "Studio", 30, "Rendering", 58, "Storyboard", now - 7L * 60L * 1000L,
                "Lyric Bloom", 104, "C major", "Warm");
        t3.variant = "Hand-painted";
        t3.aesthetic = "Golden hour with soft botanical texture";
        t3.lyric = "Paper petals in the morning light\nEvery color starts to breathe\nWe bloom where the shadows end\nStay until the sky turns gold";

        Task t4 = addSeed("Afrobeat Pulse", "OnBeat Effects",
                "Beat-synced flash cuts with bold color blocks and transform transitions.",
                "9:16", "Fast", 15, "Done", 100, "Final cut", now - 26L * 60L * 60L * 1000L,
                "Afrobeat Pulse", 112, "G minor", "High energy");
        t4.variant = "Flash";
        t4.effect = "Flash grid";

        Task t5 = addSeed("Studio Portrait", "Photo Karaoke",
                "A warm studio portrait that sings the lead vocal with soft rim light.",
                "9:16", "Studio", 30, "Done", 100, "Final polish", now - 2L * 24L * 60L * 60L * 1000L,
                "Studio Portrait", 96, "D major", "Intimate");
        t5.variant = "Solo";
        t5.scenePreset = "Studio";
        t5.characters = "Solo portrait · warm rim light";
        t5.characterA = "Solo portrait · warm rim light";

        Task t6 = addSeed("Glass Echo", "Music Cover Video",
                "Animate a glass-textured cover artwork into a slow, looping motion poster.",
                "1:1", "Studio", 20, "Failed", 46, "Rendering loops", now - 4L * 24L * 60L * 60L * 1000L,
                "Glass Echo", 88, "E minor", "Reflective");
        t6.variant = "Cover";
        t6.motion = "Parallax loop";
        t6.failure = "The demo provider rejected the second motion pass. Retry to rebuild the storyboard.";

        Task t7 = addSeed("Afterglow", "Realtime",
                "A live-directed performance postcard with one start image and slow camera drift.",
                "9:16", "Studio", 15, "Cancelled", 22, "Live direction", now - 5L * 24L * 60L * 60L * 1000L,
                "Afterglow", 100, "B minor", "Dreamy");
        t7.variant = "Live portrait";
        t7.startImage = "local://start/afterglow";

        t1.scenes = Pipeline.scenesFor(t1);
        t2.scenes = Pipeline.scenesFor(t2);
        t3.scenes = Pipeline.scenesFor(t3);
        t4.scenes = Pipeline.scenesFor(t4);
        t5.scenes = Pipeline.scenesFor(t5);
        t6.scenes = Pipeline.scenesFor(t6);
        t7.scenes = Pipeline.scenesFor(t7);
    }

    private Task addSeed(String title, String mode, String prompt, String ratio, String model,
                         int duration, String status, int progress, String stage, long createdAt,
                         String musicTitle, int bpm, String key, String mood) {
        Task task = new Task("seed_" + tasks.size(), title, mode, prompt, ratio, model, duration,
                creditsFor(mode, model, duration), status, progress, stage, createdAt, stagesFor(mode));
        task.musicTitle = musicTitle;
        task.musicBpm = bpm;
        task.musicKey = key;
        task.musicMood = mood;
        task.imageModel = ModelCatalog.defaultImage(model);
        task.videoModel = ModelCatalog.defaultVideo(mode, model);
        task.concept = DirectorBot.brief(mode, musicTitle);
        task.aesthetic = "Editorial, cinematic, restrained color";
        task.world = "A single location that shifts with each chorus";
        task.characters = "Lead performer";
        task.scenes = Pipeline.scenesFor(task);
        tasks.add(task);
        return task;
    }

    public static List<String> stagesFor(String mode) {
        if ("Dance Video".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Analyzing track", "Choreography brief",
                    "Camera direction", "Scene plan", "Storyboard", "Rendering sections", "Final cut"));
        }
        if ("Lyric Video".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Analyzing lyrics", "Lyric timing", "Visual style",
                    "Scene plan", "Storyboard", "Rendering lines", "Final cut"));
        }
        if ("OnBeat Effects".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Analyzing beat", "Matching effect",
                    "Generating materials", "Beat-sync plan", "Storyboard", "Beat-sync render", "Final cut"));
        }
        if ("Photo Karaoke".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Detecting subject", "Lip-sync analysis",
                    "Setting the scene", "Performance brief", "Storyboard", "Rendering performance", "Final polish"));
        }
        if ("Music Cover Video".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Analyzing cover art", "Motion design",
                    "Depth layers", "Scene plan", "Storyboard", "Rendering loops", "Final cut"));
        }
        if ("Video to Music".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Analyzing clip", "Tempo map",
                    "Composing music", "Arrangement", "Audio timeline", "Mastering track", "Final mix"));
        }
        if ("Realtime".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Preparing session", "Planning scene",
                    "Preparing start image", "Live direction", "Local recording", "Session review", "Finalizing"));
        }
        return new ArrayList<>(Arrays.asList("Analyzing music", "Creative brief", "Scene plan",
                "Shot plan", "Storyboard", "Rendering clips", "Final cut"));
    }

    public static int creditsFor(String mode, String model, int duration) {
        return ModelCatalog.estimate(mode, model, duration,
                ModelCatalog.defaultImage(model), ModelCatalog.defaultVideo(mode, model));
    }

    public static int creditsFor(String mode, String model, int duration,
                                 String imageModel, String videoModel) {
        return ModelCatalog.estimate(mode, model, duration, imageModel, videoModel);
    }

    public static String resolutionFor(String mode, String model) {
        return ModelCatalog.resolution(mode, model, ModelCatalog.defaultVideo(mode, model));
    }
}
