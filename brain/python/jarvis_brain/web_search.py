"""Small API-free web search adapter for JARVİS Brain.

Uses DuckDuckGo's public HTML search page with the Python standard library.
This is a prototype for the research layer, not a replacement for Android
integration or a guarantee that the provider is always reachable.
"""
from __future__ import annotations

from html.parser import HTMLParser
from urllib.error import HTTPError, URLError
from urllib.parse import quote_plus, urljoin, urlparse
from urllib.request import Request, urlopen


class _SearchParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.results: list[dict[str, str]] = []
        self._active: str | None = None
        self._text: list[str] = []
        self._href = ""

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_map = dict(attrs)
        classes = (attrs_map.get("class") or "").split()
        if tag == "a" and "result__a" in classes:
            self._active = "title"
            self._text = []
            self._href = attrs_map.get("href") or ""
        elif tag in {"a", "div"} and "result__snippet" in classes:
            self._active = "snippet"
            self._text = []

    def handle_data(self, data: str) -> None:
        if self._active:
            self._text.append(data)

    def handle_endtag(self, tag: str) -> None:
        if not self._active:
            return
        if (self._active == "title" and tag == "a") or (self._active == "snippet" and tag in {"a", "div"}):
            value = " ".join(" ".join(self._text).split())
            if self._active == "title" and value and self._href:
                self.results.append({"title": value, "url": self._href, "snippet": ""})
            elif self._active == "snippet" and value:
                for result in reversed(self.results):
                    if not result["snippet"]:
                        result["snippet"] = value
                        break
            self._active = None
            self._text = []


def search_web(query: str, *, limit: int = 5, timeout: float = 8.0) -> list[dict[str, str]]:
    """Return title/url/snippet results without an AI API key.

    Raises ValueError for invalid input and RuntimeError for network/provider
    errors so callers can show a clear offline/unavailable message.
    """
    query = " ".join(query.split())
    if not query:
        raise ValueError("Search query cannot be empty")
    if not 1 <= limit <= 10:
        raise ValueError("limit must be between 1 and 10")
    url = "https://html.duckduckgo.com/html/?q=" + quote_plus(query)
    request = Request(url, headers={"User-Agent": "JarvisBrain/0.1 (web search prototype)"})
    try:
        with urlopen(request, timeout=timeout) as response:
            html = response.read(2_000_000).decode("utf-8", errors="replace")
    except (HTTPError, URLError, TimeoutError, OSError) as exc:
        raise RuntimeError("Internet search is unavailable; check connection or try later") from exc

    parser = _SearchParser()
    parser.feed(html)
    results: list[dict[str, str]] = []
    for item in parser.results:
        href = item["url"]
        # DuckDuckGo sometimes wraps destinations in its redirect URL.
        if href.startswith("//"):
            href = "https:" + href
        elif href.startswith("/"):
            href = urljoin("https://duckduckgo.com", href)
        parsed = urlparse(href)
        if parsed.scheme not in {"http", "https"}:
            continue
        item["url"] = href
        results.append(item)
        if len(results) >= limit:
            break
    return results
