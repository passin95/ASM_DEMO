package me.passin.asm

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter;
import org.apache.commons.io.FileUtils


class MethodConsumedTimePlugin extends Transform implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        // 注册 Transform。
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "methodConsumedTime"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        println "===============asm visit start==============="

        def startTime = System.currentTimeMillis()

        transformInvocation.inputs.each { input ->
            input.directoryInputs.each { directoryInput ->

                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        def fileName = file.name
                        if (isNeedInject(fileName)) {
                            ClassReader cr = new ClassReader(file.bytes)
                            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                            // 具体的插桩代码在 MethodConsumedTimeVisitor 中。
                            ClassVisitor cv = new MethodConsumedTimeVisitor(cw)

                            cr.accept(cv, ClassReader.EXPAND_FRAMES)

                            byte[] code = cw.toByteArray()
                            // 直接覆盖掉原来的字节码文件。
                            FileOutputStream fos = new FileOutputStream(file)
                            fos.write(code)
                            fos.close()
                        }
                    }
                }

                def outputDirFile = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY
                )

                FileUtils.copyDirectory(directoryInput.file, outputDirFile)
            }

            input.jarInputs.each { jarInput ->
                // jar 包不处理。
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR
                )
                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        def cost = System.currentTimeMillis() - startTime
        println "MethodConsumedTimePlugin cost $cost millisecond"
        println "===============asm visit end==============="
    }

    static isNeedInject(String name) {
        return name.endsWith(".class") && !name.startsWith("R\$") &&
                "R.class" != name && "BuildConfig.class" != name
    }

}