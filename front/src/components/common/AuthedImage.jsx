import { useAuthedObjectUrl } from "../hooks/useAuthedObjectUrl";

const TRANSPARENT_PIXEL =
    "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";

export default function AuthedImage({ src, alt = "", className = "", ...rest }) {
    const objectUrl = useAuthedObjectUrl(src);

    if (!src) {
        return null;
    }

    return <img src={objectUrl || TRANSPARENT_PIXEL} alt={alt} className={className} {...rest} />;
}
