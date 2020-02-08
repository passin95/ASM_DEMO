package me.passin.asm;

import java.io.Serializable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class Demo {

    public static void main(String[] args) throws Exception {
        ClassReader cr = new ClassReader("me/passin/asm/Demo$Passin");
        ClassPrinter printer = new ClassPrinter();
        cr.accept(printer, ClassReader.EXPAND_FRAMES);
    }

    @Deprecated
    final static class Passin implements Serializable {

        public int field1 = 1;
        public final int field2 = 1;

        public Passin() {
        }

        public void method1(int i) throws Exception {
        }

        public float method2(Object o) {
            return 1.0f;
        }

        public Object method3() {
            return new Object();
        }
    }

    static class ClassPrinter extends ClassVisitor {

        public ClassPrinter() {
            super(Opcodes.ASM7);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            // version：是 JDK 版本号。
            // access：用于添加修饰符，在 ASM 中是以“Opcodes.ACC_”开头的常量。
            // signature：泛型信息，如果类并未定义任何泛型该参数为 null。
            super.visit(version, access, name, signature, superName, interfaces);
            if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                System.out.println("visit() run，类添加了 final 修饰符");
            }
            if (interfaces == null || interfaces.length == 0) {
                System.out.println("visit() run，name：" + name + "，superName：" + superName);
            } else {
                // 此处 demo 只实现了一个接口，因此直接取第一个元素。
                System.out.println(
                        "visit() run，name：" + name + "，superName：" + superName + "interfaces：" + interfaces[0]);
            }
        }

        @Override
        public void visitSource(String source, String debug) {
            System.out.println("visitSource() run，source：" + source + "，debug：" + debug);
            super.visitSource(source, debug);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            System.out.println("visitAnnotation() run，descriptor：" + descriptor + "，运行期是否可见：" + visible);
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            System.out.println(
                    "visitInnerClass() run，name：" + name + "，outerName：" + outerName + "，innerName：" + innerName);
            super.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            System.out.println("visitField() run，name：" + name + "，descriptor：" + descriptor + "，value：" + value);
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (exceptions != null) {
                System.out.println("visitMethod() run，name：" + name + "，desc：" + desc + "，exceptions：" + exceptions[0]);
            } else {
                System.out.println("visitMethod() run，name：" + name + "，desc：" + desc);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
                boolean visible) {
            System.out.println("visitTypeAnnotation() run");
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitEnd() {
            System.out.println("visitEnd() run");
            super.visitEnd();
        }
    }
}
