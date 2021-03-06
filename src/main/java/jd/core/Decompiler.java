package jd.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jd.ide.intellij.JavaDecompiler;

public class Decompiler {
	
	private static final JavaDecompiler decompiler = new JavaDecompiler();
	
	public Decompiler(){
		
	}
	
	public String decompile(String jarPath, String internalClassName) throws DecompilerException {
		String src = decompiler.decompile(jarPath, internalClassName);
		
		if (src == null){
			throw new DecompilerException("cannot decompile " + jarPath + "!" + internalClassName);
		}
		
		StringBuilder result = new StringBuilder(src.length());
		
		for (String string : src.split("\n")){
			for (int i = 0; true; i += 2){
				String temp = string.substring(i);
				
				if (temp.startsWith("  ")){
					result.append('\t');
				}else{
					result.append(temp);
					break;
				}
			}
			
			result.append('\n');
		}
		
		src = result.toString();
		
		src = src.replaceAll("\\n(\\s*)\\{", " {$1");
		src = src.replaceAll("\\n\\t(extends|implements)", " $1");
		src = src.replaceAll("(class|interface)( .*\\{)", "$1$2\n");
		
		return src;
	}
	
	public void decompileToDir(String jarPath, String outDir) throws IOException {
		ZipInputStream zip = new ZipInputStream(new FileInputStream(jarPath));
		ZipEntry entry = null;
		
		while ((entry = zip.getNextEntry()) != null){
			String entryName = entry.getName();
			
			if (!entry.isDirectory()){
				if (entryName.endsWith(".class")){
					String classPath = entryName.replaceAll("\\$.*\\.class$", ".class");
					String javaPath = classPath.replaceAll("\\.class$", ".java");
					
					File outFile = new File(outDir, javaPath);
					outFile.getParentFile().mkdirs();
					
					FileOutputStream output = new FileOutputStream(outFile);
					
					try{
						output.write(this.decompile(jarPath, classPath).getBytes());
					}catch (DecompilerException e){
						System.err.println("Failed to decompile " + classPath);
					}
					
					output.close();
				}else{
					File outFile = new File(outDir, entryName);
					outFile.getParentFile().mkdirs();
					
					if (!outFile.exists()){
						FileOutputStream output = new FileOutputStream(outFile);
						
						byte[] buffer = new byte[4096];
						int len;
						
						while ((len = zip.read(buffer)) != -1){
							output.write(buffer, 0, len);
						}
						
						output.close();
					}
				}
			}
		}
		
		zip.close();
	}
	
}
