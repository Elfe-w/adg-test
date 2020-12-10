package com.yourorganization.maven_sample;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
* Resolves resolvable nodes from one or more source files, and reports the results.
* It is mainly intended as an example usage of JavaSymbolSolver.
*
* @author Federico Tomassetti
* 
*/
public class depenceCopy{
	 	 
	 /**
	  * 创建文件
	  * @param fileName
	  * @return
	  */
	public static void createFile() throws IOException {
		int num=100;
		int[] arr=new int[100];
		arr[2]=7;
		
	    String filePath = "E:/output/";
	    File dir = new File(filePath);
	    // 一、检查放置文件的文件夹路径是否存在，不存在则创建
	    if (!dir.exists()) {
	        dir.mkdirs();// mkdirs创建多级目录
	    }
	    File checkFile = new File(filePath + "onehot.txt");
	    FileWriter writer = null;
	    try {
	        // 二、检查目标文件是否存在，不存在则创建
	        if (!checkFile.exists()) {
	            checkFile.createNewFile();// 创建目标文件
	        }
	        // 三、向目标文件中写入内容
	        // FileWriter(File file, boolean append)，append为true时为追加模式，false或缺省则为覆盖模式
	        writer = new FileWriter(checkFile, false);
	        writer.append(Arrays.toString(arr));
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (null != writer)
	            writer.close();
	    }
	}
	
	
	
	public static void main(String args[]){
		try {
			createFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	 
	
}
