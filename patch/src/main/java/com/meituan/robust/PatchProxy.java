package com.meituan.robust;

import android.text.TextUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by c_kunwu on 16/7/5.
 */
public class PatchProxy {

    private static Set<RobustExtension> registerSet=new LinkedHashSet<>();
    private static RobustExtension executedExtension=null;

    static public boolean isSupport(Object[] paramsArray, Object current, ChangeQuickRedirect changeQuickRedirect, boolean isStatic, int methodNumber,Class[] paramsClassTypes,Class returnType) {
        //Robust补丁优先执行，其他功能靠后
        if (changeQuickRedirect == null) {
            //不执行补丁，轮询其他监听者
            if(registerSet==null||registerSet.isEmpty()){
                return false;
            }
            for(RobustExtension robustExtension:registerSet){
                if(robustExtension.isSupport(paramsArray,current,methodNumber,paramsClassTypes,returnType)){
                    executedExtension=robustExtension;
                    return true;
                }
            }
            return false;
        }
        String classMethod = getClassMethod(isStatic, methodNumber);
        if (TextUtils.isEmpty(classMethod)) {
            return false;
        }
        Object[] objects = getObjects(paramsArray, current, isStatic);

        try {
            return changeQuickRedirect.isSupport(classMethod, objects);
        } catch (Throwable t) {
            return false;
        }
    }

    static public Object accessDispatch(Object[] arrayOfObject, Object current, ChangeQuickRedirect changeQuickRedirect, boolean isStatic, int methodNumber,Class[] paramsClassTypes,Class returnType) {
        if (changeQuickRedirect == null) {
            if(executedExtension!=null){
                return executedExtension.accessDispatch(arrayOfObject,current,methodNumber,paramsClassTypes,returnType);
            }
            return null;
        }
        String classMethod = getClassMethod(isStatic, methodNumber);
        if (TextUtils.isEmpty(classMethod)) {
            return null;
        }
        Object[] objects = getObjects(arrayOfObject, current, isStatic);
        return changeQuickRedirect.accessDispatch(classMethod, objects);
    }

    static public void accessDispatchVoid(Object[] arrayOfObject, Object current, ChangeQuickRedirect changeQuickRedirect, boolean isStatic, int methodNumber,Class[] paramsClassTypes,Class returnType) {
        if (changeQuickRedirect == null) {
            if(executedExtension!=null){
                executedExtension.accessDispatch(arrayOfObject,current,methodNumber,paramsClassTypes,returnType);
            }
            return;
        }
        String classMethod = getClassMethod(isStatic, methodNumber);
        if (TextUtils.isEmpty(classMethod)) {
            return;
        }
        Object[] objects = getObjects(arrayOfObject, current, isStatic);
        changeQuickRedirect.accessDispatch(classMethod, objects);
    }


    static private Object[] getObjects(Object[] arrayOfObject, Object current, boolean isStatic) {
        Object[] objects;
        if (arrayOfObject == null) {
            return null;
        }
        int argNum = arrayOfObject.length;
        if (isStatic) {
            objects = new Object[argNum];
        } else {
            objects = new Object[argNum + 1];
        }
        int x = 0;
        for (; x < argNum; x++) {
            objects[x] = arrayOfObject[x];
        }
        if (!(isStatic)) {
            objects[x] = current;
        }
        return objects;
    }

    static private String getClassMethod(boolean isStatic, int methodNumber) {
        String classMethod = "";
        try {
            java.lang.StackTraceElement stackTraceElement = (new java.lang.Throwable()).getStackTrace()[2];
            String methodName = stackTraceElement.getMethodName();
            String className = stackTraceElement.getClassName();
            classMethod = className + ":" + methodName + ":" + isStatic + ":" + methodNumber;
        } catch (Throwable t) {
        }
        return classMethod;
    }

    /***
     *
     * @param robustExtension
     * 注册RobustExtension监听器，通知当前执行程序
     * @return
     */
    public static boolean registerListener(RobustExtension robustExtension){
        if(registerSet==null){
            registerSet=new LinkedHashSet<RobustExtension>();
        }
        return registerSet.add(robustExtension);
    }

    public static boolean removeListener(RobustExtension robustExtension){
        if(registerSet==null){
            return false;
        }
        if(robustExtension.equals(executedExtension)){
            executedExtension=null;
        }
        return registerSet.remove(robustExtension);
    }

    private static void notifyListener(String info){
       for(RobustExtension robustExtension:registerSet){
           robustExtension.notifyListner(info);
           registerSet.remove(robustExtension);
       }
        //手动制空，防止内存泄漏
        registerSet=null;
    }

}
