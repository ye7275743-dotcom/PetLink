#!/usr/bin/env python3
"""PetLink V1.0 M01-M08 local 50-user performance acceptance.

The script creates isolated USER accounts as unmeasured setup, then runs two
50-worker scenarios against the real local backend/MySQL stack:

* ordinary public queries (target P95 <= 2 seconds)
* authenticated profile updates (target P95 <= 3 seconds)

It writes a token-free JSON evidence file and exits non-zero if availability,
HTTP/API success, concurrency, or either latency target fails.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import datetime as dt
import json
import math
import os
import platform
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Callable


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_USERS = 50
DEFAULT_ITERATIONS = 3
PASSWORD = "PetLinkPerf_2026!"


@dataclass
class Measurement:
    worker: int
    iteration: int
    operation: str
    method: str
    path: str
    latency_ms: float
    http_status: int
    api_code: int | None
    success: bool
    error: str | None = None


def request_json(
    base_url: str,
    method: str,
    path: str,
    body: dict[str, Any] | None = None,
    token: str | None = None,
    timeout: float = 15.0,
) -> tuple[int, dict[str, Any]]:
    headers = {"Accept": "application/json"}
    data = None
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        base_url.rstrip("/") + path,
        data=data,
        headers=headers,
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = json.loads(response.read().decode("utf-8"))
            return response.status, payload
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            payload = {"code": None, "message": raw[:500]}
        return exc.code, payload


def percentile(values: list[float], percentile_value: float) -> float:
    """Linear-interpolated percentile, matching common monitoring tools."""
    ordered = sorted(values)
    if not ordered:
        return 0.0
    position = (len(ordered) - 1) * percentile_value / 100.0
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def summarize(measurements: list[Measurement], threshold_ms: float) -> dict[str, Any]:
    latencies = [item.latency_ms for item in measurements]
    successes = sum(item.success for item in measurements)
    p95 = percentile(latencies, 95)
    return {
        "sample_count": len(measurements),
        "success_count": successes,
        "failure_count": len(measurements) - successes,
        "success_rate_percent": round(successes * 100.0 / len(measurements), 3),
        "latency_ms": {
            "min": round(min(latencies), 3),
            "average": round(statistics.fmean(latencies), 3),
            "p50": round(percentile(latencies, 50), 3),
            "p95": round(p95, 3),
            "p99": round(percentile(latencies, 99), 3),
            "max": round(max(latencies), 3),
        },
        "threshold_ms": threshold_ms,
        "passed": successes == len(measurements) and p95 <= threshold_ms,
    }


def setup_user(base_url: str, run_id: str, worker: int) -> tuple[int, str]:
    account = f"perf_{run_id}_{worker:02d}"
    register_status, register_payload = request_json(
        base_url,
        "POST",
        "/api/auth/register",
        {"account": account, "password": PASSWORD, "nickname": f"性能用户{worker:02d}"},
    )
    if register_status != 201 or register_payload.get("code") != 0:
        raise RuntimeError(
            f"worker {worker}: registration failed: HTTP {register_status}, "
            f"code={register_payload.get('code')}"
        )
    login_status, login_payload = request_json(
        base_url,
        "POST",
        "/api/auth/login",
        {"account": account, "password": PASSWORD},
    )
    token = (login_payload.get("data") or {}).get("accessToken")
    if login_status != 200 or login_payload.get("code") != 0 or not token:
        raise RuntimeError(
            f"worker {worker}: login failed: HTTP {login_status}, "
            f"code={login_payload.get('code')}"
        )
    return worker, token


def measured_request(
    base_url: str,
    worker: int,
    iteration: int,
    operation: str,
    method: str,
    path: str,
    token: str,
    body: dict[str, Any] | None = None,
) -> Measurement:
    started = time.perf_counter_ns()
    try:
        status, payload = request_json(base_url, method, path, body, token)
        latency_ms = (time.perf_counter_ns() - started) / 1_000_000
        api_code = payload.get("code")
        success = 200 <= status < 300 and api_code == 0
        return Measurement(
            worker,
            iteration,
            operation,
            method,
            path,
            round(latency_ms, 3),
            status,
            api_code,
            success,
            None if success else str(payload.get("message", "API request failed"))[:300],
        )
    except Exception as exc:  # Evidence must retain transport failures.
        latency_ms = (time.perf_counter_ns() - started) / 1_000_000
        return Measurement(
            worker,
            iteration,
            operation,
            method,
            path,
            round(latency_ms, 3),
            0,
            None,
            False,
            f"{type(exc).__name__}: {exc}"[:300],
        )


def run_scenario(
    users: int,
    iterations: int,
    worker_function: Callable[[int, int], Measurement],
) -> tuple[list[Measurement], float]:
    barrier = threading.Barrier(users)

    def run_worker(worker: int) -> list[Measurement]:
        barrier.wait(timeout=30)
        return [worker_function(worker, iteration) for iteration in range(1, iterations + 1)]

    started = time.perf_counter()
    all_measurements: list[Measurement] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=users) as executor:
        futures = [executor.submit(run_worker, worker) for worker in range(users)]
        for future in concurrent.futures.as_completed(futures):
            all_measurements.extend(future.result())
    duration = time.perf_counter() - started
    all_measurements.sort(key=lambda item: (item.worker, item.iteration))
    return all_measurements, duration


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--users", type=int, default=DEFAULT_USERS)
    parser.add_argument("--iterations", type=int, default=DEFAULT_ITERATIONS)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if args.users < 1 or args.iterations < 1:
        parser.error("--users and --iterations must be positive")

    health_status, health = request_json(args.base_url, "GET", "/actuator/health")
    if health_status != 200 or health.get("status") != "UP":
        print(f"Backend health check failed: HTTP {health_status}, {health}", file=sys.stderr)
        return 2

    now = dt.datetime.now(dt.timezone(dt.timedelta(hours=8)))
    run_id = now.strftime("%m%d%H%M%S")
    print(f"Preparing {args.users} isolated test users (not measured)...", flush=True)
    tokens: dict[int, str] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(args.users, 10)) as executor:
        futures = [executor.submit(setup_user, args.base_url, run_id, worker) for worker in range(args.users)]
        for future in concurrent.futures.as_completed(futures):
            worker, token = future.result()
            tokens[worker] = token

    query_paths = [
        "/api/animals?page=1&size=20",
        "/api/announcements?page=1&size=20",
    ]

    print("Running ordinary-query scenario...", flush=True)
    query_measurements, query_duration = run_scenario(
        args.users,
        args.iterations,
        lambda worker, iteration: measured_request(
            args.base_url,
            worker,
            iteration,
            "ordinary_query",
            "GET",
            query_paths[(worker + iteration) % len(query_paths)],
            tokens[worker],
        ),
    )

    print("Running ordinary-update scenario...", flush=True)
    write_measurements, write_duration = run_scenario(
        args.users,
        args.iterations,
        lambda worker, iteration: measured_request(
            args.base_url,
            worker,
            iteration,
            "ordinary_update",
            "PATCH",
            "/api/users/me",
            tokens[worker],
            {"nickname": f"perf-{run_id}-{worker:02d}-{iteration}"},
        ),
    )

    query_summary = summarize(query_measurements, 2_000.0)
    write_summary = summarize(write_measurements, 3_000.0)
    passed = (
        args.users == DEFAULT_USERS
        and query_summary["passed"]
        and write_summary["passed"]
    )

    output = args.output or (
        Path(__file__).resolve().parents[2]
        / "docs"
        / "testing"
        / "evidence"
        / f"performance-{now:%Y-%m-%d}.json"
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    evidence = {
        "evidence_type": "PetLink V1.0 M01-M08 50-user performance acceptance",
        "started_at": now.isoformat(),
        "completed_at": dt.datetime.now(dt.timezone(dt.timedelta(hours=8))).isoformat(),
        "base_url": args.base_url,
        "backend_health": health,
        "environment": {
            "os": platform.platform(),
            "python": platform.python_version(),
            "database": "local MySQL container (real database)",
        },
        "setup": {
            "concurrent_users": args.users,
            "iterations_per_user_per_scenario": args.iterations,
            "accounts_created": len(tokens),
            "registration_and_login_excluded_from_measurement": True,
            "image_upload_network_time_included": False,
        },
        "criteria": {
            "required_concurrent_users": DEFAULT_USERS,
            "ordinary_query_p95_ms_max": 2_000.0,
            "ordinary_create_or_update_p95_ms_max": 3_000.0,
            "required_http_api_success_rate_percent": 100.0,
        },
        "scenarios": {
            "ordinary_query": {
                "endpoints": query_paths,
                "wall_time_seconds": round(query_duration, 3),
                "requests_per_second": round(len(query_measurements) / query_duration, 3),
                "summary": query_summary,
                "measurements": [asdict(item) for item in query_measurements],
            },
            "ordinary_update": {
                "endpoint": "PATCH /api/users/me",
                "wall_time_seconds": round(write_duration, 3),
                "requests_per_second": round(len(write_measurements) / write_duration, 3),
                "summary": write_summary,
                "measurements": [asdict(item) for item in write_measurements],
            },
        },
        "overall_passed": passed,
        "notes": [
            "Each worker represents one authenticated USER account.",
            "A barrier releases all 50 workers together at the start of each scenario.",
            "Tokens, passwords, and phone numbers are not written to evidence.",
        ],
    }
    output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(
        "Query: "
        f"{query_summary['success_count']}/{query_summary['sample_count']} success, "
        f"P95={query_summary['latency_ms']['p95']:.3f} ms / 2000 ms, "
        f"{'PASS' if query_summary['passed'] else 'FAIL'}"
    )
    print(
        "Update: "
        f"{write_summary['success_count']}/{write_summary['sample_count']} success, "
        f"P95={write_summary['latency_ms']['p95']:.3f} ms / 3000 ms, "
        f"{'PASS' if write_summary['passed'] else 'FAIL'}"
    )
    print(f"Evidence: {output}")
    print(f"Overall: {'PASS' if passed else 'FAIL'}")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
