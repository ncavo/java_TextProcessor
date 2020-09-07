import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class TextFileAutoEditor {

	public static Scanner openScanner(String fileName) {

		FileInputStream fis = null;		
		try {
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			System.out.println("file not found error(" + getLineNumber() + ")");
			return null;
		} catch (Exception e2) {
			System.out.println("file open error(" + getLineNumber() + ")");
			return null;
		}

		byte[] fileSignature = new byte[4];
		try {
			fis.read(fileSignature);
		} catch (IOException e1) {
			System.out.println("file read error(" + getLineNumber() + ")");
			return null;
		} finally {
			try {
				fis.close();
			} catch (IOException e) {}			
		}
		
		try {
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			System.out.println("file not found error(" + getLineNumber() + ")");
			return null;
		} catch (Exception e2) {
			System.out.println("file open error(" + getLineNumber() + ")");
			return null;
		}
		
		try {
			if(fileSignature[0] == (byte) 0xFF && fileSignature[1] == (byte) 0xFE && fileSignature[2] == 0x00 && fileSignature[3] == 0x00) {
				fis.skip(4);
				return new Scanner(new InputStreamReader(fis, "UTF-32LE"));
			}
			else if(fileSignature[0] == (byte) 0xFE && fileSignature[1] == (byte) 0xFF && fileSignature[2] == 0x00 && fileSignature[3] == 0x00) {
				fis.skip(4);
				return new Scanner(new InputStreamReader(fis, "UTF-32BE"));
			}
			else if(fileSignature[0] == (byte) 0xFF && fileSignature[1] == (byte) 0xFE) {
				fis.skip(2);
				return new Scanner(new InputStreamReader(fis, "UTF-16LE"));
			}
			else if(fileSignature[0] == (byte) 0xFE && fileSignature[1] == (byte) 0xFF) {
				fis.skip(2);
				return new Scanner(new InputStreamReader(fis, "UTF-16BE"));
			}
			else {
				return new Scanner(new InputStreamReader(fis, "UTF-8"));			
			}
			
		} catch (Exception e1) {
			System.out.println("file read error(" + getLineNumber() + ")");
			try {
				fis.close();
			} catch (IOException e) {}			
			return null;
		}		
	}
	
	public static void main(String[] args) {

		if(args.length == 1 && args[0].equalsIgnoreCase("commands")) {
			System.out.println("commands: p4copy                           p4 paths to copy commands for backup");
			System.out.println("          separate");
			System.out.println("          custom                           custom edit for temporary work");
			return;
		}
		
		if(args.length < 2) {
			System.out.println("usage:   TextFileAutoEditor {command} {input file} [output file] [options]");
			System.out.println();
			System.out.println("         TextFileAutoEditor commands       list all commands");
			System.out.println();
			System.out.println("options: /l {number}                       limit lines to process");
			return;
		}

		int cmd = 0;
		boolean useDefaultOutputStream = true;
		if(args[0].equalsIgnoreCase("p4copy")) {
			cmd = 1;
		}
		else if(args[0].equalsIgnoreCase("separate")) {
			cmd = 2;
			useDefaultOutputStream = false;
		}
		else if(args[0].equalsIgnoreCase("custom")) {
			cmd = 10000;
		}
		else {
			System.out.println("unknown command error(" + getLineNumber() + ")");
			return;			
		}
		
		boolean hasOutputFileName = false;
		int limitLineCount = 0;

		for(int i = 2; i < args.length; i++) {
			
			if(i == 2 && !args[i].startsWith("/")) {
				hasOutputFileName = true;
				continue;
			}
			
			if(!args[i].startsWith("/")) {
				System.out.println("option parameter error(" + getLineNumber() + ")");
				return;
			}
			
			if(args[i].equalsIgnoreCase("/l")) {
				if(++i == args.length) {
					System.out.println("option parameter error(" + getLineNumber() + ")");
					return;					
				}

				try {
					limitLineCount = Integer.parseInt(args[i]);
				}
				catch (NumberFormatException e) {
					System.out.println("option parameter error(" + getLineNumber() + ")");
					return;										
				}
			}
			else {
				System.out.println("option parameter error(" + getLineNumber() + ")");
				return;				
			}
		}
		
		Scanner scanner = openScanner(args[1]);
		if(scanner == null) return;

		OutputStreamWriter outputStream = null;
		if(useDefaultOutputStream) {
			try {
				outputStream = new OutputStreamWriter(new FileOutputStream(hasOutputFileName ? args[2] : args[1] + ".out"));
			} catch (FileNotFoundException e1) {
				System.out.println("output file not found error(" + getLineNumber() + ")");
				scanner.close();
				return;
			}
		}
		
		// for 2
		int lineCounter = 0;
		
		// for 10000
		Map<String, Integer> map = null;	
		
		switch(cmd) {
		case 10000:
			{
				map = new TreeMap<String, Integer>(); 
			}
			break;
		}		
		
		int lineNum = 0;
		boolean errorBreak = false;
		while(scanner.hasNextLine()) {
			lineNum++;
			if(limitLineCount > 0 && lineNum > limitLineCount) break;
			System.out.print("\r" + lineNum + " line processing");
			String line = scanner.nextLine();
			boolean writeLine = true;

			switch(cmd) {
			case 1:
				{
					String m = "//depot/Platform/NCPlatform/Portal/Main/Code/";
					String b = "//depot/Platform/NCPlatform/Portal/Bench/Code/";
					String s = "//depot/Platform/NCPlatform/Portal/Stage/Code/";
					String l = "//depot/Platform/NCPlatform/Portal/Live/Code/";
					
					if(line.startsWith(m)) {
						int index = line.indexOf("#");
						if(index < 0) index = line.length();
						line = line.substring(m.length(), index).replaceAll("/", "\\\\");
						line = "xcopy " + "C:\\Perforce\\ncavo-05\\NP\\M\\Portal\\" + line + " " 
						                + "C:\\Perforce\\ncavo-05\\NP\\M.bak\\Portal\\" + line.substring(0, line.lastIndexOf("\\")) + " /aiy";
					}
					else if(line.startsWith(b)) {
						int index = line.indexOf("#");
						if(index < 0) index = line.length();
						line = line.substring(b.length(), index).replaceAll("/", "\\\\");
						line = "xcopy " + "C:\\Perforce\\ncavo-05\\NP\\B\\Portal\\" + line + " " 
						                + "C:\\Perforce\\ncavo-05\\NP\\B.bak\\Portal\\" + line.substring(0, line.lastIndexOf("\\")) + " /aiy";
					}
					else if(line.startsWith(s)) {
						int index = line.indexOf("#");
						if(index < 0) index = line.length();
						line = line.substring(s.length(), index).replaceAll("/", "\\\\");
						line = "xcopy " + "C:\\Perforce\\ncavo-05\\NP\\S\\Portal\\" + line + " " 
						                + "C:\\Perforce\\ncavo-05\\NP\\S.bak\\Portal\\" + line.substring(0, line.lastIndexOf("\\")) + " /aiy";
					}
					else if(line.startsWith(l)) {
						int index = line.indexOf("#");
						if(index < 0) index = line.length();
						line = line.substring(l.length(), index).replaceAll("/", "\\\\");
						line = "xcopy " + "C:\\Perforce\\ncavo-05\\NP\\L\\Portal\\" + line + " " 
						                + "C:\\Perforce\\ncavo-05\\NP\\L.bak\\Portal\\" + line.substring(0, line.lastIndexOf("\\")) + " /aiy";
					}
				}
				break;
			case 2:
				{
					if(outputStream != null && lineCounter >= 100000) {
						try {
							outputStream.flush();
							outputStream.close();
						} catch (IOException e) {}
						
						outputStream = null;
						
						lineCounter = 0;
					}

					if(outputStream == null) {
						String newFileName = hasOutputFileName ? args[2] : args[1] + ".out";
						for(int i = 0;; i++) {
							int a = newFileName.lastIndexOf(".");
							File newFile = new File(String.format("%s_%04d%s", newFileName.substring(0, a), i, newFileName.substring(a)));
							if(newFile.exists()) {
								newFile = null;
								continue;
							}
							try {
								outputStream = new OutputStreamWriter(new FileOutputStream(newFile));
							} catch (FileNotFoundException e1) {
								System.out.println("output file not found error(" + getLineNumber() + ")");
								scanner.close();
								return;
							}
							break;
						}
					}
					
					lineCounter++;
					
					useDefaultOutputStream = true;
				}
				break;
			case 10000:
				{
					String token[] = line.split("\t");
					line = "INSERT INTO @TT1 (onlineItemDeliveryNo) VALUES (" + token[8] + ")";
				}
				break;
			}
			
			if(errorBreak) break;
			
			if(useDefaultOutputStream && writeLine) {
				try {
					outputStream.write(line + System.lineSeparator());
				} catch (IOException e) {
					break;
				}
			}
		}

		// post job
		switch(cmd) {
		case 10000:
			break;
		}

		System.out.println();
		if(!errorBreak) System.out.println("done");
		
		if(useDefaultOutputStream) {
			try {
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {}
		}
		
		scanner.close();		
	}
	
	public static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }		
}
