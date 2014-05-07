package ist.meic.pa;

import javassist.*;
import javassist.expr.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Modifier;

public class TraceTranslator implements Translator {

	protected String methodSaveArgs = "";
	private String methodSaveReturn = "";

	@Override
	public void onLoad(ClassPool pool, String className)
			throws NotFoundException, CannotCompileException {
		CtClass ctClass = pool.get(className);
		makeTracable(ctClass, pool);

	}

	private void makeTracable(CtClass ctClass, final ClassPool pool)
			throws CannotCompileException, NotFoundException {
		for (final CtBehavior ctMethod : ctClass.getDeclaredBehaviors()) {
			ctMethod.instrument(new ExprEditor() {

				public void edit(NewExpr newEx) throws CannotCompileException {
					String saveNewObject = newExprEval(newEx);
					newEx.replace(saveNewObject);
				}

				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					try {
						String methodCall = "";
						for (int i = 1; i <= m.getMethod().getParameterTypes().length; i++) {
							methodCall = saveObjectArgString(m, methodCall, i);
						}
						methodCall = saveObjectRetString(m, methodCall);
						m.replace(methodCall);
					} catch (NotFoundException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public void start(ClassPool arg0) throws NotFoundException,
			CannotCompileException {

	}

	private String newExprEval(NewExpr newEx) throws CannotCompileException {
		String src = "";
		/*
		 * System.out.println(newEx.getClassName() + " " + newEx.getFileName() +
		 * " at line: " + newEx.getLineNumber());
		 */
		try {
			src += "$_=$proceed($$); ist.meic.pa.History.saveObject(($w)$_"
					+ ",\"  <- " + newEx.getConstructor().getLongName()
					+ " on " + newEx.getFileName() + ":"
					+ newEx.getLineNumber() + "\");";
		} catch (NotFoundException e1) {
			e1.printStackTrace();
		}

		return src;
	}

	private String saveObjectArgString(MethodCall m, String methodCall, int i)
			throws NotFoundException {
		methodCall += " if(!(($w)$" + i + ").getClass().isPrimitive()) "
				+ "	ist.meic.pa.History.saveObject((($w)$" + i + "),\"  -> "
				+ m.getMethod().getLongName() + " on " + m.getFileName() + ":"
				+ m.getLineNumber() + "\");";
		return methodCall;
	}

	private String saveObjectRetString(MethodCall m, String methodCall)
			throws NotFoundException {
		methodCall += "$_=$proceed($$);"
				+ "if((($w)$_) != null && !(($w)$_).getClass().isPrimitive()) "
				+ " ist.meic.pa.History.saveObject(($w)$_,\"  <- "
				+ m.getMethod().getLongName() + " on " + m.getFileName() + ":"
				+ m.getLineNumber() + "\");";
		return methodCall;
	}

}
