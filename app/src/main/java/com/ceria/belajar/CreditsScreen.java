package com.ceria.belajar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows who recorded the human voice clips.
 *
 * The Commons recordings are CC BY-SA 4.0, which requires credit, so this screen
 * is not decoration: shipping the clips without it would breach the licence.
 */
public class CreditsScreen extends BaseScreen {

    public CreditsScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Voice credits", "Kredit suara");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView intro = Util.text(host, t(
                "The kind people who recorded the words you hear. Tap a name to see what they recorded.",
                "Orang yang merakam perkataan yang anda dengar. Tekan nama untuk lihat rakaman mereka."),
                13, 0xFF455A64);
        intro.setPadding(dp(4), 0, dp(4), dp(10));
        column.addView(intro, matchWrap());

        List<String[]> rows = readCredits();
        if (rows.isEmpty()) {
            column.addView(Util.text(host, t(
                    "All words are currently spoken by the phone's speech engine.",
                    "Semua perkataan kini disebut oleh enjin suara telefon."), 14, 0xFF616161),
                    matchWrap());
            return column;
        }

        Map<String, List<String>> byAuthor = new LinkedHashMap<>();
        Map<String, String> licences = new LinkedHashMap<>();
        for (String[] row : rows) {
            // key,lang,text,commons_file,license,author,source
            if (row.length < 6) {
                continue;
            }
            byAuthor.computeIfAbsent(row[5], k -> new ArrayList<>()).add(row[2]);
            licences.putIfAbsent(row[5], row[4]);
        }

        for (Map.Entry<String, List<String>> entry : byAuthor.entrySet()) {
            LinearLayout card = new LinearLayout(host);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(Util.roundedStroke(Color.WHITE, dp(14), 0xFFCFD8DC, dp(2)));

            TextView name = Util.text(host, "\uD83C\uDFA4 " + entry.getKey(), 16, 0xFF311B92);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(name, matchWrap());

            TextView licence = Util.text(host, licences.get(entry.getKey())
                    + t(" via Wikimedia Commons", " melalui Wikimedia Commons"), 12, 0xFF6D4C41);
            licence.setPadding(0, dp(2), 0, dp(6));
            card.addView(licence, matchWrap());

            TextView words = Util.text(host, join(entry.getValue()), 14, 0xFF37474F);
            card.addView(words, matchWrap());

            LinearLayout.LayoutParams lp = matchWrap();
            lp.setMargins(0, dp(4), 0, dp(8));
            column.addView(card, lp);
        }

        TextView note = Util.text(host, t(
                "Recordings are shared under the licence shown above. Your own recordings stay on your phone.",
                "Rakaman dikongsi di bawah lesen di atas. Rakaman anda sendiri kekal dalam telefon anda."),
                12, 0xFF78909C);
        note.setPadding(dp(4), dp(6), dp(4), 0);
        column.addView(note, matchWrap());
        return column;
    }

    private String join(List<String> words) {
        StringBuilder out = new StringBuilder();
        int shown = 0;
        for (String word : words) {
            if (shown == 12) {
                out.append(", ...");
                break;
            }
            if (shown > 0) {
                out.append(", ");
            }
            out.append(word);
            shown++;
        }
        return out.toString();
    }

    /** Reads the credit list bundled by tools/fetch_commons_voice.py. */
    private List<String[]> readCredits() {
        List<String[]> rows = new ArrayList<>();
        Context context = host.getApplicationContext();
        try (InputStream in = context.getAssets().open("voice-credits.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    rows.add(splitCsv(line));
                }
            }
        } catch (IOException ignored) {
            // No bundled recordings, so nothing to credit.
        }
        return rows;
    }

    /** Minimal CSV split that honours double quotes. */
    private String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
