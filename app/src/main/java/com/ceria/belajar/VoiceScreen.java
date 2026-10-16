package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Parent-facing settings for the reading voice. Children should hear Malay read
 * the Malay way, so if the phone has no Malay voice installed we say so and
 * offer a one-tap way to install it, instead of quietly switching to another
 * language.
 */
public class VoiceScreen extends BaseScreen {

    private TextView statusView;
    private LinearLayout voiceList;
    private LinearLayout installBox;

    public VoiceScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Reading Voice \uD83D\uDD0A", "Suara Bacaan \uD83D\uDD0A");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView note = Util.text(host, t(
                "Choose the voice that reads letters, numbers and words aloud.",
                "Pilih suara yang membaca huruf, nombor dan perkataan."), 15, 0xFF455A64);
        note.setPadding(dp(4), 0, dp(4), dp(12));
        column.addView(note, matchWrap());

        statusView = Util.text(host, "", 17, 0xFF4A148C);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        statusView.setPadding(dp(4), 0, dp(4), dp(8));
        column.addView(statusView, matchWrap());

        installBox = new LinearLayout(host);
        installBox.setOrientation(LinearLayout.VERTICAL);
        installBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        installBox.setBackground(Util.roundedStroke(0xFFFFF8E1, dp(20), 0xFFFFB300, dp(3)));
        installBox.setVisibility(View.GONE);

        TextView warn = Util.text(host, t(
                "This phone has no Bahasa Melayu voice yet. Install it so Malay words "
                        + "are read the Malay way (otherwise \"epal\" sounds like \"e-pal\").",
                "Telefon ini belum ada suara Bahasa Melayu. Pasang dulu supaya perkataan "
                        + "Melayu dibaca cara Melayu (jika tidak, \"epal\" jadi \"e-pal\")."),
                14, 0xFF5D4037);
        warn.setGravity(Gravity.START);
        installBox.addView(warn, matchWrap());

        TextView installButton = Util.text(host,
                t("\u2B07\uFE0F Install voices", "\u2B07\uFE0F Pasang suara"), 17, Color.WHITE);
        installButton.setTypeface(Typeface.DEFAULT_BOLD);
        installButton.setGravity(Gravity.CENTER);
        installButton.setPadding(dp(16), dp(14), dp(16), dp(14));
        installButton.setBackground(Util.rounded(0xFFFF6D00, dp(18)));
        installButton.setElevation(dp(3));
        installButton.setOnClickListener(v -> host.speech().openVoiceInstall());
        LinearLayout.LayoutParams ib = matchWrap();
        ib.setMargins(0, dp(12), 0, 0);
        installBox.addView(installButton, ib);
        column.addView(installBox, matchWrap());

        LinearLayout onlineBox = bigButton(
                host.speech().allowOnline() ? "\u2601\uFE0F" : "\uD83D\uDCF5",
                t("Online voices: ", "Suara dalam talian: ")
                        + (host.speech().allowOnline() ? t("On", "Buka") : t("Off", "Tutup")),
                host.speech().allowOnline() ? 0xFF29B6F6 : 0xFF78909C,
                this::toggleOnline);
        column.addView(onlineBox, matchWrap());

        TextView listTitle = Util.text(host, t("Available Malay voices:",
                "Suara Melayu yang ada:"), 15, 0xFF455A64);
        listTitle.setPadding(dp(4), dp(16), dp(4), dp(6));
        column.addView(listTitle, matchWrap());

        voiceList = new LinearLayout(host);
        voiceList.setOrientation(LinearLayout.VERTICAL);
        column.addView(voiceList, matchWrap());

        refresh();
        host.speech().setEngineListener((ready, malayVoiceFound) ->
                host.rootFrame().post(this::refresh));
        return column;
    }

    private void toggleOnline() {
        host.speech().setAllowOnline(!host.speech().allowOnline());
        host.showHome();
        host.open(new VoiceScreen(host));
    }

    private void refresh() {
        if (statusView == null || voiceList == null) {
            return;
        }
        boolean ready = host.speech().isTtsReady();
        List<String> voices = host.speech().malayVoiceNames();

        if (!ready) {
            statusView.setText(t("Voice engine is starting...",
                    "Enjin suara sedang dimulakan..."));
        } else if (voices.isEmpty()) {
            statusView.setText(t("No Bahasa Melayu voice found",
                    "Tiada suara Bahasa Melayu dijumpai"));
        } else {
            statusView.setText(t("Malay voice ready \u2714", "Suara Melayu sedia \u2714"));
        }
        statusView.setTextColor(voices.isEmpty() || !ready ? 0xFFD32F2F : 0xFF2E7D32);

        installBox.setVisibility(ready && voices.isEmpty() ? View.VISIBLE : View.GONE);

        voiceList.removeAllViews();
        String active = host.speech().activeVoiceName(Content.Lang.MS);
        for (String name : voices) {
            boolean selected = name.equals(active);
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(Util.roundedStroke(selected ? 0xFFC8E6C9 : Color.WHITE,
                    dp(16), selected ? 0xFF2E7D32 : 0xFFCFD8DC, dp(2)));
            LinearLayout.LayoutParams rlp = matchWrap();
            rlp.setMargins(0, dp(4), 0, dp(4));
            row.addView(Util.text(host, selected ? "\u2705 " : "\uD83D\uDD0A ", 18, 0xFF37474F));
            TextView label = Util.text(host, name, 14, 0xFF37474F);
            label.setGravity(Gravity.START);
            row.addView(label, weighted(0));
            row.setOnClickListener(v -> {
                host.speech().setVoiceByName(Content.Lang.MS, name);
                refresh();
                host.voice().speak(Content.Lang.MS, "epal");
            });
            voiceList.addView(row, rlp);
        }

        if (ready && voices.isEmpty()) {
            voiceList.addView(Util.text(host, t(
                    "Tip: also install the voice in Android Settings > Text-to-speech.",
                    "Petua: pasang juga di Tetapan Android > Teks-ke-ucapan."), 13, 0xFF78909C),
                    matchWrap());
        }
    }
}
