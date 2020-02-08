import me.passin.asm.MethodConsumedTime
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class MethodConsumedTimeVisitor extends ClassVisitor {

    private String mClassName

    MethodConsumedTimeVisitor(ClassVisitor cv) {
        super(Opcodes.ASM6, cv)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        mClassName = name
    }

    @Override
    MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
        mv = new AdviceAdapter(api, mv, access, methodName, desc) {


            private boolean isNeedInserCode = false;
            private int timeLocalIndex = 0

            @Override
            AnnotationVisitor visitAnnotation(String desc1, boolean visible) {
                if (Type.getDescriptor(MethodConsumedTime.class) == desc1) {
                    isNeedInserCode = true;
                }
                return super.visitAnnotation(desc1, visible);
            }

            @Override
            protected void onMethodEnter() {
                super.onMethodEnter()
                if (isNeedInserCode) {
                    // 创建一个 long 类型的局部变量，并返回该变量的索引（通过索引来确定指令的操作对象）。
                    timeLocalIndex = newLocal(Type.LONG_TYPE)
                    // 调用静态方法 System.currentTimeMillis()，拿到刚进入方法时的时间戳，并将返回的结果推到栈顶。
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                    // 将栈顶 long 类型数值存入指定本地变量。
                    mv.visitVarInsn(LSTORE, timeLocalIndex)
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                super.onMethodExit(opcode)
                if (isNeedInserCode) {
                    // 拿到执行完方法的时间戳。
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                    // 将标识为 timeLocalIndex 的本地变量推到栈顶。
                    mv.visitVarInsn(LLOAD, timeLocalIndex)
                    // 将栈顶两 long 类型出栈后数值相减并将结果压入栈顶，这个差值也就是该方法的耗时。
                    mv.visitInsn(LSUB)
                    // 将差值存入指定本地变量。
                    mv.visitVarInsn(LSTORE, timeLocalIndex)

                    // 将插入的代码：Log.d("MethodConsumedTime", 方法所处类 -> 方法描述：方法耗时);
                    mv.visitLdcInsn("MethodConsumedTime")
                    // 实例化 StringBuilder 对象
                    mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
                    mv.visitInsn(DUP)
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                    mv.visitLdcInsn(mClassName)
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                    mv.visitLdcInsn(" -> ")
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                    mv.visitLdcInsn(methodName + methodDesc + "：")
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                    mv.visitVarInsn(LLOAD, timeLocalIndex)
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
                    mv.visitLdcInsn("ms")
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                    // 此时栈从底到顶的数值为：MethodConsumedTime，方法所处类 -> 方法描述：方法耗时，再使用栈顶 2 个数值作为参数调用 Log.d()。
                    mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
                    mv.visitInsn(POP)
                }
            }

        }
        return mv
    }
}