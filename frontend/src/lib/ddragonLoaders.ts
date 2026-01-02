import { ddragon } from "./ddragon";

type DDragonEntry = {
    name: string;
    image: { full: string };
};

type ChampionEntry = {
    id: string;
    name: string;
    image?: { full: string };
};

export async function loadItemMap() {
    const res = await fetch(ddragon.itemData, {
        cache: "force-cache",
    });
    const json = await res.json();
    return json.data as Record<string, DDragonEntry>;
}

export async function loadTraitMap() {
    const res = await fetch(ddragon.traitData, {
        cache: "force-cache",
    });
    const json = await res.json();
    return json.data as Record<string, DDragonEntry>;
}

export async function loadChampionMap() {
    const res = await fetch(ddragon.champData, { cache: "force-cache" });
    if (!res.ok) throw new Error("Failed to load champion data");
    const json = await res.json();

    const raw: Record<string, ChampionEntry> = json.data ?? {};

    // Build by ID map
    const byId: Record<string, ChampionEntry> = {};
    for (const entry of Object.values(raw)) {
        if (entry?.id) byId[entry.id] = entry;
    }
    return byId;
}

