#!/usr/bin/env python3
"""Check explicit system_menu IDs in SQL seed scripts."""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path


INSERT_RE = re.compile(
    r"INSERT\s+INTO\s+`?system_menu`?\s*\((.*?)\)\s*VALUES\s*(.*?);",
    re.IGNORECASE | re.DOTALL,
)


def split_tuples(values_sql: str) -> list[tuple[str, int]]:
    rows: list[tuple[str, int]] = []
    in_string = False
    depth = 0
    start: int | None = None
    i = 0
    while i < len(values_sql):
        ch = values_sql[i]
        if in_string:
            if ch == "'":
                if i + 1 < len(values_sql) and values_sql[i + 1] == "'":
                    i += 1
                else:
                    in_string = False
        else:
            if ch == "'":
                in_string = True
            elif ch == "(":
                if depth == 0:
                    start = i + 1
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0 and start is not None:
                    rows.append((values_sql[start:i], start))
                    start = None
        i += 1
    return rows


def split_fields(row_sql: str) -> list[str]:
    fields: list[str] = []
    in_string = False
    depth = 0
    start = 0
    i = 0
    while i < len(row_sql):
        ch = row_sql[i]
        if in_string:
            if ch == "'":
                if i + 1 < len(row_sql) and row_sql[i + 1] == "'":
                    i += 1
                else:
                    in_string = False
        else:
            if ch == "'":
                in_string = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            elif ch == "," and depth == 0:
                fields.append(row_sql[start:i].strip())
                start = i + 1
        i += 1
    fields.append(row_sql[start:].strip())
    return fields


def collect_menu_ids(paths: list[Path]) -> dict[int, list[tuple[Path, int, str]]]:
    ids: dict[int, list[tuple[Path, int, str]]] = defaultdict(list)
    for root in paths:
        if root.is_file():
            sql_files = [root]
        else:
            sql_files = sorted(root.rglob("*.sql"))
        for sql_file in sql_files:
            text = sql_file.read_text(encoding="utf-8")
            for insert in INSERT_RE.finditer(text):
                columns = [c.strip().strip("`").lower() for c in insert.group(1).split(",")]
                if "id" not in columns:
                    continue
                id_index = columns.index("id")
                name_index = columns.index("name") if "name" in columns else None
                values_start = insert.start(2)
                for row_sql, row_start in split_tuples(insert.group(2)):
                    fields = split_fields(row_sql)
                    if len(fields) <= id_index:
                        continue
                    raw_id = fields[id_index]
                    if not re.fullmatch(r"\d+", raw_id):
                        continue
                    name = fields[name_index] if name_index is not None and len(fields) > name_index else ""
                    line_no = text.count("\n", 0, values_start + row_start) + 1
                    ids[int(raw_id)].append((sql_file, line_no, name))
    return ids


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=[Path("backend/sql/module"), Path("backend/sql/mysql")],
        help="SQL files or directories to scan.",
    )
    args = parser.parse_args()

    ids = collect_menu_ids(args.paths)
    duplicates = {menu_id: refs for menu_id, refs in ids.items() if len(refs) > 1}
    if not duplicates:
        print(f"OK: {len(ids)} explicit system_menu IDs are unique.")
        return 0

    print(f"ERROR: found {len(duplicates)} duplicate system_menu IDs.", file=sys.stderr)
    for menu_id in sorted(duplicates):
        print(f"ID {menu_id}:", file=sys.stderr)
        for sql_file, line_no, name in duplicates[menu_id]:
            print(f"  {sql_file}:{line_no} {name}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
