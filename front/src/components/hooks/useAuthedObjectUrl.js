import { useEffect, useState } from "react";
import { fetchAuthedObjectUrl, revokeObjectUrl } from "../api/fileApi";

export function useAuthedObjectUrl(url) {
    const [objectUrl, setObjectUrl] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function load() {
            if (!url) {
                setObjectUrl((prev) => {
                    revokeObjectUrl(prev);
                    return "";
                });
                return;
            }

            try {
                const next = await fetchAuthedObjectUrl(url);
                if (cancelled) {
                    revokeObjectUrl(next);
                    return;
                }
                setObjectUrl((prev) => {
                    revokeObjectUrl(prev);
                    return next;
                });
            } catch (e) {
                setObjectUrl((prev) => {
                    revokeObjectUrl(prev);
                    return "";
                });
            }
        }

        load();

        return () => {
            cancelled = true;
        };
    }, [url]);

    useEffect(() => {
        return () => {
            revokeObjectUrl(objectUrl);
        };
    }, [objectUrl]);

    return objectUrl;
}
