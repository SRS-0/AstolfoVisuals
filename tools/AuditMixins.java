import java.nio.file.*;
import java.util.*;
import java.net.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks injection selectors against target bytecode without loading the game. */
public class AuditMixins {
    static URLClassLoader loader;
    static final Map<String,ClassNode> cache=new HashMap<>();
    static ClassNode read(String name) throws Exception {
        if(cache.containsKey(name))return cache.get(name);
        try(var stream=loader.getResourceAsStream(name+".class")) {
            if(stream==null)return null;
            ClassNode node=new ClassNode();new ClassReader(stream).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);cache.put(name,node);return node;
        }
    }
    static List<AnnotationNode> annotations(List<AnnotationNode> a,List<AnnotationNode>b){var out=new ArrayList<AnnotationNode>();if(a!=null)out.addAll(a);if(b!=null)out.addAll(b);return out;}
    static Object value(AnnotationNode node,String key) {if(node.values!=null)for(int i=0;i<node.values.size();i+=2)if(node.values.get(i).equals(key))return node.values.get(i+1);return null;}
    static boolean hasMethod(ClassNode target,String selector) {
        int at=selector.indexOf('(');String name=at<0?selector:selector.substring(0,at),descriptor=at<0?null:selector.substring(at);
        return target.methods.stream().anyMatch(m->m.name.equals(name)&&(descriptor==null||descriptor.equals(m.desc)));
    }
    static boolean inheritedMethod(ClassNode target,String selector)throws Exception {
        if(target==null)return false;if(hasMethod(target,selector))return true;
        if(target.superName!=null&&inheritedMethod(read(target.superName),selector))return true;
        for(String iface:target.interfaces)if(inheritedMethod(read(iface),selector))return true;
        return false;
    }
    static int auditAt(Object value,String context)throws Exception {
        int failures=0;
        if(value instanceof List<?> list)for(var element:list)failures+=auditAt(element,context);
        if(value instanceof AnnotationNode annotation) {
            if(annotation.desc.endsWith("/At;")) {
                Object kind=value(annotation,"value"),reference=value(annotation,"target");
                if("INVOKE".equals(kind)&&reference instanceof String target&&target.startsWith("L")) {
                    int split=target.indexOf(';');String owner=target.substring(1,split),selector=target.substring(split+1);
                    if(!inheritedMethod(read(owner),selector)){System.out.println(context+" INVOKE TARGET "+target);failures++;}
                }
            }
            if(annotation.values!=null)for(int i=1;i<annotation.values.size();i+=2)failures+=auditAt(annotation.values.get(i),context);
        }
        return failures;
    }
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<URL>();for(String item:Files.readString(Path.of(args[1])).split(java.io.File.pathSeparator))urls.add(Path.of(item).toUri().toURL());
        loader=new URLClassLoader(urls.toArray(URL[]::new),null);
        var config=Files.readString(Path.of(args[2]));int failures=0;
        try(var paths=Files.walk(Path.of(args[0]))) {
            for(Path path:paths.filter(p->p.toString().endsWith(".class")&&!p.getFileName().toString().contains("$")).sorted().toList()) {
                if(!config.contains("\""+path.getFileName().toString().replace(".class","")+"\""))continue;
                ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(mixin,ClassReader.SKIP_CODE);
                for(var annotation:annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations))if(annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                    var targets=new ArrayList<String>();Object classes=value(annotation,"value"),strings=value(annotation,"targets");
                    if(classes instanceof List<?> list)for(var item:list)targets.add(((Type)item).getInternalName());
                    if(strings instanceof List<?> list)for(var item:list)targets.add(item.toString().replace('.','/'));
                    for(String name:targets) {
                        var target=read(name);if(target==null){System.out.println(mixin.name+" MISSING CLASS "+name);failures++;continue;}
                        for(var field:mixin.fields)for(var ann:annotations(field.visibleAnnotations,field.invisibleAnnotations))if(ann.desc.endsWith("/Shadow;")) {
                            boolean found=target.fields.stream().anyMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc));
                            if(!found){System.out.println(path.getFileName()+" SHADOW FIELD "+field.name+":"+field.desc+" -> "+name);failures++;}
                        }
                        for(var method:mixin.methods)for(var ann:annotations(method.visibleAnnotations,method.invisibleAnnotations)) {
                            failures+=auditAt(ann,path.getFileName()+"."+method.name);
                            if(ann.desc.endsWith("/Shadow;")&&!hasMethod(target,method.name+method.desc)){System.out.println(path.getFileName()+" SHADOW METHOD "+method.name+method.desc+" -> "+name);failures++;}
                            Object selectors=value(ann,"method");if(selectors instanceof List<?> list)for(var selector:list)if(!hasMethod(target,selector.toString())){System.out.println(path.getFileName()+" INJECTION "+selector+" -> "+name);failures++;}
                            if(ann.desc.endsWith("/Inject;") && selectors instanceof List<?> list) {
                                var handlerArgs=Type.getArgumentTypes(method.desc);int count=0;
                                while(count<handlerArgs.length&&!handlerArgs[count].getDescriptor().contains("/callback/CallbackInfo"))count++;
                                for(var selected:list) {
                                    String selector=selected.toString(),methodName=selector.split("\\(",2)[0];
                                    for(var destination:target.methods)if(destination.name.equals(methodName)&&(selector.indexOf('(')<0||selector.equals(destination.name+destination.desc))) {
                                        if((destination.access&Opcodes.ACC_STATIC)!=0&&(method.access&Opcodes.ACC_STATIC)==0){System.out.println(path.getFileName()+" STATIC TARGET "+method.name+" -> "+destination.name+destination.desc);failures++;}
                                        var destinationArgs=Type.getArgumentTypes(destination.desc);boolean matches=count==0||count==destinationArgs.length;
                                        if(matches)for(int i=0;i<count;i++)if(!handlerArgs[i].equals(destinationArgs[i])){matches=false;break;}
                                        if(!matches){System.out.println(path.getFileName()+" HANDLER "+method.name+method.desc+" -> "+name+"."+destination.name+destination.desc);failures++;}
                                    }
                                }
                            }
                            if(ann.desc.endsWith("/Accessor;")){Object selector=value(ann,"value");if(selector!=null&&target.fields.stream().noneMatch(f->f.name.equals(selector.toString()))){System.out.println(path.getFileName()+" ACCESSOR "+selector+" -> "+name);failures++;}}
                            if(ann.desc.endsWith("/Invoker;")){Object selector=value(ann,"value");if(selector!=null&&!hasMethod(target,selector+method.desc)){System.out.println(path.getFileName()+" INVOKER "+selector+method.desc+" -> "+name);failures++;}}
                        }
                    }
                }
            }
        }
        System.out.println("Unresolved selectors: "+failures);
    }
}
