from __future__ import absolute_import, division

import os
import time
import uuid

from .checkpoint import FileLock
from .compat import open_text
from .serialization import atomic_json, dumps, loads
from .validation import event, integer


class DiskSpool(object):
    def __init__(self, directory, application_id, maximum_files=10000, maximum_bytes=104857600):
        self.directory = os.path.abspath(directory)
        self.application_id = application_id
        self.maximum_files = integer(maximum_files, "maximumFiles", 1, 1000000)
        self.maximum_bytes = integer(maximum_bytes, "maximumBytes", 1024, 1099511627776)
        if not os.path.isdir(self.directory):
            os.makedirs(self.directory, 0o700)
        self.pending = os.path.join(self.directory, "pending")
        self.rejected = os.path.join(self.directory, "rejected")
        for path in (self.pending, self.rejected):
            if not os.path.isdir(path):
                os.mkdir(path, 0o700)

    def _files(self):
        return sorted(os.path.join(self.pending, name) for name in os.listdir(self.pending)
                      if name.endswith(".json") and os.path.isfile(os.path.join(self.pending, name)))

    def enqueue(self, raw):
        value = event(raw, self.application_id)
        encoded = dumps(value).encode("utf-8")
        with FileLock(os.path.join(self.directory, ".enqueue.lock")):
            files = self._files()
            if len(files) >= self.maximum_files:
                raise ValueError("spool file capacity exceeded")
            size = sum(os.path.getsize(path) for path in files)
            if size + len(encoded) > self.maximum_bytes:
                raise ValueError("spool byte capacity exceeded")
            name = "{0:015d}-{1}.json".format(int(time.time() * 1000), uuid.uuid4().hex)
            path = os.path.join(self.pending, name)
            atomic_json(path, value)
            return name

    def drain(self, client, batch_size=100):
        integer(batch_size, "batchSize", 1, 500)
        if client.application_id != self.application_id:
            raise ValueError("client application does not match spool application")
        with FileLock(os.path.join(self.directory, ".drain.lock")):
            files = self._files()[:batch_size]
            if not files:
                return {"accepted": 0, "rejected": 0, "retryable": 0}
            events = []
            for path in files:
                with open_text(path) as stream:
                    raw = stream.read(1048577)
                if len(raw) > 1048576:
                    raise ValueError("spooled event exceeds size limit")
                events.append(event(loads(raw), self.application_id))
            outcomes = client.ingest(events)
            result = {"accepted": 0, "rejected": 0, "retryable": 0}
            for path, outcome in zip(files, outcomes):
                status = outcome["status"]
                if status in ("CREATED", "DUPLICATE"):
                    os.unlink(path)
                    result["accepted"] += 1
                elif status in ("INVALID", "CONFLICT"):
                    destination = os.path.join(self.rejected, os.path.basename(path))
                    if os.path.exists(destination):
                        raise ValueError("rejected spool destination already exists")
                    os.rename(path, destination)
                    atomic_json(destination + ".outcome", outcome)
                    result["rejected"] += 1
                else:
                    result["retryable"] += 1
            return result

    def inventory(self):
        files = self._files()
        return {"pendingEvents": len(files), "pendingBytes": sum(os.path.getsize(path) for path in files),
                "oldestFile": os.path.basename(files[0]) if files else None,
                "rejectedEvents": sum(1 for name in os.listdir(self.rejected) if name.endswith(".json"))}
