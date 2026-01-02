"use client";

import Image from "next/image";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";

export default function Landing() {
    const router = useRouter();
    const [riotId, setRiotId] = useState("");
    const [multi, setMulti] = useState(false);

    const canSubmit = useMemo(() => riotId.trim().length >= 3, [riotId]);

    function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!canSubmit) return;

        const params = new URLSearchParams();
        params.set("riotId", riotId.trim());
        if (multi) params.set("multi", "1");

        params.set("year", "2025");
        router.push(`/start?${params.toString()}`);

    }

    return (
        <main className="relative min-h-[100svh] w-full overflow-hidden bg-black text-white">
            <Image src="/bg/landing-bg1.jpg" alt="" fill priority className="object-cover" />

            <div className="absolute inset-0 bg-black/55" />
            <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/60" />

            <header className="relative z-10 flex items-center justify-between px-6 py-5 sm:px-10">
                <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-2xl bg-white/10 border border-white/10 grid place-items-center">
                        <span className="text-lg font-black">⏱</span>
                    </div>
                    <div>
                        <div className="text-sm font-semibold tracking-tight">YearReviewTFT</div>
                        <div className="text-xs text-white/60 -mt-0.5">your TFT timewarp</div>
                    </div>
                </div>
            </header>

            <section className="relative z-10 px-6 sm:px-10">
                <div className="mx-auto max-w-3xl pt-14 sm:pt-20">

                    <h1 className="text-center text-4xl sm:text-6xl font-black tracking-tight leading-[1.05]">
                        Search your profile
                    </h1>

                    <p className="mt-4 text-center text-white/75 text-base sm:text-lg">
                        Enter your Riot ID like <span className="font-semibold text-white">GameName#TAG</span>
                    </p>

                    <form onSubmit={onSubmit} className="mt-10">
                        <div className="mx-auto max-w-xl">
                            <div className="relative">
                                <input
                                    value={riotId}
                                    onChange={(e) => setRiotId(e.target.value)}
                                    placeholder="Your Riot ID"
                                    className="w-full rounded-2xl bg-white/10 backdrop-blur-[13px] border border-fuchsia-400/50 px-5 py-4 pr-12 text-white placeholder:text-white/65 caret-fuchsia-400 shadow-[0_0_25px_rgba(255,0,180,0.35)] ring-1 ring-fuchsia-400/40 focus:outline-none focus:ring-2 focus:ring-fuchsia-500/90 focus:border-fuchsia-400 focus:shadow-[0_0_35px_rgba(255,0,180,0.5)] transition-all"
                                />
                                <button
                                    type="submit"
                                    disabled={!canSubmit}
                                    aria-label="Search"
                                    className={
                                        "absolute right-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-xl grid place-items-center transition " +
                                        (canSubmit ? "bg-fuchsia-500/90 hover:bg-fuchsia-500" : "bg-white/10 cursor-not-allowed")
                                    }
                                >
                                    🔍
                                </button>
                            </div>

                            <div className="mt-6 flex items-center justify-center gap-4">
                                <button
                                    type="submit"
                                    disabled={!canSubmit}
                                    className={
                                        "h-12 px-8 rounded-2xl font-semibold transition shadow-lg " +
                                        (canSubmit ? "bg-fuchsia-600 hover:bg-fuchsia-500" : "bg-white/10 cursor-not-allowed text-white/50")
                                    }
                                >
                                    Let the timewarp begin
                                </button>
                            </div>

                            <div className="mt-8 text-center text-xs text-white/50">
                                By continuing you agree to the terms. Not affiliated with Riot Games.
                            </div>
                        </div>
                    </form>
                </div>
            </section>

            <div className="pointer-events-none absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-black/70 to-transparent" />
        </main>
    );
}
