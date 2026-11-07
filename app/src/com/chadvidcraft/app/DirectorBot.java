package com.chadvidcraft.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class DirectorBot {

    private DirectorBot() {
    }

    static String greeting() {
        return "I’m your local Director Bot. I can shape a concept, write shot direction, choose a model, or explain the pipeline. Try “concept”, “prompt”, “models”, or “credits”.";
    }

    static List<String> suggestions(String mode) {
        if ("Dance Video".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Dance concept", "Camera plan", "Pick a model"));
        }
        if ("Photo Karaoke".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Solo or duet?", "Scene ideas", "Lip-sync tips"));
        }
        if ("OnBeat Effects".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Beat effect", "Template idea", "Prompt"));
        }
        if ("Realtime".equals(mode)) {
            return new ArrayList<>(Arrays.asList("Live concept", "Start image", "Session tips"));
        }
        return new ArrayList<>(Arrays.asList("Write a concept", "Improve my prompt", "Choose models"));
    }

    static String reply(String raw, String mode, String track) {
        String input = raw == null ? "" : raw.trim().toLowerCase(Locale.US);
        String song = track == null || track.isEmpty() ? "your track" : track;
        if (input.isEmpty()) return greeting();
        if (input.contains("credit") || input.contains("cost") || input.contains("price")) {
            return "Credits are reserved when a render starts and shown again in the task ledger. Fast draft models cost less; a 1080p hero model costs more. The estimate in Create uses duration, model and mode complexity.";
        }
        if (input.contains("model")) {
            return "Use Chad Canvas for fast frames, Chad Canvas Pro for detailed key art, and Chad Ink for graphic or typography-led frames. For motion, Chad Motion Lite is the draft choice; Chad Motion is the hero choice. Dance and Photo Karaoke get their own compatibility lists.";
        }
        if (input.contains("camera") || input.contains("shot")) {
            return "Shot plan: open with a wide establishing frame, move to a medium performance frame, cut to a close detail on the hook, then finish with a slow pull-back. Keep one camera idea per scene so the storyboard stays readable.";
        }
        if (input.contains("character") || input.contains("reference")) {
            return "Add one character anchor and one world or wardrobe reference. Keep the same face, silhouette and palette across scenes; change camera and lighting instead of changing identity.";
        }
        if (input.contains("dance")) {
            return "For dance, lock the choreography to the strongest 8-count, alternate wide performance shots with close footwork, and keep the background simple so the motion reads. Dance is exported at 720p.";
        }
        if (input.contains("karaoke") || input.contains("sing")) {
            return "For Photo Karaoke, use a clean portrait with visible mouth and eyes, choose Solo for one subject or Duet for two, then match the scene to the vocal mood. Avoid heavy occlusion around the face.";
        }
        if (input.contains("onbeat") || input.contains("beat")) {
            return "Pick one beat effect and commit: flash cuts, liquid glow, or a transform grid. Sync the largest change to the downbeat and leave breathing room between hits.";
        }
        if (input.contains("lyric")) {
            return "Treat lyrics as a rhythm track: one line per phrase, two lines maximum on screen, and let the visual style change only at section boundaries.";
        }
        if (input.contains("realtime") || input.contains("live")) {
            return "Realtime works best with a fixed start image, a simple subject, and short direction updates. Prepare the music range before launch and keep each live nudge focused on one change.";
        }
        if (input.contains("concept") || input.contains("idea") || input.contains("story")) {
            return concept(mode, song);
        }
        if (input.contains("prompt") || input.contains("improve") || input.contains("write")) {
            return enhance("A cinematic music video for " + song, mode, "editorial neon");
        }
        return "Try: “Write a concept for " + song + "”, “Give me a camera plan”, or “Which model should I use?” I work locally now and can be connected to a model later.";
    }

    static String concept(String mode, String track) {
        String song = track == null || track.isEmpty() ? "the track" : track;
        if ("Dance Video".equals(mode)) {
            return "Concept: " + song + " becomes a nocturnal rehearsal. Start in a bare concrete room, reveal a single moving light, then let the final chorus open into a rain-soaked courtyard. Wardrobe stays monochrome so the movement carries the story.";
        }
        if ("Photo Karaoke".equals(mode)) {
            return "Concept: a portrait steps out of a still frame and performs " + song + " inside three shifting rooms. Keep the face consistent, use practical light for each room, and end on a direct-to-camera final note.";
        }
        if ("OnBeat Effects".equals(mode)) {
            return "Concept: build " + song + " as a sequence of graphic hits. Each downbeat changes the frame treatment; each verse keeps one motif. Use hard cuts, colour blocks and one repeating shape.";
        }
        if ("Realtime".equals(mode)) {
            return "Concept: a live performance postcard for " + song + ". Lock a single start image, keep the camera language simple, and let the live direction add one visual change every few bars.";
        }
        return "Concept: " + song + " is a late-night drive through a city that keeps rearranging itself. A lone performer moves from rooftop to tunnel to empty street; every chorus reveals a new version of the same neon skyline.";
    }

    static String enhance(String prompt, String mode, String aesthetic) {
        String base = prompt == null || prompt.trim().isEmpty()
                ? "A cinematic music video with a strong performance anchor"
                : prompt.trim();
        String look = aesthetic == null || aesthetic.trim().isEmpty() ? "editorial neon" : aesthetic.trim();
        return base + ". " + look + " art direction, motivated practical light, layered foreground depth, "
                + "consistent character identity, rhythmic camera movement, and a clean final chorus. "
                + "Mode: " + mode + ". Keep one clear subject and one visual idea per scene.";
    }

    static String brief(String mode, String track) {
        String song = track == null || track.isEmpty() ? "the track" : track;
        return "A " + mode.toLowerCase(Locale.US) + " built around " + song
                + " with a repeating visual motif, one character anchor and a final chorus that resolves the opening image.";
    }
}
