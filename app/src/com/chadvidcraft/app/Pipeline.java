package com.chadvidcraft.app;

import java.util.ArrayList;
import java.util.List;

final class Pipeline {

    private static final String[] SECTIONS = {"Cold open", "Verse one", "Pre-hook", "Hook", "Verse two", "Final chorus"};
    private static final String[] CAMERAS = {"Slow push in", "Lateral track", "Handheld follow", "Locked wide", "Orbit", "Slow pull back"};
    private static final String[] PLACES = {"Rooftop", "Tunnel", "Empty street", "Studio", "Stairwell", "Rain courtyard"};

    private Pipeline() {
    }

    static List<Scene> scenesFor(Task task) {
        List<Scene> scenes = new ArrayList<>();
        int count = Math.max(3, Math.min(6, task.duration / 12));
        int segment = Math.max(4, task.duration / count);
        String look = task.aesthetic == null || task.aesthetic.isEmpty() ? "editorial neon" : task.aesthetic;
        String subject = task.characters == null || task.characters.isEmpty() ? "lead performer" : task.characters;
        String[] lyrics = lyricLines(task);
        for (int i = 0; i < count; i++) {
            int start = i * segment;
            int end = Math.min(task.duration, start + segment + (i == count - 1 ? 2 : 0));
            String place = placeFor(task, i);
            String camera = cameraFor(task, i);
            String action = actionFor(task.mode, i, count);
            if ("Lyric Video".equals(task.mode) && lyrics.length > 0) {
                action = "Line “" + lyrics[Math.min(i, lyrics.length - 1)] + "”";
            }
            if ("OnBeat Effects".equals(task.mode) && task.effect != null && !task.effect.isEmpty()) {
                action = task.effect + " · " + action;
            }
            if ("Music Cover Video".equals(task.mode) && task.motion != null && !task.motion.isEmpty()) {
                action = task.motion + " · " + action;
            }
            String prompt = look + " · " + subject + " · " + place + " · " + action
                    + " · " + camera + " · " + SECTIONS[Math.min(i, SECTIONS.length - 1)];
            scenes.add(new Scene(sectionTitle(i, count), start, end, place, camera, action, prompt,
                    1 + (i % 3)));
        }
        return scenes;
    }

    private static String placeFor(Task task, int index) {
        if ("Photo Karaoke".equals(task.mode) && task.scenePreset != null && !task.scenePreset.isEmpty()) {
            return task.scenePreset + " set";
        }
        if ("Video to Music".equals(task.mode) && task.sourceClip != null && !task.sourceClip.isEmpty()) {
            return "Source clip";
        }
        if ("Realtime".equals(task.mode) && task.startImage != null && !task.startImage.isEmpty()) {
            return "Start image";
        }
        if ("Music Cover Video".equals(task.mode)) {
            return "Artwork plane";
        }
        return PLACES[index % PLACES.length];
    }

    private static String cameraFor(Task task, int index) {
        if (task.camera != null && !task.camera.isEmpty()) {
            return task.camera;
        }
        return CAMERAS[index % CAMERAS.length];
    }

    private static String[] lyricLines(Task task) {
        if (task.lyric == null || task.lyric.trim().isEmpty()) {
            return new String[0];
        }
        String[] raw = task.lyric.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String line : raw) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) lines.add(trimmed);
        }
        return lines.toArray(new String[0]);
    }

    private static String sectionTitle(int index, int count) {
        if (index == 0) return "Opening image";
        if (index == count - 1) return "Final resolve";
        if (index == count / 2) return "Hook turn";
        return "Scene " + (index + 1);
    }

    private static String actionFor(String mode, int index, int count) {
        if ("Dance Video".equals(mode)) {
            String[] actions = {"Full-body 8-count in a wide frame", "Footwork detail and shadow",
                    "Two-step repeat with camera orbit", "Group formation and final pose"};
            return actions[index % actions.length];
        }
        if ("Photo Karaoke".equals(mode)) {
            String[] actions = {"Portrait turns to camera and starts the first line",
                    "Close lip-sync with practical light", "Wider performance with the scene revealed",
                    "Direct-to-camera final note"};
            return actions[index % actions.length];
        }
        if ("OnBeat Effects".equals(mode)) {
            String[] actions = {"Hard cut on the first downbeat", "Graphic flash on the snare",
                    "Transform block on the hook", "Colour hit and freeze on the last beat"};
            return actions[index % actions.length];
        }
        if ("Lyric Video".equals(mode)) {
            String[] actions = {"First lyric line enters with the motif", "Type stacks on the pre-hook",
                    "Kinetic line change on the hook", "Final line resolves into the opening frame"};
            return actions[index % actions.length];
        }
        if ("Realtime".equals(mode)) {
            String[] actions = {"Start image holds while the first direction lands",
                    "Subject steps forward on the live cue", "Environment shifts with the chorus",
                    "Camera settles for the final line"};
            return actions[index % actions.length];
        }
        if ("Video to Music".equals(mode)) {
            String[] actions = {"Clip pulse establishes tempo", "Cut rhythm maps to the first hit",
                    "Score rises with the visual movement", "Final frame lands on the last accent"};
            return actions[index % actions.length];
        }
        if ("Music Cover Video".equals(mode)) {
            String[] actions = {"Cover art begins to breathe", "Layers separate into depth",
                    "Motion loop hits the chorus", "Artwork resolves to a still"};
            return actions[index % actions.length];
        }
        String[] actions = {"Lead performer enters the frame", "Medium performance with foreground depth",
                "Hook movement across the location", "Wide reveal and final pose"};
        return actions[index % actions.length];
    }

    static void regenerate(Task task, int index) {
        if (task.scenes == null || index < 0 || index >= task.scenes.size()) return;
        Scene s = task.scenes.get(index);
        s.camera = CAMERAS[(index + 2) % CAMERAS.length];
        s.location = PLACES[(index + 3) % PLACES.length];
        s.action = actionFor(task.mode, index + 1, Math.max(3, task.scenes.size()));
        s.prompt = task.aesthetic + " · " + task.characters + " · " + s.location + " · "
                + s.action + " · " + s.camera + " · alternate take";
        s.approved = false;
    }

    static void regenerateAll(Task task) {
        for (int i = 0; i < task.scenes.size(); i++) {
            Pipeline.regenerate(task, i);
            task.scenes.get(i).approved = false;
        }
    }
}
