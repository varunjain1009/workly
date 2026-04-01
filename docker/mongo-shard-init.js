/**
 * mongo-shard-init.js
 *
 * Run this script against a mongos router AFTER the sharded cluster is
 * bootstrapped and the replica sets are added as shards.
 *
 * Usage:
 *   mongosh --host <mongos-host>:27017 mongo-shard-init.js
 *
 * Prerequisites:
 *   1. mongos is running and at least one shard (rs0) is registered
 *   2. The `workly` database and collections already exist (start the
 *      application once in monolith mode so Spring auto-creates indexes)
 *
 * What this does:
 *   - Enables sharding on the `workly` database
 *   - Shards `jobs` on { region: 1, _id: 1 }
 *   - Shards `worker_profiles` on { region: 1, _id: 1 }
 *   - Pre-splits common Indian city regions to avoid the "hot shard at
 *     startup" problem where all initial writes go to one chunk
 */

// ── Enable database sharding ─────────────────────────────────────────────────
sh.enableSharding("workly");

// ── Shard: jobs ──────────────────────────────────────────────────────────────
// Shard key: { region: 1, _id: 1 }
//   - `region` provides coarse locality (1°×1° cell ≈ 111 km)
//   - `_id` appended to make the key unique and avoid hotspots within a region
sh.shardCollection("workly.jobs", { region: 1, _id: 1 });

// Pre-split for major Indian metro regions (lat_lon grid cells)
// Mumbai area
sh.splitAt("workly.jobs", { region: "18_72", _id: MinKey });
// Delhi area
sh.splitAt("workly.jobs", { region: "28_77", _id: MinKey });
// Bengaluru area
sh.splitAt("workly.jobs", { region: "12_77", _id: MinKey });
// Chennai area
sh.splitAt("workly.jobs", { region: "13_80", _id: MinKey });
// Hyderabad area
sh.splitAt("workly.jobs", { region: "17_78", _id: MinKey });
// Pune area
sh.splitAt("workly.jobs", { region: "18_73", _id: MinKey });
// Kolkata area
sh.splitAt("workly.jobs", { region: "22_88", _id: MinKey });

// ── Shard: worker_profiles ───────────────────────────────────────────────────
sh.shardCollection("workly.worker_profiles", { region: 1, _id: 1 });

sh.splitAt("workly.worker_profiles", { region: "18_72", _id: MinKey }); // Mumbai
sh.splitAt("workly.worker_profiles", { region: "28_77", _id: MinKey }); // Delhi
sh.splitAt("workly.worker_profiles", { region: "12_77", _id: MinKey }); // Bengaluru
sh.splitAt("workly.worker_profiles", { region: "13_80", _id: MinKey }); // Chennai
sh.splitAt("workly.worker_profiles", { region: "17_78", _id: MinKey }); // Hyderabad

// ── Verify ───────────────────────────────────────────────────────────────────
print("\n=== Shard status ===");
sh.status();
