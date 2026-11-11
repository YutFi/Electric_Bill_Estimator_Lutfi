// Run with: node tests/tariff.test.js
// Expected values were produced by the original Java BillCalculator (Android app).
const assert = require('assert');
const T = require('../docs/js/tariff.js');

const cases = [
  // units, rebate, total, final
  [0, 0, 0, 0],
  [1, 0, 0.218, 0.218],
  [200, 0, 43.6, 43.6],
  [250, 2, 60.3, 59.094],
  [300, 0, 77.0, 77.0],
  [600, 0, 231.8, 231.8],
  [750, 5, 313.7, 298.015],
];
let n = 0;
for (const [u, r, total, fin] of cases) {
  const c = T.calculate(u);
  assert.ok(Math.abs(c.total - total) < 1e-9, `total for ${u} kWh: got ${c.total}, want ${total}`);
  assert.ok(Math.abs(T.finalCost(c.total, r) - fin) < 1e-9, `final for ${u} kWh @${r}%`);
  const sumUnits = c.blocks.reduce((a, b) => a + b.units, 0);
  assert.strictEqual(sumUnits, u, 'block units add back up to the input');
  n++;
}
assert.deepStrictEqual(T.parseUnits('').ok, false);
assert.deepStrictEqual(T.parseUnits('-1').ok, false);
assert.deepStrictEqual(T.parseUnits('1.5').ok, false);
assert.deepStrictEqual(T.parseUnits('abc').ok, false);
assert.deepStrictEqual(T.parseUnits('250'), { ok: true, value: 250 });
assert.deepStrictEqual(T.parseUnits('1000001').ok, false);
console.log(`tariff tests passed (${n} calculation cases + validation checks)`);
