import { Suspense } from "react";
import StartPageClient from "./StartPageClient";

export default function Page() {
    return (
        <Suspense fallback={null}>
            <StartPageClient />
        </Suspense>
    );
}
