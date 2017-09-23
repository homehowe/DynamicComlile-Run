/**
 * 
 * java代码内存编译及运行
 * 
 */
public class JavaRunner {

	private String name;
	private String code;
	private SimpleJavaFileObject fileObject;
	public ClassFileManager fileManager;

	public ClassFileManager getFileManager() {
		return fileManager;
	}

	public void setFileManager(ClassFileManager fileManager) {
		this.fileManager = fileManager;
	}

	public JavaRunner(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public JsonResult compile() {
		JsonResult jsonResult = new JsonResult();
		try {
			fileObject = new DynamicJavaSourceCodeObject(name, code);

			JavaFileObject[] javaFileObjects = new JavaFileObject[] { fileObject };

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

			if (compiler == null) {
				jsonResult.addMsg("ToolProvider.getSystemJavaCompiler() is null,check Tools.jar!");
				jsonResult.setSuccess(false);
				return jsonResult;
			}

			StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);

			fileManager = new ClassFileManager(stdFileManager);

			Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFileObjects);

			List<String> compileOptions = new ArrayList<String>();
			compileOptions.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path")));
			compileOptions.addAll(Arrays.asList("-XDuseUnsharedTable"));

			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

			CompilationTask compilerTask = compiler.getTask(null, fileManager, diagnostics, compileOptions, null, compilationUnits);

			if (!compilerTask.call()) {
				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
					jsonResult.addMsg("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic + "\n");
				}
				jsonResult.setSuccess(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsonResult.addMsg(e.getMessage());
			jsonResult.setSuccess(false);
		}

		return jsonResult;
	}

	public long run() {
		long t1 = System.currentTimeMillis();

		try {
			fileManager.getClassLoader(null).loadClass(name).getDeclaredMethod("main", new Class[] { String[].class }).invoke(null, new Object[] { null });
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found: " + e);
		} catch (NoSuchMethodException e) {
			System.err.println("No such method: " + e);
		} catch (IllegalAccessException e) {
			System.err.println("Illegal access: " + e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof NoSuchElementException) {
				System.err.println("RuntimeError: java.util.NoSuchElementException: no more input\n\tat " + e.getCause().getStackTrace()[1].toString());
			} else {
				System.err.println("RuntimeError: " + e.getCause() + "\n\tat " + e.getCause().getStackTrace()[0].toString());
			}
		} catch (Exception e) {
			System.err.println("Exception： " + e);
		}

		long t2 = System.currentTimeMillis();
		return t2 - t1;
	}

	public void clear() {
		try {
			if (fileObject != null) {
				fileObject.delete();
			}
			if (fileManager != null) {
				fileManager.close();
				fileManager.unloadClass();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class DynamicJavaSourceCodeObject extends SimpleJavaFileObject {
		private String sourceCode;

		protected DynamicJavaSourceCodeObject(String name, String source) {
			super(URI.create("string:///" + name.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
			this.sourceCode = source;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return sourceCode;
		}

		public String getSourceCode() {
			return sourceCode;
		}
	}

	class JavaClassObject extends SimpleJavaFileObject {

		protected ByteArrayOutputStream bos = new ByteArrayOutputStream();

		public JavaClassObject(String name, Kind kind) {
			super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
		}

		public byte[] getBytes() {
			return bos.toByteArray();
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			return bos;
		}
	}

	class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

		private JavaClassObject jclassObject;

		private SecureClassLoader classLoader;

		public ClassFileManager(StandardJavaFileManager standardManager) {
			super(standardManager);
			this.classLoader = new SecureClassLoader() {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					byte[] b = jclassObject.getBytes();
					return super.defineClass(name, jclassObject.getBytes(), 0, b.length);
				}
			};
		}

		@Override
		public ClassLoader getClassLoader(Location location) {
			return this.classLoader;
		}

		public void unloadClass() {
			this.classLoader = null;
			this.jclassObject = null;
			System.gc();
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
			jclassObject = new JavaClassObject(className, kind);
			return jclassObject;
		}
	}
}
