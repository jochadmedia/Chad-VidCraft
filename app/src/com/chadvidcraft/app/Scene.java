package com.chadvidcraft.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Scene {
    public String title;
    public int startSec;
    public int endSec;
    public String location;
    public String camera;
    public String action;
    public String prompt;
    public int shots;
    public boolean approved;

    public Scene(String title, int startSec, int endSec, String location, String camera,
                 String action, String prompt, int shots) {
        this.title = title;
        this.startSec = startSec;
        this.endSec = endSec;
        this.location = location;
        this.camera = camera;
        this.action = action;
        this.prompt = prompt;
        this.shots = shots;
    }

    public String timeRange() {
        return String.format(java.util.Locale.US, "%d:%02d–%d:%02d",
                startSec / 60, startSec % 60, endSec / 60, endSec % 60);
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("startSec", startSec);
            o.put("endSec", endSec);
            o.put("location", location);
            o.put("camera", camera);
            o.put("action", action);
            o.put("prompt", prompt);
            o.put("shots", shots);
            o.put("approved", approved);
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static Scene fromJson(JSONObject o) {
        Scene s = new Scene(o.optString("title", "Scene"),
                o.optInt("startSec", 0), o.optInt("endSec", 8),
                o.optString("location", "Studio"),
                o.optString("camera", "Slow push in"),
                o.optString("action", "Performance"),
                o.optString("prompt", ""),
                o.optInt("shots", 2));
        s.approved = o.optBoolean("approved", false);
        return s;
    }

    public static List<Scene> listFromJson(JSONArray arr) {
        List<Scene> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) out.add(fromJson(o));
        }
        return out;
    }
}
