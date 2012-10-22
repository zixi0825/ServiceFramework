package net.csdn.mongo.enhancer;

import javassist.*;
import net.csdn.common.Strings;
import net.csdn.common.logging.support.MessageFormat;
import net.csdn.common.settings.Settings;
import net.csdn.enhancer.Enhancer;

import java.io.DataInputStream;
import java.lang.reflect.Modifier;
import java.util.List;

import static net.csdn.common.collections.WowCollections.list;
import static net.csdn.common.logging.support.MessageFormat.format;

/**
 * User: WilliamZhu
 * Date: 12-10-18
 * Time: 上午11:14
 */
public class MongoEnhancer extends Enhancer {

    private Settings settings;

    public MongoEnhancer(Settings settings) {
        this.settings = settings;
    }

    private List<String> shouldNotCopyToSubclassStaticMethods = list(
            "where",
            "select",
            "order",
            "skip",
            "limit",
            "count",
            "in",
            "not",
            "notIn",
            "create",
            "findById",
            "find"
    );

    @Override
    public CtClass enhanceThisClass(DataInputStream dataInputStream) throws Exception {
        CtClass ctClass = classPool.makeClassIfNew(dataInputStream);
        CtClass document = ctClass.getSuperclass();


        // copy static fields to subclass.Importance because of inheritance strategy of java
        CtField[] ctFields = document.getFields();
        for (CtField ctField : ctFields) {
            if (Modifier.isStatic(ctField.getModifiers()) && ctField.getName().startsWith("parent$_")) {
                CtField ctField1 = new CtField(ctField.getType(), ctField.getName(), ctClass);
                ctField1.setModifiers(ctField.getModifiers());
                ctClass.addField(ctField1);
            }
        }

        //copy static methods to subclass
        CtMethod[] ctMethods = document.getMethods();

        for (CtMethod ctMethod : ctMethods) {
            if (Modifier.isStatic(ctMethod.getModifiers()) && !shouldNotCopyToSubclassStaticMethods.contains(ctMethod.getName())) {
                CtMethod ctNewMethod = CtNewMethod.copy(ctMethod, ctClass, null);
                ctClass.addMethod(ctNewMethod);
            }

        }


        enhanceCriteriaClassMethods(ctClass);

        //enhance getter/setter methods to put them into attributes field
        CtMethod[] modelMethods = ctClass.getMethods();

        for (CtMethod ctMethod : modelMethods) {
//            if (ctMethod.getName().startsWith("get")) {
//                //return (String)attributes.get({});
//                String fieldName = Strings.extractFieldFromGetSetMethod(ctMethod.getName());
//                try {
//                    ctClass.getDeclaredField(fieldName);
//                    String methodBody = MessageFormat.format(
//                            "return ({})attributes.get(\"{}\");",
//                            ctMethod.getReturnType().getSimpleName(),
//                            fieldName
//                    );
//                    ctMethod.setBody(methodBody);
//                } catch (NotFoundException exception) {
//                    //when NotFoundException happens means it is NOT a get/set method
//                }
//
//
//            } else
            if (ctMethod.getName().startsWith("set")) {
                try {
                    String fieldName = Strings.extractFieldFromGetSetMethod(ctMethod.getName());

                    ctClass.getDeclaredField(fieldName);
                    String methodBody = MessageFormat.format(
                            "attributes.put(\"{}\",{});",
                            fieldName,
                            fieldName
                    );
                    ctMethod.insertBefore(methodBody);

                } catch (NotFoundException exception) {
                    //when NotFoundException happens means it is NOT a get/set method
                }
                //attributes.put({},{})

            }
        }

        return ctClass;
    }

    private void enhanceCriteriaClassMethods(CtClass ctClass) throws Exception {
        String entityName = ctClass.getName();
        //String simpleEntityName = ctClass.getSimpleName();

        //create
        CtMethod create = CtMethod.make("public static net.csdn.mongo.Document create(java.util.Map params) { " +
                entityName + " doc = new " + entityName + "();" +
                "doc.attributes.putAll(params);" +
                "doc.attributesToPojo();" +
                "return doc;}", ctClass);
        ctClass.addMethod(create);


        String newCriteriaPiece = "new net.csdn.mongo.Criteria(" + entityName + ".class)";
        String returnTypeCriteriaPiece = "net.csdn.mongo.Criteria";

        //where
        CtMethod where = CtMethod.make(format("public static {} {}(java.util.Map params) {" +
                "        return {}.{}(" +
                "params" +
                ");" +
                "}", returnTypeCriteriaPiece, "where", newCriteriaPiece, "where"), ctClass);
        ctClass.addMethod(where);

        //select
        CtMethod select = CtMethod.make("public static net.csdn.mongo.Criteria select(String[] params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).select(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(select);


        //order
        CtMethod order = CtMethod.make("public static net.csdn.mongo.Criteria order(java.util.List params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).order(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(order);


        //skip
        CtMethod skip = CtMethod.make("public static net.csdn.mongo.Criteria skip(int params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).skip(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(skip);

        //limit
        CtMethod limit = CtMethod.make("public static net.csdn.mongo.Criteria limit(int params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).limit(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(limit);


        //count
        CtMethod count = CtMethod.make("public static int count() {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).count(" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(count);

        //in
        CtMethod in = CtMethod.make("public static net.csdn.mongo.Criteria in(java.util.Map params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).in(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(in);


        //not
        CtMethod not = CtMethod.make("public static net.csdn.mongo.Criteria not(java.util.Map params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).not(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(not);


        //notIn
        CtMethod notIn = CtMethod.make("public static net.csdn.mongo.Criteria notIn(java.util.Map params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).notIn(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(notIn);

        //findById
        CtMethod findById = CtMethod.make("public static net.csdn.mongo.Document findById(Object params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).findById(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(findById);


        //find
        CtMethod find = CtMethod.make("public static java.util.List find(java.util.List params) {" +
                "        return new net.csdn.mongo.Criteria(" + entityName + ".class).find(" +
                "params" +
                ");" +
                "    }", ctClass);
        ctClass.addMethod(find);


    }

    @Override
    public void enhanceThisClass2(List<CtClass> ctClasses) throws Exception {
        for (CtClass ctClass : ctClasses) {
            ctClass.toClass();
        }
    }
}