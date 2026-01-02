"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

type RequestResponse = {
    puuid: string;
    year: number;
    jobId: string | null;
    state: string;
};

type StatusResponse = {
    puuid: string;
    year: number;
    state: string;
    matchIdsFound: number;
    matchesCached: number;
    summaryReady: boolean;
    message: string;
};

type ApiError = {
    message?: string;
    error?: string;
    status?: number;
}

function parseApiError(err: any): string {
    // err could be a string, Error, or ApiJson

    if (!err) return "Something went wrong";

    if (typeof err == "string") return err.replace(
        /from GET https?:\/\/[^\s]+/i,
        ""
    ).trim();

    if(err?.message && typeof err.message === "string") return err.message;

    // fetch error payload
    if (err?.error?.message && typeof err.error.message === "string") return err.error.message;

    return "Something went wrong";
}

function friendlyErrorMessage(msg: string): { title: string; body: string } {
    const lower = msg.toLowerCase();

    if (lower.includes("riotid must be in format")) {
        return {
            title: "Riot ID format",
            body: "Please make sure your Riot ID is in the correct format.",
        };
    }

    if (lower.includes("not found")) {
        return {
            title: "Player not found",
            body: "We couldn't find a player with that Riot ID. Make sure it’s written as GameName#TAG.",
        };
    }

    if (lower.includes("rate") && lower.includes("limit")) {
        return {
            title: "Too many requests",
            body: "Riot API is throttling us for a moment. Try again in a bit.",
        };
    }

    return {
        title: "Something went wrong",
        body: msg,
    };
}

function isValidRiotId(s: string) {
    return /^[^#]{3,16}#[A-Za-z0-9]{2,5}$/.test(s.trim());
}

function sleep(ms: number) {
    return new Promise((r) => setTimeout(r, ms));
}

export default function StartPageClient() {
    const sp = useSearchParams();
    const router = useRouter();

    const riotId = sp.get("riotId") ?? "";
    const year = useMemo(() => Number(sp.get("year") ?? "2025"), [sp]);

    const [stage, setStage] = useState<"REQUESTING" | "POLLING" | "DONE" | "ERROR">("REQUESTING");
    const [info, setInfo] = useState<string>("Starting…");
    const [status, setStatus] = useState<StatusResponse | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function readErrorPayload(res: Response): Promise<any> {
            const ct = res.headers.get("content-type") || "";
            try {
                if (ct.includes("application/json")) return await res.json();
                return await res.text();
            } catch {
                return await res.text().catch(() => "");
            }
        }

        async function run() {
            try {
                if (!riotId) {
                    if (!isValidRiotId(riotId)) {
                        setStage("ERROR");
                        setInfo("RiotId must be in format GameName#TAG");
                        return;
                    }
                }

                setStage("REQUESTING");
                setInfo("Starting your Wrapped…");

                // 1) Start/Resume job
                const reqBody = { riotId, year, platform: "EUW1" }; // adjust platform if needed
                const requestRes = await fetch(`/api/wrapped/request`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(reqBody),
                });

                if (!requestRes.ok) {
                    const payload = await readErrorPayload(requestRes);
                    throw payload;
                }

                const reqJson = (await requestRes.json()) as RequestResponse;
                if (cancelled) return;

                // If backend says DONE, we can jump straight
                if (reqJson.state === "DONE") {
                    setStage("DONE");
                    router.replace(`/wrapped?puuid=${encodeURIComponent(reqJson.puuid)}&year=${encodeURIComponent(String(year))}`);
                    return;
                }

                setStage("POLLING");
                setInfo("Crunching your matches…");

                // 2) Poll status until ready
                const puuid = reqJson.puuid;

                while (!cancelled) {
                    const stRes = await fetch(
                        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/wrapped/status?puuid=${encodeURIComponent(puuid)}&year=${encodeURIComponent(
                            String(year)
                        )}`
                    );

                    if (!stRes.ok) {
                        const payload = await readErrorPayload(stRes);
                        throw payload;
                    }

                    const stJson = (await stRes.json()) as StatusResponse;
                    if (cancelled) return;

                    setStatus(stJson);

                    // if ready -> go to /wrapped
                    if (stJson.summaryReady || stJson.state === "DONE") {
                        setStage("DONE");
                        router.replace(`/wrapped?puuid=${encodeURIComponent(puuid)}&year=${encodeURIComponent(String(year))}`);
                        return;
                    }

                    // If failed -> show error
                    if (stJson.state === "FAILED") {
                        throw new Error(stJson.message || "Job failed.");
                    }

                    setInfo(stJson.message || stJson.state);

                    // Adaptive polling delay
                    await sleep(1200);
                }
            } catch (e: any) {
                if (cancelled) return;
                const msg = parseApiError(e);
                setStage("ERROR");
                setInfo(msg);
            }
        }

        run();

        return () => {
            cancelled = true;
        };
    }, [riotId, year, router]);

    return (
        <main className="min-h-[100svh] bg-black text-white grid place-items-center px-6">
            <div className="w-full max-w-xl">
                <div className="rounded-3xl border border-white/10 bg-white/5 p-6 sm:p-8">
                    <div className="text-sm text-white/60">Timewarp</div>

                    {stage === "POLLING" && (
                        <div className="mt-6 space-y-3 text-white/80">
                            <div className="text-sm">
                                Riot ID: <span className="text-white">{riotId}</span>
                            </div>

                            <div className="h-2 w-full rounded-full bg-white/10 overflow-hidden">
                                <div
                                    className="h-full bg-white/60 transition-all"
                                    style={{
                                        width: status
                                            ? `${Math.min(
                                                95,
                                                Math.round(
                                                    (status.matchesCached / Math.max(1, status.matchIdsFound || 1)) * 100
                                                )
                                            )}%`
                                            : "10%",
                                    }}
                                />
                            </div>

                            <div className="text-sm text-white/70">
                                Cached {status?.matchesCached ?? 0} / {status?.matchIdsFound ?? 0} matches
                            </div>

                            <div className="text-xs text-white/50">
                                State: {status?.state ?? "—"}
                            </div>
                        </div>
                    )}

                    {stage === "ERROR" && (
                        <div className="mt-6">
                            {(() => {
                                const ui = friendlyErrorMessage(info);

                                const showRiotHints =
                                    ui.title === "Player not found" || ui.title === "Riot ID format";

                                return (
                                    <>
                                        {/* main headline */}
                                        <div className="mt-2 text-3xl sm:text-4xl font-black tracking-tight">
                                            {ui.title}
                                        </div>

                                        {/* friendly explanation */}
                                        <div className="mt-3 text-white/75">{ui.body}</div>

                                        {/* extra hints */}
                                        {showRiotHints && (
                                            <ul className="mt-4 text-sm text-white/60 space-y-1">
                                                <li>• Use the format <span className="text-white/80 font-semibold">GameName#TAG</span></li>
                                                <li>• The <span className="text-white/80 font-semibold">#TAG</span> is required</li>
                                            </ul>
                                        )}

                                        {/* dev details (optional) */}
                                        <details className="mt-5 text-xs text-white/40">
                                            <summary className="cursor-pointer select-none hover:text-white/60">
                                                Technical details
                                            </summary>
                                            <pre className="mt-2 whitespace-pre-wrap break-words text-white/50">
                                                {info}
                                            </pre>
                                        </details>

                                        <div className="mt-6 flex gap-3">
                                            <button
                                                className="rounded-2xl bg-white/10 hover:bg-white/15 border border-white/10 px-5 py-3 transition"
                                                onClick={() => router.push("/")}
                                            >
                                                Back to search
                                            </button>
                                        </div>
                                    </>
                                );
                            })()}
                        </div>
                    )}


                    {(stage === "REQUESTING" || stage === "DONE") && (
                        <div className="mt-6 text-white/60 text-sm">Please keep this tab open.</div>
                    )}
                </div>
            </div>
        </main>
    );
}
