/**
 * 测试用例runner
 * 
 */
public class TestCaseRunner {

	//System.in/our/err与ThreadLocalInputStream、ThreadLocalPrintStream关联
	public TestCaseRunner() {
		System.setIn(new ThreadLocalInputStream(System.in));
		System.setOut(new ThreadLocalPrintStream(System.out));
		System.setErr(new ThreadLocalPrintStream(System.err));
	}

	/**
	 * runner选择方法
	 * 
	 * @param code
	 * @param name
	 * @param path
	 * @param type
	 * @param time
	 * @param caseList
	 * @return
	 */
	public JsonResult execute(String code, String name, String path, String type, int time, List<TestCase> caseList) {

		if ("java".equals(type)) {
			return run(code, name, time, caseList);
		} else if ("c".equals(type) || "cpp".equals(type)) {
			return run(code, name, path, type, time, caseList);
		} else {
			JsonResult jsonResult = new JsonResult();
			jsonResult.setSuccess(false);
			jsonResult.setMsg("请检查runner类型type参数是否为java、c或者cpp!");
			return jsonResult;
		}
	}

	/**
	 * java测试用例runner,内存编译及运行
	 * 
	 * @param code
	 *            代码
	 * @param name
	 *            类名称
	 * @param time
	 *            超时时间
	 * @param caseList
	 *            测试用例
	 * @return
	 */
	public JsonResult run(String code, String name, int time, List<TestCase> caseList) {
		JsonResult jsonResult = new JsonResult();
		JavaRunner javaRunner = new JavaRunner(name, code);

		try {
			JsonResult compileResult = javaRunner.compile();

			if (compileResult.isSuccess()) {

				int rightCase = 0;
				long timeout = time * 1000;

				ByteArrayOutputStream outputStream = null;
				ByteArrayOutputStream errStream = null;

				for (int i = 0; i < caseList.size(); i++) {
					TestCase caseVO = caseList.get(i);

					outputStream = new ByteArrayOutputStream();
					errStream = new ByteArrayOutputStream();

					RunCaseThread runCaseThread = new RunCaseThread(caseVO.getIncase(), javaRunner);
					FutureTask<Map<String, Object>> task = new FutureTask<Map<String, Object>>(runCaseThread);

					Thread thread = new Thread(task);
					thread.start();

					boolean success = false;
					long runtime = 0;
					long t1 = System.currentTimeMillis();
					try {
						Map<String, Object> resultMap = task.get(timeout, TimeUnit.MILLISECONDS);
						runtime = (long) resultMap.get("runTime");
						timeout -= runtime;

						outputStream = (ByteArrayOutputStream) resultMap.get("outputStream");
						errStream = (ByteArrayOutputStream) resultMap.get("errStream");

						success = true;
						jsonResult.addMsg("测试用例#" + (i + 1) + "运行" + runtime + "ms，");
					} catch (TimeoutException e) {
						long t2 = System.currentTimeMillis();
						task.cancel(true);
						thread.stop();

						jsonResult.addMsg("测试用例#" + (i + 1) + "运行" + (t2 - t1) + "ms，Not Pass，已超过本题限制时间" + time * 1000 + "ms！");
						break;
					} catch (InterruptedException e) {
						task.cancel(true);
						thread.stop();

						jsonResult.addMsg("测试用例#" + (i + 1) + "运行InterruptedException：" + e.getMessage());
					} catch (ExecutionException e) {
						task.cancel(true);
						thread.stop();

						jsonResult.addMsg("测试用例#" + (i + 1) + "运行ExecutionException：" + e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						task.cancel(true);
						thread.stop();

						jsonResult.addMsg("测试用例#" + (i + 1) + "运行Exception：" + e.getMessage());
						e.printStackTrace();
					}

					if (success) {
						String outCase1 = caseVO.getOutcase();
						outCase1 = StringUtils.trim(outCase1);
						outCase1 = outCase1.replaceAll("\r\n", "\n");

						String outCase2 = outputStream.toString();
						outCase2 = StringUtils.trim(outCase2);
						outCase2 = outCase2.replaceAll("\r\n", "\n");

						if (outCase1.equals(outCase2)) {
							rightCase++;
							jsonResult.appMsg("Pass！");
						} else {
							caseVO.setIncase(outCase1);
							caseVO.setOutcase(outCase2);
							jsonResult.appMsg("Not Pass！");
						}
					}
				}

				if (caseList.size() == rightCase) {
					jsonResult.setStatus(1);
				}
				jsonResult.setData(caseList);
				jsonResult.addMsg("\n共" + caseList.size() + "条测试用例：通过" + rightCase + "条，未通过" + (caseList.size() - rightCase) + "条！");
				if (errStream.size() > 0) {
					jsonResult.addMsg("\n运行错误：\n" + errStream.toString());
					jsonResult.setSuccess(false);
				}
			} else {
				jsonResult.setSuccess(false);
				jsonResult.setMsg("编译出错：\n" + compileResult.getMsg());
			}
		} catch (Exception e) {
			jsonResult.setSuccess(false);
			jsonResult.setMsg(e.getMessage());
			e.printStackTrace();
		} finally {
			javaRunner.clear();
		}
		return jsonResult;
	}

	/**
	 * C/C++测试用例runner,cmd gcc编译及运行
	 * 
	 * @param code
	 *            代码
	 * @param name
	 *            类名称
	 * @param path
	 *            编译路径
	 * @param type
	 *            c或者cpp
	 * @param time
	 *            超时时间
	 * @param caseList
	 *            测试用例
	 * @return
	 */
	public JsonResult run(String code, String name, String path, String type, int time, List<TestCase> caseList) {
		JsonResult jsonResult = new JsonResult();
		try {
			CCppRunner cCppRunner = new CCppRunner(name, code, path, type);

			JsonResult compileResult = cCppRunner.compile();

			if (compileResult.isSuccess()) {

				int rightCase = 0;

				long timeout = time * 1000;
				for (int i = 0; i < caseList.size(); i++) {
					TestCase caseVO = caseList.get(i);

					ProcessBuilder processBuilder = new ProcessBuilder(path + name);
					Process process = processBuilder.start();

					InputStream inputStream = process.getInputStream();
					OutputStream outputStream = process.getOutputStream();

					outputStream.write(caseVO.getIncase().getBytes("UTF-8"));
					outputStream.write("\n".getBytes());
					outputStream.flush();

					long t1 = System.currentTimeMillis();
					if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
						long runtime = System.currentTimeMillis() - t1;
						timeout -= runtime;

						inputStream.close();
						outputStream.close();

						// 超时结束exe进程
						process.destroy();
						jsonResult.addMsg("测试用例#" + (i + 1) + "运行" + runtime + "ms，Not Pass，已超过本题限制时间" + time * 1000 + "ms！");
						break;
					}

					long runtime = System.currentTimeMillis() - t1;
					timeout -= runtime;

					String outCase1 = caseVO.getOutcase();
					outCase1 = StringUtils.trim(outCase1);
					outCase1 = outCase1.replaceAll("\r\n", "\n");

					String outCase2 = cCppRunner.output(inputStream);
					outCase2 = StringUtils.trim(outCase2);
					outCase2 = outCase2.replaceAll("\r\n", "\n");

					if (outCase1.equals(outCase2)) {
						rightCase++;
						jsonResult.addMsg("测试用例#" + (i + 1) + "运行" + runtime + "ms，Pass！");
					} else {
						caseVO.setOutcase(outCase2);
						jsonResult.addMsg("测试用例#" + (i + 1) + "运行" + runtime + "ms，Not Pass！");
					}

					inputStream.close();
					outputStream.close();
				}

				if (caseList.size() == rightCase) {
					jsonResult.setStatus(1);
				}
				jsonResult.setData(caseList);
				jsonResult.addMsg("\n共" + caseList.size() + "条测试用例：通过" + rightCase + "条，未通过" + (caseList.size() - rightCase) + "条！");

			} else {
				jsonResult.setSuccess(false);
				jsonResult.setMsg("编译出错：\n" + compileResult.getMsg());
			}
		} catch (Exception e) {
			jsonResult.setSuccess(false);
			jsonResult.setMsg("编译或运行出错：\n" + e.getMessage());
			e.printStackTrace();
		}
		return jsonResult;
	}

	/**
	 * java测试用例运行线程
	 * 
	 * 
	 */
	public class RunCaseThread implements Callable<Map<String, Object>> {

		private String inCase;
		private JavaRunner runner;

		public RunCaseThread(String inCase, JavaRunner runner) {
			this.inCase = inCase;
			this.runner = runner;
		}

		@Override
		public Map<String, Object> call() {

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ByteArrayOutputStream errStream = new ByteArrayOutputStream();
			long runTime = 0;

			//线程独立的输入和输出
			((ThreadLocalInputStream) System.in).init(new ByteArrayInputStream(inCase.getBytes(StandardCharsets.UTF_8)));
			((ThreadLocalPrintStream) System.out).init(new ThreadLocalPrintStream(new PrintStream(outputStream)));
			((ThreadLocalPrintStream) System.err).init(new ThreadLocalPrintStream(new PrintStream(errStream)));

			try {

				runTime = runner.run();

			} catch (Exception e) {
				System.err.println("Exception： " + e);
			}

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("outputStream", outputStream);
			map.put("errStream", errStream);
			map.put("runTime", runTime);

			//还原System.in/out/err
			((ThreadLocalInputStream) System.in).init(null);
			((ThreadLocalPrintStream) System.out).init(null);
			((ThreadLocalPrintStream) System.err).init(null);

			return map;
		}
	}
}