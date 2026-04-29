from __future__ import annotations

import argparse
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
ET.register_namespace("w", NS_W)


def w_tag(name: str) -> str:
    return f"{{{NS_W}}}{name}"


def make_info_paragraph(text: str) -> ET.Element:
    p = ET.Element(w_tag("p"))
    p_pr = ET.SubElement(p, w_tag("pPr"))
    ET.SubElement(
        p_pr,
        w_tag("spacing"),
        {
            w_tag("before"): "120",
            w_tag("after"): "120",
            w_tag("line"): "400",
            w_tag("lineRule"): "exact",
        },
    )
    ET.SubElement(p_pr, w_tag("jc"), {w_tag("val"): "center"})
    run = ET.SubElement(p, w_tag("r"))
    r_pr = ET.SubElement(run, w_tag("rPr"))
    ET.SubElement(
        r_pr,
        w_tag("rFonts"),
        {
            w_tag("ascii"): "Times New Roman",
            w_tag("hAnsi"): "Times New Roman",
            w_tag("eastAsia"): "SimSun",
            w_tag("cs"): "Times New Roman",
        },
    )
    ET.SubElement(r_pr, w_tag("sz"), {w_tag("val"): "32"})
    ET.SubElement(r_pr, w_tag("szCs"), {w_tag("val"): "32"})
    t = ET.SubElement(run, w_tag("t"))
    if "  " in text:
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    t.text = text
    return p


def make_blank_paragraph() -> ET.Element:
    p = ET.Element(w_tag("p"))
    p_pr = ET.SubElement(p, w_tag("pPr"))
    ET.SubElement(
        p_pr,
        w_tag("spacing"),
        {
            w_tag("before"): "0",
            w_tag("after"): "0",
            w_tag("line"): "400",
            w_tag("lineRule"): "exact",
        },
    )
    ET.SubElement(p_pr, w_tag("jc"), {w_tag("val"): "both"})
    run = ET.SubElement(p, w_tag("r"))
    r_pr = ET.SubElement(run, w_tag("rPr"))
    ET.SubElement(
        r_pr,
        w_tag("rFonts"),
        {
            w_tag("ascii"): "Times New Roman",
            w_tag("hAnsi"): "Times New Roman",
            w_tag("eastAsia"): "SimSun",
            w_tag("cs"): "Times New Roman",
        },
    )
    ET.SubElement(r_pr, w_tag("sz"), {w_tag("val"): "24"})
    ET.SubElement(r_pr, w_tag("szCs"), {w_tag("val"): "24"})
    ET.SubElement(run, w_tag("t")).text = ""
    return p


def make_cover_heading(text: str, size: str, before: str, after: str, line: str) -> ET.Element:
    p = ET.Element(w_tag("p"))
    p_pr = ET.SubElement(p, w_tag("pPr"))
    ET.SubElement(
        p_pr,
        w_tag("spacing"),
        {
            w_tag("before"): before,
            w_tag("after"): after,
            w_tag("line"): line,
            w_tag("lineRule"): "exact",
        },
    )
    ET.SubElement(p_pr, w_tag("jc"), {w_tag("val"): "center"})
    run = ET.SubElement(p, w_tag("r"))
    r_pr = ET.SubElement(run, w_tag("rPr"))
    ET.SubElement(
        r_pr,
        w_tag("rFonts"),
        {
            w_tag("ascii"): "Arial",
            w_tag("hAnsi"): "Arial",
            w_tag("eastAsia"): "SimHei",
            w_tag("cs"): "Arial",
        },
    )
    ET.SubElement(r_pr, w_tag("b"))
    ET.SubElement(r_pr, w_tag("bCs"))
    ET.SubElement(r_pr, w_tag("sz"), {w_tag("val"): size})
    ET.SubElement(r_pr, w_tag("szCs"), {w_tag("val"): size})
    ET.SubElement(run, w_tag("t")).text = text
    return p


def replace_cover(document_xml: bytes, *, title: str, student: str, student_id: str, institute: str, class_name: str, major: str, teacher: str, date_text: str) -> bytes:
    root = ET.fromstring(document_xml)
    body = root.find(w_tag("body"))
    if body is None:
        raise RuntimeError("document body not found")

    children = list(body)
    section_break_index = None
    for idx, child in enumerate(children):
        if child.tag != w_tag("p"):
            continue
        p_pr = child.find(w_tag("pPr"))
        if p_pr is not None and p_pr.find(w_tag("sectPr")) is not None:
            section_break_index = idx
            break
    if section_break_index is None:
        raise RuntimeError("cover section break not found")

    for _ in range(section_break_index):
        body.remove(body[0])

    cover_nodes = [
        make_blank_paragraph(),
        make_blank_paragraph(),
        make_cover_heading("唐山学院毕业设计（论文）", size="36", before="600", after="600", line="400"),
        make_cover_heading(title, size="52", before="800", after="800", line="520"),
        make_info_paragraph(f"学生姓名：{student}"),
        make_info_paragraph(f"学    号：{student_id}"),
        make_info_paragraph(f"学    院：{institute}"),
        make_info_paragraph(f"班    级：{class_name}"),
        make_info_paragraph(f"专    业：{major}"),
        make_info_paragraph(f"指导教师：{teacher}"),
        make_info_paragraph(f"完成日期：{date_text}"),
    ]

    for node in reversed(cover_nodes):
        body.insert(0, node)

    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--student", required=True)
    parser.add_argument("--student-id", required=True)
    parser.add_argument("--institute", required=True)
    parser.add_argument("--class-name", required=True)
    parser.add_argument("--major", required=True)
    parser.add_argument("--teacher", required=True)
    parser.add_argument("--date-text", required=True)
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temp_output_path = output_path.with_suffix(output_path.suffix + ".tmp")

    with zipfile.ZipFile(input_path, "r") as src, zipfile.ZipFile(temp_output_path, "w", zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            data = src.read(item.filename)
            if item.filename == "word/document.xml":
                data = replace_cover(
                    data,
                    title=args.title,
                    student=args.student,
                    student_id=args.student_id,
                    institute=args.institute,
                    class_name=args.class_name,
                    major=args.major,
                    teacher=args.teacher,
                    date_text=args.date_text,
                )
            dst.writestr(item, data)

    temp_output_path.replace(output_path)
    print(output_path)


if __name__ == "__main__":
    main()
