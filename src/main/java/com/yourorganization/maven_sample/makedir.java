package com.yourorganization.maven_sample;

import java.io.File;
//遍历原来路径，找到File
//通过阅读csv文件，获取到包的名称
//替换包的名称把.换成/
//进行文件的移动
//
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class makedir {
	
	public static boolean mkdir() {
		File newFile=new File("E:/a/b/c/d.txt");
        try {
         if(!(newFile.isFile())){
        	 System.out.println("不适合创建呀");
        	 return false;
         }
         else{
        	 System.out.println("不存在可以创建");
        	 newFile.mkdirs();  
         }
               
           
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return true;
    }
	

	
	
	public static void moveFile(){
        String oldPath="D:/c/d/e.txt";
        String toPath="D:/a";

        File file = new File(oldPath);
            if (file.isFile()){  
                File toFile=new File(toPath+"/"+file.getName());  
                if (toFile.exists()){  
                   System.out.println("文件已存在");
                }
                else{
                    file.renameTo(toFile); 
                    System.out.println("移动文件成功");
                } 
            }         
        }
public static void testSet(){
	Set<Integer> test = new TreeSet<>();
	test.add(3);
	test.add(4);
	test.add(3);
	test.add(7);
	test.add(1);
	test.add(3);
	for(Integer s:test){
		System.out.println(s+" ");
	}
}

	public static void main(String args[]){
        testSet();
	}
}
