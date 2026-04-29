from __future__ import annotations

import argparse
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
W = "{%s}" % NS["w"]


def iter_paragraphs(document_xml: bytes):
    root = ET.fromstring(document_xml)
    body = root.find("w:body", NS)
    if body is None:
        return
    for p in body.findall("w:p", NS):
        text = "".join(node.text or "" for node in p.findall(".//w:t", NS)).strip()
        p_style = ""
        p_pr = p.find("w:pPr", NS)
        if p_pr is not None:
            p_style_el = p_pr.find("w:pStyle", NS)
            if p_style_el is not None:
                p_style = p_style_el.attrib.get(W + "val", "")
        if text:
            yield p_style, text


def inspect_docx(path: Path, limit: int, grep: list[str], show_sectpr: bool):
    print("=" * 80)
    print(path)
    with zipfile.ZipFile(path) as zf:
        styles_xml = zf.read("word/styles.xml")
        document_xml = zf.read("word/document.xml")

    styles_root = ET.fromstring(styles_xml)
    styles = []
    for style in styles_root.findall("w:style", NS):
        style_id = style.attrib.get(W + "styleId", "")
        name_el = style.find("w:name", NS)
        name = name_el.attrib.get(W + "val", "") if name_el is not None else ""
        if style_id:
            styles.append((style_id, name))
    print("style_count:", len(styles))
    for style_id, name in styles[:30]:
        print(f"STYLE\t{style_id}\t{name}")

    root = ET.fromstring(document_xml)
    if show_sectpr:
        body = root.find("w:body", NS)
        sect_pr = body.find("w:sectPr", NS) if body is not None else None
        print("sectPr:")
        if sect_pr is None:
            print("  NONE")
        else:
            for child in sect_pr:
                tag = child.tag.split("}", 1)[-1]
                print(f"  {tag}\t{child.attrib}")

    paragraphs = list(iter_paragraphs(document_xml))
    print("paragraph_count:", len(paragraphs))
    if grep:
        lowered = [item.lower() for item in grep]
        for idx, (style, text) in enumerate(paragraphs, start=1):
            hay = text.lower()
            if any(item in hay for item in lowered):
                print(f"P{idx:03d}\t{style or '-'}\t{text[:120]}")
    else:
        for idx, (style, text) in enumerate(paragraphs[:limit], start=1):
            print(f"P{idx:03d}\t{style or '-'}\t{text[:120]}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--limit", type=int, default=80)
    parser.add_argument("--grep", nargs="*", default=[])
    parser.add_argument("--sectpr", action="store_true")
    args = parser.parse_args()

    for raw_path in args.paths:
        inspect_docx(Path(raw_path), args.limit, args.grep, args.sectpr)


if __name__ == "__main__":
    main()
