// Server dashboard: polls /api/* every 2 s and draws Overview, Players, Tribes and the live map. No libraries.
"use strict";

const $ = id => document.getElementById(id);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
const cap = s => s ? s[0].toUpperCase() + s.slice(1) : "";
const fmt = (n, d = 0) => n == null || isNaN(n) ? "—" : Number(n).toLocaleString("en-GB", { minimumFractionDigits: d, maximumFractionDigits: d });
const OVERWORLD = "minecraft:overworld";
const SEASONS = ["spring", "summer", "autumn", "winter"];
const MONTHS = ["thaw", "sowing", "blossom", "midsun", "haymonth", "reaping", "leaffall", "hunting", "frostfall", "deepwinter", "wolfmoon", "snowmelt"];
const HOUR = 3600e3;

const data = { status: {}, players: [], tribes: [], map: {} };
const history = []; // {t, tps, online}, last hour, page memory only

/* ---------- tabs ---------- */
const TABS = ["overview", "players", "tribes", "map"];
function showTab() {
  const tab = TABS.includes(location.hash.slice(1)) ? location.hash.slice(1) : "overview";
  for (const t of TABS) $(t).classList.toggle("open", t === tab);
  document.querySelectorAll(".tabs a").forEach(a => a.dataset.tab === tab ? a.setAttribute("aria-current", "page") : a.removeAttribute("aria-current"));
  if (tab === "map") map.resize();
}
window.addEventListener("hashchange", showTab);
/** Shows the map at once (hashchange fires later), so the canvas has its size before we centre or fit it. */
function openMap() {
  location.hash = "#map";
  showTab();
}

/* ---------- polling ---------- */
async function get(path) {
  const r = await fetch(path, { cache: "no-store" });
  if (!r.ok) throw new Error(path + " " + r.status);
  return r.json();
}

async function poll() {
  try {
    const [status, players, tribes, mapData] = await Promise.all(["/api/status", "/api/players", "/api/tribes", "/api/map"].map(get));
    // Right after start the server answers {} / [] until its first snapshot: keep the shapes the page expects.
    data.status = status && typeof status === "object" ? status : {};
    data.players = Array.isArray(players) ? players : [];
    data.tribes = Array.isArray(tribes) ? tribes : [];
    data.map = mapData && typeof mapData === "object" ? mapData : {};
    if (data.status.tps != null) {
      const now = Date.now();
      history.push({ t: now, tps: data.status.tps, online: data.status.online });
      while (history.length && history[0].t < now - HOUR) history.shift();
    }
    setLive(true, data.status.tps == null ? "Waiting for the first numbers" : "Live");
    render();
  } catch (e) {
    setLive(false, "No answer from the server");
  }
}

function setLive(ok, text) {
  $("live").className = "live " + (ok ? "ok" : "bad");
  $("live-text").textContent = text;
}

function render() {
  renderHeader();
  renderOverview();
  renderPlayers();
  renderTribes();
  map.setData(data.map);
}

/* ---------- header and strip ---------- */
const tpsClass = t => t == null ? "" : t >= 19 ? "good" : t >= 15 ? "warn" : "bad";
const gb = mb => mb == null ? "—" : (mb / 1024).toFixed(1);

function renderHeader() {
  const s = data.status;
  if (s.name) $("mod-name").textContent = s.name;
  $("mod-version").textContent = s.version ? "v" + s.version : "";
  if (s.name) document.title = s.name + " server";
  const parts = [
    ["TPS", `<b class="${tpsClass(s.tps)}">${fmt(s.tps, 1)}</b>`],
    ["MSPT", `<b>${fmt(s.mspt, 1)}</b>`],
    ["Players", `<b>${fmt(s.online)}/${fmt(s.max)}</b>`],
    ["Memory", `<b>${gb(s.memUsedMb)}/${gb(s.memMaxMb)} GB</b>`],
    ["Day", `<b>${fmt(s.day)} ${esc(cap(s.season))}</b>`],
  ];
  $("strip").innerHTML = parts.map(([k, v]) => `<span>${k}${v}</span>`).join("");
}

/* ---------- overview ---------- */
function uptime(sec) {
  if (sec == null) return "—";
  const d = Math.floor(sec / 86400), h = Math.floor(sec % 86400 / 3600), m = Math.floor(sec % 3600 / 60);
  return d ? `${d}<small>d</small> ${h}<small>h</small>` : h ? `${h}<small>h</small> ${m}<small>min</small>` : `${m}<small>min</small>`;
}

function renderOverview() {
  const s = data.status;
  const mem = s.memMaxMb ? Math.min(100, s.memUsedMb / s.memMaxMb * 100) : 0;
  const tiles = [
    { label: "Ticks per second", value: fmt(s.tps, 1), cls: tpsClass(s.tps), hint: "20 is full speed" },
    { label: "Milliseconds per tick", value: fmt(s.mspt, 1), cls: s.mspt == null ? "" : s.mspt <= 40 ? "" : s.mspt <= 50 ? "warn" : "bad", hint: "Average; 50 ms is the limit" },
    { label: "Players online", value: `${fmt(s.online)}<small>of ${fmt(s.max)}</small>` },
    { label: "Memory", value: `${gb(s.memUsedMb)}<small>of ${gb(s.memMaxMb)} GB</small>`, bar: mem },
    { label: "Loaded chunks", value: fmt(s.loadedChunks), hint: "All dimensions" },
    { label: "Entities", value: fmt(s.entities), hint: "Mobs, items and more" },
    { label: "World age", value: s.worldAge ? esc(cap(s.worldAge)) + " Age" : "—", hint: "The furthest any tribe has come" },
    { label: "Uptime", value: uptime(s.uptimeSec), hint: "Since the server started" },
  ];
  $("stats").innerHTML = tiles.map(t => `<article class="card stat"><p class="label">${t.label}</p><p class="value ${t.cls || ""}">${t.value}</p>` +
    (t.bar != null ? `<div class="bar"><i style="width:${t.bar.toFixed(1)}%"></i></div>` : t.hint ? `<p class="hint">${t.hint}</p>` : "") + `</article>`).join("");

  const mi = MONTHS.indexOf(s.month);
  if (mi >= 0 && s.dayOfMonth) {
    $("date").textContent = `${s.dayOfMonth} ${cap(s.month)}, year ${s.year}`;
    $("date-sub").textContent = `${cap(s.season)}, ${s.time}. Day ${fmt(s.day)} of the world.`;
  }
  drawWheel(mi >= 0 ? mi * 30 + (s.dayOfMonth || 1) - 1 : null, s);

  const tpsHist = history.map(h => h.tps);
  spark($("tps-spark"), "tps", 20, "#93b06c", 20);
  $("tps-note").textContent = tpsHist.length ? `Now ${fmt(s.tps, 1)}, lowest ${fmt(Math.min(...tpsHist), 1)}` : "";
  const most = Math.max(0, ...history.map(h => h.online || 0));
  spark($("pl-spark"), "online", Math.max(1, most), "#d6a44e");
  $("pl-note").textContent = history.length ? `Now ${fmt(s.online)}, most ${fmt(most)}` : "";
}

const SEASON_COLOURS = ["#93b06c", "#d6a44e", "#c26a3c", "#9fb4bd"];
function arc(r, a0, a1, reverse) {
  const p = a => [Math.cos(a) * r, Math.sin(a) * r].map(v => v.toFixed(2)).join(" ");
  return reverse ? `M${p(a1)} A${r} ${r} 0 0 0 ${p(a0)}` : `M${p(a0)} A${r} ${r} 0 0 1 ${p(a1)}`;
}

/** The year as a wheel: four season bands starting at the top with Thaw, month ticks, and the current day marked. */
function drawWheel(doy, s) {
  const ang = d => d / 360 * Math.PI * 2 - Math.PI / 2;
  const cur = doy == null ? -1 : Math.floor(doy / 90);
  let out = `<defs>`;
  for (let i = 0; i < 4; i++) {
    const bottom = i === 1 || i === 2; // text on the lower half runs the other way so it stays upright
    out += `<path id="sp${i}" d="${arc(bottom ? 103 : 97, ang(i * 90 + 2), ang(i * 90 + 88), bottom)}"/>`;
  }
  out += `</defs>`;
  for (let i = 0; i < 4; i++) {
    const op = i === cur ? 1 : .35;
    out += `<path d="${arc(96, ang(i * 90 + .6), ang(i * 90 + 89.4))}" stroke="${SEASON_COLOURS[i]}" stroke-width="22" fill="none" opacity="${op}"/>`;
  }
  for (let m = 0; m < 12; m++) {
    const a = ang(m * 30), c = Math.cos(a), si = Math.sin(a), r0 = m % 3 ? 82 : 78;
    out += `<line x1="${(c * r0).toFixed(1)}" y1="${(si * r0).toFixed(1)}" x2="${(c * 72).toFixed(1)}" y2="${(si * 72).toFixed(1)}" stroke="#a8997f" stroke-width="${m % 3 ? 1 : 1.6}" opacity=".7"/>`;
  }
  for (let i = 0; i < 4; i++)
    out += `<text class="wheel-season" fill-opacity="${i === cur ? 1 : .7}" style="fill:${i === cur ? "#1d1813" : "#ece2cf"}"><textPath href="#sp${i}" startOffset="50%" text-anchor="middle">${cap(SEASONS[i])}</textPath></text>`;
  if (doy != null) {
    // The hand stops inside the ring so it never covers a season name; an ember notch outside marks the same day.
    const deg = doy + .5, a = ang(deg), c = Math.cos(a), si = Math.sin(a);
    out += `<line x1="${(c * 42).toFixed(1)}" y1="${(si * 42).toFixed(1)}" x2="${(c * 70).toFixed(1)}" y2="${(si * 70).toFixed(1)}" stroke="#ece2cf" stroke-width="2.5" stroke-linecap="round"/>`;
    out += `<path d="M0 -107 L-6 -117 L6 -117Z" fill="#e2683c" stroke="#1d1813" stroke-width="1.5" stroke-linejoin="round" transform="rotate(${deg})"/>`;
    out += `<text class="wheel-centre" text-anchor="middle" y="6">${esc(s.dayOfMonth)}</text>`;
    out += `<text class="wheel-centre-sub" text-anchor="middle" y="26">${esc(cap(s.month))}</text>`;
  }
  $("wheel").innerHTML = out;
  $("wheel").setAttribute("aria-label", doy == null ? "The year" : `Day ${doy + 1} of 360, ${s.season}`);
}

/** Sparkline over the last hour: x is time, so gaps (page closed, server down) stay visible as straight runs. */
function spark(svg, key, top, colour, ref) {
  const now = Date.now(), W = 300, H = 70, pad = 4;
  const x = t => (W * (t - (now - HOUR)) / HOUR).toFixed(1);
  const y = v => (H - pad - (H - 2 * pad) * Math.min(1, (v || 0) / top)).toFixed(1);
  let out = `<defs><linearGradient id="g-${key}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${colour}" stop-opacity=".35"/><stop offset="1" stop-color="${colour}" stop-opacity="0"/></linearGradient></defs>`;
  out += `<line x1="0" x2="${W}" y1="${H - pad}" y2="${H - pad}" stroke="#4a3e31" stroke-width="1" vector-effect="non-scaling-stroke"/>`;
  if (ref) out += `<line x1="0" x2="${W}" y1="${y(ref)}" y2="${y(ref)}" stroke="#a8997f" stroke-dasharray="3 4" stroke-width="1" opacity=".5" vector-effect="non-scaling-stroke"/>`;
  if (history.length) {
    const pts = history.map(h => `${x(h.t)},${y(h[key])}`);
    if (history.length === 1) pts.push(`${W},${y(history[0][key])}`);
    out += `<path d="M${pts[0].split(",")[0]},${H - pad} L${pts.join(" L")} L${pts[pts.length - 1].split(",")[0]},${H - pad}Z" fill="url(#g-${key})"/>`;
    out += `<polyline points="${pts.join(" ")}" fill="none" stroke="${colour}" stroke-width="2" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>`;
  }
  svg.innerHTML = out;
}

/* ---------- players ---------- */
const tribeByName = name => data.tribes.find(t => t.name === name);

function meter(kind, v, max) {
  if (v == null) return `<span class="dim">—</span>`;
  return `<span class="meter ${kind}"><span class="bar"><i style="width:${Math.max(0, Math.min(100, v / max * 100)).toFixed(0)}%"></i></span>${fmt(v, v % 1 ? 1 : 0)}</span>`;
}

function renderPlayers() {
  const rows = [...data.players].sort((a, b) => (b.online - a.online) || a.name.localeCompare(b.name));
  $("players-empty").hidden = rows.length > 0;
  $("player-rows").innerHTML = rows.map(p => {
    const t = p.tribe ? tribeByName(p.tribe) : null;
    const onMap = p.online && p.dim === OVERWORLD;
    const dim = p.online && p.dim !== OVERWORLD ? `<span class="dim-tag">${esc(cap((p.dim || "").replace(/^minecraft:/, "").replace(/_/g, " ")))}</span>` : "";
    return `<tr class="${onMap ? "go" : ""} ${p.online ? "" : "away"}" ${onMap ? `tabindex="0" data-x="${p.x}" data-z="${p.z}" title="Show on the map"` : ""}>
      <td><span class="who"><span class="pip ${p.online ? "on" : ""}" aria-label="${p.online ? "online" : "offline"}"></span>${esc(p.name)}</span></td>
      <td>${p.tribe ? `<span class="swatch" style="background:${esc(t ? t.colour : "#a8997f")}"></span>${esc(p.tribe)}` : `<span class="dim">No tribe</span>`}</td>
      <td>${p.online ? `${fmt(p.x)}, ${fmt(p.y)}, ${fmt(p.z)}${dim}` : "—"}</td>
      <td>${p.online ? meter("health", p.health, 20) : "—"}</td>
      <td>${p.online ? meter("food", p.food, 20) : "—"}</td>
      <td class="num">${p.online ? fmt(p.ping) + " ms" : "—"}</td>
      <td>${p.online ? "Now" : p.lastSeenDay != null ? "Day " + fmt(p.lastSeenDay) : "—"}</td></tr>`;
  }).join("");
}

function goPlayer(el) {
  const tr = el.closest("tr.go");
  if (!tr) return;
  openMap();
  map.centre(+tr.dataset.x, +tr.dataset.z, 4);
}
$("player-rows").addEventListener("click", e => goPlayer(e.target));
$("player-rows").addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); goPlayer(e.target); } });

/* ---------- tribes ---------- */
function renderTribes() {
  const tribes = data.tribes;
  $("tribes-empty").hidden = tribes.length > 0;
  $("tribe-cards").innerHTML = tribes.map(t => {
    const online = t.members.filter(m => m.online).length;
    const pit = t.pit ? `${fmt(t.pit.x)}, ${fmt(t.pit.y)}, ${fmt(t.pit.z)}` + (t.pit.dim !== OVERWORLD ? ` <span class="dim-tag">${esc(t.pit.dim)}</span>` : "") : "None yet";
    const rep = Object.entries(t.reputation || {});
    return `<article class="card tribe" style="--tc:${esc(t.colour)}" tabindex="0" role="button" data-id="${esc(t.id)}" aria-label="${esc(t.name)}, show on the map">
      <div class="tribe-head"><span class="banner"></span><div class="tribe-name"><h3>${esc(t.name)}</h3>
        <p>${esc(cap(t.age))} Age, ${t.members.length} ${t.members.length === 1 ? "member" : "members"}${online ? `, ${online} online` : ""}</p></div></div>
      <dl class="facts">
        <div><dt>Fire Pit</dt><dd>${pit}</dd></div>
        <div><dt>Pit size</dt><dd>${t.pit ? (t.big ? "Big" : "Regular") : "—"}</dd></div>
        <div><dt>Territory</dt><dd>${fmt(t.chunks)} ${t.chunks === 1 ? "chunk" : "chunks"}</dd></div>
      </dl>
      <ul class="members">${t.members.map(m => `<li><span class="pip ${m.online ? "on" : ""}" aria-label="${m.online ? "online" : "offline"}"></span>${esc(m.name)}<span class="rank">${esc(cap(m.rank))}</span></li>`).join("")}</ul>
      ${rep.length ? `<p class="rep">Standing with ${rep.map(([k, v]) => `<b>${esc(k)}</b> <span class="${v > 0 ? "pos" : v < 0 ? "neg" : ""}">${v > 0 ? "+" : ""}${fmt(v)}</span>`).join(", ")}</p>` : ""}
      <p class="show">Show on map</p>
    </article>`;
  }).join("");
}

function goTribe(el) {
  const card = el.closest(".tribe");
  if (!card) return;
  openMap();
  map.fitTribe(card.dataset.id);
}
$("tribe-cards").addEventListener("click", e => goTribe(e.target));
$("tribe-cards").addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); goTribe(e.target); } });

/* ---------- map ---------- */
const map = (() => {
  const canvas = $("map-canvas"), ctx = canvas.getContext("2d");
  const hit = document.createElement("canvas").getContext("2d"); // identity transform, for point-in-territory tests in block space
  const REGION = 512, MIN = .25, MAX = 8, RELOAD = 10e3, RETRY = 30e3;
  const HALO = "rgba(18, 13, 9, .9)";
  let w = 0, h = 0, dpr = 1;
  let cx = 0, cz = 0, s = 1, target = 1, anchor = null; // centre block, pixels per block, zoom goal, {sx, sy, bx, bz}
  let mapData = {}, paths = new Map(), touched = false, dirty = false, mouse = null;
  const tiles = new Map(); // url -> {img, at, missingAt, loading}

  function resize() {
    const r = canvas.getBoundingClientRect();
    if (!r.width) return;
    dpr = window.devicePixelRatio || 1;
    w = r.width; h = r.height;
    canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
    if (!touched) fitAll();
    redraw();
  }
  window.addEventListener("resize", resize);

  const toScreen = (bx, bz) => [(bx - cx) * s + w / 2, (bz - cz) * s + h / 2];
  const toBlock = (sx, sy) => [(sx - w / 2) / s + cx, (sy - h / 2) / s + cz];
  const redraw = () => { if (!dirty) { dirty = true; requestAnimationFrame(frame); } };

  function setData(d) {
    mapData = d || {};
    paths = new Map();
    for (const t of mapData.tribes || []) {
      const p = new Path2D();
      for (const r of t.rings) {
        p.moveTo(r[0], r[1]);
        for (let i = 2; i < r.length; i += 2) p.lineTo(r[i], r[i + 1]);
        p.closePath();
      }
      paths.set(t.id, p);
    }
    renderLegend();
    if (!touched && w) fitAll();
    redraw();
  }

  function bounds(list) {
    let x0 = Infinity, z0 = Infinity, x1 = -Infinity, z1 = -Infinity;
    for (const t of list) for (const r of t.rings) for (let i = 0; i < r.length; i += 2) {
      x0 = Math.min(x0, r[i]); x1 = Math.max(x1, r[i]); z0 = Math.min(z0, r[i + 1]); z1 = Math.max(z1, r[i + 1]);
    }
    return x0 === Infinity ? null : [x0, z0, x1, z1];
  }

  function fit(b) {
    if (!b || !w) return;
    cx = (b[0] + b[2]) / 2; cz = (b[1] + b[3]) / 2;
    s = target = clamp(Math.min(w / (b[2] - b[0] + 96), h / (b[3] - b[1] + 96)));
    anchor = null;
    redraw();
  }
  const clamp = v => Math.max(MIN, Math.min(MAX, v));

  function fitAll() {
    const b = bounds(mapData.tribes || []);
    if (b) fit(b);
    else if ((mapData.players || []).length) { cx = mapData.players[0].x; cz = mapData.players[0].z; }
  }

  function fitTribe(id) {
    touched = true;
    resize();
    const t = (mapData.tribes || []).find(t => t.id === id);
    if (t) return fit(bounds([t]));
    const pit = (data.tribes.find(t => t.id === id) || {}).pit;
    if (pit) centre(pit.x, pit.z, 2);
  }

  function centre(x, z, zoom) {
    touched = true;
    resize();
    cx = x; cz = z;
    s = target = clamp(Math.max(s, zoom || 1));
    anchor = null;
    redraw();
  }

  /* tiles: one image per region, reloaded at most every 10 s while visible; a 404 is remembered for 30 s */
  function tile(rx, rz) {
    const url = `/tiles/${rx}.${rz}.png`, now = Date.now();
    let t = tiles.get(url);
    if (!t) tiles.set(url, t = { img: null, at: 0, missingAt: 0, loading: false });
    const stale = t.img ? now - t.at > RELOAD : t.missingAt ? now - t.missingAt > RETRY : true;
    if (stale && !t.loading) {
      t.loading = true;
      const img = new Image();
      img.onload = () => { t.img = img; t.at = Date.now(); t.missingAt = 0; t.loading = false; redraw(); };
      img.onerror = () => { if (!t.img) t.missingAt = Date.now(); else t.at = Date.now(); t.loading = false; };
      img.src = url + "?v=" + now; // path is what the server matches; the query only defeats the browser's image cache
    }
    return t.img;
  }

  function frame() {
    dirty = false;
    // Smooth zoom: ease toward the goal in log space, keeping the block under the cursor still.
    if (Math.abs(Math.log(target / s)) > .002) {
      s = Math.exp(Math.log(s) + (Math.log(target) - Math.log(s)) * .28);
      redraw();
    } else s = target;
    if (anchor) { cx = anchor.bx - (anchor.sx - w / 2) / s; cz = anchor.bz - (anchor.sy - h / 2) / s; }
    draw();
  }

  function draw() {
    if (!w) return; // hidden tab: nothing to size against yet
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = "#15110d";
    ctx.fillRect(0, 0, w, h);
    const [bx0, bz0] = toBlock(0, 0), [bx1, bz1] = toBlock(w, h);

    ctx.imageSmoothingEnabled = s < 1; // crisp block pixels when zoomed in, smooth when shrunk
    for (let rx = Math.floor(bx0 / REGION); rx <= Math.floor(bx1 / REGION); rx++)
      for (let rz = Math.floor(bz0 / REGION); rz <= Math.floor(bz1 / REGION); rz++) {
        const img = tile(rx, rz);
        if (!img) continue;
        const [x0, y0] = toScreen(rx * REGION, rz * REGION), [x1, y1] = toScreen((rx + 1) * REGION, (rz + 1) * REGION);
        const X0 = Math.round(x0), Y0 = Math.round(y0); // whole pixels so neighbouring tiles meet without seams
        ctx.drawImage(img, X0, Y0, Math.round(x1) - X0, Math.round(y1) - Y0);
      }

    grid(bx0, bz0, bx1, bz1);
    territories();
    fires();
    players();
    labels();
    $("zoom-level").textContent = (s >= 1 ? s.toFixed(s % 1 ? 1 : 0) : s.toFixed(2)) + "×";
    readout();
  }

  function grid(bx0, bz0, bx1, bz1) {
    const step = s >= 3 ? 16 : REGION;
    ctx.beginPath();
    for (let x = Math.ceil(bx0 / step) * step; x <= bx1; x += step) { const X = Math.round(toScreen(x, 0)[0]) + .5; ctx.moveTo(X, 0); ctx.lineTo(X, h); }
    for (let z = Math.ceil(bz0 / step) * step; z <= bz1; z += step) { const Y = Math.round(toScreen(0, z)[1]) + .5; ctx.moveTo(0, Y); ctx.lineTo(w, Y); }
    ctx.strokeStyle = step === 16 ? "rgba(0, 0, 0, .13)" : "rgba(236, 226, 207, .05)";
    ctx.lineWidth = 1;
    ctx.stroke();
  }

  const blockSpace = () => ctx.setTransform(dpr * s, 0, 0, dpr * s, dpr * (w / 2 - cx * s), dpr * (h / 2 - cz * s));

  /* Each tribe: a light fill, then its edge drawn only inside its own land (clipped), so two tribes sharing a border
     each keep their full colour band with a dark seam between, however alike the colours are. */
  function territories() {
    for (const t of mapData.tribes || []) {
      const p = paths.get(t.id);
      ctx.save();
      blockSpace();
      ctx.globalAlpha = .25;
      ctx.fillStyle = t.colour;
      ctx.fill(p, "evenodd");
      ctx.globalAlpha = 1;
      ctx.clip(p, "evenodd");
      ctx.lineJoin = "miter";
      ctx.strokeStyle = HALO; ctx.lineWidth = 12 / s; ctx.stroke(p); // half of each width shows inside: 3.5 px colour, 2.5 px dark seam
      ctx.strokeStyle = t.colour; ctx.lineWidth = 7 / s; ctx.stroke(p);
      ctx.restore();
      ctx.save();
      blockSpace();
      ctx.strokeStyle = HALO; ctx.lineWidth = 1.5 / s; ctx.stroke(p); // outer hairline so pale colours hold against snow or sand
      ctx.restore();
    }
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function flame(x, y, r) {
    ctx.beginPath();
    ctx.moveTo(x, y - r * 1.25);
    ctx.bezierCurveTo(x + r * .35, y - r * .55, x + r * .9, y - r * .2, x + r * .8, y + r * .35);
    ctx.arc(x, y + r * .3, r * .8, 0, Math.PI);
    ctx.bezierCurveTo(x - r * .9, y - r * .1, x - r * .35, y - r * .5, x - r * .15, y - r * .9);
    ctx.bezierCurveTo(x, y - r * .7, x, y - r * 1, x, y - r * 1.25);
    ctx.closePath();
  }

  /* Campfire: a lone flame. Fire Pit: the flame in a ring of stones; a big pit is drawn larger still. Tribe pits get a ring in the tribe colour. */
  function fires() {
    const tribeColour = id => ((mapData.tribes || []).find(t => t.id === id) || data.tribes.find(t => t.id === id) || {}).colour;
    const zoomed = Math.min(1.4, Math.max(.8, Math.sqrt(s)));
    for (const f of mapData.fires || []) {
      const [x, y] = toScreen(f.x + .5, f.z + .5);
      if (x < -40 || y < -40 || x > w + 40 || y > h + 40) continue;
      const big = f.kind === "big_fire_pit", pit = big || f.kind === "fire_pit";
      const R = (big ? 15 : pit ? 11 : 6) * zoomed;
      if (pit) {
        const c = tribeColour(f.tribe);
        ctx.beginPath(); ctx.arc(x, y, R + 3, 0, Math.PI * 2);
        ctx.fillStyle = HALO; ctx.fill();
        if (c) { ctx.lineWidth = 3; ctx.strokeStyle = c; ctx.beginPath(); ctx.arc(x, y, R + 1.5, 0, Math.PI * 2); ctx.stroke(); }
        const n = big ? 10 : 8;
        for (let i = 0; i < n; i++) {
          const a = i / n * Math.PI * 2;
          ctx.beginPath(); ctx.arc(x + Math.cos(a) * R * .72, y + Math.sin(a) * R * .72, R * (big ? .2 : .23), 0, Math.PI * 2);
          ctx.fillStyle = "#a59c8d"; ctx.fill();
          ctx.lineWidth = 1; ctx.strokeStyle = "#3b332a"; ctx.stroke();
        }
        flame(x, y + R * .08, R * .42);
      } else {
        flame(x, y, R);
        ctx.lineWidth = 3.5; ctx.strokeStyle = HALO; ctx.stroke();
      }
      ctx.fillStyle = "#e2683c"; ctx.fill();
      flame(x, y + (pit ? R * .2 : R * .3), (pit ? R * .42 : R) * .5);
      ctx.fillStyle = "#f2c25a"; ctx.fill();
    }
  }

  function halo(text, x, y, font, fill, align = "left") {
    ctx.font = font; ctx.textAlign = align; ctx.textBaseline = "middle";
    ctx.lineJoin = "round"; ctx.lineWidth = 4.5; ctx.strokeStyle = HALO; ctx.strokeText(text, x, y);
    ctx.fillStyle = fill; ctx.fillText(text, x, y);
  }

  /* Players: dot in the tribe colour with a pointer for where they face (yaw 0 = south, 90 = west). */
  function players() {
    for (const p of mapData.players || []) {
      const [x, y] = toScreen(p.x, p.z);
      const c = (mapData.tribes || []).find(t => t.id === p.tribe)?.colour || "#ece2cf";
      const a = p.yaw * Math.PI / 180, dx = -Math.sin(a), dy = Math.cos(a);
      ctx.beginPath();
      ctx.moveTo(x + dx * 17, y + dy * 17);
      ctx.lineTo(x - dy * 6.5, y + dx * 6.5);
      ctx.lineTo(x + dy * 6.5, y - dx * 6.5);
      ctx.closePath();
      ctx.fillStyle = HALO; ctx.fill();
      ctx.beginPath();
      ctx.moveTo(x + dx * 14.5, y + dy * 14.5);
      ctx.lineTo(x - dy * 4.5, y + dx * 4.5);
      ctx.lineTo(x + dy * 4.5, y - dx * 4.5);
      ctx.closePath();
      ctx.fillStyle = "#ece2cf"; ctx.fill();
      ctx.beginPath(); ctx.arc(x, y, 6.5, 0, Math.PI * 2);
      ctx.fillStyle = c; ctx.fill();
      ctx.lineWidth = 2.5; ctx.strokeStyle = HALO; ctx.stroke();
      halo(p.name, x + 12, y - 12, "600 13px " + getComputedStyle(document.body).fontFamily, "#ece2cf");
    }
  }

  /* Tribe names last, on top of everything: a pennant in the tribe colour and the name with a dark halo. */
  function labels() {
    const serif = '600 16px "Palatino Linotype", "Book Antiqua", Palatino, Georgia, serif';
    for (const t of mapData.tribes || []) {
      if (!t.label) continue;
      let [x, y] = toScreen(t.label.x, t.label.z);
      // The label chunk is often the pit's own chunk: drop the name below the fire instead of over it.
      if ((mapData.fires || []).some(f => { const [fx, fy] = toScreen(f.x + .5, f.z + .5); return Math.abs(fx - x) < 90 && Math.abs(fy - y) < 22; })) y += 34;
      ctx.font = serif;
      const tw = ctx.measureText(t.name).width, left = x - (tw + 18) / 2;
      ctx.beginPath();
      ctx.moveTo(left, y - 8); ctx.lineTo(left + 11, y - 8); ctx.lineTo(left + 11, y + 8); ctx.lineTo(left + 5.5, y + 4.5); ctx.lineTo(left, y + 8); ctx.closePath();
      ctx.lineWidth = 3.5; ctx.lineJoin = "round"; ctx.strokeStyle = HALO; ctx.stroke();
      ctx.fillStyle = t.colour; ctx.fill();
      halo(t.name, left + 18, y, serif, "#f3ead8");
    }
  }

  function tribeAt(bx, bz) {
    for (const t of mapData.tribes || []) if (hit.isPointInPath(paths.get(t.id), bx, bz, "evenodd")) return t;
    return null;
  }

  function readout() {
    if (!mouse) return;
    const [bx, bz] = toBlock(mouse[0], mouse[1]).map(Math.floor);
    $("ro-pos").textContent = `X ${bx}   Z ${bz}`;
    const t = tribeAt(bx + .5, bz + .5);
    $("ro-land").innerHTML = t ? `<span class="swatch" style="background:${esc(t.colour)}"></span><b>${esc(t.name)}</b>` : "Unclaimed";
  }

  function renderLegend() {
    const list = mapData.tribes || [];
    $("legend").hidden = !list.length;
    $("legend").innerHTML = list.map(t => {
      const n = (data.tribes.find(d => d.id === t.id) || {}).chunks;
      return `<button type="button" data-id="${esc(t.id)}"><span class="swatch" style="background:${esc(t.colour)}"></span>${esc(t.name)}<span class="count">${n == null ? "" : fmt(n) + " ch"}</span></button>`;
    }).join("");
  }
  $("legend").addEventListener("click", e => { const b = e.target.closest("button"); if (b) fitTribe(b.dataset.id); });

  /* input */
  let drag = null;
  canvas.addEventListener("pointerdown", e => {
    canvas.setPointerCapture(e.pointerId);
    drag = { x: e.clientX, y: e.clientY, cx, cz };
    canvas.classList.add("drag");
  });
  canvas.addEventListener("pointermove", e => {
    const r = canvas.getBoundingClientRect();
    mouse = [e.clientX - r.left, e.clientY - r.top];
    if (drag) {
      touched = true;
      anchor = null;
      cx = drag.cx - (e.clientX - drag.x) / s;
      cz = drag.cz - (e.clientY - drag.y) / s;
    }
    redraw();
  });
  const end = () => { drag = null; canvas.classList.remove("drag"); };
  canvas.addEventListener("pointerup", end);
  canvas.addEventListener("pointercancel", end);
  canvas.addEventListener("pointerleave", () => { if (!drag) { mouse = null; $("ro-pos").textContent = "Point at the map"; $("ro-land").textContent = ""; } });

  function zoomBy(f, sx, sy) {
    touched = true;
    const [bx, bz] = toBlock(sx, sy);
    anchor = { sx, sy, bx, bz };
    target = clamp(target * f);
    redraw();
  }
  canvas.addEventListener("wheel", e => {
    e.preventDefault();
    const r = canvas.getBoundingClientRect();
    zoomBy(Math.exp(-Math.sign(e.deltaY) * Math.min(Math.abs(e.deltaY), 120) / 240), e.clientX - r.left, e.clientY - r.top);
  }, { passive: false });
  canvas.addEventListener("keydown", e => {
    const step = 64 / s;
    const k = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[e.key];
    if (k) { e.preventDefault(); touched = true; anchor = null; cx += k[0]; cz += k[1]; redraw(); }
    else if (e.key === "+" || e.key === "=") zoomBy(2, w / 2, h / 2);
    else if (e.key === "-") zoomBy(.5, w / 2, h / 2);
  });
  $("zoom-in").onclick = () => zoomBy(2, w / 2, h / 2);
  $("zoom-out").onclick = () => zoomBy(.5, w / 2, h / 2);
  $("zoom-fit").onclick = () => { touched = false; fitAll(); touched = true; };

  setInterval(redraw, RELOAD); // keeps visible tiles fresh even when nothing moves
  return { resize, setData, centre, fitTribe };
})();

showTab();
poll();
setInterval(poll, 2000);
