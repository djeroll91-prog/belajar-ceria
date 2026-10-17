/* The robot friend. Drawn as inline SVG so it needs no image file and can change
   expression instantly: the eyes blink on their own, and the whole robot reacts
   to right and wrong answers. */
(function () {
  "use strict";

  var MOODS = {
    happy: { mouth: "M 34 74 Q 50 90 66 74", eye: "#2E2440", bounce: true },
    cheer: { mouth: "M 32 72 Q 50 96 68 72", eye: "#2E2440", bounce: true },
    think: { mouth: "M 40 78 L 60 78", eye: "#2E2440", bounce: false },
    oops: { mouth: "M 36 84 Q 50 70 64 84", eye: "#2E2440", bounce: false }
  };

  function svg(mood) {
    var m = MOODS[mood] || MOODS.happy;
    return '' +
      '<svg viewBox="0 0 100 108" xmlns="http://www.w3.org/2000/svg">' +
        '<defs>' +
          '<linearGradient id="body" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0" stop-color="#8FD3F4"/><stop offset="1" stop-color="#5FA8D3"/>' +
          '</linearGradient>' +
          '<linearGradient id="head" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0" stop-color="#BDE3FB"/><stop offset="1" stop-color="#8FD3F4"/>' +
          '</linearGradient>' +
        '</defs>' +
        '<ellipse cx="50" cy="104" rx="26" ry="5" fill="rgba(0,0,0,.14)"/>' +
        '<line x1="50" y1="14" x2="50" y2="22" stroke="#7FB4D9" stroke-width="3"/>' +
        '<circle cx="50" cy="11" r="6" fill="#FFD166">' +
          '<animate attributeName="r" values="6;7.2;6" dur="1.6s" repeatCount="indefinite"/>' +
        '</circle>' +
        '<rect x="22" y="22" width="56" height="46" rx="16" fill="url(#head)" stroke="#4E7FA6" stroke-width="3"/>' +
        '<rect x="31" y="34" width="38" height="24" rx="11" fill="#22314A"/>' +
        '<circle class="eye-l" cx="42" cy="46" r="5.4" fill="' + m.eye + '">' +
          '<animate attributeName="ry" values="5.4;5.4;0.8;5.4" dur="4.2s" repeatCount="indefinite"/>' +
        '</circle>' +
        '<circle class="eye-r" cx="58" cy="46" r="5.4" fill="' + m.eye + '">' +
          '<animate attributeName="ry" values="5.4;5.4;0.8;5.4" dur="4.2s" repeatCount="indefinite"/>' +
        '</circle>' +
        '<circle cx="43.6" cy="44" r="1.7" fill="#fff" opacity=".9"/>' +
        '<circle cx="59.6" cy="44" r="1.7" fill="#fff" opacity=".9"/>' +
        '<path d="' + m.mouth + '" fill="none" stroke="#22314A" stroke-width="3" stroke-linecap="round"/>' +
        '<rect x="30" y="70" width="40" height="30" rx="12" fill="url(#body)" stroke="#4E7FA6" stroke-width="3"/>' +
        '<circle cx="42" cy="85" r="3.4" fill="#FF8A65"/>' +
        '<circle cx="58" cy="85" r="3.4" fill="#FFD166"/>' +
        '<circle cx="50" cy="92" r="2.6" fill="#7ED957"/>' +
        '<rect x="14" y="74" width="14" height="9" rx="4.5" fill="#8FD3F4" stroke="#4E7FA6" stroke-width="2.5"/>' +
        '<rect x="72" y="74" width="14" height="9" rx="4.5" fill="#8FD3F4" stroke="#4E7FA6" stroke-width="2.5"/>' +
      '</svg>';
  }

  var el = null, bubble = null, hideTimer = null;

  function mount() {
    el = document.getElementById("robot");
    bubble = document.getElementById("robot-bubble");
    if (!el) { return; }
    el.innerHTML = svg("happy");
    el.addEventListener("click", function () { Robot.say(Robot.greeting(), 2200); Robot.mood("cheer"); });
  }

  var Robot = {
    mount: mount,

    mood: function (name) {
      if (!el) { return; }
      el.innerHTML = svg(name);
      var cls = (MOODS[name] && MOODS[name].bounce) ? "bounce" : (name === "oops" ? "sad" : "");
      if (cls) {
        el.classList.remove("bounce", "sad");
        void el.offsetWidth;
        el.classList.add(cls);
        setTimeout(function () { el.classList.remove(cls); }, 700);
      }
    },

    /** Shows a short line above the robot. Speech is handled by the caller. */
    say: function (text, ms) {
      if (!bubble) { return; }
      bubble.textContent = text;
      bubble.classList.remove("hidden");
      clearTimeout(hideTimer);
      hideTimer = setTimeout(function () { bubble.classList.add("hidden"); }, ms || 2400);
    },

    hide: function () {
      if (bubble) { bubble.classList.add("hidden"); }
    },

    greeting: function () {
      return (window.App && App.lang === "en")
        ? "Hi friend! Let's learn together!"
        : "Hai kawan! Jom belajar sama-sama!";
    },

    praise: function () {
      var ms = ["Bagus sekali!", "Hebat!", "Pandai!", "Syabas!", "Betul!"];
      var en = ["Great job!", "Well done!", "Brilliant!", "You did it!", "Correct!"];
      var list = (window.App && App.lang === "en") ? en : ms;
      return list[Math.floor(Math.random() * list.length)];
    },

    encourage: function () {
      var ms = ["Cuba lagi ya", "Hampir betul!", "Jangan putus asa", "Cuba sekali lagi"];
      var en = ["Try again", "Almost!", "Keep going", "One more try"];
      var list = (window.App && App.lang === "en") ? en : ms;
      return list[Math.floor(Math.random() * list.length)];
    }
  };

  window.Robot = Robot;
  window.addEventListener("DOMContentLoaded", mount);
})();
