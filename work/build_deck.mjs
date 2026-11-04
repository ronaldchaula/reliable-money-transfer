import fs from 'node:fs/promises';
import path from 'node:path';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const root = '/home/chax/work/Pesalock/projects/Ndani/reliable-money-transfer';
const build = path.join(root, 'work', 'deck-build');
const output = path.join(root, 'outputs');
await fs.mkdir(build, { recursive: true });
await fs.mkdir(output, { recursive: true });

const deck = Presentation.create({ slideSize: { width: 1280, height: 720 } });
const C = { bg: '#08121f', panel: '#102b42', panel2: '#15263c', text: '#f4f7fb', muted: '#b9c8d8', cyan: '#5ce1e6', amber: '#f2b95e', red: '#ff7b7b', green: '#78e08f' };

function box(slide, x, y, w, h, fill, radius = 'rounded-lg') {
  return slide.shapes.add({ geometry: 'roundRect', position: { left: x, top: y, width: w, height: h }, fill, line: { style: 'solid', fill: fill, width: 1 }, borderRadius: radius });
}
function text(slide, value, x, y, w, h, size, color = C.text, bold = false) {
  const s = slide.shapes.add({ geometry: 'textbox', position: { left: x, top: y, width: w, height: h }, fill: 'none', line: { style: 'solid', fill: 'none', width: 0 } });
  s.text = value;
  s.text.style = { fontSize: size, color, bold, fontFamily: 'Arial' };
  return s;
}
function line(slide, x1, y1, x2, y2, color = C.cyan, width = 3) {
  return slide.shapes.add({ geometry: 'line', position: { left: x1, top: y1, width: x2 - x1, height: y2 - y1 }, line: { style: 'solid', fill: color, width }, fill: 'none' });
}
function base(title, kicker = '') {
  const slide = deck.slides.add();
  slide.background.fill = C.bg;
  if (kicker) text(slide, kicker.toUpperCase(), 72, 42, 400, 24, 14, C.cyan, true);
  text(slide, title, 72, kicker ? 76 : 52, 1120, 60, 32, C.text, true);
  return slide;
}

// 1. Cover
{
  const s = deck.slides.add(); s.background.fill = C.bg;
  text(s, 'RELIABLE MONEY TRANSFER', 80, 80, 520, 26, 16, C.cyan, true);
  text(s, 'Retries that protect the customer\nwhen the network cannot tell us what happened', 80, 150, 740, 150, 42, C.text, true);
  text(s, 'Kotlin reference implementation for bank and MNO payment flows', 84, 340, 650, 40, 20, C.muted);
  box(s, 850, 130, 300, 320, C.panel);
  text(s, 'One transfer', 900, 180, 200, 30, 20, C.muted, true);
  text(s, 'stable key', 925, 250, 160, 30, 24, C.cyan, true);
  line(s, 930, 310, 1070, 310, C.amber, 4);
  text(s, 'same key\nacross every attempt', 905, 345, 190, 70, 20, C.text, true);
  text(s, '04 Nov 2026', 84, 615, 250, 26, 15, C.muted);
}

// 2. Problem
{
  const s = base('The failure mode is ambiguous', '01 / why it matters');
  text(s, 'A timeout does not mean the provider rejected the transfer.', 72, 150, 780, 34, 22, C.text, true);
  box(s, 90, 250, 250, 150, C.panel); text(s, 'POST /transfers', 125, 285, 190, 30, 22, C.cyan, true); text(s, 'request leaves app', 125, 330, 190, 25, 17, C.muted);
  line(s, 340, 325, 490, 325, C.cyan, 4);
  box(s, 500, 250, 250, 150, C.panel2); text(s, 'Provider', 575, 285, 120, 30, 22, C.text, true); text(s, 'may commit money', 545, 330, 170, 25, 17, C.muted);
  line(s, 750, 325, 900, 325, C.red, 4);
  box(s, 910, 250, 250, 150, '#3b2530'); text(s, 'timeout', 985, 285, 120, 30, 22, C.red, true); text(s, 'response never arrives', 945, 330, 180, 25, 17, C.muted);
  text(s, 'A blind replay can turn one customer intent into two money movements.', 160, 500, 960, 40, 24, C.amber, true);
}

// 3. Core rule
{
  const s = base('The same idempotency key travels with the request', '02 / core rule');
  text(s, 'The provider stores the key with the final result. Duplicate POSTs return that result.', 72, 145, 1080, 34, 21, C.text);
  const xs = [90, 430, 770]; const labels = ['Attempt 1', 'Attempt 2', 'Attempt 3'];
  xs.forEach((x, i) => { box(s, x, 250, 250, 180, C.panel); text(s, labels[i], x + 30, 285, 180, 28, 21, C.text, true); text(s, 'Idempotency-Key', x + 30, 335, 190, 24, 15, C.muted); text(s, 'transfer-7f2a', x + 30, 370, 190, 27, 20, C.cyan, true); if (i < 2) line(s, x + 250, 340, x + 330, 340, C.amber, 4); });
  text(s, 'One logical transfer  =  one durable key  =  one provider-side effect', 160, 535, 980, 35, 23, C.green, true);
}

// 4. Algorithm
{
  const s = base('The client separates retryable errors from unknown outcomes', '03 / algorithm');
  const rows = [
    ['HTTP 4xx / auth / validation', 'FAILED', C.red, 'Stop'],
    ['HTTP 5xx / 429', 'PENDING', C.amber, 'Backoff, retry same key'],
    ['Timeout / network failure', 'UNKNOWN_OUTCOME', C.cyan, 'Resolve by key before POST'],
    ['Provider success', 'SUCCEEDED', C.green, 'Stop immediately'],
  ];
  text(s, 'Provider signal', 90, 175, 300, 28, 15, C.muted, true); text(s, 'Stored state', 430, 175, 300, 28, 15, C.muted, true); text(s, 'Next action', 760, 175, 400, 28, 15, C.muted, true);
  rows.forEach((r, i) => { const y = 220 + i * 82; box(s, 72, y, 1120, 60, i % 2 ? C.panel2 : C.panel); text(s, r[0], 95, y + 18, 300, 25, 18, C.text); text(s, r[1], 430, y + 18, 280, 25, 18, r[2], true); text(s, r[3], 760, y + 18, 390, 25, 18, C.text); });
  text(s, 'The unknown state is the guardrail against duplicate money movement.', 130, 590, 1000, 30, 22, C.amber, true);
}

// 5. Kotlin design
{
  const s = base('The Kotlin code is organized around failure boundaries', '04 / implementation');
  const cols = [
    ['domain', 'TransferRequest\nTransferRecord\nTransferStatus', C.cyan],
    ['transport', 'ProviderClient\nProviderFailure', C.amber],
    ['persistence', 'TransferRepository\nRoom / SQLite adapter', C.green],
    ['application', 'TransferService\nresume after process death', '#d6a8ff'],
  ];
  cols.forEach((c, i) => { const x = 72 + i * 285; box(s, x, 205, 250, 230, C.panel); text(s, c[0], x + 24, 235, 200, 28, 21, c[2], true); text(s, c[1], x + 24, 295, 205, 90, 19, C.text); });
  text(s, 'RetryPolicy', 460, 500, 180, 30, 21, C.amber, true); text(s, 'exponential backoff + jitter + max attempts', 660, 500, 470, 30, 19, C.text);
  text(s, 'The REST adapter can change without changing transfer safety rules.', 170, 590, 950, 30, 22, C.muted, true);
}

// 6. Architecture
{
  const s = base('Typical bank / MNO deployment', '05 / architecture');
  const blocks = [
    [72, 210, 220, 125, 'Mobile app', 'Kotlin + Compose\nWorkManager + Room', C.cyan],
    [380, 210, 240, 125, 'Transfer API', 'auth, limits, risk, ledger', C.cyan],
    [710, 210, 220, 125, 'Outbox worker', 'Kafka / SQS\nbackoff + retry', C.amber],
    [1020, 210, 190, 125, 'Provider API', 'HTTPS REST', C.cyan],
  ];
  blocks.forEach(b => { box(s, b[0], b[1], b[2], b[3], C.panel); text(s, b[4], b[0] + 18, b[1] + 30, b[2] - 30, 27, 19, b[5], true); text(s, b[5+0], 0, 0, 1, 1, 1); text(s, b[5], 0, 0, 1, 1, 1); });
  // Re-draw useful body labels separately so the slide remains editable and readable.
  text(s, 'Kotlin + Compose\nWorkManager + Room', 90, 275, 180, 48, 16, C.muted); text(s, 'auth, limits, risk, ledger', 398, 275, 205, 48, 16, C.muted); text(s, 'Kafka / SQS\nbackoff + retry', 728, 275, 180, 48, 16, C.muted); text(s, 'HTTPS REST', 1042, 275, 150, 32, 16, C.muted);
  line(s, 292, 272, 380, 272); line(s, 620, 272, 710, 272); line(s, 930, 272, 1020, 272);
  box(s, 380, 450, 240, 100, C.panel2); text(s, 'Transfer DB', 430, 475, 150, 25, 19, C.green, true); text(s, 'unique idempotency key', 410, 510, 190, 22, 15, C.muted);
  box(s, 1020, 450, 190, 100, C.panel2); text(s, 'Provider store', 1040, 475, 150, 25, 19, C.green, true); text(s, 'key → final result', 1040, 510, 150, 22, 15, C.muted);
  line(s, 500, 335, 500, 450, C.green); line(s, 1115, 335, 1115, 450, C.green);
  text(s, 'Protocol: HTTPS/TLS 1.3 • JSON REST • Idempotency-Key • status lookup • OpenTelemetry + audit logs', 90, 625, 1110, 28, 16, C.muted);
}

// 7. Delivery checklist
{
  const s = base('What must be true in production', '06 / checklist');
  const items = [
    'Provider persists idempotency key and returns the original result on duplicate POSTs',
    'Mobile and backend persist transfer state before attempting the network call',
    'Unknown outcomes resolve by key before any new submission',
    'Outbox and ledger use a unique constraint on the logical transfer reference',
    'Reconciliation, audit logs, alerting, and dead-letter handling close the operational loop',
  ];
  items.forEach((item, i) => { const y = 165 + i * 78; text(s, '0' + (i + 1), 100, y, 45, 35, 22, C.cyan, true); text(s, item, 170, y, 980, 45, 21, C.text, i === 0); });
  text(s, 'Reference code and architecture are in the project folder.', 100, 610, 900, 30, 20, C.muted);
}

for (const [index, slide] of deck.slides.items.entries()) {
  const png = await deck.export({ slide, format: 'png', scale: 1 });
  await fs.writeFile(path.join(build, `slide-${String(index + 1).padStart(2, '0')}.png`), new Uint8Array(await png.arrayBuffer()));
}
const pptx = await PresentationFile.exportPptx(deck);
await pptx.save(path.join(build, 'reliable-money-transfer-candidate.pptx'));
console.log(JSON.stringify({ build, slides: deck.slides.items.length }));

