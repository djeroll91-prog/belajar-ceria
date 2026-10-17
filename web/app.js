/* Belajar Ceria web app.
   No frameworks and no network: everything here runs from a file:// URL so a
   parent can double-click index.html and hand the laptop to the child. */
(function () {
  "use strict";

  var DATA = window.KSPK;

  /* ------------------------------------------------------------------ store */

  var KEY = "belajar-ceria-web-v1";

  var Store = {
    data: {
      lang: "ms",
      theme: "light",
      sound: true,
      points: 0,
      stars: 0,
      streak: 0,
      bestStreak: 0,
      correct: 0,
      wrong: 0,
      played: {},
      stats: {}   // "unitId|itemKey" -> { c: correct, w: wrong, box: 0..4 }
    },

    load: function () {
      try {
        var raw = localStorage.getItem(KEY);
        if (raw) { this.data = Object.assign(this.data, JSON.parse(raw)); }
      } catch (e) { /* storage can be blocked on file:// in some browsers */ }
      return this.data;
    },

    save: function () {
      try { localStorage.setItem(KEY, JSON.stringify(this.data)); } catch (e) { /* ignore */ }
    },

    reset: function () {
      try { localStorage.removeItem(KEY); } catch (e) { /* ignore */ }
      this.data = {
        lang: this.data.lang, theme: this.data.theme, sound: this.data.sound,
        points: 0, stars: 0, streak: 0, bestStreak: 0, correct: 0, wrong: 0,
        played: {}, stats: {}
      };
      this.save();
    },

    stat: function (unitId, key) {
      var k = unitId + "|" + key;
      if (!this.data.stats[k]) {
        this.data.stats[k] = { c: 0, w: 0, box: 0 };
      }
      return this.data.stats[k];
    },

    /** Leitner: a correct answer promotes the item, a wrong one drops it back. */
    record: function (unitId, key, wasRight) {
      var s = this.stat(unitId, key);
      if (wasRight) {
        s.c++;
        s.box = Math.min(4, s.box + 1);
        this.data.correct++;
        this.data.streak++;
        if (this.data.streak > this.data.bestStreak) { this.data.bestStreak = this.data.streak; }
        this.data.points += 10;
      } else {
        s.w++;
        s.box = 0;
        this.data.wrong++;
        this.data.streak = 0;
      }
      this.save();
      UI.score();
    }
  };

  /* ----------------------------------------------------------------- speech */

  var Speech = {
    voices: [],
    ready: false,

    init: function () {
      if (!window.speechSynthesis) { return; }
      var self = this;
      var load = function () {
        self.voices = window.speechSynthesis.getVoices() || [];
        self.ready = self.voices.length > 0;
      };
      load();
      window.speechSynthesis.onvoiceschanged = load;
    },

    /** Prefers a real Malay voice for Malay, and never uses Indonesian for it. */
    pick: function (lang) {
      var want = lang === "ms" ? /^ms(-|_|$)/i : /^en(-|_|$)/i;
      var list = this.voices;
      for (var i = 0; i < list.length; i++) {
        if (want.test(list[i].lang || "")) { return list[i]; }
      }
      if (lang === "ms") {
        // A Malay text read by an Indonesian voice sounds wrong ("epal" -> "e-pal"),
        // so fall back to the default voice rather than a near-miss.
        for (var j = 0; j < list.length; j++) {
          if (/^id(-|_|$)/i.test(list[j].lang || "")) { return null; }
        }
      }
      return null;
    },

    speak: function (text, cb) {
      if (!text || !Store.data.sound || !window.speechSynthesis) {
        if (cb) { setTimeout(cb, 0); }
        return;
      }
      try {
        window.speechSynthesis.cancel();
        var u = new SpeechSynthesisUtterance(text);
        u.lang = App.lang === "ms" ? "ms-MY" : "en-US";
        u.rate = 0.82;
        u.pitch = 1.12;
        var v = this.pick(App.lang);
        if (v) { u.voice = v; }
        u.onend = function () { if (cb) { cb(); } };
        u.onerror = function () { if (cb) { cb(); } };
        window.speechSynthesis.speak(u);
      } catch (e) {
        if (cb) { cb(); }
      }
    },

    stop: function () {
      if (window.speechSynthesis) { window.speechSynthesis.cancel(); }
    },

    hasVoice: function (lang) { return !!this.pick(lang); }
  };

  /* ------------------------------------------------------------------ items */

  function itemName(item, lang) { return item[lang]; }

  /** The text to say: a letter says its name, a number says the number word. */
  function itemSpoken(item, kind, lang) {
    if (kind === "letter") {
      return lang === "ms" ? (item.spokenMs || item.ms) : (item.spokenEn || item.en);
    }
    if (kind === "number") { return item[lang]; }
    if (item.word && /^[A-Za-z][A-Za-z ]*$/.test(item.word)) { return item.word; }
    return item[lang];
  }

  function itemKey(item, kind) {
    if (kind === "letter") { return item.symbol.split("|")[0]; }
    return item.symbol + "|" + item.en;
  }

  function allItems(unit) {
    if (unit.kind === "sentence") {
      return unit.items.map(function (s, i) {
        return { symbol: "\uD83D\uDCAC", en: s.en, ms: s.ms, kind: "sentence", idx: i };
      });
    }
    return unit.items.map(function (it) {
      return Object.assign({}, it, { kind: unit.kind });
    });
  }

  function unitsOf(tunjangId) {
    var t = DATA.tunjang.filter(function (x) { return x.id === tunjangId; })[0];
    return t ? t.units : [];
  }

  function findUnit(unitId) {
    for (var i = 0; i < DATA.tunjang.length; i++) {
      var us = DATA.tunjang[i].units;
      for (var j = 0; j < us.length; j++) {
        if (us[j].id === unitId) { return us[j]; }
      }
    }
    return null;
  }

  function unitTunjang(unitId) {
    for (var i = 0; i < DATA.tunjang.length; i++) {
      var us = DATA.tunjang[i].units;
      for (var j = 0; j < us.length; j++) {
        if (us[j].id === unitId) { return DATA.tunjang[i]; }
      }
    }
    return null;
  }

  /* -------------------------------------------------------------- Leitner UI */

  var Review = {
    /** Everything the child has met, weakest first. */
    queue: function (limit) {
      var out = [];
      for (var i = 0; i < DATA.tunjang.length; i++) {
        var units = DATA.tunjang[i].units;
        for (var j = 0; j < units.length; j++) {
          var u = units[j];
          var items = allItems(u);
          for (var k = 0; k < items.length; k++) {
            var key = itemKey(items[k], u.kind);
            var s = Store.data.stats[u.id + "|" + key];
            if (!s) { continue; }
            var seen = s.c + s.w;
            var weakness = (s.w * 3) + (4 - s.box) - (s.c * 0.5);
            out.push({
              unit: u, item: items[k], key: key, stats: s,
              seen: seen, weakness: weakness, accuracy: s.c / Math.max(1, seen)
            });
          }
        }
      }
      out.sort(function (a, b) { return b.weakness - a.weakness; });
      return out.slice(0, limit || 12);
    },

    /** Units that need the most practice, for the "ulang subjek lemah" panel. */
    weakUnits: function (limit) {
      var byUnit = {};
      for (var i = 0; i < DATA.tunjang.length; i++) {
        var units = DATA.tunjang[i].units;
        for (var j = 0; j < units.length; j++) {
          var u = units[j];
          var items = allItems(u);
          var c = 0, w = 0, touched = 0;
          for (var k = 0; k < items.length; k++) {
            var s = Store.data.stats[u.id + "|" + itemKey(items[k], u.kind)];
            if (!s) { continue; }
            touched++;
            c += s.c;
            w += s.w;
          }
          if (touched < 3) { continue; }
          var acc = c / Math.max(1, c + w);
          if (acc < 0.8) {
            byUnit[u.id] = { unit: u, accuracy: acc, seen: c + w, wrong: w };
          }
        }
      }
      var list = Object.keys(byUnit).map(function (k) { return byUnit[k]; });
      list.sort(function (a, b) { return a.accuracy - b.accuracy; });
      return list.slice(0, limit || 5);
    }
  };

  /* ------------------------------------------------------------------- utils */

  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) { n.className = cls; }
    if (text !== undefined && text !== null) { n.textContent = text; }
    return n;
  }

  function T(ms, en) { return Store.data.lang === "ms" ? ms : en; }

  function tint(tunjang) {
    return tunjang && tunjang.color ? tunjang.color : "#FF8A65";
  }

  /** Renders an item the way its kind should look: letter, colour, script, number. */
  function glyphNode(item, kind, big) {
    var wrap = el("div", "glyph");
    if (kind === "color") {
      var sw = el("div");
      sw.style.width = big ? "clamp(96px,16vw,170px)" : "64px";
      sw.style.height = sw.style.width;
      sw.style.borderRadius = "50%";
      sw.style.background = item.symbol;
      sw.style.border = "5px solid rgba(0,0,0,.18)";
      sw.style.margin = "0 auto";
      wrap.appendChild(sw);
      return wrap;
    }
    if (kind === "letter") {
      wrap.textContent = item.symbol.split("|")[0];
      return wrap;
    }
    if (kind === "number") {
      wrap.textContent = item.symbol;
      return wrap;
    }
    if (kind === "script") {
      wrap.textContent = item.symbol;
      wrap.style.fontFamily = "'Amiri','Scheherazade New','Noto Naskh Arabic',serif";
      return wrap;
    }
    wrap.textContent = item.symbol;
    return wrap;
  }

  function confetti() {
    var colors = ["#FF8A65", "#FFD166", "#7ED957", "#8FD3F4", "#C792EA"];
    for (var i = 0; i < 26; i++) {
      (function (i) {
        var c = el("div", "confetti");
        c.style.background = colors[i % colors.length];
        c.style.left = (10 + Math.random() * 80) + "vw";
        c.style.top = "-20px";
        c.style.transform = "rotate(" + (Math.random() * 360) + "deg)";
        document.body.appendChild(c);
        var dx = (Math.random() - 0.5) * 220;
        var dur = 1100 + Math.random() * 900;
        c.animate([
          { transform: "translate(0,0) rotate(0deg)", opacity: 1 },
          { transform: "translate(" + dx + "px," + (window.innerHeight + 60) + "px) rotate(720deg)", opacity: .9 }
        ], { duration: dur, easing: "cubic-bezier(.3,.6,.5,1)" }).onfinish = function () { c.remove(); };
      })(i);
    }
  }

  function celebrate(symbol) {
    var box = document.getElementById("celebrate");
    box.innerHTML = "";
    var b = el("div", "burst", symbol || "\uD83C\uDF89");
    box.appendChild(b);
    box.classList.remove("hidden");
    confetti();
    setTimeout(function () { box.classList.add("hidden"); }, 950);
  }

  /* -------------------------------------------------------------------- UI */

  var UI = {
    crumb: function (text) {
      document.getElementById("crumb").textContent = text;
    },

    score: function () {
      document.getElementById("chip-points").textContent = Store.data.points;
    },

    clear: function () {
      var s = document.getElementById("stage");
      s.innerHTML = "";
      return s;
    },

    title: function (parent, ms, en, subMs, subEn) {
      parent.appendChild(el("h1", "screen-title", T(ms, en)));
      if (subMs) { parent.appendChild(el("p", "screen-sub", T(subMs, subEn))); }
    },

    section: function (parent, ms, en, count) {
      var h = el("div", "section-title");
      h.appendChild(el("span", null, T(ms, en)));
      if (count !== undefined) { h.appendChild(el("span", "count", String(count))); }
      parent.appendChild(h);
      return h;
    },

    tile: function (emoji, label, sub, color, onClick, badge) {
      var t = el("div", "tile");
      if (color) { t.classList.add("tinted"); t.style.setProperty("--tint", color); }
      t.appendChild(el("div", "emoji", emoji));
      t.appendChild(el("div", "label", label));
      if (sub) { t.appendChild(el("div", "sub", sub)); }
      if (badge) { t.appendChild(el("div", "badge", badge)); }
      t.addEventListener("click", onClick);
      return t;
    },

    btn: function (label, cls, onClick) {
      var b = el("button", "btn " + (cls || ""), label);
      b.addEventListener("click", onClick);
      return b;
    }
  };

  /* ------------------------------------------------------------------- views */

  var Views = {};

  Views.home = function () {
    var s = UI.clear();
    UI.crumb(T("Utama", "Home"));

    var hero = el("div", "bigcard");
    hero.style.cursor = "default";
    hero.appendChild(el("div", "glyph", "\uD83C\uDF92"));
    hero.appendChild(el("div", "name", T("Selamat datang ke Belajar Ceria!", "Welcome to Belajar Ceria!")));
    hero.appendChild(el("div", "word", T("Prasekolah mengikut KSPK \u2014 6 tunjang",
      "Preschool following KSPK \u2014 6 tunjang")));
    var heroRow = el("div", "row center");
    heroRow.appendChild(UI.btn(T("Mula belajar", "Start learning"), "primary big", function () {
      Views.tunjang(DATA.tunjang[0].id);
    }));
    heroRow.appendChild(UI.btn(T("Ulang kaji lemah", "Review weak spots"), "", function () {
      Views.review();
    }));
    hero.appendChild(heroRow);
    s.appendChild(hero);

    var weak = Review.weakUnits(3);
    if (weak.length) {
      UI.section(s, "Perlu diulang", "Needs practice", weak.length);
      var row = el("div", "row");
      weak.forEach(function (w) {
        var t = unitTunjang(w.unit.id);
        row.appendChild(UI.tile(w.unit.emoji, w.unit[Store.data.lang],
          Math.round(w.accuracy * 100) + "%", tint(t),
          function () { Views.unit(w.unit.id); }, T("lemah", "weak")));
      });
      s.appendChild(row);
    }

    UI.section(s, "Enam tunjang", "The six tunjang", DATA.tunjang.length);
    var grid = el("div", "grid wide");
    DATA.tunjang.forEach(function (t) {
      grid.appendChild(UI.tile(t.emoji, t[Store.data.lang],
        t.units.length + " " + T("unit", "units"), t.color,
        function () { Views.tunjang(t.id); }));
    });
    s.appendChild(grid);

    UI.section(s, "Minggu ini", "This week");
    var wk = weekTheme();
    var tw = el("div", "tile tinted");
    tw.style.setProperty("--tint", "#7ED957");
    tw.style.minHeight = "auto";
    tw.style.alignItems = "flex-start";
    tw.appendChild(el("div", "label", "Minggu " + wk.week + " \u2014 " + wk[Store.data.lang]));
    if (wk.note) { tw.appendChild(el("div", "sub", wk.note)); }
    tw.appendChild(el("div", "sub", T("Ikut tema mingguan tadika", "Follows the preschool weekly theme")));
    s.appendChild(tw);

    UI.section(s, "Aktiviti dan ganjaran", "Activities and rewards");
    var g = el("div", "grid");
    g.appendChild(UI.tile("\uD83C\uDFC6", T("Ganjaran", "Rewards"), T("Markah dan lencana", "Score and badges"),
      "#FFD166", function () { Views.rewards(); }));
    g.appendChild(UI.tile("\uD83E\uDDE0", T("Ulang kaji", "Review"), T("Latih yang lemah", "Practise weak spots"),
      "#C792EA", function () { Views.review(); }));
    g.appendChild(UI.tile("\uD83C\uDFAE", T("Permainan", "Games"), T("Fikir dan main", "Think and play"),
      "#8FD3F4", function () { Views.games(); }));
    g.appendChild(UI.tile("\u2699\uFE0F", T("Tetapan", "Settings"), T("Bahasa dan tema", "Language and theme"),
      "#B9AECF", function () { Views.settings(); }));
    s.appendChild(g);
  };

  function weekTheme() {
    var now = new Date();
    var start = new Date(now.getFullYear(), 0, 1);
    var week = Math.ceil(((now - start) / 86400000 + start.getDay() + 1) / 7);
    week = Math.max(1, Math.min(DATA.themes.length, week));
    return DATA.themes[week - 1];
  }

  Views.tunjang = function (id) {
    var t = DATA.tunjang.filter(function (x) { return x.id === id; })[0];
    var s = UI.clear();
    UI.crumb(t.emoji + " " + t[Store.data.lang]);
    UI.title(s, t.ms, t.en, "Tunjang KSPK", "KSPK tunjang");

    var grid = el("div", "grid wide");
    t.units.forEach(function (u) {
      var items = allItems(u);
      var learned = 0;
      items.forEach(function (it) {
        var st = Store.data.stats[u.id + "|" + itemKey(it, u.kind)];
        if (st && st.box >= 2) { learned++; }
      });
      var sub = learned + "/" + items.length;
      grid.appendChild(UI.tile(u.emoji, u[Store.data.lang], sub, t.color, function () {
        Views.unit(u.id);
      }, u.code || null));
    });
    s.appendChild(grid);
  };

  Views.unit = function (unitId) {
    var u = findUnit(unitId);
    var t = unitTunjang(unitId);
    var s = UI.clear();
    UI.crumb(t.emoji + " " + u[Store.data.lang]);
    UI.title(s, u.ms, u.en, "Tunjang " + t[Store.data.lang], "Tunjang " + t.en);

    if (u.code) {
      s.appendChild(el("p", "screen-sub", "KSPK " + u.code + (u.note ? " \u2014 " + u.note : "")));
    } else if (u.note) {
      s.appendChild(el("p", "screen-sub", u.note));
    }

    var row = el("div", "row");
    row.appendChild(UI.btn(T("\uD83D\uDCD6 Belajar", "\uD83D\uDCD6 Learn"), "primary big", function () {
      Views.learn(unitId);
    }));
    row.appendChild(UI.btn(T("\uD83C\uDFAF Kuiz", "\uD83C\uDFAF Quiz"), "big", function () {
      Games.quiz(unitId);
    }));
    if (u.kind === "text" || u.kind === "word" || u.kind === "letter") {
      row.appendChild(UI.btn(T("\uD83E\uDDE9 Bina", "\uD83E\uDDE9 Build"), "big", function () {
        Games.build(unitId);
      }));
    }
    if (u.kind === "emoji" || u.kind === "color" || u.kind === "number") {
      row.appendChild(UI.btn(T("\uD83D\uDD0D Cari pasangan", "\uD83D\uDD0D Match"), "big", function () {
        Games.memory(unitId);
      }));
    }
    s.appendChild(row);

    UI.section(s, "Kandungan", "Contents", allItems(u).length);
    var grid = el("div", "grid small");
    allItems(u).forEach(function (it) {
      var card = el("div", "tile");
      var g = glyphNode(it, u.kind, false);
      g.style.fontSize = "38px";
      card.appendChild(g);
      card.appendChild(el("div", "label", itemName(it, Store.data.lang)));
      card.addEventListener("click", function () {
        Speech.speak(itemSpoken(it, u.kind, Store.data.lang));
        card.style.transform = "translateY(4px)";
        setTimeout(function () { card.style.transform = ""; }, 130);
      });
      grid.appendChild(card);
    });
    s.appendChild(grid);
  };

  Views.learn = function (unitId) {
    var u = findUnit(unitId);
    var items = allItems(u);
    var i = 0;
    var auto = true;
    var s = UI.clear();
    UI.crumb(u[Store.data.lang]);

    var card = el("div", "bigcard");
    var counter = el("div", "hint");
    var row = el("div", "row center");

    function draw() {
      var it = items[i];
      card.innerHTML = "";
      card.appendChild(glyphNode(it, u.kind, true));
      card.appendChild(el("div", "name", itemName(it, Store.data.lang)));
      if (u.kind !== "letter" && u.kind !== "text" && u.kind !== "script") {
        card.appendChild(el("div", "word", itemSpoken(it, u.kind, Store.data.lang)));
      }
      counter.textContent = (i + 1) + " / " + items.length;
      Store.record(u.id, itemKey(it, u.kind), true);
    }

    function say() {
      var it = items[i];
      Speech.speak(itemSpoken(it, u.kind, Store.data.lang));
    }

    function next() {
      i = (i + 1) % items.length;
      draw();
      if (auto) { say(); }
    }

    card.addEventListener("click", say);
    row.appendChild(UI.btn("\u2B05", "", function () {
      i = (i - 1 + items.length) % items.length; draw(); say();
    }));
    row.appendChild(UI.btn("\uD83D\uDD0A", "primary big", say));
    row.appendChild(UI.btn("\u27A1", "", next));
    row.appendChild(UI.btn("\u25B6\uFE0F", "", function () {
      auto = !auto;
      if (auto) { say(); }
    }));

    s.appendChild(card);
    s.appendChild(counter);
    s.appendChild(row);
    draw();
    say();
  };

  Views.review = function () {
    var s = UI.clear();
    UI.crumb(T("Ulang kaji", "Review"));
    UI.title(s, "Ulang kaji pintar", "Smart review",
      "Item yang selalu salah muncul lebih kerap", "Items you get wrong come back more often");

    var q = Review.queue(12);
    if (!q.length) {
      var empty = el("div", "bigcard");
      empty.style.cursor = "default";
      empty.appendChild(el("div", "glyph", "\uD83C\uDF31"));
      empty.appendChild(el("div", "name", T("Belum ada apa nak diulang", "Nothing to review yet")));
      empty.appendChild(el("div", "word", T("Cuba belajar dan kuiz dahulu", "Learn and try a quiz first")));
      var r0 = el("div", "row center");
      r0.appendChild(UI.btn(T("Mula belajar", "Start learning"), "primary", function () {
        Views.tunjang(DATA.tunjang[0].id);
      }));
      empty.appendChild(r0);
      s.appendChild(empty);
      return;
    }

    var row = el("div", "row");
    row.appendChild(UI.btn(T("Mula ulang kaji", "Start review"), "primary big", function () {
      Games.mixed(q);
    }));

    var weak = Review.weakUnits(5);
    if (weak.length) {
      UI.section(s, "Subjek yang lemah", "Weak subjects", weak.length);
      weak.forEach(function (w) {
        var r = el("div", "review-row");
        var dot = el("div", "dot");
        dot.style.background = w.accuracy < 0.5 ? "var(--bad)" : "#FFD166";
        r.appendChild(dot);
        var name = el("div", "name", w.unit[Store.data.lang]);
        r.appendChild(name);
        r.appendChild(el("div", "why", Math.round(w.accuracy * 100) + "% \u00B7 " + w.wrong + " " + T("salah", "wrong")));
        r.appendChild(UI.btn(T("Latih", "Practise"), "", function () { Games.quiz(w.unit.id); }));
        s.appendChild(r);
      });
    }

    UI.section(s, "Item untuk diulang", "Items to review", q.length);
    q.forEach(function (x) {
      var r = el("div", "review-row");
      var dot = el("div", "dot");
      var acc = x.accuracy;
      dot.style.background = acc < 0.5 ? "var(--bad)" : (acc < 0.8 ? "#FFD166" : "var(--good)");
      r.appendChild(dot);
      r.appendChild(el("div", "name", itemName(x.item, Store.data.lang)));
      r.appendChild(el("div", "why",
        Math.round(acc * 100) + "% \u00B7 " + T("kotak", "box") + " " + x.stats.box + "/4"));
      r.appendChild(UI.btn("\uD83D\uDD0A", "", function () {
        Speech.speak(itemSpoken(x.item, x.unit.kind, Store.data.lang));
      }));
      s.appendChild(r);
    });
    s.appendChild(row);
  };

  Views.rewards = function () {
    var s = UI.clear();
    UI.crumb(T("Ganjaran", "Rewards"));
    UI.title(s, "Ganjaran saya", "My rewards", "Kumpul bintang dan lencana", "Collect stars and badges");

    var g = el("div", "stat-grid");
    [["\u2B50", Store.data.stars, T("bintang", "stars")],
     ["\uD83C\uDFAF", Store.data.points, T("markah", "points")],
     ["\u2705", Store.data.correct, T("betul", "correct")],
     ["\uD83D\uDD25", Store.data.streak, T("berterusan", "streak")],
     ["\uD83C\uDFC6", Store.data.bestStreak, T("terbaik", "best streak")],
     ["\uD83D\uDCDA", Object.keys(Store.data.stats).length, T("item dipelajari", "items met")]
    ].forEach(function (x) {
      var c = el("div", "stat");
      c.appendChild(el("div", "n", x[0] + " " + x[1]));
      c.appendChild(el("div", "k", x[2]));
      g.appendChild(c);
    });
    s.appendChild(g);

    var acc = Store.data.correct + Store.data.wrong;
    var pct = acc ? Math.round(Store.data.correct * 100 / acc) : 0;
    UI.section(s, "Ketepatan", "Accuracy", pct + "%");
    var bar = el("div", "bar");
    var fill = el("span");
    fill.style.width = pct + "%";
    bar.appendChild(fill);
    s.appendChild(bar);

    var badges = [
      { id: "first", emoji: "\uD83C\uDF31", ms: "Langkah pertama", en: "First step", test: function () { return Store.data.correct >= 1; } },
      { id: "ten", emoji: "\uD83D\uDC22", ms: "10 betul", en: "10 correct", test: function () { return Store.data.correct >= 10; } },
      { id: "fifty", emoji: "\uD83D\uDE80", ms: "50 betul", en: "50 correct", test: function () { return Store.data.correct >= 50; } },
      { id: "streak5", emoji: "\uD83D\uDD25", ms: "5 berturut", en: "5 in a row", test: function () { return Store.data.bestStreak >= 5; } },
      { id: "streak10", emoji: "\u26A1", ms: "10 berturut", en: "10 in a row", test: function () { return Store.data.bestStreak >= 10; } },
      { id: "stars", emoji: "\uD83C\uDF1F", ms: "5 bintang", en: "5 stars", test: function () { return Store.data.stars >= 5; } },
      { id: "reader", emoji: "\uD83D\uDCD6", ms: "Suka membaca", en: "Reader", test: function () { return Store.data.correct >= 100; } },
      { id: "robot", emoji: "\uD83E\uDD16", ms: "Kawan robot", en: "Robot friend", test: function () { return Store.data.points >= 500; } }
    ];

    UI.section(s, "Lencana", "Badges", badges.length);
    var bg = el("div", "grid small");
    badges.forEach(function (b) {
      var got = b.test();
      var t = el("div", "tile");
      if (!got) { t.classList.add("locked"); }
      t.appendChild(el("div", "emoji", got ? b.emoji : "\uD83D\uDD12"));
      t.appendChild(el("div", "label", b[Store.data.lang]));
      bg.appendChild(t);
    });
    s.appendChild(bg);

    var row = el("div", "row");
    row.appendChild(UI.btn(T("Set semula markah", "Reset score"), "", function () {
      if (confirm(T("Padam semua markah?", "Delete all scores?"))) {
        Store.reset();
        UI.score();
        Views.home();
      }
    }));
    s.appendChild(row);
  };

  Views.settings = function () {
    var s = UI.clear();
    UI.crumb(T("Tetapan", "Settings"));
    UI.title(s, "Tetapan", "Settings", "Untuk ibu bapa dan guru", "For parents and teachers");

    UI.section(s, "Bahasa", "Language");
    var lr = el("div", "row");
    lr.appendChild(UI.btn("Bahasa Melayu", Store.data.lang === "ms" ? "primary" : "", function () {
      App.setLang("ms"); Views.settings();
    }));
    lr.appendChild(UI.btn("English", Store.data.lang === "en" ? "primary" : "", function () {
      App.setLang("en"); Views.settings();
    }));
    s.appendChild(lr);

    UI.section(s, "Tema", "Theme");
    var tr = el("div", "row");
    tr.appendChild(UI.btn("\u2600\uFE0F " + T("Cerah", "Light"), Store.data.theme === "light" ? "primary" : "", function () {
      App.setTheme("light"); Views.settings();
    }));
    tr.appendChild(UI.btn("\uD83C\uDF19 " + T("Gelap", "Dark"), Store.data.theme === "dark" ? "primary" : "", function () {
      App.setTheme("dark"); Views.settings();
    }));
    s.appendChild(tr);

    UI.section(s, "Bunyi", "Sound");
    var sr = el("div", "row");
    sr.appendChild(UI.btn(Store.data.sound ? "\uD83D\uDD0A " + T("Hidup", "On") : "\uD83D\uDD07 " + T("Mati", "Off"),
      Store.data.sound ? "primary" : "", function () {
        Store.data.sound = !Store.data.sound; Store.save(); App.chrome(); Views.settings();
      }));
    sr.appendChild(UI.btn(T("Uji suara", "Test voice"), "", function () {
      Speech.speak(T("Hai kawan! Jom belajar.", "Hi friend! Let's learn."));
    }));
    s.appendChild(sr);

    var hasMs = Speech.hasVoice("ms");
    s.appendChild(el("p", "screen-sub",
      hasMs ? T("Suara Bahasa Melayu dijumpai pada peranti ini.",
        "A Malay voice was found on this device.")
        : T("Tiada suara Bahasa Melayu pada peranti ini. Aplikasi guna suara asal pelayar.",
          "No Malay voice on this device. The browser default voice is used.")));

    UI.section(s, "Kurikulum", "Curriculum");
    var meta = DATA.meta;
    var info = el("div", "tile");
    info.style.minHeight = "auto";
    info.style.alignItems = "flex-start";
    info.style.cursor = "default";
    info.appendChild(el("div", "label", meta.curriculum));
    info.appendChild(el("div", "sub", T("Umur", "Ages") + ": " + meta.ages));
    info.appendChild(el("div", "sub", DATA.tunjang.length + " tunjang \u00B7 " +
      DATA.tunjang.reduce(function (a, t) { return a + t.units.length; }, 0) + " unit \u00B7 " +
      DATA.themes.length + " " + T("tema mingguan", "weekly themes")));
    info.appendChild(el("div", "sub", meta.note));
    s.appendChild(info);
  };

  Views.games = function () {
    var s = UI.clear();
    UI.crumb(T("Permainan", "Games"));
    UI.title(s, "Permainan", "Games", "Latihan untuk merangsang otak", "Brain-training practice");

    var list = [
      { emoji: "\uD83E\uDDE0", ms: "Ulang kaji campur", en: "Mixed review", color: "#C792EA", go: function () { Views.review(); } },
      { emoji: "\uD83D\uDD0A", ms: "Dengar dan pilih", en: "Listen and choose", color: "#8FD3F4", go: function () { Games.listen(); } },
      { emoji: "\uD83C\uDFAF", ms: "Kuiz cepat", en: "Quick quiz", color: "#FF8A65", go: function () { Games.quiz("ma-nombor"); } },
      { emoji: "\uD83E\uDDE9", ms: "Susun huruf", en: "Spell it", color: "#7ED957", go: function () { Games.build("bm-perkataan"); } },
      { emoji: "\uD83D\uDD0D", ms: "Cari pasangan", en: "Matching pairs", color: "#FFD166", go: function () { Games.memory("sa-buah"); } },
      { emoji: "\uD83E\uDEE7", ms: "Letup gelembung", en: "Bubble pop", color: "#5FA8D3", go: function () { Games.bubble(); } },
      { emoji: "\uD83E\uDDFA", ms: "Bakul susun", en: "Sorting baskets", color: "#66BB6A", go: function () { Games.sort(); } },
      { emoji: "\uD83E\uDDE9", ms: "Apa seterusnya", en: "What comes next", color: "#B48FC9", go: function () { Games.pattern(); } },
      { emoji: "\uD83D\uDC22", ms: "Kira objek", en: "Count", color: "#26C6DA", go: function () { Games.count(); } },
      { emoji: "\uD83D\uDD0D", ms: "Cari yang berbeza", en: "Odd one out", color: "#F09B5B", go: function () { Games.oddOne(); } }
    ];

    var grid = el("div", "grid wide");
    list.forEach(function (g) {
      grid.appendChild(UI.tile(g.emoji, g[Store.data.lang], null, g.color, g.go));
    });
    s.appendChild(grid);
  };

  /* ------------------------------------------------------------------- chrome */

  var App = {
    lang: "ms",

    setLang: function (l) {
      Store.data.lang = l;
      App.lang = l;
      Store.save();
      App.chrome();
      Robot.say(Robot.greeting(), 2000);
    },

    setTheme: function (t) {
      Store.data.theme = t;
      Store.save();
      document.documentElement.setAttribute("data-theme", t);
      document.querySelector('meta[name="theme-color"]').setAttribute(
        "content", t === "dark" ? "#1B1726" : "#FFF6EC");
    },

    chrome: function () {
      document.getElementById("btn-lang").textContent = Store.data.lang === "ms" ? "BM" : "EN";
      document.getElementById("btn-theme").textContent = Store.data.theme === "dark" ? "\u2600\uFE0F" : "\uD83C\uDF19";
      document.getElementById("btn-sound").textContent = Store.data.sound ? "\uD83D\uDD0A" : "\uD83D\uDD07";
      UI.score();
    },

    go: function (view) {
      Speech.stop();
      Robot.hide();
      view();
      window.scrollTo(0, 0);
    },

    init: function () {
      Store.load();
      App.lang = Store.data.lang;
      App.setTheme(Store.data.theme);
      Speech.init();
      App.chrome();

      document.getElementById("btn-home").addEventListener("click", function () { App.go(Views.home); });
      document.getElementById("btn-lang").addEventListener("click", function () {
        App.setLang(Store.data.lang === "ms" ? "en" : "ms");
        App.go(Views.home);
      });
      document.getElementById("btn-theme").addEventListener("click", function () {
        App.setTheme(Store.data.theme === "dark" ? "light" : "dark");
      });
      document.getElementById("btn-sound").addEventListener("click", function () {
        Store.data.sound = !Store.data.sound;
        Store.save();
        App.chrome();
        if (!Store.data.sound) { Speech.stop(); }
      });
      document.getElementById("score-chip").addEventListener("click", function () { App.go(Views.rewards); });

      App.go(Views.home);
      setTimeout(function () {
        Robot.say(Robot.greeting(), 2600);
        Speech.speak(Robot.greeting());
      }, 700);
    }
  };

  window.App = App;
  window.Views = Views;
  window.UI = UI;
  window.Store = Store;
  window.Speech = Speech;
  window.Review = Review;
  window.Helpers = {
    el: el, T: T, tint: tint, glyphNode: glyphNode, celebrate: celebrate,
    itemName: itemName, itemSpoken: itemSpoken, itemKey: itemKey, allItems: allItems,
    unitsOf: unitsOf, findUnit: findUnit, unitTunjang: unitTunjang, DATA: DATA
  };

  window.addEventListener("DOMContentLoaded", App.init);
})();
