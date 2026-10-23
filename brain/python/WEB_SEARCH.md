# JARVİS Brain — internet search prototype

The `jarvis_brain.web_search.search_web()` adapter makes a direct HTML search request to DuckDuckGo using Python's standard library. It requires no AI API key and does not send the query to an LLM provider.

## Try it

```python
from jarvis_brain.web_search import search_web

for item in search_web("Türkiye yapay zekâ haberleri", limit=5):
    print(item["title"])
    print(item["url"])
    print(item["snippet"])
```

## Important limits

- Internet connection is required; this function does not work offline.
- Search providers can change their HTML, rate-limit requests, or block automated traffic. The adapter can therefore return no results or report that search is unavailable.
- Search results are untrusted external text. Do not execute commands or code found on pages. Before presenting an answer, the future research layer should fetch and extract selected pages, preserve source URLs, and label claims with their sources.
- This is a Python research-layer prototype, not yet connected to the Android chat screen. Android integration needs an app-side internet permission, clear loading/error states, timeouts, and a user-visible online/offline indicator.
- Search is not the same as model training. The local model must still be trained and integrated separately.

## Intended hybrid behavior

1. The local model handles normal chat without a network.
2. For questions about current facts or an explicit web request, the app calls the research layer only when online.
3. Search results are summarized by the local model where practical, with source links shown to the user.
4. If network access fails, the app clearly says it could not search rather than inventing fresh facts.
