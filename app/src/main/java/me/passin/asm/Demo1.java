package me.passin.asm;

import java.io.File;
import java.io.FileOutputStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class Demo1 {

    static class Passin {

        public void insetCodeBlock() {
            System.out.println("insetCodeBlock run");
        }
    }

    public static void main(String[] args) throws Exception {
        // 读取对应文件或流的字节码。
        ClassReader classReader = new ClassReader("me/passin/asm/Demo1$Passin");
        // 第二个参数为 0 时，表明需要手动计算栈帧大小、局地变量和操作数栈的大小；
        // 为 ClassWriter.COMPUTE_MAXS 时，自动计算操作数栈大小和局部变量的最大个数，但需要自己计算栈帧大小；
        // 为 ClassWriter.COMPUTE_FRAMES 时，从头开始自动计算方法的堆栈映射框架，不需要调用 visitFrame() 和 visitMaxs()，即使调用也会被忽略。
        // 因此从上往下，性能越高也越麻烦。
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        CustomClassVisitor classVisitor = new CustomClassVisitor(Opcodes.ASM7, classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

        // 生成一个域（此处是常量），并添加相关关键字以及设置初始化值。
        FieldVisitor fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, "field", "I", null, 1);
        // 添加域上的注解。
        AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation("Landroidx/annotation/NonNull;", false);
        // 通知生成注解结束。
        annotationVisitor.visitEnd();
        // 通知生成变量结束。
        fieldVisitor.visitEnd();

        // 生成方法。
        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "method", "(I)V", null, null);
        // 加载类 System 中的静态变量 out 并压入栈顶，该变量的描述是 Ljava/io/PrintStream;。
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        // 将字符串 “Hello World” 常量池中压入栈顶。
        methodVisitor.visitLdcInsn("Hello World");
        // 调用对象 out 的 println 方法。注意顺序不能变，具体的顺序可查看上文的 JVM 指令集。
        methodVisitor
                .visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        // 当前方法返回 void。
        methodVisitor.visitInsn(Opcodes.RETURN);
        // 通知生成方法结束。
        methodVisitor.visitEnd();

        // 通知生成方法结束。
        classWriter.visitEnd();

        // 获得字节数组结果并输出到文件。
        byte[] newClassBytes = classWriter.toByteArray();
        // 获取项目根目录路径。
        String systemRootUrl = new File("").toURI().toURL().getPath();
        FileOutputStream fos = new FileOutputStream
                (systemRootUrl + "/app/src/main/java/me/passin/asm/Passin.class");
        fos.write(newClassBytes);
        fos.close();
    }

    static class CustomClassVisitor extends ClassVisitor {

        public CustomClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            // 如果方法名为 insetCodeBlock，则进行字节码插桩。
            if (name.equals("insetCodeBlock")) {
                // cv 为我们传入的 ClassWriter。
                MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);
                return new CustomClassMethod(Opcodes.ASM7, methodVisitor);
            }

            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    static class CustomClassMethod extends MethodVisitor {

        public CustomClassMethod(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitCode() {
            // 开始访问方法的代码时调用。
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("insetCodeBlock head");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            // 在 Return 指令前插入代码。
            if (opcode == Opcodes.RETURN) {
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn("insetCodeBlock foot");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }
            super.visitInsn(opcode);
        }
    }
}