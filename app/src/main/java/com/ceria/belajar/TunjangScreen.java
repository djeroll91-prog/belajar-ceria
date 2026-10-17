package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One tunjang and its units. Each unit shows how much of it the child has
 * mastered, so the next thing to do is obvious without reading a menu.
 */
public class TunjangScreen extends BaseScreen {

    private final Content.Tunjang tunjang;

    public TunjangScreen(MainActivity host, Content.Tunjang tunjang) {
        super(host);
        this.tunjang = tunjang;
    }

    @Override
    protected String titleText() {
        return tunjang.emoji + " " + tunjang.title(lang);
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        int color = theme.tunjangColor(tunjang.color);

        TextView sub = Util.text(host, t("Tunjang KSPK", "KSPK tunjang"), 13, theme.inkSoft());
        sub.setGravity(Gravity.START);
        sub.setPadding(dp(4), 0, 0, dp(10));
        column.addView(sub, matchWrap());

        GridLayout grid = new GridLayout(host);
        grid.setColumnCount(host.getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2);
        column.addView(grid, matchWrap());

        for (Content.Unit unit : tunjang.units) {
            grid.addView(unitTile(unit, color), tileParams());
        }
        return scrollWrap(column);
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(124);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View unitTile(Content.Unit unit, int color) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(12), dp(8), dp(12));
        tile.setBackground(Util.roundedStroke(theme.wash(color, 0.14f), dp(22), color, dp(3)));
        tile.setElevation(dp(3));
        tile.setClickable(true);

        TextView icon = Util.text(host, unit.emoji, 30, theme.ink());
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon);

        TextView name = Util.text(host, unit.title(lang), 14, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        tile.addView(name);

        TextView progress = Util.text(host,
                host.progress().masteredIn(unit) + "/" + unit.items.size(), 12, theme.inkSoft());
        progress.setGravity(Gravity.CENTER);
        tile.addView(progress);

        tile.setOnClickListener(v -> {
            host.tapFeedback(v);
            v.postDelayed(() -> host.open(new UnitScreen(host, unit)), 90);
        });
        return tile;
    }
}
