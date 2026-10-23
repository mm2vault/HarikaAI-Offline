import unittest
from unittest.mock import patch

from jarvis_brain.web_search import search_web


class _FakeResponse:
    def __init__(self, body: bytes):
        self.body = body
    def __enter__(self):
        return self
    def __exit__(self, *args):
        return False
    def read(self, limit=-1):
        return self.body[:limit]


class WebSearchTests(unittest.TestCase):
    def test_rejects_empty_query(self):
        with self.assertRaises(ValueError):
            search_web("  ")

    def test_rejects_invalid_limit(self):
        with self.assertRaises(ValueError):
            search_web("HarikaAI", limit=0)

    @patch("jarvis_brain.web_search.urlopen")
    def test_parses_results_without_real_network(self, mock_open):
        html = b'''<a class="result__a" href="https://example.com/ai">Example AI</a>
        <a class="result__snippet">A small test snippet.</a>
        <a class="result__a" href="https://example.org">Second result</a>'''
        mock_open.return_value = _FakeResponse(html)
        results = search_web("Turkce yapay zeka")
        self.assertEqual(len(results), 2)
        self.assertEqual(results[0]["title"], "Example AI")
        self.assertEqual(results[0]["url"], "https://example.com/ai")
        self.assertEqual(results[0]["snippet"], "A small test snippet.")


if __name__ == "__main__":
    unittest.main()
