from __future__ import absolute_import, division

import os
import time
import uuid

from .compat import open_text
from .errors import CheckpointError
from .serialization import atomic_json, file_digest, loads


class FileLock(object):
    def __init__(self, path):
        self.path = os.path.abspath(path)
        self.descriptor = None

    def __enter__(self):
        try:
            self.descriptor = os.open(self.path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        except OSError:
            raise CheckpointError("job lock already exists; inspect the owner before removing it")
        payload = "pid={0}\ncreated={1}\n".format(os.getpid(), int(time.time()))
        os.write(self.descriptor, payload.encode("ascii"))
        os.fsync(self.descriptor)
        return self

    def __exit__(self, exception_type, exception, traceback):
        if self.descriptor is not None:
            os.close(self.descriptor)
            self.descriptor = None
            os.unlink(self.path)


class Checkpoint(object):
    def __init__(self, path, job_name, input_path, parameters):
        self.path = path
        self.job_name = job_name
        self.input_path = os.path.abspath(input_path)
        self.input_digest = file_digest(input_path)
        self.parameters = dict(parameters)
        self.state = {
            "formatVersion": 1,
            "jobName": job_name,
            "inputPath": self.input_path,
            "inputSha256": self.input_digest,
            "parameters": self.parameters,
            "runId": str(uuid.uuid4()),
            "state": "new",
            "processedRows": 0,
            "outputSha256": None,
        }

    def load(self):
        if not os.path.exists(self.path):
            return False
        with open_text(self.path) as stream:
            existing = loads(stream.read())
        for name in ("formatVersion", "jobName", "inputPath", "inputSha256", "parameters"):
            if existing.get(name) != self.state[name]:
                raise CheckpointError("checkpoint does not match the job inputs")
        if existing.get("state") not in ("running", "failed", "completed"):
            raise CheckpointError("unknown checkpoint state")
        self.state = existing
        return True

    def start(self):
        if self.state["state"] == "completed":
            raise CheckpointError("completed job must not be restarted without a new checkpoint")
        self.state["state"] = "running"
        self.state["startedAt"] = int(time.time() * 1000)
        self.state["runId"] = str(uuid.uuid4())
        self.state["processedRows"] = 0
        self.save()

    def progress(self, rows):
        if self.state["state"] != "running" or rows < self.state["processedRows"]:
            raise CheckpointError("invalid checkpoint progress transition")
        self.state["processedRows"] = rows
        self.save()

    def complete(self, output_path):
        if self.state["state"] != "running":
            raise CheckpointError("job must be running to complete")
        self.state["outputPath"] = os.path.abspath(output_path)
        self.state["outputSha256"] = file_digest(output_path)
        self.state["state"] = "completed"
        self.state["completedAt"] = int(time.time() * 1000)
        self.save()

    def fail(self, code):
        if self.state["state"] != "running":
            raise CheckpointError("only a running job can fail")
        self.state["state"] = "failed"
        self.state["failureCode"] = str(code)[:128]
        self.save()

    def verify_output(self):
        if self.state["state"] != "completed":
            return False
        path = self.state.get("outputPath")
        return bool(path and os.path.isfile(path) and file_digest(path) == self.state["outputSha256"])

    def save(self):
        atomic_json(self.path, self.state)
