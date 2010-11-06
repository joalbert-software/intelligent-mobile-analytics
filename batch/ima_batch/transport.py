from __future__ import absolute_import, division

import email.utils
import os
import random
import time

try:
    from urllib.request import Request, build_opener, HTTPRedirectHandler
    from urllib.error import HTTPError, URLError
    from urllib.parse import urlparse, quote
except ImportError:
    from urllib2 import Request, build_opener, HTTPRedirectHandler, HTTPError, URLError
    from urlparse import urlparse
    from urllib import quote

from .errors import TransportError
from .serialization import dumps, loads
from .validation import event, identifier, integer
from .compat import is_integer, string_types


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, file_pointer, code, message, headers, new_url):
        return None


def retry_after(value, now=None):
    if not value:
        return None
    now = time.time() if now is None else now
    try:
        return max(0.0, min(300.0, float(value)))
    except ValueError:
        parsed = email.utils.parsedate_tz(value)
        if parsed is None:
            return None
        return max(0.0, min(300.0, email.utils.mktime_tz(parsed) - now))


class Client(object):
    def __init__(self, endpoint, application_id, token, timeout=10, maximum_attempts=4):
        parsed = urlparse(endpoint)
        if parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ValueError("endpoint must not contain credentials, a query, or a fragment")
        local = parsed.hostname in ("127.0.0.1", "localhost", "::1")
        if parsed.scheme != "https" and not (parsed.scheme == "http" and local):
            raise ValueError("remote endpoints require HTTPS")
        if not parsed.hostname:
            raise ValueError("endpoint requires a hostname")
        identifier(application_id, "applicationId")
        if not isinstance(token, string_types) or not 32 <= len(token) <= 256:
            raise ValueError("invalid bearer token length")
        if "\r" in token or "\n" in token:
            raise ValueError("invalid bearer token")
        integer(timeout, "timeout", 1, 120)
        integer(maximum_attempts, "maximumAttempts", 1, 10)
        self.endpoint = endpoint.rstrip("/")
        self.application_id = application_id
        self.token = token
        self.timeout = timeout
        self.maximum_attempts = maximum_attempts
        self.opener = build_opener(NoRedirect())
        self.random = random.Random()

    def _post(self, inputs):
        path = "/api/v1/apps/{0}/events".format(quote(self.application_id, safe=""))
        request = Request(self.endpoint + path, data=dumps(inputs).encode("utf-8"))
        request.add_header("Authorization", "Bearer " + self.token)
        request.add_header("Content-Type", "application/json")
        request.add_header("Accept", "application/json")
        request.add_header("User-Agent", "IMA-Python/1.0")
        response = None
        try:
            response = self.opener.open(request, timeout=self.timeout)
            raw = response.read(1048577)
            if len(raw) > 1048576:
                raise TransportError("response exceeds the client size limit")
            return loads(raw.decode("utf-8"))
        except HTTPError as failure:
            code = failure.code
            delay = retry_after(failure.headers.get("Retry-After"))
            failure.close()
            raise TransportError("HTTP status {0}".format(code), code in (429, 502, 503, 504), delay)
        except (URLError, IOError):
            raise TransportError("network request failed", retryable=True)
        except (ValueError, UnicodeError):
            raise TransportError("server returned malformed JSON")
        finally:
            if response is not None:
                response.close()

    def ingest(self, events):
        events = list(events)
        if not 1 <= len(events) <= 500:
            raise ValueError("ingestion requires 1 to 500 events")
        pending = []
        for index, raw in enumerate(events):
            normalized = event(raw, self.application_id)
            normalized.pop("applicationId")
            normalized.pop("receivedAt")
            pending.append((index, normalized))
        outcomes = [None] * len(events)
        for attempt in range(self.maximum_attempts):
            delay = None
            try:
                response = self._post([value for unused, value in pending])
                items = response.get("items") if isinstance(response, dict) else None
                if not isinstance(items, list) or len(items) != len(pending):
                    raise TransportError("invalid ingestion response")
                following = []
                seen = set()
                for item in items:
                    if not isinstance(item, dict):
                        raise TransportError("invalid response item")
                    local_index = item.get("index")
                    if not is_integer(local_index) or not 0 <= local_index < len(pending) or local_index in seen:
                        raise TransportError("invalid response item index")
                    seen.add(local_index)
                    original_index, value = pending[local_index]
                    if item.get("eventId") != value["eventId"]:
                        raise TransportError("response event identity mismatch")
                    status = item.get("status")
                    if status == "UNAVAILABLE":
                        following.append((original_index, value))
                    elif status in ("CREATED", "DUPLICATE", "CONFLICT", "INVALID"):
                        result = dict(item)
                        result["index"] = original_index
                        outcomes[original_index] = result
                    else:
                        raise TransportError("unknown ingestion item status")
                pending = following
                if not pending:
                    return outcomes
            except TransportError as failure:
                if not failure.retryable:
                    raise
                delay = failure.retry_after
            if attempt + 1 < self.maximum_attempts:
                wait = delay if delay is not None else self.random.uniform(0, min(30, 2 ** attempt))
                time.sleep(wait)
        for index, value in pending:
            outcomes[index] = {"index": index, "eventId": value["eventId"],
                               "status": "UNAVAILABLE", "error": "retry_budget_exhausted"}
        return outcomes
