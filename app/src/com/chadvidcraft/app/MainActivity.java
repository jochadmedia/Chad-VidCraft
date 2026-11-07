package com.chadvidcraft.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int REQ_MUSIC = 2001;
    private static final int REQ_REFERENCE = 2002;
    private static final int REQ_EXPORT = 2003;
    private static final int REQ_CLIP = 2004;
    private static final int REQ_START_IMAGE = 2005;
    private static final String DRAFT_PREFS = "chadvidcraft_draft";
    private static final String DRAFT_KEY = "draft";

    private interface Sink {
        void set(String value);
    }

    private static final class Mode {
        final String name;
        final String desc;
        final String tag;
        final int icon;
        final int color;

        Mode(String name, String desc, String tag, int icon, int color) {
            this.name = name;
            this.desc = desc;
            this.tag = tag;
            this.icon = icon;
            this.color = color;
        }
    }

    private static final class NavItem {
        final LinearLayout view;
        final ImageView icon;
        final TextView label;

        NavItem(LinearLayout view, ImageView icon, TextView label) {
            this.view = view;
            this.icon = icon;
            this.label = label;
        }
    }

    private static final class DiscoveryItem {
        final String title;
        final String mode;
        final String creator;
        final String track;
        final String description;
        final int colorA;
        final int colorB;
        final String plays;

        DiscoveryItem(String title, String mode, String creator, String track,
                      String description, int colorA, int colorB, String plays) {
            this.title = title;
            this.mode = mode;
            this.creator = creator;
            this.track = track;
            this.description = description;
            this.colorA = colorA;
            this.colorB = colorB;
            this.plays = plays;
        }
    }

    private TaskStore store;
    private SharedPreferences draftPrefs;
    private CloudApi cloud;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean cloudBusy;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<NavItem> navItems = new ArrayList<>();
    private FrameLayout content;
    private FrameLayout stage;
    private LinearLayout navBar;
    private LinearLayout botButton;

    private int tab = 0;
    private int createMode = 0;
    private String createRatio = "9:16";
    private String createModel = "Studio";
    private int createDuration = 30;
    private String createPrompt = "";
    private String taskFilter = "All";
    private String activeRenderId;
    private Runnable renderRunnable;
    private Runnable cloudPollRunnable;
    private String cloudPollJobId;

    private String imageModel = "canvas-pro";
    private String videoModel = "motion-1";
    private String musicSource = "Demo library";
    private String musicSourceTab = "Generate";
    private String musicModel = "Chad Voice";
    private String musicTitle = "Neon Skyline";
    private String musicUrl = "";
    private String musicTrackId = "";
    private boolean cloudRender = false;
    private int musicBpm = 122;
    private String musicKey = "F# minor";
    private String musicMood = "Midnight drive";
    private String concept = "";
    private String aesthetic = "Editorial neon";
    private String world = "Rooftops, wet streets, a tunnel of moving light";
    private String characters = "Lead performer · silver jacket";
    private String variant = "Performance";
    private String optionB = "Balanced";
    private String lookStyle = "Abstract";
    private String lyricText = "";
    private String effectFamily = "Flash";
    private String effectTemplate = "Flash grid";
    private String scenePreset = "Studio";
    private String choreo = "Solo";
    private String camera = "Orbit";
    private String motionPreset = "Parallax loop";
    private String startImage = "";
    private String sourceClip = "";
    private String characterA = "";
    private String characterB = "";

    private boolean ttsEnabled = false;
    private String ttsVoice = "neutral";
    private boolean personaEnabled = false;
    private String personaLook = "default";
    private boolean cutoutEnabled = false;
    private boolean chromaEnabled = false;
    private boolean stabilizeEnabled = false;
    private boolean captionsEnabled = false;
    private boolean beatSyncEnabled = false;

    private final List<String> referenceUris = new ArrayList<>();

    private Mode[] modes;
    private EditText promptInput;
    private TextView createEstimate;
    private TextView durationLabel;
    private TextView generateBtn;
    private TextView openTaskBtn;
    private View progressCard;
    private LinearLayout stageList;
    private ProgressBar progressBar;
    private TextView progressPct;
    private TextView progressStage;
    private final List<TextView> modeChips = new ArrayList<>();
    private final List<TextView> ratioChips = new ArrayList<>();
    private final List<TextView> modelChips = new ArrayList<>();
    private LinearLayout directionHost;
    private LinearLayout imageModelHost;
    private LinearLayout videoModelHost;
    private TextView estimateSub;
    private Runnable directionRefresher;

    private String discoverQuery = "";
    private String discoverFilter = "All";
    private LinearLayout discoverFeed;
    private String lastBotText = "";
    private Task pendingExport;

    private final DiscoveryItem[] discoveries = new DiscoveryItem[]{
            new DiscoveryItem("Afterglow Sessions", "Music Video", "Maya North",
                    "Afterglow · 100 BPM", "A slow-burn rooftop performance built around one moving light.",
                    Ui.INDIGO, Ui.MAGENTA, "18.4k"),
            new DiscoveryItem("Concrete Bloom", "Dance Video", "Studio Noor",
                    "Concrete Bloom · 132 BPM", "A concrete stairwell becomes a stage for sharp footwork and hard shadows.",
                    Ui.MAGENTA, Ui.AMBER, "12.1k"),
            new DiscoveryItem("Paper Moon", "Lyric Video", "Owen Vale",
                    "Paper Moon · 92 BPM", "Hand-set type drifts through a paper-cut city at dawn.",
                    Ui.CYAN, Ui.INDIGO, "9.8k"),
            new DiscoveryItem("Signal Fire", "OnBeat Effects", "Rin K.",
                    "Signal Fire · 118 BPM", "Graphic flashes and colour blocks cut against a single silhouette.",
                    Ui.EMERALD, Ui.EMERALD, "22.7k"),
            new DiscoveryItem("Sunday Portrait", "Photo Karaoke", "Ava Lune",
                    "Sunday Portrait · 84 BPM", "A quiet portrait performance in a sunlit apartment.",
                    Ui.AMBER, Ui.MAGENTA, "7.6k"),
            new DiscoveryItem("Velvet Current", "Music Cover Video", "Nico S.",
                    "Velvet Current · 108 BPM", "A painted cover breathes into a slow parallax loop.",
                    Ui.EMERALD, Ui.CYAN, "6.2k"),
            new DiscoveryItem("Neon Persona", "AI Persona", "Kai Studio",
                    "Persona · 110 BPM", "A digital human performs to an original track in a virtual studio.",
                    Ui.ROSE, Ui.INDIGO, "31.2k")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        modes = new Mode[]{
                new Mode("Music Video", "Prompt, cast and director-led scenes", "AGENT", R.drawable.ic_music_video, Ui.INDIGO),
                new Mode("Dance Video", "Choreography, camera and 8-counts", "MOVE", R.drawable.ic_dance, Ui.MAGENTA),
                new Mode("Lyric Video", "Timed type and visual rhythm", "TYPE", R.drawable.ic_lyric, Ui.CYAN),
                new Mode("OnBeat Effects", "Beat-synced graphic hits", "BETA", R.drawable.ic_onbeat, Ui.EMERALD),
                new Mode("Photo Karaoke", "Make a portrait sing", "LIP SYNC", R.drawable.ic_photo, Ui.AMBER),
                new Mode("Music Cover Video", "Animate artwork into motion", "MOTION", R.drawable.ic_cover, Ui.EMERALD),
                new Mode("Video to Music", "Score a clip with original music", "REVERSE", R.drawable.ic_v2m, Ui.CYAN),
                new Mode("Realtime", "Live-directed performance session", "LIVE", R.drawable.ic_realtime, Ui.AMBER),
                new Mode("AI Video", "Text-to-video generation", "AI-TXT", R.drawable.ic_ai_video, Ui.ROSE),
                new Mode("Image-to-Video", "Animate a still image", "AI-IMG", R.drawable.ic_i2v, Ui.ROSE),
                new Mode("AI Persona", "Digital human / avatar", "AVATAR", R.drawable.ic_persona, Ui.ROSE),
                new Mode("AI Canvas", "AI image generation", "AI-ART", R.drawable.ic_canvas, Ui.ROSE),
                new Mode("Auto Edit", "Smart cut and auto-reframe", "AUTO", R.drawable.ic_autoedit, Ui.ROSE),
                new Mode("Cutout & Chroma", "AI cutout and green screen", "CUT", R.drawable.ic_cutout, Ui.ROSE),
                new Mode("Voice & Captions", "TTS, voiceover and subtitles", "VOICE", R.drawable.ic_voice, Ui.ROSE),
                new Mode("Motion FX", "Stabilize, track, keyframes, 3D", "FX", R.drawable.ic_motionfx, Ui.ROSE)
        };

        draftPrefs = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE);
        cloud = new CloudApi(this);
        store = new TaskStore(this);
        if (!restoreDraft()) applyModeDefaults();
        buildShell();
        showTab(tab);
        resumeCloudRender();
    }

    private void buildShell() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);

        stage = new FrameLayout(this);
        root.addView(stage, Ui.lparams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content = new FrameLayout(this);
        stage.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        botButton = Ui.row(this);
        botButton.setGravity(Gravity.CENTER);
        botButton.setPadding(Ui.dp(this, 13), Ui.dp(this, 10), Ui.dp(this, 15), Ui.dp(this, 10));
        botButton.setBackground(Ui.gradient(Ui.CYAN, Ui.INDIGO, 100));
        botButton.setElevation(Ui.dp(this, 8));
        botButton.addView(Ui.icon(this, R.drawable.ic_spark, Ui.INK, 18));
        TextView botLabel = Ui.text(this, "Ask Director", 11.5f, Ui.INK, true);
        Ui.margin(botLabel, 6, 0, 0, 0);
        botButton.addView(botLabel);
        botButton.setClickable(true);
        botButton.setOnClickListener(v -> showAssistant());
        FrameLayout.LayoutParams botParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 44), Gravity.BOTTOM | Gravity.END);
        botParams.setMargins(0, 0, Ui.dp(this, 18), Ui.dp(this, 16));
        stage.addView(botButton, botParams);

        navBar = Ui.column(this);
        navBar.setBackgroundColor(0xFF0F111C);
        View line = new View(this);
        line.setBackgroundColor(0x1AFFFFFF);
        navBar.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));

        LinearLayout navRow = Ui.row(this);
        navRow.setGravity(Gravity.CENTER);
        navRow.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        navBar.addView(navRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));

        int[] icons = {R.drawable.ic_home, R.drawable.ic_discover, R.drawable.ic_create,
                R.drawable.ic_tasks, R.drawable.ic_profile};
        String[] labels = {"Home", "Discover", "Create", "Tasks", "Profile"};
        for (int i = 0; i < icons.length; i++) {
            final int index = i;
            LinearLayout item = Ui.column(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            ImageView icon = Ui.icon(this, icons[i], Ui.MUTED, 22);
            TextView label = Ui.text(this, labels[i], 9.5f, Ui.MUTED, true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, Ui.dp(this, 4), 0, 0);
            item.addView(icon, Ui.lparams(Ui.dp(this, 22), Ui.dp(this, 22)));
            item.addView(label);
            item.setClickable(true);
            item.setOnClickListener(v -> showTab(index));
            navRow.addView(item, Ui.lparams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            navItems.add(new NavItem(item, icon, label));
        }

        root.addView(navBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
            navBar.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    private void showTab(int index) {
        tab = index;
        content.removeAllViews();
        if (index == 0) buildHome();
        else if (index == 1) buildDiscover();
        else if (index == 2) buildCreate();
        else if (index == 3) buildTasks();
        else buildProfile();
        updateNav();
    }

    private void updateNav() {
        for (int i = 0; i < navItems.size(); i++) {
            NavItem item = navItems.get(i);
            boolean selected = i == tab;
            int color = selected ? Ui.CYAN : Ui.MUTED;
            item.icon.setImageTintList(ColorStateList.valueOf(color));
            item.label.setTextColor(color);
            item.view.setBackgroundColor(selected ? 0x14102B12 : Color.TRANSPARENT);
        }
    }

    private LinearLayout addPage(int top, int bottom) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = Ui.column(this);
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, top), Ui.dp(this, 20), Ui.dp(this, bottom));
        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return col;
    }

    private void buildHome() {
        LinearLayout col = addPage(16, 30);

        LinearLayout top = Ui.row(this);
        top.addView(Ui.iconTile(this, R.drawable.ic_logo, Ui.CYAN, Ui.SURFACE_2, 38));
        LinearLayout brand = Ui.column(this);
        brand.setPadding(Ui.dp(this, 10), 0, 0, 0);
        brand.addView(Ui.text(this, "Chad-VidCraft", 15f, Ui.TEXT, true));
        brand.addView(Ui.text(this, "Director's desk", 10.5f, Ui.MUTED, false));
        top.addView(brand, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView credits = Ui.pill(this, "500 CR", Ui.SURFACE, Ui.CYAN);
        credits.setOnClickListener(v -> showCredits());
        top.addView(credits);
        col.addView(top);

        col.addView(Ui.spacer(this, 22));

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(Ui.gradient(Ui.INDIGO, 0xFF2A174D, 26));
        hero.setClipToOutline(true);
        ArtView art = new ArtView(this, Ui.INDIGO, Ui.MAGENTA, false);
        art.setAlpha(0.52f);
        hero.addView(art, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout heroBody = Ui.column(this);
        heroBody.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        heroBody.addView(Ui.caps(this, "Director mode", 0xFFD9D0FF));
        heroBody.addView(Ui.spacer(this, 8));
        heroBody.addView(Ui.text(this, "Turn a track\ninto a world.", 29f, Color.WHITE, true));
        heroBody.addView(Ui.spacer(this, 7));
        TextView heroSub = Ui.text(this,
                "Brief, cast, scenes, shots, storyboard, final cut. Your original studio, end to end.",
                12f, 0xFFE7E1FF, false);
        heroSub.setMaxLines(3);
        heroBody.addView(heroSub);
        heroBody.addView(Ui.spacer(this, 18));
        LinearLayout actions = Ui.row(this);
        TextView start = Ui.button(this, "Start a film", Ui.CYAN, Ui.INK);
        start.setOnClickListener(v -> showTab(2));
        actions.addView(start, Ui.lparams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 46)));
        TextView ask = Ui.outlineButton(this, "Ask Director", Color.WHITE);
        ask.setOnClickListener(v -> showAssistant());
        Ui.margin(ask, 10, 0, 0, 0);
        actions.addView(ask, Ui.lparams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 46)));
        heroBody.addView(actions);
        hero.addView(heroBody, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));
        col.addView(hero, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 300)));

        col.addView(Ui.spacer(this, 14));

        LinearLayout insight = Ui.card(this);
        insight.setBackground(Ui.shape(0xFF182016, 20));
        LinearLayout insightRow = Ui.row(this);
        insightRow.addView(Ui.iconTile(this, R.drawable.ic_spark, Ui.CYAN, Ui.SURFACE_2, 42));
        LinearLayout insightText = Ui.column(this);
        insightText.setPadding(Ui.dp(this, 12), 0, 0, 0);
        insightText.addView(Ui.text(this, "Director Bot", 13.5f, Ui.TEXT, true));
        insightText.addView(Ui.text(this, "Need a concept, shot plan or model pick?", 11f, Ui.MUTED, false));
        insightRow.addView(insightText, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView open = Ui.pill(this, "OPEN", Ui.CYAN, Ui.INK);
        open.setOnClickListener(v -> showAssistant());
        insightRow.addView(open);
        insight.addView(insightRow);
        col.addView(insight);

        Task active = store.firstRendering();
        if (active != null) {
            col.addView(Ui.spacer(this, 20));
            Ui.sectionHeader(col, this, "In the edit", "Task center", () -> showTab(3));
            col.addView(Ui.spacer(this, 10));
            LinearLayout card = Ui.card(this);
            LinearLayout row = Ui.row(this);
            row.addView(Ui.iconTile(this, iconForMode(active.mode), colorForMode(active.mode),
                    withAlpha(colorForMode(active.mode), 0x22), 46));
            LinearLayout body = Ui.column(this);
            body.setPadding(Ui.dp(this, 12), 0, 0, 0);
            body.addView(Ui.text(this, active.title, 14f, Ui.TEXT, true));
            body.addView(Ui.text(this, active.stage + " · " + active.progress + "%", 10.5f, Ui.AMBER, false));
            row.addView(body, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView resume = Ui.pill(this, "CONTINUE", Ui.SURFACE_2, Ui.CYAN);
            resume.setOnClickListener(v -> showTaskDetail(active));
            row.addView(resume);
            card.addView(row);
            card.setOnClickListener(v -> showTaskDetail(active));
            col.addView(card);
        }

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "Fresh from Discover", "Explore", () -> showTab(1));
        col.addView(Ui.spacer(this, 10));
        HorizontalScrollView rail = new HorizontalScrollView(this);
        rail.setHorizontalScrollBarEnabled(false);
        LinearLayout railRow = Ui.row(this);
        for (int i = 0; i < 3; i++) {
            DiscoveryItem item = discoveries[i];
            LinearLayout card = discoveryCard(item, true);
            card.setOnClickListener(v -> showDiscoveryDetail(item));
            railRow.addView(card, Ui.lparams(Ui.dp(this, 172), ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.margin(card, i == 0 ? 0 : 10, 0, 0, 4);
        }
        rail.addView(railRow);
        col.addView(rail);

        col.addView(Ui.spacer(this, 22));
        Ui.sectionHeader(col, this, "Create", modes.length + " modes", () -> showTab(2));
        col.addView(Ui.spacer(this, 12));
        for (int i = 0; i < modes.length; i += 2) {
            LinearLayout row = Ui.row(this);
            for (int j = i; j < Math.min(i + 2, modes.length); j++) {
                final int modeIndex = j;
                LinearLayout card = modeCard(modes[j]);
                card.setOnClickListener(v -> {
                    createMode = modeIndex;
                    applyModeDefaults();
                    showTab(2);
                });
                row.addView(card, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                Ui.margin(card, j % 2 == 0 ? 0 : 5, 0, j % 2 == 0 ? 5 : 0, 10);
            }
            if ((modes.length - i) == 1) {
                View filler = new View(this);
                row.addView(filler, Ui.lparams(0, 1, 1f));
                Ui.margin(filler, 5, 0, 0, 10);
            }
            col.addView(row);
        }

        col.addView(Ui.spacer(this, 16));
        Ui.sectionHeader(col, this, "Recent work", "All tasks", () -> showTab(3));
        col.addView(Ui.spacer(this, 10));
        List<Task> tasks = store.all();
        int shown = 0;
        for (Task task : tasks) {
            if (shown >= 3) break;
            View row = taskRow(task, false);
            col.addView(row);
            Ui.margin(row, 0, 0, 0, 10);
            shown++;
        }

        col.addView(Ui.spacer(this, 16));
        LinearLayout models = Ui.card(this);
        models.addView(Ui.caps(this, "Studio stack", Ui.MUTED));
        models.addView(Ui.spacer(this, 6));
        models.addView(Ui.text(this, "Chad Canvas · Chad Motion · Chad Persona · Chad Voice",
                16f, Ui.TEXT, true));
        models.addView(Ui.spacer(this, 4));
        models.addView(Ui.text(this, "Model choices are original Chad configuration. Brief sync runs through your own cloud server; no provider key ships in the APK.",
                11f, Ui.MUTED, false));
        col.addView(models);

        col.addView(Ui.spacer(this, 20));
        TextView footer = Ui.text(this, "Original studio · original icons · independently designed",
                10.5f, 0xFF6F6F8A, false);
        footer.setGravity(Gravity.CENTER);
        col.addView(footer);
    }

    private int withAlpha(int base, int overlay) {
        int a = (overlay >>> 24) & 0xFF;
        int r = ((base >>> 16) & 0xFF) * a / 255 + ((overlay >>> 16) & 0xFF) * (255 - a) / 255;
        int g = ((base >>> 8) & 0xFF) * a / 255 + ((overlay >>> 8) & 0xFF) * (255 - a) / 255;
        int b = (base & 0xFF) * a / 255 + (overlay & 0xFF) * (255 - a) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int colorForMode(String mode) {
        for (Mode m : modes) if (m.name.equals(mode)) return m.color;
        return Ui.CYAN;
    }

    private int iconForMode(String mode) {
        for (Mode m : modes) if (m.name.equals(mode)) return m.icon;
        return R.drawable.ic_music_video;
    }

    private LinearLayout modeCard(Mode mode) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        card.setMinimumHeight(Ui.dp(this, 138));
        ImageView icon = Ui.iconTile(this, mode.icon, mode.color,
                withAlpha(mode.color, 0x22), 38);
        card.addView(icon);
        TextView title = Ui.text(this, mode.name, 14.5f, Ui.TEXT, true);
        Ui.margin(title, 0, 12, 0, 0);
        card.addView(title);
        TextView desc = Ui.text(this, mode.desc, 10.5f, Ui.MUTED, false);
        desc.setMaxLines(2);
        card.addView(desc);
        card.addView(Ui.spacer(this, 8));
        card.addView(Ui.caps(this, mode.tag, mode.color));
        return card;
    }

    private LinearLayout discoveryCard(DiscoveryItem item, boolean compact) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 12));
        FrameLayout art = new FrameLayout(this);
        ArtView view = new ArtView(this, item.colorA, item.colorB, false);
        art.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, compact ? Ui.dp(this, 96) : Ui.dp(this, 150)));
        art.setClipToOutline(true);
        TextView tag = Ui.pill(this, item.mode.toUpperCase(Locale.US), 0x99000000, Color.WHITE);
        FrameLayout.LayoutParams tagParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        tagParams.setMargins(Ui.dp(this, 8), Ui.dp(this, 8), 0, 0);
        art.addView(tag, tagParams);
        card.addView(art);
        TextView title = Ui.text(this, item.title, 13.5f, Ui.TEXT, true);
        title.setMaxLines(1);
        Ui.margin(title, 0, 8, 0, 0);
        card.addView(title);
        card.addView(Ui.text(this, item.creator + " · " + item.plays + " views", 10f, Ui.MUTED, false));
        return card;
    }

    private void buildDiscover() {
        LinearLayout col = addPage(16, 30);

        LinearLayout top = Ui.row(this);
        LinearLayout heading = Ui.column(this);
        heading.addView(Ui.caps(this, "Community", Ui.CYAN));
        heading.addView(Ui.text(this, "Discover", 30f, Ui.TEXT, true));
        top.addView(heading, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView remix = Ui.outlineButton(this, "Remix", Ui.TEXT);
        remix.setOnClickListener(v -> {
            createMode = 0;
            applyModeDefaults();
            showTab(2);
        });
        top.addView(remix);
        col.addView(top);

        col.addView(Ui.spacer(this, 14));

        final EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search styles, creators, tracks");
        search.setText(discoverQuery);
        search.setTextSize(13f);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED);
        search.setBackground(Ui.shape(Ui.SURFACE, 16));
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(Ui.dp(this, 8));
        search.setCompoundDrawableTintList(ColorStateList.valueOf(Ui.MUTED));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                discoverQuery = s.toString().trim();
                refreshDiscoverFeed();
            }
        });
        col.addView(search);

        col.addView(Ui.spacer(this, 12));

        HorizontalScrollView filters = new HorizontalScrollView(this);
        filters.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = Ui.row(this);
        String[] options = {"All", "Music Video", "Dance Video", "Lyric Video", "OnBeat Effects",
                "Photo Karaoke", "AI Video", "AI Persona", "Motion FX"};
        for (String option : options) {
            TextView chip = Ui.chip(this, option);
            Ui.styleChip(chip, option.equals(discoverFilter), Ui.CYAN);
            chip.setOnClickListener(v -> {
                discoverFilter = option;
                showTab(1);
            });
            filterRow.addView(chip);
            Ui.margin(chip, filterRow.getChildCount() == 1 ? 0 : 8, 0, 0, 0);
        }
        filters.addView(filterRow);
        col.addView(filters);

        col.addView(Ui.spacer(this, 16));

        LinearLayout featured = Ui.column(this);
        featured.setBackground(Ui.gradient(Ui.CYAN, Ui.INDIGO, 24));
        featured.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        featured.setClipToOutline(true);
        featured.addView(Ui.caps(this, "Featured this week", 0xFFE3F6FF));
        featured.addView(Ui.spacer(this, 8));
        featured.addView(Ui.text(this, "Make the beat\nvisible.", 25f, Color.WHITE, true));
        featured.addView(Ui.spacer(this, 6));
        featured.addView(Ui.text(this, "A curated runway of original prompt-first music films.",
                11f, 0xFFEAF7FF, false));
        featured.addView(Ui.spacer(this, 14));
        TextView explore = Ui.button(this, "Open collection", Ui.CYAN, Ui.INK);
        explore.setOnClickListener(v -> showCollection());
        featured.addView(explore, Ui.lparams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 44)));
        col.addView(featured);

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "Made with Chad", discoveries.length + " films", null);
        col.addView(Ui.spacer(this, 10));

        discoverFeed = Ui.column(this);
        col.addView(discoverFeed);
        refreshDiscoverFeed();
    }

    private void refreshDiscoverFeed() {
        if (discoverFeed == null) return;
        discoverFeed.removeAllViews();
        int count = 0;
        for (DiscoveryItem item : discoveries) {
            if (!"All".equals(discoverFilter) && !item.mode.equals(discoverFilter)) continue;
            if (!discoverQuery.isEmpty()) {
                String haystack = (item.title + " " + item.creator + " " + item.track + " "
                        + item.description).toLowerCase(Locale.US);
                if (!haystack.contains(discoverQuery.toLowerCase(Locale.US))) continue;
            }
            LinearLayout card = discoveryWideCard(item);
            card.setOnClickListener(v -> showDiscoveryDetail(item));
            discoverFeed.addView(card);
            Ui.margin(card, 0, 0, 0, 12);
            count++;
        }
        if (count == 0) {
            LinearLayout empty = Ui.card(this);
            empty.setGravity(Gravity.CENTER);
            empty.addView(Ui.icon(this, R.drawable.ic_search, Ui.MUTED, 28));
            TextView text = Ui.text(this, "No films match that search.", 13f, Ui.MUTED, false);
            text.setGravity(Gravity.CENTER);
            Ui.margin(text, 0, 10, 0, 0);
            empty.addView(text);
            discoverFeed.addView(empty);
        }
    }

    private LinearLayout discoveryWideCard(DiscoveryItem item) {
        LinearLayout card = Ui.card(this);
        LinearLayout row = Ui.row(this);
        FrameLayout preview = new FrameLayout(this);
        ArtView art = new ArtView(this, item.colorA, item.colorB, false);
        preview.addView(art, new FrameLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 104)));
        preview.setClipToOutline(true);
        row.addView(preview, Ui.lparams(Ui.dp(this, 104), Ui.dp(this, 104)));
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 13), 0, 0, 0);
        body.addView(Ui.caps(this, item.mode, item.colorA));
        body.addView(Ui.text(this, item.title, 15f, Ui.TEXT, true));
        body.addView(Ui.text(this, item.creator + " · " + item.plays + " views", 10.5f, Ui.MUTED, false));
        TextView track = Ui.pill(this, "♪ " + item.track, Ui.SURFACE_2, Ui.TEXT);
        track.setMaxLines(1);
        body.addView(track);
        Ui.margin(track, 0, 7, 0, 0);
        row.addView(body, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    private void showCollection() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Collection 01", Ui.CYAN));
        panel.addView(Ui.text(this, "Beat becomes image", 24f, Ui.TEXT, true));
        panel.addView(Ui.spacer(this, 8));
        panel.addView(Ui.text(this, "Three original directions to start from. Each one opens a fresh brief; nothing is copied from another app.",
                12f, Ui.MUTED, false));
        panel.addView(Ui.spacer(this, 16));
        String[] titles = {"Neon performance", "Graphic rhythm", "Quiet persona"};
        String[] bodies = {"Rooftop, one moving light, slow push-ins.",
                "Hard cuts, colour blocks, beat-locked type.",
                "A digital human in a virtual studio, soft rim light."};
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this, titles[i], 14f, Ui.TEXT, true));
            card.addView(Ui.text(this, bodies[i], 10.5f, Ui.MUTED, false));
            card.setOnClickListener(v -> {
                dialog.dismiss();
                createMode = 0;
                applyModeDefaults();
                if (index == 0) {
                    createPrompt = "A midnight rooftop performance. One moving sodium light, wet concrete, slow push-ins, silver jacket, anamorphic flares.";
                    aesthetic = "Cinematic noir";
                    world = "Rooftops above a sleeping city";
                } else if (index == 1) {
                    createMode = 3;
                    applyModeDefaults();
                    createPrompt = "Graphic beat hits, hard colour blocks, type that lands on every kick, silhouette performance.";
                    aesthetic = "Swiss poster";
                    world = "A white cyclorama with black geometric shadows";
                } else {
                    createMode = 10;
                    applyModeDefaults();
                    createPrompt = "A digital human performs to an original track in a virtual studio, soft rim light, cinematic depth.";
                    personaEnabled = true;
                    personaLook = "Studio presenter";
                    aesthetic = "Virtual studio";
                    world = "A clean virtual stage with soft key light";
                }
                showTab(2);
            });
            panel.addView(card);
            Ui.margin(card, 0, 0, 0, 10);
        }
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.92, 0.82);
    }

    private void showDiscoveryDetail(DiscoveryItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);

        FrameLayout preview = new FrameLayout(this);
        ArtView art = new ArtView(this, item.colorA, item.colorB, true);
        preview.addView(art, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 220)));
        panel.addView(preview);

        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18));
        body.addView(Ui.caps(this, item.mode, item.colorA));
        body.addView(Ui.text(this, item.title, 24f, Ui.TEXT, true));
        body.addView(Ui.text(this, item.creator + " · " + item.plays + " views", 11f, Ui.MUTED, false));
        body.addView(Ui.spacer(this, 10));
        body.addView(Ui.text(this, item.description, 12.5f, 0xFFD6D6E8, false));
        body.addView(Ui.spacer(this, 12));
        body.addView(Ui.pill(this, "♪ " + item.track, Ui.SURFACE_2, Ui.TEXT));
        body.addView(Ui.spacer(this, 16));

        LinearLayout actions = Ui.row(this);
        TextView watch = Ui.button(this, "Preview", Ui.SURFACE_2, Ui.TEXT);
        watch.setOnClickListener(v -> {
            dialog.dismiss();
            showPreview(null, item.title, item.colorA, item.colorB);
        });
        actions.addView(watch, Ui.lparams(0, Ui.dp(this, 46), 1f));
        TextView use = Ui.button(this, "Remix this brief", Ui.CYAN, Ui.INK);
        use.setOnClickListener(v -> {
            dialog.dismiss();
            createMode = modeIndexFor(item.mode);
            applyModeDefaults();
            createPrompt = item.description + " Reinterpret the mood with a new subject and original framing.";
            aesthetic = item.title + " mood";
            world = item.track;
            showTab(2);
        });
        Ui.margin(use, 10, 0, 0, 0);
        actions.addView(use, Ui.lparams(0, Ui.dp(this, 46), 1.25f));
        body.addView(actions);

        TextView close = Ui.text(this, "Close", 12f, Ui.MUTED, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, Ui.dp(this, 14), 0, 0);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close);

        panel.addView(body);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        dialog.setContentView(scroll);
        showDialog(dialog, 0.94, 0.9);
    }

    private int modeIndexFor(String name) {
        for (int i = 0; i < modes.length; i++) if (modes[i].name.equals(name)) return i;
        return 0;
    }

    private void buildCreate() {
        modeChips.clear();
        ratioChips.clear();
        modelChips.clear();
        promptInput = null;
        createEstimate = null;
        durationLabel = null;
        generateBtn = null;
        openTaskBtn = null;
        progressCard = null;
        stageList = null;
        progressBar = null;
        progressPct = null;
        progressStage = null;

        LinearLayout col = addPage(16, 34);

        LinearLayout top = Ui.row(this);
        LinearLayout heading = Ui.column(this);
        heading.addView(Ui.caps(this, "New project", Ui.CYAN));
        heading.addView(Ui.text(this, "Create", 30f, Ui.TEXT, true));
        top.addView(heading, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView reset = Ui.outlineButton(this, "Reset", Ui.MUTED);
        reset.setOnClickListener(v -> {
            resetDraft();
            showTab(2);
        });
        top.addView(reset);
        col.addView(top);

        col.addView(Ui.spacer(this, 14));

        HorizontalScrollView modeRail = new HorizontalScrollView(this);
        modeRail.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(this);
        for (int i = 0; i < modes.length; i++) {
            final int index = i;
            Mode m = modes[i];
            TextView chip = Ui.chip(this, m.name);
            chip.setCompoundDrawablesWithIntrinsicBounds(m.icon, 0, 0, 0);
            chip.setCompoundDrawablePadding(Ui.dp(this, 6));
            chip.setCompoundDrawableTintList(ColorStateList.valueOf(
                    index == createMode ? Ui.INK : Ui.MUTED));
            Ui.styleChip(chip, index == createMode, Ui.CYAN);
            chip.setOnClickListener(v -> {
                createMode = index;
                applyModeDefaults();
                showTab(2);
            });
            chips.addView(chip);
            Ui.margin(chip, i == 0 ? 0 : 8, 0, 0, 0);
        }
        modeRail.addView(chips);
        col.addView(modeRail);

        col.addView(Ui.spacer(this, 12));

        LinearLayout modeInfo = Ui.card(this);
        modeInfo.setBackground(Ui.gradient(withAlpha(modes[createMode].color, 0x33),
                Ui.SURFACE, 22));
        LinearLayout infoRow = Ui.row(this);
        infoRow.addView(Ui.iconTile(this, modes[createMode].icon, modes[createMode].color,
                withAlpha(modes[createMode].color, 0x33), 44));
        LinearLayout infoText = Ui.column(this);
        infoText.setPadding(Ui.dp(this, 12), 0, 0, 0);
        infoText.addView(Ui.text(this, modes[createMode].name, 16f, Ui.TEXT, true));
        infoText.addView(Ui.text(this, modes[createMode].desc, 11f, Ui.MUTED, false));
        infoRow.addView(infoText, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        infoRow.addView(Ui.caps(this, modes[createMode].tag, modes[createMode].color));
        modeInfo.addView(infoRow);
        col.addView(modeInfo);

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "1 · Music", "Import", this::importMusic);
        col.addView(Ui.spacer(this, 10));
        col.addView(buildMusicCard());

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "2 · Brief", "Director fill", () -> fillBriefWithBot());
        col.addView(Ui.spacer(this, 10));
        col.addView(buildBriefCard());

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "3 · Direction", null, null);
        col.addView(Ui.spacer(this, 10));
        col.addView(buildDirectionCard());

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "4 · Cast & references", "Add image", this::pickReference);
        col.addView(Ui.spacer(this, 10));
        col.addView(buildReferencesCard());

        col.addView(Ui.spacer(this, 20));
        Ui.sectionHeader(col, this, "5 · Output", null, null);
        col.addView(Ui.spacer(this, 10));
        col.addView(buildOutputCard());

        col.addView(Ui.spacer(this, 18));
        progressCard = buildProgressCard();
        progressCard.setVisibility(View.GONE);
        col.addView(progressCard);

        col.addView(Ui.spacer(this, 18));
        generateBtn = Ui.button(this, "Generate  ·  " + estimateCredits() + " credits", Ui.CYAN, Ui.INK);
        generateBtn.setTextSize(15f);
        generateBtn.setOnClickListener(v -> startGeneration());
        col.addView(generateBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)));

        openTaskBtn = Ui.outlineButton(this, "Open latest task", Ui.TEXT);
        openTaskBtn.setOnClickListener(v -> {
            Task latest = store.latest();
            if (latest != null) showTaskDetail(latest);
            else showTab(3);
        });
        Ui.margin(openTaskBtn, 0, 10, 0, 0);
        col.addView(openTaskBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

        TextView note = Ui.text(this,
                "Studio preview renders on this device. Sign in and sync the brief to move it to your cloud workspace.",
                10.5f, Ui.MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, Ui.dp(this, 12), 0, 0);
        col.addView(note);

        restoreActiveProgress();
    }

    private LinearLayout buildMusicCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout row = Ui.row(this);
        FrameLayout art = new FrameLayout(this);
        art.setBackground(Ui.gradient(Ui.MAGENTA, Ui.INDIGO, 16));
        art.addView(Ui.icon(this, R.drawable.ic_music, Color.WHITE, 24),
                new FrameLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24), Gravity.CENTER));
        row.addView(art, Ui.lparams(Ui.dp(this, 58), Ui.dp(this, 58)));

        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 12), 0, 0, 0);
        body.addView(Ui.text(this, musicTitle, 15f, Ui.TEXT, true));
        String meta = musicSource + " · " + musicBpm + " BPM · " + musicKey;
        body.addView(Ui.text(this, meta, 10.5f, Ui.MUTED, false));
        body.addView(Ui.text(this, musicMood, 10.5f, Ui.MAGENTA, true));
        row.addView(body, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView change = Ui.pill(this, "CHANGE", Ui.SURFACE_2, Ui.TEXT);
        change.setOnClickListener(v -> showMusicPicker());
        row.addView(change);
        card.addView(row);

        if (musicUrl != null && !musicUrl.isEmpty()) {
            card.addView(Ui.spacer(this, 10));
            card.addView(Ui.text(this, "Linked file: " + shortName(musicUrl), 10f, Ui.EMERALD, false));
        }

        card.addView(Ui.spacer(this, 12));
        LinearLayout actions = Ui.row(this);
        TextView analyze = Ui.outlineButton(this, "Analyze beat", Ui.TEXT);
        analyze.setOnClickListener(v -> analyzeMusic());
        actions.addView(analyze, Ui.lparams(0, Ui.dp(this, 40), 1f));
        TextView library = Ui.outlineButton(this, "Demo tracks", Ui.TEXT);
        library.setOnClickListener(v -> showMusicPicker());
        Ui.margin(library, 8, 0, 0, 0);
        actions.addView(library, Ui.lparams(0, Ui.dp(this, 40), 1f));
        card.addView(actions);

        card.addView(Ui.spacer(this, 10));
        card.addView(buildBeatBars());
        return card;
    }

    private LinearLayout buildBeatBars() {
        LinearLayout bars = Ui.row(this);
        bars.setGravity(Gravity.BOTTOM);
        int[] heights = {12, 20, 31, 22, 40, 27, 35, 18, 29, 42, 24, 33, 16, 38, 26, 30, 19, 36, 23, 32, 15, 28, 41, 21};
        for (int i = 0; i < heights.length; i++) {
            View bar = new View(this);
            bar.setBackground(Ui.shape(i % 4 == 0 ? Ui.MAGENTA : Ui.INDIGO, 3));
            bars.addView(bar, Ui.lparams(0, Ui.dp(this, heights[i]), 1f));
            Ui.margin(bar, 1, 0, 1, 0);
        }
        return bars;
    }

    private LinearLayout buildBriefCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "What are we making?", 13f, Ui.TEXT, true));
        card.addView(Ui.spacer(this, 8));

        EditText prompt = new EditText(this);
        prompt.setHint("Describe the film, mood, performance and visual idea");
        prompt.setText(createPrompt);
        prompt.setTextSize(13f);
        prompt.setTextColor(Ui.TEXT);
        prompt.setHintTextColor(Ui.MUTED);
        prompt.setGravity(Gravity.TOP);
        prompt.setMinLines(4);
        prompt.setMaxLines(7);
        prompt.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        prompt.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        prompt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                createPrompt = s.toString();
            }
        });
        promptInput = prompt;
        card.addView(prompt, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 108)));

        card.addView(Ui.spacer(this, 12));
        addField(card, "Concept", concept.isEmpty() ? "A visual idea in one sentence" : concept,
                value -> concept = value);
        addField(card, "Aesthetic", aesthetic, value -> aesthetic = value);
        addField(card, "World / location", world, value -> world = value);
        addField(card, "Characters / cast", characters, value -> characters = value);

        card.addView(Ui.spacer(this, 4));
        LinearLayout suggestions = Ui.row(this);
        TextView shorthand = Ui.text(this, "Quick briefs", 10.5f, Ui.MUTED, true);
        suggestions.addView(shorthand);
        TextView idea = Ui.pill(this, "SURPRISE ME", Ui.SURFACE_2, Ui.CYAN);
        idea.setOnClickListener(v -> surprisingBrief());
        Ui.margin(idea, 8, 0, 0, 0);
        suggestions.addView(idea);
        card.addView(suggestions);
        return card;
    }

    private void addField(LinearLayout parent, String label, String value, Sink sink) {
        LinearLayout row = Ui.row(this);
        TextView name = Ui.text(this, label, 10.5f, Ui.MUTED, true);
        name.setWidth(Ui.dp(this, 92));
        row.addView(name);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setTextSize(12f);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setHint(label);
        input.setBackground(null);
        input.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                sink.set(s.toString());
            }
        });
        row.addView(input, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
        parent.addView(Ui.divider(this));
    }

    private LinearLayout buildReferencesCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Character anchors keep the same face and silhouette across scenes.",
                12f, Ui.MUTED, false));
        card.addView(Ui.spacer(this, 6));
        addField(card, "Character A", characterA.isEmpty() ? "Lead · face, hair, wardrobe" : characterA,
                value -> characterA = value);
        addField(card, "Character B", characterB.isEmpty() ? "Optional second subject" : characterB,
                value -> characterB = value);
        card.addView(Ui.spacer(this, 8));
        card.addView(Ui.text(this, referenceUris.isEmpty()
                ? "No image references attached."
                : referenceUris.size() + " reference" + (referenceUris.size() == 1 ? "" : "s") + " attached",
                10.5f, referenceUris.isEmpty() ? Ui.MUTED : Ui.TEXT, false));
        card.addView(Ui.spacer(this, 10));

        HorizontalScrollView rail = new HorizontalScrollView(this);
        rail.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(this);
        for (int i = 0; i < referenceUris.size(); i++) {
            final int index = i;
            FrameLayout thumb = new FrameLayout(this);
            thumb.setBackground(Ui.gradient(i % 2 == 0 ? Ui.CYAN : Ui.INDIGO, Ui.SURFACE_2, 14));
            ImageView icon = Ui.icon(this, R.drawable.ic_image, Color.WHITE, 22);
            thumb.addView(icon, new FrameLayout.LayoutParams(
                    Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
            TextView remove = Ui.pill(this, "×", 0xAA000000, Color.WHITE);
            FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            rp.setMargins(0, Ui.dp(this, 4), Ui.dp(this, 4), 0);
            thumb.addView(remove, rp);
            remove.setOnClickListener(v -> {
                referenceUris.remove(index);
                showTab(2);
            });
            row.addView(thumb, Ui.lparams(Ui.dp(this, 76), Ui.dp(this, 76)));
            Ui.margin(thumb, i == 0 ? 0 : 8, 0, 0, 0);
        }
        TextView add = Ui.outlineButton(this, "+ Add", Ui.TEXT);
        add.setOnClickListener(v -> pickReference());
        row.addView(add, Ui.lparams(Ui.dp(this, 76), Ui.dp(this, 76)));
        Ui.margin(add, referenceUris.isEmpty() ? 0 : 8, 0, 0, 0);
        rail.addView(row);
        card.addView(rail);

        card.addView(Ui.spacer(this, 10));
        card.addView(Ui.text(this, "References are stored as local URIs in this demo. Upload starts with the API phase.",
                10f, Ui.MUTED, false));
        return card;
    }

    private LinearLayout buildDirectionCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.caps(this, "Mode direction", modes[createMode].color));
        card.addView(Ui.spacer(this, 8));
        LinearLayout host = Ui.column(this);
        directionHost = host;
        card.addView(host);
        renderDirection(host);
        return card;
    }

    private void renderDirection(LinearLayout host) {
        host.removeAllViews();
        directionRefresher = () -> renderDirection(host);
        String mode = modes[createMode].name;

        if ("Music Video".equals(mode)) {
            addDirField(host, "Variant", variant, v -> variant = v,
                    new String[]{"Performance", "Narrative", "Abstract"});
            addDirField(host, "Camera", camera, v -> camera = v,
                    new String[]{"Orbit", "Slow push in", "Locked wide", "Handheld follow"});
        } else if ("Dance Video".equals(mode)) {
            addDirField(host, "Choreography", choreo, v -> choreo = v,
                    new String[]{"Solo", "Duet", "Group", "Street"});
            addDirField(host, "Camera", camera, v -> camera = v,
                    new String[]{"Wide", "Orbit", "Follow", "Locked"});
        } else if ("Lyric Video".equals(mode)) {
            addDirField(host, "Look", lookStyle, v -> lookStyle = v,
                    new String[]{"Abstract", "Hand-painted", "Kinetic", "Minimal"});
            addDirText(host, "Lyrics", lyricText, v -> lyricText = v);
            addDirToggle(host, "Captions", captionsEnabled ? "true" : "false",
                    v -> captionsEnabled = Boolean.parseBoolean(v));
        } else if ("OnBeat Effects".equals(mode)) {
            addDirField(host, "Effect family", effectFamily, v -> effectFamily = v,
                    new String[]{"Flash", "Glow", "Transform", "Liquid"});
            addDirField(host, "Template", effectTemplate, v -> effectTemplate = v,
                    new String[]{"Flash grid", "Pulse blocks", "Colour wave"});
        } else if ("Photo Karaoke".equals(mode)) {
            addDirField(host, "Scene preset", scenePreset, v -> scenePreset = v,
                    new String[]{"Studio", "Jazz club", "Bar guitar", "Home sofa"});
            addDirField(host, "Motion", motionPreset, v -> motionPreset = v,
                    new String[]{"Parallax loop", "Slow drift", "Rim light"});
        } else if ("Music Cover Video".equals(mode)) {
            addDirField(host, "Motion", motionPreset, v -> motionPreset = v,
                    new String[]{"Parallax loop", "Depth layers", "Breathing", "Still"});
        } else if ("Video to Music".equals(mode)) {
            addDirText(host, "Source clip", sourceClip.isEmpty() ? "Pick a clip" : sourceClip,
                    v -> sourceClip = v);
        } else if ("Realtime".equals(mode)) {
            addDirText(host, "Start image", startImage.isEmpty() ? "Pick an image" : startImage,
                    v -> startImage = v);
        } else if ("AI Video".equals(mode)) {
            addDirField(host, "Video model", videoModel, v -> videoModel = v,
                    new String[]{"motion-ai", "motion-1", "motion-style"});
            addDirToggle(host, "Stabilize", stabilizeEnabled ? "true" : "false",
                    v -> stabilizeEnabled = Boolean.parseBoolean(v));
        } else if ("Image-to-Video".equals(mode)) {
            addDirField(host, "Video model", videoModel, v -> videoModel = v,
                    new String[]{"motion-i2v", "motion-ai", "motion-1"});
            addDirToggle(host, "Stabilize", stabilizeEnabled ? "true" : "false",
                    v -> stabilizeEnabled = Boolean.parseBoolean(v));
        } else if ("AI Persona".equals(mode)) {
            addDirField(host, "Persona look", personaLook, v -> personaLook = v,
                    new String[]{"default", "presenter", "performer", "host"});
            addDirToggle(host, "Persona enabled", personaEnabled ? "true" : "false",
                    v -> personaEnabled = Boolean.parseBoolean(v));
        } else if ("AI Canvas".equals(mode)) {
            addDirField(host, "Image model", imageModel, v -> imageModel = v,
                    new String[]{"canvas-fast", "canvas-pro", "ink-1"});
        } else if ("Auto Edit".equals(mode)) {
            addDirField(host, "Camera", camera, v -> camera = v,
                    new String[]{"Orbit", "Slow push in", "Locked wide", "Handheld follow"});
            addDirToggle(host, "Beat sync", beatSyncEnabled ? "true" : "false",
                    v -> beatSyncEnabled = Boolean.parseBoolean(v));
            addDirToggle(host, "Auto-reframe", stabilizeEnabled ? "true" : "false",
                    v -> stabilizeEnabled = Boolean.parseBoolean(v));
        } else if ("Cutout & Chroma".equals(mode)) {
            addDirToggle(host, "AI cutout", cutoutEnabled ? "true" : "false",
                    v -> cutoutEnabled = Boolean.parseBoolean(v));
            addDirToggle(host, "Chroma key", chromaEnabled ? "true" : "false",
                    v -> chromaEnabled = Boolean.parseBoolean(v));
        } else if ("Voice & Captions".equals(mode)) {
            addDirField(host, "TTS voice", ttsVoice, v -> ttsVoice = v,
                    new String[]{"neutral", "warm", "bright", "calm"});
            addDirToggle(host, "TTS enabled", ttsEnabled ? "true" : "false",
                    v -> ttsEnabled = Boolean.parseBoolean(v));
            addDirToggle(host, "Captions", captionsEnabled ? "true" : "false",
                    v -> captionsEnabled = Boolean.parseBoolean(v));
        } else if ("Motion FX".equals(mode)) {
            addDirToggle(host, "Stabilize", stabilizeEnabled ? "true" : "false",
                    v -> stabilizeEnabled = Boolean.parseBoolean(v));
            addDirToggle(host, "Motion track", beatSyncEnabled ? "true" : "false",
                    v -> beatSyncEnabled = Boolean.parseBoolean(v));
            addDirToggle(host, "Keyframes", captionsEnabled ? "true" : "false",
                    v -> captionsEnabled = Boolean.parseBoolean(v));
        }
    }

    private void addDirField(LinearLayout host, String label, String value, Sink sink, String[] options) {
        LinearLayout row = Ui.row(this);
        TextView name = Ui.text(this, label, 10.5f, Ui.MUTED, true);
        name.setWidth(Ui.dp(this, 92));
        row.addView(name);
        LinearLayout opts = Ui.row(this);
        for (String opt : options) {
            final String o = opt;
            TextView chip = Ui.chip(this, opt);
            Ui.styleChip(chip, value.equals(opt), Ui.CYAN);
            chip.setOnClickListener(v -> {
                sink.set(o);
                renderDirection(directionHost);
            });
            opts.addView(chip);
            Ui.margin(chip, opts.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        row.addView(opts, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        host.addView(row);
        host.addView(Ui.divider(this));
    }

    private void addDirText(LinearLayout host, String label, String value, Sink sink) {
        LinearLayout row = Ui.row(this);
        TextView name = Ui.text(this, label, 10.5f, Ui.MUTED, true);
        name.setWidth(Ui.dp(this, 92));
        row.addView(name);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setTextSize(12f);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setHint(label);
        input.setBackground(null);
        input.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                sink.set(s.toString());
            }
        });
        row.addView(input, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        host.addView(row);
        host.addView(Ui.divider(this));
    }

    private void addDirToggle(LinearLayout host, String label, String value, Sink sink) {
        LinearLayout row = Ui.row(this);
        TextView name = Ui.text(this, label, 10.5f, Ui.MUTED, true);
        name.setWidth(Ui.dp(this, 92));
        row.addView(name);
        TextView on = Ui.pill(this, "ON", "true".equals(value) ? Ui.CYAN : Ui.SURFACE_2,
                "true".equals(value) ? Ui.INK : Ui.MUTED);
        TextView off = Ui.pill(this, "OFF", "false".equals(value) || value.isEmpty() ? Ui.CYAN : Ui.SURFACE_2,
                ("false".equals(value) || value.isEmpty()) ? Ui.INK : Ui.MUTED);
        on.setOnClickListener(v -> { sink.set("true"); renderDirection(directionHost); });
        off.setOnClickListener(v -> { sink.set("false"); renderDirection(directionHost); });
        row.addView(on);
        row.addView(off);
        Ui.margin(off, 6, 0, 0, 0);
        host.addView(row);
        host.addView(Ui.divider(this));
    }

    private LinearLayout buildOutputCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Resolution, duration, model and feature toggles.", 12f, Ui.MUTED, false));
        card.addView(Ui.spacer(this, 6));

        LinearLayout ratioRow = Ui.row(this);
        ratioRow.addView(Ui.text(this, "Ratio", 10.5f, Ui.MUTED, true));
        ratioRow.addView(Ui.spacer(this, 0));
        String[] ratios = {"16:9", "9:16", "1:1", "4:3", "3:4"};
        for (String r : ratios) {
            final String rr = r;
            TextView chip = Ui.chip(this, r);
            Ui.styleChip(chip, createRatio.equals(rr), Ui.CYAN);
            chip.setOnClickListener(v -> createRatio = rr);
            ratioRow.addView(chip);
            Ui.margin(chip, ratioRow.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        card.addView(ratioRow);

        LinearLayout durRow = Ui.row(this);
        durRow.addView(Ui.text(this, "Duration (s)", 10.5f, Ui.MUTED, true));
        SeekBar seek = new SeekBar(this);
        seek.setProgress(createDuration - 10);
        seek.setMax(290);
        seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar bar, int p, boolean from) {
                createDuration = p + 10;
                if (durationLabel != null) durationLabel.setText(createDuration + "s");
                if (createEstimate != null) createEstimate.setText("Generate  ·  " + estimateCredits() + " credits");
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar bar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar bar) {}
        });
        durRow.addView(seek, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        durationLabel = Ui.text(this, createDuration + "s", 12f, Ui.TEXT, true);
        durRow.addView(durationLabel);
        card.addView(durRow);

        LinearLayout styleRow = Ui.row(this);
        styleRow.addView(Ui.text(this, "Style", 10.5f, Ui.MUTED, true));
        String[] styles = {"Studio", "Fast", "Custom"};
        for (String s : styles) {
            final String ss = s;
            TextView chip = Ui.chip(this, s);
            Ui.styleChip(chip, createModel.equals(ss), Ui.CYAN);
            chip.setOnClickListener(v -> createModel = ss);
            styleRow.addView(chip);
            Ui.margin(chip, styleRow.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        card.addView(styleRow);

        LinearLayout imgRow = Ui.row(this);
        imgRow.addView(Ui.text(this, "Image model", 10.5f, Ui.MUTED, true));
        String[] images = {"canvas-fast", "canvas-pro", "ink-1"};
        for (String im : images) {
            final String iim = im;
            TextView chip = Ui.chip(this, ModelCatalog.byImage(im).name);
            Ui.styleChip(chip, imageModel.equals(iim), Ui.CYAN);
            chip.setOnClickListener(v -> imageModel = iim);
            imgRow.addView(chip);
            Ui.margin(chip, imgRow.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        card.addView(imgRow);

        LinearLayout vidRow = Ui.row(this);
        vidRow.addView(Ui.text(this, "Video model", 10.5f, Ui.MUTED, true));
        for (ModelCatalog.Model m : ModelCatalog.videosFor(modes[createMode].name)) {
            final ModelCatalog.Model mm = m;
            TextView chip = Ui.chip(this, mm.name + " · " + mm.videoCredits + "cr");
            Ui.styleChip(chip, videoModel.equals(mm.id), Ui.CYAN);
            chip.setOnClickListener(v -> videoModel = mm.id);
            vidRow.addView(chip);
            Ui.margin(chip, vidRow.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        card.addView(vidRow);

        card.addView(Ui.spacer(this, 8));
        card.addView(Ui.text(this, "Est. resolution: " + ModelCatalog.resolution(modes[createMode].name, createModel, videoModel),
                10f, Ui.MUTED, false));
        return card;
    }

    private LinearLayout buildProgressCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Render progress", 13f, Ui.TEXT, true));
        card.addView(Ui.spacer(this, 8));
        stageList = Ui.column(this);
        card.addView(stageList);
        card.addView(Ui.spacer(this, 8));
        progressBar = new ProgressBar(this);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        card.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 6)));
        LinearLayout pctRow = Ui.row(this);
        progressPct = Ui.text(this, "0%", 12f, Ui.MUTED, true);
        progressStage = Ui.text(this, "Idle", 12f, Ui.MUTED, false);
        pctRow.addView(progressPct);
        pctRow.addView(progressStage, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(pctRow);
        return card;
    }

    private int estimateCredits() {
        return ModelCatalog.estimate(modes[createMode].name, createModel, createDuration,
                imageModel, videoModel);
    }

    private void applyModeDefaults() {
        switch (modes[createMode].name) {
            case "Music Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-1";
                createDuration = 45; createRatio = "16:9"; break;
            case "Dance Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-dance";
                createDuration = 30; createRatio = "9:16"; break;
            case "Lyric Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-1";
                createDuration = 30; createRatio = "1:1"; break;
            case "OnBeat Effects":
                createModel = "Fast"; imageModel = "canvas-fast"; videoModel = "motion-lite";
                createDuration = 15; createRatio = "9:16"; break;
            case "Photo Karaoke":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-stage";
                createDuration = 30; createRatio = "9:16"; break;
            case "Music Cover Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-stage";
                createDuration = 20; createRatio = "1:1"; break;
            case "Video to Music":
                createModel = "Studio"; imageModel = "canvas-fast"; videoModel = "motion-lite";
                createDuration = 20; createRatio = "9:16"; break;
            case "Realtime":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "live-director";
                createDuration = 15; createRatio = "9:16"; break;
            case "AI Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-ai";
                createDuration = 30; createRatio = "16:9"; break;
            case "Image-to-Video":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-i2v";
                createDuration = 20; createRatio = "9:16"; break;
            case "AI Persona":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "persona-1";
                createDuration = 20; createRatio = "9:16"; break;
            case "AI Canvas":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-lite";
                createDuration = 10; createRatio = "1:1"; break;
            case "Auto Edit":
                createModel = "Fast"; imageModel = "canvas-fast"; videoModel = "motion-lite";
                createDuration = 30; createRatio = "9:16"; break;
            case "Cutout & Chroma":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-1";
                createDuration = 20; createRatio = "9:16"; break;
            case "Voice & Captions":
                createModel = "Studio"; imageModel = "canvas-fast"; videoModel = "motion-lite";
                createDuration = 30; createRatio = "16:9"; break;
            case "Motion FX":
                createModel = "Studio"; imageModel = "canvas-pro"; videoModel = "motion-1";
                createDuration = 20; createRatio = "16:9"; break;
        }
    }

    private void surprisingBrief() {
        String[] concepts = {
            "A midnight rooftop performance under a single sodium light with slow anamorphic push-ins.",
            "Graphic beat hits with hard colour blocks and type landing on every kick.",
            "A quiet window-lit portrait with gentle head movement and subtle film grain.",
            "A concrete stairwell turning into a stage for sharp footwork and hard shadows.",
            "Hand-painted typography blooming through a soft greenhouse at golden hour.",
            "A digital human presenting in a clean virtual studio with soft rim light."
        };
        createPrompt = concepts[(int) (Math.random() * concepts.length)];
        if (promptInput != null) promptInput.setText(createPrompt);
        aesthetic = "Editorial neon";
    }

    private void fillBriefWithBot() {
        createPrompt = DirectorBot.enhance(createPrompt.isEmpty() ? musicTitle : createPrompt,
                modes[createMode].name, aesthetic);
        concept = DirectorBot.brief(modes[createMode].name, musicTitle);
        if (promptInput != null) promptInput.setText(createPrompt);
        showTab(2);
    }

    private void importMusic() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQ_MUSIC);
    }

    private void pickReference() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_REFERENCE);
    }

    private void analyzeMusic() {
        Toast.makeText(this, "Beat analysis is local in this demo.", Toast.LENGTH_SHORT).show();
    }

    private void showMusicPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Music source", Ui.CYAN));
        panel.addView(Ui.text(this, "Pick a track for your project", 13f, Ui.MUTED, false));
        panel.addView(Ui.spacer(this, 14));

        String[] sources = {"Demo library", "Upload (MP3/WAV/M4A)", "Link import"};
        for (String s : sources) {
            final String ss = s;
            TextView row = Ui.pill(this, s, musicSource.equals(s) ? Ui.SURFACE_2 : Ui.SURFACE_2,
                    musicSource.equals(s) ? Ui.CYAN : Ui.TEXT);
            row.setOnClickListener(v -> {
                musicSource = ss;
                if ("Upload (MP3/WAV/M4A)".equals(ss)) importMusic();
                if ("Link import".equals(ss)) {
                    dialog.dismiss();
                    showLinkImport();
                }
                showTab(2);
            });
            panel.addView(row);
            Ui.margin(row, 0, 0, 0, 8);
        }

        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.9, 0.7);
    }

    private void showLinkImport() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Link import", Ui.CYAN));
        panel.addView(Ui.text(this, "Paste a supported track link", 13f, Ui.MUTED, false));
        panel.addView(Ui.spacer(this, 12));
        EditText input = new EditText(this);
        input.setHint("https://…");
        input.setTextSize(13f);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        input.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(input);
        panel.addView(Ui.spacer(this, 14));
        TextView save = Ui.button(this, "Save import", Ui.CYAN, Ui.INK);
        save.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                musicUrl = url;
                musicSource = "Link import";
                musicTitle = "Imported track";
                musicBpm = 120;
                musicKey = "C major";
                musicMood = "Imported";
                Toast.makeText(this, "Import queued (demo)", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            showTab(2);
        });
        panel.addView(save);
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.9, 0.65);
    }

    private void startGeneration() {
        if (createPrompt == null || createPrompt.trim().isEmpty()) {
            Toast.makeText(this, "Add a brief before generating.", Toast.LENGTH_SHORT).show();
            return;
        }
        String id = "task_" + System.currentTimeMillis();
        int credits = estimateCredits();
        String stageName = "Analyzing music";
        List<String> stages = TaskStore.stagesFor(modes[createMode].name);
        Task t = new Task(id, "Untitled " + modes[createMode].name, modes[createMode].name,
                createPrompt, createRatio, createModel, createDuration, credits,
                "Queued", 0, stageName, System.currentTimeMillis(), stages);
        t.musicTitle = musicTitle;
        t.musicBpm = musicBpm;
        t.musicKey = musicKey;
        t.musicMood = musicMood;
        t.musicSource = musicSource;
        t.musicUrl = musicUrl;
        t.imageModel = imageModel;
        t.videoModel = videoModel;
        t.concept = concept;
        t.aesthetic = aesthetic;
        t.world = world;
        t.characters = characters;
        t.variant = variant;
        t.camera = camera;
        t.lyric = lyricText;
        t.effect = effectFamily;
        t.scenePreset = scenePreset;
        t.choreo = choreo;
        t.motion = motionPreset;
        t.startImage = startImage;
        t.sourceClip = sourceClip;
        t.characterA = characterA;
        t.characterB = characterB;
        t.refCount = referenceUris.size();
        t.cloud = cloudRender;
        t.ttsEnabled = ttsEnabled;
        t.ttsVoice = ttsVoice;
        t.personaEnabled = personaEnabled;
        t.personaLook = personaLook;
        t.cutoutEnabled = cutoutEnabled;
        t.chromaEnabled = chromaEnabled;
        t.stabilizeEnabled = stabilizeEnabled;
        t.captionsEnabled = captionsEnabled;
        t.beatSyncEnabled = beatSyncEnabled;
        t.scenes = Pipeline.scenesFor(t);
        store.add(t);
        saveDraft();
        progressCard.setVisibility(View.VISIBLE);
        generateBtn.setEnabled(false);
        activeRenderId = id;
        restoreActiveProgress();
        if (cloudRender) {
            startCloudRender(t);
        } else {
            startLocalRender(t);
        }
        showTab(3);
    }

    private void startLocalRender(Task t) {
        final String[] stageNames = t.stages.toArray(new String[0]);
        final int[] idx = {0};
        renderRunnable = new Runnable() {
            @Override public void run() {
                if (idx[0] >= stageNames.length) {
                    t.status = "Done";
                    t.progress = 100;
                    t.stage = "Final cut";
                    store.update(t);
                    updateProgressUI(t);
                    generateBtn.setEnabled(true);
                    activeRenderId = null;
                    renderRunnable = null;
                    return;
                }
                t.status = "Rendering";
                t.stage = stageNames[idx[0]];
                t.progress = Math.min(100, (idx[0] + 1) * 100 / Math.max(1, stageNames.length));
                store.update(t);
                updateProgressUI(t);
                idx[0]++;
                handler.postDelayed(this, 700);
            }
        };
        handler.post(renderRunnable);
    }

    private void startCloudRender(Task t) {
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("mode", t.mode);
                body.put("prompt", t.prompt);
                body.put("ratio", t.ratio);
                body.put("duration", t.duration);
                JSONObject project = cloud.createProject(body);
                String projectId = project.optString("id", "");
                JSONObject job = cloud.createRender(projectId);
                String jobId = job.optString("id", "");
                handler.post(() -> {
                    t.cloudJobId = jobId;
                    t.status = "Queued";
                    t.stage = "Cloud render queued";
                    t.progress = 5;
                    store.update(t);
                    updateProgressUI(t);
                });
                cloudPollJobId = jobId;
                pollCloudRender();
            } catch (Exception e) {
                handler.post(() -> {
                    t.status = "Failed";
                    t.stage = "Cloud error";
                    t.failure = CloudApi.errorMessage(e);
                    store.update(t);
                    updateProgressUI(t);
                    generateBtn.setEnabled(true);
                    activeRenderId = null;
                    Toast.makeText(MainActivity.this, CloudApi.errorMessage(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void pollCloudRender() {
        if (cloudPollJobId == null || cloudBusy) return;
        cloudBusy = true;
        io.execute(() -> {
            try {
                Thread.sleep(2500);
                JSONObject job = cloud.renderJob(cloudPollJobId);
                String status = job.optString("status", "");
                int progress = job.optInt("progress", 0);
                String stage = job.optString("stage", "");
                String outputUrl = job.optString("outputUrl", "");
                handler.post(() -> {
                    Task t = store.find(activeRenderId);
                    if (t == null) {
                        cloudBusy = false;
                        return;
                    }
                    if ("Done".equals(status) || "completed".equals(status)) {
                        t.status = "Done";
                        t.progress = 100;
                        t.stage = "Final cut";
                        t.outputUrl = outputUrl;
                        store.update(t);
                        updateProgressUI(t);
                        generateBtn.setEnabled(true);
                        activeRenderId = null;
                        cloudPollJobId = null;
                        cloudBusy = false;
                        Toast.makeText(MainActivity.this, "Cloud render complete", Toast.LENGTH_SHORT).show();
                    } else if ("Failed".equals(status) || "failed".equals(status)) {
                        t.status = "Failed";
                        t.stage = "Cloud failed";
                        t.failure = job.optString("error", "Cloud render failed.");
                        store.update(t);
                        updateProgressUI(t);
                        generateBtn.setEnabled(true);
                        activeRenderId = null;
                        cloudPollJobId = null;
                        cloudBusy = false;
                    } else {
                        t.status = "Rendering";
                        t.progress = progress;
                        t.stage = stage.isEmpty() ? "Cloud render" : stage;
                        store.update(t);
                        updateProgressUI(t);
                        handler.postDelayed(() -> {
                            cloudBusy = false;
                            pollCloudRender();
                        }, 2500);
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    cloudBusy = false;
                    handler.postDelayed(() -> pollCloudRender(), 4000);
                });
            }
        });
    }

    private void updateProgressUI(Task t) {
        if (stageList == null) return;
        stageList.removeAllViews();
        for (int i = 0; i < t.stages.size(); i++) {
            String s = t.stages.get(i);
            boolean active = s.equals(t.stage);
            boolean done = t.isDone() || i < Math.max(0, t.progress / 14);
            TextView row = Ui.text(this, s, 12f, done ? Ui.EMERALD : (active ? Ui.CYAN : Ui.MUTED), true);
            Ui.pad(row, 8, 6, 8, 6);
            row.setBackground(active ? Ui.shape(Ui.SURFACE_2, 100) : null);
            stageList.addView(row);
            Ui.margin(row, 0, 0, 0, 4);
        }
        if (progressBar != null) progressBar.setProgress(t.progress);
        if (progressPct != null) progressPct.setText(t.progress + "%");
        if (progressStage != null) progressStage.setText(t.stage);
    }

    private void restoreActiveProgress() {
        Task t = store.firstRendering();
        if (t != null) {
            progressCard.setVisibility(View.VISIBLE);
            generateBtn.setEnabled(false);
            activeRenderId = t.id;
            updateProgressUI(t);
            if (t.cloud) {
                cloudPollJobId = t.cloudJobId;
                pollCloudRender();
            } else {
                startLocalRender(t);
            }
        }
    }

    private void resumeCloudRender() {
        Task latest = store.latest();
        if (latest != null && latest.cloud && latest.cloudJobId != null && !latest.cloudJobId.isEmpty()
                && !latest.isDone() && !latest.isFailed() && !latest.isCancelled()) {
            activeRenderId = latest.id;
            cloudPollJobId = latest.cloudJobId;
            progressCard.setVisibility(View.VISIBLE);
            generateBtn.setEnabled(false);
            pollCloudRender();
        }
    }

    private void saveDraft() {
        try {
            Task t = new Task("draft", "Draft", modes[createMode].name, createPrompt, createRatio,
                    createModel, createDuration, estimateCredits(), "Queued", 0, "Draft",
                    System.currentTimeMillis(), TaskStore.stagesFor(modes[createMode].name));
            t.imageModel = imageModel; t.videoModel = videoModel; t.musicSource = musicSource;
            t.musicTitle = musicTitle; t.musicUrl = musicUrl; t.musicBpm = musicBpm;
            t.musicKey = musicKey; t.musicMood = musicMood; t.concept = concept;
            t.aesthetic = aesthetic; t.world = world; t.characters = characters;
            t.variant = variant; t.camera = camera; t.lyric = lyricText; t.effect = effectFamily;
            t.scenePreset = scenePreset; t.choreo = choreo; t.motion = motionPreset;
            t.startImage = startImage; t.sourceClip = sourceClip; t.characterA = characterA;
            t.characterB = characterB; t.refCount = referenceUris.size(); t.cloud = cloudRender;
            t.ttsEnabled = ttsEnabled; t.ttsVoice = ttsVoice;
            t.personaEnabled = personaEnabled; t.personaLook = personaLook;
            t.cutoutEnabled = cutoutEnabled; t.chromaEnabled = chromaEnabled;
            t.stabilizeEnabled = stabilizeEnabled; t.captionsEnabled = captionsEnabled;
            t.beatSyncEnabled = beatSyncEnabled;
            JSONObject json = t.toJson();
            SharedPreferences.Editor editor = draftPrefs.edit();
            editor.putString(DRAFT_KEY, json.toString());
            editor.putString("mode", modes[createMode].name);
            editor.putString("ratio", createRatio);
            editor.putString("model", createModel);
            editor.putInt("duration", createDuration);
            editor.apply();
        } catch (Exception ignored) {
        }
    }

    private boolean restoreDraft() {
        try {
            String raw = draftPrefs.getString(DRAFT_KEY, null);
            if (raw == null) return false;
            JSONObject o = new JSONObject(raw);
            String mode = o.optString("mode", "Music Video");
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].name.equals(mode)) {
                    createMode = i;
                    break;
                }
            }
            createPrompt = o.optString("prompt", "");
            createRatio = o.optString("ratio", "9:16");
            createModel = o.optString("model", "Studio");
            createDuration = o.optInt("duration", 30);
            imageModel = o.optString("imageModel", "canvas-pro");
            videoModel = o.optString("videoModel", "motion-1");
            musicSource = o.optString("musicSource", "Demo library");
            musicTitle = o.optString("musicTitle", "Neon Skyline");
            musicUrl = o.optString("musicUrl", "");
            musicBpm = o.optInt("musicBpm", 0);
            musicKey = o.optString("musicKey", "");
            musicMood = o.optString("musicMood", "");
            concept = o.optString("concept", "");
            aesthetic = o.optString("aesthetic", "");
            world = o.optString("world", "");
            characters = o.optString("characters", "");
            variant = o.optString("variant", "");
            camera = o.optString("camera", "");
            lyricText = o.optString("lyric", "");
            effectFamily = o.optString("effect", "");
            scenePreset = o.optString("scenePreset", "");
            choreo = o.optString("choreo", "");
            motionPreset = o.optString("motion", "");
            startImage = o.optString("startImage", "");
            sourceClip = o.optString("sourceClip", "");
            characterA = o.optString("characterA", "");
            characterB = o.optString("characterB", "");
            cloudRender = o.optBoolean("cloud", false);
            ttsEnabled = o.optBoolean("ttsEnabled", false);
            ttsVoice = o.optString("ttsVoice", "neutral");
            personaEnabled = o.optBoolean("personaEnabled", false);
            personaLook = o.optString("personaLook", "default");
            cutoutEnabled = o.optBoolean("cutoutEnabled", false);
            chromaEnabled = o.optBoolean("chromaEnabled", false);
            stabilizeEnabled = o.optBoolean("stabilizeEnabled", false);
            captionsEnabled = o.optBoolean("captionsEnabled", false);
            beatSyncEnabled = o.optBoolean("beatSyncEnabled", false);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void resetDraft() {
        createPrompt = "";
        concept = ""; aesthetic = "Editorial neon"; world = "Rooftops, wet streets, a tunnel of moving light";
        characters = "Lead performer · silver jacket"; variant = "Performance"; camera = "Orbit";
        lookStyle = "Abstract"; lyricText = ""; effectFamily = "Flash"; effectTemplate = "Flash grid";
        scenePreset = "Studio"; choreo = "Solo"; motionPreset = "Parallax loop";
        startImage = ""; sourceClip = ""; characterA = ""; characterB = "";
        referenceUris.clear(); musicUrl = ""; cloudRender = false;
        ttsEnabled = false; ttsVoice = "neutral"; personaEnabled = false; personaLook = "default";
        cutoutEnabled = false; chromaEnabled = false; stabilizeEnabled = false;
        captionsEnabled = false; beatSyncEnabled = false;
        applyModeDefaults();
        SharedPreferences.Editor editor = draftPrefs.edit();
        editor.remove(DRAFT_KEY).apply();
    }

    private void showCredits() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Credits", Ui.CYAN));
        panel.addView(Ui.text(this, "500 CR", 24f, Ui.TEXT, true));
        panel.addView(Ui.spacer(this, 8));
        panel.addView(Ui.text(this, "Demo credits. Reserved when a render starts; shown in the task ledger.",
                11f, Ui.MUTED, false));
        panel.addView(Ui.spacer(this, 16));
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.85, 0.5);
    }

    private void showAssistant() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Director Bot", Ui.CYAN));
        panel.addView(Ui.text(this, DirectorBot.greeting(), 12f, Ui.MUTED, false));
        panel.addView(Ui.spacer(this, 10));
        EditText input = new EditText(this);
        input.setHint("Ask about concepts, shots, models or credits");
        input.setTextSize(13f);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        input.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(input);
        panel.addView(Ui.spacer(this, 10));
        LinearLayout chips = Ui.row(this);
        String[] sugg = DirectorBot.suggestions(modes[createMode].name).toArray(new String[0]);
        for (String s : sugg) {
            final String ss = s;
            TextView chip = Ui.chip(this, s);
            chip.setOnClickListener(v -> {
                input.setText(ss);
                String reply = DirectorBot.reply(ss, modes[createMode].name, musicTitle);
                lastBotText = reply;
                Toast.makeText(MainActivity.this, reply, Toast.LENGTH_LONG).show();
            });
            chips.addView(chip);
            Ui.margin(chip, chips.getChildCount() == 1 ? 0 : 6, 0, 0, 0);
        }
        panel.addView(chips);
        panel.addView(Ui.spacer(this, 10));
        TextView ask = Ui.button(this, "Ask", Ui.CYAN, Ui.INK);
        ask.setOnClickListener(v -> {
            String reply = DirectorBot.reply(input.getText().toString(), modes[createMode].name, musicTitle);
            lastBotText = reply;
            Toast.makeText(MainActivity.this, reply, Toast.LENGTH_LONG).show();
        });
        panel.addView(ask);
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.92, 0.78);
    }

    private void showPreview(String url, String title, int colorA, int colorB) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        FrameLayout preview = new FrameLayout(this);
        ArtView art = new ArtView(this, colorA, colorB, true);
        preview.addView(art, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 200)));
        panel.addView(preview);
        panel.addView(Ui.text(this, title == null ? "Preview" : title, 18f, Ui.TEXT, true));
        panel.addView(Ui.spacer(this, 8));
        TextView play = Ui.button(this, "Play preview", Ui.SURFACE_2, Ui.TEXT);
        play.setOnClickListener(v -> {
            ArtView av = null;
            for (int i = 0; i < preview.getChildCount(); i++) {
                if (preview.getChildAt(i) instanceof ArtView) av = (ArtView) preview.getChildAt(i);
            }
            if (av != null) av.setPlaying(!av.isPlaying());
        });
        panel.addView(play);
        panel.addView(Ui.spacer(this, 10));
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.9, 0.7);
    }

    private void buildTasks() {
        LinearLayout col = addPage(16, 30);

        LinearLayout top = Ui.row(this);
        LinearLayout heading = Ui.column(this);
        heading.addView(Ui.caps(this, "Task center", Ui.CYAN));
        heading.addView(Ui.text(this, "Tasks", 30f, Ui.TEXT, true));
        top.addView(heading, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView reset = Ui.outlineButton(this, "Reset demo", Ui.MUTED);
        reset.setOnClickListener(v -> {
            store.reset();
            showTab(3);
        });
        top.addView(reset);
        col.addView(top);

        col.addView(Ui.spacer(this, 12));

        LinearLayout stats = Ui.row(this);
        LinearLayout s1 = statChip("All", String.valueOf(store.all().size()), Ui.TEXT);
        LinearLayout s2 = statChip("In progress", String.valueOf(store.activeCount()), Ui.CYAN);
        LinearLayout s3 = statChip("Done", String.valueOf(store.completedCount()), Ui.EMERALD);
        LinearLayout s4 = statChip("Failed", String.valueOf(store.failedCount()), Ui.ROSE);
        stats.addView(s1, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(s2, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(s3, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(s4, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Ui.margin(s1, 0, 0, 6, 0);
        Ui.margin(s2, 0, 0, 6, 0);
        Ui.margin(s3, 0, 0, 6, 0);
        col.addView(stats);

        col.addView(Ui.spacer(this, 16));

        HorizontalScrollView filters = new HorizontalScrollView(this);
        filters.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = Ui.row(this);
        String[] options = {"All", "In progress", "Completed", "Failed"};
        for (String option : options) {
            TextView chip = Ui.chip(this, option);
            Ui.styleChip(chip, option.equals(taskFilter), Ui.CYAN);
            chip.setOnClickListener(v -> {
                taskFilter = option;
                showTab(3);
            });
            filterRow.addView(chip);
            Ui.margin(chip, filterRow.getChildCount() == 1 ? 0 : 8, 0, 0, 0);
        }
        filters.addView(filterRow);
        col.addView(filters);

        col.addView(Ui.spacer(this, 16));

        List<Task> tasks = store.filtered(taskFilter);
        if (tasks.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.setGravity(Gravity.CENTER);
            empty.addView(Ui.icon(this, R.drawable.ic_tasks, Ui.MUTED, 32));
            TextView text = Ui.text(this, "No " + taskFilter.toLowerCase() + " tasks yet.", 13f, Ui.MUTED, false);
            text.setGravity(Gravity.CENTER);
            Ui.margin(text, 0, 10, 0, 0);
            empty.addView(text);
            col.addView(empty);
        } else {
            for (Task t : tasks) {
                View row = taskRow(t, true);
                col.addView(row);
                Ui.margin(row, 0, 0, 0, 10);
            }
        }
        col.addView(Ui.spacer(this, 20));
    }

    private LinearLayout statChip(String label, String value, int color) {
        LinearLayout chip = Ui.card(this);
        chip.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        chip.setBackground(Ui.shape(Ui.SURFACE_2, 16));
        LinearLayout body = Ui.column(this);
        body.addView(Ui.text(this, value, 18f, color, true));
        body.addView(Ui.text(this, label, 10f, Ui.MUTED, false));
        chip.addView(body);
        return chip;
    }

    private View taskRow(Task t, boolean detail) {
        LinearLayout row = Ui.card(this);
        row.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout inner = Ui.row(this);
        FrameLayout iconWrap = new FrameLayout(this);
        iconWrap.setBackground(Ui.gradient(colorForMode(t.mode), Ui.SURFACE_2, 14));
        ImageView icon = Ui.icon(this, iconForMode(t.mode), Color.WHITE, 22);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(
                Ui.dp(this, 32), Ui.dp(this, 32), Gravity.CENTER));
        inner.addView(iconWrap, Ui.lparams(Ui.dp(this, 44), Ui.dp(this, 44)));
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 10), 0, 0, 0);
        body.addView(Ui.text(this, t.title, 14f, Ui.TEXT, true));
        body.addView(Ui.text(this, t.meta() + "  ·  " + t.statusLabel() + "  ·  " + t.timeAgo(),
                10f, Ui.MUTED, false));
        inner.addView(body, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = Ui.pill(this, t.statusLabel(), 0xFF1E2130, Ui.statusColor(t.status));
        inner.addView(status);
        row.addView(inner);
        row.setOnClickListener(v -> {
            if (detail) showTaskDetail(t);
        });
        if (detail && t.isFailed()) {
            TextView retry = Ui.pill(this, "RETRY", Ui.SURFACE_2, Ui.CYAN);
            retry.setOnClickListener(v -> {
                t.status = "Queued";
                t.progress = 0;
                t.stage = "Analyzing music";
                t.failure = "";
                store.update(t);
                showTab(3);
            });
            inner.addView(retry);
            Ui.margin(retry, 10, 0, 0, 0);
        }
        return row;
    }

    private void showTaskDetail(Task t) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);

        FrameLayout preview = new FrameLayout(this);
        ArtView art = new ArtView(this, colorForMode(t.mode), Ui.SURFACE_2, t.isDone());
        preview.addView(art, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        panel.addView(preview);

        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        body.addView(Ui.caps(this, t.mode, colorForMode(t.mode)));
        body.addView(Ui.text(this, t.title, 20f, Ui.TEXT, true));
        body.addView(Ui.text(this, t.meta() + "  ·  " + t.resolution(), 11f, Ui.MUTED, false));
        body.addView(Ui.spacer(this, 8));
        body.addView(Ui.text(this, "Status: " + t.statusLabel() + "  ·  " + t.progress + "%", 11f,
                t.isDone() ? Ui.EMERALD : (t.isFailed() ? Ui.ROSE : Ui.AMBER), true));
        if (!t.failure.isEmpty()) {
            body.addView(Ui.spacer(this, 6));
            body.addView(Ui.text(this, t.failure, 10.5f, Ui.ROSE, false));
        }
        body.addView(Ui.spacer(this, 10));

        body.addView(Ui.caps(this, "Storyboard", Ui.MUTED));
        body.addView(Ui.spacer(this, 6));
        for (Scene s : t.scenes) {
            LinearLayout sc = Ui.row(this);
            TextView num = Ui.text(this, String.valueOf(t.scenes.indexOf(s) + 1), 10f, Ui.MUTED, true);
            num.setWidth(Ui.dp(this, 16));
            sc.addView(num);
            LinearLayout info = Ui.column(this);
            info.setPadding(Ui.dp(this, 6), 0, 0, 0);
            info.addView(Ui.text(this, s.title + " · " + s.timeRange(), 11f, Ui.TEXT, true));
            info.addView(Ui.text(this, s.camera + " · " + s.location, 9.5f, Ui.MUTED, false));
            sc.addView(info, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView check = Ui.pill(this, s.approved ? "OK" : "…", s.approved ? Ui.SURFACE_2 : Ui.SURFACE,
                    s.approved ? Ui.EMERALD : Ui.MUTED);
            check.setOnClickListener(v -> {
                s.approved = !s.approved;
                store.update(t);
                showTaskDetail(t);
                dialog.dismiss();
            });
            sc.addView(check);
            body.addView(sc);
            Ui.margin(sc, 0, 0, 0, 6);
        }

        body.addView(Ui.spacer(this, 10));
        LinearLayout actions = Ui.row(this);
        if (t.isActive()) {
            TextView cancel = Ui.pill(this, "CANCEL", Ui.SURFACE_2, Ui.ROSE);
            cancel.setOnClickListener(v -> {
                t.status = "Cancelled";
                t.stage = "Cancelled";
                store.update(t);
                dialog.dismiss();
                showTab(3);
            });
            actions.addView(cancel, Ui.lparams(0, Ui.dp(this, 44), 1f));
            Ui.margin(cancel, 0, 0, 8, 0);
            TextView retry = Ui.pill(this, "RETRY", Ui.SURFACE_2, Ui.CYAN);
            retry.setOnClickListener(v -> {
                t.status = "Queued";
                t.progress = 0;
                t.stage = "Analyzing music";
                t.failure = "";
                store.update(t);
                dialog.dismiss();
                showTab(3);
            });
            actions.addView(retry, Ui.lparams(0, Ui.dp(this, 44), 1.2f));
        } else if (t.isDone()) {
            TextView play = Ui.pill(this, "PLAY", Ui.SURFACE_2, Ui.CYAN);
            play.setOnClickListener(v -> showPreview(t.outputUrl, t.title, colorForMode(t.mode), Ui.SURFACE_2));
            actions.addView(play, Ui.lparams(0, Ui.dp(this, 44), 1f));
            Ui.margin(play, 0, 0, 8, 0);
            TextView dup = Ui.pill(this, "DUPLICATE", Ui.SURFACE_2, Ui.MUTED);
            dup.setOnClickListener(v -> {
                String id = "task_" + System.currentTimeMillis();
                Task copy = new Task(id, t.title + " (copy)", t.mode, t.prompt, t.ratio, t.model,
                        t.duration, t.credits, "Done", 100, "Final cut", System.currentTimeMillis(),
                        new ArrayList<>(t.stages));
                copy.imageModel = t.imageModel; copy.videoModel = t.videoModel;
                copy.ttsEnabled = t.ttsEnabled; copy.ttsVoice = t.ttsVoice;
                copy.personaEnabled = t.personaEnabled; copy.personaLook = t.personaLook;
                copy.cutoutEnabled = t.cutoutEnabled; copy.chromaEnabled = t.chromaEnabled;
                copy.stabilizeEnabled = t.stabilizeEnabled; copy.captionsEnabled = t.captionsEnabled;
                copy.beatSyncEnabled = t.beatSyncEnabled;
                copy.scenes = new ArrayList<>(t.scenes);
                store.add(copy);
                dialog.dismiss();
                showTab(3);
            });
            actions.addView(dup, Ui.lparams(0, Ui.dp(this, 44), 1.2f));
        } else if (t.isFailed() || t.isCancelled()) {
            TextView retry = Ui.pill(this, "RETRY", Ui.SURFACE_2, Ui.CYAN);
            retry.setOnClickListener(v -> {
                t.status = "Queued";
                t.progress = 0;
                t.stage = "Analyzing music";
                t.failure = "";
                store.update(t);
                dialog.dismiss();
                showTab(3);
            });
            actions.addView(retry, Ui.lparams(0, Ui.dp(this, 44), 1.2f));
        }
        TextView del = Ui.pill(this, "DELETE", Ui.SURFACE_2, Ui.ROSE);
        del.setOnClickListener(v -> {
            store.delete(t.id);
            dialog.dismiss();
            showTab(3);
        });
        actions.addView(del, Ui.lparams(Ui.dp(this, 56), Ui.dp(this, 44)));
        body.addView(actions);

        panel.addView(body);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        dialog.setContentView(scroll);
        showDialog(dialog, 0.92, 0.9);
    }

    private void buildProfile() {
        LinearLayout col = addPage(16, 30);

        LinearLayout top = Ui.row(this);
        top.addView(Ui.iconTile(this, R.drawable.ic_profile, Ui.CYAN, Ui.SURFACE_2, 40));
        LinearLayout brand = Ui.column(this);
        brand.setPadding(Ui.dp(this, 10), 0, 0, 0);
        brand.addView(Ui.text(this, cloud.isSignedIn() ? cloud.getDisplayName() : "Local creator",
                15f, Ui.TEXT, true));
        brand.addView(Ui.text(this, cloud.isSignedIn() ? cloud.getUserEmail() : "Signed out · demo mode",
                10.5f, Ui.MUTED, false));
        top.addView(brand, Ui.lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!cloud.isSignedIn()) {
            TextView sign = Ui.pill(this, "SIGN IN", Ui.SURFACE_2, Ui.CYAN);
            sign.setOnClickListener(v -> showAuth());
            top.addView(sign);
        }
        col.addView(top);

        col.addView(Ui.spacer(this, 20));

        LinearLayout account = Ui.card(this);
        account.addView(Ui.caps(this, "Account", Ui.CYAN));
        account.addView(Ui.spacer(this, 8));
        TextView test = Ui.outlineButton(this, "Test cloud connection", Ui.TEXT);
        test.setOnClickListener(v -> testCloud());
        account.addView(test);
        TextView base = Ui.pill(this, cloud.getBaseUrl(), Ui.SURFACE_2, Ui.TEXT);
        base.setOnClickListener(v -> setBaseUrl());
        account.addView(base);
        if (cloud.isSignedIn()) {
            TextView me = Ui.outlineButton(this, "Profile", Ui.TEXT);
            me.setOnClickListener(v -> loadProfile());
            account.addView(me);
            Ui.margin(me, 8, 0, 0, 0);
            TextView logout = Ui.outlineButton(this, "Sign out", Ui.ROSE);
            logout.setOnClickListener(v -> {
                cloud.logout();
                showTab(4);
            });
            account.addView(logout);
            Ui.margin(logout, 8, 0, 0, 0);
        }
        col.addView(account);

        col.addView(Ui.spacer(this, 16));

        LinearLayout render = Ui.card(this);
        render.addView(Ui.caps(this, "Render target", Ui.CYAN));
        render.addView(Ui.spacer(this, 8));
        LinearLayout targetRow = Ui.row(this);
        TextView dev = Ui.pill(this, "Device", cloudRender ? Ui.SURFACE_2 : Ui.SURFACE,
                cloudRender ? Ui.MUTED : Ui.CYAN);
        dev.setOnClickListener(v -> { cloudRender = false; showTab(4); });
        targetRow.addView(dev);
        TextView cloudTarget = Ui.pill(this, "Chad cloud", cloudRender ? Ui.SURFACE : Ui.SURFACE_2,
                cloudRender ? Ui.CYAN : Ui.MUTED);
        cloudTarget.setOnClickListener(v -> { cloudRender = true; showTab(4); });
        targetRow.addView(cloudTarget);
        Ui.margin(cloudTarget, 8, 0, 0, 0);
        render.addView(targetRow);
        col.addView(render);

        col.addView(Ui.spacer(this, 16));

        LinearLayout data = Ui.card(this);
        data.addView(Ui.caps(this, "Data", Ui.CYAN));
        data.addView(Ui.spacer(this, 8));
        TextView export = Ui.outlineButton(this, "Export storyboard JSON", Ui.TEXT);
        export.setOnClickListener(v -> exportStoryboard());
        data.addView(export);
        TextView clear = Ui.outlineButton(this, "Clear drafts", Ui.ROSE);
        clear.setOnClickListener(v -> {
            resetDraft();
            Toast.makeText(this, "Draft cleared", Toast.LENGTH_SHORT).show();
        });
        data.addView(clear);
        Ui.margin(clear, 8, 0, 0, 0);
        col.addView(data);

        col.addView(Ui.spacer(this, 20));
        TextView footer = Ui.text(this, "Original studio · original icons · independently designed",
                10.5f, 0xFF6F6F8A, false);
        footer.setGravity(Gravity.CENTER);
        col.addView(footer);
    }

    private void testCloud() {
        io.execute(() -> {
            try {
                cloud.models();
                handler.post(() -> Toast.makeText(this, "Cloud reachable", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Cloud: " + CloudApi.errorMessage(e), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadProfile() {
        io.execute(() -> {
            try {
                cloud.me();
                handler.post(() -> showTab(4));
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, CloudApi.errorMessage(e), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setBaseUrl() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Cloud URL", Ui.CYAN));
        EditText input = new EditText(this);
        input.setText(cloud.getBaseUrl());
        input.setTextSize(13f);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        input.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(input);
        panel.addView(Ui.spacer(this, 14));
        TextView save = Ui.button(this, "Save", Ui.CYAN, Ui.INK);
        save.setOnClickListener(v -> {
            cloud.setBaseUrl(input.getText().toString());
            dialog.dismiss();
            showTab(4);
        });
        panel.addView(save);
        TextView close = Ui.outlineButton(this, "Close", Ui.TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.88, 0.55);
    }

    private void showAuth() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.SURFACE);
        panel.addView(Ui.caps(this, "Account", Ui.CYAN));
        EditText email = new EditText(this);
        email.setHint("Email");
        email.setTextSize(13f);
        email.setTextColor(Ui.TEXT);
        email.setHintTextColor(Ui.MUTED);
        email.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        email.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(email);
        panel.addView(Ui.spacer(this, 8));
        EditText pass = new EditText(this);
        pass.setHint("Password");
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setTextSize(13f);
        pass.setTextColor(Ui.TEXT);
        pass.setHintTextColor(Ui.MUTED);
        pass.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        pass.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(pass);
        panel.addView(Ui.spacer(this, 8));
        EditText name = new EditText(this);
        name.setHint("Display name");
        name.setTextSize(13f);
        name.setTextColor(Ui.TEXT);
        name.setHintTextColor(Ui.MUTED);
        name.setBackground(Ui.shape(Ui.SURFACE_2, 14));
        name.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        panel.addView(name);
        panel.addView(Ui.spacer(this, 14));
        TextView reg = Ui.button(this, "Register", Ui.CYAN, Ui.INK);
        reg.setOnClickListener(v -> {
            io.execute(() -> {
                try {
                    cloud.register(email.getText().toString(), pass.getText().toString(), name.getText().toString());
                    handler.post(() -> {
                        dialog.dismiss();
                        showTab(4);
                    });
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(this, CloudApi.errorMessage(e), Toast.LENGTH_LONG).show());
                }
            });
        });
        panel.addView(reg);
        TextView login = Ui.outlineButton(this, "Login", Ui.TEXT);
        login.setOnClickListener(v -> {
            io.execute(() -> {
                try {
                    cloud.login(email.getText().toString(), pass.getText().toString());
                    handler.post(() -> {
                        dialog.dismiss();
                        showTab(4);
                    });
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(this, CloudApi.errorMessage(e), Toast.LENGTH_LONG).show());
                }
            });
        });
        Ui.margin(login, 8, 0, 0, 0);
        panel.addView(login);
        TextView close = Ui.outlineButton(this, "Close", Ui.MUTED);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close);
        dialog.setContentView(panel);
        showDialog(dialog, 0.9, 0.8);
    }

    private void exportStoryboard() {
        Task latest = store.latest();
        if (latest == null) {
            Toast.makeText(this, "No task to export.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject json = latest.toJson();
            String filename = "chadvidcraft_storyboard_" + latest.id + ".json";
            File output = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
            OutputStreamWriter writer = new OutputStreamWriter(new java.io.FileOutputStream(output));
            writer.write(json.toString(2));
            writer.close();
            Uri uri = Uri.fromFile(output);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/json");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            Toast.makeText(this, "Exported storyboard JSON", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String shortName(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private void showDialog(Dialog dialog, double width, double height) {
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * width);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * height);
            w.setAttributes(lp);
        }
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        ContentResolver resolver = getContentResolver();
        String displayName = null;
        try (Cursor c = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) displayName = c.getString(0);
        } catch (Exception ignored) {}
        if (requestCode == REQ_MUSIC) {
            musicUrl = uri.toString();
            musicSource = "Local upload";
            musicTitle = displayName == null ? "Imported track" : displayName;
            musicBpm = 120;
            musicKey = "C major";
            musicMood = "Imported";
            Toast.makeText(this, "Track linked: " + musicTitle, Toast.LENGTH_SHORT).show();
            showTab(2);
        } else if (requestCode == REQ_REFERENCE) {
            referenceUris.add(uri.toString());
            showTab(2);
        } else if (requestCode == REQ_CLIP) {
            sourceClip = uri.toString();
            showTab(2);
        } else if (requestCode == REQ_START_IMAGE) {
            startImage = uri.toString();
            showTab(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDraft();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveDraft();
    }
}
