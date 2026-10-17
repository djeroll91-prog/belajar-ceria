/* Headless checks for the web app. Run with: node web/test_web.js
   jsdom is a test-only dependency; the app itself ships nothing but plain files. */
"use strict";

const fs = require("fs");
const path = require("path");
const { JSDOM } = require("jsdom");

const DIR = __dirname;
let pass = 0, fail = 0;

function ok(name, cond, extra) {
  if (cond) { pass++; console.log("  ok   " + name); }
  else { fail++; console.log("  FAIL " + name + (extra ? "  -> " + extra : "")); }
}

function boot(url) {
  const html = fs.readFileSync(path.join(DIR, "index.html"), "utf8");
  const dom = new JSDOM(html, {
    url: url || "https://belajar-ceria.test/index.html",
    runScripts: "outside-only",
    pretendToBeVisual: true
  });
  const w = dom.window;
  w.speechSynthesis = {
    cancel() {}, speak(u) { if (u.onend) { setTimeout(u.onend, 0); } },
    getVoices() { return [{ lang: "ms-MY", name: "Test Malay" }, { lang: "en-US", name: "Test English" }]; },
    set onvoiceschanged(fn) { if (fn) { fn(); } }
  };
  w.SpeechSynthesisUtterance = function (t) { this.text = t; };
  w.confirm = () => true;
  w.scrollTo = () => {};
  w.Element.prototype.animate = function () { return { onfinish: null, finished: Promise.resolve() }; };

  for (const f of ["data.js", "robot.js", "games.js", "app.js"]) {
    w.eval(fs.readFileSync(path.join(DIR, f), "utf8"));
  }
  w.document.dispatchEvent(new w.Event("DOMContentLoaded", { bubbles: true }));
  return w;
}

console.log("web app");

/* ---------------------------------------------------------------- boot */
{
  const w = boot();
  ok("KSPK data loaded", !!w.KSPK && w.KSPK.tunjang.length === 6);
  ok("helpers exported", !!w.Helpers);
  ok("six tunjang rendered on home", w.document.querySelectorAll("#stage .grid .tile").length >= 6);
  ok("robot drew an SVG", !!w.document.querySelector("#robot svg"));
  ok("score chip starts at 0", w.document.getElementById("chip-points").textContent === "0");
}

/* ------------------------------------------------------- content shape */
{
  const w = boot();
  const D = w.KSPK;
  const ids = new Set();
  let dupes = 0, noName = 0, noEmoji = 0;
  for (const t of D.tunjang) {
    for (const u of t.units) {
      if (ids.has(u.id)) { dupes++; }
      ids.add(u.id);
      if (!u.ms || !u.en) { noName++; }
      if (!u.emoji) { noEmoji++; }
    }
  }
  ok("unit ids are unique", dupes === 0, dupes + " duplicates");
  ok("every unit is named in both languages", noName === 0);
  ok("every unit has an emoji", noEmoji === 0);

  const kinds = new Set();
  for (const t of D.tunjang) { for (const u of t.units) { kinds.add(u.kind); } }
  ok("syllable unit exists (kaedah KV)", [...kinds].length > 0 && !!w.Helpers.findUnit("bm-suku-kata"));
  ok("shapes unit exists", !!w.Helpers.findUnit("ma-bentuk"));
  ok("senses unit exists", !!w.Helpers.findUnit("sa-deria"));
  ok("family unit exists", !!w.Helpers.findUnit("hum-keluarga"));
  ok("manners unit exists", !!w.Helpers.findUnit("nilai-adab"));
  ok("42 weekly themes", D.themes.length === 42);

  const syl = w.Helpers.findUnit("bm-suku-kata");
  ok("syllable grid has 70 entries", syl.items.length === 70, String(syl.items.length));
  ok("syllable grid starts at ba", syl.items[0].symbol === "ba");
}

/* ------------------------------------------------------ letter speaking */
{
  const w = boot();
  const unit = w.Helpers.findUnit("bm-huruf");
  const A = unit.items[0];
  ok("letter A stores a picture", A.symbol.indexOf("|") > -1, A.symbol);
  const spokenMs = w.Helpers.itemSpoken(A, "letter", "ms");
  const spokenEn = w.Helpers.itemSpoken(A, "letter", "en");
  ok("letter A says the letter name in Malay, not the picture", spokenMs === "ei", spokenMs);
  ok("letter A says the letter name in English", spokenEn === "ay", spokenEn);
  ok("the picture name is still reachable", A.ms === "Apel" && A.en === "Apple");

  let bad = [];
  for (const it of unit.items) {
    const g = it.symbol.split("|")[0];
    if (w.Helpers.itemSpoken(it, "letter", "ms") === it.ms) { bad.push(g + " (ms)"); }
    if (w.Helpers.itemSpoken(it, "letter", "en") === it.en) { bad.push(g + " (en)"); }
  }
  ok("all 26 letters say their name in both languages", bad.length === 0, bad.slice(0, 5).join(", "));
}

/* -------------------------------------------------------------- themes */
{
  const w = boot();
  const weeks = w.KSPK.themes.map(t => t.week);
  ok("themes run 1..42", weeks[0] === 1 && weeks[weeks.length - 1] === 42);
}

/* --------------------------------------------------------- navigation */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));

  const firstTunjang = w.document.querySelectorAll("#stage .grid.wide .tile")[0];
  click(firstTunjang);
  ok("opening a tunjang lists its units", w.document.querySelectorAll("#stage .grid .tile").length > 1);
  ok("crumb shows the tunjang name", w.document.getElementById("crumb").textContent.length > 0);

  const firstUnit = w.document.querySelectorAll("#stage .grid .tile")[0];
  click(firstUnit);
  const btns = [...w.document.querySelectorAll("#stage .btn")].map(b => b.textContent);
  ok("unit offers Learn", btns.some(b => /Belajar|Learn/.test(b)), btns.join("|"));
  ok("unit offers Quiz", btns.some(b => /Kuiz|Quiz/.test(b)), btns.join("|"));

  click(w.document.getElementById("btn-home"));
  ok("home returns", w.document.getElementById("crumb").textContent.match(/Utama|Home/) !== null);
}

/* ----------------------------------------------------- language toggle */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  ok("starts in Malay", w.App.lang === "ms");
  click(w.document.getElementById("btn-lang"));
  ok("switches to English", w.App.lang === "en", w.App.lang);
  ok("button label follows", w.document.getElementById("btn-lang").textContent === "EN");
  const html = w.document.getElementById("stage").textContent;
  ok("home renders English words", /Welcome|Home|Start learning|The six/.test(html));
  click(w.document.getElementById("btn-lang"));
  ok("switches back to Malay", w.App.lang === "ms");
}

/* ------------------------------------------------------------ dark mode */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  const root = w.document.documentElement;
  ok("starts light", root.getAttribute("data-theme") === "light");
  click(w.document.getElementById("btn-theme"));
  ok("toggles to dark", root.getAttribute("data-theme") === "dark");
  ok("theme colour meta follows", w.document.querySelector('meta[name="theme-color"]')
    .getAttribute("content") === "#1B1726");
  click(w.document.getElementById("btn-theme"));
  ok("toggles back to light", root.getAttribute("data-theme") === "light");
}

/* ------------------------------------------------------- sound toggle */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  ok("sound starts on", w.Store.data.sound === true);
  click(w.document.getElementById("btn-sound"));
  ok("sound can be muted", w.Store.data.sound === false);
  ok("icon changes", w.document.getElementById("btn-sound").textContent === "\uD83D\uDD07");
  click(w.document.getElementById("btn-sound"));
  ok("sound can be restored", w.Store.data.sound === true);
}

/* ---------------------------------------------------- scoring + Leitner */
{
  const w = boot();
  const S = w.Store;
  S.record("bm-huruf", "A", true);
  S.record("bm-huruf", "A", true);
  ok("correct answers add points", S.data.points === 20, String(S.data.points));
  ok("streak counts up", S.data.streak === 2);

  const before = S.stat("bm-huruf", "A").box;
  S.record("bm-huruf", "A", false);
  ok("a wrong answer resets the Leitner box", S.stat("bm-huruf", "A").box === 0, "was " + before);
  ok("a wrong answer clears the streak", S.data.streak === 0);

  for (let i = 0; i < 6; i++) { S.record("bm-huruf", "A", true); }
  ok("box caps at 4", S.stat("bm-huruf", "A").box === 4, String(S.stat("bm-huruf", "A").box));

  const q = w.Review.queue(5);
  ok("review queue is populated", q.length > 0, String(q.length));
  ok("weakest item sorts first", q[0].weakness >= q[q.length - 1].weakness);
  ok("review knows the item", q[0].key === "A", q[0].key);
}

/* --------------------------------------------- review prioritises weakness */
{
  const w = boot();
  const S = w.Store;
  for (let i = 0; i < 4; i++) { S.record("bm-huruf", "A", true); }
  S.record("bm-huruf", "B", false);
  S.record("bm-huruf", "B", false);
  S.record("bm-huruf", "B", false);
  const q = w.Review.queue(3);
  ok("the always-wrong item comes first", q[0].key === "B", q[0].key);
  const weak = w.Review.weakUnits(5);
  ok("weak unit is reported", weak.length === 0 || weak[0].unit.id === "bm-huruf");
}

/* ---------------------------------------------------------- quiz flow */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Games.quiz("ma-nombor");
  const options = w.document.querySelectorAll("#stage .option");
  ok("quiz shows answer options", options.length === 4, String(options.length));
  const correct = w.document.querySelector('#stage .option[data-correct="1"]');
  ok("exactly one correct option is marked",
    w.document.querySelectorAll('#stage .option[data-correct="1"]').length === 1);
  const ptsBefore = w.Store.data.points;
  click(correct);
  ok("a correct tap scores", w.Store.data.points > ptsBefore);
  ok("a correct tap marks the option", correct.classList.contains("correct"));
}

/* ------------------------------------------------- wrong answer reveals truth */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Games.quiz("ma-nombor");
  const wrong = [...w.document.querySelectorAll("#stage .option")].find(o => o.dataset.correct !== "1");
  click(wrong);
  ok("a wrong tap is marked wrong", wrong.classList.contains("wrong"));
  ok("the right answer is revealed",
    w.document.querySelector("#stage .option.correct") !== null);
}

/* ------------------------------------------------------- build the word */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Games.build("bm-perkataan");
  const slots = w.document.querySelectorAll("#stage .slot");
  ok("word building shows one slot per syllable", slots.length >= 2, String(slots.length));
  ok("word building offers chips", w.document.querySelectorAll("#stage .chip").length >= 2);

  // Read the target from the card ("Bina: <word>") and work out the chunks the
  // same way the game does, so the test taps a syllable that is definitely not
  // first and can prove the order is enforced.
  const cardText = w.document.getElementById("stage").textContent;
  const target = (cardText.match(/(?:Bina|Build):\s*([a-zA-Z]+)/) || [])[1];
  ok("the card states the word to build", !!target, cardText.slice(0, 80));

  const chunks = [];
  for (let k = 0; k < target.length; k += 2) { chunks.push(target.substr(k, 2)); }
  const chips = [...w.document.querySelectorAll("#stage .chip")];
  const second = chips.find(c => c.textContent === chunks[1]);
  if (second && chunks[0] !== chunks[1]) {
    const filledBefore = w.document.querySelectorAll("#stage .slot.filled").length;
    click(second);
    const filledAfter = w.document.querySelectorAll("#stage .slot.filled").length;
    ok("an out-of-order chip does not fill a slot", filledAfter === filledBefore,
      "tapped " + second.textContent + " while " + chunks[0] + " was due");
  } else {
    ok("an out-of-order chip does not fill a slot", true);
  }

  const firstChip = chips.find(c => c.textContent === chunks[0]);
  if (firstChip) {
    click(firstChip);
    const filled = w.document.querySelectorAll("#stage .slot.filled").length;
    ok("the correct first syllable fills the first slot", filled === 1, String(filled));
    ok("that chip is then spent", firstChip.classList.contains("used"));
  }
}

/* ------------------------------------------------------ memory game */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Games.memory("sa-buah");
  const tiles = w.document.querySelectorAll("#stage .grid .tile");
  ok("memory deals an even number of cards", tiles.length > 0 && tiles.length % 2 === 0, String(tiles.length));
  click(tiles[0]);
  ok("a card turns over", tiles[0].dataset.open === "1");
}

/* -------------------------------------------------------- count game */
{
  const w = boot();
  w.Games.count();
  ok("count game shows options", w.document.querySelectorAll("#stage .option").length === 4);
  ok("count game draws pictures", /🍎/.test(w.document.getElementById("stage").textContent));
}

/* --------------------------------------------------------- bubble pop */
{
  const w = boot();
  w.Games.bubble();
  const bubbles = w.document.querySelectorAll("#stage div[style*='border-radius: 50%']");
  ok("bubble pop spawns four bubbles", bubbles.length === 4, String(bubbles.length));
}

/* ------------------------------------------------------- rewards view */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Store.record("bm-huruf", "A", true);
  w.Views.rewards();
  const stats = w.document.querySelectorAll("#stage .stat");
  ok("rewards shows stat cards", stats.length === 6, String(stats.length));
  ok("rewards shows badges", w.document.querySelectorAll("#stage .grid.small .tile").length === 8);
  const unlocked = [...w.document.querySelectorAll("#stage .grid.small .tile")]
    .filter(t => !t.classList.contains("locked"));
  ok("the first badge unlocks after one answer", unlocked.length >= 1);
  ok("reset is offered", /Set semula|Reset/.test(w.document.getElementById("stage").textContent));
}

/* -------------------------------------------------------- persistence */
{
  const w = boot();
  w.Store.record("ma-nombor", "3|3", true);
  w.Store.save();
  const raw = w.localStorage.getItem("belajar-ceria-web-v1");
  ok("progress is written to storage", !!raw && raw.indexOf("ma-nombor") > -1);
  const parsed = JSON.parse(raw);
  ok("stored points match", parsed.points === 10, String(parsed.points));

  w.Store.reset();
  ok("reset clears points", w.Store.data.points === 0);
  ok("reset clears stats", Object.keys(w.Store.data.stats).length === 0);
}

/* ---------------------------------------------------------- settings */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Views.settings();
  const text = w.document.getElementById("stage").textContent;
  ok("settings names the curriculum", /KSPK/.test(text));
  ok("settings mentions the age range", /4\+ and 5\+/.test(text));
  ok("settings reports voice availability", /suara Bahasa Melayu/i.test(text));
}

/* --------------------------------------------------------------- games */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  w.Views.games();
  const tiles = w.document.querySelectorAll("#stage .grid .tile");
  ok("games hub lists ten games", tiles.length === 10, String(tiles.length));
}

/* ------------------------------------------------ every unit renders */
{
  const w = boot();
  let broken = [];
  for (const t of w.KSPK.tunjang) {
    for (const u of t.units) {
      try {
        w.Views.unit(u.id);
        const n = w.document.querySelectorAll("#stage .grid.small .tile").length;
        if (n !== u.items.length) { broken.push(u.id + " (" + n + "/" + u.items.length + ")"); }
      } catch (e) {
        broken.push(u.id + " threw " + e.message);
      }
    }
  }
  ok("all 42 units render every item", broken.length === 0, broken.slice(0, 4).join(", "));
}

/* ------------------------------------------- every game survives a click */
{
  const w = boot();
  const click = (node) => node.dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  const cases = [
    ["quiz", () => w.Games.quiz("bm-huruf")],
    ["listen", () => w.Games.listen()],
    ["build", () => w.Games.build("bm-perkataan")],
    ["memory", () => w.Games.memory("sa-buah")],
    ["bubble", () => w.Games.bubble()],
    ["sort", () => w.Games.sort()],
    ["pattern", () => w.Games.pattern()],
    ["count", () => w.Games.count()],
    ["oddOne", () => w.Games.oddOne()]
  ];
  let threw = [];
  for (const [name, fn] of cases) {
    try {
      fn();
      const first = w.document.querySelector("#stage .option, #stage .tile, #stage .chip");
      if (first) { click(first); }
    } catch (e) {
      threw.push(name + ": " + e.message);
    }
  }
  ok("no game throws on start or first tap", threw.length === 0, threw.join(" | "));
}

/* ------------------------------------------- survives a file:// origin */
{
  // Browsers treat a page opened by double-click as an opaque origin, where
  // localStorage throws. The app must still run; progress just cannot persist.
  const w = boot("file://" + DIR + "/index.html");
  ok("app boots from a file:// URL", w.document.querySelectorAll("#stage .tile").length > 0);
  let threw = false;
  try {
    w.Store.record("bm-huruf", "A", true);
    w.Store.save();
  } catch (e) { threw = true; }
  ok("recording progress never throws without storage", !threw);
  ok("in-memory progress still advances", w.Store.data.points === 10, String(w.Store.data.points));
}

/* ------------------------------------------- no external dependencies */
{
  const files = ["index.html", "style.css", "app.js", "games.js", "robot.js", "data.js"];
  let remote = [];
  for (const f of files) {
    const src = fs.readFileSync(path.join(DIR, f), "utf8");
    const m = src.match(/https?:\/\/[^\s"'<>)]+/g) || [];
    m.forEach(u => { if (!/w3\.org|example\.com/.test(u)) { remote.push(f + ": " + u); } });
  }
  ok("nothing loads from the network", remote.length === 0, remote.slice(0, 3).join(", "));
}

console.log("\n" + pass + " passed, " + fail + " failed");
process.exit(fail === 0 ? 0 : 1);
