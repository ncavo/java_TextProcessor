package JiraParser.ncavo.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
	
public class JiraParser {
	public static void main(String[] args) {
		boolean print_test01 = false;
		boolean print_jira = false;
		boolean print_weeklyReport = false;
		boolean print_release = false;
		Map<String, String> memberMap = new HashMap<String, String>();
		try {
			Scanner scanner = new Scanner(new File("config.ini"), StandardCharsets.UTF_8);
			String group = "xxx";
			int lineNum = 0;
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine().strip();
				lineNum++;
				if(line.isBlank()) continue;
				if(line.startsWith("[") && line.endsWith("]")) {
					group = line;
				}
				else if(group.equals("[출력]")) {
					String[] token = line.split("=");
					if(token.length != 2) {
						JOptionPane.showMessageDialog(null, String.format("[Line %d] %s <- wrong format", lineNum, line));
						scanner.close();
						return;
					}
					else if(token[0].strip().equalsIgnoreCase("test01")) {
						if(token[1].strip().equalsIgnoreCase("yes") || token[1].strip().equalsIgnoreCase("true") || token[1].strip().equalsIgnoreCase("1")) print_test01 = true;
					}
					else if(token[0].strip().equalsIgnoreCase("jira")) {
						if(token[1].strip().equalsIgnoreCase("yes") || token[1].strip().equalsIgnoreCase("true") || token[1].strip().equalsIgnoreCase("1")) print_jira = true;
					}
					else if(token[0].strip().equalsIgnoreCase("weeklyReport")) {
						if(token[1].strip().equalsIgnoreCase("yes") || token[1].strip().equalsIgnoreCase("true") || token[1].strip().equalsIgnoreCase("1")) print_weeklyReport = true;
					}
					else if(token[0].strip().equalsIgnoreCase("release")) {
						if(token[1].strip().equalsIgnoreCase("yes") || token[1].strip().equalsIgnoreCase("true") || token[1].strip().equalsIgnoreCase("1")) print_release = true;
					}					
				}
				else if(group.equals("[담당자]")) {
					String[] token = line.split("=");
					if(token.length != 2) {
						JOptionPane.showMessageDialog(null, String.format("[Line %d] %s <- wrong format", lineNum, line));
						scanner.close();
						return;
					}
					memberMap.put(token[0].strip(), token[1].strip());
				}
			}
			scanner.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			return;
		}
		
		JFileChooser fc = new JFileChooser();
	    fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if(f.isDirectory()) return true;
				if(f.getName().endsWith(".html")) return true;
				return false;
			}
			@Override
			public String getDescription() {
				return "JIRA Exported HTML";
			}});		
		if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
	    File file = fc.getSelectedFile();
		String fileNameWithoutExt = null;
	    String htmlData = null;
		try {
			fileNameWithoutExt = file.getCanonicalPath();
			fileNameWithoutExt = fileNameWithoutExt.substring(0, fileNameWithoutExt.lastIndexOf("."));						
			FileInputStream in = new FileInputStream(file);
			byte[] htmlBytes = new byte[in.available()];
		    in.read(htmlBytes);
		    in.close();
		    int j = 0;
		    for(int i = 0; i < htmlBytes.length - 1 - j; i++) {
		    	if(j > 0) {
		    		htmlBytes[i] = htmlBytes[i + j]; 
		    		htmlBytes[i + 1] = htmlBytes[i + 1 + j]; 
		    	}		    	
		    	if(htmlBytes[i] == (byte)0xC2 && htmlBytes[i + 1] == (byte)0xA0) {
		    		htmlBytes[i] = 0x20;
		    		j++;
		    	}		
		    }
		    htmlData = new String(htmlBytes, 0, htmlBytes.length - j, StandardCharsets.UTF_8);
		} catch (FileNotFoundException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		    return;
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		    return;
		}

		if(print_test01)
		try {
			FileOutputStream out = new FileOutputStream(new File(fileNameWithoutExt + ".TEST01.txt"));
			out.write(htmlData.getBytes());
			if(out != null) out.close();
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		}

		List<Map<String, String>> list = new LinkedList<Map<String, String>>(); 
		
		String errorMsg = "nothing matched";
		Pattern p1 = Pattern.compile("<tr id=\"(.+?)\">([\\s\\S]*?)<\\/tr>");
		Matcher m1 = p1.matcher(htmlData);
		while(m1.find()) {
			if(m1.groupCount() != 2) {
				errorMsg = m1.group() + "<-- group count error" + System.lineSeparator(); 
				break;
			}
			Map<String, String> map = new HashMap<String, String>();
			map.put("issuerow", m1.group(1).strip());
			Pattern p2 = Pattern.compile("<td class=\"(.+?)\">([\\s\\S]*?)<\\/td>");
			Matcher m2 = p2.matcher(m1.group(2));
			int expKeyCount = 0;
			while(m2.find()) {
				if(m2.groupCount() != 2) {
					errorMsg = m2.group() + "<-- group count error at " + m1.group(1).strip() + System.lineSeparator(); 
					break;
				}
				errorMsg = null;
				String key = m2.group(1).strip();
				String value = m2.group(2).strip();
				if(key.equalsIgnoreCase("issuekey")) {
					expKeyCount++;
					Pattern p3 = Pattern.compile("<a .+?>(.+?)<\\/a>");
					Matcher m3 = p3.matcher(value);
					if(m3.find() && m3.groupCount() == 1) map.put(key, m3.group(1).strip());
					else map.put(key, value);
					continue;
				}
				else if(key.equalsIgnoreCase("status")) {
					expKeyCount++;
					if(value.indexOf("Completed") > 0) { map.put("status", "Completed"); map.put("status2", "01"); } 
					else if(value.indexOf("Live") > 0) { map.put("status", "Live"); map.put("status2", "02"); } 
					else if(value.indexOf("RC") > 0) { map.put("status", "RC"); map.put("status2", "03"); } 
					else if(value.indexOf("Done") > 0 || value.indexOf("완료") > 0) { map.put("status", "완료"); map.put("status2", "04"); } 
					else if(value.indexOf("In Progress") > 0 || value.indexOf("진행 중") > 0) { map.put("status", "진행 중"); map.put("status2", "05"); } 
					else if(value.indexOf("To Do") > 0 || value.indexOf("할 일") > 0) { map.put("status", "할 일"); map.put("status2", "06"); } 
					else if(value.indexOf("Backlog") > 0 || value.indexOf("백로그") > 0) { map.put("status", "백로그"); map.put("status2", "07"); }
					else { map.put("status", value); map.put("status2", "08"); }
					continue;
				}
				else if(key.equalsIgnoreCase("components") || key.equalsIgnoreCase("customfield_14406")) { // 구성 요소 속성
					expKeyCount++;
					String[] values = value.split(",");
					String delimiter = "";
					value = "";
					for(String v : values) {
						v = v.replaceAll("&nbsp;", " ");
						value += delimiter + v.strip();
						delimiter = ",";
					}
					map.put(key, value);
					continue;
				}
				else if(key.equalsIgnoreCase("assignee")) {
					expKeyCount++;
					int a = value.indexOf("(");
					if(a > 0) {
						value = value.substring(0, a);
						String incharge = memberMap.get(value);
						if(incharge != null)
							map.put("incharge", incharge);
					}
					map.put(key, value);
					continue;
				}
				else if(key.equalsIgnoreCase("summary")) {
					expKeyCount++;
					Pattern p3 = Pattern.compile("<p>([\\s\\S]+?)<\\/p>");
					Matcher m3 = p3.matcher(value);
					if(m3.find() && m3.groupCount() == 1) map.put(key, m3.group(1).strip());
					else map.put(key, value);
					continue;
				}
				else if(key.equalsIgnoreCase("customfield_14830") || key.equalsIgnoreCase("customfield_14831")) { // YETI: RC/Live 배포 일시
					expKeyCount++;
					Pattern p3 = Pattern.compile("<time .+?>(.+?)<\\/time>");
					Matcher m3 = p3.matcher(value);
					if(m3.find() && m3.groupCount() == 1) map.put(key, m3.group(1).strip());
					else map.put(key, value);
					continue;
				}
				else if(key.equalsIgnoreCase("created") || key.equalsIgnoreCase("creator") || key.equalsIgnoreCase("description") || key.equalsIgnoreCase("issuetype")
						|| key.equalsIgnoreCase("labels") || key.equalsIgnoreCase("priority") || key.equalsIgnoreCase("project") || key.equalsIgnoreCase("reporter")
						|| key.equalsIgnoreCase("updated")) expKeyCount++;
				if(value.length() > 0)
					map.put(key, value);
			}
			if(expKeyCount < 17) errorMsg = m1.group() + "<-- not enough keys error" + System.lineSeparator(); 
			if(errorMsg != null) break;
			list.add(map);
		}
		
		if(errorMsg != null) {
			try {
				FileOutputStream out = new FileOutputStream(new File(fileNameWithoutExt + ".ERR.txt"));
				out.write(errorMsg.getBytes());
				if(out != null) out.close();
			} catch (IOException e) {
			    JOptionPane.showMessageDialog(null, e.getMessage());
			}
		    return;
		}

		//--------------------------------------------------------------------------------[JIRA]
		list.sort(new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				String s1 = o1.get("customfield_10006"); // 에픽 링크
				String s2 = o2.get("customfield_10006");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}
				
				s1 = o1.get("assignee");
				s2 = o2.get("assignee");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}
				
				s1 = o1.get("issuekey");
				s2 = o2.get("issuekey");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}					
				return 0;
			}
		});
		
		StringBuilder result = new StringBuilder();
		String lastEpic = "xxx";
		for(Map<String, String> map : list) {
			String s = map.get("customfield_10006"); // 에픽 링크
			if(s == null || s.length() == 0) s = "에픽 미지정";
			if(lastEpic.compareTo(s) != 0) {
				lastEpic = s;
				result.append(lastEpic)
				.append(System.lineSeparator())
				.append("||")
				.append("담당자").append("||")
				.append("상태").append("||")
				.append("이슈키").append("||")
				.append("유형").append("||")
				.append("구성 요소").append("||")
				.append("구성 요소 속성").append("||") // 구성 요소 속성
				.append("요약").append("||")
				.append("생성일").append("||")
				.append("변경일").append("||")
				.append("RC 배포일").append("||") // YETI: RC 배포 일시
				.append("Live 배포일").append("||") // YETI: Live 배포 일시 
				.append(System.lineSeparator());
			}
			result.append("|");
			s = map.get("assignee");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s).append("|");
			s = map.get("status");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s).append("|");
			s = map.get("issuekey");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s).append("|");
			s = map.get("issuetype");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s).append("|");
			s = map.get("components");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll(",", System.lineSeparator())).append("|");
			s = map.get("customfield_14406"); // 구성 요소 속성
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll(",", System.lineSeparator())).append("|");
			s = map.get("summary");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s).append("|");
			s = map.get("created");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll("\\s+", System.lineSeparator())).append("|");
			s = map.get("updated");
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll("\\s+", System.lineSeparator())).append("|");
			s = map.get("customfield_14830"); // YETI: RC 배포 일시
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll("\\s+", System.lineSeparator())).append("|");
			s = map.get("customfield_14831"); // YETI: Live 배포 일시
			if(s == null || s.length() == 0) result.append(" |"); else result.append(s.replaceAll("\\s+", System.lineSeparator())).append("|");
			result.append(System.lineSeparator());
		}
		
		if(print_jira)
		try {
			FileOutputStream out = new FileOutputStream(new File(fileNameWithoutExt + ".JIRA.txt"));
			out.write(result.toString().getBytes());
			if(out != null) out.close();
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		    return;
		}

		//--------------------------------------------------------------------------------[WeeklyReport]
		list.sort(new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				String s1 = o1.get("customfield_10006"); // 에픽 링크
				String s2 = o2.get("customfield_10006");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}
				
				s1 = o1.get("incharge");
				s2 = o2.get("incharge");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}
				
				s1 = o1.get("status2");
				s2 = o2.get("status2");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}
				
				s1 = o1.get("issuekey");
				s2 = o2.get("issuekey");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}					
				return 0;
			}
		});
		
		result = new StringBuilder();
		result.append("<tr>").append(System.lineSeparator());
		result.append("<td> </td>").append(System.lineSeparator());
		result.append("<td>").append(System.lineSeparator());
		result.append("</td>").append(System.lineSeparator());
		result.append("<td>").append(System.lineSeparator());
		result.append("</td>").append(System.lineSeparator());
		result.append("</tr>").append(System.lineSeparator()).append(System.lineSeparator());
		result.append("<p>Left</p>").append(System.lineSeparator());
		lastEpic = "xxx";
		String lastInCharge = "xxx";
		String closeElemEpic = "";
		String closeElemInCharge = "";
		for(Map<String, String> map : list) {
			String s = map.get("assignee");
			if(memberMap.get(s) == null) continue;
			s = map.get("status2");
			if(s.equals("06")) continue;
			
			s = map.get("customfield_10006"); // 에픽 링크
			if(s == null || s.length() == 0) s = "에픽 미지정";
			if(lastEpic.compareTo(s) != 0) {
				lastEpic = s;
				lastInCharge = "xxx";
				result
				.append(closeElemInCharge)
				.append(closeElemEpic)
				.append("\t<p>")
				.append(lastEpic)
				.append("</p>")
				.append(System.lineSeparator())
				.append("\t\t<ul>")
				.append(System.lineSeparator());
				closeElemInCharge = "";
				closeElemEpic = "\t\t</ul>" + System.lineSeparator();
			}
			s = map.get("incharge");
			if(lastInCharge.compareTo(s) != 0) {
				lastInCharge = s;
				result
				.append(closeElemInCharge)
				.append("\t\t\t<li>")
				.append(lastInCharge)
				.append("<ul>")
				.append(System.lineSeparator());
				closeElemInCharge = "\t\t\t</ul></li>" + System.lineSeparator();
			}
			result.append("\t\t\t\t<li>");
			s = map.get("issuekey");
			String closeElemA = "";
			if(s != null && s.length() > 0) {
				result.append("<a href=\"https://jira.ncsoft.com/browse/").append(s).append("\">");
				closeElemA = "</a>";
			}
			s = map.get("status");
			if(s != null && s.length() > 0) result.append("[").append(s).append("] ");
			s = map.get("summary");
			if(s != null && s.length() > 0) result.append(lastEpic).append(" - ").append(s);
			s = map.get("assignee");
			if(s != null && s.length() > 0) result.append(" (").append(s).append(")");
			result.append(closeElemA).append("</li>").append(System.lineSeparator());
		}
		result.append(closeElemInCharge).append(closeElemEpic).append(System.lineSeparator());

		result.append("<p>Right</p>").append(System.lineSeparator());
		lastEpic = "xxx";
		lastInCharge = "xxx";
		closeElemEpic = "";
		closeElemInCharge = "";
		for(Map<String, String> map : list) {
			String s = map.get("assignee");
			if(memberMap.get(s) == null) continue;
			s = map.get("status2");
			if(s.equals("01") || s.equals("02") || s.equals("04")) continue;
			
			s = map.get("customfield_10006"); // 에픽 링크
			if(s == null || s.length() == 0) s = "에픽 미지정";
			if(lastEpic.compareTo(s) != 0) {
				lastEpic = s;
				lastInCharge = "xxx";
				result
				.append(closeElemInCharge)
				.append(closeElemEpic)
				.append("\t<p>")
				.append(lastEpic)
				.append("</p>")
				.append(System.lineSeparator())
				.append("\t\t<ul>")
				.append(System.lineSeparator());
				closeElemInCharge = "";
				closeElemEpic = "\t\t</ul>" + System.lineSeparator();
			}
			s = map.get("incharge");
			if(lastInCharge.compareTo(s) != 0) {
				lastInCharge = s;
				result
				.append(closeElemInCharge)
				.append("\t\t\t<li>")
				.append(lastInCharge)
				.append("<ul>")
				.append(System.lineSeparator());
				closeElemInCharge = "\t\t\t</ul></li>" + System.lineSeparator();
			}
			result.append("\t\t\t\t<li>");
			s = map.get("issuekey");
			String closeElemA = "";
			if(s != null && s.length() > 0) {
				result.append("<a href=\"https://jira.ncsoft.com/browse/").append(s).append("\">");
				closeElemA = "</a>";
			}
			s = map.get("status");
			if(s != null && s.length() > 0) result.append("[").append(s).append("] ");
			s = map.get("summary");
			if(s != null && s.length() > 0) result.append(lastEpic).append(" - ").append(s);
			s = map.get("assignee");
			if(s != null && s.length() > 0) result.append(" (").append(s).append(")");
			result.append(closeElemA).append("</li>").append(System.lineSeparator());
		}
		result.append(closeElemInCharge).append(closeElemEpic).append(System.lineSeparator());
				
		if(print_weeklyReport)
		try {
			FileOutputStream out = new FileOutputStream(new File(fileNameWithoutExt + ".WeeklyReport.txt"));
			out.write(result.toString().getBytes());
			if(out != null) out.close();
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		    return;
		}
		
		//--------------------------------------------------------------------------------[Release]
		list.sort(new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				String s1 = o1.get("components");
				String s2 = o2.get("components");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}

				s1 = o1.get("status2");
				s2 = o2.get("status2");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return -a; // reverse
				}

				s1 = o1.get("customfield_14831"); // YETI: Live 배포 일시
				s2 = o2.get("customfield_14831");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}					

				s1 = o1.get("customfield_14830"); // YETI: RC 배포 일시
				s2 = o2.get("customfield_14830");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}

				s1 = o1.get("issuekey");
				s2 = o2.get("issuekey");
				if(s1 == null && s2 != null) return -1;
				if(s1 != null && s2 == null) return 1;
				if(s1 != null && s2 != null) {
					int a = s1.compareTo(s2);
					if(a != 0) return a;
				}				
				return 0;
			}
		});

		result = new StringBuilder();
		String lastCompo = "xxx";
		for(Map<String, String> map : list) {
			String s = map.get("issuetype");
			if(!s.contentEquals("YETI: 배포")) continue;
			String desc = map.get("description");
			int i = desc.indexOf("||일시||Type||ID||작업자||내용||비고||");
			if(i < 0) continue;
			desc = desc.substring(i + "||일시||Type||ID||작업자||내용||비고||".length());
			s = map.get("components");
			if(s == null || s.length() == 0) s = "구성요소 미지정";
			if(lastCompo.compareTo(s) != 0) {
				lastCompo = s;
				result
				.append(System.lineSeparator())
				.append(lastCompo)
				.append(System.lineSeparator())
				.append("||이슈키||구성 요소||구성 요소 속성||RC배포일||Live배포일||일시||Type||ID||작업자||내용||비고||")
				.append(System.lineSeparator());
			}
			String prefix = "|";
			s = map.get("issuekey");
			if(s == null || s.length() == 0) prefix += " |";
			else prefix += s + "|";
			s = map.get("components");
			if(s == null || s.length() == 0) prefix += " |";
			else prefix += s.replaceAll(",", System.lineSeparator()) + "|";
			s = map.get("customfield_14406");
			if(s == null || s.length() == 0) prefix += " |";
			else prefix += s.replaceAll(",", System.lineSeparator()) + "|";
			s = map.get("customfield_14830");
			if(s == null || s.length() == 0) prefix += " |";
			else prefix += s.replaceAll("\\s+", System.lineSeparator()) + "|";
			s = map.get("customfield_14831");
			if(s == null || s.length() == 0) prefix += " |";
			else prefix += s.replaceAll("\\s+", System.lineSeparator()) + "|";
			Pattern p3 = Pattern.compile("\\|([\\s\\S]*?)\\|([\\s\\S]*?)\\|([\\s\\S]*?)\\|([\\s\\S]*?)\\|([\\s\\S]*?)\\|([\\s\\S]*?)\\|");
			Matcher m3 = p3.matcher(desc.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("<br\\/>", System.lineSeparator()));
			while(m3.find()) {
				if(m3.groupCount() != 6) continue;
				result.append(prefix).append(m3.group(1)).append("|").append(m3.group(2)).append("|").append(m3.group(3)).append("|")
				.append(m3.group(4)).append("|").append(m3.group(5)).append("|").append(m3.group(6)).append("|").append(System.lineSeparator());				
			}
		}
		
		if(print_release)
		try {
			FileOutputStream out = new FileOutputStream(new File(fileNameWithoutExt + ".Release.txt"));
			out.write(result.toString().getBytes());
			if(out != null) out.close();
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null, e.getMessage());
		    return;
		}		
	}
}
