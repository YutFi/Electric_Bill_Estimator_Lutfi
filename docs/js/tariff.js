/*
 * Tariff maths – a direct port of BillCalculator.java from the Android app.
 * Same blocks, same rates, same order of additions, so results match the app.
 * Works in the browser (window.Tariff) and in Node (module.exports) for testing.
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.Tariff = factory();
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /** Each block: how many kWh it covers (Infinity = everything left) and its RM/kWh rate. */
  var BLOCKS = [
    { key: 'b1', name: 'First 200 kWh', range: '1 – 200 kWh', size: 200, rate: 0.218 },
    { key: 'b2', name: 'Next 100 kWh', range: '201 – 300 kWh', size: 100, rate: 0.334 },
    { key: 'b3', name: 'Next 300 kWh', range: '301 – 600 kWh', size: 300, rate: 0.516 },
    { key: 'b4', name: 'Above 600 kWh', range: '601 kWh and over', size: Infinity, rate: 0.546 }
  ];

  var MAX_UNITS = 1000000;
  var MAX_REBATE = 5;

  /** Returns { total, blocks:[{...block, units, cost}] } for a whole number of units. */
  function calculate(units) {
    var remaining = units;
    var total = 0.0;
    var blocks = [];
    for (var i = 0; i < BLOCKS.length; i++) {
      var b = BLOCKS[i];
      var used = 0;
      if (remaining > 0) {
        used = Math.min(remaining, b.size);
        remaining -= used;
      }
      var cost = used * b.rate;
      total += cost;
      blocks.push({ key: b.key, name: b.name, range: b.range, rate: b.rate, units: used, cost: cost });
    }
    return { total: total, blocks: blocks };
  }

  function finalCost(total, rebatePercent) {
    return total - (total * rebatePercent / 100.0);
  }

  /** Validate raw text from the units field. Returns { ok, value, message }. */
  function parseUnits(text) {
    var s = String(text == null ? '' : text).trim();
    if (s === '') return { ok: false, empty: true, message: 'Please enter units used.' };
    if (/^-/.test(s)) return { ok: false, message: 'Units cannot be negative.' };
    if (!/^\d+$/.test(s)) return { ok: false, message: 'Invalid number. Use whole numbers only, e.g. 250.' };
    var n = parseInt(s, 10);
    if (n > MAX_UNITS) return { ok: false, message: 'That looks too high. Please enter ' + MAX_UNITS.toLocaleString('en-MY') + ' kWh or less.' };
    return { ok: true, value: n };
  }

  return { BLOCKS: BLOCKS, MAX_UNITS: MAX_UNITS, MAX_REBATE: MAX_REBATE, calculate: calculate, finalCost: finalCost, parseUnits: parseUnits };
});
