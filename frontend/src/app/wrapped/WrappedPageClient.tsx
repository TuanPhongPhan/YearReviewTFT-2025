"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import WrappedStory from "@/components/WrappedStory";
import { buildCards, type WrappedApiResponse } from "@/lib/wrapped";

export default function WrappedPageClient() {
    const sp = useSearchParams();
    const router = useRouter();

    const puuid = sp.get("puuid");
    const year = sp.get("year");

    const [data, setData] = useState<WrappedApiResponse | null>(null);
    const [error, setError] = useState<string | null>(null);

    const [cards, setCards] = useState<any[] | null>(null);
    const [cardsError, setCardsError] = useState<string | null>(null);

    const yearNum = useMemo(() => {
        const n = Number(year);
        return Number.isFinite(n) ? n : null;
    }, [year]);

    // Fetch wrapped data
    useEffect(() => {
        if (!puuid || !yearNum) {
            setError("Missing puuid or year in URL.");
            setData(null);
            return;
        }

        setError(null);
        setData(null);
        setCards(null);
        setCardsError(null);

        fetch(
            `/api/wrapped?puuid=${encodeURIComponent(
                puuid
            )}&year=${encodeURIComponent(String(yearNum))}`
        )
            .then(async (res) => {
                if (!res.ok) throw new Error(await res.text());
                return res.json();
            })
            .then((json: WrappedApiResponse) => setData(json))
            .catch((e) => setError(e?.message ?? "Failed to load wrapped"));
    }, [puuid, yearNum]);

    // Build cards (async) once data is ready
    useEffect(() => {
        let cancelled = false;

        async function run() {
            if (!data || !data.ready) {
                setCards(null);
                return;
            }

            try {
                setCardsError(null);
                const built = await buildCards(data); // buildCards is async now
                if (!cancelled) setCards(built as any[]);
            } catch (e: any) {
                if (!cancelled) setCardsError(e?.message ?? "Failed to build cards");
            }
        }

        run();
        return () => {
            cancelled = true;
        };
    }, [data]);

    // ---- Rendering ----

    if (error) {
        return (
            <main className="min-h-[100svh] grid place-items-center bg-black text-white px-6 text-center">
                <div className="max-w-xl">
                    <div className="text-2xl font-black">Couldn’t load your Wrapped</div>
                    <div className="mt-3 text-white/70 break-words">{error}</div>
                    <button
                        className="mt-6 rounded-2xl bg-white/10 hover:bg-white/15 border border-white/10 px-5 py-3 transition"
                        onClick={() => router.push("/")}
                    >
                        Back to search
                    </button>
                </div>
            </main>
        );
    }

    if (!data) {
        return (
            <main className="min-h-[100svh] grid place-items-center bg-black text-white">
                <div className="text-white/80">Loading your timewarp…</div>
            </main>
        );
    }

    if (!data.ready) {
        return (
            <main className="min-h-[100svh] grid place-items-center bg-black text-white px-6 text-center">
                <div className="max-w-xl">
                    <div className="text-2xl font-black">Preparing your Wrapped…</div>
                    <div className="mt-3 text-white/70">
                        Still cooking. This usually takes a bit for first-time runs.
                    </div>
                </div>
            </main>
        );
    }

    if (cardsError) {
        return (
            <main className="min-h-[100svh] grid place-items-center bg-black text-white px-6 text-center">
                <div className="max-w-xl">
                    <div className="text-2xl font-black">Almost there…</div>
                    <div className="mt-3 text-white/70 break-words">{cardsError}</div>
                    <button
                        className="mt-6 rounded-2xl bg-white/10 hover:bg-white/15 border border-white/10 px-5 py-3 transition"
                        onClick={() => window.location.reload()}
                    >
                        Retry
                    </button>
                </div>
            </main>
        );
    }

    if (!cards) {
        return (
            <main className="min-h-[100svh] grid place-items-center bg-black text-white">
                <div className="text-white/80">Finalizing your Wrapped…</div>
            </main>
        );
    }

    return <WrappedStory cards={cards} />;
}
