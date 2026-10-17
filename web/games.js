/* The practice games. Each one records every answer through Store.record, which
   is what feeds the Leitner review queue, so playing a game is also what teaches
   the app which items a child finds hard. */
(function () {
  "use strict";

  // Helpers live in app.js, which loads after this file, so they are read
  // lazily rather than captured at load time.
  function H() { return window.Helpers; }

  function el() { return H().el.apply(null, arguments); }
  function T(ms, en) { return H().T(ms, en); }

  function shuffle(a) {
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  function pickN(list, n) { return shuffle(list.slice()).slice(0, n); }

  function say(text, cb) { window.Speech.speak(text, cb); }

  function feedback(correct) {
    if (correct) {
      window.Robot.mood("cheer");
      window.Robot.say(window.Robot.praise(), 1500);
      say(window.Robot.praise());
    } else {
      window.Robot.mood("oops");
      window.Robot.say(window.Robot.encourage(), 1500);
    }
  }

  /** A round of N questions over a list of items, with a result card at the end. */
  function runQuestions(titleMs, titleEn, questions, onFinish) {
    var s = window.UI.clear();
    window.UI.crumb(T(titleMs, titleEn));
    var i = 0, right = 0;
    var head = el("div", "row");
    var counter = el("div", "hint");
    var bar = el("div", "bar");
    var fill = el("span");
    bar.appendChild(fill);

    function render() {
      if (i >= questions.length) { return finish(); }
      var q = questions[i];
      s.innerHTML = "";
      window.UI.crumb(T(titleMs, titleEn));

      var top = el("div", "row");
      top.appendChild(el("div", "section-title", q.prompt));
      top.appendChild(el("div", "spacer"));
      top.appendChild(el("div", "hint", (i + 1) + " / " + questions.length));
      s.appendChild(top);

      fill.style.width = Math.round(i * 100 / questions.length) + "%";
      s.appendChild(bar);

      if (q.say) {
        var speakRow = el("div", "row center");
        speakRow.appendChild(window.UI.btn("\uD83D\uDD0A " + T("Dengar", "Listen"), "primary big", function () {
          say(q.say);
        }));
        s.appendChild(speakRow);
      }

      if (q.show) {
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.style.minHeight = "auto";
        card.appendChild(q.show);
        s.appendChild(card);
      }

      var grid = el("div", "option-grid");
      q.options.forEach(function (opt) {
        var o = el("div", "option");
        var node = opt.node ? opt.node : el("div", null, opt.label);
        o.appendChild(node);
        o.addEventListener("click", function () {
          if (o.dataset.done) { return; }
          var ok = opt.correct;
          o.dataset.done = "1";
          if (ok) {
            o.classList.add("correct");
            right++;
            window.Store.record(q.unitId, q.key, true);
            feedback(true);
          } else {
            o.classList.add("wrong");
            window.Store.record(q.unitId, q.key, false);
            feedback(false);
            grid.querySelectorAll(".option").forEach(function (x) {
              if (x.dataset.correct === "1") { x.classList.add("correct"); }
            });
          }
          setTimeout(function () { i++; render(); }, ok ? 750 : 1250);
        });
        if (opt.correct) { o.dataset.correct = "1"; }
        grid.appendChild(o);
      });
      s.appendChild(grid);
      if (q.say) { say(q.say); }
    }

    function finish() {
      fill.style.width = "100%";
      var pct = Math.round(right * 100 / questions.length);
      var stars = pct >= 90 ? 3 : pct >= 70 ? 2 : pct >= 40 ? 1 : 0;
      for (var k = 0; k < stars; k++) { window.Store.data.stars++; }
      window.Store.save();
      window.UI.score();

      s.innerHTML = "";
      var card = el("div", "bigcard");
      card.style.cursor = "default";
      card.appendChild(el("div", "glyph", stars === 3 ? "\uD83C\uDFC6" : stars === 2 ? "\uD83C\uDF89" : "\uD83D\uDCAA"));
      card.appendChild(el("div", "name", right + " / " + questions.length));
      card.appendChild(el("div", "stars", "\u2B50".repeat(stars) || "\u2014"));
      card.appendChild(el("div", "word", T("Markah: ", "Points: ") + window.Store.data.points));

      var row = el("div", "row center");
      row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () {
        (onFinish && onFinish.again ? onFinish.again : function () { window.App.go(window.Views.home); })();
      }));
      row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
      card.appendChild(row);
      s.appendChild(card);

      if (stars >= 2) { H().celebrate(stars === 3 ? "\uD83C\uDFC6" : "\uD83C\uDF89"); }
      window.Robot.mood(stars >= 2 ? "cheer" : "think");
      window.Robot.say(window.Robot.praise(), 2500);
      say(window.Robot.praise());
    }

    render();
  }

  /** Builds the question set for a unit: pick the right kind of question. */
  function questionsFor(unit, count) {
    var items = H().allItems(unit);
    if (!items.length) { return []; }
    var pool = pickN(items, Math.min(count, items.length));
    return pool.map(function (item) {
      var kind = unit.kind;
      var label = H().itemSpoken(item, kind, window.Store.data.lang);
      var options = [];
      var others = pickN(items.filter(function (x) { return x !== item; }), 3);

      var correctNode = null;
      if (kind === "color") {
        correctNode = swatchNode(item.symbol);
        options.push({ node: swatchNode(item.symbol), correct: true });
      } else {
        correctNode = null;
        options.push({ label: displayOf(item, kind), correct: true });
      }
      others.forEach(function (o) {
        if (kind === "color") {
          options.push({ node: swatchNode(o.symbol), correct: false });
        } else {
          options.push({ label: displayOf(o, kind), correct: false });
        }
      });
      shuffle(options);

      return {
        unitId: unit.id,
        key: H().itemKey(item, kind),
        prompt: T("Yang mana ", "Which one is ") + label + "?",
        say: label,
        options: options,
        show: kind === "color" ? swatchNode(item.symbol, true) : null
      };
    });
  }

  function displayOf(item, kind) {
    if (kind === "letter") { return item.symbol.split("|")[0]; }
    if (kind === "number") { return item.symbol; }
    if (kind === "script") { return item.symbol; }
    if (kind === "emoji" || kind === "word" || kind === "sentence") {
      return item.symbol + "  " + item[window.Store.data.lang];
    }
    return item[window.Store.data.lang];
  }

  function swatchNode(hex, big) {
    var d = el("div");
    d.style.width = big ? "clamp(90px,15vw,150px)" : "72px";
    d.style.height = d.style.width;
    d.style.borderRadius = "50%";
    d.style.background = hex;
    d.style.border = "5px solid rgba(0,0,0,.18)";
    d.style.margin = "0 auto";
    return d;
  }

  /* ------------------------------------------------------------------- games */

  var Games = {
    quiz: function (unitId) {
      var unit = H().findUnit(unitId);
      var qs = questionsFor(unit, 8);
      if (!qs.length) { return; }
      runQuestions(unit.ms, unit.en, qs, {
        again: function () { Games.quiz(unitId); }
      });
    },

    /** A review round over whatever the child got wrong most often. */
    mixed: function (queue) {
      if (!queue || !queue.length) { window.Views.review(); return; }
      var items = queue.slice(0, 10);
      var qs = items.map(function (x) {
        var unit = x.unit;
        var kind = unit.kind;
        var all = H().allItems(unit);
        var label = H().itemSpoken(x.item, kind, window.Store.data.lang);
        var options = [{ label: displayOf(x.item, kind), correct: true }];
        pickN(all.filter(function (o) { return H().itemKey(o, kind) !== x.key; }), 3)
          .forEach(function (o) { options.push({ label: displayOf(o, kind), correct: false }); });
        shuffle(options);
        return {
          unitId: unit.id, key: x.key,
          prompt: T("Yang mana ", "Which one is ") + label + "?",
          say: label, options: options
        };
      });
      runQuestions("Ulang kaji", "Review", qs, {
        again: function () { window.Views.review(); }
      });
    },

    /** Listen to a word and choose it, the purest listening exercise. */
    listen: function () {
      var unit = H().findUnit("bm-perkataan");
      var items = H().allItems(unit);
      var qs = pickN(items, 8).map(function (item) {
        var label = item[window.Store.data.lang];
        var options = [{ label: item.symbol + "  " + label, correct: true }];
        pickN(items.filter(function (x) { return x !== item; }), 3).forEach(function (o) {
          options.push({ label: o.symbol + "  " + o[window.Store.data.lang], correct: false });
        });
        shuffle(options);
        return {
          unitId: unit.id, key: H().itemKey(item, unit.kind),
          prompt: T("Dengar dan pilih", "Listen and choose"),
          say: H().itemSpoken(item, unit.kind, window.Store.data.lang),
          options: options
        };
      });
      runQuestions("Dengar dan pilih", "Listen and choose", qs, { again: function () { Games.listen(); } });
    },

    /** Split a word into syllables (Malay) or letters (English) and rebuild it. */
    build: function (unitId) {
      var unit = H().findUnit(unitId);
      var items = H().allItems(unit);
      var useLetters = unitId.indexOf("bm") !== 0;
      var pool = pickN(items, 6);

      var s = window.UI.clear();
      window.UI.crumb(unit[window.Store.data.lang]);
      var i = 0, right = 0;

      function chunk(text) {
        if (useLetters || text.length <= 2) { return text.split(""); }
        var out = [];
        for (var k = 0; k < text.length; k += 2) { out.push(text.substr(k, 2)); }
        return out;
      }

      function render() {
        if (i >= pool.length) { return done(); }
        var item = pool[i];
        var text = item.word || item.symbol;
        var parts = chunk(text);
        var placed = [];

        s.innerHTML = "";
        var top = el("div", "row");
        top.appendChild(el("div", "section-title", T("Bina perkataan", "Build the word")));
        top.appendChild(el("div", "spacer"));
        top.appendChild(el("div", "hint", (i + 1) + " / " + pool.length));
        s.appendChild(top);

        var hintCard = el("div", "bigcard");
        hintCard.style.cursor = "default";
        hintCard.style.minHeight = "auto";
        hintCard.appendChild(el("div", "glyph", item.symbol));
        hintCard.appendChild(el("div", "word", T("Bina: ", "Build: ") + text));
        var hearRow = el("div", "row center");
        hearRow.appendChild(window.UI.btn("\uD83D\uDD0A " + T("Dengar", "Listen"), "primary", function () {
          say(H().itemSpoken(item, unit.kind, window.Store.data.lang));
        }));
        hintCard.appendChild(hearRow);
        s.appendChild(hintCard);

        var slots = el("div", "slots");
        var slotNodes = parts.map(function (p) {
          var sl = el("div", "slot");
          slots.appendChild(sl);
          return sl;
        });
        s.appendChild(slots);

        var chips = el("div", "chip-row");
        shuffle(parts.map(function (p, idx) { return { p: p, idx: idx }; })).forEach(function (c) {
          var chip = el("div", "chip", c.p);
          chip.addEventListener("click", function () {
            if (placed.length >= parts.length) { return; }
            var want = parts[placed.length];
            if (c.p === want) {
              placed.push(c.p);
              slotNodes[placed.length - 1].textContent = c.p;
              slotNodes[placed.length - 1].classList.add("filled");
              chip.classList.add("used");
              if (placed.length === parts.length) {
                right++;
                window.Store.record(unit.id, H().itemKey(item, unit.kind), true);
                feedback(true);
                setTimeout(function () { i++; render(); }, 850);
              }
            } else {
              window.Store.record(unit.id, H().itemKey(item, unit.kind), false);
              feedback(false);
              chip.animate([{ transform: "translateX(0)" }, { transform: "translateX(-8px)" },
                { transform: "translateX(8px)" }, { transform: "translateX(0)" }], { duration: 260 });
            }
          });
          chips.appendChild(chip);
        });
        s.appendChild(chips);
      }

      function done() {
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.appendChild(el("div", "glyph", "\uD83E\uDDF1"));
        card.appendChild(el("div", "name", right + " / " + pool.length));
        var row = el("div", "row center");
        row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () { Games.build(unitId); }));
        row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
        card.appendChild(row);
        s.innerHTML = "";
        s.appendChild(card);
        if (right >= pool.length - 1) { H().celebrate("\uD83C\uDF89"); }
      }

      render();
    },

    /** Turn over pairs. Also the gentlest memory workout for 4-year-olds. */
    memory: function (unitId) {
      var unit = H().findUnit(unitId);
      var items = H().allItems(unit);
      var chosen = pickN(items, Math.min(6, items.length));
      var deck = [];
      chosen.forEach(function (it) {
        deck.push({ it: it, id: H().itemKey(it, unit.kind) });
        deck.push({ it: it, id: H().itemKey(it, unit.kind) });
      });
      shuffle(deck);

      var s = window.UI.clear();
      window.UI.crumb(unit[window.Store.data.lang]);
      window.UI.title(s, "Cari pasangan", "Matching pairs",
        "Buka dua kad yang sama", "Turn over two matching cards");

      var grid = el("div", "grid small");
      grid.style.gridTemplateColumns = "repeat(auto-fit, minmax(96px, 1fr))";
      var open = [];
      var matched = 0;
      var lock = false;

      deck.forEach(function (card) {
        var t = el("div", "tile");
        t.style.minHeight = "110px";
        t.textContent = "?";
        t.style.fontSize = "34px";
        t.style.fontWeight = "900";
        t.addEventListener("click", function () {
          if (lock || t.dataset.open === "1" || t.dataset.done === "1") { return; }
          t.dataset.open = "1";
          t.textContent = "";
          t.appendChild(H().glyphNode(card.it, unit.kind, false));
          say(H().itemSpoken(card.it, unit.kind, window.Store.data.lang));
          open.push({ node: t, card: card });
          if (open.length === 2) {
            lock = true;
            var a = open[0], b = open[1];
            if (a.card.id === b.card.id) {
              matched++;
              window.Store.record(unit.id, a.card.id, true);
              feedback(true);
              a.node.dataset.done = "1"; b.node.dataset.done = "1";
              open = []; lock = false;
              if (matched === chosen.length) {
                setTimeout(function () {
                  H().celebrate("\uD83C\uDF89");
                  window.Robot.mood("cheer");
                  window.UI.score();
                }, 400);
              }
            } else {
              window.Store.record(unit.id, a.card.id, false);
              feedback(false);
              setTimeout(function () {
                [a, b].forEach(function (x) {
                  x.node.dataset.open = "";
                  x.node.textContent = "?";
                  x.node.innerHTML = "?";
                });
                open = []; lock = false;
              }, 900);
            }
          }
        });
        grid.appendChild(t);
      });
      s.appendChild(grid);
    },

    /** Bubbles drift up; tap the one that matches what the robot says. */
    bubble: function () {
      var unit = H().findUnit("bm-huruf");
      var items = H().allItems(unit);
      var targets = pickN(items, 8);
      var s = window.UI.clear();
      window.UI.crumb(T("Letup gelembung", "Bubble pop"));
      var i = 0;

      var field = el("div");
      field.style.position = "relative";
      field.style.height = "clamp(320px, 52vh, 520px)";
      field.style.overflow = "hidden";
      field.style.borderRadius = "26px";
      field.style.border = "3px solid var(--line)";
      field.style.background = "linear-gradient(180deg, color-mix(in srgb, var(--accent) 10%, var(--surface)), var(--surface))";

      var head = el("div", "row center");
      var sayBtn = window.UI.btn("\uD83D\uDD0A " + T("Dengar sasaran", "Hear the target"), "primary big", function () {
        say(targetSpoken());
      });
      head.appendChild(sayBtn);
      s.appendChild(head);
      s.appendChild(field);

      function targetSpoken() {
        var t = targets[i];
        return H().itemSpoken(t, unit.kind, window.Store.data.lang);
      }

      function spawn() {
        if (i >= targets.length) { return finish(); }
        field.innerHTML = "";
        var target = targets[i];
        var choices = [target].concat(pickN(items.filter(function (x) { return x !== target; }), 3));
        shuffle(choices);
        say(targetSpoken());
        window.Robot.say(T("Cari ", "Find ") + targetSpoken(), 2200);

        choices.forEach(function (choice, idx) {
          var b = el("div");
          var size = 84;
          b.style.position = "absolute";
          b.style.width = size + "px";
          b.style.height = size + "px";
          b.style.borderRadius = "50%";
          b.style.display = "grid";
          b.style.placeItems = "center";
          b.style.fontSize = "34px";
          b.style.fontWeight = "900";
          b.style.cursor = "pointer";
          b.style.background = "radial-gradient(circle at 32% 28%, #fff9, transparent 60%), " +
            ["#8FD3F4", "#FFD166", "#7ED957", "#C792EA"][idx % 4];
          b.style.border = "3px solid rgba(255,255,255,.7)";
          b.style.boxShadow = "0 8px 20px rgba(0,0,0,.16)";
          b.textContent = choice.symbol.split("|")[0];
          b.style.left = (8 + Math.random() * 72) + "%";
          b.style.top = (10 + Math.random() * 60) + "%";
          b.animate([
            { transform: "translateY(0) scale(1)" },
            { transform: "translateY(-14px) scale(1.03)" },
            { transform: "translateY(0) scale(1)" }
          ], { duration: 2200 + idx * 320, iterations: Infinity, easing: "ease-in-out" });

          b.addEventListener("click", function () {
            if (field.dataset.lock) { return; }
            if (choice === target) {
              field.dataset.lock = "1";
              window.Store.record(unit.id, H().itemKey(target, unit.kind), true);
              feedback(true);
              b.animate([{ transform: "scale(1)", opacity: 1 }, { transform: "scale(1.6)", opacity: 0 }],
                { duration: 380 }).onfinish = function () {
                  field.dataset.lock = "";
                  i++;
                  spawn();
                };
            } else {
              window.Store.record(unit.id, H().itemKey(target, unit.kind), false);
              feedback(false);
              b.animate([{ transform: "translateX(0)" }, { transform: "translateX(-10px)" },
                { transform: "translateX(10px)" }, { transform: "translateX(0)" }], { duration: 300 });
            }
          });
          field.appendChild(b);
        });
      }

      function finish() {
        field.innerHTML = "";
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.appendChild(el("div", "glyph", "\uD83E\uDEE7"));
        card.appendChild(el("div", "name", T("Siap semua!", "All done!")));
        var row = el("div", "row center");
        row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () { Games.bubble(); }));
        row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
        card.appendChild(row);
        s.innerHTML = "";
        s.appendChild(card);
        H().celebrate("\uD83E\uDEE7");
      }

      spawn();
    },

    /** Sort items into two baskets. Tap the item, then tap the basket. */
    sort: function () {
      var left = H().findUnit("sa-haiwan");
      var right = H().findUnit("sa-tumbuhan");
      var a = pickN(H().allItems(left), 4);
      var b = pickN(H().allItems(right), 4);
      var queue = shuffle(a.map(function (x) { return { it: x, unit: left }; })
        .concat(b.map(function (x) { return { it: x, unit: right }; })));

      var s = window.UI.clear();
      window.UI.crumb(T("Bakul susun", "Sorting baskets"));
      window.UI.title(s, "Susun ikut kumpulan", "Sort into groups",
        "Haiwan atau tumbuhan?", "Animal or plant?");

      var picked = null;
      var tray = el("div", "chip-row");
      tray.style.minHeight = "110px";
      s.appendChild(tray);

      var baskets = el("div", "row");
      baskets.style.marginTop = "18px";
      [[left, "\uD83D\uDC3E"], [right, "\uD83C\uDF31"]].forEach(function (pair) {
        var u = pair[0];
        var box = el("div", "tile");
        box.style.flex = "1";
        box.style.minHeight = "170px";
        box.appendChild(el("div", "emoji", pair[1]));
        box.appendChild(el("div", "label", u[window.Store.data.lang]));
        box.addEventListener("click", function () {
          if (!picked) { return; }
          var ok = picked.unit === u;
          window.Store.record(u.id, H().itemKey(picked.it, u.kind), ok);
          feedback(ok);
          if (ok) {
            picked.node.remove();
            picked = null;
            if (!tray.children.length) {
              setTimeout(function () { H().celebrate("\uD83E\uDDFA"); }, 350);
            }
          } else {
            picked.node.animate([{ transform: "translateX(0)" }, { transform: "translateX(-10px)" },
              { transform: "translateX(10px)" }, { transform: "translateX(0)" }], { duration: 300 });
          }
        });
        baskets.appendChild(box);
      });
      s.appendChild(baskets);

      queue.forEach(function (x) {
        var c = el("div", "chip");
        c.appendChild(H().glyphNode(x.it, x.unit.kind, false));
        c.appendChild(el("div", null, x.it[window.Store.data.lang]));
        c.addEventListener("click", function () {
          tray.querySelectorAll(".chip").forEach(function (n) { n.style.outline = "none"; });
          c.style.outline = "4px solid var(--accent)";
          picked = { it: x.it, unit: x.unit, node: c };
          say(H().itemSpoken(x.it, x.unit.kind, window.Store.data.lang));
        });
        tray.appendChild(c);
      });
    },

    /** Complete the pattern. */
    pattern: function () {
      var unit = H().findUnit("ma-pola");
      var items = H().allItems(unit);
      var s = window.UI.clear();
      window.UI.crumb(T("Apa seterusnya?", "What comes next?"));
      var i = 0, right = 0, rounds = 6;

      function render() {
        if (i >= rounds) { return finish(); }
        var three = pickN(items, 2);
        var seq = [three[0], three[1], three[0], three[1], three[0]];
        var answer = three[1];
        var choices = shuffle([three[0], three[1]]);

        s.innerHTML = "";
        window.UI.title(s, "Apa seterusnya?", "What comes next?",
          "Lengkapkan corak", "Complete the pattern");

        var row = el("div", "chip-row");
        seq.forEach(function (x) {
          var c = el("div", "chip");
          c.style.fontSize = "44px";
          c.textContent = x.symbol;
          row.appendChild(c);
        });
        var q = el("div", "chip");
        q.style.fontSize = "44px";
        q.textContent = "?";
        q.style.borderColor = "var(--accent)";
        row.appendChild(q);
        s.appendChild(row);

        var grid = el("div", "option-grid");
        choices.forEach(function (c) {
          var o = el("div", "option");
          o.style.fontSize = "46px";
          o.textContent = c.symbol;
          o.addEventListener("click", function () {
            if (o.dataset.done) { return; }
            o.dataset.done = "1";
            var ok = c === answer;
            window.Store.record(unit.id, H().itemKey(c, unit.kind), ok);
            feedback(ok);
            o.classList.add(ok ? "correct" : "wrong");
            if (ok) { right++; }
            setTimeout(function () { i++; render(); }, ok ? 700 : 1100);
          });
          grid.appendChild(o);
        });
        s.appendChild(grid);
      }

      function finish() {
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.appendChild(el("div", "glyph", "\uD83E\uDDE9"));
        card.appendChild(el("div", "name", right + " / " + rounds));
        var row = el("div", "row center");
        row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () { Games.pattern(); }));
        row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
        card.appendChild(row);
        s.innerHTML = "";
        s.appendChild(card);
        if (right >= rounds - 1) { H().celebrate("\uD83E\uDDE9"); }
      }

      render();
    },

    /** Count the pictures and choose the number. */
    count: function () {
      var unit = H().findUnit("ma-nombor");
      var items = H().allItems(unit);
      var s = window.UI.clear();
      window.UI.crumb(T("Kira objek", "Count"));
      var i = 0, right = 0, rounds = 8;

      function render() {
        if (i >= rounds) { return finish(); }
        var n = 1 + Math.floor(Math.random() * 10);
        var target = items[n - 1];
        var pic = "\uD83C\uDF4E";

        s.innerHTML = "";
        window.UI.title(s, "Kira objek", "Count the pictures", "Berapa banyak?", "How many?");
        var box = el("div", "bigcard");
        box.style.cursor = "default";
        box.style.minHeight = "auto";
        var pics = el("div");
        pics.style.fontSize = "clamp(28px, 5vw, 44px)";
        pics.style.lineHeight = "1.5";
        pics.style.textAlign = "center";
        pics.style.maxWidth = "620px";
        pics.textContent = new Array(n + 1).join(pic + " ");
        box.appendChild(pics);
        s.appendChild(box);

        var choices = [target];
        pickN(items.filter(function (x) { return x !== target; }), 3).forEach(function (x) { choices.push(x); });
        shuffle(choices);

        var grid = el("div", "option-grid");
        choices.forEach(function (c) {
          var o = el("div", "option");
          o.textContent = c.symbol;
          o.addEventListener("click", function () {
            if (o.dataset.done) { return; }
            o.dataset.done = "1";
            var ok = c === target;
            window.Store.record(unit.id, H().itemKey(target, unit.kind), ok);
            feedback(ok);
            if (ok) { say(H().itemSpoken(target, unit.kind, window.Store.data.lang)); }
            o.classList.add(ok ? "correct" : "wrong");
            if (ok) { right++; }
            setTimeout(function () { i++; render(); }, ok ? 750 : 1200);
          });
          grid.appendChild(o);
        });
        s.appendChild(grid);
        say(String(n));
      }

      function finish() {
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.appendChild(el("div", "glyph", "\uD83D\uDC22"));
        card.appendChild(el("div", "name", right + " / " + rounds));
        var row = el("div", "row center");
        row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () { Games.count(); }));
        row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
        card.appendChild(row);
        s.innerHTML = "";
        s.appendChild(card);
        if (right >= rounds - 1) { H().celebrate("\uD83D\uDC22"); }
      }

      render();
    },

    /** Three of a kind and one stranger. */
    oddOne: function () {
      var groups = [
        H().findUnit("sa-haiwan"), H().findUnit("sa-tumbuhan"), H().findUnit("sa-buah"),
        H().findUnit("fiz-badan"), H().findUnit("hum-keluarga")
      ];
      var s = window.UI.clear();
      window.UI.crumb(T("Cari yang berbeza", "Odd one out"));
      var i = 0, right = 0, rounds = 6;

      function render() {
        if (i >= rounds) { return finish(); }
        var two = pickN(groups, 2);
        var same = pickN(H().allItems(two[0]), 3);
        var odd = pickN(H().allItems(two[1]), 1)[0];
        var opts = shuffle(same.concat([odd]));

        s.innerHTML = "";
        window.UI.title(s, "Cari yang berbeza", "Odd one out",
          "Mana yang lain daripada yang lain?", "Which one is different?");

        var grid = el("div", "option-grid");
        opts.forEach(function (o) {
          var t = el("div", "option");
          t.appendChild(H().glyphNode(o, two[0].kind, false));
          t.appendChild(el("div", null, o[window.Store.data.lang]));
          t.addEventListener("click", function () {
            if (t.dataset.done) { return; }
            t.dataset.done = "1";
            var ok = o === odd;
            window.Store.record(two[1].id, H().itemKey(odd, two[1].kind), ok);
            feedback(ok);
            t.classList.add(ok ? "correct" : "wrong");
            if (ok) { right++; }
            setTimeout(function () { i++; render(); }, ok ? 800 : 1300);
          });
          grid.appendChild(t);
        });
        s.appendChild(grid);
      }

      function finish() {
        var card = el("div", "bigcard");
        card.style.cursor = "default";
        card.appendChild(el("div", "glyph", "\uD83D\uDD0D"));
        card.appendChild(el("div", "name", right + " / " + rounds));
        var row = el("div", "row center");
        row.appendChild(window.UI.btn(T("Main lagi", "Play again"), "primary big", function () { Games.oddOne(); }));
        row.appendChild(window.UI.btn(T("Utama", "Home"), "big", function () { window.App.go(window.Views.home); }));
        card.appendChild(row);
        s.innerHTML = "";
        s.appendChild(card);
        if (right >= rounds - 1) { H().celebrate("\uD83D\uDD0D"); }
      }

      render();
    }
  };

  window.Games = Games;
  })();
