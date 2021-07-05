package com.viclab.hwpreader;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlID;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class hwpReader {

	public static void main(String[] args) {
		System.out.println("HWP READER");

		List<String> argsList = new ArrayList<String>();
		List<String> optsList = new ArrayList<String>();

		if (args == null || args.length == 0) {
			System.out.println("인자가 없습니다");
			return;
		}
		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(0)) {
				case '-':
					if (args[i].length() < 2) {
						throw new IllegalArgumentException("Not a valid argument: " + args[i]);
					}
					if (args[i].charAt(1) == '-') {
						throw new IllegalArgumentException("Not a valid argument: " + args[i]);
					}
					optsList.add(args[i].substring(1));
					break;
				default:
					argsList.add(args[i]);
					break;
			}
		}

		if (argsList.size() == 0) {
			System.out.println("파일 경로가 없습니다");
		}

		HWPFile hwpFile = null;
		try {

			String filePath = argsList.get(0);
			File file = new File(filePath);
			String fileName = file.getName();
			if (fileName.contains(".")) fileName = fileName.substring(0, fileName.indexOf("."));

			hwpFile = HWPReader.fromFile(argsList.get(0));
			String hwpText = TextExtractor.extract(hwpFile, TextExtractMethod.AppendControlTextAfterParagraphText);

			if (optsList.contains("plain")) {
				System.out.println("PRINT PLAIN TEXT");
				System.out.println(hwpText);
			}

			if (optsList.contains("paragraph")) {
				System.out.println("PRINT PARAGRAPH TEXT");
				System.out.println("total text : " + hwpText.length());
				System.out.println("section size : " + hwpFile.getBodyText().getSectionList().size());
				for (int sectionIndex = 0; sectionIndex < hwpFile.getBodyText().getSectionList().size(); sectionIndex ++) {
					Section section = hwpFile.getBodyText().getSectionList().get(sectionIndex);
					System.out.println("section[" + sectionIndex + "] paragraph size : " + section.getParagraphCount());
					for (int paragraphIndex = 0; paragraphIndex < section.getParagraphCount(); paragraphIndex ++) {
						Paragraph paragraph = section.getParagraph(paragraphIndex);
						if (paragraph.getNormalString() != null && paragraph.getNormalString().length() > 0) System.out.println("section[" + sectionIndex + "] paragraph[" + paragraphIndex + "] : " + paragraph.getNormalString());
					}
				}
			}

			if (optsList.contains("table")) {
				System.out.println("PRINT TABLE TEXT");
				for (int sectionIndex = 0; sectionIndex < hwpFile.getBodyText().getSectionList().size(); sectionIndex ++) {
					Section section = hwpFile.getBodyText().getSectionList().get(sectionIndex);
					for (int paragraphIndex = 0; paragraphIndex < section.getParagraphCount(); paragraphIndex ++) {
						Paragraph paragraph = section.getParagraph(paragraphIndex);
						if (paragraph.getControlList() != null) {
							//System.out.println("section[" + sectionIndex + "] paragraph[" + paragraphIndex + "] : control size : " + paragraph.getControlList().size());
						} else {
							//System.out.println("section[" + sectionIndex + "] paragraph[" + paragraphIndex + "] : control empty");
							continue;
						}
						for (int controlIndex = 0; controlIndex < paragraph.getControlList().size(); controlIndex ++) {
							Control control = paragraph.getControlList().get(controlIndex);
							if (control.getType() == ControlType.Table) {
								ControlTable controlTable = (ControlTable) control;
								System.out.println("section[" + sectionIndex + "] paragraph[" + paragraphIndex + "] TABLE PRINT");
								for (int rowIndex = 0; rowIndex < controlTable.getRowList().size(); rowIndex ++) {
									Row row = controlTable.getRowList().get(rowIndex);
									for (int cellIndex = 0; cellIndex < row.getCellList().size(); cellIndex ++) {
										Cell cell = row.getCellList().get(cellIndex);
										System.out.println("row[" + rowIndex + "] cell[" + cellIndex + "] : " + cell.getParagraphList().getNormalString().replace("\n", ""));
									}
								}
							}
						}
					}
				}
			}

			if (optsList.contains("xml")) {
				System.out.println("MAKING XML OUTPUT");

				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

				Document document = documentBuilder.newDocument();
				Element rootElement = document.createElement("hangeul");
				document.appendChild(rootElement);

				for (int sectionIndex = 0; sectionIndex < hwpFile.getBodyText().getSectionList().size(); sectionIndex ++) {
					Section section = hwpFile.getBodyText().getSectionList().get(sectionIndex);
					Element sectionElement = document.createElement("section");
					sectionElement.setAttribute("id", String.valueOf(sectionIndex));
					rootElement.appendChild(sectionElement);
					for (int paragraphIndex = 0; paragraphIndex < section.getParagraphCount(); paragraphIndex ++) {
						Paragraph paragraph = section.getParagraph(paragraphIndex);
						Element paragraphElement = document.createElement("paragraph");
						paragraphElement.setAttribute("id", String.valueOf(paragraphIndex));
						sectionElement.appendChild(paragraphElement);
						if (paragraph.getControlList() == null) {
							continue;
						}
						Element paragraphTextElement = document.createElement("text");
						paragraphElement.appendChild(paragraphTextElement);
						if (paragraph.getNormalString() != null && paragraph.getNormalString().length() > 0) {
							paragraphTextElement.appendChild(document.createTextNode(paragraph.getNormalString()));
						} else {
							paragraphTextElement.appendChild(document.createTextNode(""));
						}
						for (int controlIndex = 0; controlIndex < paragraph.getControlList().size(); controlIndex ++) {
							Control control = paragraph.getControlList().get(controlIndex);
							Element controlElement = document.createElement("control");
							controlElement.setAttribute("id", String.valueOf(controlIndex));
							controlElement.setAttribute("type", typeToString(control.getType()));
							paragraphElement.appendChild(controlElement);
							if (control.getType() == ControlType.Table) {
								ControlTable controlTable = (ControlTable) control;
							//	System.out.println("section[" + sectionIndex + "] paragraph[" + paragraphIndex + "] TABLE PRINT");
								for (int rowIndex = 0; rowIndex < controlTable.getRowList().size(); rowIndex ++) {
									Row row = controlTable.getRowList().get(rowIndex);
									Element rowElement = document.createElement("row");
									controlElement.appendChild(rowElement);
									for (int cellIndex = 0; cellIndex < row.getCellList().size(); cellIndex ++) {
										Cell cell = row.getCellList().get(cellIndex);
										Element cellElement = document.createElement("cell");
										cellElement.appendChild(document.createTextNode(cell.getParagraphList().getNormalString().replace("\n", "")));
										rowElement.appendChild(cellElement);
									}
								}
							}
						}
					}
				}

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				DOMSource domSource = new DOMSource(document);
				StreamResult result = new StreamResult(new FileOutputStream(new File("./" + fileName + ".xml")));
				StreamResult textResult = new StreamResult(System.out);
				transformer.transform(domSource, result);
				System.out.println("XML SUCCESS : " + fileName + ".xml");
			}


		} catch (Exception e) {
			System.out.println("오류가 발생했습니다\n" + e.getMessage());
			e.printStackTrace();
		}


	}

	private static String typeToString(ControlType controlType) {
		switch (controlType) {
			case Table : return "Table";
			case Gso : return "Gso";
			case Equation : return "Equation";
			case SectionDefine : return "SectionDefine";
			case ColumnDefine : return "ColumnDefine";
			case Header : return "Header";
			case Footer : return "Footer";
			case Footnote : return "Footnote";
			case Endnote : return "Endnote";
			case AutoNumber : return "AutoNumber";
			case NewNumber : return "NewNumber";
			case PageHide : return "PageHide";
			case PageOddEvenAdjust : return "PageOddEvenAdjust";
			case PageNumberPositon : return "PageNumberPositon";
			case IndexMark : return "IndexMark";
			case Bookmark : return "Bookmark";
			case OverlappingLetter : return "OverlappingLetter";
			case AdditionalText : return "AdditionalText";
			case HiddenComment : return "HiddenComment";
			case FIELD_UNKNOWN : return "FIELD_UNKNOWN";
			case FIELD_DATE : return "FIELD_DATE";
			case FIELD_DOCDATE : return "FIELD_DOCDATE";
			case FIELD_PATH : return "FIELD_PATH";
			case FIELD_BOOKMARK : return "FIELD_BOOKMARK";
			case FIELD_MAILMERGE : return "FIELD_MAILMERGE";
			case FIELD_CROSSREF : return "FIELD_CROSSREF";
			case FIELD_FORMULA : return "FIELD_FORMULA";
			case FIELD_CLICKHERE : return "FIELD_CLICKHERE";
			case FIELD_SUMMARY : return "FIELD_SUMMARY";
			case FIELD_USERINFO : return "FIELD_USERINFO";
			case FIELD_HYPERLINK : return "FIELD_HYPERLINK";
			case FIELD_REVISION_SIGN : return "FIELD_REVISION_SIGN";
			case FIELD_REVISION_DELETE : return "FIELD_REVISION_DELETE";
			case FIELD_REVISION_ATTACH : return "FIELD_REVISION_ATTACH";
			case FIELD_REVISION_CLIPPING : return "FIELD_REVISION_CLIPPING";
			case FIELD_REVISION_THINKING : return "FIELD_REVISION_THINKING";
			case FIELD_REVISION_PRAISE : return "FIELD_REVISION_PRAISE";
			case FIELD_REVISION_LINE : return "FIELD_REVISION_LINE";
			case FIELD_REVISION_SIMPLECHANGE : return "FIELD_REVISION_SIMPLECHANGE";
			case FIELD_REVISION_HYPERLINK : return "FIELD_REVISION_HYPERLINK";
			case FIELD_REVISION_LINEATTACH : return "FIELD_REVISION_LINEATTACH";
			case FIELD_REVISION_LINELINK : return "FIELD_REVISION_LINELINK";
			case FIELD_REVISION_LINETRANSFER : return "FIELD_REVISION_LINETRANSFER";
			case FIELD_REVISION_RIGHTMOVE : return "FIELD_REVISION_RIGHTMOVE";
			case FIELD_REVISION_LEFTMOVE : return "FIELD_REVISION_LEFTMOVE";
			case FIELD_REVISION_TRANSFER : return "FIELD_REVISION_TRANSFER";
			case FIELD_REVISION_SIMPLEINSERT : return "FIELD_REVISION_SIMPLEINSERT";
			case FIELD_REVISION_SPLIT : return "FIELD_REVISION_SPLIT";
			case FIELD_REVISION_CHANGE : return "FIELD_REVISION_CHANGE";
			case FIELD_MEMO : return "FIELD_MEMO";
			case FIELD_PRIVATE_INFO_SECURITY : return "FIELD_PRIVATE_INFO_SECURITY";
			default : return "";
		}
	}

}
