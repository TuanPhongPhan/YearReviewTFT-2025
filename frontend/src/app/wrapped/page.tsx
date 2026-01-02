import { Suspense } from "react";
import WrappedPageClient from "./WrappedPageClient";

export default function Page() {
    return (
        <Suspense fallback={null}>
            <WrappedPageClient />
        </Suspense>
    );
}
