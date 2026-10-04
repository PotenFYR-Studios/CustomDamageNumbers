#!/usr/bin/env node
/*
 * Prepares one server directory inside the shared e2e volume: Paper, PacketEvents,
 * our plugin jar, eula, server.properties and ops.json for the probe bot.
 *
 * Runs in a node container so it can compute the offline-mode UUID the server will
 * assign to the bot (needed to pre-op it without a console).
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

/**
 * A complete flat-world preset. Omitting it makes Paper log "No key layers in
 * MapLike[{}]", which would be noise in the error scan.
 */
const FLAT_GENERATOR_SETTINGS = JSON.stringify({
  layers: [
    { block: 'minecraft:bedrock', height: 1 },
    { block: 'minecraft:dirt', height: 3 },
    { block: 'minecraft:grass_block', height: 1 }
  ],
  biome: 'minecraft:plains'
});

/** RCON gives the harness a command channel that does not depend on bot protocol support. */
const RCON_PASSWORD = 'cdn-e2e';

function arg(name, fallback) {
  const index = process.argv.indexOf('--' + name);
  return index >= 0 && index + 1 < process.argv.length ? process.argv[index + 1] : fallback;
}

/** The UUID a server in offline mode derives from a player name. */
function offlineUuid(name) {
  const digest = crypto.createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  digest[6] = (digest[6] & 0x0f) | 0x30;
  digest[8] = (digest[8] & 0x3f) | 0x80;
  const hex = digest.toString('hex');
  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20)
  ].join('-');
}

async function download(url, destination, label) {
  if (fs.existsSync(destination) && fs.statSync(destination).size > 1024) {
    console.log(`[prepare] ${label} already present`);
    return;
  }
  const response = await fetch(url, { redirect: 'follow' });
  if (!response.ok) {
    throw new Error(`${label} download failed: HTTP ${response.status} from ${url}`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  if (buffer.length < 1024) {
    throw new Error(`${label} download is suspiciously small (${buffer.length} bytes)`);
  }
  fs.writeFileSync(destination, buffer);
  console.log(`[prepare] ${label} -> ${destination} (${buffer.length} bytes)`);
}

function extraPlugins() {
  const plugins = [];
  process.argv.forEach((value, index) => {
    if (value !== '--extra-plugin' || index + 1 >= process.argv.length) {
      return;
    }
    const [name, url] = process.argv[index + 1].split('=');
    if (name && url) {
      plugins.push({ name, url });
    }
  });
  return plugins;
}

async function main() {
  const dir = arg('dir');
  const paperUrl = arg('paper-url');
  const packetEventsUrl = arg('packetevents-url');
  const pluginJar = arg('plugin-jar');
  const botName = arg('bot', 'probe');
  const version = arg('version', 'unknown');

  if (!dir || !paperUrl || !pluginJar) {
    throw new Error('usage: prepare.js --dir <server dir> --paper-url <url> --packetevents-url <url> --plugin-jar <path> [--bot name]');
  }

  fs.mkdirSync(path.join(dir, 'plugins'), { recursive: true });

  await download(paperUrl, path.join(dir, 'server.jar'), 'paper ' + version);
  await download(packetEventsUrl, path.join(dir, 'plugins', 'packetevents.jar'), 'packetevents');

  const target = path.join(dir, 'plugins', path.basename(pluginJar));
  fs.copyFileSync(pluginJar, target);
  console.log(`[prepare] plugin -> ${target}`);

  for (const plugin of extraPlugins()) {
    await download(plugin.url, path.join(dir, 'plugins', plugin.name + '.jar'), plugin.name);
  }

  // Start from shipped defaults every run: an older messages.yml would otherwise hide
  // the current frame labels, and the harness must exercise what users get on install.
  fs.rmSync(path.join(dir, 'plugins', 'CustomDamageNumbers'), { recursive: true, force: true });

  const config = path.join(dir, 'plugins', 'CustomDamageNumbers', 'config.yml');
  fs.mkdirSync(path.dirname(config), { recursive: true });

  fs.writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n');

  fs.writeFileSync(path.join(dir, 'server.properties'), [
    'online-mode=false',
    'spawn-protection=0',
    'max-players=8',
    'view-distance=8',
    'simulation-distance=8',
    'level-type=flat',
    'generator-settings=' + FLAT_GENERATOR_SETTINGS,
    'spawn-monsters=false',
    'spawn-animals=false',
    'spawn-npcs=false',
    'gamemode=survival',
    'difficulty=normal',
    'motd=cdn-e2e',
    'enable-command-block=false',
    'sync-chunk-writes=false',
    'enable-rcon=true',
    'rcon.port=25575',
    'rcon.password=' + RCON_PASSWORD,
    'broadcast-rcon-to-ops=true'
  ].join('\n') + '\n');

  fs.writeFileSync(path.join(dir, 'ops.json'), JSON.stringify([
    { uuid: offlineUuid(botName), name: botName, level: 4, bypassesPlayerLimit: true }
  ], null, 2));

  console.log(`[prepare] bot ${botName} pre-opped as ${offlineUuid(botName)}`);
  console.log('[prepare] done');
}

main().catch(error => {
  console.error('[prepare] FAILED: ' + error.message);
  process.exit(1);
});
