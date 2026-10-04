# Doctor Anant's Mega Clinic & The Expired Mutant Outbreak — v2
NeoForge **1.21.1** · Java **21** · Mod ID `dranant`

```bash
./gradlew runClient     # dev client
./gradlew build         # -> build/libs/dranant-2.0.0.jar
```
If Gradle can't find a version, bump `neo_version` (gradle.properties) / `net.neoforged.moddev` (build.gradle) to the newest 21.1.x / 2.0.x.

## What's new in v2
**Villagers / patients**
* Clinic customers are now `Patient Villager` entities (vanilla villager look, biome + profession skins) driven 100% by the clinic AI → they always walk INTO the clinic and form neat straight lines (1.2 blocks apart). Stuck patients hop to their spot.
* Every patient says its own problem in a speech bubble (fever, golgappe stomach ache, skeleton arrow, Warden...). Mild → Line 1 medicine. Severe → Line 2: they **lie on stretchers**, then on the **Examination Bed**.
* At the counter they argue and haggle with the pharmacist (~9 s of dialogue), pay, get medicine and leave healthy.
* Patients choose the **closest clinic**. Nearby vanilla villagers are converted into patients and turned back into the same villager (trades kept) when they leave.
* Assistants get random names (Raju, Pappu, Dr. Sneha...). The shop has a **Seller** (Seth Dhaniram etc.) who randomly talks to you — sometimes friendly, sometimes rude.

**Clinics**
* Only **6 levels**, ordered small → huge: 1 Village Dispensary, 2 Community Clinic, 3 Health Centre (procedural), 4 City Hospital, 5 Medical College, 6 Mega Hospital (your .nbt files, re-ordered by size).
* **Right-click** with a summoner = build the clinic **7 blocks in front of you**, entrance facing you. **Left-click** = place only the block (right-click that block later to build).
* Buildings rise **layer by layer** with sounds, particles and fireworks.

**Shop**
* 19×7×19 Central Shop: 14 medicine/tool pedestals + **Clinic Upgrades gallery** (Level 1-6: 5K / 20K / 60K / 150K / 350K / 750K — all under 1 million).
* Right-click a clinic upgrade → green **preview outline** in front of you (follows where you look) → right-click again within 6 s to **buy & build**.
* Hologram fix: only the pedestal you look at shows the full name/price card (it lifts and spins faster); others show a small price tag — no more overlapping text.
* **Shift + right-click** any (non-shop) pedestal with an item to display it, empty hand to take it back. `/pedestalprice <amount>` changes a price.

**Diamond Counter = Diamond Bank**
* `#run diamond counter` → picked-up diamonds disappear from your inventory and are added to the counter; the counter also grows non-stop on an accelerating curve and reaches **1,000,000 in 5 minutes** (configurable).
* Purchases are paid from the counter (then inventory). Rolling number HUD, live "+X/sec", pulse + green/red popups.
* `#stop diamond counter`, `#reset diamond counter`, `/diamondcounter run|stop|reset|set <n>`.

**Boss** — Phase 2 enrage below 50% HP (faster, shaking, shorter cooldowns), **Bull Charge** (gets stunned if it hits a wall), **Blood Roar** shockwave, **Blood Geysers** (dodge the red circles), 3 **Infected Patient** minions at 30% HP, speech bubbles, new animation poses.

**HD textures** — 64-128 px medicines/items/blocks, 128 px assistant & seller skins, 256 px boss, 512-1024 px posters with **your skin**.

## Notes
* Original clinic_level_1/2/3.nbt were empty, so tiers 1-3 are generated in code. Drop real files named `clinic_level_1.nbt`.. into `src/main/resources/data/dranant/structure/` to override.
* Structure "front" is assumed to be the template's south (+Z) side when turning it toward the player.
* Config: `config/dranant-common.toml` (customer spawn rate, queue length, passive growth target/time, auto-deposit).
* Model credits (CC-BY-4.0) in `CREDITS.txt`.
