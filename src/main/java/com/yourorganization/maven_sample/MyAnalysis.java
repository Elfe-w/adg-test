package com.yourorganization.maven_sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import javassist.bytecode.MethodInfo;

//搬运引用
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.symbolsolver.javaparser.Navigator.demandParentNode;
import static java.util.Comparator.comparing;



/**
 * Some code that uses JavaSymbolSolver.
 */
public class MyAnalysis {
	public static Logger logger=Logger.getLogger("MyAnalysis.class");
	private static ArrayList<String> methodInfo=new ArrayList<>();
	private static PrintStream out = System.out;
	private static boolean isMatch=false;
	private static StringBuilder stringBuild;
	private static Map<Integer,String> methodFile;
	private static Map<Integer,String> methodFileReturnType;
	private static Map<Integer,String> methodCallInfo;
	private static Map<Integer,Integer> onID;//建立新旧ID的关联
	private static Integer newID=0;
	private static Map<Integer,ArrayList<Integer>> edgePerInfo;//用来匹配ID来构建图,记录前向节点
	private static Map<Integer,ArrayList<Integer>> edgeNextInfo;//用来匹配ID来构建图,记录后向节点
	private static Map<Integer,String> tagInfo;
	private static Map<Integer,String> nMethodInfo;
	
	//用一个related的map记录下所有出现的ID，key等于原来的ID然后，value等于新的ID根据新的ID序号进行编码
	/*
	 * 所以所有的准备工作都是为了最后建立一个图
	 * 图中节点用one-hot编码
	 * 图中边就是通过辨识对应的节点进行建立的
	 * 辨识通过获取到的csv文件的给出的ID
	 * 
	 * 
	 * 建立图关系的时候，添加新的ID需要关系到的是有边建立的时候，所以为newID赋值的时候就是需要在建立边的时候进行赋值
	 * 
	 * 每一次建立边的时候都是要遍历整个csv文件，也就是意味着不在newID顺序生成
	 * 所以新旧ID必须通过一个Map进行关联起来,获取newID所以oldID是key
	 * 
	 * ID不同的存储在一个集合里面了
	 * 建立边的节点关系放在一个map里面的，只需要遍历一个map就可以建立起来两个图
	 * 通过
	 */
	//nMethodInfo中包含的是内容是，构成调用图的节点，（文件中包含的所有签名函数的节点，methodCallInfo中包含的是所有关于函数调用的节点），
	//并且 ，新的里面是赋予的从0开始的新的ID值
	
	
	
	public static void storeNewMethodInfo(){
    	nMethodInfo=new HashMap<Integer,String>();
    	for(Map.Entry<Integer,Integer>entry:onID.entrySet()){
    		nMethodInfo.put(entry.getValue(), methodFile.get(entry.getKey()));
    	}
	}
	
	public static void createIDFile(String filePath,String fileName) throws IOException {		
	    File dir = new File(filePath);
	    // 一、检查放置文件的文件夹路径是否存在，不存在则创建
	    if (!dir.exists()) {
	        dir.mkdirs();// mkdirs创建多级目录
	    }
	    File checkFile = new File(filePath + fileName);
	    FileWriter writer = null;
	    try {
	        // 二、检查目标文件是否存在，不存在则创建
	        if (!checkFile.exists()) {
	            checkFile.createNewFile();// 创建目标文件
	        }
	        // 三、向目标文件中写入内容
	        // FileWriter(File file, boolean append)，append为true时为追加模式，false或缺省则为覆盖模式
	        writer = new FileWriter(checkFile, false);
	       for(Map.Entry<Integer, Integer>entry:onID.entrySet()){
		       writer.append(entry.getKey()+":"+"\n"+entry.getValue());
//		       if(methodCallInfo.get(entry.getKey()).equals(nMethodInfo.get(entry.getValue()))){
//		    	   writer.append("YES\n");
//		       }
//		       else{
//		    	   writer.append("NO\n");
//		       }
	       }
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (null != writer)
	            writer.close();
	    }
	}

	public static void createNewEdgeInfoFile(String filePath,String fileName) throws IOException {		
	    File dir = new File(filePath);
	    // 一、检查放置文件的文件夹路径是否存在，不存在则创建
	    if (!dir.exists()) {
	        dir.mkdirs();// mkdirs创建多级目录
	    }
	    File checkFile = new File(filePath + fileName);
	    FileWriter writer = null;
	    try {
	        // 二、检查目标文件是否存在，不存在则创建
	        if (!checkFile.exists()) {
	            checkFile.createNewFile();// 创建目标文件
	        }
	        // 三、向目标文件中写入内容
	        // FileWriter(File file, boolean append)，append为true时为追加模式，false或缺省则为覆盖模式
	        writer = new FileWriter(checkFile, false);
	        
	        
	        
	       for(Map.Entry<Integer, ArrayList<Integer>>entry:edgePerInfo.entrySet()){
		      for(Integer perid:entry.getValue()){
		    	  writer.append(perid+"->"+entry.getKey()+":"+tagInfo.get(perid)+"\n");
		      }
	       }
	       
	       
	       
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (null != writer)
	            writer.close();
	    }
	}

	public static void createNewMethodInfoFile(String filePath,String fileName) throws IOException {		
	    File dir = new File(filePath);
	    // 一、检查放置文件的文件夹路径是否存在，不存在则创建
	    if (!dir.exists()) {
	        dir.mkdirs();// mkdirs创建多级目录
	    }
	    File checkFile = new File(filePath + fileName);
	    FileWriter writer = null;
	    try {
	        // 二、检查目标文件是否存在，不存在则创建
	        if (!checkFile.exists()) {
	            checkFile.createNewFile();// 创建目标文件
	        }
	        // 三、向目标文件中写入内容
	        // FileWriter(File file, boolean append)，append为true时为追加模式，false或缺省则为覆盖模式
	        writer = new FileWriter(checkFile, false);
	        
	        
	        
	       for(Entry<Integer, String> entry:nMethodInfo.entrySet()){
		      writer.append(entry.getKey()+":"+entry.getValue()+"\n");
	       }
	       
	       
	       
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (null != writer)
	            writer.close();
	    }
	}
	
	
	public static void createOneHotFile(String filePath,String fileName) throws IOException {

	    File dir = new File(filePath);
	    // 一、检查放置文件的文件夹路径是否存在，不存在则创建
	    if (!dir.exists()) {
	        dir.mkdirs();// mkdirs创建多级目录
	    }
	    File checkFile = new File(filePath + "onehotTest.txt");
	    FileWriter writer = null;
	    try {
	        // 二、检查目标文件是否存在，不存在则创建
	        if (!checkFile.exists()) {
	            checkFile.createNewFile();// 创建目标文件
	        }
	        // 三、向目标文件中写入内容
	        // FileWriter(File file, boolean append)，append为true时为追加模式，false或缺省则为覆盖模式
	        writer = new FileWriter(checkFile, false);
	       for(Map.Entry<Integer, Integer>entry:onID.entrySet()){
	    	   int[] arr=new int[onID.size()];
	    	   arr[entry.getValue()]=1;
		       writer.append(entry.getValue()+","+Arrays.toString(arr)+"\n");
	       }
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (null != writer)
	            writer.close();
	    }
	}
	

	public static void readFile(){
		
		File csv=new File("E:/test/SignalResultsTest.csv");
		BufferedReader br=null;
		try{
			br=new BufferedReader(new FileReader(csv));
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}

		String line="";
		int lineNum=0;
		try{
			line=br.readLine();//忽略第一行
			while((line=br.readLine())!=null){
				lineNum++;
				stringBuild=new StringBuilder();
				String readMethod[]=line.split(",");//用来过滤掉含有的ID，类名之类的信息，这步本来省掉的，但是为了避免之后的信息用到先这样写
				for(int i=0;i<readMethod.length;i++){
					if(i!=0&&i!=3&&i!=5){
						stringBuild.append(readMethod[i]+",");
					}
				}
				if(readMethod.length==0){
					//pass
					System.out.println(lineNum);
				}
				else{
					methodFile.put(Integer.parseInt(readMethod[3]),((stringBuild+"").substring(0, (stringBuild+"").length()-1)).replaceFirst(",", "\\.").replaceFirst(",", "\\."));
					if(readMethod.length>5){
						methodFileReturnType.put(Integer.parseInt(readMethod[3]), readMethod[5]);
					}
				}
				
			}
			
		}catch(IOException e){
			
		}
	}

	//匹配出方法的ID
    public static void getMethodID(){
    	methodCallInfo=new HashMap<Integer,String>();
    	for(String s:methodInfo){
    		for(Integer key : methodFile.keySet()){
         	   String value = methodFile.get(key); 
         	   if (s.equals(value)){ 
         		   methodCallInfo.put(key,s);
         		   break;
         	   }
         }
    	}    
    } 
	
    //确定边的关系，把每一个形参的类型重新分离出来，如果是基本数据类型或者是String类型则不用去遍历能够提供的函数
    public static void getEdgeInfo(){
    	edgePerInfo=new HashMap<Integer,ArrayList<Integer>>();
    	edgeNextInfo=new HashMap<Integer,ArrayList<Integer>>();
    	tagInfo=new HashMap<Integer,String>();
    	onID=new HashMap<Integer,Integer>();
    	String tagSmall="";
    	
    	
    	for(Map.Entry<Integer, String> entry:methodCallInfo.entrySet()){//对每一个函数调用进行遍历
    		String methodInfoGet=entry.getValue();
    		String parList[]=methodInfoGet.split(",");
    		ArrayList<Integer> perID=new ArrayList<>();
    		
    		//给newID
    		if(onID.containsKey(entry.getKey())){
    			//pass
    		}
    		else{
    			//如果没有存在就给他一个新的ID
    			onID.put(entry.getKey(), newID);
    			//并且把他存到新的map容器中
    			newID++;
    		}
    		
    		//通过调用的类名构建边
    		String classNameList[]=methodInfoGet.split("\\.");
    		
    		String className=classNameList[classNameList.length-2];    		
    		for(Map.Entry<Integer, String> entryTwo:methodFileReturnType.entrySet()){//查找文件中的每一个返回值
				String fileInfoGet=entryTwo.getValue();
	    			if(fileInfoGet.equals(className)){//如果返回值和形参类型相同就保存下他的ID，并将标志位置为真
		    			isMatch=true;
		    			
		    			
		    			if(onID.containsKey(entryTwo.getKey())){
		        			//pass
		        		}
		        		else{
		        			//如果没有存在就给他一个新的ID
		        			onID.put(entryTwo.getKey(), newID);
		        			//并且把他存到新的map容器中
		        			newID++;
		        		}	
		    			
		    			
		    			tagSmall=className;
		    			perID.add(onID.get(entryTwo.getKey()));   
		    			tagInfo.put(onID.get(entryTwo.getKey()),className);
		    		}	    		
	    		
			}
    		
    		//通过参数构建边
    		//分离出来第一个是返回值类型，不需要考虑,获取下标的时候需要考虑是否存在这个参数，否则会出现数组越界的问题
    		if(parList.length<=1){
    			
    		}
    		else{
    			for(int i=1;i<parList.length;i++){//对每一个参数进行遍历
        			if(parList[i].equals("String")
        					||parList[i].equals("int")
        					||parList[i].equals("byte")
        					||parList[i].equals("short")
        					||parList[i].equals("int")
        					||parList[i].equals("long")
        					||parList[i].equals("float")
        					||parList[i].equals("double")
        					||parList[i].equals("boolean")
        					||parList[i].equals("char")){

        			}
        			else{
        				for(Map.Entry<Integer, String> entryTwo:methodFileReturnType.entrySet()){//查找文件中的每一个返回值
        					String fileInfoGet=entryTwo.getValue();
        		    			if(fileInfoGet.equals(parList[i])){//如果返回值和形参类型相同就保存下他的ID，并将标志位置为真
        		    				
        		    				
        		    				
        		    				if(onID.containsKey(entryTwo.getKey())){
        			        			//pass
        			        		}
        			        		else{
        			        			//如果没有存在就给他一个新的ID
        			        			onID.put(entryTwo.getKey(), newID);
        			        			newID++;
        			        		}	
        			    			
        		    				
        		    				
        		    				
        		    				isMatch=true;
            		    			tagSmall=parList[i];
            		    			perID.add(onID.get(entryTwo.getKey()));   
            		    			tagInfo.put(onID.get(entryTwo.getKey()),parList[i]);
            		    		}
        		    		
        				}
        			}
        		}
    			
    		}

    		
    		if(isMatch)
    		{
    			edgePerInfo.put(onID.get(entry.getKey()), perID);
    			for(Integer perid:perID){
    				ArrayList<Integer> temp=new ArrayList<>();
    				if(edgeNextInfo.containsKey(perid)){    					   					
    					edgeNextInfo.get(perid).add(onID.get(entry.getKey()));
    				}
    				else{  					
    					temp.add(onID.get(entry.getKey()));
    					edgeNextInfo.put(perid, temp);
    				}
    			}
    			isMatch=false;
    		}   
        }
    }
  

    public static void main(String[] args) throws IOException {
    	PrintWriter printWriter;
    	printWriter = new PrintWriter(new File("E:/test/MethodCall.csv"));
    	
    	Path src=Paths.get("E:/dataSrcTest");
    	//Path src=Paths.get("E:/00-data/00-javadataset/java-small/java-small/test/hadoop");
    	//Path src=Paths.get("E:/dataSet/Java-master");
    	//Path src=Paths.get("E:/JavaParser-Tool-master");
    	Path jar_src=Paths.get("E:/src_temp");
 //   	Path src=Paths.get("E:/javaparser-master");
//    	Path jar1=Paths.get("E:/dataSet/JAVADataset-master/JAVADataset-master/lib/JNIPort30.jar");
//    	Path jar2=Paths.get("E:/dataSet/JAVADataset-master/JAVADataset-master/lib/ch/akuhn/fame/1.1/fame-1.1.jar");
//    	Path jar3=Paths.get("E:/dataSet/JAVADataset-master/JAVADataset-master/lib/ch/akuhn/fame/1.0/fame-1.0.jar");
    	methodFile=new HashMap<Integer, String>();
    	methodFileReturnType=new HashMap<Integer, String>();
    	
    	CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    	
    	combinedTypeSolver.add(new ReflectionTypeSolver());
        
        
        Files.walkFileTree(jar_src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path jar, BasicFileAttributes attrs) throws IOException {
                if (jar.toString().endsWith(".jar")) {
                	combinedTypeSolver.add(new JarTypeSolver(jar));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        
        //combinedTypeSolver.add(new JarTypeSolver(src1));
       // combinedTypeSolver.add(new JavaParserTypeSolver(src1));
        //combinedTypeSolver.add(new JavaParserTypeSolver(new File("/home/federico/repos/javaparser/javaparser-core/target/generated-sources/javacc")));
        SourceFileInfoExtractor sourceFileInfoExtractor = new SourceFileInfoExtractor(combinedTypeSolver);
        
        
        try {
        	methodInfo=sourceFileInfoExtractor.solveMethodCalls(src);  
        	//sourceFileInfoExtractor.solve(src1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        for(String s:methodInfo){
        	 printWriter.write(s+"\n");
        }            
        // We're done with printWriter!
        printWriter.close();
        logger.info("i saved.......");
        String filePath="E:/outputTest";
      readFile();
      getMethodID();
      getEdgeInfo();
      createIDFile(filePath,"IDTest.txt");
//      createOneHotFile();
      storeNewMethodInfo();
      createNewMethodInfoFile(filePath,"methodInfo.txt");
      createNewEdgeInfoFile(filePath,"edge.txt");

    
    }
}



class SourceFileInfoExtractor {

    private final TypeSolver typeSolver;
    
    private static PrintWriter printWriter;
	

    public static Logger logger=Logger.getLogger("SourceFileInfoExtractor.class");
    private int successes = 0;
    private int failures = 0;
    private float successesCall = 0;
    private float failuresCall = 0;
    private int unsupported = 0;
    private boolean printFileName = true;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private boolean verbose = false;
    
    private StringBuilder stringBuilder;
    private ArrayList<String> methodInfo=new ArrayList<>();

    public SourceFileInfoExtractor(TypeSolver typeSolver) {
        this.typeSolver = typeSolver;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setPrintFileName(boolean printFileName) {
        this.printFileName = printFileName;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    public int getSuccesses() {
        return successes;
    }

    public int getUnsupported() {
        return unsupported;
    }

    public int getFailures() {
        return failures;
    }

    private void solveTypeDecl(ClassOrInterfaceDeclaration node) {
        ResolvedTypeDeclaration typeDeclaration = JavaParserFacade.get(typeSolver).getTypeDeclaration(node);
        if (typeDeclaration.isClass()) {
            out.println("\n[ Class " + typeDeclaration.getQualifiedName() + " ]");
            for (ResolvedReferenceType sc : typeDeclaration.asClass().getAllSuperClasses()) {
                out.println("  superclass: " + sc.getQualifiedName());
            }
            for (ResolvedReferenceType sc : typeDeclaration.asClass().getAllInterfaces()) {
                out.println("  interface: " + sc.getQualifiedName());
            }
        }
    }

    private void solve(Node node) {
//    	System.out.println("-----"+String.valueOf(node)+"-----");
//    	System.out.println("\n");
        if (node instanceof ClassOrInterfaceDeclaration) {
            //solveTypeDecl((ClassOrInterfaceDeclaration) node);
        } else if (node instanceof Expression) {
            Node parentNode = demandParentNode(node);
            if (parentNode instanceof ImportDeclaration ||
                    parentNode instanceof Expression ||
                    parentNode instanceof MethodDeclaration ||
                    parentNode instanceof PackageDeclaration) {
                // skip
                return;
            }
            if (parentNode instanceof Statement ||
                    parentNode instanceof VariableDeclarator ||
                    parentNode instanceof SwitchEntry) {
                try {
                    ResolvedType ref = JavaParserFacade.get(typeSolver).getType(node);
                   //out.println("  Line " + lineNr(node) + ") " + node + " ==> " + ref.describe());
                    successes++;
                } catch (UnsupportedOperationException upe) {
                    unsupported++;
                    err.println(upe.getMessage());
                    throw upe;
                } catch (RuntimeException re) {
                    failures++;
                    err.println(re.getMessage());
                    throw re;
                }
            }
        }
    }

    private void solveMethodCalls(Node node) {
        if (node instanceof MethodCallExpr) {
        	stringBuilder=new StringBuilder();
            String methodhalfSignature=toString((MethodCallExpr) node);
            
            //System.out.println(methodhalfSignature);
            if(methodhalfSignature.contains("(")&&!methodhalfSignature.contains("java.lang")&&!methodhalfSignature.contains("java.util")&&!methodhalfSignature.contains("java.io")){
            	successesCall++;
            	String[] aa = methodhalfSignature.split("\\(");
                String methodName=aa[0];
                String par=aa[1];          
                par=par.replaceAll("\\)","");
                String[] parameters = par.split(",");
               for(int i=0;i<parameters.length;i++){
            	   String parameterString="";
            	   if(parameters[i].split("\\.").length>0)
            		   parameterString=parameters[i].split("\\.")[parameters[i].split("\\.").length-1];
            	   stringBuilder.append(parameterString);
               }
        	   if("".equals(stringBuilder.toString())){
        		   methodInfo.add(methodName);
        	   }else
        	   {
        		   String parcon=stringBuilder+"";//形参用空格隔开的所以，将他换成逗号隔开的    		 
        		   parcon=parcon.replaceAll("\\s", ",");
        		   methodInfo.add(methodName+","+parcon);
        		   
        		   
        	   } 
        	   
            }   
            else{
            	printWriter.write(methodhalfSignature+"\n");
            	failuresCall++;
            }           
        }        
        for (Node child : node.getChildNodes()) {
            solveMethodCalls(child);
        }
    }

    private String toString(MethodCallExpr node) {
        try {        	
            return toString(JavaParserFacade.get(typeSolver).solve(node));
        } catch (Exception e) {
        	 printWriter.write(node.getName()+","+String.valueOf(node));
            if (verbose) {           	
                System.err.println("Error resolving call at L" + lineNr(node) + ": " + node);
                e.printStackTrace();
            }
            return "ERROR";
        }
    }

    private String toString(SymbolReference<ResolvedMethodDeclaration> methodDeclarationSymbolReference) {
        if (methodDeclarationSymbolReference.isSolved()) {
            return methodDeclarationSymbolReference.getCorrespondingDeclaration().getQualifiedSignature();
        } else {
            return "UNSOLVED";
        }
    }

    private List<Node> collectAllNodes(Node node) {
        List<Node> nodes = new ArrayList<>();
        node.walk(nodes::add);
        logger.info("print ast......");
        for(Node n:nodes)
        {
        	//System.out.println("=================I am: ======================\n"+String.valueOf(n)+"\n=================my parent is:======================\n "+"   "+String.valueOf(n.getParentNode()));
        	
        	System.out.println("\n..........................");
        	System.out.println("\nI am:"+String.valueOf(n)+"\n》》》》》"+n.getClass());
        	System.out.println("\n.....................");
        	System.out.println("\n===below===");
        	for(Node n1:n.getChildNodes()){
        		System.out.print("**"+String.valueOf(n1)+"**");
        	}
        	System.out.println("\n===up===");
        }              
        nodes.sort(comparing(n -> n.getBegin().get()));
        return nodes;
    }

    public void solve(Path path) throws IOException {
    	
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    if (printFileName) {
                        out.println("- parsing " + file.toAbsolutePath());
                    }
                    CompilationUnit cu = parse(file);
                    List<Node> nodes = collectAllNodes(cu);
                    nodes.forEach(n -> solve(n));
                }
                return FileVisitResult.CONTINUE;
            }
        });
       
    }

    public ArrayList<String> solveMethodCalls(Path path) throws IOException {
    	printWriter = new PrintWriter(new File("E:/test/FailMethodCall.csv"));
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    if (printFileName) {
                    	printWriter.write(file.toAbsolutePath()+"\n");
                        out.println("- parsing " + file.toAbsolutePath());
                    }
                    CompilationUnit cu = parse(file);
                    solveMethodCalls(cu);
                }               
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("MethodCallExpr total num:  "+(successesCall+failuresCall));
        System.out.println("The proportion of correct parsing method  is: "+(successesCall/(successesCall+failuresCall))*100+"%");
        printWriter.close();
        return methodInfo;
    }

    private int lineNr(Node node) {
        return node.getRange().map(range -> range.begin.line).orElseThrow(IllegalStateException::new);
    }
}
