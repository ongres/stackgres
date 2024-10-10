#!/usr/bin/env python3
"""Copy an image between two registries using the Docker Registry HTTP API v2.

Used by the docir cache controller to copy the images resolved by the StackGres images
repository (that requires a bearer token) into the plain image registry hosted by the cache:
the manifest (or the whole manifest list) is retrieved from the source, the blobs it references
(configs and layers) that are missing in the destination are streamed through a temporary file
and the manifest is pushed under the same repository name and digest (and tag).

Usage: docir-cache-copy-image.py <source image> <destination registry> [--dest-http] [--src-http]

Example: docir-cache-copy-image.py sgcr.dev/stackgres/patroni-4.1.0-8790-postgres-16.15@sha256:... 127.0.0.1:5000
"""

import argparse
import base64
import hashlib
import json
import os
import re
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request

MANIFEST_TYPES = ", ".join([
    "application/vnd.oci.image.index.v1+json",
    "application/vnd.docker.distribution.manifest.list.v2+json",
    "application/vnd.oci.image.manifest.v1+json",
    "application/vnd.docker.distribution.manifest.v2+json",
])
INDEX_TYPES = (
    "application/vnd.oci.image.index.v1+json",
    "application/vnd.docker.distribution.manifest.list.v2+json",
)
CHUNK = 1024 * 1024


def log(message):
    print(message, flush=True)


class Registry:
    def __init__(self, host, scheme, credentials=None):
        self.host = host
        self.scheme = scheme
        self.credentials = credentials
        self.tokens = {}

    def url(self, path):
        return "%s://%s/v2/%s" % (self.scheme, self.host, path)

    def request(self, method, path, headers=None, data=None, repository=None, scope="pull",
                stream=False, retry_auth=True):
        headers = dict(headers or {})
        token = self.tokens.get((repository, scope))
        if token:
            headers["Authorization"] = "Bearer " + token
        elif self.credentials:
            headers["Authorization"] = "Basic " + base64.b64encode(
                self.credentials.encode()).decode()
        request = urllib.request.Request(self.url(path), data=data, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=600)
            if stream:
                return response
            with response:
                return response.status, dict(response.headers), response.read()
        except urllib.error.HTTPError as error:
            if error.code == 401 and retry_auth and repository is not None:
                challenge = error.headers.get("WWW-Authenticate", "")
                if challenge.startswith("Bearer "):
                    self.tokens[(repository, scope)] = self.fetch_token(challenge, repository, scope)
                    return self.request(method, path, headers, data, repository, scope, stream, False)
            if stream:
                raise
            return error.code, dict(error.headers), error.read()

    def fetch_token(self, challenge, repository, scope):
        params = dict(re.findall(r'(\w+)="([^"]*)"', challenge))
        query = {"scope": "repository:%s:%s" % (repository, scope)}
        if "service" in params:
            query["service"] = params["service"]
        url = params["realm"] + "?" + urllib.parse.urlencode(query)
        request = urllib.request.Request(url)
        if self.credentials:
            request.add_header("Authorization", "Basic " + base64.b64encode(
                self.credentials.encode()).decode())
        with urllib.request.urlopen(request, timeout=60) as response:
            body = json.loads(response.read())
        return body.get("token") or body.get("access_token")

    def get_manifest(self, repository, reference):
        status, headers, body = self.request(
            "GET", "%s/manifests/%s" % (repository, reference),
            headers={"Accept": MANIFEST_TYPES}, repository=repository)
        if status != 200:
            raise RuntimeError("Can not get manifest %s@%s from %s: HTTP %s %s" % (
                repository, reference, self.host, status, body[:200]))
        content_type = headers.get("Content-Type", "").split(";")[0]
        digest = headers.get("Docker-Content-Digest") or (
            "sha256:" + hashlib.sha256(body).hexdigest())
        return content_type, digest, body

    def has_manifest(self, repository, reference):
        status, _, _ = self.request(
            "HEAD", "%s/manifests/%s" % (repository, reference),
            headers={"Accept": MANIFEST_TYPES}, repository=repository)
        return status == 200

    def has_blob(self, repository, digest):
        status, _, _ = self.request(
            "HEAD", "%s/blobs/%s" % (repository, digest), repository=repository)
        return status == 200

    def open_blob(self, repository, digest):
        return self.request(
            "GET", "%s/blobs/%s" % (repository, digest), repository=repository, stream=True)

    def put_blob(self, repository, digest, path, size):
        status, headers, body = self.request(
            "POST", "%s/blobs/uploads/" % repository, repository=repository, scope="pull,push")
        if status != 202:
            raise RuntimeError("Can not start upload of %s to %s: HTTP %s %s" % (
                digest, self.host, status, body[:200]))
        location = headers["Location"]
        if location.startswith("/"):
            location = "%s://%s%s" % (self.scheme, self.host, location)
        separator = "&" if "?" in location else "?"
        location = location + separator + urllib.parse.urlencode({"digest": digest})
        with open(path, "rb") as blob:
            request = urllib.request.Request(location, data=blob, method="PUT", headers={
                "Content-Type": "application/octet-stream",
                "Content-Length": str(size),
            })
            token = self.tokens.get((repository, "pull,push"))
            if token:
                request.add_header("Authorization", "Bearer " + token)
            elif self.credentials:
                request.add_header("Authorization", "Basic " + base64.b64encode(
                    self.credentials.encode()).decode())
            with urllib.request.urlopen(request, timeout=3600) as response:
                if response.status not in (201, 204):
                    raise RuntimeError("Can not upload %s to %s: HTTP %s" % (
                        digest, self.host, response.status))

    def put_manifest(self, repository, reference, content_type, body):
        status, _, response = self.request(
            "PUT", "%s/manifests/%s" % (repository, reference),
            headers={"Content-Type": content_type}, data=body,
            repository=repository, scope="pull,push")
        if status not in (201, 202):
            raise RuntimeError("Can not put manifest %s:%s to %s: HTTP %s %s" % (
                repository, reference, self.host, status, response[:200]))


def parse_image(image):
    """Return (host, repository, tag, digest) of an image reference."""
    match = re.match(r"^([^/]+)/([^@:]+)(?::([^@]+))?(?:@(sha256:[0-9a-f]+))?$", image)
    if not match:
        raise ValueError("Invalid image reference " + image)
    host, repository, tag, digest = match.groups()
    return host, repository, tag, digest


def copy_blob(source, destination, repository, digest, size, workdir):
    if destination.has_blob(repository, digest):
        log("   . blob %s already present" % digest)
        return
    log("   + blob %s (%s bytes)" % (digest, size))
    fd, path = tempfile.mkstemp(dir=workdir, prefix="blob-")
    try:
        hasher = hashlib.sha256()
        total = 0
        with os.fdopen(fd, "wb") as target, source.open_blob(repository, digest) as response:
            while True:
                chunk = response.read(CHUNK)
                if not chunk:
                    break
                hasher.update(chunk)
                total += len(chunk)
                target.write(chunk)
        actual = "sha256:" + hasher.hexdigest()
        if actual != digest:
            raise RuntimeError("Digest mismatch for %s: got %s" % (digest, actual))
        destination.put_blob(repository, digest, path, total)
    finally:
        os.unlink(path)


def copy_manifest(source, destination, repository, reference, workdir):
    content_type, digest, body = source.get_manifest(repository, reference)
    if destination.has_manifest(repository, digest):
        log(" . manifest %s@%s already present" % (repository, digest))
        return content_type, digest, body
    manifest = json.loads(body)
    if manifest.get("schemaVersion") != 2:
        # The manifest must be copied byte by byte to keep its digest (the images are referenced
        # by digest) and the registry rejects the manifests without the OCI mandatory
        # schemaVersion field, so the copy can not be done until the source manifest is fixed.
        raise RuntimeError(
            "Manifest %s@%s has no schemaVersion 2 (found %r): the registry rejects it and it can"
            " not be modified without changing its digest" % (
                repository, digest, manifest.get("schemaVersion")))
    if content_type in INDEX_TYPES:
        log(" * manifest list %s@%s" % (repository, digest))
        for entry in manifest.get("manifests", []):
            copy_manifest(source, destination, repository, entry["digest"], workdir)
    else:
        log(" * manifest %s@%s" % (repository, digest))
        blobs = list(manifest.get("layers", []))
        if "config" in manifest:
            blobs.append(manifest["config"])
        for blob in blobs:
            copy_blob(source, destination, repository, blob["digest"], blob.get("size", 0), workdir)
    destination.put_manifest(repository, digest, content_type, body)
    return content_type, digest, body


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", help="the source image (<host>/<repository>[:<tag>][@<digest>])")
    parser.add_argument("destination", help="the destination registry (<host>[:<port>])")
    parser.add_argument("--src-http", action="store_true", help="use HTTP for the source registry")
    parser.add_argument("--dest-http", action="store_true",
                        help="use HTTP for the destination registry")
    parser.add_argument("--workdir", default=os.environ.get("TMPDIR", "/tmp"),
                        help="the directory where blobs are temporarily stored")
    args = parser.parse_args()
    host, repository, tag, digest = parse_image(args.image)
    source = Registry(host, "http" if args.src_http else "https",
                      os.environ.get("DOCIR_CACHE_SOURCE_CREDENTIALS"))
    destination = Registry(args.destination, "http" if args.dest_http else "https",
                           os.environ.get("DOCIR_CACHE_DESTINATION_CREDENTIALS"))
    reference = digest or tag or "latest"
    log("Copying %s to %s" % (args.image, args.destination))
    content_type, digest, body = copy_manifest(source, destination, repository, reference,
                                               args.workdir)
    if tag:
        destination.put_manifest(repository, tag, content_type, body)
    log("Copied %s/%s@%s" % (args.destination, repository, digest))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # pylint: disable=broad-except
        log("Error: %s" % error)
        sys.exit(1)
