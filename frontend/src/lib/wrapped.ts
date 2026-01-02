import { loadTraitMap, loadChampionMap } from "./ddragonLoaders";
import { ddragon } from "./ddragon";

export type WrappedApiResponse = {
    ready: boolean;
    puuid: string;
    year: number;
    gamesPlayed: number;
    avgPlacement: number;
    top4Rate: number;
    placements: Record<string, number>;
    topTraits: { name: string; count: number }[];
    topUnits: { name: string; count: number }[];
    bestGame: { level: number; placement: number; goldLeft: number; matchId: string };
    worstGame: { level: number; placement: number; goldLeft: number; matchId: string };
};

export type WrappedListItem = {
    id: string;
    label: string;
    icon?: string;
};

export type WrappedCard =
    | { id: string; type: "cover"; title: string; bg: string }
    | { id: string; type: "stat"; title: string; headline: string; subtext: string; bg: string; value?: string | number, kicker?: string }
    | { id: string; type: "list"; title: string; headline: string; subtext: string; items: WrappedListItem[]; bg: string }
    | { id: string; type: "highlight"; title: string; headline: string; subtext: string; meta: WrappedListItem[]; bg: string }
    | { id: string; type: "outro"; title: string; headline: string; subtext: string; bg: string };

function pct(n: number) {
    return `${Math.round(n * 100)}%`;
}

function ordinal(n: number) {
    const s = ["th", "st", "nd", "rd"];
    const v = n % 100;
    return `${n}${s[(v - 20) % 10] || s[v] || s[0]}`;
}

function humanizeTrait(id: string) {
    const x = id.replace(/^TFT\d+_/, "");
    return x.replace(/([a-z])([A-Z])/g, "$1 $2");
}

function traitIcon(traitId: string, traitMap: any): string | undefined {
    const entry = traitMap?.[traitId];
    if (!entry?.image?.full) return undefined;

    return ddragon.traitImg(entry.image.full);
}

function humanizeUnit(id: string) {
    return id.replace(/^TFT\d+_/, "").replace(/([a-z])([A-Z])/g, "$1 $2");
}

function champIcon(champId: string, champMap: any): string | undefined {
    const entry = champMap?.[champId];
    if (!entry?.image?.full) return undefined;
    return ddragon.champImg(entry.image.full);
}

export async function buildCards(data: WrappedApiResponse): Promise<WrappedCard[]> {
    const gamesPerDay = data.gamesPlayed / 365;

    const secondCount = Number(data.placements["2"] ?? 0);
    const firstCount = Number(data.placements["1"] ?? 0);
    const eighthCount = Number(data.placements["8"] ?? 0);

    // Placement “personality”
    const placementsEntries = Object.entries(data.placements ?? {}).map(
        ([k, v]) => [Number(k), Number(v)] as const
    );
    const mostCommon = placementsEntries.reduce(
        (best, cur) => (cur[1] > best[1] ? cur : best),
        [4, Number(data.placements?.["4"] ?? 0)] as const
    );
    const mostCommonPlace = mostCommon[0];
    const mostCommonCount = mostCommon[1];

    const topHalf = [1, 2, 3, 4].reduce((acc, p) => acc + Number(data.placements?.[String(p)] ?? 0), 0);
    const botHalf = [5, 6, 7, 8].reduce((acc, p) => acc + Number(data.placements?.[String(p)] ?? 0), 0);
    const topHalfRate = data.gamesPlayed ? topHalf / data.gamesPlayed : 0;

    // Background gradients
    const bgs = [
        "bg-gradient-to-br from-orange-600 via-purple-700 to-indigo-800",
        "bg-gradient-to-br from-rose-600 via-orange-500 to-amber-400",
        "bg-gradient-to-br from-emerald-500 via-teal-650 to-cyan-700",
        "bg-gradient-to-br from-sky-500 via-blue-700 to-violet-800",
        "bg-gradient-to-br from-zinc-900 via-slate-900 to-black",
    ];

    const [traitMap, champMap] = await Promise.all([loadTraitMap(), loadChampionMap()]);

    const unitItems: WrappedListItem[] = (data.topUnits ?? []).slice(0, 8).map((u, idx) => ({
        id: `units-item-${idx}-${u.name}`,
        label: `${humanizeUnit(u.name)} — ${u.count}`,
        icon: champIcon(u.name, champMap),
    }));

    function gamesTitle(games: number) {
        if (games > 800) return "Built different";
        if (games > 400) return "Locked in";
        if (games > 150) return "Consistent climb";
        return "Just getting started";
    }

    function gamesIntroSubtitle(games: number): string {
        if (games >= 1000) {
            return "You didn’t log in. You clocked in.";
        }

        if (games >= 600) {
            return "This wasn’t a phase. It was a lifestyle.";
        }

        if (games >= 400) {
            return "Consistency is a skill. You mastered it.";
        }

        if (games >= 250) {
            return "You kept coming back — and it showed.";
        }

        if (games >= 150) {
            return "Not every day. But often enough to matter.";
        }

        if (games >= 75) {
            return "Enough games to leave a mark.";
        }

        return "Every journey starts somewhere.";
    }


    function unitSubtitle(units: { name: string; count: number }[], gamesPlayed: number) {
        if (!units.length || !gamesPlayed) return "Your most trusted champions this year.";

        const top = units[0];
        const second = units[1];
        const pct = top.count / gamesPlayed;

        if (pct > 0.25) {
            return `You kept coming back to ${humanizeUnit(top.name)}. Loyalty matters.`;
        }

        if (second && top.count - second.count < 10) {
            return `You rotated your carries — flexibility over commitment.`;
        }

        return `${humanizeUnit(top.name)} led the way, but you weren’t afraid to adapt.`;
    }

    function traitSubtitle(
        traits: { name: string; count: number }[],
        gamesPlayed: number
    ) {
        if (!traits.length || !gamesPlayed) {
            return "The traits that shaped your comps.";
        }

        const top = traits[0];
        const second = traits[1];
        const pct = top.count / gamesPlayed;

        if (pct > 0.3) {
            return `${humanizeTrait(top.name)} was your foundation. You built around it again and again.`;
        }

        if (second && top.count - second.count < 10) {
            return `You flexed between traits — adaptability over commitment.`;
        }

        return `${humanizeTrait(top.name)} led your comps, but you stayed flexible.`;
    }

    function avgPlacementSubtitle(avgPlacement: number) {
        if (!Number.isFinite(avgPlacement)) {
            return "Your average finish across the year.";
        }

        if (avgPlacement <= 3.8) {
            return "You consistently played for the top — results followed.";
        }

        if (avgPlacement <= 4.5) {
            return "Solid finishes, steady decisions.";
        }

        if (avgPlacement <= 5.2) {
            return "You took risks. Sometimes they paid off.";
        }

        return "High risk, high variance — not every game was safe.";
    }

    function gamesPerDaySubtitle(gamesPerDay: number) {
        if (!Number.isFinite(gamesPerDay)) {
            return "How often you queued up this year.";
        }

        if (gamesPerDay >= 6) {
            return "TFT was basically a daily ritual.";
        }

        if (gamesPerDay >= 3) {
            return "You checked in almost every day.";
        }

        if (gamesPerDay >= 1) {
            return "A steady part of your routine.";
        }

        return "You played when it felt right.";
    }

    function lobbySplitSubtitle(topHalf: number, botHalf: number, rate: number) {
        if (rate >= 0.6) {
            return `Top 4: ${topHalf} • Bottom 4: ${botHalf}\nYou controlled the lobby`;
        }

        if (rate >= 0.53) {
            return `Top 4: ${topHalf} • Bottom 4: ${botHalf}\nMore pressure than excuses`;
        }

        if (rate >= 0.5) {
            return `Top 4: ${topHalf} • Bottom 4: ${botHalf}\nJust enough to stay dangerous`;
        }

        return `Top 4: ${topHalf} • Bottom 4: ${botHalf}\nHigh risk, high variance`;
    }


    function secondPlaceSubtitle(count: number) {
        if (count >= 60) {
            return "You lived on the edge — one fight from victory, again and again.";
        }
        if (count >= 40) {
            return "Final boss energy… just one round short.";
        }
        if (count >= 20) {
            return "Close calls were kind of your thing.";
        }
        return "When you lost, it was rarely by much.";
    }

    function secondPlaceChip(count: number) {
        if (count >= 50) return "One fight away";
        if (count >= 30) return "Final round regular";
        return "Top 2 finishes";
    }


    function clamp(n: number, lo: number, hi: number) {
        return Math.max(lo, Math.min(hi, n));
    }

    // A tiny deterministic selector so it feels varied per match without randomness.
    function pick<T>(arr: T[], seed: number): T {
        return arr[((seed % arr.length) + arr.length) % arr.length];
    }

    function peakMomentLabel(m: { level: number; placement: number; goldLeft: number; matchId: string }) {
        // seed derived from matchId so it stays stable for the same match
        const seed = Array.from(m.matchId).reduce((a, ch) => a + ch.charCodeAt(0), 0);

        const lvl = m.level ?? 0;
        const p = m.placement ?? 0;
        const g = m.goldLeft ?? 0;

        // Strong signals first
        if (p === 1 && lvl >= 9 && g <= 10) {
            return pick(
                [
                    "The final roll down",
                    "Endgame spike",
                    "All-in, all hit",
                    "Perfect cash-out",
                    "The lobby ended here",
                ],
                seed
            );
        }

        if (p === 1 && g >= 30) {
            return pick(
                [
                    "Rich AND winning",
                    "Econ diff",
                    "Saved gold, still won",
                    "Luxury victory",
                ],
                seed
            );
        }

        if (lvl >= 10) {
            return pick(
                [
                    "Level 10 supremacy",
                    "Capped board moment",
                    "Exodia energy",
                    "Late-game masterpiece",
                ],
                seed
            );
        }

        if (p <= 2) {
            return pick(
                [
                    "Clean execution",
                    "Top 2 aura",
                    "Calculated finish",
                    "Everything clicked",
                ],
                seed
            );
        }

        return pick(["A good day in queue", "Momentum game", "The one that felt easy"], seed);
    }

    function rockBottomLabel(m: { level: number; placement: number; goldLeft: number; matchId: string }) {
        const seed = Array.from(m.matchId).reduce((a, ch) => a + ch.charCodeAt(0), 0);

        const lvl = m.level ?? 0;
        const p = m.placement ?? 0;
        const g = m.goldLeft ?? 0;

        if (p === 8 && g >= 30) {
            return pick(
                [
                    "Died rich",
                    "Econ with no time",
                    "Greed punished",
                    "Bank account, no HP",
                ],
                seed
            );
        }

        if (p === 8 && lvl <= 6) {
            return pick(
                [
                    "Never stabilized",
                    "Fast 8 incident",
                    "Rough early game",
                    "Went next speedrun",
                ],
                seed
            );
        }

        if (p >= 7) {
            return pick(
                [
                    "One of those games",
                    "Couldn’t find the angle",
                    "Tilt queue moment",
                    "Bad RNG allegations",
                ],
                seed
            );
        }

        return pick(["Not your cleanest", "We learn and queue again"], seed);
    }

    function lobbySplitCopy(topHalfRate: number) {
        if (topHalfRate >= 0.6) {
            return {
                headline: "You controlled the lobby",
                subtext: "Winning positions were the norm."
            };
        }

        if (topHalfRate >= 0.53) {
            return {
                headline: "You showed up to win",
                subtext: "More pressure than excuses."
            };
        }

        if (topHalfRate >= 0.5) {
            return {
                headline: "On the right side",
                subtext: "Just enough top finishes to matter."
            };
        }

        return {
            headline: "High risk, high variance",
            subtext: "Some days you carried. Some days you learned."
        };
    }

    function winsSubtitle(firsts: number, games: number): string {
        const rate = firsts / Math.max(1, games);

        if (rate >= 0.12) {
            return `Winrate ${(rate * 100).toFixed(1)}%. When you hit first, the lobby knows it’s over.`;
        }

        if (rate >= 0.08) {
            return `Winrate ${(rate * 100).toFixed(1)}%. You didn’t win often — but when you did, it was loud.`;
        }

        if (rate >= 0.05) {
            return `Winrate ${(rate * 100).toFixed(1)}%. Clutch victories, perfectly timed.`;
        }

        return `Winrate ${(rate * 100).toFixed(1)}%. Every win felt earned.`;
    }

    function lowsSubtitle(eighths: number, games: number): string {
        const rate = eighths / Math.max(1, games);

        if (rate >= 0.15) {
            return "Because TFT isn’t a game. It’s character development.";
        }

        if (rate >= 0.1) {
            return "Some games were unwinnable. You queued again anyway.";
        }

        if (rate >= 0.05) {
            return "Not every lobby was kind. You survived them all.";
        }

        return "Even your bad games didn’t last long.";
    }


    const traitItems: WrappedListItem[] = (data.topTraits ?? []).slice(0, 8).map((t, idx) => ({
        id: `traits-item-${idx}-${t.name}`, // unique even if same trait repeats
        label: `${humanizeTrait(t.name)} — ${t.count} games`,
        icon: traitIcon(t.name, traitMap),
    }));

    const bestMeta: WrappedListItem[] = [
        { id: `best-meta-0`, label: peakMomentLabel(data.bestGame) },
    ];

    const worstMeta: WrappedListItem[] = [
        { id: `worst-meta-0`, label: rockBottomLabel(data.worstGame) },
    ];



    return [
        {
            id: "cover",
            type: "cover",
            title: `Your 2025 TFT Wrapped`,
            bg: bgs[0],
        },
        {
            id: "time",
            type: "stat",
            title: gamesTitle(data.gamesPlayed),
            headline: `${data.gamesPlayed} games`,
            subtext: gamesIntroSubtitle(data.gamesPlayed),
            bg: bgs[1],
        },
        {
            id: "comfort",
            type: "stat",
            title: "Comfort zone",
            headline: `You lived in ${ordinal(mostCommonPlace)}`,
            subtext: `${mostCommonCount} times. That place had your name on it.`,
            bg: bgs[3],
        },
        {
            id: "grind",
            type: "stat",
            title: "The grind",
            headline: `${gamesPerDay.toFixed(1)} games / day`,
            subtext: gamesPerDaySubtitle(gamesPerDay),
            bg: bgs[4],
        },
        {
            id: "avg",
            type: "stat",
            title: "Consistency",
            headline: `${data.avgPlacement.toFixed(2)} avg`,
            subtext: avgPlacementSubtitle(data.avgPlacement),
            bg: bgs[2],
        },
        {
            id: "split",
            type: "stat",
            title: "Lobby split",
            headline: `${pct(topHalfRate)} top-half`,
            subtext: lobbySplitSubtitle(topHalf, botHalf, topHalfRate),
            bg: bgs[3],
        },
        {
            id: "podium",
            type: "stat",
            title: "Podium magnet",
            headline: secondPlaceChip(secondCount),
            value: `2nd × ${secondCount}`,
            subtext: secondPlaceSubtitle(secondCount),
            bg: bgs[0],
        },
        {
            id: "wins",
            type: "stat",
            title: "The highs",
            headline: `${firstCount} firsts`,
            subtext: winsSubtitle(firstCount, data.gamesPlayed),
            bg: bgs[1],
        },
        {
            id: "lows",
            type: "stat",
            title: "The lows",
            headline: `${eighthCount} eighths`,
            subtext: lowsSubtitle(eighthCount, data.gamesPlayed),
            bg: bgs[4],
        },
        {
            id: "best",
            type: "highlight",
            title: "Peak moment",
            headline: `Lvl ${data.bestGame.level} • #${data.bestGame.placement}`,
            subtext: `${data.bestGame.goldLeft} gold left. You didn’t win — you ended it.`,
            meta: bestMeta,
            bg: bgs[1],
        },
        {
            id: "worst",
            type: "highlight",
            title: "Rock bottom",
            headline: `Lvl ${data.worstGame.level} • #${data.worstGame.placement}`,
            subtext: `${data.worstGame.goldLeft} gold. Some of us just have receipts.`,
            meta: worstMeta,
            bg: bgs[4],
        },
        {
            id: "units",
            type: "list",
            title: "Signature units",
            headline: "Your top units",
            subtext: unitSubtitle(data.topUnits ?? [], data.gamesPlayed),
            items: unitItems,
            bg: bgs[2],
        },
        {
            id: "traits",
            type: "list",
            title: "Fortress comps",
            headline: "Your top traits",
            subtext: traitSubtitle(data.topTraits ?? [], data.gamesPlayed),
            items: traitItems,
            bg: bgs[3],
        },
        {
            id: "outro",
            type: "outro",
            title: "See you in queue",
            headline: "Turn 2nds into 1sts",
            subtext: "2026 is your conversion arc.",
            bg: bgs[1],
        },
    ];
}
