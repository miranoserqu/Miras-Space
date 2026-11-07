package com.miranoserqu.miras_space.network;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("com.miranoserqu.miras_space.com.miranoserqu.miras_space.network.Sendable")
@SupportedSourceVersion(SourceVersion.RELEASE_26)
public class PayloadRegister extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return false;

        StringBuilder code = new StringBuilder();
        code.append("package com.miranoserqu.miras_space.generated;\n\n");
        code.append("import java.util.Map;\n");
        code.append("import java.io.DataInputStream;\n");
        code.append("import com.miranoserqu.miras_space.game.Game;\n");
        code.append("import com.miranoserqu.miras_space.game.com.miranoserqu.miras_space.network.Sendable;\n");
        code.append("import java.util.HashMap;\n\n");
        code.append("public class PayloadManager {\n\n");
        code.append("    private final Map<Integer, Class<? extends Payload>> payloads = new HashMap<>();\n");
        code.append("    private final Map<Integer, Sendable.Target> targets = new HashMap<>();\n");
        code.append("    private static final PayloadManager MANAGER = new PayloadManager();\n\n");
        code.append("    public static PayloadManager getInstance(){\n");
        code.append("        return MANAGER;\n");
        code.append("    }\n\n");
        code.append("    public PayloadManager(){\n");

        for (Element element : roundEnv.getElementsAnnotatedWith(Sendable.class)) {
            if (element instanceof TypeElement typeElement) {
                Sendable annotation = typeElement.getAnnotation(Sendable.class);
                code.append(String.format("        this.payloads.put(%d, %s.class.asSubClass(Payload.class));\n",
                        annotation.id(), typeElement.getQualifiedName()));
                switch(annotation.target()) {
                    case SERVER -> code.append(String.format("        this.targets.put(%d, Sendable.Target.SERVER);\n",
                            annotation.id()));
                    case CLIENT -> code.append(String.format("        this.targets.put(%d, Sendable.Target.CLIENT);\n",
                            annotation.id()));
                }
            }
        }

        code.append("        return map;\n");
        code.append("    }\n\n");
        code.append("    public static managePayload(DataInputStream dis, int id){\n");
        code.append("        Game game = Game.getInstance();");
        code.append("        switch(this.targets(id)){\n");
        code.append("            case Sendable.Target.SERVER -> game.getServerBus().post(payloads.get(id).getConstructor(DataInputStream.class).newInstance(dis))\n");
        code.append("            case Sendable.Target.CLIENT -> game.getClientBus().post(payloads.get(id).getConstructor(DataInputStream.class).newInstance(dis))\n");
        code.append("        }\n");
        code.append("}\n");

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile("com.miranoserqu.miras_space.generated.PayloadManager");
            try (Writer writer = file.openWriter()) {
                writer.write(code.toString());
            }
        } catch (IOException ignored) {}

        return true;
    }
}