"""Static inventory only: does not import or execute application modules."""
from __future__ import print_function

import argparse
import ast
import io
import json
import os
import re
import tokenize

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INCLUDED = ("src/main/java/", "batch/ima_batch/", "sdk/", "sql/", "ops/")
EXTENSIONS = {".java": "Java", ".py": "Python", ".sql": "SQL", ".sh": "Bash"}


def python_lines(source, path):
    tree = ast.parse(source, filename=path)
    excluded = set()
    for node in ast.walk(tree):
        if isinstance(node, (ast.Module, ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef)):
            body = node.body
            if body and isinstance(body[0], ast.Expr):
                value = body[0].value
                if isinstance(value, ast.Constant) and isinstance(value.value, str):
                    excluded.update(range(body[0].lineno, body[0].end_lineno + 1))
    active = set()
    ignored = {tokenize.ENCODING, tokenize.ENDMARKER, tokenize.NEWLINE,
               tokenize.NL, tokenize.INDENT, tokenize.DEDENT, tokenize.COMMENT}
    for token in tokenize.generate_tokens(io.StringIO(source).readline):
        if token.type not in ignored:
            active.update(range(token.start[0], token.end[0] + 1))
    return len(active - excluded)


def c_style_lines(source, sql=False):
    pattern = r"'(?:''|[^'])*'|\"(?:\\.|[^\"\\])*\"|/\*[\s\S]*?\*/|//[^\n]*"
    if sql:
        pattern = r"'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"|/\*[\s\S]*?\*/|--[^\n]*"

    def strip(match):
        value = match.group(0)
        if value.startswith(("/*", "//", "--")):
            return "\n" * value.count("\n")
        return value

    cleaned = re.sub(pattern, strip, source)
    return sum(bool(line.strip()) for line in cleaned.splitlines())


def audit():
    languages = {}
    files = []
    for directory, subdirectories, names in os.walk(ROOT):
        subdirectories[:] = sorted(name for name in subdirectories
                                   if name not in (".git", "target", "__pycache__", ".tools"))
        for name in sorted(names):
            path = os.path.join(directory, name)
            relative = os.path.relpath(path, ROOT).replace(os.sep, "/")
            extension = os.path.splitext(name)[1]
            if not relative.startswith(INCLUDED) or extension not in EXTENSIONS:
                continue
            with io.open(path, encoding="utf-8") as stream:
                source = stream.read()
            if extension == ".py":
                count = python_lines(source, relative)
            elif extension in (".java", ".sql"):
                count = c_style_lines(source, extension == ".sql")
            else:
                count = sum(bool(line.strip()) and not line.lstrip().startswith("#")
                            for line in source.splitlines())
            language = EXTENSIONS[extension]
            languages[language] = languages.get(language, 0) + count
            files.append({"path": relative, "language": language, "code_lines": count})
    return {
        "definition": "nonblank, noncomment implementation source lines; Python docstrings excluded",
        "excluded": ["tests", "documentation", "configuration", "tools", "generated code", "dependencies"],
        "target": 10000,
        "total_code_lines": sum(languages.values()),
        "by_language": languages,
        "files": files,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", help="write JSON inventory to a repository-relative path")
    arguments = parser.parse_args()
    report = audit()
    if arguments.write:
        target = os.path.abspath(os.path.join(ROOT, arguments.write))
        if os.path.commonpath([ROOT, target]) != ROOT:
            parser.error("inventory output must stay inside the repository")
        with io.open(target, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(json.dumps(report, indent=2, sort_keys=True) + "\n")
    summary = dict(report)
    summary["file_count"] = len(summary.pop("files"))
    print(json.dumps(summary, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
