package com.ceria.belajar;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Parent studio for turning the app from synthesised speech into real human
 * speech. One grown-up records the clips once; a child then hears a natural
 * voice instead of the phone's robot. Recordings beat synthesis, so a
 * half-finished pack still helps and uncovered phrases simply stay synthetic.
 */
public class RecordingStudioScreen extends BaseScreen {

    private static final int REQUEST_MIC = 7301;
    public static final int REQUEST_ZIP = 7302;

    private LinearLayout groupRow;
    private LinearLayout listColumn;
    private TextView progressView;
    private TextView statusView;
    private Content.Lang studioLang = Content.Lang.MS;
    private String group = "letters";

    private MediaRecorder recorder;
    private String recordingKey;
    private File recordingFile;

    public RecordingStudioScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Recording Studio \uD83C\uDF99\uFE0F", "Studio Rakaman \uD83C\uDF99\uFE0F");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView intro = Util.text(host, t(
                "Record each word once in a quiet room. The app then plays your voice "
                        + "instead of the robot voice. Tap Record, wait for the beep, say the word, tap Stop.",
                "Rakam setiap perkataan sekali dalam bilik yang tenang. Aplikasi akan "
                        + "memainkan suara anda dan bukan suara robot. Tekan Rakam, tunggu bunyi, "
                        + "sebut perkataan, tekan Berhenti."), 13, 0xFF455A64);
        intro.setPadding(dp(4), 0, dp(4), dp(10));
        column.addView(intro, matchWrap());

        LinearLayout langRow = horizontal();
        langRow.setGravity(Gravity.START);
        for (Content.Lang option : Content.Lang.values()) {
            langRow.addView(chip(option == Content.Lang.MS ? "\uD83C\uDDF2\uD83C\uDDFE BM"
                            : "\uD83C\uDDEC\uD83C\uDDE7 EN",
                    studioLang == option, v -> {
                        studioLang = option;
                        render();
                    }));
        }
        column.addView(langRow, matchWrap());

        progressView = Util.text(host, "", 15, 0xFF4A148C);
        progressView.setTypeface(Typeface.DEFAULT_BOLD);
        progressView.setPadding(dp(4), dp(10), dp(4), dp(4));
        column.addView(progressView, matchWrap());

        statusView = Util.text(host, "", 13, 0xFF00695C);
        statusView.setPadding(dp(4), 0, dp(4), dp(6));
        column.addView(statusView, matchWrap());

        groupRow = horizontal();
        groupRow.setGravity(Gravity.START);
        column.addView(groupRow, matchWrap());

        LinearLayout actions = horizontal();
        actions.setGravity(Gravity.START);
        actions.addView(smallButton(t("\u2B07\uFE0F Export pack", "\u2B07\uFE0F Eksport pek"),
                0xFF5C6BC0, v -> exportPack()));
        actions.addView(smallButton(t("\u2B06\uFE0F Import pack", "\u2B06\uFE0F Import pek"),
                0xFF26A69A, v -> importPack()));
        column.addView(actions, matchWrap());

        listColumn = new LinearLayout(host);
        listColumn.setOrientation(LinearLayout.VERTICAL);
        column.addView(listColumn, matchWrap());

        render();
        return column;
    }

    private View chip(String label, boolean selected, View.OnClickListener onClick) {
        TextView chip = Util.text(host, label, 14, selected ? Color.WHITE : 0xFF37474F);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setBackground(Util.rounded(selected ? 0xFF7C4DFF : 0xFFECEFF1, dp(16)));
        chip.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = wrap();
        lp.setMargins(0, dp(4), dp(8), dp(4));
        chip.setLayoutParams(lp);
        return chip;
    }

    private View smallButton(String label, int color, View.OnClickListener onClick) {
        TextView button = Util.text(host, label, 14, Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setBackground(Util.rounded(color, dp(16)));
        button.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = wrap();
        lp.setMargins(0, dp(6), dp(8), dp(6));
        button.setLayoutParams(lp);
        return button;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void render() {
        if (listColumn == null) {
            return;
        }
        int[] coverage = host.voice().coverage(studioLang);
        progressView.setText(t("Recorded: ", "Sudah dirakam: ") + coverage[0] + " / " + coverage[1]);

        groupRow.removeAllViews();
        if (group == null || !groups().contains(group)) {
            group = "letters";
        }
        for (String name : groups()) {
            groupRow.addView(chip(groupLabel(name), name.equals(group), v -> {
                group = name;
                render();
            }));
        }

        listColumn.removeAllViews();
        for (PhraseCatalog.Phrase phrase : phrasesFor(studioLang)) {
            if (!phrase.group.equals(group)) {
                continue;
            }
            listColumn.addView(row(phrase));
        }
    }

    private List<String> groups() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (PhraseCatalog.Phrase phrase : phrasesFor(studioLang)) {
            out.add(phrase.group);
        }
        return new ArrayList<>(out);
    }

    private String groupLabel(String name) {
        switch (name) {
            case "letters":
                return t("Letters", "Huruf");
            case "numbers":
                return t("Numbers", "Nombor");
            case "words":
                return t("Words", "Perkataan");
            case "sentences":
                return t("Sentences", "Ayat");
            case "colours":
                return t("Colours", "Warna");
            case "body":
                return t("Body", "Badan");
            case "animals":
                return t("Animals", "Haiwan");
            case "plants":
                return t("Plants", "Tumbuhan");
            case "fruits":
                return t("Fruits", "Buah");
            case "living":
                return t("Living", "Hidup");
            case "jawi":
                return "Jawi";
            case "arabic":
                return "Arab";
            case "praise":
                return t("Praise", "Pujian");
            default:
                return name;
        }
    }

    private List<PhraseCatalog.Phrase> phrasesFor(Content.Lang lang) {
        List<PhraseCatalog.Phrase> all = new ArrayList<>(PhraseCatalog.forLang(lang));
        all.addAll(PhraseCatalog.praise(lang));
        return all;
    }

    private View row(PhraseCatalog.Phrase phrase) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));

        VoicePack.Source source = host.voice().sourceOf(phrase.lang, phrase.text);
        boolean human = source != VoicePack.Source.SYNTHESIZED;
        box.setBackground(Util.roundedStroke(human ? 0xFFE8F5E9 : Color.WHITE, dp(14),
                human ? 0xFF66BB6A : 0xFFCFD8DC, dp(2)));
        LinearLayout.LayoutParams boxLp = matchWrap();
        boxLp.setMargins(0, dp(3), 0, dp(3));

        TextView badge = Util.text(host,
                human ? "\uD83D\uDC64" : "\uD83E\uDD16", 18, 0xFF37474F);
        box.addView(badge);

        TextView label = Util.text(host, phrase.text, 15, 0xFF263238);
        label.setGravity(Gravity.START);
        label.setPadding(dp(8), 0, dp(8), 0);
        box.addView(label, weighted(0));

        box.addView(actionIcon("\u25B6", 0xFF29B6F6, v -> playPhrase(phrase)));
        if (human) {
            box.addView(actionIcon("\uD83D\uDDD1", 0xFFFF5252, v -> deleteClip(phrase)));
        }
        box.addView(actionIcon(recordingKey != null && recordingKey.equals(phrase.key())
                ? "\u23F9" : "\uD83C\uDF99", 0xFFFF4081, v -> toggleRecord(phrase)));
        return box;
    }

    private View actionIcon(String glyph, int color, View.OnClickListener onClick) {
        TextView button = Util.text(host, glyph, 16, Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackground(Util.rounded(color, dp(12)));
        button.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = wrap();
        lp.setMargins(dp(3), 0, 0, 0);
        button.setLayoutParams(lp);
        return button;
    }

    // ------------------------------------------------------------------
    // Recording
    // ------------------------------------------------------------------

    private void playPhrase(PhraseCatalog.Phrase phrase) {
        host.voice().speak(phrase.lang, phrase.text);
        statusView.setText(t("Playing: ", "Memainkan: ") + phrase.text);
    }

    private void deleteClip(PhraseCatalog.Phrase phrase) {
        File file = host.voice().fileFor(phrase.lang, phrase.text);
        if (file.exists() && file.delete()) {
            host.refreshVoicePack();
            statusView.setText(t("Deleted recording for ", "Rakaman dipadam untuk ") + phrase.text);
            render();
        }
    }

    private void toggleRecord(PhraseCatalog.Phrase phrase) {
        if (recordingKey != null) {
            stopRecording();
            return;
        }
        if (host.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            host.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            statusView.setText(t("Allow the microphone, then tap Record again.",
                    "Benarkan mikrofon, kemudian tekan Rakam semula."));
            return;
        }
        startRecording(phrase);
    }

    private void startRecording(PhraseCatalog.Phrase phrase) {
        File file = host.voice().fileFor(phrase.lang, phrase.text);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? new MediaRecorder(host)
                    : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recordingKey = phrase.key();
            recordingFile = file;
            statusView.setText(t("Recording... say it now: ", "Merakam... sebut sekarang: ")
                    + phrase.text);
            render();
        } catch (IOException | RuntimeException e) {
            releaseRecorder();
            recordingKey = null;
            statusView.setText(t("Could not start recording: ", "Tidak dapat mula merakam: ")
                    + e.getMessage());
        }
    }

    private void stopRecording() {
        String finishedKey = recordingKey;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                // stop() throws when nothing was captured; drop the empty file.
                if (recordingFile != null) {
                    recordingFile.delete();
                }
                releaseRecorder();
                recordingKey = null;
                statusView.setText(t("Nothing was captured, try again.",
                        "Tiada suara ditangkap, cuba lagi."));
                render();
                return;
            }
        }
        releaseRecorder();
        recordingKey = null;
        host.refreshVoicePack();
        if (finishedKey != null) {
            statusView.setText(t("Saved. Tap \u25B6 to hear it.",
                    "Disimpan. Tekan \u25B6 untuk dengar."));
        }
        render();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (RuntimeException ignored) {
            }
            recorder.release();
            recorder = null;
        }
    }

    /** Called by the host once the microphone permission is answered. */
    public void onPermissionResult(boolean granted) {
        if (!granted) {
            statusView.setText(t("Microphone permission is needed to record.",
                    "Kebenaran mikrofon diperlukan untuk merakam."));
        }
    }

    // ------------------------------------------------------------------
    // Pack export / import
    // ------------------------------------------------------------------

    /** Zips every recording so it can be sent in and bundled into the APK. */
    private void exportPack() {
        List<String> keys = new ArrayList<>();
        File dir = host.voice().userDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".m4a"));
        if (files != null) {
            for (File file : files) {
                keys.add(file.getName());
            }
        }
        if (keys.isEmpty()) {
            statusView.setText(t("Record something first.", "Rakam sesuatu dahulu."));
            return;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        String name = "belajar-ceria-voice-" + stamp + ".zip";
        try {
            File cache = new File(host.getCacheDir(), name);
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(cache))) {
                StringBuilder manifest = new StringBuilder();
                for (String key : keys) {
                    zip.putNextEntry(new java.util.zip.ZipEntry(key));
                    try (java.io.FileInputStream in =
                                 new java.io.FileInputStream(new File(dir, key))) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) > 0) {
                            zip.write(buffer, 0, read);
                        }
                    }
                    zip.closeEntry();
                    manifest.append(key.replace(".m4a", "")).append('\n');
                }
                zip.putNextEntry(new java.util.zip.ZipEntry("manifest_all.txt"));
                zip.write(manifest.toString().getBytes("UTF-8"));
                zip.closeEntry();
            }
            publishZip(cache, name);
            statusView.setText(t("Exported: ", "Dieksport: ") + name);
        } catch (IOException e) {
            statusView.setText(t("Export failed: ", "Eksport gagal: ") + e.getMessage());
        }
    }

    /** Saves the zip where the parent can find it, then offers to share it. */
    private void publishZip(File cache, String name) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/zip");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download");
            Uri target = host.getContentResolver().insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (target != null) {
                try (OutputStream out = host.getContentResolver().openOutputStream(target);
                     java.io.FileInputStream in = new java.io.FileInputStream(cache)) {
                    if (out != null) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
                statusView.setText(t("Saved to Downloads: ", "Disimpan ke Muat Turun: ") + name);
                return;
            }
        }
        File external = new File(host.getExternalFilesDir(null), name);
        try (java.io.FileInputStream in = new java.io.FileInputStream(cache);
             OutputStream out = new java.io.FileOutputStream(external)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        statusView.setText(t("Saved to ", "Disimpan ke ") + external.getAbsolutePath());
    }

    private void importPack() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        try {
            host.startActivityForResult(intent, REQUEST_ZIP);
        } catch (RuntimeException e) {
            statusView.setText(t("No file picker found.", "Tiada pemilih fail dijumpai."));
        }
    }

    /** Restores every clip inside a chosen zip back into the voice folder. */
    public void importZip(Uri uri) {
        File dir = host.voice().userDir();
        int restored = 0;
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                host.getContentResolver().openInputStream(uri))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = new File(entry.getName()).getName();
                if (!name.endsWith(".m4a")) {
                    continue;
                }
                try (OutputStream out = new java.io.FileOutputStream(new File(dir, name))) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zip.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                }
                restored++;
            }
        } catch (IOException | RuntimeException e) {
            statusView.setText(t("Import failed: ", "Import gagal: ") + e.getMessage());
            return;
        }
        host.refreshVoicePack();
        statusView.setText(t("Imported ", "Diimport ") + restored + t(" recordings.", " rakaman."));
        render();
    }

    /** The host forwards the file picker result here. */
    public boolean handlesRequest(int requestCode) {
        return requestCode == REQUEST_ZIP;
    }

    /** Stops any recording when the parent leaves the screen. */
    public void onLeave() {
        if (recorder != null) {
            stopRecording();
        }
        host.voice().stop();
    }
}
