package ist.meic.pa;

import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.Translator;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class TraceTranslator implements Translator {

	private String methodSaveReturn = "";

	@Override
	public void onLoad(ClassPool pool, String className)
			throws NotFoundException, CannotCompileException {
		CtClass ctClass = pool.get(className);
		makeTracable(ctClass, pool);

	}

	private void makeTracable(CtClass ctClass, final ClassPool pool)
			throws CannotCompileException, NotFoundException {
		// System.out.println(ctClass.getName());
		if (ctClass.getPackageName() != null
				&& ctClass.getPackageName().equals("ist.meic.pa")
				&& !ctClass.getName().equals("ist.meic.pa.Trace"))
			return;
		for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			methodSaveReturn = "";
			if (ctMethod.isEmpty()
					|| Modifier.isNative(ctMethod.getModifiers())
					|| Modifier.isTransient(ctMethod.getModifiers()))
				continue;

			ctMethod.insertAfter(methodSaveReturn);

			ctMethod.instrument(new ExprEditor() {
				public void edit(NewExpr newEx) throws CannotCompileException {
					if (newEx.getClassName().startsWith("ist.meic.pa.History"))
						return;
					String src = "";
					/*
					 * System.out.println(newEx.getClassName() + " " +
					 * newEx.getFileName() + " at line: " +
					 * newEx.getLineNumber());
					 */
					try {
						src += "$_=$proceed($$); new ist.meic.pa.History().saveObject($_"
								+ ",\"  <- "
								+ newEx.getConstructor().getLongName()
								+ " on "
								+ newEx.getFileName()
								+ ":"
								+ newEx.getLineNumber() + "\");";
					} catch (NotFoundException e1) {
						e1.printStackTrace();
					}

					newEx.replace(src);
				}

				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					try {
						if (m.getClassName().startsWith("ist.meic.pa.History")
								|| m.getMethod().getLongName()
										.startsWith("java.lang")
								|| Modifier.isNative(m.getMethod().getModifiers())
								|| Modifier.isStrict(m.getMethod().getModifiers())
								|| Modifier.isTransient(m.getMethod().getModifiers()))
							/*
							 * || m.getMethod().getLongName()
							 * .startsWith("java.util"))
							 */
							return;

						/*System.out.println(m.getMethodName() + " "
								+ m.getFileName() + " at line: "
								+ m.getLineNumber());*/

						String methodCall = "";
						for (int i = 1; i <= m.getMethod().getParameterTypes().length; i++) {
							methodCall += " if(!$" + i
									+ ".getClass().isPrimitive()) "
									+ "	new ist.meic.pa.History().saveObject($"
									+ i + ",\"  -> "
									+ m.getMethod().getLongName() + " on "
									+ m.getFileName() + ":" + m.getLineNumber()
									+ "\"); ";
						}
						methodCall += "$_=$proceed($$);"
								+ "if($_ != null && !$_.getClass().isPrimitive()) "
								+ "new ist.meic.pa.History().saveObject($_,\"  <- "
								+ m.getMethod().getLongName() + " on "
								+ m.getFileName() + ":" + m.getLineNumber()
								+ "\");";
						m.replace(methodCall);
					} catch (NotFoundException e) {
						e.printStackTrace();
					}catch (Exception e) {
						//e.printStackTrace();
					}
				}

			});
		}

	}

	@Override
	public void start(ClassPool arg0) throws NotFoundException,
			CannotCompileException {

	}

}
