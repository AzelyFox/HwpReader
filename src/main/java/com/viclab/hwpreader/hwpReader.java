package com.viclab.hwpreader;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;

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


		} catch (Exception e) {
			System.out.println("오류가 발생했습니다\n" + e.getMessage());
			e.printStackTrace();
		}


	}

}
