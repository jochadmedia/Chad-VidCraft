package com.chadvidcraft.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ModelCatalog {

    static final class Model {
        final String id;
        final String name;
        final String kind;
        final String note;
        final int imageCredits;
        final int videoCredits;

        Model(String id, String name, String kind, String note, int imageCredits, int videoCredits) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.note = note;
            this.imageCredits = imageCredits;
            this.videoCredits = videoCredits;
        }
    }

    static final List<Model> IMAGES = new ArrayList<>(Arrays.asList(
            new Model("canvas-fast", "Chad Canvas", "image", "Fast concept frames", 10, 0),
            new Model("canvas-pro", "Chad Canvas Pro", "image", "Detailed key art", 18, 0),
            new Model("ink-1", "Chad Ink", "image", "Graphic and type-led frames", 22, 0)
    ));

    static final List<Model> VIDEOS = new ArrayList<>(Arrays.asList(
            new Model("motion-lite", "Chad Motion Lite", "video", "Draft motion · 720p", 0, 38),
            new Model("motion-1", "Chad Motion", "video", "Hero motion · 1080p", 0, 74),
            new Model("motion-style", "Chad Motion Style", "video", "Stylised motion · 1080p", 0, 52),
            new Model("motion-stage", "Chad Motion Stage", "video", "Performance motion · 1080p", 0, 64),
            new Model("motion-dance", "Chad Motion Dance", "video", "Dance · 720p", 0, 86),
            new Model("motion-ai", "Chad Motion AI", "video", "Text-to-video · 1080p", 0, 120),
            new Model("motion-i2v", "Chad Motion Image2Vid", "video", "Image-to-video · 1080p", 0, 110)
    ));

    static final Model REALTIME = new Model("live-director", "Chad Live Director", "realtime",
            "Live session · 720p", 0, 58);

    static final Model PERSONA = new Model("persona-1", "Chad Persona", "persona",
            "Digital human / avatar · 720p", 0, 90);

    static final Model TTS = new Model("voice-1", "Chad Voice", "tts",
            "Text-to-speech / voiceover", 0, 6);

    private ModelCatalog() {
    }

    static List<Model> videosFor(String mode) {
        switch (mode) {
            case "Dance Video":
                return new ArrayList<>(Arrays.asList(
                        byVideo("motion-dance"), byVideo("motion-1"), byVideo("motion-stage")));
            case "Photo Karaoke":
                return new ArrayList<>(Arrays.asList(
                        byVideo("motion-stage"), byVideo("motion-1"), byVideo("motion-style")));
            case "AI Video":
                return new ArrayList<>(Arrays.asList(
                        byVideo("motion-ai"), byVideo("motion-1"), byVideo("motion-style")));
            case "Image-to-Video":
                return new ArrayList<>(Arrays.asList(
                        byVideo("motion-i2v"), byVideo("motion-ai"), byVideo("motion-1")));
            case "AI Persona":
                return new ArrayList<>(Arrays.asList(PERSONA));
            case "Realtime":
                return new ArrayList<>(Arrays.asList(REALTIME));
            default:
                return new ArrayList<>(VIDEOS);
        }
    }

    static Model byImage(String id) {
        for (Model m : IMAGES) if (m.id.equals(id)) return m;
        return IMAGES.get(0);
    }

    static Model byVideo(String id) {
        for (Model m : VIDEOS) if (m.id.equals(id)) return m;
        if (REALTIME.id.equals(id)) return REALTIME;
        if (PERSONA.id.equals(id)) return PERSONA;
        return VIDEOS.get(0);
    }

    static Model byModel(String id) {
        Model m = byVideo(id);
        if (m != null) return m;
        m = byImage(id);
        if (m != null) return m;
        if (PERSONA.id.equals(id)) return PERSONA;
        if (TTS.id.equals(id)) return TTS;
        return VIDEOS.get(0);
    }

    static String defaultImage(String style) {
        return "Fast".equals(style) ? "canvas-fast" : "canvas-pro";
    }

    static String defaultVideo(String mode, String style) {
        switch (mode) {
            case "Dance Video": return "motion-dance";
            case "Realtime": return "live-director";
            case "Photo Karaoke": return "motion-stage";
            case "AI Video": return "motion-ai";
            case "Image-to-Video": return "motion-i2v";
            case "AI Persona": return "persona-1";
            default:
                return "Fast".equals(style) ? "motion-lite" : "motion-1";
        }
    }

    static int estimate(String mode, String style, int duration, String imageId, String videoId) {
        Model video = byModel(videoId);
        int base = video.videoCredits;
        if (!"Realtime".equals(mode) && !"AI Persona".equals(mode)) {
            base += byImage(imageId).imageCredits;
        }
        if ("Custom".equals(style)) base += 8;
        if ("OnBeat Effects".equals(mode)) base = Math.round(base * 0.62f);
        if ("Lyric Video".equals(mode)) base = Math.round(base * 0.72f);
        if ("Music Cover Video".equals(mode)) base = Math.round(base * 0.68f);
        if ("Video to Music".equals(mode)) base = Math.round(base * 0.78f);
        if ("AI Video".equals(mode)) base = Math.round(base * 1.15f);
        if ("Image-to-Video".equals(mode)) base = Math.round(base * 1.10f);
        if ("AI Persona".equals(mode)) base = Math.round(base * 1.25f);
        return Math.max(18, Math.round(base * (duration / 30f)));
    }

    static int ttsCredits(int durationSeconds) {
        return Math.max(4, Math.round((durationSeconds / 30f) * TTS.videoCredits));
    }

    static String resolution(String mode, String style, String videoId) {
        Model v = byModel(videoId);
        if (v == null) v = byVideo(videoId);
        String id = v.id;
        if ("Dance Video".equals(mode) || "Photo Karaoke".equals(mode)
                || "Realtime".equals(mode) || "Fast".equals(style)
                || "motion-lite".equals(id) || "live-director".equals(id)
                || "persona-1".equals(id)) {
            return "720p";
        }
        return "1080p";
    }

    static String modelKind(String mode) {
        switch (mode) {
            case "AI Persona": return "persona";
            case "AI Video":
            case "Image-to-Video": return "video";
            default: return "video";
        }
    }
}
