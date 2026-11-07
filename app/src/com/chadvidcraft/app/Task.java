package com.chadvidcraft.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Task {
    public final String id;
    public String title;
    public String mode;
    public String prompt;
    public String ratio;
    public String model;
    public int duration;
    public int credits;
    public long createdAt;
    public List<String> stages;
    public String status;
    public int progress;
    public String stage;
    public String failure = "";

    public String imageModel = "canvas-fast";
    public String videoModel = "motion-1";
    public String musicSource = "Demo library";
    public String musicTitle = "Neon Skyline";
    public String musicUrl = "";
    public int musicBpm = 0;
    public String musicKey = "";
    public String musicMood = "";
    public String concept = "";
    public String aesthetic = "";
    public String world = "";
    public String characters = "";
    public String variant = "";
    public String camera = "";
    public String lyric = "";
    public String effect = "";
    public String scenePreset = "";
    public String choreo = "";
    public String motion = "";
    public String startImage = "";
    public String sourceClip = "";
    public String characterA = "";
    public String characterB = "";
    public int refCount = 0;
    public String musicTrackId = "";
    public String cloudJobId = "";
    public String outputUrl = "";
    public boolean cloud = false;
    public List<Scene> scenes = new ArrayList<>();

    public boolean ttsEnabled = false;
    public String ttsVoice = "neutral";
    public boolean personaEnabled = false;
    public String personaLook = "default";
    public boolean cutoutEnabled = false;
    public boolean chromaEnabled = false;
    public boolean stabilizeEnabled = false;
    public boolean captionsEnabled = false;
    public boolean beatSyncEnabled = false;

    public Task(String id, String title, String mode, String prompt, String ratio, String model,
                int duration, int credits, String status, int progress, String stage,
                long createdAt, List<String> stages) {
        this.id = id;
        this.title = title;
        this.mode = mode;
        this.prompt = prompt;
        this.ratio = ratio;
        this.model = model;
        this.duration = duration;
        this.credits = credits;
        this.status = status;
        this.progress = progress;
        this.stage = stage;
        this.createdAt = createdAt;
        this.stages = stages == null ? new ArrayList<>() : stages;
        this.videoModel = ModelCatalog.defaultVideo(mode, model);
    }

    public String meta() {
        String music = musicTitle == null || musicTitle.isEmpty() ? "Original track" : musicTitle;
        return mode + "  ·  " + ratio + "  ·  " + duration + "s  ·  " + music;
    }

    public String modelLine() {
        return ModelCatalog.byImage(imageModel).name + " + " + ModelCatalog.byVideo(videoModel).name;
    }

    public String resolution() {
        return ModelCatalog.resolution(mode, model, videoModel);
    }

    public boolean isActive() {
        return "Queued".equals(status) || "Analyzing".equals(status)
                || "Planning".equals(status) || "Rendering".equals(status);
    }

    public boolean isFailed() {
        return "Failed".equals(status);
    }

    public boolean isCancelled() {
        return "Cancelled".equals(status);
    }

    public boolean isDone() {
        return "Done".equals(status);
    }

    public String statusLabel() {
        if ("Queued".equals(status)) return "Queued";
        if ("Analyzing".equals(status)) return "Analyzing";
        if ("Planning".equals(status)) return "Planning";
        if ("Rendering".equals(status)) return "Rendering";
        if ("Failed".equals(status)) return "Failed";
        if ("Cancelled".equals(status)) return "Cancelled";
        return "Ready";
    }

    public String timeAgo() {
        long delta = Math.max(0, System.currentTimeMillis() - createdAt);
        long minutes = delta / 60000L;
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("mode", mode);
        o.put("prompt", prompt);
        o.put("ratio", ratio);
        o.put("model", model);
        o.put("duration", duration);
        o.put("credits", credits);
        o.put("status", status);
        o.put("progress", progress);
        o.put("stage", stage);
        o.put("failure", failure);
        o.put("createdAt", createdAt);
        o.put("imageModel", imageModel);
        o.put("videoModel", videoModel);
        o.put("musicSource", musicSource);
        o.put("musicTitle", musicTitle);
        o.put("musicUrl", musicUrl);
        o.put("musicBpm", musicBpm);
        o.put("musicKey", musicKey);
        o.put("musicMood", musicMood);
        o.put("concept", concept);
        o.put("aesthetic", aesthetic);
        o.put("world", world);
        o.put("characters", characters);
        o.put("variant", variant);
        o.put("camera", camera);
        o.put("lyric", lyric);
        o.put("effect", effect);
        o.put("scenePreset", scenePreset);
        o.put("choreo", choreo);
        o.put("motion", motion);
        o.put("startImage", startImage);
        o.put("sourceClip", sourceClip);
        o.put("characterA", characterA);
        o.put("characterB", characterB);
        o.put("refCount", refCount);
        o.put("musicTrackId", musicTrackId);
        o.put("cloudJobId", cloudJobId);
        o.put("outputUrl", outputUrl);
        o.put("cloud", cloud);
        o.put("ttsEnabled", ttsEnabled);
        o.put("ttsVoice", ttsVoice);
        o.put("personaEnabled", personaEnabled);
        o.put("personaLook", personaLook);
        o.put("cutoutEnabled", cutoutEnabled);
        o.put("chromaEnabled", chromaEnabled);
        o.put("stabilizeEnabled", stabilizeEnabled);
        o.put("captionsEnabled", captionsEnabled);
        o.put("beatSyncEnabled", beatSyncEnabled);
        JSONArray arr = new JSONArray();
        for (String s : stages) arr.put(s);
        o.put("stages", arr);
        JSONArray sceneArr = new JSONArray();
        for (Scene s : scenes) sceneArr.put(s.toJson());
        o.put("scenes", sceneArr);
        return o;
    }

    public static Task fromJson(JSONObject o) {
        try {
            String id = o.optString("id", "task_" + System.currentTimeMillis());
            String title = o.optString("title", "Untitled draft");
            String mode = o.optString("mode", "Music Video");
            String prompt = o.optString("prompt", "");
            String ratio = o.optString("ratio", "9:16");
            String model = o.optString("model", "Studio");
            int duration = o.optInt("duration", 30);
            int credits = o.optInt("credits", 50);
            String status = o.optString("status", "Done");
            int progress = o.optInt("progress", 100);
            String stage = o.optString("stage", "Final cut");
            long createdAt = o.optLong("createdAt", System.currentTimeMillis());
            List<String> stages = new ArrayList<>();
            JSONArray arr = o.optJSONArray("stages");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) stages.add(arr.optString(i));
            }
            if (stages.isEmpty()) stages = TaskStore.stagesFor(mode);
            Task t = new Task(id, title, mode, prompt, ratio, model, duration, credits, status,
                    progress, stage, createdAt, stages);
            t.failure = o.optString("failure", "");
            t.imageModel = o.optString("imageModel", ModelCatalog.defaultImage(model));
            t.videoModel = o.optString("videoModel", ModelCatalog.defaultVideo(mode, model));
            t.musicSource = o.optString("musicSource", "Demo library");
            t.musicTitle = o.optString("musicTitle", "Neon Skyline");
            t.musicUrl = o.optString("musicUrl", "");
            t.musicBpm = o.optInt("musicBpm", 0);
            t.musicKey = o.optString("musicKey", "");
            t.musicMood = o.optString("musicMood", "");
            t.concept = o.optString("concept", "");
            t.aesthetic = o.optString("aesthetic", "");
            t.world = o.optString("world", "");
            t.characters = o.optString("characters", "");
            t.variant = o.optString("variant", "");
            t.camera = o.optString("camera", "");
            t.lyric = o.optString("lyric", "");
            t.effect = o.optString("effect", "");
            t.scenePreset = o.optString("scenePreset", "");
            t.choreo = o.optString("choreo", "");
            t.motion = o.optString("motion", "");
            t.startImage = o.optString("startImage", "");
            t.sourceClip = o.optString("sourceClip", "");
            t.characterA = o.optString("characterA", "");
            t.characterB = o.optString("characterB", "");
            t.refCount = o.optInt("refCount", 0);
            t.musicTrackId = o.optString("musicTrackId", "");
            t.cloudJobId = o.optString("cloudJobId", "");
            t.outputUrl = o.optString("outputUrl", "");
            t.cloud = o.optBoolean("cloud", false);
            t.ttsEnabled = o.optBoolean("ttsEnabled", false);
            t.ttsVoice = o.optString("ttsVoice", "neutral");
            t.personaEnabled = o.optBoolean("personaEnabled", false);
            t.personaLook = o.optString("personaLook", "default");
            t.cutoutEnabled = o.optBoolean("cutoutEnabled", false);
            t.chromaEnabled = o.optBoolean("chromaEnabled", false);
            t.stabilizeEnabled = o.optBoolean("stabilizeEnabled", false);
            t.captionsEnabled = o.optBoolean("captionsEnabled", false);
            t.beatSyncEnabled = o.optBoolean("beatSyncEnabled", false);
            t.scenes = Scene.listFromJson(o.optJSONArray("scenes"));
            return t;
        } catch (Exception e) {
            return null;
        }
    }
}
