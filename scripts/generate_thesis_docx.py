from __future__ import annotations

import argparse
import copy
import re
import xml.etree.ElementTree as ET
import zipfile
from dataclasses import dataclass
from pathlib import Path


NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
NS_CP = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
NS_DC = "http://purl.org/dc/elements/1.1/"
NS_DCTERMS = "http://purl.org/dc/terms/"
NS_XSI = "http://www.w3.org/2001/XMLSchema-instance"

ET.register_namespace("w", NS_W)
ET.register_namespace("r", NS_R)
ET.register_namespace("cp", NS_CP)
ET.register_namespace("dc", NS_DC)
ET.register_namespace("dcterms", NS_DCTERMS)
ET.register_namespace("xsi", NS_XSI)


def w_tag(name: str) -> str:
    return f"{{{NS_W}}}{name}"


def text_of_paragraph(paragraph: ET.Element) -> str:
    return "".join(node.text or "" for node in paragraph.findall(".//w:t", {"w": NS_W})).strip()


def parse_docx_paragraphs(path: Path) -> list[str]:
    with zipfile.ZipFile(path) as zf:
        root = ET.fromstring(zf.read("word/document.xml"))
    body = root.find(w_tag("body"))
    if body is None:
        return []
    texts: list[str] = []
    for paragraph in body.findall(w_tag("p")):
        text = text_of_paragraph(paragraph)
        if text:
            texts.append(text)
    return texts


def read_markdown_sections(path: Path) -> list[dict]:
    lines = path.read_text(encoding="utf-8").splitlines()
    sections: list[dict] = []
    current: dict | None = None
    for raw_line in lines:
        line = raw_line.rstrip().lstrip("\ufeff")
        match = re.match(r"^(#{1,3})\s+(.*)$", line)
        if match:
            if current is not None:
                sections.append(current)
            current = {
                "level": len(match.group(1)),
                "title": match.group(2).strip(),
                "lines": [],
            }
            continue
        if current is not None:
            current["lines"].append(line)
    if current is not None:
        sections.append(current)
    return sections


def clean_inline_markdown(text: str) -> str:
    text = text.replace("`", "")
    text = text.replace("**", "")
    text = text.replace("*", "")
    return text.strip()


def split_blocks(lines: list[str]) -> list[tuple[str, object]]:
    blocks: list[tuple[str, object]] = []
    paragraph_parts: list[str] = []
    table_lines: list[str] = []

    def flush_paragraph():
        nonlocal paragraph_parts
        if paragraph_parts:
            blocks.append(("paragraph", clean_inline_markdown(" ".join(part.strip() for part in paragraph_parts if part.strip()))))
            paragraph_parts = []

    def flush_table():
        nonlocal table_lines
        if not table_lines:
            return
        rows = []
        for line in table_lines:
            cols = [item.strip() for item in line.strip().strip("|").split("|")]
            if cols and all(cols):
                rows.append(cols)
        if len(rows) >= 2:
            header = rows[0]
            for row in rows[2:]:
                if len(row) >= 3:
                    blocks.append(
                        (
                            "paragraph",
                            f"{row[0]}：主要字段包括 {row[1]}，说明为{row[2]}。",
                        )
                    )
        table_lines = []

    for raw_line in lines:
        line = raw_line.strip()
        if not line:
            flush_paragraph()
            flush_table()
            continue
        if line.startswith("|"):
            flush_paragraph()
            table_lines.append(line)
            continue
        flush_table()
        cleaned = clean_inline_markdown(line)
        if re.match(r"^\d+\.\s+", cleaned):
            flush_paragraph()
            blocks.append(("list", cleaned))
        elif re.match(r"^\[[0-9]+\]\s+", cleaned):
            flush_paragraph()
            blocks.append(("reference", cleaned))
        else:
            paragraph_parts.append(cleaned)
    flush_paragraph()
    flush_table()
    return blocks


def extract_cover_info(initial_docx: Path) -> dict[str, str]:
    info = {
        "title": "基于 Spring Boot 的程序设计评测系统的设计与实现",
        "student": "李融荣",
        "class_name": "22计本3",
        "teacher": "王雅坤",
        "date": "二〇二六年四月",
        "major": "计算机科学与技术",
    }
    for paragraph in parse_docx_paragraphs(initial_docx):
        if paragraph.startswith("题目："):
            info["title"] = paragraph.split("：", 1)[1].strip()
        elif paragraph.startswith("学生："):
            match = re.search(r"学生：(.+?)\s+班级：(.+?)\s+指导教师：(.+)", paragraph)
            if match:
                info["student"] = match.group(1).strip()
                info["class_name"] = match.group(2).strip()
                info["teacher"] = match.group(3).strip()
        elif paragraph.startswith("完成日期："):
            raw_date = paragraph.split("：", 1)[1].strip()
            info["date"] = normalize_chinese_date(raw_date)
    return info


def normalize_chinese_date(raw_date: str) -> str:
    year_match = re.search(r"(\d{4})年", raw_date)
    month_match = re.search(r"(\d{1,2})月", raw_date)
    if not year_match:
        return raw_date
    digits = {
        "0": "〇",
        "1": "一",
        "2": "二",
        "3": "三",
        "4": "四",
        "5": "五",
        "6": "六",
        "7": "七",
        "8": "八",
        "9": "九",
    }
    year = "".join(digits[ch] for ch in year_match.group(1))
    month_num = int(month_match.group(1)) if month_match else 4
    month_map = {
        1: "一",
        2: "二",
        3: "三",
        4: "四",
        5: "五",
        6: "六",
        7: "七",
        8: "八",
        9: "九",
        10: "十",
        11: "十一",
        12: "十二",
    }
    return f"{year}年{month_map.get(month_num, '四')}月"


def build_sections_lookup(sections: list[dict]) -> dict[str, dict]:
    return {section["title"]: section for section in sections}


def supplementary_paragraphs() -> dict[str, list[str]]:
    return {
        "2.2 Spring Boot 技术": [
            "结合项目代码结构，后端以 Spring Boot 2.7.18 为核心框架，并集成 Spring Security、Validation、MyBatis-Plus、Flyway、Redis 与 JWT 等组件，形成“接口接入、业务处理、数据访问、权限控制、运行监测”较为完整的企业级开发基础设施。",
        ],
        "2.3 Vue 技术": [
            "前端工程基于 Vue 3 生态实现，配合 Vue Router、Pinia、Axios 与 Element Plus 组织页面、状态与接口调用流程，能够较好支撑题库检索、提交列表、竞赛页面和讨论交流等交互场景。",
        ],
        "4.3.1 数据库概念结构设计": [
            "依据当前项目 ER 设计，核心实体进一步细化为用户、学生档案、教师档案、管理员档案、题目、测试点、提交记录、判题结果、竞赛、竞赛题目关联、竞赛参与、竞赛成绩、讨论帖子、讨论评论、系统配置和管理员操作日志等对象。",
        ],
        "5.1 用户认证与权限模块实现": [
            "从控制器层看，系统通过 AuthController、UserController 与相关拦截逻辑完成注册、登录、用户信息查询以及基于角色的访问控制，保证学生、教师、管理员在不同功能入口下具有清晰的权限边界。",
        ],
        "5.2 题库与判题模块实现": [
            "题库与判题链路由 ProblemController、SubmissionController、TestCaseController 以及 JudgeService 等模块协同完成，能够实现题目查询、测试点维护、提交轮询、结果持久化与状态回写等关键流程。",
        ],
        "5.3 竞赛管理模块实现": [
            "竞赛功能主要由 ContestController 和相关服务模块支撑，系统支持竞赛时间区间、题目关联、报名记录、排行榜统计以及罚时策略扩展，满足课程练习赛与基础校内赛场景。",
        ],
        "5.4 讨论区模块实现": [
            "社区交流部分由 DiscussionController、DiscussionCommentController 与 SocialController 共同支撑，形成发帖、评论、回复和互动的内容闭环，有助于学生在解题之后开展经验总结与同伴互助。",
        ],
        "5.5 后台管理与统计实现": [
            "系统后台提供 AdminSystemController、TeacherAnalyticsController、SystemController 等接口，用于查看系统监控、教学统计、配置项和运行状态，为系统维护与教学管理提供数据依据。",
        ],
        "6.1 测试环境与测试方法": [
            "项目仓库中已提供后端检查脚本、接口冒烟脚本和性能冒烟脚本，说明系统测试不仅覆盖页面功能，也覆盖了接口可用性与基础性能验证，具有一定工程实践基础。",
        ],
        "附录1：关键接口清单": [
            "控制器层当前包括 AuthController、ProblemController、SubmissionController、ContestController、DiscussionController、DiscussionCommentController、SocialController、TeacherAnalyticsController、UserController 等接口入口。",
        ],
        "附录2：数据库表结构补充": [
            "数据库迁移脚本已按版本方式管理，目前包含竞赛冻结榜与罚时配置、讨论回复与社交能力、用户头像字段、提交样例结果、帖子审核等增量变更记录，便于后续维护与演进。",
        ],
    }


@dataclass
class ParagraphSpec:
    text: str
    east_font: str = "SimSun"
    ascii_font: str = "Times New Roman"
    size: int = 24
    bold: bool = False
    align: str = "both"
    before: int = 0
    after: int = 0
    line: int = 400
    first_line: int = 0


class DocBuilder:
    def __init__(self, title: str):
        self.root = ET.Element(w_tag("document"))
        self.body = ET.SubElement(self.root, w_tag("body"))
        self.title = title

    def add_paragraph(self, spec: ParagraphSpec):
        paragraph = ET.SubElement(self.body, w_tag("p"))
        p_pr = ET.SubElement(paragraph, w_tag("pPr"))
        ET.SubElement(
            p_pr,
            w_tag("spacing"),
            {
                w_tag("before"): str(spec.before),
                w_tag("after"): str(spec.after),
                w_tag("line"): str(spec.line),
                w_tag("lineRule"): "exact",
            },
        )
        if spec.first_line:
            ET.SubElement(p_pr, w_tag("ind"), {w_tag("firstLine"): str(spec.first_line)})
        if spec.align != "left":
            ET.SubElement(p_pr, w_tag("jc"), {w_tag("val"): spec.align})
        run = ET.SubElement(paragraph, w_tag("r"))
        r_pr = ET.SubElement(run, w_tag("rPr"))
        ET.SubElement(
            r_pr,
            w_tag("rFonts"),
            {
                w_tag("ascii"): spec.ascii_font,
                w_tag("hAnsi"): spec.ascii_font,
                w_tag("eastAsia"): spec.east_font,
                w_tag("cs"): spec.ascii_font,
            },
        )
        if spec.bold:
            ET.SubElement(r_pr, w_tag("b"))
            ET.SubElement(r_pr, w_tag("bCs"))
        ET.SubElement(r_pr, w_tag("sz"), {w_tag("val"): str(spec.size)})
        ET.SubElement(r_pr, w_tag("szCs"), {w_tag("val"): str(spec.size)})
        text_el = ET.SubElement(run, w_tag("t"))
        if spec.text.startswith(" ") or spec.text.endswith(" ") or "  " in spec.text:
            text_el.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        text_el.text = spec.text

    def add_blank(self, line: int = 400):
        self.add_paragraph(ParagraphSpec(text="", line=line))

    def add_page_break(self):
        paragraph = ET.SubElement(self.body, w_tag("p"))
        run = ET.SubElement(paragraph, w_tag("r"))
        ET.SubElement(run, w_tag("br"), {w_tag("type"): "page"})

    def _append_common_sectpr(
        self,
        sect_pr: ET.Element,
        *,
        header_rid: str | None = None,
        footer_rid: str | None = None,
        first_header_rid: str | None = None,
        first_footer_rid: str | None = None,
        page_num_fmt: str | None = None,
        page_num_start: int | None = None,
        next_page: bool = False,
        title_page: bool = False,
    ):
        if first_header_rid:
            ET.SubElement(
                sect_pr,
                w_tag("headerReference"),
                {w_tag("type"): "first", f"{{{NS_R}}}id": first_header_rid},
            )
        if first_footer_rid:
            ET.SubElement(
                sect_pr,
                w_tag("footerReference"),
                {w_tag("type"): "first", f"{{{NS_R}}}id": first_footer_rid},
            )
        if header_rid:
            ET.SubElement(
                sect_pr,
                w_tag("headerReference"),
                {w_tag("type"): "default", f"{{{NS_R}}}id": header_rid},
            )
        if footer_rid:
            ET.SubElement(
                sect_pr,
                w_tag("footerReference"),
                {w_tag("type"): "default", f"{{{NS_R}}}id": footer_rid},
            )
        if title_page:
            ET.SubElement(sect_pr, w_tag("titlePg"))
        if next_page:
            ET.SubElement(sect_pr, w_tag("type"), {w_tag("val"): "nextPage"})
        ET.SubElement(sect_pr, w_tag("pgSz"), {w_tag("w"): "11906", w_tag("h"): "16838"})
        ET.SubElement(
            sect_pr,
            w_tag("pgMar"),
            {
                w_tag("top"): "1418",
                w_tag("right"): "1134",
                w_tag("bottom"): "1134",
                w_tag("left"): "1134",
                w_tag("header"): "851",
                w_tag("footer"): "992",
                w_tag("gutter"): "284",
            },
        )
        if page_num_fmt or page_num_start is not None:
            attrs: dict[str, str] = {}
            if page_num_fmt:
                attrs[w_tag("fmt")] = page_num_fmt
            if page_num_start is not None:
                attrs[w_tag("start")] = str(page_num_start)
            ET.SubElement(sect_pr, w_tag("pgNumType"), attrs)
        ET.SubElement(sect_pr, w_tag("cols"), {w_tag("space"): "425", w_tag("num"): "1"})
        ET.SubElement(sect_pr, w_tag("docGrid"), {w_tag("linePitch"): "326", w_tag("charSpace"): "0"})

    def add_section_break(
        self,
        *,
        header_rid: str | None = None,
        footer_rid: str | None = None,
        first_header_rid: str | None = None,
        first_footer_rid: str | None = None,
        page_num_fmt: str | None = None,
        page_num_start: int | None = None,
        title_page: bool = False,
    ):
        paragraph = ET.SubElement(self.body, w_tag("p"))
        p_pr = ET.SubElement(paragraph, w_tag("pPr"))
        sect_pr = ET.SubElement(p_pr, w_tag("sectPr"))
        self._append_common_sectpr(
            sect_pr,
            header_rid=header_rid,
            footer_rid=footer_rid,
            first_header_rid=first_header_rid,
            first_footer_rid=first_footer_rid,
            page_num_fmt=page_num_fmt,
            page_num_start=page_num_start,
            next_page=True,
            title_page=title_page,
        )

    def finalize(self) -> bytes:
        sect_pr = ET.SubElement(self.body, w_tag("sectPr"))
        self._append_common_sectpr(
            sect_pr,
            header_rid="rId7",
            footer_rid="rId9",
            page_num_fmt="decimal",
            page_num_start=1,
        )
        return ET.tostring(self.root, encoding="utf-8", xml_declaration=True)


def add_chapter_heading(builder: DocBuilder, title: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=title,
            east_font="SimHei",
            ascii_font="Arial",
            size=32,
            bold=True,
            align="center",
            before=800,
            after=400,
        )
    )


def add_section_heading(builder: DocBuilder, title: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=title,
            east_font="SimHei",
            ascii_font="Arial",
            size=28,
            bold=True,
            before=480,
            after=120,
        )
    )


def add_subsection_heading(builder: DocBuilder, title: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=title,
            east_font="SimHei",
            ascii_font="Arial",
            size=26,
            bold=True,
            before=240,
            after=120,
        )
    )


def add_body_paragraph(builder: DocBuilder, text: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=text,
            east_font="SimSun",
            ascii_font="Times New Roman",
            size=24,
            first_line=480,
        )
    )


def add_body_list(builder: DocBuilder, text: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=text,
            east_font="SimSun",
            ascii_font="Times New Roman",
            size=24,
            first_line=0,
        )
    )


def add_reference_paragraph(builder: DocBuilder, text: str):
    builder.add_paragraph(
        ParagraphSpec(
            text=text,
            east_font="SimSun",
            ascii_font="Times New Roman",
            size=21,
            line=320,
            before=60,
            after=0,
        )
    )


def render_cover(builder: DocBuilder, info: dict[str, str]):
    builder.add_blank()
    builder.add_blank()
    builder.add_paragraph(
        ParagraphSpec(
            text="唐山学院毕业设计（论文）",
            east_font="SimHei",
            ascii_font="Arial",
            size=36,
            bold=True,
            align="center",
            before=600,
            after=600,
        )
    )
    builder.add_paragraph(
        ParagraphSpec(
            text=info["title"],
            east_font="SimHei",
            ascii_font="Arial",
            size=52,
            bold=True,
            align="center",
            line=520,
            before=800,
            after=800,
        )
    )
    cover_lines = [
        f"学生姓名：{info['student']}",
        f"班    级：{info['class_name']}",
        f"专    业：{info['major']}",
        f"指导教师：{info['teacher']}",
        f"完成日期：{info['date']}",
    ]
    for line in cover_lines:
        builder.add_paragraph(
            ParagraphSpec(
                text=line,
                east_font="SimSun",
                ascii_font="Times New Roman",
                size=32,
                align="center",
                before=120,
                after=120,
            )
        )


def render_statement_page(builder: DocBuilder, title: str, lines: list[str]):
    add_chapter_heading(builder, title)
    for kind, content in split_blocks(lines):
        if kind == "paragraph":
            add_body_paragraph(builder, str(content))
        else:
            add_body_list(builder, str(content))
    builder.add_page_break()


def render_abstract_pages(builder: DocBuilder, lookup: dict[str, dict]):
    add_chapter_heading(builder, "摘    要")
    zh_lines = lookup.get("摘要", {}).get("lines", [])
    for kind, content in split_blocks(zh_lines):
        if kind == "paragraph":
            add_body_paragraph(builder, str(content))
        elif kind == "list":
            add_body_list(builder, str(content))
    keywords = "；".join(item.strip() for item in lookup.get("关键词", {}).get("lines", []) if item.strip())
    builder.add_blank()
    builder.add_paragraph(
        ParagraphSpec(
            text=f"关键词：{keywords}",
            east_font="SimHei",
            ascii_font="Times New Roman",
            size=24,
            bold=True,
            first_line=0,
        )
    )
    builder.add_page_break()

    english_title = ""
    title_lines = lookup.get("Title", {}).get("lines", [])
    if title_lines:
        english_title = clean_inline_markdown(" ".join(item.strip() for item in title_lines if item.strip()))
    builder.add_paragraph(
        ParagraphSpec(
            text=english_title,
            east_font="SimHei",
            ascii_font="Arial",
            size=28,
            bold=True,
            align="center",
            before=500,
            after=300,
        )
    )
    builder.add_paragraph(
        ParagraphSpec(
            text="ABSTRACT",
            east_font="SimHei",
            ascii_font="Arial",
            size=32,
            bold=True,
            align="center",
            before=800,
            after=400,
        )
    )
    en_lines = lookup.get("Abstract", {}).get("lines", [])
    for kind, content in split_blocks(en_lines):
        if kind == "paragraph":
            builder.add_paragraph(
                ParagraphSpec(
                    text=str(content),
                    east_font="SimSun",
                    ascii_font="Times New Roman",
                    size=24,
                )
            )
    en_keywords = "；".join(item.strip() for item in lookup.get("Key words", {}).get("lines", []) if item.strip())
    builder.add_blank()
    builder.add_paragraph(
        ParagraphSpec(
            text=f"Key words: {en_keywords}",
            east_font="SimSun",
            ascii_font="Times New Roman",
            size=24,
            bold=True,
        )
    )
    builder.add_page_break()


def render_toc_page(builder: DocBuilder, toc_lines: list[str]):
    add_chapter_heading(builder, "目    录")
    for line in toc_lines:
        text = line.strip()
        if not text:
            continue
        is_chapter = bool(re.match(r"^\d+\s", text)) or text.startswith("参考文献") or text.startswith("附录") or text.startswith("致谢")
        builder.add_paragraph(
            ParagraphSpec(
                text=text,
                east_font="SimSun" if not is_chapter else "SimHei",
                ascii_font="Times New Roman",
                size=24,
                bold=is_chapter,
                before=120 if is_chapter else 0,
                after=0,
                line=400,
            )
        )


def render_body(builder: DocBuilder, sections: list[dict]):
    extra_map = supplementary_paragraphs()
    in_body = False
    for section in sections:
        title = section["title"]
        if title == "1 引言":
            in_body = True
        if not in_body:
            continue

        if section["level"] == 1:
            add_chapter_heading(builder, title)
        elif section["level"] == 2:
            add_section_heading(builder, title)
        elif section["level"] == 3:
            add_subsection_heading(builder, title)

        for kind, content in split_blocks(section["lines"]):
            if kind == "paragraph":
                if title == "参考文献":
                    add_reference_paragraph(builder, str(content))
                else:
                    add_body_paragraph(builder, str(content))
            elif kind == "reference":
                add_reference_paragraph(builder, str(content))
            else:
                add_body_list(builder, str(content))

        for paragraph in extra_map.get(title, []):
            add_body_paragraph(builder, paragraph)


def update_core_properties(xml_bytes: bytes, title: str) -> bytes:
    root = ET.fromstring(xml_bytes)
    title_el = root.find(f"{{{NS_DC}}}title")
    if title_el is None:
        title_el = ET.SubElement(root, f"{{{NS_DC}}}title")
    title_el.text = title
    creator_el = root.find(f"{{{NS_DC}}}creator")
    if creator_el is None:
        creator_el = ET.SubElement(root, f"{{{NS_DC}}}creator")
    creator_el.text = "Codex"
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def build_output(base_docx: Path, output_docx: Path, document_xml: bytes, title: str):
    output_docx.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(base_docx, "r") as src, zipfile.ZipFile(output_docx, "w", zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            data = src.read(item.filename)
            if item.filename == "word/document.xml":
                data = document_xml
            elif item.filename == "docProps/core.xml":
                data = update_core_properties(data, title)
            dst.writestr(item, data)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--markdown", required=True)
    parser.add_argument("--initial-docx", required=True)
    parser.add_argument("--base-docx", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    markdown_path = Path(args.markdown)
    initial_docx_path = Path(args.initial_docx)
    base_docx_path = Path(args.base_docx)
    output_path = Path(args.output)

    sections = read_markdown_sections(markdown_path)
    lookup = build_sections_lookup(sections)
    cover_info = extract_cover_info(initial_docx_path)

    builder = DocBuilder(title=cover_info["title"])
    render_cover(builder, cover_info)
    builder.add_section_break()
    render_statement_page(builder, "毕业设计（论文）原创性声明", lookup["毕业设计（论文）原创性声明"]["lines"])
    render_statement_page(builder, "毕业设计（论文）版权使用授权书", lookup["毕业设计（论文）版权使用授权书"]["lines"])
    render_abstract_pages(builder, lookup)
    render_toc_page(builder, lookup["目  录"]["lines"])
    builder.add_section_break(header_rid="rId7", footer_rid="rId9", page_num_fmt="upperRoman", page_num_start=1)
    render_body(builder, sections)

    document_xml = builder.finalize()
    build_output(base_docx_path, output_path, document_xml, cover_info["title"])
    print(output_path)


if __name__ == "__main__":
    main()
