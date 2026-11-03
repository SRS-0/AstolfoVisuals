import java.nio.file.*;
import java.net.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class DumpApi {
 public static void main(String[] args)throws Exception {
  var urls=new ArrayList<URL>();for(var item:Files.readString(Path.of(args[0])).split(java.io.File.pathSeparator))urls.add(Path.of(item).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),null)) {
   for(String name:Files.readAllLines(Path.of(args[1])))try(var input=loader.getResourceAsStream(name.replace('.','/')+".class")) {
    System.out.println("CLASS\t"+name);if(input==null){System.out.println("MISSING");continue;}
    var node=new ClassNode();new ClassReader(input).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_FRAMES);
    for(var method:node.methods)System.out.println("METHOD\t"+method.name+"\t"+method.desc);
    for(var field:node.fields)System.out.println("FIELD\t"+field.name+"\t"+field.desc);
   }
  }
 }
}
