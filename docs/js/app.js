/* E-Bill Estimator – web version */
(function () {
  'use strict';

  var T = window.Tariff;
  var MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
  var STORE_KEY = 'ebe.bills.v1';
  var VIEWS = ['calculator', 'history', 'about'];
  var reduceMotion = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- tiny helpers ---------- */
  function $(sel, root) { return (root || document).querySelector(sel); }
  function $$(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
  function el(tag, attrs, children) {
    var n = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) {
      if (k === 'class') n.className = attrs[k];
      else if (k === 'text') n.textContent = attrs[k];
      else if (k.slice(0, 2) === 'on') n.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== false && attrs[k] != null) n.setAttribute(k, attrs[k] === true ? '' : attrs[k]);
    });
    (children || []).forEach(function (c) { n.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return n;
  }
  function rm(n, d) { return 'RM ' + (Number(n) || 0).toLocaleString('en-MY', { minimumFractionDigits: d == null ? 2 : d, maximumFractionDigits: d == null ? 2 : d }); }
  function num(n, d) { return (Number(n) || 0).toLocaleString('en-MY', { minimumFractionDigits: d || 0, maximumFractionDigits: d || 0 }); }
  function svgIcon(path) {
    var s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    s.setAttribute('viewBox', '0 0 24 24'); s.setAttribute('aria-hidden', 'true');
    var p = document.createElementNS('http://www.w3.org/2000/svg', 'path'); p.setAttribute('d', path); s.appendChild(p);
    return s;
  }

  /** Animate a number from its current value to `to`. */
  function countUp(node, to, opts) {
    opts = opts || {};
    var decimals = opts.decimals == null ? 2 : opts.decimals;
    var prefix = opts.prefix || '';
    var from = Number(node.dataset.value || 0);
    node.dataset.value = to;
    var fmt = function (v) { return prefix + num(v, decimals); };
    if (reduceMotion || from === to) { node.textContent = fmt(to); return; }
    var dur = opts.duration || 800, start = null;
    if (node._raf) cancelAnimationFrame(node._raf);
    function step(ts) {
      if (start === null) start = ts;
      var p = Math.min(1, (ts - start) / dur);
      var e = 1 - Math.pow(1 - p, 4);
      node.textContent = fmt(from + (to - from) * e);
      if (p < 1) node._raf = requestAnimationFrame(step); else node.textContent = fmt(to);
    }
    node._raf = requestAnimationFrame(step);
  }

  function toast(message, kind) {
    var box = $('#toasts');
    var t = el('div', { class: 'toast' + (kind === 'error' ? ' error' : ''), role: 'status' }, [
      svgIcon(kind === 'error' ? 'M12 8v5M12 16.5v.1M12 3 2.5 20h19z' : 'M5 12.5l4.5 4.5L19 7.5'),
      el('span', { text: message })
    ]);
    box.appendChild(t);
    setTimeout(function () { t.classList.add('out'); setTimeout(function () { t.remove(); }, 400); }, 2800);
  }

  /* ---------- storage ---------- */
  function sanitize(b) {
    if (!b || typeof b !== 'object') return null;
    var units = Number(b.units), rebate = Number(b.rebate);
    if (MONTHS.indexOf(b.month) < 0 || !isFinite(units) || units < 0 || units > T.MAX_UNITS || !isFinite(rebate) || rebate < 0 || rebate > T.MAX_REBATE) return null;
    var c = T.calculate(Math.floor(units));
    return { id: String(b.id || Date.now()), month: b.month, units: Math.floor(units), rebate: Math.round(rebate), total: c.total, final: T.finalCost(c.total, Math.round(rebate)), createdAt: Number(b.createdAt) || Date.now() };
  }
  function loadBills() {
    try {
      var raw = JSON.parse(localStorage.getItem(STORE_KEY) || '[]');
      return Array.isArray(raw) ? raw.map(sanitize).filter(Boolean) : [];
    } catch (e) { return []; }
  }
  var bills = loadBills();
  /** Returns false if the browser blocks storage (e.g. private mode); the app keeps working in memory. */
  function persist() {
    try { localStorage.setItem(STORE_KEY, JSON.stringify(bills)); return true; }
    catch (e) { return false; }
  }
  function newId() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 7); }

  /* ---------- theme ---------- */
  $('#theme-toggle').addEventListener('click', function () {
    var next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    try { localStorage.setItem('ebe.theme', next); } catch (e) {}
    var meta = $('meta[name="theme-color"]'); if (meta) meta.setAttribute('content', next === 'dark' ? '#07110f' : '#00796b');
  });

  /* ---------- navigation (hash based) ---------- */
  function currentView() { var h = location.hash.replace('#', ''); return VIEWS.indexOf(h) >= 0 ? h : 'calculator'; }
  function moveIndicator() {
    var tabs = $('#tabs'), active = $('button[aria-current="page"]', tabs);
    if (!active) return;
    tabs.style.setProperty('--ix', active.offsetLeft + 'px');
    tabs.style.setProperty('--iw', active.offsetWidth + 'px');
  }
  function showView(name, firstLoad) {
    VIEWS.forEach(function (v) { var s = $('#view-' + v); if (s) s.hidden = v !== name; });
    $$('#tabs button').forEach(function (b) {
      if (b.dataset.view === name) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    var section = $('#view-' + name);
    // restart the entrance animation
    section.style.animation = 'none'; void section.offsetWidth; section.style.animation = '';
    document.title = $('#view-' + name).dataset.title + ' – E-Bill Estimator';
    moveIndicator();
    if (name === 'history') renderHistory();
    if (!firstLoad) { window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' }); }
  }
  window.addEventListener('hashchange', function () { showView(currentView()); });
  window.addEventListener('resize', moveIndicator);
  $$('#tabs button').forEach(function (b) { b.addEventListener('click', function () { location.hash = b.dataset.view; }); });
  $$('[data-go]').forEach(function (b) { b.addEventListener('click', function () { location.hash = b.dataset.go; }); });

  /* ---------- calculator ---------- */
  var monthSel = $('#month'), unitsIn = $('#units'), rebateIn = $('#rebate'), rebateOut = $('#rebate-out');
  MONTHS.forEach(function (m, i) { monthSel.appendChild(el('option', { value: m, text: m })); });
  monthSel.selectedIndex = new Date().getMonth();

  function setRebate(v) {
    rebateOut.textContent = v + '%';
    rebateIn.setAttribute('aria-valuetext', v + ' percent');
    rebateIn.style.setProperty('--fill', (v / T.MAX_REBATE * 100) + '%');
  }
  setRebate(0);

  function showUnitsError(msg) {
    var box = $('#units-error'), wrap = $('#units-wrap');
    if (msg) {
      box.textContent = msg; box.hidden = false; wrap.classList.add('invalid'); unitsIn.setAttribute('aria-invalid', 'true');
    } else {
      box.hidden = true; wrap.classList.remove('invalid'); unitsIn.removeAttribute('aria-invalid');
    }
  }

  var TIER_COLORS = ['var(--tier1)', 'var(--tier2)', 'var(--tier3)', 'var(--tier4)'];

  function renderResult(units, rebate) {
    var c = T.calculate(units), fin = T.finalCost(c.total, rebate);
    $('#result-empty').hidden = true;
    var body = $('#result-body'); var wasHidden = body.hidden; body.hidden = false;
    if (wasHidden) { body.style.animation = 'none'; void body.offsetWidth; body.style.animation = ''; }

    $('#res-rebate-note').textContent = '(rebate ' + rebate + '%)';
    countUp($('#res-final'), fin, { decimals: 2 });
    $('#res-total').textContent = rm(c.total);
    $('#res-saved').textContent = rm(c.total - fin);
    $('#res-avg').textContent = units > 0 ? rm(fin / units, 3) : rm(0, 3);

    var stack = $('#res-stack'), list = $('#res-tiers');
    stack.innerHTML = ''; list.innerHTML = '';
    var segs = [];
    c.blocks.forEach(function (b, i) {
      var seg = el('i', { style: 'background:' + TIER_COLORS[i], title: b.name }); stack.appendChild(seg);
      segs.push({ node: seg, pct: c.total > 0 ? b.cost / c.total * 100 : 0 });
      list.appendChild(el('li', { class: b.units === 0 ? 'unused' : '', style: '--i:' + i }, [
        el('span', { class: 'dot', style: 'background:' + TIER_COLORS[i] }),
        el('span', { class: 't-name' }, [b.name, el('small', { text: num(b.units) + ' kWh × RM ' + b.rate.toFixed(3) })]),
        el('span', { class: 't-cost', text: rm(b.cost) })
      ]));
    });
    stack.setAttribute('aria-label', c.blocks.map(function (b) { return b.name + ': ' + rm(b.cost); }).join('; '));
    requestAnimationFrame(function () { requestAnimationFrame(function () { segs.forEach(function (s) { s.node.style.width = s.pct + '%'; }); }); });
    return { total: c.total, final: fin };
  }

  function clearResult() {
    $('#result-body').hidden = true; $('#result-empty').hidden = false; $('#res-final').dataset.value = 0;
  }

  function currentInput(showErrors) {
    var p = T.parseUnits(unitsIn.value);
    if (!p.ok) {
      if (showErrors || !p.empty) showUnitsError(p.empty && !showErrors ? '' : p.message); else showUnitsError('');
      return null;
    }
    showUnitsError('');
    return { units: p.value, rebate: Number(rebateIn.value), month: monthSel.value };
  }

  function livePreview() {
    var inp = currentInput(false);
    if (inp) renderResult(inp.units, inp.rebate); else clearResult();
  }
  unitsIn.addEventListener('input', function () {
    var cleaned = unitsIn.value.replace(/[\s,]/g, '');
    if (cleaned !== unitsIn.value) unitsIn.value = cleaned;
    livePreview();
  });
  rebateIn.addEventListener('input', function () { setRebate(rebateIn.value); livePreview(); });
  $$('.chip').forEach(function (c) { c.addEventListener('click', function () { unitsIn.value = c.dataset.units; livePreview(); unitsIn.focus(); }); });

  $('#calc-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var inp = currentInput(true);
    if (!inp) { unitsIn.focus(); return; }
    var r = renderResult(inp.units, inp.rebate);
    bills.unshift({ id: newId(), month: inp.month, units: inp.units, rebate: inp.rebate, total: r.total, final: r.final, createdAt: Date.now() });
    var ok = persist();
    updateBadge();
    var btn = $('#save-btn'); btn.classList.remove('saved'); void btn.offsetWidth; btn.classList.add('saved');
    toast(ok ? 'Calculation saved successfully!' : 'Calculated, but this browser blocked saving.', ok ? 'ok' : 'error');
    unitsIn.value = ''; // same as the app: clear the input after a successful save
    if (window.matchMedia('(max-width: 899px)').matches) {
      // single-column layout: bring the result into view so it is not hidden below the form
      $('#result').scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' });
    } else {
      unitsIn.focus();
    }
  });

  function updateBadge() {
    var b = $('#history-badge');
    b.hidden = bills.length === 0;
    if (bills.length) { b.textContent = bills.length > 99 ? '99+' : bills.length; b.style.animation = 'none'; void b.offsetWidth; b.style.animation = ''; }
    requestAnimationFrame(moveIndicator);
  }

  /* ---------- history ---------- */
  function renderHistory() {
    var empty = bills.length === 0;
    $('#history-empty').hidden = !empty;
    $('#history-content').hidden = empty;
    $('#export-btn').hidden = empty;
    if (empty) return;

    var sum = bills.reduce(function (a, b) { return a + b.final; }, 0);
    var max = bills.reduce(function (a, b) { return Math.max(a, b.final); }, 0);
    var stats = { records: bills.length, sum: sum, avg: sum / bills.length, max: max };
    $$('[data-count]').forEach(function (n) {
      var k = n.dataset.count;
      countUp(n, stats[k], { decimals: k === 'records' ? 0 : 2, prefix: n.dataset.prefix || '', duration: 900 });
    });

    renderChart();

    var list = $('#bill-list'); list.innerHTML = '';
    bills.forEach(function (b, i) {
      var li = el('li', null, [
        el('button', { class: 'bill-item', type: 'button', style: '--i:' + Math.min(i, 12), 'aria-label': b.month + ', ' + rm(b.final) + '. Open details', onclick: function () { openDetail(b.id); } }, [
          el('span', { class: 'mon', text: b.month.slice(0, 3).toUpperCase() }),
          el('span', null, [el('span', { class: 'm-name', text: b.month }), el('br'), el('span', { class: 'm-sub', text: num(b.units) + ' kWh · rebate ' + b.rebate + '%' })]),
          el('span', { class: 'm-cost', text: rm(b.final) })
        ])
      ]);
      list.appendChild(li);
    });
  }

  function renderChart() {
    var chart = $('#chart'); chart.innerHTML = '';
    var latest = bills.slice(0, 12).reverse();
    var max = latest.reduce(function (a, b) { return Math.max(a, b.final); }, 0) || 1;
    var maxIdx = latest.reduce(function (mi, b, i, arr) { return b.final > arr[mi].final ? i : mi; }, 0);
    var fills = [];
    latest.forEach(function (b, i) {
      var fill = el('div', { class: 'fill', style: '--i:' + i });
      var bar = el('div', { class: 'bar' + (i === maxIdx ? ' max' : ''), title: b.month + ': ' + rm(b.final) }, [
        el('span', { class: 'val', text: num(b.final, 0) }), fill, el('span', { class: 'lbl', text: b.month.slice(0, 3) })
      ]);
      chart.appendChild(bar); fills.push({ fill: fill, bar: bar, pct: Math.max(2, b.final / max * 78) });
    });
    chart.setAttribute('aria-label', 'Bar chart of your latest ' + latest.length + ' bills: ' + latest.map(function (b) { return b.month + ' ' + rm(b.final); }).join(', '));
    requestAnimationFrame(function () { requestAnimationFrame(function () {
      fills.forEach(function (f) { f.fill.style.height = f.pct + '%'; });
      setTimeout(function () { fills.forEach(function (f) { f.bar.classList.add('ready'); }); }, 50);
    }); });
  }

  $('#export-btn').addEventListener('click', function () {
    function cell(v) { return '"' + String(v).replace(/"/g, '""') + '"'; }
    var rows = [['Month', 'Units (kWh)', 'Rebate (%)', 'Total charges (RM)', 'Final cost (RM)', 'Saved on']];
    bills.slice().reverse().forEach(function (b) {
      rows.push([b.month, b.units, b.rebate, b.total.toFixed(2), b.final.toFixed(2), new Date(b.createdAt).toISOString().slice(0, 10)]);
    });
    var csv = '﻿' + rows.map(function (r) { return r.map(cell).join(','); }).join('\r\n');
    var url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    var a = el('a', { href: url, download: 'electric-bill-history.csv' });
    document.body.appendChild(a); a.click(); a.remove(); setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    toast('History exported as CSV.');
  });

  /* ---------- dialog: detail / edit / delete ---------- */
  var dlg = $('#dialog'), lastFocus = null;
  function openDialog(content) {
    dlg.innerHTML = ''; dlg.appendChild(content);
    if (!dlg.open) { lastFocus = document.activeElement; if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', ''); }
  }
  function closeDialog() { if (dlg.open) dlg.close(); }
  dlg.addEventListener('close', function () { if (lastFocus && lastFocus.focus) lastFocus.focus(); });
  dlg.addEventListener('click', function (e) { if (e.target === dlg) closeDialog(); });

  function dlgHeader(title) {
    return el('div', { class: 'dlg-head' }, [
      el('h2', { id: 'dlg-title', text: title }),
      el('button', { class: 'icon-btn x', type: 'button', 'aria-label': 'Close', onclick: closeDialog }, [svgIcon('M6 6l12 12M18 6 6 18')])
    ]);
  }
  function findBill(id) { for (var i = 0; i < bills.length; i++) if (bills[i].id === id) return bills[i]; return null; }

  function openDetail(id) {
    var b = findBill(id); if (!b) return;
    var rows = el('dl', { class: 'detail-rows' }, [
      el('div', null, [el('dt', { text: 'Month' }), el('dd', { text: b.month })]),
      el('div', null, [el('dt', { text: 'Units used' }), el('dd', { text: num(b.units) + ' kWh' })]),
      el('div', null, [el('dt', { text: 'Total charges' }), el('dd', { text: rm(b.total) })]),
      el('div', null, [el('dt', { text: 'Rebate' }), el('dd', { text: b.rebate + '%' })]),
      el('div', { class: 'final' }, [el('dt', { text: 'Final cost' }), el('dd', { text: rm(b.final) })])
    ]);
    openDialog(el('div', { class: 'dlg' }, [
      dlgHeader('Bill details'), rows,
      el('div', { class: 'dlg-actions' }, [
        el('button', { class: 'btn ghost small', type: 'button', onclick: function () { openEdit(id); } }, [el('span', { text: 'Edit' })]),
        el('button', { class: 'btn danger small', type: 'button', onclick: function () { openDelete(id); } }, [el('span', { text: 'Delete' })])
      ])
    ]));
  }

  function openEdit(id) {
    var b = findBill(id); if (!b) return;
    var mSel = el('select', { id: 'e-month' }, MONTHS.map(function (m) { return el('option', { value: m, text: m }); }));
    mSel.value = b.month;
    var uIn = el('input', { id: 'e-units', type: 'text', inputmode: 'numeric', maxlength: '7', value: String(b.units), 'aria-describedby': 'e-err' });
    var rIn = el('input', { id: 'e-rebate', type: 'range', min: '0', max: String(T.MAX_REBATE), step: '1', value: String(b.rebate) });
    var rOut = el('output', { for: 'e-rebate', text: b.rebate + '%' });
    var err = el('p', { class: 'error', id: 'e-err', role: 'alert', hidden: true });
    var preview = el('p', { class: 'dlg-preview' });
    function fillRange() { rIn.style.setProperty('--fill', (rIn.value / T.MAX_REBATE * 100) + '%'); rOut.textContent = rIn.value + '%'; }
    function update() {
      fillRange();
      var p = T.parseUnits(uIn.value);
      if (!p.ok) { preview.textContent = 'Final cost: –'; return null; }
      var c = T.calculate(p.value);
      preview.textContent = 'Final cost: ' + rm(T.finalCost(c.total, Number(rIn.value)));
      return p.value;
    }
    uIn.addEventListener('input', function () { uIn.value = uIn.value.replace(/[\s,]/g, ''); err.hidden = true; update(); });
    rIn.addEventListener('input', update);
    update();

    var form = el('form', { class: 'dlg', novalidate: true, onsubmit: function (e) {
      e.preventDefault();
      var p = T.parseUnits(uIn.value);
      if (!p.ok) { err.textContent = p.message; err.hidden = false; uIn.focus(); return; }
      var c = T.calculate(p.value), reb = Number(rIn.value);
      b.month = mSel.value; b.units = p.value; b.rebate = reb; b.total = c.total; b.final = T.finalCost(c.total, reb);
      var ok = persist(); closeDialog(); renderHistory();
      toast(ok ? 'Bill updated successfully!' : 'Updated, but this browser blocked saving.', ok ? 'ok' : 'error');
    } }, [
      dlgHeader('Edit bill'),
      el('div', { class: 'field' }, [el('label', { for: 'e-month', text: 'Month' }), el('div', { class: 'select-wrap' }, [mSel])]),
      el('div', { class: 'field' }, [el('label', { for: 'e-units', text: 'Units used (kWh)' }), el('div', { class: 'input-wrap' }, [uIn, el('span', { class: 'suffix', text: 'kWh' })]), err]),
      el('div', { class: 'field' }, [el('label', { for: 'e-rebate' }, ['Rebate ', rOut]), rIn]),
      preview,
      el('div', { class: 'dlg-actions' }, [
        el('button', { class: 'btn ghost small', type: 'button', onclick: function () { openDetail(id); } }, [el('span', { text: 'Cancel' })]),
        el('button', { class: 'btn primary small', type: 'submit', style: 'width:auto' }, [el('span', { text: 'Save changes' })])
      ])
    ]);
    openDialog(form);
    setTimeout(function () { uIn.focus(); uIn.select(); }, 60);
  }

  function openDelete(id) {
    var b = findBill(id); if (!b) return;
    openDialog(el('div', { class: 'dlg' }, [
      dlgHeader('Delete bill'),
      el('p', { text: 'Are you sure you want to delete the ' + b.month + ' record (' + rm(b.final) + ')? This cannot be undone.' }),
      el('div', { class: 'dlg-actions' }, [
        el('button', { class: 'btn ghost small', type: 'button', onclick: function () { openDetail(id); } }, [el('span', { text: 'No, keep it' })]),
        el('button', { class: 'btn danger small', type: 'button', onclick: function () {
          bills = bills.filter(function (x) { return x.id !== id; });
          var ok = persist(); closeDialog(); updateBadge(); renderHistory();
          toast(ok ? 'Bill deleted.' : 'Deleted, but this browser blocked saving.', ok ? 'ok' : 'error');
        } }, [el('span', { text: 'Yes, delete' })])
      ])
    ]));
  }

  /* ---------- install prompt (Chrome/Edge/Android) ---------- */
  var deferredPrompt = null;
  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault(); deferredPrompt = e; $('#install-btn').hidden = false;
  });
  $('#install-btn').addEventListener('click', function () {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    deferredPrompt.userChoice.then(function () { deferredPrompt = null; $('#install-btn').hidden = true; });
  });

  /* ---------- offline support ---------- */
  if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
    window.addEventListener('load', function () { navigator.serviceWorker.register('sw.js').catch(function () {}); });
  }

  /* ---------- start ---------- */
  updateBadge();
  showView(currentView(), true);
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(moveIndicator);
  window.addEventListener('load', moveIndicator);
})();
