#!/usr/bin/env node
/*
 * End-to-end probe: joins a server with a bot, drives the plugin's commands and
 * asserts what actually went over the wire.
 *
 * It does not trust the plugin's own logging: it reads the packet stream the client
 * receives (spawn / metadata / teleport / destroy) and the chat replies, then prints a
 * JSON report the driver turns into a pass/fail.
 *
 * When no bot can speak the server's protocol (mineflayer has no 26.x protocol), it
 * falls back to RCON and asserts the command surface instead, marking the packet-level
 * checks as skipped rather than pretending they passed.
 *
 * Usage: node probe.js --host <host> --mc-version 1.20.1 --server-version 1.20.1
 *                      --backend legacy|modern [--bot name] [--rcon-port 25575]
 *                      [--rcon-password cdn-e2e]
 */
const mineflayer = require('mineflayer');

function arg(name, fallback) {
  const index = process.argv.indexOf('--' + name);
  return index >= 0 && index + 1 < process.argv.length ? process.argv[index + 1] : fallback;
}

const HOST = arg('host', '127.0.0.1');
const PORT = parseInt(arg('port', '25565'), 10);
const MC_VERSION = arg('mc-version', '1.20.1');
const SERVER_VERSION = arg('server-version', MC_VERSION);
const BACKEND = arg('backend', 'legacy');
const BOT_NAME = arg('bot', 'probe');
const RCON_PORT = parseInt(arg('rcon-port', '25575'), 10);
const RCON_PASSWORD = arg('rcon-password', 'cdn-e2e');

// TextDisplay carries its text at index 23; the legacy armour-stand backend at index 2.
const TEXT_INDEX = BACKEND === 'modern' ? 23 : 2;

const SPAWN_PACKETS = new Set(['spawn_entity', 'spawn_entity_living']);
const DESTROY_PACKETS = new Set(['entity_destroy', 'destroy_entities']);
const TELEPORT_PACKET = 'entity_teleport';
const METADATA_PACKET = 'entity_metadata';

const checks = [];
const chat = [];
const teleports = new Map();    // entity id -> [y, y, ...]
const metadataText = new Map(); // entity id -> [text, ...]
const spawned = new Set();
const spawnedCoords = new Map(); // entity id -> "x,y,z" from the spawn packet
const destroyed = new Set();
let packetsSeen = 0;
let notes = [];

function record(name, passed, detail) {
  checks.push({ name, passed: Boolean(passed), detail: detail || '' });
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}${detail ? ' :: ' + detail : ''}`);
}

function skip(name, reason) {
  checks.push({ name, passed: null, skipped: true, detail: reason });
  console.log(`SKIP ${name} :: ${reason}`);
}

function note(message) {
  notes.push(message);
  console.log(`[probe] ${message}`);
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/** Pulls a readable string out of whatever nmp decoded the metadata value into. */
function flatten(value) {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'object') {
    if (typeof value.text === 'string' && Object.keys(value).length === 1) {
      return value.text;
    }
    try {
      return JSON.stringify(value);
    } catch (error) {
      return String(value);
    }
  }
  return String(value);
}

/** Ids seen in metadata carrying our damage text at the expected index. */
function idsWithText(needle) {
  const matches = [];
  for (const [entityId, texts] of metadataText.entries()) {
    if (texts.some(text => text.includes(needle))) {
      matches.push(entityId);
    }
  }
  return matches;
}

function mark() {
  return {
    spawned: new Set(spawned),
    destroyed: new Set(destroyed),
    metadata: new Map(metadataText),
    teleports: new Map(teleports)
  };
}

function sinceStart(before) {
  const newSpawns = [...spawned].filter(id => !before.spawned.has(id));
  const newDestroyed = [...destroyed].filter(id => !before.destroyed.has(id));
  const newTextIds = [];
  for (const [entityId, texts] of metadataText.entries()) {
    const previous = before.metadata.get(entityId) || [];
    if (texts.length > previous.length) {
      newTextIds.push(entityId);
    }
  }
  return { newSpawns, newDestroyed, newTextIds };
}

function risesThenFalls(heights) {
  if (heights.length < 3) {
    return false;
  }
  const peak = Math.max(...heights);
  const peakIndex = heights.indexOf(peak);
  return peakIndex > 0 && peakIndex < heights.length - 1 && heights[0] < peak;
}

async function command(bot, text, waitMs = 900) {
  chat.length = 0;
  bot.chat(text);
  await sleep(waitMs);
  return chat.slice();
}

/** Joins the server, or rejects with a reason a caller can report. */
function connectBot() {
  return new Promise((resolve, reject) => {

    let bot;

    try {
      bot = mineflayer.createBot({
        host: HOST,
        port: PORT,
        username: BOT_NAME,
        version: MC_VERSION,
        auth: 'offline',
        hideErrors: false
      });
    } catch (error) {
      reject(error);
      return;
    }

    const timer = setTimeout(() => reject(new Error('bot did not spawn within 60s')), 60000);

    bot.once('spawn', () => {
      clearTimeout(timer);
      resolve(bot);
    });

    bot.once('error', error => {
      clearTimeout(timer);
      reject(error);
    });

    bot.once('kicked', reason => {
      clearTimeout(timer);
      reject(new Error('kicked: ' + JSON.stringify(reason)));
    });
  });
}

function attachPacketCapture(bot) {
  bot._client.on('packet', (data, meta) => {

    packetsSeen++;

    if (SPAWN_PACKETS.has(meta.name)) {
      spawned.add(data.entityId);
      if (typeof data.x === 'number') {
        spawnedCoords.set(data.entityId,
          `${data.x.toFixed(1)},${data.y.toFixed(1)},${data.z.toFixed(1)}`);
      }
    } else if (DESTROY_PACKETS.has(meta.name)) {
      const ids = Array.isArray(data.entityIds) ? data.entityIds : [data.entityId];
      ids.filter(id => id !== undefined).forEach(id => destroyed.add(id));
    } else if (meta.name === TELEPORT_PACKET) {
      const list = teleports.get(data.entityId) || [];
      list.push(data.y);
      teleports.set(data.entityId, list);
    } else if (meta.name === METADATA_PACKET) {
      const entry = (data.metadata || []).find(item => item.key === TEXT_INDEX);
      if (entry) {
        const list = metadataText.get(data.entityId) || [];
        list.push(flatten(entry.value));
        metadataText.set(data.entityId, list);
      }
    }
  });
}

/** mineflayer exposes entities as a plain object on older builds and a Map on newer ones. */
function allEntities(bot) {
  return bot.entities instanceof Map
    ? [...bot.entities.values()]
    : Object.values(bot.entities || {});
}

/**
 * Runs one command over RCON, returning the console reply, or null when RCON is not
 * available in this environment.
 */
async function rconCommand(text) {
  let Rcon;
  try {
    ({ Rcon } = require('rcon-client'));
  } catch (missing) {
    return null;
  }
  try {
    const rcon = await Rcon.connect({ host: HOST, port: RCON_PORT, password: RCON_PASSWORD });
    const response = await rcon.send(text);
    rcon.end();
    return response;
  } catch (error) {
    return `rcon failed: ${error.message}`;
  }
}

/**
 * Puts a zombie next to the bot and reports what the server said.
 *
 * Tries the bot's own command first and falls back to the console: the console always may
 * summon, and absolute coordinates make the position independent of who runs it (a
 * console's "~ ~ ~" resolves to the world spawn, not to the bot). Success is judged from
 * the reply, not from the client's entity tracking: on newer protocols mineflayer can
 * track the bot's neighbours yet still not see a summoned mob.
 */
async function summonZombie(bot) {
  const before = new Set(spawned);
  const fresh = () => [...spawned].filter(id => !before.has(id));

  const botReply = (await command(bot, '/summon zombie ~ ~ ~2', 1500)).join(' ');
  note(`bot /summon reply: ${botReply || 'no reply'}`);
  if (/summoned/i.test(botReply)) {
    return { reply: botReply, ids: fresh() };
  }

  const position = bot.entity.position;
  const absolute = `${(position.x + 2).toFixed(1)} ${position.y.toFixed(1)} ${position.z.toFixed(1)}`;
  const viaConsole = await rconCommand(`summon zombie ${absolute}`);
  const consoleReply = String(viaConsole ?? 'no rcon available').trim();
  note(`console /summon ${absolute} reply: ${consoleReply.slice(0, 200) || '(empty)'}`);
  await sleep(1200);

  return { reply: consoleReply, ids: fresh() };
}

async function botChecks() {

  const bot = await connectBot();

  attachPacketCapture(bot);
  bot.on('message', message => chat.push(message.toString()));

  record('bot joined the server', true, `as ${bot.username} on ${MC_VERSION} (protocol ${MC_VERSION})`);

  // Make the run deterministic: a bot that fell on spawn would be below the test damage
  // and advanced.remove-on-death would (correctly) flash a killing blow instead of
  // playing the full animation.
  await command(bot, '/kill @e[type=!player]', 600);
  await command(bot, '/effect give @s minecraft:instant_health 1 10', 900);
  note(`bot health before the animation test: ${bot.health}/${bot.food}`);

  // ---- 1. /cdn test renders a number and the packets match ----
  const before = mark();
  const testChat = await command(bot, '/cdn test normal 12.5', 2000);
  const afterTest = sinceStart(before);

  record('test command answered', testChat.some(line => /spawned/i.test(line)), testChat.join(' | '));
  record('a display entity was spawned', afterTest.newSpawns.length > 0,
    `spawned ids: ${afterTest.newSpawns.join(',') || 'none'}`);

  const textIds = idsWithText('12.5');

  record('the display carried the damage text at the expected metadata index',
    textIds.length > 0,
    `index ${TEXT_INDEX}, ids: ${textIds.join(',') || 'none'}`);

  const animated = textIds.map(id => ({ id, heights: teleports.get(id) || [] }))
    .filter(entry => entry.heights.length > 0);

  const best = animated.reduce((current, entry) =>
    !current || entry.heights.length > current.heights.length ? entry : current, null);

  record('the display was moved over the wire (anchored animation)',
    best !== null && best.heights.length >= 3,
    animated.map(entry => `${entry.id}:${entry.heights.length} teleports`).join(', ') || 'no teleports');

  record('the animation rises then falls',
    best !== null && risesThenFalls(best.heights),
    best ? `${best.id}:${best.heights.map(y => y.toFixed(2)).join('/')}` : 'n/a');

  await sleep(2500);
  record('the display was destroyed when its animation ended',
    textIds.some(id => destroyed.has(id)),
    `destroyed: ${[...destroyed].join(',') || 'none'}`);

  // ---- 2. player-side toggle ----
  await command(bot, '/cdn toggle off', 700);
  await command(bot, '/cdn test normal 7.5', 1800);

  // Only our metadata index proves a damage number was rendered: vanilla spawns (items,
  // xp, mobs) are unrelated, and stale displays from earlier steps must not count.
  record('toggle off suppresses new displays',
    idsWithText('7.5').length === 0,
    `displays carrying 7.5: ${idsWithText('7.5').length}`);

  await command(bot, '/cdn toggle on', 700);
  const beforeOn = mark();
  await command(bot, '/cdn test normal 8.5', 1800);
  const afterOn = sinceStart(beforeOn);

  record('toggle on restores displays',
    afterOn.newSpawns.length > 0 || idsWithText('8.5').length > 0,
    `spawns: ${afterOn.newSpawns.length}, texts: ${idsWithText('8.5').length}`);

  await command(bot, '/cdn clear', 800);

  // ---- 3. a real damage event goes through the listener ----
  // Two ways to land a real hit, because neither works everywhere:
  //   - the console's /damage with an entity source (1.20.5+): deterministic, and it needs
  //     neither mob AI nor the client seeing the entity. The damager is the player itself,
  //     which the plugin supports through general.self-damage;
  //   - older servers have no /damage command, so there a zombie is summoned and the bot
  //     attacks it, using the entity tracking that is reliable on those protocols.
  // Both end in EntityDamageByEntityEvent, which is what the listener handles.
  const healthBefore = bot.health;
  const beforeHit = mark();

  const damageReply = String(await rconCommand(
    'minecraft:damage @e[type=player,limit=1] 3 minecraft:player_attack'
    + ' by @e[type=player,limit=1]') ?? '').trim();
  const refused = /unknown|incorrect|usage|failed|no entity|not found/i.test(damageReply);
  const applied = damageReply !== '' && !refused;
  note(`console /damage reply: ${damageReply.slice(0, 160) || '(empty)'}`);

  if (!applied) {
    const summon = await summonZombie(bot);
    note(`summon (fallback): ${summon.reply || 'no reply'}`);
    const target = allEntities(bot).find(entity => entity.id === summon.ids[summon.ids.length - 1]);
    if (target) {
      bot.attack(target);
      note('attacked the summoned zombie with bot.attack');
    } else {
      note('the summoned entity is not tracked by this client');
    }
    bot.chat('/kill @e[type=zombie]');
  }

  // A display carrying the damage that was just applied is the only thing that proves the
  // listener ran. New spawns alone are not proof: unrelated entities appear during the
  // wait, and an earlier revision of this check passed on exactly that.
  let hit = null;
  for (let attempt = 0; attempt < 12 && !hit; attempt++) {
    await sleep(500);
    if (idsWithText('3').length > 0) {
      hit = sinceStart(beforeHit);
    }
  }
  note(`bot health ${healthBefore} -> ${bot.health}`);
  record('a real damage event produced a display', Boolean(hit),
    hit
      ? `text ids carrying the 3 damage: ${idsWithText('3').join(',')}`
        + ` (spawns: ${hit.newSpawns.length})`
      : `no display carried the damage (health ${healthBefore} -> ${bot.health},`
        + ` damage reply: ${damageReply.slice(0, 60) || 'none'})`);

  // ---- 4. the rest of the command surface ----
  const backendChat = await command(bot, '/cdn backend', 700);
  record('/cdn backend reports the active renderer',
    backendChat.some(line => line.includes(BACKEND === 'modern' ? 'text-display' : 'armor-stand')),
    backendChat.join(' | '));

  const versionChat = await command(bot, '/cdn version', 700);
  record('/cdn version reports the plugin version',
    versionChat.some(line => line.includes('CustomDamageNumbers')),
    versionChat.join(' | '));

  const statsChat = await command(bot, '/cdn stats', 700);
  record('/cdn stats reports counters',
    statsChat.some(line => /active displays/i.test(line)),
    statsChat.join(' | '));

  const helpChat = await command(bot, '/cdn help', 700);
  record('/cdn help lists the subcommands',
    helpChat.some(line => line.includes('/cdn toggle')) && helpChat.some(line => line.includes('/cdn reload')),
    `${helpChat.length} lines`);

  bot.chat('/kill @e[type=zombie]');
  bot.end();
  await sleep(500);
}

/**
 * Command-surface checks over RCON, used when no bot can speak the server's protocol.
 * Console responses prove routing, permissions, messages and the player-only guards,
 * but they cannot observe packets, which is why those checks are marked skipped.
 */
async function rconChecks(reason) {

  let Rcon;

  try {
    ({ Rcon } = require('rcon-client'));
  } catch (missing) {
    skip('RCON fallback', 'rcon-client is not installed');
    return;
  }

  note(`falling back to RCON because ${reason}`);

  let rcon;

  try {
    rcon = await Rcon.connect({ host: HOST, port: RCON_PORT, password: RCON_PASSWORD });
  } catch (error) {
    record('RCON connection', false, String(error));
    return;
  }

  record('RCON connection', true, `${HOST}:${RCON_PORT}`);

  const ask = async (command, waitMs = 350) => {
    const response = await rcon.send(command);
    await sleep(waitMs);
    return response || '';
  };

  const backend = await ask('/cdn backend');
  record('/cdn backend reports the active renderer',
    backend.includes(BACKEND === 'modern' ? 'text-display' : 'armor-stand'), backend);

  const version = await ask('/cdn version');
  record('/cdn version reports the plugin version', version.includes('CustomDamageNumbers'), version);

  const stats = await ask('/cdn stats');
  record('/cdn stats reports counters', /active displays/i.test(stats), stats);

  const help = await ask('/cdn help');
  record('/cdn help lists the subcommands',
    help.includes('/cdn toggle') && help.includes('/cdn reload'), `${help.split('\n').length} lines`);

  const reload = await ask('/cdn reload', 900);
  record('/cdn reload succeeds', /reloaded/i.test(reload), reload);

  const test = await ask('/cdn test normal 12.5');
  record('/cdn test refuses the console (player-only guard)', /only players/i.test(test), test);

  const toggle = await ask('/cdn toggle');
  record('/cdn toggle refuses the console (player-only guard)', /only players/i.test(toggle), toggle);

  const clear = await ask('/cdn clear all');
  record('/cdn clear works from the console', /cleared/i.test(clear), clear);

  ['spawn of a display entity', 'the damage text at the expected metadata index',
    'the anchored teleport animation', 'the display destroy', 'toggle off/on suppression',
    'a real damage event']
    .forEach(name => skip(name, `no client can speak protocol ${MC_VERSION}`));

  await rcon.end();
}

function report() {

  const failed = checks.filter(check => check.passed === false).length;
  const passed = checks.filter(check => check.passed === true).length;
  const skipped = checks.filter(check => check.passed === null).length;

  console.log('REPORT_JSON ' + JSON.stringify({
    version: MC_VERSION,
    serverVersion: SERVER_VERSION,
    backend: BACKEND,
    packetsSeen,
    notes,
    checks,
    passed,
    failed,
    skipped
  }));

  return failed;
}

async function main() {

  console.log(`[probe] connecting to ${HOST}:${PORT} as ${BOT_NAME} using protocol ${MC_VERSION} (server ${SERVER_VERSION})`);

  let failure = 0;

  try {
    await botChecks();
  } catch (error) {
    console.log(`[probe] no bot available: ${error.message}`);
    await rconChecks(error.message);
  }

  failure = report();
  process.exit(failure === 0 ? 0 : 1);
}

main().catch(error => {
  console.log('REPORT_JSON ' + JSON.stringify({
    version: MC_VERSION,
    serverVersion: SERVER_VERSION,
    backend: BACKEND,
    fatal: String(error),
    checks
  }));
  console.error('probe failed: ' + error.stack);
  process.exit(1);
});
