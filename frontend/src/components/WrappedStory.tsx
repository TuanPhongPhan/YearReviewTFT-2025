"use client";

import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import useEmblaCarousel from "embla-carousel-react";
import {motion} from "framer-motion";
import type {WrappedCard} from "@/lib/wrapped";

function Dot({active}: { active: boolean }) {
    return (
        <div
            className={[
                "w-1.5 rounded-full transition-all",
                active ? "h-8 bg-white/90" : "h-2 bg-white/35",
            ].join(" ")}
        />
    );
}

function CardShell({bg, children}: { bg: string; children: React.ReactNode }) {
    return (
        <section className={`relative h-[100svh] w-full ${bg} text-white grain overflow-hidden`}>
            <div className="pointer-events-none absolute inset-0 bg-black/10" />
            <div className="relative h-full w-full px-5 py-6 sm:px-10 sm:py-10 flex flex-col">
                {children}
            </div>
        </section>
    );
}

function TitleBlock({title}: { title: string }) {
    return <div className="text-white/85 text-lg tracking-wide uppercase">{title}</div>;
}

function Headline({children}: { children: React.ReactNode }) {
    return (
        <div className="mt-3 font-black leading-[0.95] tracking-tight text-[clamp(2.2rem,8vw,4.5rem)] sm:text-7xl">
            {children}
        </div>
    );
}

function Subtext({children}: { children: React.ReactNode }) {
    return <div className="mt-4 text-lg sm:text-xl text-white/85 max-w-[34ch] whitespace-pre-line">{children}</div>;
}

function Chip({children}: { children: React.ReactNode }) {
    return (
        <div
            className="inline-flex items-center gap-2 rounded-full bg-white/10 border border-white/15 px-4 py-2 text-sm text-white/90 backdrop-blur">
            {children}
        </div>
    );
}

export default function WrappedStory({cards}: { cards: WrappedCard[] }) {
    const [emblaRef, emblaApi] = useEmblaCarousel({
        axis: "y",
        loop: false,
        duration: 22,
        // better story feel on touch
        dragFree: false,
    });

    const [index, setIndex] = useState(0);
    const total = cards.length;

    const onSelect = useCallback(() => {
        if (!emblaApi) return;
        setIndex(emblaApi.selectedScrollSnap());
    }, [emblaApi]);

    useEffect(() => {
        if (!emblaApi) return;
        onSelect();
        emblaApi.on("select", onSelect);
        return () => {
            emblaApi.off("select", onSelect);
        };
    }, [emblaApi, onSelect]);

    // Keyboard navigation
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (!emblaApi) return;
            if (e.key === "ArrowDown" || e.key === "PageDown" || e.key === " ") emblaApi.scrollNext();
            if (e.key === "ArrowUp" || e.key === "PageUp") emblaApi.scrollPrev();
        };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [emblaApi]);

    // Wheel navigation (debounced so it feels like story paging)
    const wheelLock = useRef(false);
    const onWheel = useCallback(
        (e: React.WheelEvent) => {
            if (!emblaApi) return;
            if (wheelLock.current) return;

            const dy = e.deltaY;
            if (Math.abs(dy) < 10) return;

            wheelLock.current = true;
            if (dy > 0) emblaApi.scrollNext();
            else emblaApi.scrollPrev();

            window.setTimeout(() => (wheelLock.current = false), 500);
        },
        [emblaApi]
    );

    const slides = useMemo(
        () =>
            cards.map((c, i) => (
                <div className="flex-[0_0_100%]" key={c.id}>
                    <CardShell bg={c.bg}>
                        <div className="flex items-start justify-end">
                            <div className="flex items-center gap-2">
                                <Chip>
                                    <span className="tabular-nums">{i + 1}</span>
                                    <span className="text-white/60">/</span>
                                    <span className="tabular-nums">{total}</span>
                                </Chip>
                            </div>
                        </div>

                        <div
                            className={
                                c.type === "stat"
                                    ? "flex flex-1 items-center"
                                    : "mt-6 flex flex-1 min-h-0"
                            }
                        >
                            <motion.div
                                initial={{opacity: 0, y: 16}}
                                animate={{opacity: 1, y: 0}}
                                transition={{duration: 0.5, ease: "easeOut"}}
                                className="w-full"
                            >
                                {c.type === "cover" ? (
                                    <div className="mt-10 sm:mt-14">
                                        {/* small kicker */}
                                        <div className="text-white/75 text-base tracking-[0.25em] uppercase">
                                            {c.title}
                                        </div>

                                        {/* hero */}
                                        <div
                                            className="mt-6 text-6xl sm:text-8xl font-black leading-[0.92] tracking-tight">
                                            Let’s rewind
                                            <span className="block text-white/85">your set.</span>
                                        </div>

                                        {/* sub */}
                                        <div
                                            className="mt-5 text-lg sm:text-xl text-white/85 max-w-[38ch] leading-relaxed">
                                            A full-year recap of your climbs, throws, comfort comps, and villain arcs.
                                        </div>

                                        {/* CTA */}
                                        <div className="mt-8">
                                            <div
                                                className="inline-flex items-center gap-2 rounded-full bg-white/12 border border-white/15 px-4 py-2 text-sm text-white/90 backdrop-blur">
                                                Swipe ↑ to start
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    <>
                                        <TitleBlock title={c.title}/>

                                        <Headline>{c.headline}</Headline>

                                        {"value" in c && c.type === "stat" && c.value && (() => {
                                            const valueStr =
                                                typeof c.value === "number" ? c.value.toString() : c.value;

                                            const [left, right] = valueStr.split("×");

                                            return (
                                                <div className="mt-4 flex items-baseline gap-3">
                                                    <div className="text-6xl sm:text-7xl font-black tracking-tight">
                                                        {left.trim()}
                                                    </div>
                                                    {right && (
                                                        <div className="text-4xl sm:text-5xl font-black text-white/80">
                                                            × {right.trim()}
                                                        </div>
                                                    )}
                                                </div>
                                            );
                                        })()}

                                        <Subtext>{c.subtext}</Subtext>

                                        {"items" in c && c.items && (
                                            <div className="mt-6 flex-1 min-h-0 overflow-y-auto overscroll-contain pr-1">
                                                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-3">
                                                    {c.items.map((it, idx) => {
                                                        const [name, rest] = it.label.split(" — ");
                                                        return (
                                                            <div
                                                                key={`${c.id}-${it.id}`}
                                                                className="group relative overflow-hidden rounded-2xl sm:rounded-3xl bg-white/10 border border-white/10 px-4 py-3 backdrop-blur
                                                                hover:bg-white/14 hover:border-white/20 transition-transform duration-300 hover:-translate-y-0.5"
                                                            >
                                                                <div className="pointer-events-none absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300
                                                                bg-gradient-to-r from-white/10 via-white/0 to-white/10" />

                                                                <div className="relative flex items-center gap-4">
                                                                    <div className="w-8 text-center text-sm font-semibold text-white/75 tabular-nums">
                                                                        #{idx + 1}
                                                                    </div>

                                                                    <div className="relative shrink-0">
                                                                        <div className="h-10 w-10 sm:h-12 sm:w-12 rounded-2xl bg-black/25 ring-1 ring-white/15 overflow-hidden shadow-sm">
                                                                            {it.icon ? (
                                                                                <img
                                                                                    src={it.icon}
                                                                                    alt=""
                                                                                    className="h-full w-full object-cover object-[80%_50%] saturate-110 contrast-110"
                                                                                    loading="lazy"
                                                                                    onError={(e) => (e.currentTarget.style.display = "none")}
                                                                                />
                                                                            ) : (
                                                                                <div className="h-full w-full bg-white/10" />
                                                                            )}
                                                                        </div>

                                                                        <div className="pointer-events-none absolute -inset-1 rounded-2xl blur-md opacity-0 group-hover:opacity-60 transition-opacity bg-white/20" />
                                                                    </div>

                                                                    <div className="min-w-0 flex-1">
                                                                        <div className="text-white font-semibold truncate">{name}</div>
                                                                        <div className="text-white/70 text-sm">
                                                                            {rest ? `${rest} games` : ""}
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            </div>
                                        )}

                                        {"meta" in c && c.meta && (
                                            <div className="mt-6 flex flex-wrap gap-x-3 gap-y-1 text-white/85 text-lg">
                                                {c.meta.map((m) => (
                                                    <span key={m.id} className="font-semibold">
                                                        {m.label}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                )}
                            </motion.div>

                        </div>
                    </CardShell>
                </div>
            )),
        [cards, total]
    );

    return (
        <div className="relative w-full h-[100svh] overflow-hidden" onWheel={onWheel}>
            {/* progress rail */}
            <div
                className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 z-10 flex flex-col items-center gap-2">
                {Array.from({length: total}).map((_, i) => (
                    <Dot key={i} active={i === index}/>
                ))}
            </div>

            <div className="h-full" ref={emblaRef}>
                <div className="flex flex-col h-full">{slides}</div>
            </div>
        </div>
    );
}
